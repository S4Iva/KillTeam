package com.example.killteam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import com.example.killteam.data.RuleSection
import com.example.killteam.data.RulesData
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
        open != null -> RulePageView(data, open, ui.rulesTarget, ui.rulesHighlight, onScrolled = { ui.rulesTarget = null }) { ui.closeRulesPage() }
        else -> RulesIndex(data) { p, anchor -> ui.rulesPage = p; ui.rulesTarget = anchor; ui.rulesHighlight = anchor }
    }
}

@Composable
private fun RulesIndex(data: RulesData, onOpen: (String, String?) -> Unit) {
    val ui = LocalUi.current
    val query = ui.rulesQuery
    val results = if (query.length < 3) emptyList() else data.sections.filter {
        it.title.contains(query, ignoreCase = true) || it.plain.contains(query, ignoreCase = true)
    }.take(60)

    Page("Rules", listState = ui.list("rules-index")) {
        item {
            OutlinedTextField(query, { ui.rulesQuery = it }, Modifier.fillMaxWidth(), label = { Text("Search all rules (min. 3 letters)") }, singleLine = true)
        }
        if (query.length < 3) {
            data.pages.forEach { p ->
                val count = data.sections.count { it.page == p.id }
                item {
                    SectionCard(p.title, Modifier.clickable { onOpen(p.id, null) }) {
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
        } else {
            if (results.isEmpty()) item { Text("No matches.") }
            items(results.size) { i ->
                val s = results[i]
                val pageTitle = data.pages.firstOrNull { it.id == s.page }?.title ?: s.page
                SectionCard(s.title, Modifier.clickable { onOpen(s.page, s.id) }) {
                    Text(pageTitle, style = MaterialTheme.typography.bodySmall, color = Waha.Orange)
                    Text(snippet(s, query), maxLines = 4, style = MaterialTheme.typography.bodyMedium)
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
            SectionCard(
                s.title,
                Modifier.padding(start = ((s.level - 2).coerceAtLeast(0) * 12).dp),
                headerColor = if (s.id.equals(highlight, ignoreCase = true)) Waha.Orange else Waha.Black,
            ) {
                if (s.paras.isNotEmpty()) RichParas(s.paras)
                else if (s.plain.isNotBlank()) KeywordText(s.plain, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
