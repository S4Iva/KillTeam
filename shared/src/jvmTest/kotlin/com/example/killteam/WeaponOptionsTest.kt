package com.example.killteam

import com.example.killteam.data.AppJson
import com.example.killteam.data.KillTeamData
import com.example.killteam.data.parseKillTeamData
import com.example.killteam.logic.Roster
import com.example.killteam.logic.RosterRules
import com.example.killteam.ui.rosterToTracks
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeaponOptionsTest {
    private val data: KillTeamData by lazy {
        parseKillTeamData(File("src/commonMain/composeResources/files/killteams.json").readText())
    }
    private val kasrkin get() = data.teams.first { it.slug == "kasrkin" }

    @Test
    fun sergeantOptionsComeFromTheCompositionText() {
        val options = kasrkin.weaponOptions.getValue("Kasrkin Sergeant")
        assertEquals(4, options.size)
        val plasma = options.first { it.label.startsWith("Plasma pistol") }
        assertTrue(plasma.weapons.containsAll(listOf("Plasma pistol (standard)", "Plasma pistol (supercharge)", "Chainsword")))
        assertEquals(setOf("Bolt pistol", "Power weapon"), options.first { it.label.startsWith("Bolt pistol") }.weapons.toSet())
    }

    @Test
    fun gunnerEntriesInTheListBecomeOptions() {
        val gunner = kasrkin.weaponOptions.entries.first { it.key.contains("Gunner") }.value
        assertEquals(5, gunner.size)
        assertTrue(gunner.all { o -> o.weapons.any { it.startsWith("Gun butt") } })
    }

    @Test
    fun operativesWithoutOptionTextHaveNoOptions() {
        assertNull(kasrkin.weaponOptions["Kasrkin Combat Medic"])
    }

    @Test
    fun groupsOfAlternativesAreCombined() {
        val breacher = data.teams.first { it.slug == "imperial-navy-breacher" }
        val sergeant = breacher.weaponOptions.entries.first { it.key.contains("Sergeant-At-Arms") }.value
        // one fixed option and 2 x 2 combinations
        assertEquals(5, sergeant.size)
    }

    @Test
    fun equippedWithTextsAreRead() {
        val team = data.teams.first { it.name == "Nemesis Claw" }
        val options = team.weaponOptions
        println("nemesis claw options: " + options.mapValues { e -> e.value.map { it.label } })
        assertEquals(4, options.entries.first { it.key.endsWith("Visionary") }.value.size)
        assertEquals(3, options.entries.first { it.key.endsWith("Gunner") && !it.key.contains("Heavy") }.value.size)
        assertEquals(1, options.entries.first { it.key.contains("Heavy Gunner") }.value.size.coerceAtMost(1))
        assertEquals(2, options.entries.first { it.key.endsWith("Warrior") }.value.size)
        assertTrue(team.weaponOptionReport.unrecognized.isEmpty())
    }

    @Test
    fun parserCoversMostTeamsAndReportsTheRest() {
        data.teams.filter { it.weaponOptionReport.unrecognized.isNotEmpty() }
            .forEach { println("option lines not recognized: ${it.slug}: ${it.weaponOptionReport.unrecognized.joinToString(" | ")}") }
        val withOptions = data.teams.count { it.weaponOptions.isNotEmpty() }
        val unresolved = data.teams.filter { it.weaponOptionReport.unresolved.isNotEmpty() }
        println("teams with weapon options: $withOptions of ${data.teams.size}")
        unresolved.forEach { println("not fully mapped: ${it.slug}: ${it.weaponOptionReport.unresolved.joinToString()}") }
        assertTrue(withOptions >= 20)
        // an operative with an unmapped text keeps no options at all
        unresolved.forEach { t -> t.weaponOptionReport.unresolved.forEach { name -> assertTrue(t.weaponOptions.keys.none { it.equals(name, ignoreCase = true) }) } }
    }

    @Test
    fun oldRostersWithoutNewFieldsStillLoad() {
        val roster = AppJson.decodeFromString(Roster.serializer(), """{"id":1,"name":"r","teamSlug":"kasrkin","operatives":["A","B"]}""")
        assertEquals(listOf("A", "B"), roster.operatives)
        assertEquals("", roster.optionAt(0))
        assertTrue(roster.equipment.isEmpty())
    }

    @Test
    fun optionsStayAlignedWhenOperativesAreAddedAndRemoved() {
        var r = Roster(1, "r", "kasrkin").withAdded("Kasrkin Sergeant").withAdded("Kasrkin Trooper").withAdded("Kasrkin Trooper")
        r = r.withOption(0, "Bolt pistol; Power weapon").withOption(2, "x")
        assertEquals(listOf("Bolt pistol; Power weapon", "", "x"), r.options)
        r = r.withRemovedLast("Kasrkin Trooper")
        assertEquals(listOf("Kasrkin Sergeant", "Kasrkin Trooper"), r.operatives)
        assertEquals(listOf("Bolt pistol; Power weapon", ""), r.options)
        assertEquals(r, r.withRemovedLast("Unknown"))
    }

    @Test
    fun warningsNameMissingOptionsAndTooMuchEquipment() {
        val r = Roster(1, "r", "kasrkin", listOf("Kasrkin Sergeant", "Kasrkin Combat Medic"), equipment = List(5) { "E$it" })
        val warnings = RosterRules.warnings(kasrkin, r)
        assertTrue(warnings.any { it.startsWith("Weapon option not selected: Kasrkin Sergeant") })
        assertTrue(warnings.none { it.contains("Combat Medic") })
        assertTrue(warnings.any { it.startsWith("More than 4 equipment") })
        val chosen = r.withOption(0, kasrkin.weaponOptions.getValue("Kasrkin Sergeant").first().label)
        assertTrue(RosterRules.warnings(kasrkin, chosen).none { it.startsWith("Weapon option") })
    }

    @Test
    fun gameTracksCarryTheDatacardAndTheChosenWeapons() {
        val label = kasrkin.weaponOptions.getValue("Kasrkin Sergeant")[1].label
        val roster = Roster(1, "r", "kasrkin").withAdded("Kasrkin Sergeant").withAdded("Kasrkin Combat Medic").withOption(0, label)
        val tracks = rosterToTracks(kasrkin, roster)
        assertEquals("Kasrkin Sergeant", tracks[0].card)
        assertTrue(tracks[0].weapons.isNotEmpty())
        assertEquals("Kasrkin Combat Medic", tracks[1].card)
        assertTrue(tracks[1].weapons.isEmpty())
    }
}
