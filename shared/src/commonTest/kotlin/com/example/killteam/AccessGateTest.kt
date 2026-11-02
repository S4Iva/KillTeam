package com.example.killteam

import com.example.killteam.logic.AccessCode
import com.example.killteam.logic.AccessGate
import com.example.killteam.logic.AccessResult
import com.example.killteam.logic.AccessStatus
import com.example.killteam.logic.Sha256
import com.example.killteam.logic.formatRemaining
import com.example.killteam.logic.hmacSha256
import com.example.killteam.logic.toHex
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccessGateTest {
    private val key = "test-key"
    private val hour = AccessCode.HOUR_MILLIS
    private val epoch = AccessCode.EPOCH_MILLIS

    private fun code(hours: Int, nonce: Int = 1) = AccessCode.create(key, hours, nonce)
    private fun gate(settings: MapSettings, now: () -> Long) = AccessGate(settings, key, now)

    @Test
    fun sha256MatchesTheKnownVectors() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Sha256.digest("abc".encodeToByteArray()).toHex())
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Sha256.digest(ByteArray(0)).toHex())
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            Sha256.digest("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()).toHex(),
        )
    }

    @Test
    fun hmacMatchesRfc4231() {
        assertEquals(
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
            hmacSha256("Jefe".encodeToByteArray(), "what do ya want for nothing?".encodeToByteArray()).toHex(),
        )
    }

    @Test
    fun codesMatchTheGeneratorScript() {
        // tools/generate-access-code.ps1 -Key test-key -Nonce 1234 for an expiry of 2026-12-01 00:00 UTC (8016 h)
        assertEquals("07TG-9MGR-F5EP", code(8016, 1234))
        // -Unlimited -Key test-key -Nonce 1
        assertEquals("0000-00F4-QZ5J", code(0, 1))
        // -Nonce 5 for an expiry of 2026-12-25 00:00 UTC (8592 h)
        assertEquals("08CG-01F2-NNXB", code(8592, 5))
    }

    @Test
    fun parseReadsExpiryAndToleratesTypingVariants() {
        val parsed = assertNotNull(AccessCode.parse(key, "07tg 9mgr-f5ep"))
        assertEquals(8016, parsed.expiryHours)
        assertEquals(1234, parsed.nonce)
        assertEquals(epoch + 8016 * hour, parsed.expiresAt)
        assertFalse(parsed.unlimited)
        assertTrue(assertNotNull(AccessCode.parse(key, "0000-00F4-QZ5J")).unlimited)
        // O, I and L are read as 0, 1 and 1.
        assertEquals(parsed.normalized, AccessCode.parse(key, "O7TG-9MGR-F5EP")?.normalized)
    }

    @Test
    fun tamperedOrForeignCodesAreRejected() {
        assertNull(AccessCode.parse(key, "07TG-9MGR-F5EQ"))
        assertNull(AccessCode.parse(key, "08TG-9MGR-F5EP"))
        assertNull(AccessCode.parse("other-key", "07TG-9MGR-F5EP"))
        assertNull(AccessCode.parse(key, "07TG-9MGR"))
        assertNull(AccessCode.parse(key, ""))
        assertNull(AccessCode.parse(key, "UUUU-UUUU-UUUU"))
    }

    @Test
    fun everyCreatedCodeRoundTrips() {
        for (hours in listOf(0, 1, 168, 8016, AccessCode.MAX_HOURS)) for (nonce in listOf(0, 1, 4095)) {
            val parsed = assertNotNull(AccessCode.parse(key, code(hours, nonce)))
            assertEquals(hours, parsed.expiryHours)
            assertEquals(nonce, parsed.nonce)
        }
    }

    @Test
    fun withoutAKeyTheGateIsOff() {
        val gate = AccessGate(MapSettings(), "") { 0L }
        assertTrue(gate.isUnlocked())
        assertEquals(AccessStatus.Disabled, gate.status())
        assertEquals(AccessResult.Unlocked, gate.attempt("anything"))
    }

    @Test
    fun noOrWrongCodeStaysLocked() {
        val settings = MapSettings()
        val g = gate(settings) { epoch }
        assertFalse(g.isUnlocked())
        assertEquals(AccessStatus.Locked, g.status())
        assertEquals(AccessResult.Wrong, g.attempt("0000-0000-0000"))
        assertFalse(gate(settings) { epoch }.isUnlocked())
    }

    @Test
    fun unlimitedCodeUnlocksForGood() {
        val settings = MapSettings()
        assertEquals(AccessResult.Unlocked, gate(settings) { epoch }.attempt(code(0)))
        val later = gate(settings) { epoch + 5000 * 24 * hour }
        assertTrue(later.isUnlocked())
        assertEquals(AccessStatus.Unlimited, later.status())
    }

    @Test
    fun limitedCodeRunsUntilItsExpiry() {
        var now = epoch + 100 * hour
        val settings = MapSettings()
        assertEquals(AccessResult.Unlocked, gate(settings) { now }.attempt(code(172)))
        now = epoch + 171 * hour + 30 * 60 * 1000
        val status = gate(settings) { now }.status() as AccessStatus.Temporary
        assertEquals(epoch + 172 * hour, status.expiresAt)
        assertEquals(30 * 60 * 1000L, status.remainingMillis)
        now = epoch + 172 * hour
        val expired = gate(settings) { now }
        assertFalse(expired.isUnlocked())
        assertEquals(AccessResult.Expired, expired.lockReason())
    }

    @Test
    fun anExpiredCodeCannotBeEnteredAgain() {
        val now = epoch + 200 * hour
        val settings = MapSettings()
        assertEquals(AccessResult.Expired, gate(settings) { now }.attempt(code(172)))
        assertFalse(gate(settings) { now }.isUnlocked())
    }

    @Test
    fun aNewCodeReplacesTheOldOne() {
        var now = epoch + 10 * hour
        val settings = MapSettings()
        gate(settings) { now }.attempt(code(20, 1))
        now = epoch + 25 * hour
        assertFalse(gate(settings) { now }.isUnlocked())
        assertEquals(AccessResult.Unlocked, gate(settings) { now }.attempt(code(100, 2)))
        assertTrue(gate(settings) { now }.isUnlocked())
    }

    @Test
    fun settingTheClockBackLocksALimitedCode() {
        var now = epoch + 10 * hour
        val settings = MapSettings()
        gate(settings) { now }.attempt(code(500))
        now = epoch + 100 * hour
        assertTrue(gate(settings) { now }.isUnlocked())
        now = epoch + 50 * hour
        val g = gate(settings) { now }
        assertFalse(g.isUnlocked())
        assertEquals(AccessResult.ClockChanged, g.lockReason())
        // A few minutes of clock drift are tolerated.
        now = epoch + 100 * hour - 3 * 60 * 1000
        assertTrue(gate(settings) { now }.isUnlocked())
    }

    @Test
    fun remainingTimeIsFormattedCompactly() {
        assertEquals("6 d 23 h", formatRemaining(7 * 24 * hour - 30 * 60 * 1000))
        assertEquals("4 h 12 min", formatRemaining((4 * 60 + 12) * 60 * 1000L))
        assertEquals("7 min", formatRemaining(7 * 60 * 1000L))
        assertEquals("0 min", formatRemaining(-5))
    }
}
