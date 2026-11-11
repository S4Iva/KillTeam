package com.example.killteam

import com.example.killteam.data.AppJson
import com.example.killteam.data.LookupData
import com.example.killteam.data.RulesData
import com.example.killteam.data.RulesLookup
import com.example.killteam.logic.AppState
import com.russhwolf.settings.MapSettings
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RulesLookupTest {
    private val dir = "src/commonMain/composeResources/files"
    private val terms by lazy { AppJson.decodeFromString(LookupData.serializer(), File("$dir/lookup.json").readText()).terms }
    private val sections by lazy { AppJson.decodeFromString(RulesData.serializer(), File("$dir/rules.json").readText()).sections }

    @Test
    fun theGlossaryHasOneEntryPerTitleAndNoSiteInfoOrBareNames() {
        val g = RulesLookup.glossary(terms)
        assertTrue(g.size > 100)
        assertEquals(g.size, g.map { it.title.lowercase() }.distinct().size)
        assertTrue(g.none { it.page == "about" || it.text.length < 30 })
        assertEquals(g.map { it.title.lowercase() }.sorted(), g.map { it.title.lowercase() })
    }

    @Test
    fun theQuickReferenceFindsItsEntries() {
        val quick = RulesLookup.quick(terms).toMap()
        assertEquals(setOf("Actions", "Weapon rules", "Core terms"), quick.keys)
        assertTrue(quick.getValue("Actions").map { it.title }.containsAll(listOf("Shoot", "Fight", "Dash")))
        assertTrue(quick.getValue("Weapon rules").size >= 10)
        assertTrue(RulesLookup.common(terms).size >= 8)
    }

    @Test
    fun suggestionsNeedTwoLettersAndAreLimited() {
        assertTrue(RulesLookup.suggestions(terms, sections, "p").isEmpty())
        val s = RulesLookup.suggestions(terms, sections, "pie")
        assertTrue(s.isNotEmpty() && s.size <= 4)
        assertTrue(s.first().label.startsWith("Pie", ignoreCase = true))
    }

    @Test
    fun searchGroupsKeywordsAndRuleSections() {
        assertTrue(RulesLookup.search(terms, sections, "pi").keywords.isEmpty())
        val r = RulesLookup.search(terms, sections, "piercing")
        assertTrue(r.keywords.isNotEmpty() && r.keywords.first().title.contains("Piercing", ignoreCase = true))
        assertTrue(r.sections.size <= 60)
    }

    @Test
    fun recentSearchesAreNewestFirstWithoutDuplicatesAndLimited() {
        val app = AppState(MapSettings())
        app.addRecentSearch("ab")
        assertTrue(app.rulesRecent.value.isEmpty())
        listOf("blast", "cover", "Blast", "conceal", "engage", "heavy", "hot", "shoot").forEach { app.addRecentSearch(it) }
        assertEquals("shoot", app.rulesRecent.value.first())
        assertEquals(AppState.MAX_RECENT, app.rulesRecent.value.size)
        app.addRecentSearch("COVER")
        assertEquals("COVER", app.rulesRecent.value.first())
        assertEquals(1, app.rulesRecent.value.count { it.equals("cover", ignoreCase = true) })
        assertEquals(AppState.MAX_RECENT, app.rulesRecent.value.size)
    }

    @Test
    fun pinningTogglesAKeyword() {
        val app = AppState(MapSettings())
        app.togglePinned("Blast x")
        assertEquals(listOf("Blast x"), app.rulesPinned.value)
        app.togglePinned("Blast x")
        assertTrue(app.rulesPinned.value.isEmpty())
    }
}
