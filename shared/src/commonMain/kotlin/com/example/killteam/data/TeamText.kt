package com.example.killteam.data

/** Kind of a team part that can be found by the search; the index is the position in the matching list of the team. */
enum class SearchKind { Team, Operative, FactionRule, StrategyPloy, FirefightPloy, Equipment }

/** One searchable part of a kill team (an operative, a ploy, an equipment option, ...). */
class SearchEntry(val label: String, val text: String, val kind: SearchKind = SearchKind.Team, val index: Int = -1)

private fun NamedText.fullText() = (listOf(name, text) + paras.map { it.text }).joinToString(" ")

internal fun buildSearchIndex(team: KillTeam): List<SearchEntry> = buildList {
    add(SearchEntry("Team: ${team.name}", listOf(team.name, team.description, team.archetypes.joinToString(" "), team.factionKeyword, team.composition.joinToString(" ")).joinToString(" ").lowercase()))
    team.operatives.forEachIndexed { i, o ->
        val parts = listOf(o.name, o.keywords, o.flavor) +
            o.weapons.flatMap { listOf(it.name, it.wr) } + o.rules +
            o.blocks.flatMap { b -> listOf(b.name) + b.paras.map { it.text } }
        add(SearchEntry("Operative: ${o.name}", parts.joinToString(" ").lowercase(), SearchKind.Operative, i))
    }
    if (team.factionRuleItems.isEmpty() && team.factionRules.isNotBlank()) add(SearchEntry("Faction rules", team.factionRules.lowercase(), SearchKind.FactionRule, 0))
    team.factionRuleItems.forEachIndexed { i, it -> add(SearchEntry("Faction rule: ${it.name}", it.fullText().lowercase(), SearchKind.FactionRule, i)) }
    team.strategyPloys.forEachIndexed { i, it -> add(SearchEntry("Strategy ploy: ${it.name}", it.fullText().lowercase(), SearchKind.StrategyPloy, i)) }
    team.firefightPloys.forEachIndexed { i, it -> add(SearchEntry("Firefight ploy: ${it.name}", it.fullText().lowercase(), SearchKind.FirefightPloy, i)) }
    team.equipment.forEachIndexed { i, it -> add(SearchEntry("Equipment: ${it.name}", it.fullText().lowercase(), SearchKind.Equipment, i)) }
}

/** Word search in the Teams tab: every word of the query must occur (anywhere) in the team's texts. */
object TeamSearch {
    const val MIN_LENGTH = 3

    fun words(query: String): List<String> = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

    fun matches(team: KillTeam, query: String): Boolean {
        val words = words(query)
        if (words.isEmpty()) return true
        if (query.trim().length < MIN_LENGTH) {
            return team.name.contains(query.trim(), ignoreCase = true) || team.operatives.any { it.name.contains(query.trim(), ignoreCase = true) }
        }
        val all = team.searchText
        return words.all { it in all }
    }

    /** Names of the parts of the team that contain all words, e.g. "Equipment: Grav-chute". */
    fun hits(team: KillTeam, query: String): List<String> = hitEntries(team, query).map { it.label }

    /** The parts of the team that contain all words, with the position needed to jump to them. */
    fun hitEntries(team: KillTeam, query: String): List<SearchEntry> {
        val words = words(query)
        if (query.trim().length < MIN_LENGTH || words.isEmpty()) return emptyList()
        return team.searchIndex.filter { e -> words.all { it in e.text } }
    }
}

/**
 * Indentation level of every line of the team composition: 0 for arrows, 1 for list entries and 2 for the equipment
 * options below an operative entry ("MILITANT with one of the following options:" followed by its weapon choices).
 */
fun compositionLevels(paras: List<AbilityPara>): List<Int> {
    var parent = ""
    return paras.map { p ->
        when (p.kind) {
            "arrow" -> { parent = "arrow"; 0 }
            "bullet" -> if (isOption(p.text) && parent == "bullet") 2 else { if (!isOption(p.text)) parent = "bullet"; 1 }
            else -> if (parent == "bullet" && p.text.startsWith("Or ")) 2 else 0
        }
    }
}

/** Operative entries start with an upper-case name; equipment options start with a normal word such as "Plasma pistol". */
internal fun isOption(text: String): Boolean {
    val first = text.trim().takeWhile { !it.isWhitespace() }
    return first.any { it.isLowerCase() }
}

/** Tac ops a team may select: those of its archetypes, ordered by archetype and name; all of them if the archetypes are unknown. */
fun tacOpsFor(team: KillTeam?): List<TacOp> {
    val archetypes = team?.archetypes?.mapNotNull { a -> Archetype.entries.firstOrNull { it.label == a.trim() } }.orEmpty()
    if (archetypes.isEmpty()) return ApprovedOps.tacOps.sortedWith(compareBy({ it.archetype.ordinal }, { it.name }))
    return ApprovedOps.tacOps.filter { it.archetype in archetypes }.sortedWith(compareBy({ archetypes.indexOf(it.archetype) }, { it.name }))
}
