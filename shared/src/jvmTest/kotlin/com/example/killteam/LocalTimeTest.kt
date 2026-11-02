package com.example.killteam

import com.example.killteam.logic.AccessCode
import com.example.killteam.logic.formatLocalDateTime
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalTimeTest {
    private val original = TimeZone.getDefault()

    @AfterTest
    fun restore() = TimeZone.setDefault(original)

    @Test
    fun expiryIsShownInTheDeviceTimeZone() {
        val utcMidnight = AccessCode.EPOCH_MILLIS + 8016 * AccessCode.HOUR_MILLIS // 2026-12-01 00:00 UTC
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        assertEquals("2026-12-01 01:00", formatLocalDateTime(utcMidnight))
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        assertEquals("2026-11-30 19:00", formatLocalDateTime(utcMidnight))
        // Summer time: 2026-07-01 00:00 UTC is 02:00 in Berlin.
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        assertEquals("2026-07-01 02:00", formatLocalDateTime(AccessCode.EPOCH_MILLIS + 181 * 24 * AccessCode.HOUR_MILLIS))
    }
}
