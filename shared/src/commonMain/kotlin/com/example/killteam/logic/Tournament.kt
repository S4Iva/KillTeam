package com.example.killteam.logic

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class TPlayer(val id: Int, val name: String)

/** `b == null` is a bye (counts as a win). VPs are the totals of both players; primary VPs are the primary-op bonus VP. */
@Serializable
data class Pairing(
    val a: Int,
    val b: Int?,
    val vpA: Int? = null,
    val vpB: Int? = null,
    val primaryA: Int = 0,
    val primaryB: Int = 0,
) {
    val played get() = b == null || (vpA != null && vpB != null)
}

@Serializable
data class TRound(val critOpIndex: Int, val pairings: List<Pairing>)

@Serializable
data class Tournament(
    val id: Long,
    val name: String,
    val players: List<TPlayer>,
    val rounds: List<TRound> = emptyList(),
)

enum class Outcome(val letter: Char, val points: Int) { Win('W', 2), Draw('D', 1), Loss('L', 0) }

data class Standing(
    val player: TPlayer,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    /** Win path: the result of each played round in order, e.g. "WWL". */
    val path: String,
    val strengthOfSchedule: Double,
    val primaryVp: Int,
) {
    /** Two draws equal one win plus one loss. */
    val points get() = wins * 2 + draws
}

object TournamentLogic {
    private fun outcome(p: Pairing, playerId: Int): Outcome? {
        if (!p.played) return null
        if (p.b == null) return Outcome.Win
        val mine = if (p.a == playerId) p.vpA!! else p.vpB!!
        val theirs = if (p.a == playerId) p.vpB!! else p.vpA!!
        return when {
            mine > theirs -> Outcome.Win
            mine < theirs -> Outcome.Loss
            else -> Outcome.Draw
        }
    }

    private fun opponent(p: Pairing, playerId: Int): Int? = if (p.a == playerId) p.b else if (p.b == playerId) p.a else null

    private fun playerPairings(t: Tournament, id: Int) = t.rounds.flatMap { r -> r.pairings.filter { it.a == id || it.b == id } }

    fun standings(t: Tournament): List<Standing> {
        val base = t.players.map { pl ->
            val games = playerPairings(t, pl.id)
            val outcomes = games.mapNotNull { outcome(it, pl.id) }
            val primary = games.sumOf { if (it.a == pl.id) it.primaryA else it.primaryB }
            Triple(pl, outcomes, primary)
        }
        val pointsById = base.associate { (pl, outcomes, _) -> pl.id to outcomes.sumOf { it.points } }
        return base.map { (pl, outcomes, primary) ->
            val opponents = playerPairings(t, pl.id).filter { it.played }.mapNotNull { opponent(it, pl.id) }
            val sos = if (opponents.isEmpty()) 0.0 else opponents.map { pointsById[it] ?: 0 }.average()
            Standing(
                pl, outcomes.count { it == Outcome.Win }, outcomes.count { it == Outcome.Draw }, outcomes.count { it == Outcome.Loss },
                outcomes.joinToString("") { it.letter.toString() }, sos, primary,
            )
        }
    }

    /** Record, then strength of schedule (opponents' records), then total primary-op VP. */
    fun ranking(t: Tournament): List<Standing> =
        standings(t).sortedWith(compareByDescending<Standing> { it.points }.thenByDescending { it.strengthOfSchedule }.thenByDescending { it.primaryVp })

    /** Round one is random; later rounds pair by record, then win path, then randomly. An odd player out gets a bye. */
    fun nextPairings(t: Tournament, random: Random = Random.Default): List<Pairing> {
        val standing = standings(t).shuffled(random)
        val order = if (t.rounds.isEmpty()) standing
        else standing.sortedWith(compareByDescending<Standing> { it.points }.thenByDescending { pathKey(it.path) })
        val ids = order.map { it.player.id }.toMutableList()

        if (ids.size % 2 == 1) {
            val hadBye = t.rounds.flatMap { it.pairings }.filter { it.b == null }.map { it.a }.toSet()
            val bye = ids.lastOrNull { it !in hadBye } ?: ids.last()
            ids.remove(bye)
            return pairUp(t, ids) + Pairing(bye, null)
        }
        return pairUp(t, ids)
    }

    private fun pathKey(path: String) = path.map { when (it) { 'W' -> 2; 'D' -> 1; else -> 0 } }.joinToString("")

    private fun pairUp(t: Tournament, ids: List<Int>): List<Pairing> {
        val played = t.rounds.flatMap { it.pairings }.filter { it.b != null }.map { setOf(it.a, it.b!!) }.toSet()
        val remaining = ids.toMutableList()
        val result = mutableListOf<Pairing>()
        while (remaining.isNotEmpty()) {
            val first = remaining.removeAt(0)
            val partner = remaining.firstOrNull { setOf(first, it) !in played } ?: remaining.first()
            remaining.remove(partner)
            result += Pairing(first, partner)
        }
        return result
    }
}
