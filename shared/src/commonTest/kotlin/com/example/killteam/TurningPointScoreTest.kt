package com.example.killteam

import com.example.killteam.data.ApprovedOps
import com.example.killteam.logic.GamePhase
import com.example.killteam.logic.GameState
import com.example.killteam.logic.MissionState
import com.example.killteam.logic.OperativeTrack
import com.example.killteam.logic.PlayerState
import com.example.killteam.logic.Scoring
import com.example.killteam.logic.TurningPointStep
import com.example.killteam.logic.advanceTurningPoint
import com.example.killteam.logic.changeTpCrit
import com.example.killteam.logic.changeTpTac
import com.example.killteam.logic.confirmFirefight
import com.example.killteam.logic.confirmStrategy
import com.example.killteam.logic.reopenFirefight
import com.example.killteam.logic.reopenStrategy
import com.example.killteam.logic.setIncapacitated
import com.example.killteam.logic.step
import com.example.killteam.logic.tpCrit
import com.example.killteam.logic.tpKill
import com.example.killteam.logic.tpTac
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurningPointScoreTest {
    private fun game() = GameState(
        id = 1, startedAt = 0, mission = MissionState(),
        players = listOf(
            PlayerState("A", operatives = listOf(OperativeTrack("a1", 8), OperativeTrack("a2", 8))),
            PlayerState("B", operatives = listOf(OperativeTrack("b1", 8), OperativeTrack("b2", 8), OperativeTrack("b3", 8))),
        ),
        phase = GamePhase.Play,
    )

    @Test
    fun theStepsFollowEachOtherAndNeedTheStrategyPhase() {
        val g = game()
        assertEquals(TurningPointStep.Strategy, g.step)
        assertEquals(g, g.confirmFirefight())
        val firefight = g.confirmStrategy()
        assertEquals(TurningPointStep.Firefight, firefight.step)
        val score = firefight.confirmFirefight()
        assertEquals(TurningPointStep.Score, score.step)
        assertEquals(TurningPointStep.Firefight, score.reopenFirefight().step)
        assertEquals(TurningPointStep.Strategy, score.reopenStrategy().step)
        assertEquals(TurningPointStep.Strategy, score.advanceTurningPoint().step)
    }

    @Test
    fun critVpOfTheTurningPointAreTheDifferenceToTheBaseline() {
        val start = game().copy(players = game().players.map { it.copy(critVp = 2, critVpBase = 2) })
        val scored = start.changeTpCrit(0, 1)
        assertEquals(1, scored.tpCrit(0))
        assertEquals(3, scored.players[0].critVp)
        assertEquals(2, scored.changeTpCrit(0, -1).changeTpCrit(0, -1).players[0].critVp)
        val next = scored.advanceTurningPoint()
        assertEquals(0, next.tpCrit(0))
        assertEquals(3, next.players[0].critVpBase)
        assertEquals(ApprovedOps.MAX_VP_PER_OP, (1..10).fold(next) { g, _ -> g.changeTpCrit(0, 1) }.players[0].critVp)
    }

    @Test
    fun killVpFollowTheIncapacitatedEnemyOperatives() {
        val g = game().setIncapacitated(1, 0, true).setIncapacitated(1, 1, true)
        assertEquals(2, g.players[0].kills)
        assertEquals(Scoring.killVp(g.players[0], g.players[1], false), g.tpKill(0))
        val next = g.advanceTurningPoint()
        assertEquals(0, next.tpKill(0))
        val more = next.setIncapacitated(1, 2, true)
        assertEquals(Scoring.killVp(more.players[0], more.players[1], false) - next.players[0].killVpBase, more.tpKill(0))
    }

    @Test
    fun tacVpAreCappedByTheTacOpLimitPerTurningPoint() {
        val op = ApprovedOps.tacOps.first { it.maxVpPerTurningPoint != null }
        val cap = op.maxVpPerTurningPoint!!
        val start = game().updatePlayer(0) { it.copy(tacOpId = op.id) }
        val scored = (1..10).fold(start) { g, _ -> g.changeTpTac(0, 1) }
        assertEquals(minOf(cap, ApprovedOps.MAX_VP_PER_OP), scored.tpTac(0))
        // The next turning point allows the limit again.
        assertTrue((1..10).fold(scored.advanceTurningPoint()) { g, _ -> g.changeTpTac(0, 1) }.tpTac(0) <= cap)
    }

    @Test
    fun oldSavedGamesHaveNoBaselineAndLoadWithZero() {
        val p = PlayerState("A")
        assertEquals(0, p.critVpBase + p.tacVpBase + p.killVpBase)
    }
}
