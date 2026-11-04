package com.example.killteam.data

/** One weapon choice of an operative, e.g. "Hot-shot lasgun; Gun butt", with the names of the datacard weapons it stands for. */
class WeaponOption(val label: String, val weapons: List<String>)

/** Result of reading the option texts of a team composition. [unresolved] holds the operatives whose text could not be mapped, [unrecognized] the option lines that were not understood as a header. */
class WeaponOptionReport(val options: Map<String, List<WeaponOption>>, val unresolved: List<String>, val unrecognized: List<String> = emptyList())

/**
 * Reads the weapon options of the operatives from the composition text of a team, for example
 * "1 KASRKIN SERGEANT operative with one of the following options:" followed by "Bolt pistol; power weapon".
 * An operative whose text cannot be mapped completely onto its datacard weapons gets no options at all, so the normal datacard applies.
 */
object WeaponOptions {
    private val optionWords = Regex("\\s*(?:;|\\band\\b|&)\\s*", RegexOption.IGNORE_CASE)
    private val partWords = Regex("\\s*(?:,|\\band\\b|&)\\s*", RegexOption.IGNORE_CASE)
    private val alternativeWords = Regex("\\s*(?:,|\\bor\\b)\\s*", RegexOption.IGNORE_CASE)
    private val arrowHeader = Regex("^\\d+\\s+(.+?)\\s+(?:operatives?\\s+equipped\\s+with|operatives?\\s+with|equipped\\s+with|with)\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val bulletHeader = Regex("^(?:\\d+\\s+)?(\\S.*?)(?:\\s+operatives?)?(?:\\s+equipped)?\\s+with\\s+(.+)$")
    private val optionLine = Regex("one of the following option|one option from each|the following option", RegexOption.IGNORE_CASE)

    fun parse(team: KillTeam): WeaponOptionReport {
        val paras = team.compositionParas
        val options = linkedMapOf<String, MutableList<WeaponOption>>()
        val unresolved = linkedSetOf<String>()
        val unrecognized = mutableListOf<String>()
        var i = 0
        while (i < paras.size) {
            val header = header(paras[i], team)
            if (header == null && optionLine.containsMatchIn(paras[i].text) && paras[i].kind != "text") unrecognized += paras[i].text.trim()
            i++
            if (header == null) continue
            val (rawName, spec) = header
            val following = mutableListOf<String>()
            while (i < paras.size && paras[i].kind == "bullet" && isOption(paras[i].text)) { following += paras[i].text; i++ }
            val groups = mutableListOf<List<String>>()
            if (i < paras.size && paras[i].kind == "text" && paras[i].text.trim().startsWith("Or one option from each", ignoreCase = true)) {
                i++
                while (i < paras.size && paras[i].kind == "bullet" && isOption(paras[i].text)) { groups += splitAlternatives(paras[i].text); i++ }
            }

            val texts = optionTexts(spec, following, groups)
            val datacard = resolve(team, rawName)
            val mapped = datacard?.let { op -> texts?.map { map(op, it) } }
            if (datacard == null || mapped == null || mapped.isEmpty() || mapped.any { it == null }) {
                unresolved += rawName
            } else {
                val list = options.getOrPut(datacard.name) { mutableListOf() }
                mapped.filterNotNull().forEach { o -> if (list.none { it.label == o.label }) list += o }
            }
        }
        // An operative named in an unresolved text keeps no options at all.
        val dropped = unresolved.mapNotNull { resolve(team, it)?.name }.toSet()
        return WeaponOptionReport(options.filterKeys { it !in dropped }, unresolved.toList(), unrecognized)
    }

    /** Operative name and the text after "with" of an arrow line ("1 NAME operative with ...") or a bullet line ("NAME with ..."). */
    private fun header(p: AbilityPara, team: KillTeam): Pair<String, String>? {
        val text = p.text.trim()
        when (p.kind) {
            "arrow" -> arrowHeader.find(text)?.let { return it.groupValues[1] to it.groupValues[2] }
            "bullet" -> bulletHeader.find(text)?.let { m ->
                val name = m.groupValues[1].trimEnd('*').trim()
                if (name.none { it.isLowerCase() }) return name to m.groupValues[2]
            }
        }
        return null
    }

    /** Option texts of one operative, or null when the structure is not understood. */
    private fun optionTexts(spec: String, following: List<String>, groups: List<List<String>>): List<List<String>>? {
        val s = spec.trim().trimEnd('*', '.', ':').trim()
        val fixed = Regex("^(.+?)\\s+and\\s+one of the following options$", RegexOption.IGNORE_CASE).find(s)?.groupValues?.get(1)
        val result = mutableListOf<List<String>>()
        when {
            fixed != null -> {
                if (following.isEmpty()) return null
                following.forEach { line -> splitAlternatives(line).forEach { result += splitParts(fixed) + splitParts(it) } }
            }
            s.contains("one of the following option", ignoreCase = true) || s.contains("the following option", ignoreCase = true) -> {
                if (following.isEmpty()) return null
                following.forEach { result += splitOption(it) }
            }
            s.contains("one option from each", ignoreCase = true) -> {
                if (following.isEmpty()) return null
                result += cartesian(following.map { splitAlternatives(it) })
            }
            else -> result += splitOption(s)
        }
        if (groups.isNotEmpty()) result += cartesian(groups)
        return result
    }

    private fun splitOption(text: String): List<String> = text.split(";").map { it.trim().trimEnd('*', '.') }.filter { it.isNotEmpty() }

    private fun splitParts(text: String): List<String> = text.trim().trimEnd('*', '.').let { listOf(it) }

    private fun splitAlternatives(text: String): List<String> = text.trim().trimEnd('*', '.').split(alternativeWords).map { it.trim() }.filter { it.isNotEmpty() }

    /** All combinations that take one entry from every group; each combination is the list of chosen entries. */
    private fun cartesian(groups: List<List<String>>): List<List<String>> =
        groups.fold(listOf(emptyList<String>())) { acc, group -> acc.flatMap { prefix -> group.map { prefix + it } } }

    /** The datacard of a composition name such as "KASRKIN SERGEANT", "GUNNER" or "IMPERIAL NAVY BREACHER SERGEANT-AT-ARMS". */
    private fun resolve(team: KillTeam, rawName: String): Operative? {
        val name = rawName.trimEnd('*').trim().lowercase()
        val short = name.removePrefix(team.name.lowercase()).trim()
        // "GUNNER" must find "<Team> Gunner" even when "<Team> Heavy Gunner" also ends with the same word.
        val exact = listOf(name, short, "${team.name.lowercase()} $short").filter { it.isNotEmpty() }
        for (candidate in exact) {
            team.operatives.filter { it.name.lowercase() == candidate }.singleOrNull()?.let { return it }
        }
        for (candidate in listOf(name, short).filter { it.isNotEmpty() }) {
            val matches = team.operatives.filter { it.name.lowercase().endsWith(" $candidate") }
            matches.singleOrNull()?.let { return it }
            // "GUNNER" with a "Heavy Gunner" in the same team: the datacard with the fewest words is meant, a tie stays unresolved.
            val fewest = matches.minOfOrNull { it.name.trim().split(Regex("\\s+")).size }
            matches.filter { it.name.trim().split(Regex("\\s+")).size == fewest }.singleOrNull()?.let { return it }
        }
        return null
    }

    private fun baseName(weapon: Weapon) = weapon.name.replace(Regex("\\s*\\(.*\\)$"), "").trim().lowercase()

    /** Maps the parts of one option onto datacard weapons; null if any part is unknown. */
    private fun map(op: Operative, parts: List<String>): WeaponOption? {
        val bases = op.weapons.map { baseName(it) }.distinct()
        val chosen = mutableListOf<String>()
        for (part in parts) {
            val whole = part.lowercase().trimEnd('*', '.').trim()
            val pieces = when {
                whole in bases -> listOf(whole)
                else -> whole.split(optionWords).flatMap { it.split(partWords) }.map { it.trim() }.filter { it.isNotEmpty() }
            }
            if (pieces.isEmpty() || pieces.any { it !in bases }) return null
            pieces.forEach { if (it !in chosen) chosen += it }
        }
        val weapons = op.weapons.filter { baseName(it) in chosen }.map { it.name }
        val label = chosen.joinToString("; ") { b -> op.weapons.first { baseName(it) == b }.name.replace(Regex("\\s*\\(.*\\)$"), "").trim() }
        return WeaponOption(label, weapons)
    }
}
