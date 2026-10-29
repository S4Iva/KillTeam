package com.example.killteam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.killteam.data.ApprovedOps
import com.example.killteam.data.KillTeam
import com.example.killteam.data.TacOp
import com.example.killteam.logic.AppState
import com.example.killteam.logic.GameState
import com.example.killteam.logic.OpKind
import com.example.killteam.logic.OperativeTrack
import com.example.killteam.logic.PlayerState
import com.example.killteam.logic.Roster
import com.example.killteam.logic.nowMillis

fun rosterToTracks(team: KillTeam?, roster: Roster?): List<OperativeTrack> {
    if (team == null || roster == null) return emptyList()
    val totals = roster.operatives.groupingBy { it }.eachCount()
    val seen = mutableMapOf<String, Int>()
    return roster.operatives.map { name ->
        val n = (seen[name] ?: 0) + 1
        seen[name] = n
        val wounds = team.operatives.firstOrNull { it.name == name }?.woundsValue ?: 0
        OperativeTrack(if ((totals[name] ?: 0) > 1) "$name $n" else name, wounds)
    }
}

@Composable
fun NewGameScreen(app: AppState, onOpenGenerator: () -> Unit) {
    var names by remember { mutableStateOf(listOf("Player 1", "Player 2")) }
    var teams by remember { mutableStateOf(listOf<KillTeam?>(null, null)) }
    var rosters by remember { mutableStateOf(listOf<Roster?>(null, null)) }
    val mission = app.mission.value

    Page("Game", listState = LocalUi.current.list("new-game")) {
        item {
            SectionCard("Mission") {
                Text("${mission.critOp.label}  |  ${mission.killzone.label}, Map ${mission.mapIndex + 1}")
                mission.tacOp?.let { Text("Tac op selected in generator: ${it.name}") }
                OutlinedButton(onOpenGenerator) { Text("Change mission") }
            }
        }
        for (i in 0..1) {
            item {
                SectionCard("Player ${i + 1}") {
                    OutlinedTextField(names[i], { v -> names = names.toMutableList().also { it[i] = v } }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                    Dropdown("Kill team", listOf<KillTeam?>(null) + app.teams, teams[i], { it?.name ?: "None" }, { t ->
                        teams = teams.toMutableList().also { it[i] = t }
                        rosters = rosters.toMutableList().also { it[i] = null }
                    })
                    val available = app.rosters.value.filter { it.teamSlug == teams[i]?.slug }
                    if (available.isNotEmpty()) {
                        Dropdown("Roster", listOf<Roster?>(null) + available, rosters[i], { it?.name ?: "None" }, { r ->
                            rosters = rosters.toMutableList().also { it[i] = r }
                        })
                    }
                }
            }
        }
        item {
            Button({
                val players = (0..1).map { i ->
                    val enemyRoster = rosters[1 - i]
                    PlayerState(
                        name = names[i].ifBlank { "Player ${i + 1}" },
                        teamSlug = teams[i]?.slug,
                        enemyStartCount = (enemyRoster?.operatives?.size ?: 6),
                        operatives = rosterToTracks(teams[i], rosters[i]),
                    )
                }
                val now = nowMillis()
                app.game.update(GameState(id = now, startedAt = now, mission = mission, players = players))
            }, Modifier.fillMaxWidth()) { Text("Start game") }
        }
    }
}

@Composable
fun SecretHandover(playerName: String, onReady: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Secret selection") },
        text = { Text("Hand the device to $playerName. The other player must not look.") },
        confirmButton = { Button(onReady) { Text("I am $playerName") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

/** Secret tac op and equipment selection for one player (pass-and-play). */
@Composable
fun SecretSelectionDialog(app: AppState, game: GameState, index: Int, onDismiss: () -> Unit) {
    val player = game.players[index]
    var ready by remember { mutableStateOf(false) }
    if (!ready) {
        SecretHandover(player.name, { ready = true }, onDismiss)
        return
    }
    val team = app.team(player.teamSlug)
    val archetypes = team?.archetypeSet.orEmpty()
    val tacOps: List<TacOp> = if (archetypes.isEmpty()) ApprovedOps.tacOps else ApprovedOps.tacOps.filter { it.archetype in archetypes }
    var tacId by remember { mutableStateOf(player.tacOpId) }
    var equipment by remember { mutableStateOf(player.equipment) }
    var custom by remember { mutableStateOf("") }
    val teamEquipment = team?.equipment?.map { it.name }.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${player.name}: secret selection") },
        text = {
            val scroll = rememberScrollState()
            Column(Modifier.heightIn(max = 460.dp).dragToScroll(scroll).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tac op (from the archetypes of the kill team)")
                ChipRow(tacOps, { it.id == tacId }, { "${it.name} (${it.archetype.label})" }, { tacId = it.id })
                tacOps.firstOrNull { it.id == tacId }?.let { OpSectionsView(it.sections) }
                Text("Equipment (${equipment.size}/${ApprovedOps.MAX_EQUIPMENT}, each option only once)")
                ChipRow(teamEquipment, { it in equipment }, { it }, { e ->
                    equipment = if (e in equipment) equipment - e else if (equipment.size < ApprovedOps.MAX_EQUIPMENT) equipment + e else equipment
                })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(custom, { custom = it }, Modifier.weight(1f), label = { Text("Other equipment") }, singleLine = true)
                    OutlinedButton({
                        val c = custom.trim()
                        if (c.isNotEmpty() && c !in equipment && equipment.size < ApprovedOps.MAX_EQUIPMENT) { equipment = equipment + c; custom = "" }
                    }) { Text("Add") }
                }
                equipment.filter { it !in teamEquipment }.forEach { e ->
                    TextButton({ equipment = equipment - e }) { Text("Remove: $e") }
                }
            }
        },
        confirmButton = {
            Button({
                app.game.update(game.updatePlayer(index) { it.copy(tacOpId = tacId, tacRevealed = false, equipment = equipment) })
                onDismiss()
            }, enabled = tacId != null) { Text("Confirm and hide") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

/** Strategic gambit of the first turning point: secretly select the primary op. */
@Composable
fun PrimaryOpDialog(app: AppState, game: GameState, index: Int, onDismiss: () -> Unit) {
    val player = game.players[index]
    var ready by remember { mutableStateOf(false) }
    if (!ready) {
        SecretHandover(player.name, { ready = true }, onDismiss)
        return
    }
    var choice by remember { mutableStateOf(player.primary) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${player.name}: primary op") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select one of your three ops. At the end of the battle you score half of its VP (rounded up) as a bonus.")
                ChipRow(OpKind.entries, { it == choice }, { it.label }, { choice = it })
                Text("1. Crit Op, 2. Kill Op, 3. Tac Op", modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = {
            Button({
                app.game.update(game.updatePlayer(index) { it.copy(primary = choice, primaryRevealed = false) })
                onDismiss()
            }, enabled = choice != null) { Text("Confirm and hide") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
