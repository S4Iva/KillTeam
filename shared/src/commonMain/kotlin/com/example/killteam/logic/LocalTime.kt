package com.example.killteam.logic

/** Offset of the device's time zone from UTC at the given instant (includes daylight saving time). */
expect fun localOffsetMillis(epochMillis: Long): Long

/** yyyy-MM-dd HH:mm in the device's time zone. */
fun formatLocalDateTime(epochMillis: Long): String {
    val local = epochMillis + localOffsetMillis(epochMillis)
    val minutes = local.mod(86_400_000L) / 60_000
    return "${formatDate(local)} ${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"
}
