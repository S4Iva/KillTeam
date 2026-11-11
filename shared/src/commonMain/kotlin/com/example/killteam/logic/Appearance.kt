package com.example.killteam.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings

enum class ThemeMode(val label: String) { System("System"), Light("Light"), Dark("Dark") }

/** The chosen look of the app; kept apart from [AppState] because the lock screen needs it before the app data exists. */
class Appearance(private val settings: Settings) {
    var mode: ThemeMode by mutableStateOf(load())
        private set

    fun set(mode: ThemeMode) {
        this.mode = mode
        settings.putString(KEY, mode.name)
    }

    /** Dark or light for the given state of the device setting. */
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    private fun load(): ThemeMode = ThemeMode.entries.firstOrNull { it.name == settings.getString(KEY, "") } ?: ThemeMode.System

    companion object {
        const val KEY = "appearance"
    }
}
