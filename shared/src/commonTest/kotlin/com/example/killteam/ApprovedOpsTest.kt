package com.example.killteam

import com.example.killteam.data.ApprovedOps
import com.example.killteam.data.KillOp
import com.example.killteam.data.Killzone
import com.example.killteam.logic.DamageCalc
import com.example.killteam.logic.GamePhase
import com.example.killteam.logic.GameState
import com.example.killteam.logic.Initiative
import com.example.killteam.logic.InitiativeCard
import com.example.killteam.logic.MissionGenerator
import com.example.killteam.logic.MissionState
import com.example.killteam.logic.OpKind
import com.example.killteam.logic.Pairing
import com.example.killteam.logic.PlayerState
import com.example.killteam.logic.Scoring
import com.example.killteam.logic.TPlayer
import com.example.killteam.logic.TRound
import com.example.killteam.logic.Tournament
import com.example.killteam.logic.TournamentLogic
import com.example.killteam.logic.advanceTurningPoint
import com.example.killteam.logic.formatDate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MissionGeneratorTest {
    @Test
    fun dataHasAllCardsOfApprovedOps2025() {
        assertEquals(9, ApprovedOps.critOps.size)
        assertEquals(12, ApprovedOps.tacOps.size)
        assertEquals(3, ApprovedOps.tacOps.count { it.archetype.label == "Recon" })
        assertEquals(ApprovedOps.tacOps.size, ApprovedOps.tacOps.map { it.id }.toSet().size)
    }

    @Test
    fun randomStaysInRangeAndClearsTacOp() {
        val random = Random(42)
        repeat(200) {
            val s = MissionGenerator.random(MissionState(tacOpId = "dominate"), random)
            assertTrue(s.critOpIndex in 0 until 9)
            assertTrue(s.mapIndexByKillzone.size == Killzone.entries.size && s.mapIndexByKillzone.all { it in 0..5 })
            assertEquals(null, s.tacOpId)
        }
    }

    @Test
    fun mapSelectionIsPerKillzone() {
        val s = MissionState().withKillzone(Killzone.Volkus).withMap(4).withKillzone(Killzone.Gallowdark).withMap(2)
        assertEquals(2, s.mapIndex)
        assertEquals(4, s.withKillzone(Killzone.Volkus).mapIndex)
    }

    @Test
    fun tournamentGroupsContainTheRecommendedCritOps() {
        repeat(30) {
            assertTrue(MissionGenerator.randomFromGroup(1) in listOf(3, 5, 6))
        }
    }
}

class KillGradeTest {
    @Test
    fun gradesFollowTheTable() {
        assertEquals(0, KillOp.grade(10, 1))
        assertEquals(1, KillOp.grade(10, 2))
        assertEquals(3, KillOp.grade(10, 6))
        assertEquals(5, KillOp.grade(10, 10))
        assertEquals(5, KillOp.grade(5, 5))
        assertEquals(2, KillOp.grade(8, 3))
    }

    @Test
    fun startCountsOutsideTheTableAreClamped() {
        assertEquals(KillOp.thresholds(5), KillOp.thresholds(3))
        assertEquals(KillOp.thresholds(14), KillOp.thresholds(20))
    }
}

class ScoringTest {
    private fun game(a: PlayerState, b: PlayerState, ended: Boolean = false) = GameState(
        id = 1, startedAt = 0, mission = MissionState(), players = listOf(a, b), phase = GamePhase.Play, ended = ended,
    )

    @Test
    fun eachOpIsCappedAtSixVp() {
        val g = game(PlayerState("A", critVp = 9, tacVp = 7), PlayerState("B"))
        val s = Scoring.score(g, 0)
        assertEquals(6, s.crit)
        assertEquals(6, s.tac)
    }

    @Test
    fun primaryOpBonusIsHalfRoundedUpAndNeedsReveal() {
        val hidden = game(PlayerState("A", critVp = 5, primary = OpKind.Crit), PlayerState("B"))
        assertEquals(0, Scoring.score(hidden, 0).primaryBonus)
        val shown = game(PlayerState("A", critVp = 5, primary = OpKind.Crit, primaryRevealed = true), PlayerState("B"))
        assertEquals(3, Scoring.score(shown, 0).primaryBonus)
        assertEquals(8, Scoring.score(shown, 0).total)
    }

    @Test
    fun higherKillGradeScoresAnExtraPointAtTheEnd() {
        val a = PlayerState("A", enemyStartCount = 10, kills = 6)
        val b = PlayerState("B", enemyStartCount = 10, kills = 2)
        assertEquals(3, Scoring.score(game(a, b), 0).kill)
        assertEquals(4, Scoring.score(game(a, b, ended = true), 0).kill)
        assertEquals(1, Scoring.score(game(a, b, ended = true), 1).kill)
    }

    @Test
    fun winnerAndDraw() {
        assertEquals(0, Scoring.winner(game(PlayerState("A", critVp = 3), PlayerState("B", critVp = 2))))
        assertEquals(-1, Scoring.winner(game(PlayerState("A", critVp = 2), PlayerState("B", critVp = 2))))
    }

    @Test
    fun fourthTurningPointEndsTheBattle() {
        var g = game(PlayerState("A"), PlayerState("B"))
        repeat(3) { g = g.advanceTurningPoint() }
        assertEquals(4, g.turningPoint)
        assertEquals(GamePhase.Play, g.phase)
        g = g.advanceTurningPoint()
        assertEquals(GamePhase.End, g.phase)
    }
}

class InitiativeTest {
    @Test
    fun highestRollWins() {
        assertEquals(0, Initiative.rollOffWinner(listOf(5, 3), currentInitiative = 1))
        assertEquals(1, Initiative.rollOffWinner(listOf(2, 7), currentInitiative = 0))
    }

    @Test
    fun tieGoesToThePlayerWithoutInitiative() {
        assertEquals(1, Initiative.rollOffWinner(listOf(4, 4), currentInitiative = 0))
        assertEquals(0, Initiative.rollOffWinner(listOf(4, 4), currentInitiative = 1))
    }

    @Test
    fun cardsMatchTheTurningPoint() {
        assertEquals(InitiativeCard.Mod1, Initiative.cardForTurningPoint(1))
        assertEquals(InitiativeCard.Mod3, Initiative.cardForTurningPoint(3))
        assertEquals(null, Initiative.cardForTurningPoint(4))
    }
}

class TournamentTest {
    private fun players(n: Int) = (1..n).map { TPlayer(it, "P$it") }

    @Test
    fun firstRoundPairsEveryoneAndGivesAByeForOddCounts() {
        val t = Tournament(1, "T", players(5))
        val pairings = TournamentLogic.nextPairings(t, Random(1))
        assertEquals(3, pairings.size)
        assertEquals(1, pairings.count { it.b == null })
        val ids = pairings.flatMap { listOfNotNull(it.a, it.b) }
        assertEquals(5, ids.toSet().size)
    }

    @Test
    fun rankingUsesRecordThenStrengthOfSchedule() {
        val t = Tournament(
            1, "T", players(4),
            listOf(
                TRound(0, listOf(Pairing(1, 2, 10, 5), Pairing(3, 4, 7, 7))),
                TRound(0, listOf(Pairing(1, 3, 9, 4), Pairing(2, 4, 8, 2))),
            ),
        )
        val ranking = TournamentLogic.ranking(t)
        assertEquals(listOf(1, 2, 3, 4), ranking.map { it.player.id })
        assertEquals("WW", ranking[0].path)
        assertEquals("D", ranking[2].path.take(1))
        assertEquals(4, ranking[0].points)
    }

    @Test
    fun twoDrawsEqualAWinAndALoss() {
        val t = Tournament(
            1, "T", players(3),
            listOf(
                TRound(0, listOf(Pairing(1, 2, 5, 5), Pairing(3, null))),
                TRound(0, listOf(Pairing(1, 3, 4, 4), Pairing(2, null))),
            ),
        )
        val p1 = TournamentLogic.standings(t).first { it.player.id == 1 }
        assertEquals(2, p1.points)
    }

    @Test
    fun laterRoundsAvoidRematchesWhenPossible() {
        val t = Tournament(
            1, "T", players(4),
            listOf(TRound(0, listOf(Pairing(1, 2, 8, 3), Pairing(3, 4, 6, 2)))),
        )
        repeat(20) { seed ->
            val pairings = TournamentLogic.nextPairings(t, Random(seed))
            val sets = pairings.map { setOf(it.a, it.b) }
            assertTrue(setOf(1, 2) !in sets && setOf(3, 4) !in sets)
        }
    }
}

class DamageCalcTest {
    @Test
    fun probabilitiesSumToOne() {
        val r = DamageCalc.calculate(4, 3, 3, 4, 3, 4)
        assertTrue(kotlin.math.abs(r.damageDistribution.values.sum() - 1.0) < 1e-9)
    }

    @Test
    fun noDefenceDiceMeansAllHitsLand() {
        val r = DamageCalc.calculate(1, 4, 3, 4, 0, 4)
        // One die: crit 1/6 (4 dmg), normal 2/6 (3 dmg)
        assertTrue(kotlin.math.abs(r.expectedDamage - (4.0 / 6 + 3.0 * 2 / 6)) < 1e-9)
    }

    @Test
    fun betterSaveReducesDamage() {
        val weak = DamageCalc.calculate(4, 3, 3, 4, 3, 5).expectedDamage
        val strong = DamageCalc.calculate(4, 3, 3, 4, 3, 3).expectedDamage
        assertTrue(strong < weak)
    }
}

class DateTest {
    @Test
    fun formatsEpochDates() {
        assertEquals("1970-01-01", formatDate(0))
        assertEquals("2000-03-01", formatDate(951_868_800_000L))
        assertEquals("2024-02-29", formatDate(1_709_164_800_000L))
    }
}
