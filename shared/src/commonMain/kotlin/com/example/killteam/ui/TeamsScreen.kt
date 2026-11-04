package com.example.killteam.ui

import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.example.killteam.data.NamedText
import com.example.killteam.data.SearchEntry
import com.example.killteam.data.SearchKind
import kotlinx.coroutines.delay
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.example.killteam.data.ApprovedOps
import com.example.killteam.data.Archetype
import com.example.killteam.data.KillTeam
import com.example.killteam.data.Operative
import com.example.killteam.data.TeamSearch
import com.example.killteam.data.UniversalEquipment
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
        team != null -> TeamDetail(app, team, onBack = { ui.teamsSelected = null })
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
                AppButton("Rosters (${app.rosters.value.size})", onRosters, kind = ButtonKind.Secondary, fullWidth = true)
            }
        }
        if (app.teamData == null) {
            item { LoadStatus(app, "kill team data") }
        } else {
            item {
                Text("${teams.size} teams", style = MaterialTheme.typography.bodySmall)
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
                    Text("Rules version: ${team.version} (${team.updated})", style = MaterialTheme.typography.bodySmall, color = Palette.Accent)
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
internal fun NamedTextCard(item: NamedText, headerColor: Color) {
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
                    AppButton("Create", {
                        val r = Roster(nowMillis(), newName.ifBlank { "${chosen.name} roster" }, chosen.slug)
                        app.rosters.update(rosters + r)
                        newName = ""
                        onEdit(r)
                    }, fullWidth = true)
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
    var page by remember(roster.id) { mutableStateOf<RosterPage>(RosterPage.Overview) }
    val toOverview = { page = RosterPage.Overview }

    when (val p = page) {
        RosterPage.Overview -> RosterOverview(app, roster, team, save, onBack, onUnit = { page = RosterPage.Unit(it) }, onAdd = { page = RosterPage.Add })
        RosterPage.Add -> RosterAddPage(roster, team, save, toOverview)
        is RosterPage.Unit ->
            if (p.slot in roster.operatives.indices && team != null) RosterUnitPage(roster, team, p.slot, save, toOverview)
            else LaunchedEffect(Unit) { toOverview() }
    }
}

private sealed interface RosterPage {
    data object Overview : RosterPage
    data object Add : RosterPage
    data class Unit(val slot: Int) : RosterPage
}

/** Operatives and equipment budget; shown on every page of the roster editor. */
@Composable
private fun RosterBudget(roster: Roster, team: KillTeam?) {
    val expected = team?.let { RosterRules.expectedSize(it) } ?: 0
    val equipment = roster.equipment.size
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BudgetCell("Operatives", if (expected > 0) "${roster.operatives.size}/$expected" else roster.operatives.size.toString(), Modifier.weight(1f))
        BudgetCell("Equipment", "$equipment/${ApprovedOps.MAX_EQUIPMENT}", Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(ApprovedOps.MAX_EQUIPMENT) { i ->
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < equipment) Palette.Accent else Palette.Text.copy(alpha = 0.2f)))
                }
            }
        }
    }
}

@Composable
private fun BudgetCell(label: String, value: String, modifier: Modifier, extra: @Composable () -> Unit = {}) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(Palette.Card).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.Text.copy(alpha = 0.7f))
        extra()
    }
}

@Composable
private fun Pill(text: String) {
    Text(
        text, Modifier.clip(RoundedCornerShape(12.dp)).background(Palette.Equipment).padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall, color = Color.White,
    )
}

/** Round button with a drawn chevron that turns downwards when [expanded]. */
@Composable
private fun Chevron(expanded: Boolean, onClick: () -> Unit) {
    val angle by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(Palette.Accent.copy(alpha = 0.18f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (expanded) "Hide rules" else "Show rules" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(14.dp)) {
            rotate(angle) {
                val path = Path().apply {
                    moveTo(size.width * 0.3f, size.height * 0.1f)
                    lineTo(size.width * 0.75f, size.height * 0.5f)
                    lineTo(size.width * 0.3f, size.height * 0.9f)
                }
                drawPath(path, Palette.Accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** One selectable row: filled marker when chosen (round for one-of-many, square for many), title, optional text and tag. With [details] an arrow before the title unfolds them below the row. */
@Composable
private fun ChoiceRow(
    selected: Boolean, radio: Boolean, title: String, text: String?, tag: String?, enabled: Boolean,
    expanded: Boolean = false, onToggleDetails: (() -> Unit)? = null, details: (@Composable () -> Unit)? = null, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val marker = if (radio) CircleShape else RoundedCornerShape(5.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) Palette.Accent.copy(alpha = 0.15f) else Palette.Card)
            .border(1.dp, if (selected) Palette.Accent else Palette.Text.copy(alpha = 0.2f), shape),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(10.dp).alpha(if (enabled) 1f else 0.4f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(20.dp).clip(marker).border(2.dp, Palette.Accent, marker)
                    .padding(3.dp).background(if (selected) Palette.Accent else Color.Transparent, if (radio) CircleShape else RoundedCornerShape(2.dp)),
            )
            if (onToggleDetails != null) Chevron(expanded, onToggleDetails)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (!expanded && !text.isNullOrBlank()) Text(text, style = MaterialTheme.typography.bodySmall, color = Palette.Text.copy(alpha = 0.7f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (tag != null) Text(tag, style = MaterialTheme.typography.labelSmall, color = Palette.Highlight)
        }
        if (expanded && details != null) Box(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) { details() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RosterOverview(
    app: AppState, roster: Roster, team: KillTeam?, save: (Roster) -> Unit, onBack: () -> Unit, onUnit: (Int) -> Unit, onAdd: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmDialog("Delete roster \"${roster.name}\"?", "Delete roster", {
            app.rosters.update(app.rosters.value.filter { it.id != roster.id })
            onBack()
        }, { confirmDelete = false }, danger = true)
    }
    Page(roster.name, onBack) {
        item {
            SectionCard(team?.name ?: roster.teamSlug) {
                OutlinedTextField(roster.name, { save(roster.copy(name = it)) }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                RosterBudget(roster, team)
                team?.let { CompositionList(it, MaterialTheme.typography.bodySmall) }
                val problems = team?.let { RosterRules.problems(it, roster) }.orEmpty()
                if (problems.isEmpty()) Text("Roster size is valid. Check the selection limits in the text above.", color = MaterialTheme.colorScheme.primary)
                problems.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                team?.let { RosterRules.warnings(it, roster) }.orEmpty().forEach { Text(it, color = Palette.Highlight) }
            }
        }
        items(roster.operatives.size) { slot ->
            val name = roster.operatives[slot]
            val hasOptions = team?.weaponOptions?.get(name) != null
            val option = roster.optionAt(slot)
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(Palette.Card).border(1.dp, Palette.Text.copy(alpha = 0.2f), shape)
                    .clickable { onUnit(slot) }.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(roster.slotLabel(slot), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(">", color = Palette.Accent, fontWeight = FontWeight.Bold)
                }
                Text(
                    if (!hasOptions) "Standard weapons" else option.ifBlank { "Choose weapons" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasOptions && option.isBlank()) Palette.Highlight else Palette.Text.copy(alpha = 0.7f),
                )
                val carried = roster.equipmentOf(slot)
                if (carried.isEmpty()) Text("No equipment", style = MaterialTheme.typography.labelSmall, color = Palette.Text.copy(alpha = 0.5f))
                else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { carried.forEach { Pill(it) } }
            }
        }
        item { AppButton("+ Add operative", onAdd, kind = ButtonKind.Primary, size = ButtonSize.L, fullWidth = true) }
        item { AppButton("Delete roster", { confirmDelete = true }, kind = ButtonKind.Danger, fullWidth = true) }
    }
}

@Composable
private fun RosterAddPage(roster: Roster, team: KillTeam?, save: (Roster) -> Unit, onBack: () -> Unit) {
    val datacards = team?.operatives?.map { it.name }?.distinct().orEmpty()
    val expected = team?.let { RosterRules.expectedSize(it) } ?: 0
    Page("Add operatives", onBack) {
        item { RosterBudget(roster, team) }
        items(datacards.size) { i ->
            val name = datacards[i]
            val count = roster.operatives.count { it == name }
            SectionCard(name) {
                val options = team?.weaponOptions?.get(name)?.size ?: 0
                if (options > 0) Text("$options weapon options", style = MaterialTheme.typography.bodySmall)
                val room = if (expected > 0) (expected - roster.operatives.size).coerceAtLeast(0) else Int.MAX_VALUE - count
                Stepper("Selected", count, { v -> save(if (v > count) roster.withAdded(name) else roster.withRemovedLast(name)) }, 0, count + room)
            }
        }
        item {
            val tooMany = expected > 0 && roster.operatives.size > expected
            if (tooMany) Text("Too many operatives: $expected allowed, ${roster.operatives.size} selected.", color = MaterialTheme.colorScheme.error)
            AppButton("Done", onBack, kind = ButtonKind.Primary, size = ButtonSize.L, enabled = !tooMany, fullWidth = true)
        }
    }
}

@Composable
private fun RosterUnitPage(roster: Roster, team: KillTeam, slot: Int, save: (Roster) -> Unit, onBack: () -> Unit) {
    val name = roster.operatives[slot]
    val choices = team.weaponOptions[name]
    val max = ApprovedOps.MAX_EQUIPMENT
    var factionTab by remember(roster.id, slot) { mutableStateOf(true) }
    var custom by remember { mutableStateOf("") }
    var unfolded by remember(roster.id, slot) { mutableStateOf(setOf<String>()) }
    val factionNames = team.equipment.map { it.name }.sorted()
    val universalNames = ApprovedOps.universalEquipment + roster.equipment.filter { it !in factionNames && it !in ApprovedOps.universalEquipment }

    Page(roster.slotLabel(slot), onBack) {
        item { RosterBudget(roster, team) }
        item {
            SectionCard("Weapons") {
                if (choices == null) {
                    Text("All weapons of the datacard, no choice needed.", style = MaterialTheme.typography.bodySmall)
                } else {
                    choices.forEach { c ->
                        ChoiceRow(roster.optionAt(slot) == c.label, true, c.label, null, null, true) { save(roster.withOption(slot, c.label)) }
                    }
                }
            }
        }
        item {
            SectionCard("Equipment (${roster.equipmentOf(slot).size} here, ${roster.equipment.size}/$max in total)") {
                Text("Each equipment belongs to one operative. Tap one that sits on another operative to move it here.", style = MaterialTheme.typography.bodySmall)
                ChipRow(listOf(true, false), { it == factionTab }, { if (it) "Faction" else "Universal" }, { factionTab = it })
                val names = if (factionTab) factionNames else universalNames
                if (names.isEmpty()) Text("No faction equipment.", style = MaterialTheme.typography.bodySmall)
                names.forEach { e ->
                    val owner = roster.ownerOf(e)
                    val inRoster = e in roster.equipment
                    val mine = owner == slot
                    val locked = !inRoster && roster.equipment.size >= max
                    val item = team.equipment.firstOrNull { it.name == e } ?: UniversalEquipment.card(e)
                    ChoiceRow(
                        selected = mine, radio = false, title = e,
                        text = item?.let { i -> i.paras.firstOrNull { it.kind != "flavor" }?.text ?: i.text.substringAfter('\n') },
                        tag = when {
                            locked -> "limit reached"
                            owner != null && !mine -> "on ${roster.slotLabel(owner)}"
                            inRoster && owner == null -> "not assigned"
                            else -> null
                        },
                        enabled = !locked,
                        expanded = e in unfolded,
                        onToggleDetails = item?.let { { unfolded = if (e in unfolded) unfolded - e else unfolded + e } },
                        details = item?.let { i -> { if (i.paras.isNotEmpty()) RichParas(i.paras) else KeywordText(i.text) } },
                    ) { save(if (mine) roster.withoutEquipment(e) else roster.withEquipmentOn(slot, e)) }
                }
                if (!factionTab) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(custom, { custom = it }, Modifier.weight(1f), label = { Text("Other equipment") }, singleLine = true)
                        AppButton("Add", {
                            val c = custom.trim()
                            if (c.isNotEmpty()) { save(roster.withEquipmentOn(slot, c)); custom = "" }
                        }, kind = ButtonKind.Secondary, enabled = roster.equipment.size < max)
                    }
                }
            }
        }
        item { AppButton("Done", onBack, kind = ButtonKind.Primary, size = ButtonSize.L, fullWidth = true) }
        item {
            AppButton("Remove operative", { save(roster.withRemovedAt(slot)); onBack() }, kind = ButtonKind.Danger, fullWidth = true)
        }
    }
}
