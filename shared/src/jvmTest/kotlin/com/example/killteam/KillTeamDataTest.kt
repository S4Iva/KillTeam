package com.example.killteam

import com.example.killteam.data.parseKillTeamData
import com.example.killteam.logic.RosterRules
import com.example.killteam.logic.Roster
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KillTeamDataTest {
    private val data by lazy {
        parseKillTeamData(File("src/commonMain/composeResources/files/killteams.json").readText())
    }

    @Test
    fun importedDataLoads() {
        assertTrue(data.teams.size >= 40)
        assertTrue(data.teams.all { it.name.isNotBlank() && it.operatives.isNotEmpty() })
    }

    @Test
    fun kasrkinDatacardsAreComplete() {
        val team = data.teams.first { it.slug == "kasrkin" }
        assertEquals("Kasrkin", team.name)
        val sergeant = team.operatives.first { it.name == "Kasrkin Sergeant" }
        assertEquals(9, sergeant.woundsValue)
        assertTrue(sergeant.weapons.any { it.name == "Power weapon" && it.isMelee })
        assertTrue(team.strategyPloys.isNotEmpty() && team.firefightPloys.isNotEmpty() && team.equipment.isNotEmpty())
        assertTrue(team.archetypeSet.size == 2)
    }

    @Test
    fun datacardsKeepFlavorBaseSizeAndStructuredAbilities() {
        val sergeant = data.teams.first { it.slug == "imperial-navy-breacher" }.operatives.first { it.name.startsWith("Navis Sergeant") }
        assertTrue(sergeant.flavor.startsWith("As disciplined fighters"))
        assertEquals("25mm", sergeant.baseSize)
        val ability = sergeant.blocks.first { it.name == "Command Breach" }
        assertEquals(2, ability.paras.count { it.kind == "bullet" })
        val action = data.teams.first { it.slug == "kasrkin" }.operatives.first().blocks.first { it.isAction }
        assertEquals("0AP", action.ap)
        assertTrue(action.paras.any { it.kind == "condition" })
    }

    @Test
    fun rosterSizeIsDerivedFromTheCompositionText() {
        val team = data.teams.first { it.slug == "kasrkin" }
        assertEquals(10, RosterRules.expectedSize(team))
        val roster = Roster(1, "r", team.slug, List(9) { "Kasrkin Trooper" })
        assertTrue(RosterRules.problems(team, roster).isNotEmpty())
    }
}
