package com.example.killteam.logic

import com.russhwolf.settings.Settings

/** Secret key of the access codes; set with `tools/generate-access-code.ps1 -NewKey`. An empty key switches the access check off. */
object AccessConfig {
    const val KEY = "f7e098daa04b6d32ced5d89a90895931"
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
 * Access code `XXXX-XXXX-XXXX` (60 bits, Crockford base32): 20 bit expiry in hours since 2026-01-01 UTC (0 = unlimited),
 * 12 bit nonce and a 28 bit signature (HMAC-SHA256 over expiry and nonce). Must match tools/generate-access-code.ps1.
 */
object AccessCode {
    const val EPOCH_MILLIS = 1_767_225_600_000L
    const val HOUR_MILLIS = 3_600_000L
    const val MAX_HOURS = (1 shl 20) - 1
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val LENGTH = 12

    class Parsed(val normalized: String, val expiryHours: Int, val nonce: Int) {
        val unlimited get() = expiryHours == 0
        val expiresAt get() = EPOCH_MILLIS + expiryHours * HOUR_MILLIS
    }

    fun create(key: String, expiryHours: Int, nonce: Int): String {
        require(expiryHours in 0..MAX_HOURS)
        val payload = (expiryHours.toLong() shl 12) or (nonce.toLong() and 0xFFF)
        val value = (payload shl 28) or signature(key, payload)
        val chars = CharArray(LENGTH) { i -> ALPHABET[((value ushr (55 - 5 * i)) and 31).toInt()] }
        return chars.concatToString().chunked(4).joinToString("-")
    }

    /** Upper-case, without separators; O, I and L count as 0, 1 and 1. Null if it is not 12 valid characters. */
    fun normalize(input: String): String? {
        val s = input.uppercase().filter { it != '-' && !it.isWhitespace() }
            .replace('O', '0').replace('I', '1').replace('L', '1')
        return if (s.length == LENGTH && s.all { it in ALPHABET }) s else null
    }

    /** The code's content if the signature is valid for [key], otherwise null. */
    fun parse(key: String, input: String): Parsed? {
        val s = normalize(input) ?: return null
        var value = 0L
        s.forEach { value = (value shl 5) or ALPHABET.indexOf(it).toLong() }
        val payload = value ushr 28
        if ((signature(key, payload) xor (value and 0xFFFFFFF)) != 0L) return null
        return Parsed(s, (payload ushr 12).toInt(), (payload and 0xFFF).toInt())
    }

    private fun signature(key: String, payload: Long): Long {
        val data = ByteArray(4) { i -> (payload ushr (24 - 8 * i)).toByte() }
        val h = hmacSha256(key.encodeToByteArray(), data)
        val first = h.take(4).fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xff) }
        return first ushr 4
    }
}

internal fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val block = if (key.size > 64) Sha256.digest(key) else key
    val padded = block.copyOf(64)
    val inner = Sha256.digest(ByteArray(64) { (padded[it].toInt() xor 0x36).toByte() } + data)
    return Sha256.digest(ByteArray(64) { (padded[it].toInt() xor 0x5c).toByte() } + inner)
}

/**
 * Checks the access code after installation and when the app runs. The code carries its own expiry, so it cannot be
 * used again after it ran out. A limited code also locks the app when the system clock was set back.
 */
class AccessGate(
    private val settings: Settings,
    private val key: String = AccessConfig.KEY,
    private val clock: () -> Long = ::nowMillis,
) {
    val enabled get() = key.isNotEmpty()

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
        val code = AccessCode.parse(key, input) ?: return AccessResult.Wrong
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
        val code = settings.getStringOrNull(KEY_CODE)?.let { AccessCode.parse(key, it) } ?: return State.None
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
