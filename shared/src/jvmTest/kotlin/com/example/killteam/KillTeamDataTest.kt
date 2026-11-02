package com.example.killteam

import com.example.killteam.data.TeamSearch
import com.example.killteam.data.SearchKind
import com.example.killteam.data.compositionLevels
import com.example.killteam.data.parseKillTeamData
import com.example.killteam.data.tacOpsFor
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

    @Test
    fun gameSizeComesFromRosterThenTeamRulesThenDefault() {
        val team = data.teams.first { it.slug == "kasrkin" }
        assertEquals(10, RosterRules.gameSize(team, null))
        assertEquals(7, RosterRules.gameSize(team, Roster(1, "r", team.slug, List(7) { "Kasrkin Trooper" })))
        assertEquals(6, RosterRules.gameSize(null, null))
    }

    @Test
    fun equipmentOptionsBelowAnOperativeAreIndentedOneLevelMore() {
        val team = data.teams.first { it.slug == "novitiates" }
        val levels = compositionLevels(team.compositionParas)
        val paras = team.compositionParas
        val militant = paras.indexOfFirst { it.text.startsWith("MILITANT") }
        assertEquals(1, levels[militant])
        assertEquals(2, levels[militant + 1])
        assertEquals(2, levels[militant + 2])
        assertEquals(1, levels[militant + 3])
        // Options directly below an arrow stay at the first level.
        assertEquals(1, levels[paras.indexOfFirst { it.text.startsWith("Plasma pistol") }])
    }

    @Test
    fun wordSearchFindsRulesAndEquipmentTexts() {
        val team = data.teams.first { it.slug == "kasrkin" }
        val equipment = team.equipment.first()
        val word = equipment.name.split(" ").first()
        assertTrue(TeamSearch.matches(team, word))
        assertTrue(TeamSearch.hits(team, equipment.name).any { it == "Equipment: ${equipment.name}" })
        val hit = TeamSearch.hitEntries(team, equipment.name).first { it.label == "Equipment: ${equipment.name}" }
        assertEquals(SearchKind.Equipment, hit.kind)
        assertEquals(0, hit.index)
        assertTrue(!TeamSearch.matches(team, "zzzxqy"))
        assertTrue(TeamSearch.matches(team, "kas"))
    }

    @Test
    fun tacOpsOfATeamFollowItsArchetypes() {
        val team = data.teams.first { it.slug == "kasrkin" }
        val ops = tacOpsFor(team)
        assertTrue(ops.isNotEmpty() && ops.all { it.archetype in team.archetypeSet })
        assertEquals(ops.size, ops.map { it.id }.toSet().size)
        assertEquals(12, tacOpsFor(null).size)
    }
}
