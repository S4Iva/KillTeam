package com.example.killteam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.killteam.data.ApprovedOps
import com.example.killteam.data.Archetype
import com.example.killteam.data.KillOp
import com.example.killteam.data.KillTeam
import com.example.killteam.data.tacOpsFor
import com.example.killteam.logic.AppState
import com.example.killteam.logic.GameState
import com.example.killteam.logic.OpKind
import com.example.killteam.logic.OperativeTrack
import com.example.killteam.logic.PlayerState
import com.example.killteam.logic.Roster
import com.example.killteam.logic.RosterRules
import com.example.killteam.logic.nowMillis

fun rosterToTracks(team: KillTeam?, roster: Roster?): List<OperativeTrack> {
    if (team == null || roster == null) return emptyList()
    val totals = roster.operatives.groupingBy { it }.eachCount()
    val seen = mutableMapOf<String, Int>()
    return roster.operatives.mapIndexed { index, name ->
        val n = (seen[name] ?: 0) + 1
        seen[name] = n
        val wounds = team.operatives.firstOrNull { it.name == name }?.woundsValue ?: 0
        OperativeTrack(
            if ((totals[name] ?: 0) > 1) "$name $n" else name, wounds,
            card = name, weapons = RosterRules.weaponsOf(team, name, roster.optionAt(index)),
            equipment = roster.equipmentOf(index),
        )
    }
}

/** Starting state of a player; without a roster the operatives and equipment come from the team selection ([NoRosterOperatives]). */
fun startPlayer(name: String, team: KillTeam?, roster: Roster?, enemyStartCount: Int) = PlayerState(
    name = name,
    teamSlug = team?.slug,
    enemyStartCount = enemyStartCount,
    operatives = rosterToTracks(team, roster),
    equipment = roster?.equipment.orEmpty(),
    rosterEquipment = roster?.unassignedEquipment.orEmpty(),
)

@Composable
fun NewGameScreen(app: AppState, onOpenGenerator: () -> Unit) {
    var names by remember { mutableStateOf(listOf("Player 1", "Player 2")) }
    var teams by remember { mutableStateOf(listOf<KillTeam?>(null, null)) }
    var rosters by remember { mutableStateOf(listOf<Roster?>(null, null)) }
    // Operatives picked for a team without a roster; a temporary roster that is never saved.
    var picks by remember { mutableStateOf(listOf<Roster?>(null, null)) }
    var editing by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var sizes by remember { mutableStateOf(listOf(6, 6)) }
    val mission = app.mission.value
    val setPick = { i: Int, r: Roster? ->
        picks = picks.toMutableList().also { it[i] = r }
        teams[i]?.let { t -> sizes = sizes.toMutableList().also { it[i] = RosterRules.gameSize(t, r) } }
    }
    val chosenRoster = { i: Int -> rosters[i] ?: picks[i]?.takeIf { it.operatives.isNotEmpty() } }

    editing?.let { (pi, slot) ->
        val t = teams[pi]
        val r = picks[pi]
        if (t != null && r != null && slot in r.operatives.indices) {
            RosterUnitPage(r, t, slot, { setPick(pi, it) }, { editing = null }, showWeapons = false)
            return
        }
    }

    Page("Game", listState = LocalUi.current.list("new-game")) {
        item {
            SectionCard("Mission") {
                MissionSummary(mission)
                AppButton("Change mission", onOpenGenerator, kind = ButtonKind.Secondary, fullWidth = true)
            }
        }
        for (i in 0..1) {
            item {
                SectionCard("Player ${i + 1}") {
                    OutlinedTextField(names[i], { v -> names = names.toMutableList().also { it[i] = v } }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                    Dropdown("Kill team", listOf<KillTeam?>(null) + app.teams, teams[i], { it?.name ?: "None" }, { t ->
                        teams = teams.toMutableList().also { it[i] = t }
                        rosters = rosters.toMutableList().also { it[i] = null }
                        picks = picks.toMutableList().also { it[i] = null }
                        sizes = sizes.toMutableList().also { it[i] = RosterRules.gameSize(t, null) }
                    })
                    val available = app.rosters.value.filter { it.teamSlug == teams[i]?.slug }
                    if (available.isNotEmpty()) {
                        Dropdown("Roster", listOf<Roster?>(null) + available, rosters[i], { it?.name ?: "None" }, { r ->
                            rosters = rosters.toMutableList().also { it[i] = r }
                            picks = picks.toMutableList().also { it[i] = null }
                            sizes = sizes.toMutableList().also { it[i] = RosterRules.gameSize(teams[i], r) }
                        })
                    }
                    teams[i]?.let { t ->
                        if (rosters[i] == null) {
                            NoRosterOperatives(t, picks[i] ?: Roster(i + 1L, "", t.slug), { r -> setPick(i, r) }, { slot -> editing = i to slot })
                        }
                        Stepper("Team size", sizes[i], { v -> sizes = sizes.toMutableList().also { it[i] = v } }, KillOp.minOperatives, KillOp.maxOperatives)
                        Text("Size from the team rules and roster; it sets the kill grade row of the opponent.", style = MaterialTheme.typography.bodySmall)
                        Text("Tac ops: ${tacOpsFor(t).map { it.archetype.label }.distinct().joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            AppButton("Start game", {
                val players = (0..1).map { i ->
                    startPlayer(names[i].ifBlank { "Player ${i + 1}" }, teams[i], chosenRoster(i), sizes[1 - i])
                }
                val now = nowMillis()
                app.game.update(GameState(id = now, startedAt = now, mission = mission, players = players))
            }, fullWidth = true)
        }
    }
}

@Composable
fun SecretHandover(playerName: String, onReady: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Secret selection") },
        text = { Text("Hand the device to $playerName. The other player must not look.") },
        confirmButton = { AppButton("I am $playerName", onReady, size = ButtonSize.L) },
        dismissButton = { AppButton("Cancel", onDismiss, kind = ButtonKind.Text) },
    )
}

/** Secret tac op selection for one player (pass-and-play); without a kill team the equipment is still entered here. */
@Composable
fun SecretSelectionPage(app: AppState, game: GameState, index: Int, onClose: () -> Unit) {
    val player = game.players[index]
    val team = app.team(player.teamSlug)
    val equipmentFixed = team != null
    val teamTacOps = remember(team) { tacOpsFor(team) }
    var filter by remember { mutableStateOf<Archetype?>(null) }
    var tacId by remember { mutableStateOf(player.tacOpId) }
    var equipment by remember { mutableStateOf(player.equipment) }
    var custom by remember { mutableStateOf("") }
    val teamEquipment = team?.equipment?.map { it.name }?.sorted().orEmpty()
    val maxEquipment = ApprovedOps.MAX_EQUIPMENT
    val selected = ApprovedOps.tacOps.firstOrNull { it.id == tacId }
    val visible = teamTacOps.filter { (filter == null || it.archetype == filter) && it.id != tacId }
    val toggle: (String) -> Unit = { e ->
        equipment = if (e in equipment) equipment - e else if (equipment.size < maxEquipment) equipment + e else equipment
    }

    Page("${player.name}: secret selection", onBack = onClose) {
        item {
            SectionCard("Your selection") {
                Text("Tac op: ${selected?.name ?: "none selected"}")
                if (!equipmentFixed) {
                    Text("Equipment: ${equipment.size}/$maxEquipment")
                    equipment.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton("Cancel", onClose, Modifier.weight(1f), ButtonKind.Secondary)
                    AppButton("Confirm and hide", {
                        app.game.update(game.updatePlayer(index) { it.copy(tacOpId = tacId, tacRevealed = false, equipment = equipment) })
                        onClose()
                    }, Modifier.weight(1f), enabled = tacId != null)
                }
            }
        }
        if (equipmentFixed) item {
            SectionCard("Equipment (${player.equipment.size}/$maxEquipment)") {
                Text("Equipment is set at the operatives.", style = MaterialTheme.typography.bodySmall)
                player.operatives.forEach { o -> o.equipment.forEach { Text("- $it (${o.name})", style = MaterialTheme.typography.bodySmall) } }
                player.rosterEquipment.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
                if (player.equipment.isEmpty()) Text("No equipment.", style = MaterialTheme.typography.bodySmall)
            }
        } else item {
            SectionCard("Equipment (${equipment.size}/$maxEquipment)") {
                Text("Select up to $maxEquipment options, each option only once.")
                if (teamEquipment.isNotEmpty()) {
                    Text("Faction equipment", fontWeight = FontWeight.Bold, color = Palette.Accent)
                    ChipRow(teamEquipment, { it in equipment }, { it }, toggle)
                }
                Text("Universal equipment", fontWeight = FontWeight.Bold, color = Palette.Accent)
                ChipRow(ApprovedOps.universalEquipment, { it in equipment }, { it }, toggle)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(custom, { custom = it }, Modifier.weight(1f), label = { Text("Other equipment") }, singleLine = true)
                    AppButton("Add", {
                        val c = custom.trim()
                        if (c.isNotEmpty() && c !in equipment && equipment.size < maxEquipment) { equipment = equipment + c; custom = "" }
                    }, kind = ButtonKind.Secondary)
                }
                equipment.filter { it !in teamEquipment && it !in ApprovedOps.universalEquipment }.forEach { e ->
                    AppButton("Remove: $e", { equipment = equipment - e }, kind = ButtonKind.Text)
                }
            }
        }
        item {
            SectionCard("Tac op") {
                Text("Select one tac op from the archetypes of your kill team.")
                val archetypes = teamTacOps.map { it.archetype }.distinct()
                ChipRow(listOf<Archetype?>(null) + archetypes, { it == filter }, { it?.label ?: "All archetypes" }, { filter = it })
            }
        }
        if (selected != null) {
            item { TacOpCard(selected, app, true, onRemove = { tacId = null }) }
        }
        items(visible.size) { i ->
            val op = visible[i]
            TacOpCard(op, app, false, onSelect = { tacId = op.id })
        }
    }
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
            AppButton("Confirm and hide", {
                app.game.update(game.updatePlayer(index) { it.copy(primary = choice, primaryRevealed = false) })
                onDismiss()
            }, enabled = choice != null)
        },
        dismissButton = { AppButton("Cancel", onDismiss, kind = ButtonKind.Text) },
    )
}
