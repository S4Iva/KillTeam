package com.example.killteam

import com.example.killteam.data.KillTeamData
import com.example.killteam.data.parseKillTeamData
import com.example.killteam.logic.Roster
import com.example.killteam.logic.RosterRules
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompositionLimitsTest {
    private val data: KillTeamData by lazy {
        parseKillTeamData(File("src/commonMain/composeResources/files/killteams.json").readText())
    }
    private fun team(slug: String) = data.teams.first { it.slug == slug }
    private fun card(slug: String, suffix: String) = team(slug).operatives.map { it.name }.distinct().first { it.endsWith(suffix) }

    @Test
    fun kasrkinAllowsOneSergeantAndEachSpecialistOnce() {
        val kasrkin = team("kasrkin")
        val sergeant = card("kasrkin", "Sergeant")
        val medic = card("kasrkin", "Medic")
        var roster = Roster(1, "r", "kasrkin").withAdded(sergeant)
        assertEquals(0, RosterRules.room(kasrkin, roster, sergeant))
        roster = roster.withAdded(medic)
        assertEquals(0, RosterRules.room(kasrkin, roster, medic))
        assertTrue(RosterRules.room(kasrkin, roster, card("kasrkin", "Demo-Trooper")) > 0)
    }

    @Test
    fun troopersAreLimitedOnlyByTheTeamSize() {
        val kasrkin = team("kasrkin")
        val trooper = kasrkin.operatives.map { it.name }.first { it.endsWith(" Trooper") }
        val roster = Roster(1, "r", "kasrkin").withAdded(card("kasrkin", "Sergeant"))
        assertEquals(9, RosterRules.room(kasrkin, roster, trooper))
    }

    @Test
    fun combinedLimitOfMarkedOperativesApplies() {
        val kasrkin = team("kasrkin")
        val gunner = card("kasrkin", "Gunner")
        val sharpshooter = card("kasrkin", "Sharpshooter")
        // Four of the starred operatives combined: gunners may not take the fifth.
        val roster = (1..3).fold(Roster(1, "r", "kasrkin")) { r, _ -> r.withAdded(gunner) }.withAdded(sharpshooter)
        assertEquals(0, RosterRules.room(kasrkin, roster, gunner))
        assertEquals(0, RosterRules.room(kasrkin, roster, sharpshooter))
    }

    @Test
    fun tooManyOfOneKindIsReportedForExistingRosters() {
        val kasrkin = team("kasrkin")
        val medic = card("kasrkin", "Medic")
        val roster = Roster(1, "r", "kasrkin").withAdded(medic).withAdded(medic)
        assertTrue(RosterRules.problems(kasrkin, roster).any { it.startsWith("Too many $medic") })
    }

    @Test
    fun fixedCountsOfNamedOperativesAreRead() {
        val canoptek = team("canoptek-circle")
        val crawler = card("canoptek-circle", "Tomb Crawler")
        assertEquals(2, canoptek.compositionLimits.perCard[crawler])
    }

    @Test
    fun everyTeamParsesAndNothingLimitsMoreThanTheTeamCouldFill() {
        data.teams.forEach { t ->
            val limits = t.compositionLimits
            limits.perCard.forEach { (card, max) -> assertTrue(max >= 1, "${t.slug}: $card max $max") }
            val fill = t.operatives.map { it.name }.distinct()
            fill.forEach { card -> limits.room(emptyMap(), card)?.let { assertTrue(it >= 0, "${t.slug}: $card") } }
        }
        println("teams with per-card limits: " + data.teams.count { it.compositionLimits.perCard.isNotEmpty() } + "/" + data.teams.size)
    }

    @Test
    fun aListHeaderStoredAsBulletStartsItsOwnList() {
        val groups = team("blades-of-khaine").compositionLimits.groups
        assertEquals(listOf(1, 7), groups.map { it.max }.sorted())
    }

    @Test
    fun limitsOnlyForOneKindOfWeaponDoNotLimitTheWholeDatacard() {
        assertNull(team("battleclade").compositionLimits.perCard[card("battleclade", "Combat Servitor")])
    }

    @Test
    fun aCardBoughtAsFixedCountAndFromTheListHasNoListTotal() {
        assertTrue(team("death-korps").compositionLimits.groups.isEmpty())
    }

    @Test
    fun eachGunnerLineOfKasrkinCanBeTakenOnce() {
        val kasrkin = team("kasrkin")
        val gunner = card("kasrkin", "Gunner")
        val options = kasrkin.weaponOptions.getValue(gunner).map { it.label }
        val roster = Roster(1, "r", "kasrkin").withAdded(gunner).withAdded(gunner).withOption(0, options[0])
        assertEquals("used by ${roster.slotLabel(0)}", RosterRules.optionBlock(kasrkin, roster, 1, options[0]))
        assertNull(RosterRules.optionBlock(kasrkin, roster, 1, options[1]))
        assertNull(RosterRules.optionBlock(kasrkin, roster, 0, options[0]))
        val twice = roster.withOption(1, options[0])
        assertTrue(RosterRules.warnings(kasrkin, twice).any { it.startsWith("Weapon option chosen twice") })
    }

    @Test
    fun gunnersOfExactionSquadMustHaveDifferentOptions() {
        assertTrue(card("exaction-squad", "Gunner") in team("exaction-squad").compositionLimits.distinctOptions)
    }

    @Test
    fun namedWeaponsAreCapped() {
        val caps = data.teams.flatMap { it.compositionLimits.weaponMax.keys }
        println("weapon caps: $caps")
        assertTrue(caps.isNotEmpty())
    }

    @Test
    fun teamsWithoutReadableLimitsStayUnlimited() {
        assertNull(team("kasrkin").compositionLimits.room(emptyMap(), "No such card"))
    }
}
