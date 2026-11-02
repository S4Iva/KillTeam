package com.example.killteam.logic

import com.russhwolf.settings.Settings

/**
 * Salt and hash of the access passwords; written by tools/set-access-password.ps1 (-Temp for the temporary one).
 * SALT/HASH: password without expiry. SALT_TEMP/HASH_TEMP: password valid for 7 days after its first use.
 * An empty hash means that password does not exist; if both are empty the gate is disabled.
 */
object AccessConfig {
    const val SALT = "697a389dca15d112918e4ad2bdc07ae6"
    const val HASH = "695fe6f737e9ab4c2cb5d07aa01b6a6f23f9df948cfebf40564f009269beb8f7"
    const val SALT_TEMP = "8be221c1f9b980c531144c0f8bf9dee6"
    const val HASH_TEMP = "a9475b32b36b0073f86332f033c91d6948567ac537cadb0d42cd21ac822c47af"
}

enum class AccessResult { Unlocked, Wrong, Expired }

/**
 * Password check after installation. The unlocked state of the permanent password is stored together with its hash.
 * The temporary password starts a 7-day period on its first use (start and last seen time are stored locally);
 * the period is over after 7 days or when the system clock was set back.
 */
class AccessGate(
    private val settings: Settings,
    private val salt: String = AccessConfig.SALT,
    private val expectedHash: String = AccessConfig.HASH,
    private val tempSalt: String = AccessConfig.SALT_TEMP,
    private val tempHash: String = AccessConfig.HASH_TEMP,
    private val clock: () -> Long = ::nowMillis,
) {
    private val permanentEnabled get() = expectedHash.isNotEmpty()
    private val tempEnabled get() = tempHash.isNotEmpty()
    val enabled get() = permanentEnabled || tempEnabled

    fun isUnlocked(): Boolean {
        if (!enabled) return true
        if (permanentEnabled && settings.getStringOrNull(KEY) == expectedHash) return true
        return tempState() == TempState.Valid
    }

    /** True when the temporary password was used and its 7 days are over. */
    fun isExpired(): Boolean = tempState() == TempState.Expired

    fun tryUnlock(password: String): Boolean = attempt(password) == AccessResult.Unlocked

    fun attempt(password: String): AccessResult {
        if (!enabled) return AccessResult.Unlocked
        if (permanentEnabled && constantTimeEquals(hash(password, salt), expectedHash)) {
            settings.putString(KEY, expectedHash)
            return AccessResult.Unlocked
        }
        if (tempEnabled && constantTimeEquals(hash(password, tempSalt), tempHash)) {
            return when (tempState()) {
                TempState.Valid -> AccessResult.Unlocked
                TempState.Expired -> AccessResult.Expired
                TempState.Unused -> {
                    val now = clock()
                    settings.putString(KEY_TEMP, "$tempHash:$now")
                    settings.putLong(KEY_SEEN, now)
                    AccessResult.Unlocked
                }
            }
        }
        return AccessResult.Wrong
    }

    private enum class TempState { Unused, Valid, Expired }

    private fun tempState(): TempState {
        if (!tempEnabled) return TempState.Unused
        val stored = settings.getStringOrNull(KEY_TEMP) ?: return TempState.Unused
        val hash = stored.substringBefore(':')
        val start = stored.substringAfter(':', "").toLongOrNull()
        if (hash != tempHash || start == null) return TempState.Unused
        val now = clock()
        val seen = settings.getLong(KEY_SEEN, start)
        if (now < seen - CLOCK_TOLERANCE_MILLIS || now - start >= TEMP_DURATION_MILLIS) return TempState.Expired
        if (now > seen) settings.putLong(KEY_SEEN, now)
        return TempState.Valid
    }

    companion object {
        private const val KEY = "access"
        private const val KEY_TEMP = "access.temp"
        private const val KEY_SEEN = "access.seen"

        const val TEMP_DURATION_MILLIS = 7L * 24 * 60 * 60 * 1000
        private const val CLOCK_TOLERANCE_MILLIS = 5L * 60 * 1000

        /** Must match tools/set-access-password.ps1. */
        const val ROUNDS = 20000

        fun hash(password: String, salt: String): String {
            val saltBytes = salt.encodeToByteArray()
            var d = Sha256.digest((salt + password).encodeToByteArray())
            repeat(ROUNDS) { d = Sha256.digest(d + saltBytes) }
            return d.toHex()
        }

        private fun constantTimeEquals(a: String, b: String): Boolean {
            if (a.length != b.length) return false
            var diff = 0
            for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
            return diff == 0
        }
    }
}

internal fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

@OptIn(ExperimentalUnsignedTypes::class)
internal object Sha256 {
    private val K = uintArrayOf(
        0x428a2f98u, 0x71374491u, 0xb5c0fbcfu, 0xe9b5dba5u, 0x3956c25bu, 0x59f111f1u, 0x923f82a4u, 0xab1c5ed5u,
        0xd807aa98u, 0x12835b01u, 0x243185beu, 0x550c7dc3u, 0x72be5d74u, 0x80deb1feu, 0x9bdc06a7u, 0xc19bf174u,
        0xe49b69c1u, 0xefbe4786u, 0x0fc19dc6u, 0x240ca1ccu, 0x2de92c6fu, 0x4a7484aau, 0x5cb0a9dcu, 0x76f988dau,
        0x983e5152u, 0xa831c66du, 0xb00327c8u, 0xbf597fc7u, 0xc6e00bf3u, 0xd5a79147u, 0x06ca6351u, 0x14292967u,
        0x27b70a85u, 0x2e1b2138u, 0x4d2c6dfcu, 0x53380d13u, 0x650a7354u, 0x766a0abbu, 0x81c2c92eu, 0x92722c85u,
        0xa2bfe8a1u, 0xa81a664bu, 0xc24b8b70u, 0xc76c51a3u, 0xd192e819u, 0xd6990624u, 0xf40e3585u, 0x106aa070u,
        0x19a4c116u, 0x1e376c08u, 0x2748774cu, 0x34b0bcb5u, 0x391c0cb3u, 0x4ed8aa4au, 0x5b9cca4fu, 0x682e6ff3u,
        0x748f82eeu, 0x78a5636fu, 0x84c87814u, 0x8cc70208u, 0x90befffau, 0xa4506cebu, 0xbef9a3f7u, 0xc67178f2u,
    )

    private fun Int.rotr(n: Int) = (this ushr n) or (this shl (32 - n))

    fun digest(data: ByteArray): ByteArray {
        val bitLength = data.size.toLong() * 8
        val padded = ByteArray(((data.size + 8) / 64 + 1) * 64)
        data.copyInto(padded)
        padded[data.size] = 0x80.toByte()
        for (i in 0 until 8) padded[padded.size - 1 - i] = (bitLength ushr (8 * i)).toByte()

        val h = uintArrayOf(0x6a09e667u, 0xbb67ae85u, 0x3c6ef372u, 0xa54ff53au, 0x510e527fu, 0x9b05688cu, 0x1f83d9abu, 0x5be0cd19u)
            .map { it.toInt() }.toIntArray()
        val w = IntArray(64)
        for (off in padded.indices step 64) {
            for (t in 0 until 16) {
                val p = off + t * 4
                w[t] = ((padded[p].toInt() and 0xff) shl 24) or ((padded[p + 1].toInt() and 0xff) shl 16) or
                    ((padded[p + 2].toInt() and 0xff) shl 8) or (padded[p + 3].toInt() and 0xff)
            }
            for (t in 16 until 64) {
                val s0 = w[t - 15].rotr(7) xor w[t - 15].rotr(18) xor (w[t - 15] ushr 3)
                val s1 = w[t - 2].rotr(17) xor w[t - 2].rotr(19) xor (w[t - 2] ushr 10)
                w[t] = w[t - 16] + s0 + w[t - 7] + s1
            }
            var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
            var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
            for (t in 0 until 64) {
                val s1 = e.rotr(6) xor e.rotr(11) xor e.rotr(25)
                val ch = (e and f) xor (e.inv() and g)
                val t1 = hh + s1 + ch + K[t].toInt() + w[t]
                val s0 = a.rotr(2) xor a.rotr(13) xor a.rotr(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val t2 = s0 + maj
                hh = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2
            }
            h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh
        }
        val out = ByteArray(32)
        for (i in 0 until 8) for (j in 0 until 4) out[i * 4 + j] = (h[i] ushr (24 - 8 * j)).toByte()
        return out
    }
}
