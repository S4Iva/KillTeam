package com.example.killteam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.killteam.data.LookupTerm
import com.example.killteam.data.RuleSection
import com.example.killteam.data.RulesData
import com.example.killteam.data.RulesLookup
import com.example.killteam.logic.AppState

@Composable
fun RulesScreen(app: AppState) {
    val ui = LocalUi.current
    val pending = app.pendingRule
    LaunchedEffect(pending) {
        if (pending != null) {
            ui.rulesPage = pending.page
            ui.rulesTarget = pending.anchor
            ui.rulesHighlight = pending.anchor
            app.pendingRule = null
        }
    }

    val data = app.rules
    val open = ui.rulesPage
    when {
        data == null -> Page("Rules") { item { LoadStatus(app, "rules") } }
        open != null -> RulePageView(app, data, open, ui.rulesTarget, ui.rulesHighlight, onScrolled = { ui.rulesTarget = null }) { ui.closeRulesPage() }
        else -> RulesIndex(app, data) { p, anchor -> ui.rulesPage = p; ui.rulesTarget = anchor; ui.rulesHighlight = anchor }
    }
}

/** Rules tab: "Rules" (search, pinned keywords, quick reference, rule pages) and "Glossary" (A-Z); one keyword popup for both. */
@Composable
private fun RulesIndex(app: AppState, data: RulesData, onOpen: (String, String?) -> Unit) {
    var popup by remember { mutableStateOf<LookupTerm?>(null) }
    popup?.let { t ->
        KeywordDialog(t, { popup = null }, { onOpen(it.page, it.anchor) }, pinned = t.title in app.rulesPinned.value, onTogglePin = { app.togglePinned(t.title) })
    }
    if (LocalUi.current.rulesTab == 1) GlossaryTab(app) { popup = it } else RulesStart(app, data, onOpen) { popup = it }
}

@Composable
private fun RulesTabs() {
    val ui = LocalUi.current
    SegmentedChoice(listOf(0, 1), ui.rulesTab, { if (it == 0) "Rules" else "Glossary" }, { ui.rulesTab = it })
}

/** Short one-line version of a description. */
private fun brief(text: String, max: Int = 90): String {
    val t = text.replace('\n', ' ').trim()
    return if (t.length <= max) t else t.take(max).trimEnd() + "..."
}

@Composable
private fun RulesStart(app: AppState, data: RulesData, onOpen: (String, String?) -> Unit, onKeyword: (LookupTerm) -> Unit) {
    val ui = LocalUi.current
    val query = ui.rulesQuery
    val terms = app.lookup
    val results = remember(query, terms, data) { RulesLookup.search(terms, data.sections, query) }
    val suggestions = remember(query, terms, data) { RulesLookup.suggestions(terms, data.sections, query) }
    val quick = remember(terms) { RulesLookup.quick(terms) }
    val common = remember(terms) { RulesLookup.common(terms) }
    val glossary = remember(terms) { RulesLookup.glossary(terms) }
    val searching = query.trim().length >= 3

    Page("Rules", listState = ui.list("rules-index")) {
        item { RulesTabs() }
        item {
            ChipRow(listOf(false, true), { it == ui.rulesFavoritesOnly }, { if (it) "Favorites" else "ALL" }, { ui.rulesFavoritesOnly = it })
        }
        item {
            OutlinedTextField(
                query, { ui.rulesQuery = it }, Modifier.fillMaxWidth(), label = { Text("Search rules and keywords (min. 3 letters)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { app.addRecentSearch(query) }),
            )
        }
        if (query.trim().length >= 2 && suggestions.isNotEmpty() && !(ui.rulesFavoritesOnly && !searching)) {
            item {
                SectionCard("Suggestions") {
                    suggestions.forEachIndexed { i, s ->
                        if (i > 0) HorizontalDivider(color = Palette.Text.copy(alpha = 0.15f))
                        Row(
                            Modifier.fillMaxWidth().clickable { ui.rulesQuery = s.label; app.addRecentSearch(s.label) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(s.label, Modifier.weight(1f))
                            Text(s.kind, style = MaterialTheme.typography.labelMedium, color = Palette.Accent)
                        }
                    }
                }
            }
        }
        if (searching) {
            if (results.keywords.isEmpty() && results.sections.isEmpty()) item { Text("No matches.") }
            if (results.keywords.isNotEmpty()) {
                item {
                    SectionCard("Keywords (${results.keywords.size})") {
                        results.keywords.forEachIndexed { i, t ->
                            if (i > 0) HorizontalDivider(color = Palette.Text.copy(alpha = 0.15f))
                            Column(Modifier.fillMaxWidth().clickable { app.addRecentSearch(query); onKeyword(t) }.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(t.title, fontWeight = FontWeight.Bold)
                                Text(brief(t.text, 120), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            if (results.sections.isNotEmpty()) {
                item { Text("Rule sections (${results.sections.size})", style = MaterialTheme.typography.titleMedium, color = Palette.Accent) }
                items(results.sections.size) { i ->
                    val s = results.sections[i]
                    val pageTitle = data.pages.firstOrNull { it.id == s.page }?.title ?: s.page
                    val favKey = "section:${s.page}/${s.id}"
                    SectionCard(
                        s.title, Modifier.clickable { app.addRecentSearch(query); onOpen(s.page, s.id) },
                        actions = { FavoriteButton(app.isFavorite(favKey)) { app.toggleFavorite(favKey) } },
                    ) {
                        Text(pageTitle, style = MaterialTheme.typography.bodySmall, color = Palette.Accent)
                        Text(snippet(s, query.trim()), maxLines = 4, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else if (ui.rulesFavoritesOnly) {
            item {
                SectionCard("Favorites") {
                    if (app.favorites.value.isEmpty()) Text("Nothing marked yet. Use the star on rule pages, rule sections, teams, crit ops and tac ops.")
                    app.favorites.value.forEach { key -> KeyLink(app, key) }
                }
            }
        } else {
            item {
                SectionCard("Pinned keywords") {
                    val pinned = app.rulesPinned.value.mapNotNull { name -> glossary.firstOrNull { it.title == name } }
                    if (pinned.isEmpty()) Text("Pin keywords from their popup to find them here.", style = MaterialTheme.typography.bodySmall)
                    else ChipRow(pinned, { false }, { it.title }, onKeyword)
                }
            }
            if (common.isNotEmpty()) {
                item { SectionCard("Common keywords") { ChipRow(common, { false }, { it.title }, onKeyword) } }
            }
            if (quick.isNotEmpty()) {
                item {
                    SectionCard("Quick reference") {
                        val tab = ui.rulesQuickTab.coerceIn(quick.indices)
                        ChipRow(quick.indices.toList(), { it == tab }, { quick[it].first }, { ui.rulesQuickTab = it; ui.rulesQuickOpen = null })
                        quick[tab].second.forEachIndexed { i, t ->
                            if (i > 0) HorizontalDivider(color = Palette.Text.copy(alpha = 0.15f))
                            val open = ui.rulesQuickOpen == t.title
                            val pinned = t.title in app.rulesPinned.value
                            Column(
                                Modifier.fillMaxWidth().clickable { ui.rulesQuickOpen = if (open) null else t.title }.padding(vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(t.title, fontWeight = FontWeight.Bold)
                                if (!open) {
                                    Text(brief(t.text), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                } else {
                                    KeywordText(t.text, style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (t.hasRule) AppButton("Open rule", { onOpen(t.page!!, t.anchor) }, kind = ButtonKind.Secondary, size = ButtonSize.S)
                                        AppButton(if (pinned) "Unpin" else "Pin", { app.togglePinned(t.title) }, kind = ButtonKind.Secondary, size = ButtonSize.S)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (app.rulesRecent.value.isNotEmpty()) {
                item {
                    SectionCard("Recent searches") {
                        ChipRow(app.rulesRecent.value, { false }, { it }, { ui.rulesQuery = it })
                        AppButton("Clear", { app.rulesRecent.update(emptyList()) }, kind = ButtonKind.Text)
                    }
                }
            }
            item { Text("Rule pages", style = MaterialTheme.typography.titleMedium, color = Palette.Accent) }
            data.pages.forEach { p ->
                val count = data.sections.count { it.page == p.id }
                val key = "rule:${p.id}"
                item {
                    SectionCard(
                        p.title, Modifier.clickable { onOpen(p.id, null) },
                        actions = { FavoriteButton(app.isFavorite(key)) { app.toggleFavorite(key) } },
                    ) {
                        Text("$count sections", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                SectionCard("Universal map key") {
                    mapKey.forEach { (icon, text) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ClickableImage(icon, Modifier.size(28.dp), ContentScale.Fit)
                            Text(text)
                        }
                    }
                }
            }
        }
    }
}

/** All keywords A-Z with a filter; the Index button jumps to a letter. */
@Composable
private fun GlossaryTab(app: AppState, onKeyword: (LookupTerm) -> Unit) {
    val ui = LocalUi.current
    val query = ui.glossaryQuery.trim()
    val all = remember(app.lookup) { RulesLookup.glossary(app.lookup) }
    val list = remember(all, query) { if (query.isEmpty()) all else all.filter { it.title.contains(query, ignoreCase = true) } }
    val groups = remember(list) { list.groupBy { it.title.first().uppercaseChar() }.toList() }
    val state = ui.list("rules-glossary")
    val scope = rememberCoroutineScope()
    var showLetters by remember { mutableStateOf(false) }
    // Items before the letters: tab switch and filter field.
    val firstIndex = remember(groups) {
        var next = 2
        groups.map { (_, entries) -> next.also { next += 1 + entries.size } }
    }

    if (showLetters) {
        JumpDialog("Letters", groups.map { it.first.toString() }, { scope.launch { state.scrollToItem(firstIndex[it]) } }, { showLetters = false })
    }
    Page("Rules", listState = state, onContents = { showLetters = true }) {
        item { RulesTabs() }
        item {
            OutlinedTextField(ui.glossaryQuery, { ui.glossaryQuery = it }, Modifier.fillMaxWidth(), label = { Text("Filter glossary (${list.size})") }, singleLine = true)
        }
        if (groups.isEmpty()) item { Text("No matches.") }
        groups.forEach { (letter, entries) ->
            item { Text(letter.toString(), style = MaterialTheme.typography.headlineSmall, color = Palette.Accent, fontWeight = FontWeight.Bold) }
            items(entries.size) { i ->
                val t = entries[i]
                SectionCard(t.title, Modifier.clickable { onKeyword(t) }) {
                    Text(brief(t.text, 110), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun snippet(s: RuleSection, query: String): String {
    val i = s.plain.indexOf(query, ignoreCase = true)
    if (i < 0) return s.plain.take(240)
    val from = (i - 80).coerceAtLeast(0)
    return (if (from > 0) "..." else "") + s.plain.substring(from, (i + 160).coerceAtMost(s.plain.length))
}

@Composable
private fun RulePageView(
    app: AppState,
    data: RulesData,
    pageId: String,
    target: String?,
    highlight: String?,
    onScrolled: () -> Unit,
    onBack: () -> Unit,
) {
    val sections = data.sections.filter { it.page == pageId }
    val scope = rememberCoroutineScope()
    val title = data.pages.firstOrNull { it.id == pageId }?.title ?: pageId
    val state = LocalUi.current.list("rules:$pageId")
    var showContents by remember { mutableStateOf(false) }
    LaunchedEffect(target, pageId) {
        if (target != null) {
            val index = sections.indexOfFirst { it.id.equals(target, ignoreCase = true) }
            if (index >= 0) state.scrollToItem(index)
            onScrolled()
        }
    }
    if (showContents) {
        JumpDialog(
            "Contents",
            sections.map { "    ".repeat((it.level - 2).coerceAtLeast(0)) + it.title },
            { scope.launch { state.scrollToItem(it) } },
            { showContents = false },
        )
    }

    Page(title, onBack, state, onContents = { showContents = true }) {
        items(sections.size) { i ->
            val s = sections[i]
            val favKey = "section:$pageId/${s.id}"
            SectionCard(
                s.title,
                Modifier.padding(start = ((s.level - 2).coerceAtLeast(0) * 12).dp),
                headerColor = if (s.id.equals(highlight, ignoreCase = true)) Palette.Highlight else Palette.Accent,
                actions = { FavoriteButton(app.isFavorite(favKey)) { app.toggleFavorite(favKey) } },
            ) {
                if (s.paras.isNotEmpty()) RichParas(s.paras)
                else if (s.plain.isNotBlank()) KeywordText(s.plain, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
