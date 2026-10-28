package com.example.killteam.logic

import com.example.killteam.data.ApprovedOps
import com.example.killteam.data.KillOp
import kotlinx.serialization.Serializable

@Serializable
enum class OpKind(val label: String) {
    Crit("Crit Op"),
    Kill("Kill Op"),
    Tac("Tac Op"),
}

@Serializable
enum class InitiativeCard(val label: String, val modifier: Int) {
    Reroll("Re-roll", 0),
    Mod1("+1/-1", 1),
    Mod2("+2/-2", 2),
    Mod3("+3/-3", 3),
}

@Serializable
enum class GamePhase(val label: String) {
    SetUpBattle("1. Set up the Battle"),
    SelectOperatives("2. Select Operatives"),
    SetUpOperatives("3. Set Up Operatives"),
    Play("4. Play the Battle"),
    End("5. End the Battle"),
}

@Serializable
data class OperativeTrack(
    val name: String,
    val maxWounds: Int,
    val wounds: Int = maxWounds,
    val engage: Boolean = false,
    val expended: Boolean = false,
    val incapacitated: Boolean = false,
) {
    val wounded get() = wounds < maxWounds
    val injured get() = wounds * 2 < maxWounds
}

@Serializable
data class PlayerState(
    val name: String,
    val teamSlug: String? = null,
    val critVp: Int = 0,
    val tacVp: Int = 0,
    /** Starting number of enemy operatives (row of the kill grade table). */
    val enemyStartCount: Int = 6,
    /** Number of enemy operatives this player has incapacitated. */
    val kills: Int = 0,
    val cp: Int = ApprovedOps.START_CP,
    val tacOpId: String? = null,
    val tacRevealed: Boolean = false,
    val equipment: List<String> = emptyList(),
    val primary: OpKind? = null,
    val primaryRevealed: Boolean = false,
    val cards: List<InitiativeCard> = emptyList(),
    val operatives: List<OperativeTrack> = emptyList(),
) {
    val killGrade get() = KillOp.grade(enemyStartCount, kills)
}

@Serializable
data class GameState(
    val id: Long,
    val startedAt: Long,
    val mission: MissionState,
    val players: List<PlayerState>,
    val phase: GamePhase = GamePhase.SetUpBattle,
    val turningPoint: Int = 1,
    /** Index of the player with initiative. */
    val initiative: Int = 0,
    val ended: Boolean = false,
    /** Initiative roll-off results of the current turning point, including card modifiers. */
    val rolls: List<Int?> = listOf(null, null),
    /** Winner of this turning point's roll-off; null until the roll-off is resolved. */
    val rollOffWinner: Int? = null,
    val gainedCard: InitiativeCard? = null,
    val initiativeBeforeRollOff: Int = 0,
    val initiativeChosen: Boolean = false,
    val readyDone: Boolean = false,
) {
    fun updatePlayer(index: Int, change: (PlayerState) -> PlayerState) =
        copy(players = players.mapIndexed { i, p -> if (i == index) change(p) else p })
}

data class Score(val crit: Int, val kill: Int, val tac: Int, val primaryBonus: Int) {
    val total get() = crit + kill + tac + primaryBonus
}

object Scoring {
    private fun cap(vp: Int) = vp.coerceIn(0, ApprovedOps.MAX_VP_PER_OP)

    fun killVp(player: PlayerState, opponent: PlayerState, ended: Boolean): Int {
        val bonus = if (ended && player.killGrade > opponent.killGrade) 1 else 0
        return cap(player.killGrade + bonus)
    }

    /** Scores of a player; the primary op bonus (half of that op's VP, rounded up) only counts once it was revealed. */
    fun score(game: GameState, index: Int): Score {
        val p = game.players[index]
        val o = game.players[1 - index]
        val crit = cap(p.critVp)
        val kill = killVp(p, o, game.ended)
        val tac = cap(p.tacVp)
        val bonus = if (p.primaryRevealed) {
            val vp = when (p.primary) {
                OpKind.Crit -> crit
                OpKind.Kill -> kill
                OpKind.Tac -> tac
                null -> 0
            }
            (vp + 1) / 2
        } else 0
        return Score(crit, kill, tac, bonus)
    }

    /** Index of the winner, or -1 for a draw. */
    fun winner(game: GameState): Int {
        val a = score(game, 0).total
        val b = score(game, 1).total
        return when {
            a > b -> 0
            b > a -> 1
            else -> -1
        }
    }
}

/** Initiative roll-off rules of Approved Ops 2025. */
object Initiative {
    /** The roll-off winner decides initiative; on a tie the player who currently has no initiative wins. */
    fun rollOffWinner(results: List<Int>, currentInitiative: Int): Int = when {
        results[0] > results[1] -> 0
        results[1] > results[0] -> 1
        else -> 1 - currentInitiative
    }

    /** The loser of the roll-off gains the card matching the turning point (none in the fourth). */
    fun cardForTurningPoint(turningPoint: Int): InitiativeCard? = when (turningPoint) {
        1 -> InitiativeCard.Mod1
        2 -> InitiativeCard.Mod2
        3 -> InitiativeCard.Mod3
        else -> null
    }
}

fun GameState.advanceTurningPoint(): GameState =
    if (turningPoint >= ApprovedOps.TURNING_POINTS) copy(phase = GamePhase.End, ended = true)
    else copy(
        turningPoint = turningPoint + 1,
        rolls = listOf(null, null),
        rollOffWinner = null,
        gainedCard = null,
        initiativeChosen = false,
        readyDone = false,
        players = players.map { p -> p.copy(operatives = p.operatives.map { it.copy(expended = false) }) },
    )
