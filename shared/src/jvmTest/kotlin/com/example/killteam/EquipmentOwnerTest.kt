package com.example.killteam

import com.example.killteam.logic.Roster
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EquipmentOwnerTest {
    private val roster = Roster(1, "r", "x", operatives = listOf("A", "B", "B"), options = listOf("o1", "o2", "o3"))

    @Test
    fun equipmentJoinsTheRosterOnTheOperativeItIsGivenTo() {
        val r = roster.withEquipmentOn(1, "E1")
        assertEquals(listOf("E1"), r.equipment)
        assertEquals(1, r.ownerOf("E1"))
        assertEquals(listOf("E1"), r.equipmentOf(1))
    }

    @Test
    fun givingItToAnotherOperativeMovesIt() {
        val r = roster.withEquipmentOn(0, "E1").withEquipmentOn(2, "E1")
        assertEquals(listOf("E1"), r.equipment)
        assertEquals(2, r.ownerOf("E1"))
        assertEquals(emptyList(), r.equipmentOf(0))
    }

    @Test
    fun noMoreThanFourEquipmentEntries() {
        val r = listOf("E1", "E2", "E3", "E4", "E5").fold(roster) { acc, e -> acc.withEquipmentOn(0, e) }
        assertEquals(listOf("E1", "E2", "E3", "E4"), r.equipment)
        assertNull(r.withEquipmentOn(1, "E5").ownerOf("E5"))
        assertEquals(1, r.withEquipmentOn(1, "E2").ownerOf("E2"))
    }

    @Test
    fun removingAnOperativeDropsItsEquipmentAndShiftsTheOthers() {
        val r = roster.withEquipmentOn(0, "E1").withEquipmentOn(2, "E2").withRemovedAt(0)
        assertEquals(listOf("E2"), r.equipment)
        assertEquals(1, r.ownerOf("E2"))
        assertEquals(listOf("o2", "o3"), r.options)
        assertNull(r.ownerOf("E1"))
    }

    @Test
    fun unassignedEquipmentFromOldRostersIsListed() {
        val old = roster.copy(equipment = listOf("E1", "E2")).withEquipmentOn(1, "E1")
        assertEquals(listOf("E2"), old.unassignedEquipment)
    }
}
