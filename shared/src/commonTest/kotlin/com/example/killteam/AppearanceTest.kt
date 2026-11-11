package com.example.killteam

import com.example.killteam.logic.Appearance
import com.example.killteam.logic.ThemeMode
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppearanceTest {
    @Test
    fun theDefaultFollowsTheSystem() {
        val a = Appearance(MapSettings())
        assertEquals(ThemeMode.System, a.mode)
        assertTrue(a.isDark(systemDark = true))
        assertFalse(a.isDark(systemDark = false))
    }

    @Test
    fun lightAndDarkIgnoreTheSystemSetting() {
        val a = Appearance(MapSettings())
        a.set(ThemeMode.Light)
        assertFalse(a.isDark(systemDark = true))
        a.set(ThemeMode.Dark)
        assertTrue(a.isDark(systemDark = false))
    }

    @Test
    fun theChoiceIsStoredAndUnknownValuesFallBackToSystem() {
        val settings = MapSettings()
        Appearance(settings).set(ThemeMode.Light)
        assertEquals(ThemeMode.Light, Appearance(settings).mode)
        settings.putString(Appearance.KEY, "purple")
        assertEquals(ThemeMode.System, Appearance(settings).mode)
    }
}
