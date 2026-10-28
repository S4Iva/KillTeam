package com.example.killteam.logic

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
)

object RosterRules {
    /** Team size derived from composition lines such as "1 KASRKIN SERGEANT operative ..." and "9 KASRKIN operatives ...". */
    fun expectedSize(team: KillTeam): Int =
        team.composition.sumOf { line -> Regex("^(\\d+)\\s").find(line.trim())?.groupValues?.get(1)?.toInt() ?: 0 }

    fun problems(team: KillTeam, roster: Roster): List<String> = buildList {
        val expected = expectedSize(team)
        if (expected > 0 && roster.operatives.size != expected) {
            add("Team needs $expected operatives, selected ${roster.operatives.size}.")
        }
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
