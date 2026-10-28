package com.example.killteam

import com.example.killteam.logic.GamePhase
import com.example.killteam.logic.GameState
import com.example.killteam.logic.InitiativeCard
import com.example.killteam.logic.MissionState
import com.example.killteam.logic.OpKind
import com.example.killteam.logic.OperativeTrack
import com.example.killteam.logic.PlayerState
import com.example.killteam.logic.Scoring
import com.example.killteam.logic.advanceTurningPoint
import com.example.killteam.logic.chooseInitiative
import com.example.killteam.logic.chooseSetupInitiative
import com.example.killteam.logic.finish
import com.example.killteam.logic.readyGains
import com.example.killteam.logic.readyStep
import com.example.killteam.logic.reopenRollOff
import com.example.killteam.logic.resolveRollOff
import com.example.killteam.logic.setIncapacitated
import com.example.killteam.logic.toResult
import com.example.killteam.logic.useModifier
import com.example.killteam.logic.useReroll
import com.example.killteam.logic.withRoll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameFlowTest {
    private fun game(turningPoint: Int = 1, initiative: Int = 0) = GameState(
        id = 1, startedAt = 0, mission = MissionState(),
        players = listOf(
            PlayerState("A", operatives = listOf(OperativeTrack("a1", 8, expended = true), OperativeTrack("a2", 8))),
            PlayerState("B", operatives = listOf(OperativeTrack("b1", 8, expended = true))),
        ),
        phase = GamePhase.Play, turningPoint = turningPoint, initiative = initiative,
    )

    @Test
    fun theLoserGainsTheTurningPointCardExactlyOnce() {
        val rolled = game(turningPoint = 2).withRoll(0, 5).withRoll(1, 2)
        val resolved = rolled.resolveRollOff()
        assertEquals(0, resolved.rollOffWinner)
        assertEquals(listOf(InitiativeCard.Mod2), resolved.players[1].cards)
        // Resolving again, or changing the rolls afterwards, must not grant another card.
        val again = resolved.resolveRollOff().withRoll(1, 6).withRoll(0, 1).resolveRollOff()
        assertEquals(listOf(InitiativeCard.Mod2), again.players[1].cards)
        assertEquals(5, again.rolls[0])
        assertEquals(2, again.rolls[1])
    }

    @Test
    fun noCardIsGainedInTheFourthTurningPoint() {
        val resolved = game(turningPoint = 4).withRoll(0, 3).withRoll(1, 4).resolveRollOff()
        assertTrue(resolved.players.all { it.cards.isEmpty() })
        assertNull(resolved.gainedCard)
    }

    @Test
    fun aTieGoesToThePlayerWithoutInitiative() {
        val resolved = game(initiative = 0).withRoll(0, 4).withRoll(1, 4).resolveRollOff()
        assertEquals(1, resolved.rollOffWinner)
        assertEquals(listOf(InitiativeCard.Mod1), resolved.players[0].cards)
    }

    @Test
    fun cardsModifyOrRerollAndAreSpent() {
        val start = game().chooseSetupInitiative(0).withRoll(1, 5)
        assertEquals(listOf(InitiativeCard.Reroll), start.players[1].cards)
        val rerolled = start.useReroll(1)
        assertNull(rerolled.rolls[1])
        assertTrue(rerolled.players[1].cards.isEmpty())
        assertEquals(rerolled, rerolled.useReroll(1))

        val withMod = game().copy(players = game().players.map { it.copy(cards = listOf(InitiativeCard.Mod2)) }).withRoll(0, 5)
        assertEquals(7, withMod.useModifier(0, InitiativeCard.Mod2, up = true).rolls[0])
        assertEquals(3, withMod.useModifier(0, InitiativeCard.Mod2, up = false).rolls[0])
        assertEquals(withMod, withMod.useModifier(1, InitiativeCard.Mod2, up = true)) // no roll yet
    }

    @Test
    fun reopeningTakesTheGainedCardBack() {
        val resolved = game().withRoll(0, 6).withRoll(1, 1).resolveRollOff().chooseInitiative(1)
        val reopened = resolved.reopenRollOff()
        assertNull(reopened.rollOffWinner)
        assertTrue(reopened.players[1].cards.isEmpty())
        assertEquals(0, reopened.initiative)
        assertEquals(reopened.resolveRollOff().players[1].cards, listOf(InitiativeCard.Mod1))
    }

    @Test
    fun readyStepGivesCpOncePerTurningPoint() {
        val first = game(turningPoint = 1).withRoll(0, 6).withRoll(1, 1).resolveRollOff()
        assertEquals(first, first.readyStep()) // initiative not chosen yet
        val ready = first.chooseInitiative(0).readyStep()
        assertEquals(listOf(3, 3), ready.players.map { it.cp })
        assertEquals(ready, ready.readyStep())
        assertTrue(ready.players.flatMap { it.operatives }.none { it.expended })
    }

    @Test
    fun theOpponentOfTheInitiativePlayerGainsTwoCpAfterTheFirstTurningPoint() {
        val g = game(turningPoint = 2, initiative = 0)
        assertEquals(listOf(1, 2), g.readyGains())
        val ready = g.withRoll(0, 6).withRoll(1, 1).resolveRollOff().chooseInitiative(1).readyStep()
        assertEquals(listOf(4, 3), ready.players.map { it.cp })
    }

    @Test
    fun cannotChangeInitiativeAfterTheReadyStep() {
        val ready = game().withRoll(0, 6).withRoll(1, 1).resolveRollOff().chooseInitiative(0).readyStep()
        assertEquals(ready, ready.chooseInitiative(1))
        assertEquals(ready, ready.reopenRollOff())
    }

    @Test
    fun advancingStartsAFreshTurningPoint() {
        val done = game().withRoll(0, 6).withRoll(1, 1).resolveRollOff().chooseInitiative(0).readyStep().advanceTurningPoint()
        assertEquals(2, done.turningPoint)
        assertEquals(listOf(null, null), done.rolls)
        assertNull(done.rollOffWinner)
        assertTrue(!done.initiativeChosen && !done.readyDone)
        assertEquals(listOf(InitiativeCard.Mod1), done.players[1].cards) // cards are kept
    }

    @Test
    fun incapacitatingIsIdempotentAndFeedsTheKillOp() {
        val g = game()
        val out = g.setIncapacitated(0, 0, true)
        assertEquals(1, out.players[1].kills)
        assertEquals(0, out.players[0].operatives[0].wounds)
        assertEquals(out, out.setIncapacitated(0, 0, true))
        val back = out.setIncapacitated(0, 0, false)
        assertEquals(0, back.players[1].kills)
        assertEquals(1, back.players[0].operatives[0].wounds)
        assertEquals(g.players[1].kills, back.setIncapacitated(0, 0, false).players[1].kills)
    }

    @Test
    fun finishingRevealsThePrimaryOpSoTheBonusIsNeverLost() {
        val g = game().copy(
            phase = GamePhase.End,
            players = listOf(
                PlayerState("A", critVp = 5, primary = OpKind.Crit),
                PlayerState("B", critVp = 6),
            ),
        )
        assertEquals(0, Scoring.score(g, 0).primaryBonus)
        val finished = g.finish()
        assertEquals(3, Scoring.score(finished, 0).primaryBonus)
        assertEquals(0, Scoring.winner(finished))
        assertEquals(8, finished.toResult().players[0].total)
        assertEquals(0, finished.toResult().winner)
    }
}
