package com.example.killteam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.killteam.data.KillOp
import com.example.killteam.logic.AppState
import com.example.killteam.logic.GamePhase
import com.example.killteam.logic.GameState
import com.example.killteam.logic.InitiativeCard
import com.example.killteam.logic.Scoring
import com.example.killteam.logic.advanceTurningPoint
import com.example.killteam.logic.changeOperative
import com.example.killteam.logic.chooseInitiative
import com.example.killteam.logic.chooseSetupInitiative
import com.example.killteam.logic.finish
import com.example.killteam.logic.readyGains
import com.example.killteam.logic.readyStep
import com.example.killteam.logic.reopenRollOff
import com.example.killteam.logic.resolveRollOff
import com.example.killteam.logic.rollOffResolved
import com.example.killteam.logic.setIncapacitated
import com.example.killteam.logic.toResult
import com.example.killteam.logic.useModifier
import com.example.killteam.logic.useReroll
import com.example.killteam.logic.withRoll

@Composable
fun GameScreen(app: AppState, onOpenGenerator: () -> Unit) {
    val game = app.game.value
    if (game == null) NewGameScreen(app, onOpenGenerator) else RunningGame(app, game)
}

private val phaseChecklist = mapOf(
    GamePhase.SetUpBattle to listOf(
        "Each player selects a kill team for the battle.",
        "Determine the killzone and set up terrain features. Ensure terrain types are specified.",
        "Determine one shared crit op and set up objective markers (all on the killzone floor, except in Killzone: Bheta-Decima).",
        "Roll-off: the winner decides who has initiative.",
        "The player with initiative selects one drop zone. Their opponent has the other drop zone and gains the Re-roll initiative card.",
    ),
    GamePhase.SelectOperatives to listOf(
        "Each player secretly selects their operatives, then reveals simultaneously.",
        "Each player secretly selects up to four equipment options (each only once), then reveals simultaneously.",
        "Each player starts with 2CP.",
        "Each player secretly selects a tac op from one of their kill team's archetypes.",
    ),
    GamePhase.SetUpOperatives to listOf(
        "Players alternate setting up an item of equipment that's set up before the battle (item by item), starting with the player with initiative.",
        "Players alternate setting up one third of their kill team (rounded up), starting with the player with initiative.",
        "Operatives must be wholly within their drop zone and must be given a Conceal order.",
    ),
)

private class PendingConfirm(val text: String, val label: String, val action: () -> Unit)

@Composable
fun RunningGame(app: AppState, game: GameState) {
    val update: (GameState) -> Unit = { app.game.update(it) }
    var secretFor by remember { mutableStateOf<Int?>(null) }
    var primaryFor by remember { mutableStateOf<Int?>(null) }
    var showMission by remember { mutableStateOf(false) }
    var showEquipment by remember(game.id, game.phase) { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<PendingConfirm?>(null) }

    secretFor?.let { SecretSelectionDialog(app, game, it) { secretFor = null } }
    primaryFor?.let { PrimaryOpDialog(app, game, it) { primaryFor = null } }
    confirm?.let { c -> ConfirmDialog(c.text, c.label, c.action) { confirm = null } }

    Page("Game - Turning Point ${game.turningPoint}/${ApprovedOps.TURNING_POINTS}", listState = LocalUi.current.list("game")) {
        item {
            SectionCard("Phase") {
                ChipRow(GamePhase.entries, { it == game.phase }, { it.label }, { target ->
                    if (target == GamePhase.End && game.phase != GamePhase.End) {
                        confirm = PendingConfirm("Jump to \"${target.label}\"? The battle then counts as ended.", "End the battle") {
                            update(game.copy(phase = target, ended = true))
                        }
                    } else {
                        update(game.copy(phase = target, ended = false))
                    }
                })
                val next = GamePhase.entries.getOrNull(game.phase.ordinal + 1)
                if (next != null && game.phase != GamePhase.Play) {
                    Button({ update(game.copy(phase = next)) }) { Text("Next: ${next.label}") }
                }
            }
        }
        item { Scoreboard(game) }

        phaseChecklist[game.phase]?.let { lines ->
            item { SectionCard(game.phase.label) { lines.forEach { KeywordText("- $it") } } }
        }

        when (game.phase) {
            GamePhase.SetUpBattle -> {
                item {
                    SectionCard("Initiative and drop zones") {
                        Text("Roll-off winner decides who has initiative:")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            game.players.forEachIndexed { i, p ->
                                Button({ update(game.chooseSetupInitiative(i)) }) { Text("${p.name} has initiative") }
                            }
                        }
                        Text("Initiative: ${game.players[game.initiative].name}. ${game.players[1 - game.initiative].name} holds the Re-roll initiative card.")
                    }
                }
                item { MissionCard(game, showMission) { showMission = it } }
            }

            GamePhase.SelectOperatives -> {
                game.players.forEachIndexed { i, p ->
                    item {
                        SectionCard(p.name) {
                            Stepper("Kill team size", game.players[1 - i].enemyStartCount, { v -> update(game.updatePlayer(1 - i) { it.copy(enemyStartCount = v) }) }, KillOp.minOperatives, KillOp.maxOperatives)
                            Text("(Sets the kill grade row of ${game.players[1 - i].name}.)")
                            Stepper("CP", p.cp, { v -> update(game.updatePlayer(i) { it.copy(cp = v) }) }, 0, 20)
                            Text(if (p.tacOpId != null) "Tac op selected (hidden), equipment ${p.equipment.size}/${ApprovedOps.MAX_EQUIPMENT}" else "No secret selection yet.")
                            OutlinedButton({ secretFor = i }) { Text("Secret selection (tac op, equipment)") }
                        }
                    }
                }
                item {
                    SectionCard("Reveal equipment") {
                        OutlinedButton({ showEquipment = !showEquipment }) { Text(if (showEquipment) "Hide" else "Reveal simultaneously") }
                        if (showEquipment) game.players.forEach { p ->
                            Text("${p.name}: ${p.equipment.joinToString().ifEmpty { "-" }}")
                        }
                    }
                }
            }

            GamePhase.SetUpOperatives -> Unit

            GamePhase.Play -> {
                item { InitiativePanel(game, update) }
                item { ReadyStepPanel(game, update) }
                if (game.turningPoint == 1) {
                    item {
                        SectionCard("Strategic gambit: primary op") {
                            Text("In the first turning point each player secretly selects one of their three ops as primary op.")
                            game.players.forEachIndexed { i, p ->
                                OutlinedButton({ primaryFor = i }) { Text("${p.name}: ${if (p.primary == null) "select" else "change"} primary op") }
                            }
                        }
                    }
                }
                game.players.forEachIndexed { i, _ -> item { PlayerPanel(app, game, i) } }
                item { MissionCard(game, showMission) { showMission = it } }
                item {
                    val last = game.turningPoint >= ApprovedOps.TURNING_POINTS
                    Button({
                        if (game.readyDone) update(game.advanceTurningPoint())
                        else confirm = PendingConfirm(
                            "The Ready step was not done in this turning point: no CP were gained and expended operatives were not readied. Continue anyway?",
                            if (last) "End the battle" else "Next turning point",
                        ) { update(game.advanceTurningPoint()) }
                    }, Modifier.fillMaxWidth()) {
                        Text(if (last) "End the battle" else "Next turning point (${game.turningPoint + 1})")
                    }
                }
            }

            GamePhase.End -> {
                val finished = game.finish()
                item {
                    SectionCard("Reveal primary ops") {
                        game.players.forEach { p ->
                            Text(
                                when {
                                    p.primary == null -> "${p.name}: no primary op selected"
                                    p.primaryRevealed -> "${p.name}: primary op ${p.primary.label}"
                                    else -> "${p.name}: hidden"
                                },
                            )
                        }
                        Button({ update(finished) }) { Text("Reveal both") }
                    }
                }
                item {
                    val winner = Scoring.winner(finished)
                    SectionCard("Result") {
                        finished.players.indices.forEach { i ->
                            val s = Scoring.score(finished, i)
                            Text("${finished.players[i].name}: ${s.total} VP (Crit ${s.crit}, Kill ${s.kill}, Tac ${s.tac}, Primary +${s.primaryBonus})")
                        }
                        Text(if (winner == -1) "Draw" else "Winner: ${finished.players[winner].name}", style = MaterialTheme.typography.titleLarge)
                        Text("Saving reveals both primary ops, so their bonus is included.", style = MaterialTheme.typography.bodySmall)
                        Button({
                            app.history.update(listOf(finished.toResult()) + app.history.value)
                            app.game.update(null)
                        }) { Text("Save result and close game") }
                    }
                }
            }
        }

        item {
            OutlinedButton({
                confirm = PendingConfirm("Discard this game? Its score and all entries are lost and nothing is saved to the history.", "Discard game") { app.game.update(null) }
            }) { Text("Discard game") }
        }
    }
}

@Composable
private fun Scoreboard(game: GameState) {
    SectionCard("Score") {
        game.players.forEachIndexed { i, p ->
            val s = Scoring.score(game, i)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(p.name + if (game.initiative == i) "  (initiative)" else "", fontWeight = FontWeight.Bold)
                Text("Crit ${s.crit}  Kill ${s.kill}  Tac ${s.tac}  Primary +${s.primaryBonus}   =  ${s.total} VP")
            }
        }
    }
}

@Composable
private fun MissionCard(game: GameState, open: Boolean, onToggle: (Boolean) -> Unit) {
    val m = game.mission
    SectionCard("Mission: ${m.critOp.label}  |  ${m.killzone.label}, Map ${m.mapIndex + 1}") {
        OutlinedButton({ onToggle(!open) }) { Text(if (open) "Hide crit op and map" else "Show crit op and map") }
        if (open) {
            OpSectionsView(m.critOp.sections)
            ClickableImage(mapResource(m.killzone, m.mapIndex), Modifier.fillMaxWidth())
        }
    }
}

private fun cardLabel(card: InitiativeCard) = if (card == InitiativeCard.Reroll) "Re-roll" else card.label

@Composable
private fun InitiativePanel(game: GameState, update: (GameState) -> Unit) {
    val resolved = game.rollOffResolved
    SectionCard("Initiative roll-off") {
        Text("Both players roll off (ties are not re-rolled). Starting with the loser, players alternate using initiative cards or passing. Resolve when both have passed.")
        game.players.forEachIndexed { i, p ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${p.name}: ${game.rolls[i]?.toString() ?: "-"}", fontWeight = FontWeight.Bold)
                if (!resolved) {
                    ChipRow((1..6).toList(), { game.rolls[i] == it }, { it.toString() }, { v -> update(game.withRoll(i, v)) })
                    if (p.cards.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            p.cards.forEach { card ->
                                if (card == InitiativeCard.Reroll) {
                                    OutlinedButton({ update(game.useReroll(i)) }) { Text("Use Re-roll") }
                                } else {
                                    OutlinedButton({ update(game.useModifier(i, card, up = true)) }, enabled = game.rolls[i] != null) { Text("+${card.modifier}") }
                                    OutlinedButton({ update(game.useModifier(i, card, up = false)) }, enabled = game.rolls[i] != null) { Text("-${card.modifier}") }
                                }
                            }
                        }
                    }
                } else if (p.cards.isNotEmpty()) {
                    Text("Cards in hand: ${p.cards.joinToString { cardLabel(it) }}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (!resolved) {
            Button({ update(game.resolveRollOff()) }, enabled = game.rolls.all { it != null }) { Text("Resolve roll-off") }
        } else {
            val winner = game.rollOffWinner ?: 0
            Text("${game.players[winner].name} wins the roll-off and decides who has initiative.")
            Text(
                game.gainedCard?.let { "${game.players[1 - winner].name} gains the ${cardLabel(it)} initiative card." }
                    ?: "No initiative card is gained in the fourth turning point.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                game.players.forEachIndexed { i, p ->
                    val label = "${p.name} has initiative"
                    if (game.initiativeChosen && game.initiative == i) Button({ }, enabled = !game.readyDone) { Text(label) }
                    else OutlinedButton({ update(game.chooseInitiative(i)) }, enabled = !game.readyDone) { Text(label) }
                }
            }
            if (!game.readyDone) OutlinedButton({ update(game.reopenRollOff()) }) { Text("Redo roll-off") }
        }
        Text(if (game.initiativeChosen || game.turningPoint == 1) "Initiative: ${game.players[game.initiative].name}" else "Initiative: decide after the roll-off")
    }
}

@Composable
private fun ReadyStepPanel(game: GameState, update: (GameState) -> Unit) {
    SectionCard("Ready step") {
        Text("Each player gains 1CP. After the first turning point the player without initiative gains 2CP instead. All operatives become ready.")
        if (game.readyDone) {
            Text("Done. CP: ${game.players.joinToString { "${it.name} ${it.cp}" }}")
        } else {
            val gains = game.readyGains()
            if (game.initiativeChosen) Text(game.players.indices.joinToString { "${game.players[it].name} +${gains[it]} CP" })
            else Text("Resolve the roll-off and choose who has initiative first.")
            Button({ update(game.readyStep()) }, enabled = game.initiativeChosen) { Text("Do the Ready step") }
        }
    }
}

@Composable
private fun PlayerPanel(app: AppState, game: GameState, index: Int) {
    val p = game.players[index]
    val opponent = game.players[1 - index]
    val update: (GameState) -> Unit = { app.game.update(it) }
    val score = Scoring.score(game, index)
    var showOperatives by remember(game.id, index) { mutableStateOf(false) }
    var showTac by remember(game.id, index) { mutableStateOf(false) }
    val tac = p.tacOpId?.let { id -> ApprovedOps.tacOps.firstOrNull { it.id == id } }
    val team = app.team(p.teamSlug)

    SectionCard("${p.name}${team?.let { " - ${it.name}" } ?: ""}  |  ${score.total} VP") {
        Stepper("Crit op VP", p.critVp, { v -> update(game.updatePlayer(index) { it.copy(critVp = v) }) }, 0, ApprovedOps.MAX_VP_PER_OP)
        Text("Kill op: grade ${p.killGrade}/5 = ${score.kill} VP (+1 VP at the end if higher than the opponent's grade)")
        Stepper("Enemy start", p.enemyStartCount, { v -> update(game.updatePlayer(index) { it.copy(enemyStartCount = v) }) }, KillOp.minOperatives, KillOp.maxOperatives)
        Stepper("Incapacitated", p.kills, { v -> update(game.updatePlayer(index) { it.copy(kills = v) }) }, 0, 20)
        Text("Grade thresholds: ${KillOp.thresholds(p.enemyStartCount).joinToString(" / ")}")
        Stepper("Tac op VP", p.tacVp, { v -> update(game.updatePlayer(index) { it.copy(tacVp = v) }) }, 0, ApprovedOps.MAX_VP_PER_OP)
        if (tac != null) {
            if (p.tacRevealed) {
                Text("Tac op: ${tac.name}" + (tac.maxVpPerTurningPoint?.let { " (max. $it VP per turning point)" } ?: ""), fontWeight = FontWeight.Bold)
                OutlinedButton({ showTac = !showTac }) { Text(if (showTac) "Hide tac op rules" else "Show tac op rules") }
                if (showTac) OpSectionsView(tac.sections)
            } else {
                OutlinedButton({ update(game.updatePlayer(index) { it.copy(tacRevealed = true) }) }) { Text("Reveal tac op (reveal condition met)") }
            }
        } else Text("No tac op selected.")
        Stepper("CP", p.cp, { v -> update(game.updatePlayer(index) { it.copy(cp = v) }) }, 0, 20)
        Text("Primary op: " + when {
            p.primary == null -> "not selected"
            p.primaryRevealed -> p.primary.label
            else -> "selected (hidden)"
        })

        if (p.operatives.isNotEmpty()) {
            OutlinedButton({ showOperatives = !showOperatives }) { Text(if (showOperatives) "Hide operatives" else "Operatives (${p.operatives.count { !it.incapacitated }}/${p.operatives.size})") }
            if (showOperatives) {
                p.operatives.forEachIndexed { oi, o ->
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "${o.name}  ${o.wounds}/${o.maxWounds} W" + if (o.incapacitated) "  [incapacitated]" else if (o.injured) "  [injured]" else if (o.wounded) "  [wounded]" else "",
                            fontWeight = FontWeight.Bold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton({ update(game.changeOperative(index, oi) { it.copy(wounds = (it.wounds - 1).coerceAtLeast(0)) }) }) { Text("-1 W") }
                            OutlinedButton({ update(game.changeOperative(index, oi) { it.copy(wounds = (it.wounds + 1).coerceAtMost(it.maxWounds)) }) }) { Text("+1 W") }
                            OutlinedButton({ update(game.changeOperative(index, oi) { it.copy(engage = !it.engage) }) }) { Text(if (o.engage) "Engage" else "Conceal") }
                            OutlinedButton({ update(game.changeOperative(index, oi) { it.copy(expended = !it.expended) }) }) { Text(if (o.expended) "Expended" else "Ready") }
                            OutlinedButton({ update(game.setIncapacitated(index, oi, !o.incapacitated)) }) { Text(if (o.incapacitated) "Revive" else "Out") }
                        }
                    }
                }
                Text("'Out' / 'Revive' adjust ${opponent.name}'s incapacitated count for the kill op.")
            }
        }
    }
}
