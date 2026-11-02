package com.example.killteam

import com.example.killteam.logic.AccessCode
import com.example.killteam.logic.AccessGate
import com.example.killteam.logic.AccessResult
import com.example.killteam.logic.AccessStatus
import com.example.killteam.logic.Ed25519
import com.example.killteam.logic.formatRemaining
import com.russhwolf.settings.MapSettings
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Creates codes like tools/generate-access-code.ps1, signing with the JDK's Ed25519. */
object TestCodes {
    // RFC 8032, test 1
    const val SEED = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60"
    const val PUBLIC_KEY = "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a"
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun create(hours: Int, nonce: Int, seed: String = SEED): String {
        val value = (hours shl 12) or nonce
        val payload = ByteArray(4) { (value ushr (24 - 8 * it)).toByte() }
        val pkcs8 = "302e020100300506032b657004220420" + seed
        val key = KeyFactory.getInstance("Ed25519").generatePrivate(
            PKCS8EncodedKeySpec(ByteArray(pkcs8.length / 2) { pkcs8.substring(it * 2, it * 2 + 2).toInt(16).toByte() }),
        )
        val signature = Signature.getInstance("Ed25519").run { initSign(key); update(payload); sign() }
        val sb = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in payload + signature) {
            buffer = (buffer shl 8) or (b.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                bits -= 5
                sb.append(ALPHABET[(buffer ushr bits) and 31])
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        if (bits > 0) sb.append(ALPHABET[(buffer shl (5 - bits)) and 31])
        return sb.toString().chunked(5).joinToString("-")
    }
}

class AccessGateTest {
    private val key = TestCodes.PUBLIC_KEY
    private val hour = AccessCode.HOUR_MILLIS
    private val epoch = AccessCode.EPOCH_MILLIS

    private fun code(hours: Int, nonce: Int = 1) = TestCodes.create(hours, nonce)
    private fun gate(settings: MapSettings, now: () -> Long) = AccessGate(settings, key, now)

    @Test
    fun jdkSignaturesMatchTheGeneratorScript() {
        // tools/generate-access-code.ps1 -Key <SEED> -Nonce 1234 -Until "2026-12-01 00:00" (8016 h)
        assertEquals(
            "07TG9-MH52S-EMT7W-PRQZT-F4HCZ-E89J6-9HXDG-8WCES-8QNFA-4XKB8-J4EP1-WF6BG-5GTZ9-VAW91-XY8N1-BWANA-JA6G0-XG1PZ-HJ6FS-EZM6C-CXVR5-SXGW",
            code(8016, 1234),
        )
        // -Nonce 1 -Unlimited
        assertEquals(
            "00000-0AXKZ-96YN8-5YTKB-ZBSSH-MK3ZP-Y5XB1-YD31B-V2T76-RJGAM-153N2-5RVG8-KYF1B-WZP7V-3A1TY-DVZW5-5GZPY-N26DB-AGZXG-51JZY-G1903-1J0C",
            code(0, 1),
        )
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
    fun randomKeysAndSignaturesAreAcceptedByTheVerifier() {
        val generator = KeyPairGenerator.getInstance("Ed25519")
        repeat(20) { i ->
            val pair = generator.generateKeyPair()
            val message = ByteArray(i * 7) { (it * 31 + i).toByte() }
            val signature = Signature.getInstance("Ed25519").run { initSign(pair.private); update(message); sign() }
            val publicKey = pair.public.encoded.takeLast(32).toByteArray()
            assertTrue(Ed25519.verify(publicKey, message, signature))
            assertFalse(Ed25519.verify(publicKey, message + byteArrayOf(1), signature))
        }
    }

    @Test
    fun aCodeSignedWithAnotherKeyIsRejected() {
        val other = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20"
        assertEquals(null, AccessCode.parse(key, TestCodes.create(100, 1, other)))
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
        assertEquals(AccessResult.Wrong, g.attempt("00000-00000"))
        assertEquals(AccessResult.Wrong, g.attempt("0".repeat(AccessCode.LENGTH)))
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
