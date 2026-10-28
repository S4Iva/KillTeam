package com.example.killteam.logic

import com.example.killteam.data.ApprovedOps
import com.example.killteam.data.Killzone
import com.example.killteam.data.MAPS_PER_KILLZONE
import kotlinx.serialization.Serializable
import kotlin.random.Random

/** State of the mission generator (mirrors the Wahapedia generator: one crit op, one map per killzone, max. one tac op). */
@Serializable
data class MissionState(
    val critOpIndex: Int = 0,
    val killzone: Killzone = Killzone.NonSpecific,
    val mapIndexByKillzone: List<Int> = List(Killzone.entries.size) { 0 },
    val tacOpId: String? = null,
) {
    val mapIndex: Int get() = mapIndexByKillzone.getOrElse(killzone.ordinal) { 0 }
    val critOp get() = ApprovedOps.critOps[critOpIndex]
    val tacOp get() = tacOpId?.let { id -> ApprovedOps.tacOps.firstOrNull { it.id == id } }

    fun withKillzone(kz: Killzone) = copy(killzone = kz)
    fun withCritOp(index: Int) = copy(critOpIndex = index.coerceIn(0, ApprovedOps.critOps.lastIndex))
    fun withMap(index: Int) = copy(
        mapIndexByKillzone = mapIndexByKillzone.toMutableList().also {
            it[killzone.ordinal] = index.coerceIn(0, MAPS_PER_KILLZONE - 1)
        },
    )

    /** The generator allows at most one selected tac op. */
    fun withTacOp(id: String?) = copy(tacOpId = id)
}

object MissionGenerator {
    /** RANDOM method: random crit op and a random map for every killzone; the tac op selection is reset. */
    fun random(current: MissionState, random: Random = Random.Default): MissionState = current.copy(
        critOpIndex = random.nextInt(ApprovedOps.critOps.size),
        mapIndexByKillzone = List(Killzone.entries.size) { random.nextInt(MAPS_PER_KILLZONE) },
        tacOpId = null,
    )

    /** Random crit op taken from the given tournament group (see Tournament Companion). */
    fun randomFromGroup(group: Int, random: Random = Random.Default): Int =
        ApprovedOps.critOpGroups[group.coerceIn(0, ApprovedOps.critOpGroups.lastIndex)].random(random)
}
