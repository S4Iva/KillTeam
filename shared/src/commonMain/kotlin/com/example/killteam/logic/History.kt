package com.example.killteam.logic

import com.example.killteam.data.ApprovedOps
import com.example.killteam.data.CompositionRules
import com.example.killteam.data.KillOp
import com.example.killteam.data.Killzone
import com.example.killteam.data.KillTeam
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** yyyy-MM-dd (UTC) from epoch milliseconds, via the civil-from-days algorithm. */
fun formatDate(epochMillis: Long): String {
    val z = epochMillis.floorDiv(86_400_000L) + 719_468
    val era = z.floorDiv(146_097L)
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val y = yoe + era * 400 + if (m <= 2) 1 else 0
    return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

@Serializable
data class Roster(
    val id: Long,
    val name: String,
    val teamSlug: String,
    /** Datacard names; the same name may occur several times. */
    val operatives: List<String> = emptyList(),
    /** Chosen weapon option per entry of [operatives] (label of a [com.example.killteam.data.WeaponOption], blank = none). May be shorter than [operatives]. */
    val options: List<String> = emptyList(),
    /** Equipment of the roster, at most [ApprovedOps.MAX_EQUIPMENT] different entries. */
    val equipment: List<String> = emptyList(),
    /** Index into [operatives] of the one operative carrying an equipment entry; equipment without an owner is not assigned yet. */
    val equipmentOwners: Map<String, Int> = emptyMap(),
) {
    fun optionAt(index: Int): String = options.getOrNull(index).orEmpty()

    /** Datacard name, numbered when the roster has the same datacard several times. */
    fun slotLabel(slot: Int): String {
        val name = operatives[slot]
        return if (operatives.count { it == name } > 1) "$name ${operatives.take(slot + 1).count { it == name }}" else name
    }

    fun ownerOf(equipmentName: String): Int? = equipmentOwners[equipmentName]?.takeIf { it in operatives.indices }

    fun equipmentOf(operativeIndex: Int): List<String> = equipment.filter { ownerOf(it) == operativeIndex }

    val unassignedEquipment: List<String> get() = equipment.filter { ownerOf(it) == null }

    fun withEquipmentOwner(equipmentName: String, owner: Int?) =
        copy(equipmentOwners = if (owner == null) equipmentOwners - equipmentName else equipmentOwners + (equipmentName to owner))

    fun withoutEquipment(equipmentName: String) = copy(equipment = equipment - equipmentName, equipmentOwners = equipmentOwners - equipmentName)

    /** Gives the equipment to one operative; it joins the roster's equipment unless that is already full, and leaves its previous operative. */
    fun withEquipmentOn(slot: Int, equipmentName: String): Roster {
        if (slot !in operatives.indices) return this
        if (equipmentName !in equipment && equipment.size >= ApprovedOps.MAX_EQUIPMENT) return this
        val list = if (equipmentName in equipment) equipment else equipment + equipmentName
        return copy(equipment = list, equipmentOwners = equipmentOwners + (equipmentName to slot))
    }

    private fun paddedOptions() = List(operatives.size) { optionAt(it) }

    fun withAdded(name: String) = copy(operatives = operatives + name, options = paddedOptions() + "")

    /** Removes the last entry with this datacard name together with its chosen option. */
    fun withRemovedLast(name: String): Roster = withRemovedAt(operatives.indexOfLast { it == name })

    /** Removes one entry with its option; its equipment leaves the roster, later operatives keep theirs. */
    fun withRemovedAt(index: Int): Roster {
        if (index !in operatives.indices) return this
        val dropped = equipmentOwners.filterValues { it == index }.keys
        return copy(
            operatives = operatives.toMutableList().also { it.removeAt(index) },
            options = paddedOptions().toMutableList().also { it.removeAt(index) },
            equipment = equipment - dropped,
            equipmentOwners = equipmentOwners.mapNotNull { (e, o) -> if (o == index) null else e to if (o > index) o - 1 else o }.toMap(),
        )
    }

    fun withOption(index: Int, option: String): Roster {
        if (index !in operatives.indices) return this
        return copy(options = paddedOptions().toMutableList().also { it[index] = option })
    }
}

object RosterRules {
    /** Datacard weapons of the chosen option of an operative; empty means all weapons of the datacard (no option or not chosen). */
    fun weaponsOf(team: KillTeam?, operative: String, option: String): List<String> =
        if (option.isBlank()) emptyList() else team?.weaponOptions?.get(operative)?.firstOrNull { it.label == option }?.weapons.orEmpty()

    /** Notes that do not block saving: options not chosen and too much equipment. */
    fun warnings(team: KillTeam, roster: Roster): List<String> = buildList {
        roster.operatives.forEachIndexed { i, name ->
            if (team.weaponOptions[name] != null && roster.optionAt(i).isBlank()) add("Weapon option not selected: $name")
        }
        val limits = team.compositionLimits
        limits.distinctOptions.forEach { card ->
            roster.operatives.indices.filter { roster.operatives[it] == card && roster.optionAt(it).isNotBlank() }
                .groupBy { roster.optionAt(it) }.filterValues { it.size > 1 }.keys.forEach { add("Weapon option chosen twice: $it ($card)") }
        }
        limits.weaponMax.forEach { (weapon, max) ->
            val n = roster.operatives.indices.count { weapon in slotWeapons(team, roster, it) }
            if (n > max) add("Too many with $weapon: $max allowed, $n selected.")
        }
        if (roster.equipment.distinct().size > ApprovedOps.MAX_EQUIPMENT) add("More than ${ApprovedOps.MAX_EQUIPMENT} equipment.")
        roster.unassignedEquipment.forEach { add("Equipment not given to an operative: $it") }
    }

    /** Lower-case weapon names (without profile suffix) of the option chosen at [slot]; empty when none is chosen. */
    private fun slotWeapons(team: KillTeam, roster: Roster, slot: Int): Set<String> =
        team.weaponOptions[roster.operatives[slot]]?.firstOrNull { it.label == roster.optionAt(slot) }?.weapons.orEmpty().map { CompositionRules.weaponBase(it) }.toSet()

    /** Why [option] cannot be chosen for the operative at [slot] under the composition text, or null when it can. */
    fun optionBlock(team: KillTeam, roster: Roster, slot: Int, option: String): String? {
        val card = roster.operatives.getOrNull(slot) ?: return null
        val limits = team.compositionLimits
        val others = roster.operatives.indices.filter { it != slot }
        if (card in limits.distinctOptions) {
            others.firstOrNull { roster.operatives[it] == card && roster.optionAt(it) == option }?.let { return "used by ${roster.slotLabel(it)}" }
        }
        val weapons = team.weaponOptions[card]?.firstOrNull { it.label == option }?.weapons.orEmpty().map { CompositionRules.weaponBase(it) }.toSet()
        limits.weaponMax.forEach { (weapon, max) ->
            if (weapon in weapons && others.count { weapon in slotWeapons(team, roster, it) } >= max) return "limit: $max $weapon"
        }
        return null
    }

    /** Operatives a player brings to a game: the roster size, else the size from the team rules, else [default] (within the kill grade table). */
    fun gameSize(team: KillTeam?, roster: Roster?, default: Int = 6): Int {
        val size = roster?.operatives?.size?.takeIf { it > 0 } ?: team?.let { expectedSize(it) }?.takeIf { it > 0 } ?: default
        return size.coerceIn(KillOp.minOperatives, KillOp.maxOperatives)
    }

    /** How many more operatives of this datacard the team size and the composition text still allow (at most 99). */
    fun room(team: KillTeam, roster: Roster, card: String): Int {
        val expected = expectedSize(team)
        val totalRoom = if (expected > 0) (expected - roster.operatives.size).coerceAtLeast(0) else 99
        val textRoom = team.compositionLimits.room(roster.operatives.groupingBy { it }.eachCount(), card) ?: 99
        return minOf(totalRoom, textRoom, 99)
    }

    /** Team size derived from composition lines such as "1 KASRKIN SERGEANT operative ..." and "9 KASRKIN operatives ...". */
    fun expectedSize(team: KillTeam): Int =
        team.composition.sumOf { line -> Regex("^(\\d+)\\s").find(line.trim())?.groupValues?.get(1)?.toInt() ?: 0 }

    fun problems(team: KillTeam, roster: Roster): List<String> = buildList {
        val expected = expectedSize(team)
        if (expected > 0 && roster.operatives.size != expected) {
            add("Team needs $expected operatives, selected ${roster.operatives.size}.")
        }
        addAll(team.compositionLimits.problems(roster.operatives.groupingBy { it }.eachCount()))
        val known = team.operatives.map { it.name }.toSet()
        roster.operatives.filter { it !in known }.distinct().forEach { add("Unknown operative: $it") }
    }
}

@Serializable
data class ResultPlayer(
    val name: String,
    val teamSlug: String? = null,
    val tacOpId: String? = null,
    val primary: OpKind? = null,
    val crit: Int,
    val kill: Int,
    val tac: Int,
    val bonus: Int,
) {
    val total get() = crit + kill + tac + bonus
}

@Serializable
data class GameResult(
    val id: Long,
    val playedAt: Long,
    val critOpIndex: Int,
    val killzone: Killzone,
    val mapIndex: Int,
    val players: List<ResultPlayer>,
    /** 0 or 1, or -1 for a draw. */
    val winner: Int,
)

fun GameState.toResult(): GameResult {
    val scores = players.indices.map { Scoring.score(this, it) }
    return GameResult(
        id = id,
        playedAt = nowMillis(),
        critOpIndex = mission.critOpIndex,
        killzone = mission.killzone,
        mapIndex = mission.mapIndex,
        players = players.mapIndexed { i, p ->
            ResultPlayer(p.name, p.teamSlug, p.tacOpId, p.primary, scores[i].crit, scores[i].kill, scores[i].tac, scores[i].primaryBonus)
        },
        winner = Scoring.winner(this),
    )
}

data class TeamStat(val key: String, val games: Int, val wins: Int, val draws: Int, val losses: Int, val avgVp: Double)

object Statistics {
    private data class Entry(val key: String, val vp: Int, val outcome: Int) // outcome: 1 win, 0 draw, -1 loss

    private fun stats(entries: List<Entry>): List<TeamStat> = entries.groupBy { it.key }.map { (key, list) ->
        TeamStat(key, list.size, list.count { it.outcome == 1 }, list.count { it.outcome == 0 }, list.count { it.outcome == -1 }, list.map { it.vp }.average())
    }.sortedByDescending { it.games }

    private fun outcome(r: GameResult, i: Int) = if (r.winner == -1) 0 else if (r.winner == i) 1 else -1

    fun byTeam(results: List<GameResult>): List<TeamStat> = stats(
        results.flatMap { r -> r.players.mapIndexedNotNull { i, p -> p.teamSlug?.let { Entry(it, p.total, outcome(r, i)) } } },
    )

    fun byCritOp(results: List<GameResult>, name: (Int) -> String): List<TeamStat> = stats(
        results.flatMap { r -> r.players.mapIndexed { i, p -> Entry(name(r.critOpIndex), p.total, outcome(r, i)) } },
    )

    fun byTacOp(results: List<GameResult>, name: (String) -> String): List<TeamStat> = stats(
        results.flatMap { r -> r.players.mapIndexedNotNull { i, p -> p.tacOpId?.let { Entry(name(it), p.total, outcome(r, i)) } } },
    )
}
