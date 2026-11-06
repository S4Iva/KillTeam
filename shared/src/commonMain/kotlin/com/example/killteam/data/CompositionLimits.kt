package com.example.killteam.data

/** How many operatives of one datacard, or of a set of datacards combined, the composition text of a team allows. */
class CompositionLimits(val perCard: Map<String, Int>, val groups: List<Group>) {
    class Group(val cards: Set<String>, val max: Int)

    /** How many more operatives of [card] fit with the [counts] already chosen; null when the text sets no limit for it. */
    fun room(counts: Map<String, Int>, card: String): Int? {
        val rooms = buildList {
            perCard[card]?.let { add(it - (counts[card] ?: 0)) }
            groups.filter { card in it.cards }.forEach { g -> add(g.max - g.cards.sumOf { counts[it] ?: 0 }) }
        }
        return rooms.minOrNull()?.coerceAtLeast(0)
    }

    fun problems(counts: Map<String, Int>): List<String> = buildList {
        perCard.forEach { (card, max) -> val n = counts[card] ?: 0; if (n > max) add("Too many $card: $max allowed, $n selected.") }
        groups.forEach { g ->
            val n = g.cards.sumOf { counts[it] ?: 0 }
            if (n > g.max) add("Too many of ${g.cards.joinToString()}: ${g.max} allowed, $n selected.")
        }
    }
}

/**
 * Reads the limits from the composition text, e.g. "1 KASRKIN SERGEANT operative", "9 KASRKIN operatives selected from the following list:",
 * "Other than TROOPER operatives, your kill team can only include each operative on this list once." and "* You cannot select more than four of these operatives combined.".
 * Whatever cannot be read safely sets no limit, so only the team size applies there.
 */
object CompositionRules {
    private class Entry(val card: String, val name: String, val marks: Set<Char>)

    private val numberWords = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten").withIndex().associate { it.value to it.index + 1 }
    private val listArrow = Regex("^(\\d+)\\s+.+?\\s+operatives?\\s+selected\\s+from\\s+the\\s+following\\s+list.*$", RegexOption.IGNORE_CASE)
    private val everyArrow = Regex("^Every\\b.*following list", RegexOption.IGNORE_CASE)
    private val leaderArrow = Regex("^(\\d+)\\s+(.+?)(?:\\s+operatives?\\b.*|\\s+(?:equipped\\s+)?with\\b.*)?$")
    private val onceRule = Regex("can only include each (?:operative|option) (?:on this list|above) once", RegexOption.IGNORE_CASE)
    private val exceptions = Regex("Other than (.+?) operatives?,", RegexOption.IGNORE_CASE)
    private val upTo = Regex("up to (\\w+) ([A-Z][A-Z\\- ]*[A-Z])\\s+operatives?\\b(?!\\s+with)")
    private val combined = Regex("^(\\*|\\d)\\s*You cannot select more than (\\w+) of these operatives combined", RegexOption.IGNORE_CASE)
    private val marks = Regex("[*\\d,]+$")

    private fun hasLower(s: String) = s.any { it.isLowerCase() }

    /** The datacard of a name from the composition; shortened from the front ("CELESTIAN INSIDIANT SUPERIOR") but never to a single word. */
    private fun findCard(team: KillTeam, raw: String): String? {
        val words = raw.trim().split(Regex("\\s+"))
        for (k in 0..maxOf(0, words.size - 2)) {
            WeaponOptions.resolve(team, words.drop(k).joinToString(" "))?.let { return it.name }
        }
        return null
    }

    /** An operative line of a list such as "GUNNER with flamer and gun butt*" or "SORCERER DESTINY1". */
    private fun entry(team: KillTeam, text: String): Entry? {
        val s = text.trim()
        val cut = Regex("\\s+(?:equipped\\s+)?with\\b|\\s+\\(").find(s)?.range?.first ?: s.length
        val head = s.substring(0, cut)
        val nameMarks = marks.find(head)?.value.orEmpty()
        val name = head.removeSuffix(nameMarks).replace(Regex("\\s+operatives?$"), "").trim()
        if (name.isEmpty() || hasLower(name)) return null
        val tail = if (cut < s.length) marks.find(s)?.value.orEmpty() else ""
        val card = findCard(team, name) ?: return null
        return Entry(card, name, (nameMarks + tail).filter { it == '*' || it.isDigit() }.toSet())
    }

    fun parse(team: KillTeam): CompositionLimits {
        val paras = team.compositionParas
        val lists = mutableListOf<Pair<Int, List<Entry>>>()
        val fixed = mutableMapOf<String, Int>()
        var i = 0
        while (i < paras.size) {
            val text = paras[i].text.trim()
            // A list header can also be stored as a bullet inside the previous list.
            val isArrow = paras[i].kind == "arrow" || listArrow.containsMatchIn(text)
            i++
            if (!isArrow) continue
            val bullets = mutableListOf<String>()
            while (i < paras.size && (paras[i].kind == "bullet" || (paras[i].kind == "text" && paras[i].text.trim().startsWith("Or ")))) {
                if (listArrow.containsMatchIn(paras[i].text.trim())) break
                if (paras[i].kind == "bullet") bullets += paras[i].text
                i++
            }
            val list = listArrow.find(text)
            when {
                list != null -> lists += list.groupValues[1].toInt() to bullets.filterNot { isOption(it) }.mapNotNull { entry(team, it) }
                everyArrow.containsMatchIn(text) -> bullets.forEach { b ->
                    Regex("^(\\d+)\\s+(.+)$").find(b.trim())?.let { m -> entry(team, m.groupValues[2])?.let { e -> fixed.merge(e.card, m.groupValues[1].toInt(), Int::plus) } }
                }
                !text.contains("selected from") -> leaderArrow.find(text)?.let { m ->
                    val name = m.groupValues[2]
                    if (!hasLower(name)) findCard(team, name)?.let { fixed.merge(it, m.groupValues[1].toInt(), Int::plus) }
                }
            }
        }

        val texts = paras.filter { it.kind == "text" }.map { it.text }
        val weighted = paras.any { Regex("counts? as", RegexOption.IGNORE_CASE).containsMatchIn(it.text) }
        val entries = lists.flatMap { it.second }
        val perCard = mutableMapOf<String, Int>()
        val groups = mutableListOf<CompositionLimits.Group>()

        texts.firstOrNull { onceRule.containsMatchIn(it) && !it.contains("unless", ignoreCase = true) }?.let { rule ->
            val exempt = exceptions.find(rule)?.groupValues?.get(1)?.split(Regex(",|\\band\\b"))?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
            entries.groupBy { it.card }.forEach { (card, es) ->
                if (es.none { e -> exempt.any { x -> e.name == x || e.name.endsWith(" $x") || card.uppercase().let { c -> c == x || c.endsWith(" $x") } } }) perCard[card] = es.size
            }
        }
        texts.forEach { t ->
            upTo.findAll(t).forEach { m ->
                val n = numberWords[m.groupValues[1].lowercase()] ?: m.groupValues[1].toIntOrNull() ?: return@forEach
                val card = findCard(team, m.groupValues[2]) ?: return@forEach
                perCard[card] = minOf(perCard[card] ?: n, n)
            }
        }
        val listed = entries.map { it.card }.toSet()
        fixed.forEach { (card, n) -> if (card !in listed) perCard[card] = minOf(perCard[card] ?: n, n) }

        // Totals only hold when every list stands alone: no weighted selections, no list reused by a second pool, no card also bought as a fixed count.
        val reused = paras.any { it.text.contains("list above", ignoreCase = true) }
        if (!weighted && !reused) {
            val sets = lists.map { (_, es) -> es.map { it.card }.toSet() }
            lists.forEachIndexed { idx, (n, es) ->
                val cards = es.map { it.card }.toSet()
                val overlaps = sets.filterIndexed { j, s -> j != idx && s.any { it in cards } }.isNotEmpty()
                if (cards.isNotEmpty() && !overlaps && cards.none { it in fixed }) groups += CompositionLimits.Group(cards, n)
            }
        }
        if (!weighted) {
            texts.forEach { t ->
                val m = combined.find(t.trim()) ?: return@forEach
                val n = numberWords[m.groupValues[2].lowercase()] ?: return@forEach
                val cards = entries.filter { m.groupValues[1][0] in it.marks }.map { it.card }.toSet()
                if (cards.isNotEmpty()) groups += CompositionLimits.Group(cards, n)
            }
        }
        return CompositionLimits(perCard, groups)
    }
}
