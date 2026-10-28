package com.example.killteam.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.killteam.data.AppJson
import com.russhwolf.settings.Settings
import kotlinx.serialization.KSerializer

private const val CHUNK = 4000

/** Stores long strings in chunks: java.util.prefs (desktop) rejects values above 8192 chars. */
private fun Settings.putLong(key: String, value: String) {
    val parts = value.chunked(CHUNK)
    putInt("$key#n", parts.size)
    parts.forEachIndexed { i, p -> putString("$key#$i", p) }
}

private fun Settings.getLong(key: String): String? {
    val n = getIntOrNull("$key#n") ?: return null
    return buildString { for (i in 0 until n) append(getStringOrNull("$key#$i") ?: return null) }
}

/**
 * A value stored as JSON together with a schema version.
 * Data from an older version goes through [migrate]; data that cannot be read is kept in a backup entry
 * (`<key>#backup`) and never overwritten silently. Data written before versioning counts as version 1.
 */
class Persisted<T>(
    private val settings: Settings,
    private val key: String,
    private val serializer: KSerializer<T>,
    private val default: T,
    private val version: Int = 1,
    private val migrate: (fromVersion: Int, json: String) -> String = { _, json -> json },
) {
    var value: T by mutableStateOf(load())
        private set

    val hasBackup: Boolean get() = settings.getLong("$key#backup") != null

    fun update(new: T) {
        value = new
        write(AppJson.encodeToString(serializer, new))
    }

    private fun write(json: String) {
        settings.putLong(key, json)
        settings.putInt("$key#v", version)
    }

    private fun load(): T {
        val raw = settings.getLong(key) ?: return default
        val stored = settings.getInt("$key#v", 1)
        return try {
            val json = if (stored < version) migrate(stored, raw) else raw
            val decoded = AppJson.decodeFromString(serializer, json)
            if (stored < version) write(json)
            decoded
        } catch (e: Exception) {
            if (!hasBackup) {
                settings.putLong("$key#backup", raw)
                settings.putInt("$key#backup#v", stored)
            }
            default
        }
    }
}
