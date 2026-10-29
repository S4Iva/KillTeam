package com.example.killteam

import com.example.killteam.logic.AccessGate
import com.example.killteam.logic.Sha256
import com.example.killteam.logic.toHex
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessGateTest {
    private val salt = "pepper"
    private val hash = AccessGate.hash("secret", salt)

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
    fun hashMatchesTheSetPasswordScript() {
        assertEquals("caa31ca914067170a89503093e23a985c695c12469a22041814ac1a8233d4320", hash)
    }

    @Test
    fun wrongPasswordStaysLocked() {
        val settings = MapSettings()
        val gate = AccessGate(settings, salt, hash)
        assertFalse(gate.isUnlocked())
        assertFalse(gate.tryUnlock("guess"))
        assertFalse(AccessGate(settings, salt, hash).isUnlocked())
    }

    @Test
    fun correctPasswordUnlocksOnceAndIsRemembered() {
        val settings = MapSettings()
        assertTrue(AccessGate(settings, salt, hash).tryUnlock("secret"))
        assertTrue(AccessGate(settings, salt, hash).isUnlocked())
    }

    @Test
    fun changingThePasswordAsksAgain() {
        val settings = MapSettings()
        AccessGate(settings, salt, hash).tryUnlock("secret")
        assertFalse(AccessGate(settings, salt, AccessGate.hash("other", salt)).isUnlocked())
    }

    @Test
    fun emptyConfigDisablesTheGate() {
        assertTrue(AccessGate(MapSettings(), "", "").isUnlocked())
    }
}
