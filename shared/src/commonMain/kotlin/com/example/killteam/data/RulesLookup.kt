package com.example.killteam.data

/** Keyword lists and searches of the Rules tab, built from the lookup terms and the rule sections. */
object RulesLookup {
    class Suggestion(val label: String, val kind: String)
    class Results(val keywords: List<LookupTerm>, val sections: List<RuleSection>)

    /** Tabs of the quick reference with the titles of their entries (a trailing " x" of a title is ignored when matching). */
    val quickTabs: List<Pair<String, List<String>>> = listOf(
        "Actions" to listOf("Dash", "Fall Back", "Fight", "Reposition", "Shoot", "Charge", "Pick Up Marker", "Place Marker", "Operate Hatch"),
        "Weapon rules" to listOf(
            "Accurate", "Balanced", "Blast", "Brutal", "Ceaseless", "Devastating", "Heavy", "Hot", "Limited", "Piercing", "Range", "Rending",
            "Saturate", "Seek", "Severe", "Shock", "Silent", "Stun", "Torrent",
        ),
        "Core terms" to listOf("Control Range", "Counteract", "Conceal", "Engage", "Cover", "Obscured", "Visible", "Damage", "Ploys", "Equipment"),
    )

    /** Keywords offered as chips on the start page. */
    val commonKeywords = listOf("Piercing", "Blast", "Devastating", "Heavy", "Cover", "Obscured", "Conceal", "Engage", "Counteract", "Control Range")

    private fun usable(t: LookupTerm) = t.text.length >= 30 && t.page != "about" && t.title.isNotBlank()

    private fun base(title: String) = title.removeSuffix(" x").trim()

    /** One entry per title with a real description (team ploy names and site info are left out), sorted by title. */
    fun glossary(terms: List<LookupTerm>): List<LookupTerm> =
        terms.filter(::usable).distinctBy { it.title.lowercase() }.sortedBy { it.title.lowercase() }

    fun byTitle(terms: List<LookupTerm>, name: String): LookupTerm? = terms.firstOrNull { usable(it) && base(it.title).equals(name, ignoreCase = true) }

    fun quick(terms: List<LookupTerm>): List<Pair<String, List<LookupTerm>>> =
        quickTabs.map { (tab, names) -> tab to names.mapNotNull { byTitle(terms, it) } }.filter { it.second.isNotEmpty() }

    fun common(terms: List<LookupTerm>): List<LookupTerm> = commonKeywords.mapNotNull { byTitle(terms, it) }

    /** Up to [limit] suggestions for a query of at least 2 letters: keywords starting with it first, then others, then rule sections. */
    fun suggestions(terms: List<LookupTerm>, sections: List<RuleSection>, query: String, limit: Int = 4): List<Suggestion> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val g = glossary(terms)
        val keywords = g.filter { it.title.startsWith(q, ignoreCase = true) } + g.filter { !it.title.startsWith(q, ignoreCase = true) && it.title.contains(q, ignoreCase = true) }
        val rules = sections.filter { it.title.contains(q, ignoreCase = true) }
        return (keywords.take(3).map { Suggestion(it.title, "Keyword") } + rules.map { Suggestion(it.title, "Rule") })
            .distinctBy { it.label.lowercase() }.take(limit)
    }

    /** Search over keywords (title first, then text) and rule sections for a query of at least 3 letters. */
    fun search(terms: List<LookupTerm>, sections: List<RuleSection>, query: String): Results {
        val q = query.trim()
        if (q.length < 3) return Results(emptyList(), emptyList())
        val g = glossary(terms)
        val keywords = (g.filter { it.title.contains(q, ignoreCase = true) } + g.filter { !it.title.contains(q, ignoreCase = true) && it.text.contains(q, ignoreCase = true) }).take(20)
        val found = sections.filter { it.title.contains(q, ignoreCase = true) || it.plain.contains(q, ignoreCase = true) }.take(60)
        return Results(keywords, found)
    }
}
