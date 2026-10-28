package com.example.killteam

import com.example.killteam.logic.Persisted
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Serializable
private data class Item(val name: String, val level: Int = 1)

class PersistedTest {
    private val serializer = ListSerializer(Item.serializer())

    @Test
    fun roundTripSurvivesALongValue() {
        val settings = MapSettings()
        val items = List(2000) { Item("entry $it") }
        Persisted(settings, "k", serializer, emptyList<Item>()).update(items)
        assertEquals(items, Persisted(settings, "k", serializer, emptyList<Item>()).value)
    }

    @Test
    fun dataWrittenBeforeVersioningStillLoads() {
        val settings = MapSettings("k#n" to 1, "k#0" to """[{"name":"old"}]""")
        assertEquals(listOf(Item("old")), Persisted(settings, "k", serializer, emptyList<Item>()).value)
    }

    @Test
    fun olderVersionsAreMigratedAndRewritten() {
        val settings = MapSettings("k#n" to 1, "k#0" to """[{"title":"old"}]""", "k#v" to 1)
        val migrate: (Int, String) -> String = { from, json -> if (from == 1) json.replace("\"title\"", "\"name\"") else json }
        val loaded = Persisted(settings, "k", serializer, emptyList<Item>(), version = 2, migrate = migrate)
        assertEquals(listOf(Item("old")), loaded.value)
        assertEquals(2, settings.getInt("k#v", 0))
        assertEquals(listOf(Item("old")), Persisted(settings, "k", serializer, emptyList<Item>(), version = 2, migrate = migrate).value)
    }

    @Test
    fun unreadableDataIsBackedUpAndNotOverwrittenByLoading() {
        val settings = MapSettings("k#n" to 1, "k#0" to "{ this is not json", "k#v" to 1)
        val p = Persisted(settings, "k", serializer, emptyList<Item>())
        assertTrue(p.value.isEmpty())
        assertTrue(p.hasBackup)
        assertEquals("{ this is not json", settings.getString("k#0", ""))
        assertEquals("{ this is not json", settings.getString("k#backup#0", ""))
    }

    @Test
    fun anExistingBackupIsNotReplaced() {
        val settings = MapSettings("k#n" to 1, "k#0" to "broken two", "k#v" to 1, "k#backup#n" to 1, "k#backup#0" to "first backup")
        Persisted(settings, "k", serializer, emptyList<Item>())
        assertEquals("first backup", settings.getString("k#backup#0", ""))
    }

    @Test
    fun missingDataGivesTheDefault() {
        val p = Persisted(MapSettings(), "k", String.serializer(), "fallback")
        assertEquals("fallback", p.value)
        assertFalse(p.hasBackup)
    }
}
