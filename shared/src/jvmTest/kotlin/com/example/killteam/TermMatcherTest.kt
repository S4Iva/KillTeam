package com.example.killteam

import com.example.killteam.data.AppJson
import com.example.killteam.data.LookupData
import com.example.killteam.data.LookupTerm
import com.example.killteam.data.RulesData
import com.example.killteam.data.TermMatcher
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TermMatcherTest {
    private val matcher = TermMatcher(
        listOf(
            LookupTerm("control range", "Control Range"),
            LookupTerm("range", "Range"),
            LookupTerm("Piercing 1", "Piercing x"),
            LookupTerm("Hot", "Hot"),
        ),
    )

    @Test
    fun longestPhraseWins() {
        val m = matcher.find("within control range of an enemy")
        assertEquals(1, m.size)
        assertEquals("Control Range", m[0].term.title)
    }

    @Test
    fun digitsMatchAnyNumber() {
        val m = matcher.find("Piercing 2 and Piercing 12")
        assertEquals(2, m.size)
        assertEquals("Piercing x", m[1].term.title)
    }

    @Test
    fun partsOfWordsAreIgnored() {
        assertTrue(matcher.find("Whiteness, Hot-shot lasgun and ranger").isEmpty())
    }
}

class RulesDataTest {
    private val dir = File("src/commonMain/composeResources/files")

    @Test
    fun rulesAndLookupLoad() {
        val rules = AppJson.decodeFromString(RulesData.serializer(), File(dir, "rules.json").readText())
        val lookup = AppJson.decodeFromString(LookupData.serializer(), File(dir, "lookup.json").readText())
        assertTrue(rules.sections.size > 500)
        assertTrue(lookup.terms.size > 300)
        assertNotNull(rules.section("core-rules", "Control-Range"))
    }

    @Test
    fun actionSectionsKeepEffectAndConditionParagraphs() {
        val rules = AppJson.decodeFromString(RulesData.serializer(), File(dir, "rules.json").readText())
        val charge = rules.section("core-rules", "Charge")!!
        assertTrue(charge.paras.any { it.kind == "effect" })
        assertTrue(charge.paras.any { it.kind == "condition" })
        val teams = AppJson.decodeFromString(com.example.killteam.data.KillTeamData.serializer(), File(dir, "killteams.json").readText())
        val ploy = teams.teams.first { it.slug == "kasrkin" }.strategyPloys.first()
        assertTrue(ploy.paras.any { it.kind == "flavor" } && ploy.paras.any { it.kind == "text" })
        assertTrue(teams.teams.first { it.slug == "kasrkin" }.factionRuleItems.any { it.name == "Skill at Arms" })
    }

    @Test
    fun everyArrowLeadsToAnExistingSection() {
        val rules = AppJson.decodeFromString(RulesData.serializer(), File(dir, "rules.json").readText())
        val lookup = AppJson.decodeFromString(LookupData.serializer(), File(dir, "lookup.json").readText())
        lookup.terms.filter { it.hasRule }.forEach {
            assertNotNull(rules.section(it.page!!, it.anchor!!), "${it.phrase} -> ${it.page}#${it.anchor}")
        }
    }
}
