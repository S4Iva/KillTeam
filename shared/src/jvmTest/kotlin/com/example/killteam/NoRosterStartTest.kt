package com.example.killteam

import com.example.killteam.data.KillTeamData
import com.example.killteam.data.parseKillTeamData
import com.example.killteam.logic.Roster
import com.example.killteam.ui.startPlayer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoRosterStartTest {
    private val data: KillTeamData by lazy {
        parseKillTeamData(File("src/commonMain/composeResources/files/killteams.json").readText())
    }
    private val kasrkin get() = data.teams.first { it.slug == "kasrkin" }

    private val trooper get() = kasrkin.operatives.map { it.name }.distinct().first { it.endsWith("Trooper") }

    private fun picked(): Roster {
        val names = kasrkin.operatives.map { it.name }.distinct()
        val sergeant = names.first { it.endsWith("Sergeant") }
        return Roster(1, "", kasrkin.slug).withAdded(sergeant).withAdded(trooper).withAdded(trooper)
            .withEquipmentOn(0, "E1").withEquipmentOn(2, "E2")
    }

    @Test
    fun pickedOperativesBecomeTracksWithDatacardAndEquipment() {
        val player = startPlayer("A", kasrkin, picked(), 6)
        assertEquals(3, player.operatives.size)
        assertTrue(player.operatives.all { it.card != null && it.maxWounds > 0 })
        assertEquals(listOf("$trooper 1", "$trooper 2"), player.operatives.drop(1).map { it.name })
        assertEquals(listOf("E1"), player.operatives[0].equipment)
        assertEquals(listOf("E2"), player.operatives[2].equipment)
        assertEquals(listOf("E1", "E2"), player.equipment)
        assertTrue(player.rosterEquipment.isEmpty())
    }

    @Test
    fun anEmptyPickStartsWithoutOperatives() {
        val player = startPlayer("A", kasrkin, null, 6)
        assertTrue(player.operatives.isEmpty() && player.equipment.isEmpty())
    }

    @Test
    fun removingAPickedOperativeTakesItsEquipmentAlong() {
        val roster = picked().withRemovedAt(0)
        assertEquals(listOf("E2"), roster.equipment)
        assertEquals(2, startPlayer("A", kasrkin, roster, 6).operatives.size)
    }
}
