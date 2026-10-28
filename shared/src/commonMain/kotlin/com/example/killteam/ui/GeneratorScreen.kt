package com.example.killteam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.killteam.data.ApprovedOps
import com.example.killteam.data.Archetype
import com.example.killteam.data.KillOp
import com.example.killteam.data.Killzone
import com.example.killteam.data.MAPS_PER_KILLZONE
import com.example.killteam.data.TacOp
import com.example.killteam.logic.AppState
import com.example.killteam.logic.MissionGenerator
import org.jetbrains.compose.resources.painterResource

@Composable
fun TacOpCard(
    op: TacOp,
    app: AppState,
    selected: Boolean,
    onSelect: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val key = "tac:${op.id}"
    SectionCard(
        op.name,
        headerColor = Waha.archetype(op.archetype),
        icon = tacOpIcon(op.archetype),
        actions = {
            if (selected && onRemove != null) {
                Image(painterResource(removeIcon), "Remove", Modifier.size(36.dp).clickable { onRemove() })
            } else if (!selected && onSelect != null) {
                Image(painterResource(selectIcon), "Select", Modifier.size(36.dp).clickable { onSelect() })
            }
        },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TAC OP  |  ${op.archetype.label}", color = Waha.archetype(op.archetype), fontWeight = FontWeight.Bold)
            FavoriteButton(app.isFavorite(key)) { app.toggleFavorite(key) }
        }
        Row {
            Text("REVEAL  ", color = Waha.Orange, fontWeight = FontWeight.Bold)
            KeywordText(op.reveal)
        }
        OpSectionsView(op.sections)
        if (op.id == "flank") {
            ClickableImage(reconFlankImage, Modifier.fillMaxWidth())
        }
        NoteField(key, app.note(key)) { app.setNote(key, it) }
    }
}

@Composable
fun GeneratorScreen(app: AppState, onStartGame: () -> Unit) {
    val state = app.mission.value
    var filter by remember { mutableStateOf<Archetype?>(null) }
    var showKey by remember { mutableStateOf(false) }
    var showContents by remember { mutableStateOf(false) }
    val listState = LocalUi.current.list("generator")
    val scope = rememberCoroutineScope()
    if (showContents) {
        JumpDialog(
            "Contents",
            listOf("Mission", "Crit op", "Map", "Kill op", "Selected tac op", "Archetype tac ops"),
            { scope.launch { listState.scrollToItem(it) } },
            { showContents = false },
        )
    }

    Page("Mission Generator", listState = listState, onContents = { showContents = true }) {
        item {
            SectionCard("Approved Ops 2025") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ app.mission.update(MissionGenerator.random(app.mission.value)) }) { Text("Generate Mission") }
                    OutlinedButton(onStartGame) { Text("Start game") }
                }
                Dropdown("Mission", ApprovedOps.critOps, state.critOp, { it.label }, { app.mission.update(state.withCritOp(it.index)) })
                Dropdown("Killzone", Killzone.entries, state.killzone, { it.label }, { app.mission.update(state.withKillzone(it)) })
                Dropdown("Map", (0 until MAPS_PER_KILLZONE).toList(), state.mapIndex, { "${it + 1}. Map ${it + 1}" }, { app.mission.update(state.withMap(it)) })
            }
        }
        item {
            val key = "crit:${state.critOp.index}"
            SectionCard(
                "CRIT OP  ${state.critOp.label}",
                actions = { FavoriteButton(app.isFavorite(key)) { app.toggleFavorite(key) } },
            ) {
                OpSectionsView(state.critOp.sections)
                NoteField(key, app.note(key)) { app.setNote(key, it) }
            }
        }
        item {
            SectionCard("MAP  ${state.killzone.label} - Map ${state.mapIndex + 1}") {
                ClickableImage(mapResource(state.killzone, state.mapIndex), Modifier.fillMaxWidth())
                Text("Tap the map to enlarge it.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                OutlinedButton({ showKey = !showKey }) { Text(if (showKey) "Hide map key" else "Show map key") }
                if (showKey) {
                    mapKey.forEach { (icon, text) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ClickableImage(icon, Modifier.size(28.dp), androidx.compose.ui.layout.ContentScale.Fit)
                            Text(text)
                        }
                    }
                }
            }
        }
        item {
            SectionCard("KILL OP") {
                KillOp.rules.forEach { KeywordText(it) }
                Text("Incapacitated enemy operatives needed per kill grade (rows: starting number of enemy operatives):")
                ClickableImage(killGradeImage, Modifier.fillMaxWidth())
            }
        }
        item {
            SectionCard("SELECTED TAC OP") {
                val selected = state.tacOp
                if (selected == null) Text("No tac op selected. Select one below.")
                else TacOpCard(selected, app, true, onRemove = { app.mission.update(state.withTacOp(null)) })
            }
        }
        item {
            SectionCard("ARCHETYPE TAC OPS") {
                ChipRow(listOf<Archetype?>(null) + Archetype.entries, { it == filter }, { it?.label ?: "No filter" }, { filter = it })
            }
        }
        val visible = ApprovedOps.tacOps.filter { (filter == null || it.archetype == filter) && it.id != state.tacOpId }
        items(visible.size) { i ->
            val op = visible[i]
            TacOpCard(op, app, false, onSelect = { app.mission.update(state.withTacOp(op.id)) })
        }
    }
}
