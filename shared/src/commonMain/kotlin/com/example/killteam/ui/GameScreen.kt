package com.example.killteam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import com.example.killteam.logic.Initiative
import com.example.killteam.logic.InitiativeCard
import com.example.killteam.logic.OpKind
import com.example.killteam.logic.Scoring
import com.example.killteam.logic.advanceTurningPoint
import com.example.killteam.logic.toResult

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

@Composable
fun RunningGame(app: AppState, game: GameState) {
    val update: (GameState) -> Unit = { app.game.update(it) }
    var secretFor by remember { mutableStateOf<Int?>(null) }
    var primaryFor by remember { mutableStateOf<Int?>(null) }
    var showMission by remember { mutableStateOf(false) }
    var showEquipment by remember(game.id, game.phase) { mutableStateOf(false) }

    secretFor?.let { SecretSelectionDialog(app, game, it) { secretFor = null } }
    primaryFor?.let { PrimaryOpDialog(app, game, it) { primaryFor = null } }

    Page("Game - Turning Point ${game.turningPoint}/${ApprovedOps.TURNING_POINTS}", listState = LocalUi.current.list("game")) {
        item {
            SectionCard("Phase") {
                ChipRow(GamePhase.entries, { it == game.phase }, { it.label }, { update(game.copy(phase = it, ended = it == GamePhase.End)) })
                val next = GamePhase.entries.getOrNull(game.phase.ordinal + 1)
                if (next != null && game.phase != GamePhase.Play) {
                    Button({ update(game.copy(phase = next, ended = next == GamePhase.End)) }) { Text("Next: ${next.label}") }
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
                                Button({
                                    val other = 1 - i
                                    update(
                                        game.copy(initiative = i)
                                            .updatePlayer(i) { it.copy(cards = it.cards - InitiativeCard.Reroll) }
                                            .updatePlayer(other) { it.copy(cards = (it.cards - InitiativeCard.Reroll) + InitiativeCard.Reroll) },
                                    )
                                }) { Text("${p.name} has initiative") }
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
                item { InitiativePanel(app, game) }
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
                    Button({ update(game.advanceTurningPoint().let { it.copy(ended = it.phase == GamePhase.End) }) }, Modifier.fillMaxWidth()) {
                        Text(if (game.turningPoint >= ApprovedOps.TURNING_POINTS) "End the battle" else "Next turning point (${game.turningPoint + 1})")
                    }
                }
            }

            GamePhase.End -> {
                item {
                    SectionCard("Reveal primary ops") {
                        game.players.forEachIndexed { i, p ->
                            val text = when {
                                p.primary == null -> "${p.name}: no primary op selected"
                                p.primaryRevealed -> "${p.name}: primary op ${p.primary.label}"
                                else -> "${p.name}: hidden"
                            }
                            Text(text)
                        }
                        Button({ update(game.copy(players = game.players.map { it.copy(primaryRevealed = it.primary != null) }, ended = true)) }) { Text("Reveal both") }
                    }
                }
                item {
                    val winner = Scoring.winner(game.copy(ended = true))
                    SectionCard("Result") {
                        Text(if (winner == -1) "Draw" else "Winner: ${game.players[winner].name}", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                        Button({
                            val finished = game.copy(ended = true)
                            app.history.update(listOf(finished.toResult()) + app.history.value)
                            app.game.update(null)
                        }) { Text("Save result and close game") }
                    }
                }
            }
        }

        item {
            OutlinedButton({ app.game.update(null) }) { Text("Discard game") }
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

@Composable
private fun InitiativePanel(app: AppState, game: GameState) {
    val tp = game.turningPoint
    var rolls by remember(tp, game.id) { mutableStateOf(listOf<Int?>(null, null)) }
    var winner by remember(tp, game.id) { mutableStateOf<Int?>(null) }

    SectionCard("Initiative roll-off") {
        Text("Both players roll off (ties are not re-rolled). Starting with the loser, players alternate using initiative cards or passing.")
        game.players.forEachIndexed { i, p ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${p.name}: ${rolls[i]?.toString() ?: "-"}", fontWeight = FontWeight.Bold)
                ChipRow((1..6).toList(), { rolls[i] == it }, { it.toString() }, { v -> rolls = rolls.toMutableList().also { l -> l[i] = v }; winner = null })
                if (p.cards.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        p.cards.forEach { card ->
                            if (card == InitiativeCard.Reroll) {
                                OutlinedButton({
                                    app.game.update(game.updatePlayer(i) { it.copy(cards = it.cards - card) })
                                    rolls = rolls.toMutableList().also { l -> l[i] = null }
                                    winner = null
                                }) { Text("Use Re-roll") }
                            } else {
                                val current = rolls[i]
                                OutlinedButton({
                                    app.game.update(game.updatePlayer(i) { it.copy(cards = it.cards - card) })
                                    rolls = rolls.toMutableList().also { l -> l[i] = current!! + card.modifier }
                                    winner = null
                                }, enabled = current != null) { Text("+${card.modifier}") }
                                OutlinedButton({
                                    app.game.update(game.updatePlayer(i) { it.copy(cards = it.cards - card) })
                                    rolls = rolls.toMutableList().also { l -> l[i] = current!! - card.modifier }
                                    winner = null
                                }, enabled = current != null) { Text("-${card.modifier}") }
                            }
                        }
                    }
                }
            }
        }
        val a = rolls[0]
        val b = rolls[1]
        Button({
            val w = Initiative.rollOffWinner(listOf(a!!, b!!), game.initiative)
            winner = w
            val card = Initiative.cardForTurningPoint(tp)
            if (card != null) app.game.update(game.updatePlayer(1 - w) { it.copy(cards = it.cards + card) })
        }, enabled = a != null && b != null && winner == null) { Text("Resolve roll-off") }
        winner?.let { w ->
            Text("${game.players[w].name} wins the roll-off and decides who has initiative.")
            val gained = Initiative.cardForTurningPoint(tp)
            Text(if (gained != null) "${game.players[1 - w].name} gains the ${gained.label} initiative card." else "No initiative card is gained in the fourth turning point.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                game.players.forEachIndexed { i, p ->
                    OutlinedButton({ app.game.update(app.game.value!!.copy(initiative = i)) }) { Text("${p.name} has initiative") }
                }
            }
        }
        Text("Initiative: ${game.players[game.initiative].name}")
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
                            fun change(f: (com.example.killteam.logic.OperativeTrack) -> com.example.killteam.logic.OperativeTrack) =
                                update(game.updatePlayer(index) { pl -> pl.copy(operatives = pl.operatives.mapIndexed { k, x -> if (k == oi) f(x) else x }) })
                            OutlinedButton({ change { it.copy(wounds = (it.wounds - 1).coerceAtLeast(0)) } }) { Text("-1 W") }
                            OutlinedButton({ change { it.copy(wounds = (it.wounds + 1).coerceAtMost(it.maxWounds)) } }) { Text("+1 W") }
                            OutlinedButton({ change { it.copy(engage = !it.engage) } }) { Text(if (o.engage) "Engage" else "Conceal") }
                            OutlinedButton({ change { it.copy(expended = !it.expended) } }) { Text(if (o.expended) "Expended" else "Ready") }
                            OutlinedButton({
                                val newState = !o.incapacitated
                                update(
                                    game.updatePlayer(index) { pl -> pl.copy(operatives = pl.operatives.mapIndexed { k, x -> if (k == oi) x.copy(incapacitated = newState, wounds = if (newState) 0 else maxOf(x.wounds, 1)) else x }) }
                                        .updatePlayer(1 - index) { it.copy(kills = (it.kills + if (newState) 1 else -1).coerceAtLeast(0)) },
                                )
                            }) { Text(if (o.incapacitated) "Revive" else "Out") }
                        }
                    }
                }
                Text("'Out' / 'Revive' adjust ${opponent.name}'s incapacitated count for the kill op.")
            }
        }
    }
}
