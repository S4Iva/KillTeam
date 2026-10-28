package com.example.killteam.data

import killteam.shared.generated.resources.Res
import kotlinx.serialization.Serializable

object LenientRuleSectionList : LenientList<RuleSection>(RuleSection.serializer())
object LenientRulePageList : LenientList<RulePage>(RulePage.serializer())
object LenientTermList : LenientList<LookupTerm>(LookupTerm.serializer())

@Serializable
class RulePage(val id: String = "", val title: String = "")

@Serializable
class RuleSection(
    val page: String = "",
    val id: String = "",
    val title: String = "",
    val level: Int = 2,
    val text: String = "",
    @Serializable(with = LenientParaList::class) val paras: List<AbilityPara> = emptyList(),
) {
    /** Plain text for searching. */
    val plain: String get() = text.ifEmpty { paras.joinToString("\n") { it.text } }
}

@Serializable
class RulesData(
    val importedAt: String = "",
    @Serializable(with = LenientRulePageList::class) val pages: List<RulePage> = emptyList(),
    @Serializable(with = LenientRuleSectionList::class) val sections: List<RuleSection> = emptyList(),
) {
    fun section(page: String, anchor: String) =
        sections.firstOrNull { it.page == page && it.id.equals(anchor, ignoreCase = true) }
}

/** A keyword with its popup text and, when known, the rule section it leads to. */
@Serializable
class LookupTerm(
    val phrase: String = "",
    val title: String = "",
    val text: String = "",
    val page: String? = null,
    val anchor: String? = null,
) {
    val hasRule get() = !page.isNullOrBlank() && !anchor.isNullOrBlank()
}

@Serializable
class LookupData(@Serializable(with = LenientTermList::class) val terms: List<LookupTerm> = emptyList())

data class TextMatch(val start: Int, val end: Int, val term: LookupTerm)

/** Finds the keyword phrases in a text. Digits in a phrase match any number (e.g. "Piercing 1" matches "Piercing 2"). */
class TermMatcher(terms: List<LookupTerm>) {
    private val byKey: Map<String, LookupTerm>
    private val regex: Regex?

    init {
        val usable = terms.filter { it.phrase.length >= 3 }
        byKey = buildMap { usable.forEach { putIfAbsent(key(it.phrase), it) } }
        val patterns = usable.map { it.phrase }.distinctBy { key(it) }.sortedByDescending { it.length }.map { phrase ->
            phrase.split(DIGITS).joinToString("\\d+") { Regex.escape(it) }
        }
        regex = if (patterns.isEmpty()) null else Regex(patterns.joinToString("|"), RegexOption.IGNORE_CASE)
    }

    fun find(text: String): List<TextMatch> {
        val r = regex ?: return emptyList()
        return r.findAll(text).mapNotNull { m ->
            val before = text.getOrNull(m.range.first - 1)
            val after = text.getOrNull(m.range.last + 1)
            if (before?.isWordChar() == true || after?.isWordChar() == true) return@mapNotNull null
            byKey[key(m.value)]?.let { TextMatch(m.range.first, m.range.last + 1, it) }
        }.toList()
    }

    private fun key(phrase: String) = phrase.lowercase().replace(DIGITS, "#")

    private fun Char.isWordChar() = isLetterOrDigit() || this == '-'

    private companion object {
        val DIGITS = Regex("\\d+")
    }
}

suspend fun loadRulesData(): RulesData =
    AppJson.decodeFromString(RulesData.serializer(), Res.readBytes("files/rules.json").decodeToString())

suspend fun loadLookupData(): LookupData =
    AppJson.decodeFromString(LookupData.serializer(), Res.readBytes("files/lookup.json").decodeToString())
