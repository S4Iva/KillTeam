package com.example.killteam.logic

import com.russhwolf.settings.Settings

/** Ed25519 public key (hex) of the access codes; set with `tools/generate-access-code.ps1 -NewKey`. An empty key switches the access check off. */
object AccessConfig {
    const val PUBLIC_KEY = "b20f2a89d4aa157ae042761ff4c2d23422f43d8adbb12d49bbbca2186027d6de"
}

enum class AccessResult { Unlocked, Wrong, Expired, ClockChanged }

/** How the app is currently unlocked. */
sealed interface AccessStatus {
    data object Disabled : AccessStatus
    data object Unlimited : AccessStatus
    data object Locked : AccessStatus
    data class Temporary(val expiresAt: Long, val remainingMillis: Long) : AccessStatus
}

/** Remaining time as "5 d 3 h", "4 h 12 min" or "7 min". */
fun formatRemaining(millis: Long): String {
    val minutes = millis.coerceAtLeast(0) / 60_000
    val days = minutes / (24 * 60)
    val hours = minutes % (24 * 60) / 60
    val min = minutes % 60
    return when {
        days > 0 -> "$days d $hours h"
        hours > 0 -> "$hours h $min min"
        else -> "$min min"
    }
}

/**
 * Access code (Crockford base32, 109 characters, shown in groups of 5): 4 payload bytes (20 bit expiry in hours since
 * 2026-01-01 UTC, 0 = unlimited, then a 12 bit nonce) followed by the 64 byte Ed25519 signature of the payload.
 * Must match tools/generate-access-code.ps1. The app only holds the public key, so it cannot create codes.
 */
object AccessCode {
    const val EPOCH_MILLIS = 1_767_225_600_000L
    const val HOUR_MILLIS = 3_600_000L
    const val MAX_HOURS = (1 shl 20) - 1
    const val LENGTH = 109
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val PAYLOAD_BYTES = 4
    private const val BYTES = PAYLOAD_BYTES + 64

    class Parsed(val normalized: String, val expiryHours: Int, val nonce: Int) {
        val unlimited get() = expiryHours == 0
        val expiresAt get() = EPOCH_MILLIS + expiryHours * HOUR_MILLIS
    }

    /** Upper-case, without separators; O, I and L count as 0, 1 and 1. Null if it is not 109 valid characters. */
    fun normalize(input: String): String? {
        val s = input.uppercase().filter { it != '-' && !it.isWhitespace() }
            .replace('O', '0').replace('I', '1').replace('L', '1')
        return if (s.length == LENGTH && s.all { it in ALPHABET }) s else null
    }

    /** The code's content if the signature is valid for the hex [publicKey], otherwise null. */
    fun parse(publicKey: String, input: String): Parsed? {
        val s = normalize(input) ?: return null
        val bytes = decode(s) ?: return null
        val key = hexToBytes(publicKey) ?: return null
        val payload = bytes.copyOfRange(0, PAYLOAD_BYTES)
        if (!Ed25519.verify(key, payload, bytes.copyOfRange(PAYLOAD_BYTES, BYTES))) return null
        val value = payload.fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xff) }
        return Parsed(s, (value ushr 12).toInt(), (value and 0xFFF).toInt())
    }

    /** The 68 bytes behind the characters; null if the padding bit is not zero. */
    private fun decode(s: String): ByteArray? {
        val out = ByteArray(BYTES)
        var buffer = 0
        var bits = 0
        var n = 0
        for (c in s) {
            buffer = (buffer shl 5) or ALPHABET.indexOf(c)
            bits += 5
            if (bits >= 8) {
                bits -= 8
                if (n < BYTES) out[n++] = (buffer ushr bits).toByte()
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        return if (buffer == 0) out else null
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return ByteArray(hex.length / 2) { (hex.substring(it * 2, it * 2 + 2).toIntOrNull(16) ?: return null).toByte() }
    }
}

/**
 * Checks the access code after installation and when the app runs. The code carries its own expiry, so it cannot be
 * used again after it ran out. A limited code also locks the app when the system clock was set back.
 */
class AccessGate(
    private val settings: Settings,
    private val key: String = AccessConfig.PUBLIC_KEY,
    private val clock: () -> Long = ::nowMillis,
) {
    val enabled get() = key.isNotEmpty()

    // Verifying a signature is slow compared to the rest, and the stored code rarely changes.
    private var verified: Pair<String, AccessCode.Parsed?>? = null

    private fun parse(input: String): AccessCode.Parsed? {
        verified?.let { (code, parsed) -> if (code == input) return parsed }
        return AccessCode.parse(key, input).also { verified = input to it }
    }

    private sealed interface State {
        data object Disabled : State
        data object None : State
        data object Unlimited : State
        data class Valid(val expiresAt: Long) : State
        data object Expired : State
        data object ClockChanged : State
    }

    fun isUnlocked(): Boolean = evaluate().let { it is State.Disabled || it is State.Unlimited || it is State.Valid }

    /** Why a stored code no longer works (expired, clock set back), or null if there is nothing special to tell. */
    fun lockReason(): AccessResult? = when (evaluate()) {
        State.Expired -> AccessResult.Expired
        State.ClockChanged -> AccessResult.ClockChanged
        else -> null
    }

    fun status(): AccessStatus = when (val s = evaluate()) {
        State.Disabled -> AccessStatus.Disabled
        State.Unlimited -> AccessStatus.Unlimited
        is State.Valid -> AccessStatus.Temporary(s.expiresAt, (s.expiresAt - clock()).coerceAtLeast(0))
        else -> AccessStatus.Locked
    }

    /** Checks a typed code and stores it if it is valid; a new code replaces the old one. */
    fun attempt(input: String): AccessResult {
        if (!enabled) return AccessResult.Unlocked
        val code = parse(input) ?: return AccessResult.Wrong
        if (!code.unlimited) {
            val now = clock()
            if (now >= code.expiresAt) return AccessResult.Expired
            if (clockWasSetBack(now)) return AccessResult.ClockChanged
            rememberTime(now)
        }
        settings.putString(KEY_CODE, code.normalized)
        return AccessResult.Unlocked
    }

    private fun evaluate(): State {
        if (!enabled) return State.Disabled
        val code = settings.getStringOrNull(KEY_CODE)?.let { parse(it) } ?: return State.None
        if (code.unlimited) return State.Unlimited
        val now = clock()
        if (now >= code.expiresAt) return State.Expired
        if (clockWasSetBack(now)) return State.ClockChanged
        rememberTime(now)
        return State.Valid(code.expiresAt)
    }

    private fun clockWasSetBack(now: Long) = now < settings.getLong(KEY_SEEN, 0L) - CLOCK_TOLERANCE_MILLIS

    private fun rememberTime(now: Long) {
        if (now > settings.getLong(KEY_SEEN, 0L)) settings.putLong(KEY_SEEN, now)
    }

    private companion object {
        const val KEY_CODE = "access.code"
        const val KEY_SEEN = "access.seen"
        const val CLOCK_TOLERANCE_MILLIS = 5L * 60 * 1000
    }
}
