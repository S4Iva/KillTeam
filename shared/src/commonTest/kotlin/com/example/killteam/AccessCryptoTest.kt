package com.example.killteam

import com.example.killteam.logic.AccessCode
import com.example.killteam.logic.Ed25519
import com.example.killteam.logic.Sha512
import com.example.killteam.logic.toHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccessCryptoTest {
    // Private key 9d61b19d...7f60 (RFC 8032, test 1); the codes come from tools/generate-access-code.ps1 -Key ... -Nonce ...
    private val publicKey = "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a"
    private val codeDec2026 = "07TG9-MH52S-EMT7W-PRQZT-F4HCZ-E89J6-9HXDG-8WCES-8QNFA-4XKB8-J4EP1-WF6BG-5GTZ9-VAW91-XY8N1-BWANA-JA6G0-XG1PZ-HJ6FS-EZM6C-CXVR5-SXGW"
    private val codeUnlimited = "00000-0AXKZ-96YN8-5YTKB-ZBSSH-MK3ZP-Y5XB1-YD31B-V2T76-RJGAM-153N2-5RVG8-KYF1B-WZP7V-3A1TY-DVZW5-5GZPY-N26DB-AGZXG-51JZY-G1903-1J0C"

    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    @Test
    fun sha512MatchesTheKnownVectors() {
        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            Sha512.digest("abc".encodeToByteArray()).toHex(),
        )
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            Sha512.digest(ByteArray(0)).toHex(),
        )
        // Two blocks, and lengths around the padding boundary.
        assertEquals(
            "8e959b75dae313da8cf4f72814fc143f8f7779c6eb9f7fa17299aeadb6889018501d289e4900f7e4331b99dec4b5433ac7d329eeb6dd26545e96e55b874be909",
            Sha512.digest(
                ("abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu").encodeToByteArray(),
            ).toHex(),
        )
        for (n in listOf(111, 112, 127, 128, 129)) assertEquals(64, Sha512.digest(ByteArray(n)).size)
    }

    @Test
    fun ed25519MatchesRfc8032() {
        val signature = hex(
            "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e06522490155" +
                "5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
        )
        assertTrue(Ed25519.verify(hex(publicKey), ByteArray(0), signature))
        assertFalse(Ed25519.verify(hex(publicKey), byteArrayOf(0), signature))
        assertFalse(Ed25519.verify(hex(publicKey), ByteArray(0), signature.also { it[10] = (it[10] + 1).toByte() }))
    }

    @Test
    fun codesFromTheGeneratorScriptAreAccepted() {
        val dec = assertNotNull(AccessCode.parse(publicKey, codeDec2026))
        assertEquals(8016, dec.expiryHours) // 2026-12-01 00:00 UTC
        assertEquals(1234, dec.nonce)
        assertEquals(AccessCode.EPOCH_MILLIS + 8016 * AccessCode.HOUR_MILLIS, dec.expiresAt)
        assertFalse(dec.unlimited)
        val unlimited = assertNotNull(AccessCode.parse(publicKey, codeUnlimited))
        assertTrue(unlimited.unlimited)
        assertEquals(1, unlimited.nonce)
    }

    @Test
    fun typingVariantsAreTolerated() {
        val expected = assertNotNull(AccessCode.parse(publicKey, codeDec2026)).normalized
        assertEquals(expected, AccessCode.parse(publicKey, codeDec2026.lowercase())?.normalized)
        assertEquals(expected, AccessCode.parse(publicKey, codeDec2026.replace("-", " "))?.normalized)
        assertEquals(expected, AccessCode.parse(publicKey, codeDec2026.replace('0', 'O').replace('1', 'l'))?.normalized)
    }

    @Test
    fun tamperedOrForeignCodesAreRejected() {
        fun swap(code: String, at: Int): String {
            val chars = code.toCharArray()
            chars[at] = if (chars[at] == 'A') 'B' else 'A'
            return chars.concatToString()
        }
        assertNull(AccessCode.parse(publicKey, swap(codeDec2026, 1))) // expiry
        assertNull(AccessCode.parse(publicKey, swap(codeDec2026, 50))) // signature
        assertNull(AccessCode.parse(publicKey, codeDec2026.dropLast(1) + "X")) // padding bit
        assertNull(AccessCode.parse("00".repeat(32), codeDec2026))
        assertNull(AccessCode.parse("", codeDec2026))
        assertNull(AccessCode.parse(publicKey, codeDec2026.dropLast(1)))
        assertNull(AccessCode.parse(publicKey, ""))
        assertNull(AccessCode.parse(publicKey, "U".repeat(AccessCode.LENGTH)))
    }
}
