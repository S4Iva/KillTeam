package com.example.killteam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.example.killteam.data.NamedText
import com.example.killteam.data.SearchEntry
import com.example.killteam.data.SearchKind
import kotlinx.coroutines.delay
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.killteam.data.Archetype
import com.example.killteam.data.KillTeam
import com.example.killteam.data.Operative
import com.example.killteam.data.TeamSearch
import com.example.killteam.logic.AppState
import com.example.killteam.logic.Roster
import com.example.killteam.logic.RosterRules
import com.example.killteam.logic.nowMillis

@Composable
fun TeamsScreen(app: AppState) {
    val ui = LocalUi.current
    val team = app.team(ui.teamsSelected)
    val roster = app.rosters.value.firstOrNull { it.id == ui.rosterEditing }
    when {
        roster != null -> RosterEditor(app, roster) { ui.rosterEditing = null }
        team != null -> TeamDetail(app, team, onBack = { if (ui.favoritesBack(Tab.Teams)) ui.leaveToFavorites() else ui.teamsSelected = null })
        ui.teamsPage == TeamsPage.Rosters -> RosterList(app, onBack = { ui.teamsPage = TeamsPage.List }, onEdit = { ui.rosterEditing = it.id })
        else -> TeamList(
            app,
            onOpen = { ui.teamTab = 0; ui.teamHit = null; ui.teamsSelected = it.slug },
            onOpenHit = { t, e -> ui.teamTab = detailTabFor(e.kind).ordinal; ui.teamHit = e; ui.teamsSelected = t.slug },
            onRosters = { ui.teamsPage = TeamsPage.Rosters },
        )
    }
}

@Composable
private fun TeamList(app: AppState, onOpen: (KillTeam) -> Unit, onOpenHit: (KillTeam, SearchEntry) -> Unit, onRosters: () -> Unit) {
    val ui = LocalUi.current
    val query = ui.teamQuery
    val archetype = ui.teamArchetype
    val onlyFavorites = ui.teamFavoritesOnly
    val teams = app.teams.filter { t ->
        TeamSearch.matches(t, query) &&
            (archetype == null || archetype in t.archetypeSet) &&
            (!onlyFavorites || app.isFavorite("team:${t.slug}"))
    }

    Page("Kill Teams", listState = ui.list("teams")) {
        item {
            SectionCard("Search") {
                OutlinedTextField(
                    query, { ui.teamQuery = it }, Modifier.fillMaxWidth(),
                    label = { Text("Search words in teams, operatives, rules, ploys, equipment (min. ${TeamSearch.MIN_LENGTH} letters)") },
                    singleLine = true,
                )
                ChipRow(listOf<Archetype?>(null) + Archetype.entries, { it == archetype }, { it?.label ?: "All archetypes" }, { ui.teamArchetype = it })
                ChipRow(listOf(true), { onlyFavorites }, { "Favorites only" }, { ui.teamFavoritesOnly = !onlyFavorites })
                OutlinedButton(onRosters) { Text("Rosters (${app.rosters.value.size})") }
            }
        }
        if (app.teamData == null) {
            item { LoadStatus(app, "kill team data") }
        } else {
            item {
                Text("${teams.size} teams  |  data imported ${app.teamData?.importedAt} from ${app.teamData?.source}", style = MaterialTheme.typography.bodySmall)
            }
        }
        items(teams.size) { i ->
            val t = teams[i]
            SectionCard(
                t.name,
                Modifier.clickable { onOpen(t) },
                actions = { FavoriteButton(app.isFavorite("team:${t.slug}")) { app.toggleFavorite("team:${t.slug}") } },
            ) {
                Text(t.archetypes.joinToString(" / ").ifEmpty { "-" } + "  |  ${t.operatives.size} datacards", style = MaterialTheme.typography.bodySmall)
                val hits = TeamSearch.hitEntries(t, query)
                hits.take(6).forEach { e ->
                    Text(
                        "> ${e.label}",
                        Modifier.fillMaxWidth().clickable { onOpenHit(t, e) }.padding(vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium, color = Palette.Accent, fontWeight = FontWeight.SemiBold,
                    )
                }
                if (hits.size > 6) Text("+${hits.size - 6} more", style = MaterialTheme.typography.bodySmall, color = Palette.Accent)
                if (hits.isEmpty() && t.description.isNotBlank()) Text(t.description, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private enum class DetailTab(val label: String) { Datacards("Datacards"), Rules("Faction rules"), Ploys("Ploys"), Equipment("Equipment"), Tokens("Markers") }

private fun detailTabFor(kind: SearchKind) = when (kind) {
    SearchKind.Team, SearchKind.Operative -> DetailTab.Datacards
    SearchKind.FactionRule -> DetailTab.Rules
    SearchKind.StrategyPloy, SearchKind.FirefightPloy -> DetailTab.Ploys
    SearchKind.Equipment -> DetailTab.Equipment
}

/** Position of the hit in the detail list, counted from the first item below the chip row. */
private fun hitOffset(team: KillTeam, e: SearchEntry): Int = when (e.kind) {
    SearchKind.Team, SearchKind.Operative, SearchKind.Equipment -> e.index.coerceAtLeast(0)
    SearchKind.FactionRule -> e.index.coerceAtLeast(0)
    SearchKind.StrategyPloy -> 1 + e.index
    SearchKind.FirefightPloy -> 2 + team.strategyPloys.size + e.index
}

private fun SearchEntry?.isAt(kind: SearchKind, index: Int) = this != null && this.kind == kind && this.index == index

@Composable
private fun TeamDetail(app: AppState, team: KillTeam, onBack: () -> Unit) {
    val ui = LocalUi.current
    val tab = DetailTab.entries[ui.teamTab.coerceIn(0, DetailTab.entries.lastIndex)]
    val setTab: (DetailTab) -> Unit = { ui.teamTab = it.ordinal }
    var dialog by remember { mutableStateOf<String?>(null) }
    var scrollTo by remember { mutableStateOf<Int?>(null) }
    val state = ui.list("team:${team.slug}")
    val key = "team:${team.slug}"
    val leadingItems = 2

    val hit = ui.teamHit
    var highlighted by remember { mutableStateOf<SearchEntry?>(null) }
    LaunchedEffect(hit) {
        if (hit != null) {
            highlighted = hit
            state.scrollToItem(if (hit.kind == SearchKind.Team) 0 else leadingItems + hitOffset(team, hit))
            ui.teamHit = null
        }
    }
    LaunchedEffect(highlighted) {
        if (highlighted != null) {
            delay(3000)
            highlighted = null
        }
    }

    LaunchedEffect(scrollTo, tab) {
        scrollTo?.let { state.scrollToItem(it); scrollTo = null }
    }
    if (dialog == "contents") {
        JumpDialog("Contents", DetailTab.entries.map { it.label }, { setTab(DetailTab.entries[it]); scrollTo = leadingItems }, { dialog = null })
    }
    if (dialog == "list") {
        JumpDialog("Datacards", team.operatives.map { it.name }, { setTab(DetailTab.Datacards); scrollTo = leadingItems + it }, { dialog = null })
    }

    Page(team.name, onBack, state, onContents = { dialog = "contents" }, onList = { dialog = "list" }) {
        item {
            SectionCard(
                team.archetypes.joinToString(" / ").ifEmpty { "Kill team" },
                actions = { FavoriteButton(app.isFavorite(key)) { app.toggleFavorite(key) } },
            ) {
                if (team.description.isNotBlank()) Text(team.description)
                if (team.updated.isNotBlank()) {
                    Text("Rules version: ${team.version} (${team.updated}), imported ${app.teamData?.importedAt}", style = MaterialTheme.typography.bodySmall, color = Palette.Accent)
                }
                CompositionList(team)
                NoteField(key, app.note(key)) { app.setNote(key, it) }
            }
        }
        item { ChipRow(DetailTab.entries, { it == tab }, { it.label }, { setTab(it) }) }
        when (tab) {
            DetailTab.Datacards -> items(team.operatives.size) { HitFrame(highlighted.isAt(SearchKind.Operative, it)) { OperativeDatacard(team.operatives[it]) } }
            DetailTab.Rules -> {
                if (team.factionRuleItems.isEmpty()) item { HitFrame(highlighted?.kind == SearchKind.FactionRule) { SectionCard("Faction rules") { KeywordText(team.factionRules.ifBlank { "-" }) } } }
                items(team.factionRuleItems.size) { HitFrame(highlighted.isAt(SearchKind.FactionRule, it)) { NamedTextCard(team.factionRuleItems[it], Palette.Accent) } }
            }
            DetailTab.Ploys -> {
                item { Text("STRATEGY PLOYS", style = MaterialTheme.typography.titleMedium, color = Palette.Accent) }
                items(team.strategyPloys.size) { HitFrame(highlighted.isAt(SearchKind.StrategyPloy, it)) { NamedTextCard(team.strategyPloys[it], Palette.StrategyPloy) } }
                item { Text("FIREFIGHT PLOYS", style = MaterialTheme.typography.titleMedium, color = Palette.Accent) }
                items(team.firefightPloys.size) { HitFrame(highlighted.isAt(SearchKind.FirefightPloy, it)) { NamedTextCard(team.firefightPloys[it], Palette.FirefightPloy) } }
            }
            DetailTab.Equipment -> items(team.equipment.size) { HitFrame(highlighted.isAt(SearchKind.Equipment, it)) { NamedTextCard(team.equipment[it], Palette.Equipment) } }
            DetailTab.Tokens -> items(team.tokens.size) { i ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TokenImage(team.tokens[i].image, 56.dp)
                    Text(team.tokens[i].name)
                }
            }
        }
    }
}

@Composable
private fun NamedTextCard(item: NamedText, headerColor: Color) {
    SectionCard(
        item.name,
        headerColor = headerColor,
        actions = { if (item.cost.isNotBlank()) Text(item.cost, color = Color.White, fontWeight = FontWeight.Bold) },
    ) {
        if (item.paras.isNotEmpty()) RichParas(item.paras) else KeywordText(item.text)
    }
}

@Composable
private fun RosterList(app: AppState, onBack: () -> Unit, onEdit: (Roster) -> Unit) {
    var newName by remember { mutableStateOf("") }
    var teamChoice by remember { mutableStateOf<KillTeam?>(null) }
    val rosters = app.rosters.value
    val chosen = teamChoice ?: app.teams.firstOrNull()
    Page("Rosters", onBack) {
        item {
            SectionCard("New roster") {
                OutlinedTextField(newName, { newName = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                if (chosen == null) {
                    LoadStatus(app, "kill team data")
                } else {
                    Dropdown("Kill team", app.teams, chosen, { it.name }, { teamChoice = it })
                    Button({
                        val r = Roster(nowMillis(), newName.ifBlank { "${chosen.name} roster" }, chosen.slug)
                        app.rosters.update(rosters + r)
                        newName = ""
                        onEdit(r)
                    }) { Text("Create") }
                }
            }
        }
        items(rosters.size) { i ->
            val r = rosters[i]
            val team = app.team(r.teamSlug)
            SectionCard(r.name, Modifier.clickable { onEdit(r) }) {
                Text("${team?.name ?: r.teamSlug}  |  ${r.operatives.size} operatives")
                team?.let { t -> RosterRules.problems(t, r).forEach { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        }
    }
}

@Composable
private fun RosterEditor(app: AppState, initial: Roster, onBack: () -> Unit) {
    val roster = app.rosters.value.firstOrNull { it.id == initial.id } ?: initial
    val team = app.team(roster.teamSlug)
    val save: (Roster) -> Unit = { r -> app.rosters.update(app.rosters.value.map { if (it.id == r.id) r else it }) }

    Page(roster.name, onBack) {
        item {
            SectionCard("Roster") {
                OutlinedTextField(roster.name, { save(roster.copy(name = it)) }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                Text("${team?.name ?: roster.teamSlug}: ${roster.operatives.size} operatives" + (team?.let { " (expected ${RosterRules.expectedSize(it)})" } ?: ""))
                team?.let { CompositionList(it, MaterialTheme.typography.bodySmall) }
                val problems = team?.let { RosterRules.problems(it, roster) }.orEmpty()
                if (problems.isEmpty()) Text("Roster size is valid. Check the selection limits in the text above.", color = MaterialTheme.colorScheme.primary)
                problems.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton({
                    app.rosters.update(app.rosters.value.filter { it.id != roster.id })
                    onBack()
                }) { Text("Delete roster") }
            }
        }
        val distinctOps = team?.operatives?.map { it.name }?.distinct().orEmpty()
        items(distinctOps.size) { i ->
            val name = distinctOps[i]
            val count = roster.operatives.count { it == name }
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, Modifier.weight(1f))
                OutlinedButton({
                    val idx = roster.operatives.indexOfLast { it == name }
                    if (idx >= 0) save(roster.copy(operatives = roster.operatives.toMutableList().also { it.removeAt(idx) }))
                }, enabled = count > 0) { Text("-") }
                Text(count.toString(), Modifier.padding(horizontal = 12.dp))
                OutlinedButton({ save(roster.copy(operatives = roster.operatives + name)) }) { Text("+") }
            }
        }
    }
}
