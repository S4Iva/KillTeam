package com.example.killteam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.killteam.data.ApprovedOps
import com.example.killteam.data.KillOp
import com.example.killteam.data.KillTeam
import com.example.killteam.data.UniversalEquipment
import com.example.killteam.logic.AppState
import com.example.killteam.logic.GamePhase
import com.example.killteam.logic.GameState
import com.example.killteam.logic.InitiativeCard
import com.example.killteam.logic.MissionState
import com.example.killteam.logic.Scoring
import com.example.killteam.logic.advanceTurningPoint
import com.example.killteam.logic.changeOperative
import com.example.killteam.logic.chooseInitiative
import com.example.killteam.logic.chooseSetupInitiative
import com.example.killteam.logic.confirmStrategy
import com.example.killteam.logic.finish
import com.example.killteam.logic.ployCost
import com.example.killteam.logic.readyGains
import com.example.killteam.logic.readyStep
import com.example.killteam.logic.reopenRollOff
import com.example.killteam.logic.reopenStrategy
import com.example.killteam.logic.resolveRollOff
import com.example.killteam.logic.rollOffResolved
import com.example.killteam.logic.setIncapacitated
import com.example.killteam.logic.toResult
import com.example.killteam.logic.useModifier
import com.example.killteam.logic.useReroll
import com.example.killteam.logic.withEnemyStart
import com.example.killteam.logic.withPloy
import com.example.killteam.logic.withRoll
import com.example.killteam.logic.withoutPloy

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

private class PendingConfirm(val text: String, val label: String, val danger: Boolean = false, val action: () -> Unit)

/** Reference views of a player's card; operatives come first. */
private enum class PlayerView(val label: String) {
    Operatives("Operatives"), Strategy("Strategy ploys"), Firefight("Firefight ploys"), FactionRule("Faction rules"), TeamEquipment("Other equipment"),
}

private val phaseSummary = mapOf(
    GamePhase.SetUpBattle to "Kill teams, killzone, crit op, initiative and drop zones.",
    GamePhase.SelectOperatives to "Operatives, equipment (max. 4), 2 CP and the secret tac op.",
    GamePhase.SetUpOperatives to "Set up equipment and a third of the team with Conceal orders.",
    GamePhase.Play to "Four turning points: initiative, Ready step, activations.",
    GamePhase.End to "Reveal the primary op, bonus VP and the winner.",
)

@Composable
fun RunningGame(app: AppState, game: GameState) {
    val update: (GameState) -> Unit = { app.game.update(it) }
    val ui = LocalUi.current
    val listState = ui.list("game")
    var secretFor by remember { mutableStateOf<Int?>(null) }
    var secretReady by remember { mutableStateOf(false) }
    var primaryFor by remember { mutableStateOf<Int?>(null) }
    var showMission by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<PendingConfirm?>(null) }
    var equipmentOpen by remember(game.id) { mutableStateOf(setOf<Int>()) }

    // Every new phase or turning point starts at the top of the screen (not when only switching back to this tab).
    val position = "${game.id}:${game.phase}:${game.turningPoint}:${game.strategyDone}"
    LaunchedEffect(position) {
        if (ui.gameScrollKey != null && ui.gameScrollKey != position) listState.scrollToItem(0)
        ui.gameScrollKey = position
    }

    secretFor?.let { index ->
        if (secretReady) {
            SecretSelectionPage(app, game, index) { secretFor = null; secretReady = false }
            return
        }
        SecretHandover(game.players[index].name, { secretReady = true }, { secretFor = null })
    }
    primaryFor?.let { PrimaryOpDialog(app, game, it) { primaryFor = null } }
    confirm?.let { c -> ConfirmDialog(c.text, c.label, c.action, onDismiss = { confirm = null }, danger = c.danger) }

    Page("Game - Turning Point ${game.turningPoint}/${ApprovedOps.TURNING_POINTS}" + if (game.phase == GamePhase.Play) (if (game.strategyDone) " - Firefight phase" else " - Strategy phase") else "", listState = listState) {
        item {
            SectionCard("Phase") {
                PhaseProgress(game)
                ChipRow(GamePhase.entries, { it == game.phase }, { it.label }, { target ->
                    if (target == GamePhase.End && game.phase != GamePhase.End) {
                        confirm = PendingConfirm("Jump to \"${target.label}\"? The battle then counts as ended.", "End the battle") {
                            update(game.copy(phase = target, ended = true))
                        }
                    } else {
                        update(game.copy(phase = target, ended = false))
                    }
                })
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    GamePhase.entries.forEach { ph ->
                        val current = ph == game.phase
                        Text(
                            "${ph.label}: ${phaseSummary.getValue(ph)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                            color = if (current) Palette.Accent else Palette.Text,
                        )
                    }
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
                        InitiativeButtons(game, game.initiative, true) { update(game.chooseSetupInitiative(it)) }
                        Text("Initiative: ${game.players[game.initiative].name}", fontWeight = FontWeight.Bold)
                        Text("${game.players[1 - game.initiative].name} holds the Re-roll initiative card.")
                    }
                }
                item { MissionCard(game, showMission) { showMission = it } }
            }

            GamePhase.SelectOperatives -> {
                game.players.forEachIndexed { i, p ->
                    item {
                        SectionCard(p.name) {
                            Stepper("Kill team size", game.players[1 - i].enemyStartCount, { v -> update(game.withEnemyStart(1 - i, v)) }, KillOp.minOperatives, KillOp.maxOperatives)
                            Text("(Sets the kill grade row of ${game.players[1 - i].name}.)")
                            Stepper("CP", p.cp, { v -> update(game.updatePlayer(i) { it.copy(cp = v) }) }, 0, 20)
                            Text(if (p.tacOpId != null) "Tac op selected (hidden), equipment ${p.equipment.size}/${ApprovedOps.MAX_EQUIPMENT}" else "No secret selection yet.")
                            AppButton("Secret selection (tac op, equipment)", { secretFor = i }, kind = ButtonKind.Secondary, size = ButtonSize.L, fullWidth = true)
                        }
                    }
                }
                if (game.players.any { it.rosterEquipment.isNotEmpty() || it.operatives.any { o -> o.equipment.isNotEmpty() } }) {
                    item {
                        SectionCard("Equipment") {
                            game.players.forEachIndexed { pi, p ->
                                val gear = p.rosterEquipment + p.operatives.flatMap { it.equipment }
                                if (gear.isNotEmpty()) {
                                    val open = pi in equipmentOpen
                                    AppButton(
                                        if (open) "Hide equipment: ${p.name}" else "Equipment (${gear.size}): ${p.name}",
                                        { equipmentOpen = if (open) equipmentOpen - pi else equipmentOpen + pi },
                                        kind = ButtonKind.Secondary, fullWidth = true,
                                    )
                                    if (open) EquipmentCards(app, app.team(p.teamSlug), gear)
                                }
                            }
                        }
                    }
                }
            }

            GamePhase.SetUpOperatives -> Unit

            GamePhase.Play -> {
                if (!game.strategyDone) {
                    item { InitiativePanel(game, update) }
                    item { ReadyStepPanel(game, update) }
                    if (game.turningPoint == 1) {
                        item {
                            SectionCard("Strategic gambit: primary op") {
                                Text("In the first turning point each player secretly selects one of their three ops as primary op.")
                                game.players.forEachIndexed { i, p ->
                                    AppButton(
                                        "${p.name}: ${if (p.primary == null) "select" else "change"} primary op", { primaryFor = i },
                                        kind = ButtonKind.Secondary, size = ButtonSize.L, fullWidth = true,
                                    )
                                }
                            }
                        }
                    }
                    listOf(game.initiative, 1 - game.initiative).forEach { i -> item { StrategyPloyPanel(app, game, i) } }
                    item {
                        AppButton("Confirm strategy phase", {
                            if (game.readyDone) update(game.confirmStrategy())
                            else confirm = PendingConfirm(
                                "The Ready step was not done in this turning point: no CP were gained and expended operatives were not readied. Continue anyway?",
                                "Continue anyway",
                            ) { update(game.confirmStrategy()) }
                        }, fullWidth = true)
                    }
                } else {
                    item {
                        SectionCard("Strategy phase") {
                            Text("Initiative: ${game.players[game.initiative].name}")
                            game.players.forEach { p ->
                                Text("${p.name}: strategy ploys used: ${p.ployNames.joinToString().ifEmpty { "none" }}", style = MaterialTheme.typography.bodySmall)
                            }
                            AppButton("Back to strategy phase", { update(game.reopenStrategy()) }, kind = ButtonKind.Secondary, fullWidth = true)
                        }
                    }
                    game.players.forEachIndexed { i, _ -> item { PlayerPanel(app, game, i) } }
                    item { MissionCard(game, showMission) { showMission = it } }
                    item {
                        val last = game.turningPoint >= ApprovedOps.TURNING_POINTS
                        AppButton(if (last) "End the battle" else "Next turning point (${game.turningPoint + 1})", {
                            if (game.readyDone) update(game.advanceTurningPoint())
                            else confirm = PendingConfirm(
                                "The Ready step was not done in this turning point: no CP were gained and expended operatives were not readied. Continue anyway?",
                                if (last) "End the battle" else "Next turning point",
                            ) { update(game.advanceTurningPoint()) }
                        }, fullWidth = true)
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
                        AppButton("Reveal both", { update(finished) }, fullWidth = true)
                    }
                }
                item {
                    ResultCard(finished) {
                        app.history.update(listOf(finished.toResult()) + app.history.value)
                        app.game.update(null)
                    }
                }
            }
        }

        val nextPhase = GamePhase.entries.getOrNull(game.phase.ordinal + 1)
        if (nextPhase != null && game.phase != GamePhase.Play) {
            item {
                AppButton("Next: ${nextPhase.label}", { update(game.copy(phase = nextPhase)) }, fullWidth = true)
            }
        }

        item {
            AppButton(
                "Discard game",
                { confirm = PendingConfirm("Discard this game? Its score and all entries are lost and nothing is saved to the history.", "Discard game", danger = true) { app.game.update(null) } },
                kind = ButtonKind.Danger, fullWidth = true,
            )
        }
    }
}

/** Crit op in large type with killzone and map as tags; shared by the game and the new-game page. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MissionSummary(m: MissionState) {
    Text(m.critOp.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Palette.Accent)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Tag(m.killzone.label)
        Tag("Map ${m.mapIndex + 1}")
    }
}

/** Outlined pill for short facts such as killzone and map. */
@Composable
private fun Tag(text: String) {
    Text(
        text, Modifier.border(1.dp, Palette.Accent, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
    )
}

/** Winner banner, score table and the save button of the last phase. */
@Composable
private fun ResultCard(finished: GameState, onSave: () -> Unit) {
    val winner = Scoring.winner(finished)
    SectionCard("Result") {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (winner == -1) Palette.Header else Palette.Accent).padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (winner == -1) "Draw" else "Winner: ${finished.players[winner].name}",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White,
            )
        }
        ScoreRow("", null, listOf("Crit", "Kill", "Tac", "Primary", "VP"), header = true)
        HorizontalDivider(color = Palette.Accent.copy(alpha = 0.4f))
        finished.players.forEachIndexed { i, p ->
            val s = Scoring.score(finished, i)
            ScoreRow(
                p.name, if (winner == i) "Winner" else null,
                listOf(s.crit.toString(), s.kill.toString(), s.tac.toString(), "+${s.primaryBonus}", s.total.toString()),
                header = false,
            )
        }
        Text("Saving reveals both primary ops, so their bonus is included.", style = MaterialTheme.typography.bodySmall)
        AppButton("Save result and close game", onSave, kind = ButtonKind.Primary, size = ButtonSize.L, fullWidth = true)
    }
}

/** One segment per step; finished steps and the current one are orange. */
@Composable
private fun PhaseProgress(game: GameState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GamePhase.entries.forEach { ph ->
            Box(
                Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (ph.ordinal <= game.phase.ordinal) Palette.Accent else Palette.Text.copy(alpha = 0.2f)),
            )
        }
    }
}

@Composable
private fun Scoreboard(game: GameState) {
    SectionCard("Score") {
        ScoreRow("", null, listOf("Crit", "Kill", "Tac", "Primary", "VP"), header = true)
        HorizontalDivider(color = Palette.Accent.copy(alpha = 0.4f))
        game.players.forEachIndexed { i, p ->
            val s = Scoring.score(game, i)
            ScoreRow(
                p.name, if (game.initiative == i) "Initiative" else null,
                listOf(s.crit.toString(), s.kill.toString(), s.tac.toString(), "+${s.primaryBonus}", s.total.toString()),
                header = false,
            )
        }
    }
}

/** One row of the score table: name column and five equally wide value columns, the last one is the total. */
@Composable
private fun ScoreRow(name: String, note: String?, values: List<String>, header: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(2f)) {
            Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (note != null) Text(note, style = MaterialTheme.typography.labelSmall, color = Palette.Highlight)
        }
        values.forEachIndexed { i, v ->
            val total = i == values.lastIndex
            Text(
                v, Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1,
                style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                fontWeight = if (total || header) FontWeight.Bold else FontWeight.Normal,
                color = if (header) Palette.Accent else if (total) Palette.Accent else Palette.Text,
            )
        }
    }
}

/** Equally wide buttons to choose the player with initiative; the current one is filled. */
@Composable
private fun InitiativeButtons(game: GameState, selected: Int?, enabled: Boolean, onPick: (Int) -> Unit) {
    SegmentedChoice(game.players.indices.toList(), selected, { "${game.players[it].name} has initiative" }, onPick, enabled)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissionCard(game: GameState, open: Boolean, onToggle: (Boolean) -> Unit) {
    val m = game.mission
    SectionCard("Mission") {
        MissionSummary(m)
        AppButton(if (open) "Hide crit op and map" else "Show crit op and map", { onToggle(!open) }, kind = ButtonKind.Secondary, fullWidth = true)
        if (open) {
            OpSectionsView(m.critOp.sections)
            ClickableImage(mapResource(m.killzone, m.mapIndex), Modifier.fillMaxWidth())
        }
    }
}

private fun cardLabel(card: InitiativeCard) = if (card == InitiativeCard.Reroll) "Re-roll" else card.label

@OptIn(ExperimentalLayoutApi::class)
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.cards.forEach { card ->
                                if (card == InitiativeCard.Reroll) {
                                    AppButton("Use Re-roll", { update(game.useReroll(i)) }, kind = ButtonKind.Secondary, size = ButtonSize.S)
                                } else {
                                    AppButton("+${card.modifier}", { update(game.useModifier(i, card, up = true)) }, kind = ButtonKind.Secondary, size = ButtonSize.S, enabled = game.rolls[i] != null)
                                    AppButton("-${card.modifier}", { update(game.useModifier(i, card, up = false)) }, kind = ButtonKind.Secondary, size = ButtonSize.S, enabled = game.rolls[i] != null)
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
            AppButton("Resolve roll-off", { update(game.resolveRollOff()) }, enabled = game.rolls.all { it != null }, fullWidth = true)
        } else {
            val winner = game.rollOffWinner ?: 0
            Text("${game.players[winner].name} wins the roll-off and decides who has initiative.")
            Text(
                game.gainedCard?.let { "${game.players[1 - winner].name} gains the ${cardLabel(it)} initiative card." }
                    ?: "No initiative card is gained in the fourth turning point.",
            )
            InitiativeButtons(game, if (game.initiativeChosen) game.initiative else null, !game.readyDone) { update(game.chooseInitiative(it)) }
            if (!game.readyDone) AppButton("Redo roll-off", { update(game.reopenRollOff()) }, kind = ButtonKind.Secondary, fullWidth = true)
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
            AppButton("Do the Ready step", { update(game.readyStep()) }, enabled = game.initiativeChosen, fullWidth = true)
        }
    }
}

/** Strategy ploys of one player; choosing one deducts its CP at once. Folded by default. */
@Composable
private fun StrategyPloyPanel(app: AppState, game: GameState, index: Int) {
    val p = game.players[index]
    val team = app.team(p.teamSlug)
    var open by remember(game.id, game.turningPoint, index) { mutableStateOf(false) }
    val ploys = team?.strategyPloys.orEmpty()
    SectionCard(
        "${p.name}${team?.let { " - ${it.name}" } ?: ""}",
        actions = { Text("CP ${p.cp}", color = Color.White, fontWeight = FontWeight.Bold) },
    ) {
        Text("Ploys: ${p.ployNames.size} selected" + if (p.ployNames.isEmpty()) "" else " (${p.ployNames.joinToString()})")
        if (ploys.isEmpty()) {
            Text("No strategy ploys", style = MaterialTheme.typography.bodySmall)
        } else {
            if (open) {
                if (!game.readyDone) Text("Do the Ready step first.", color = Palette.Accent)
                Text("Select the ploys you use. The CP are deducted automatically.", style = MaterialTheme.typography.bodySmall)
                ploys.forEach { ploy ->
                    val cost = ployCost(ploy.cost)
                    val on = ploy.name in p.ployNames
                    val poor = !on && p.cp < cost
                    val locked = !game.readyDone || poor
                    val shape = RoundedCornerShape(8.dp)
                    Column(
                        Modifier.fillMaxWidth().alpha(if (locked) 0.5f else 1f).clip(shape)
                            .border(1.dp, if (on) Palette.Accent else Palette.StrategyPloy, shape)
                            .clickable(enabled = !locked) { app.game.update(if (on) game.withoutPloy(index, ploy.name, cost) else game.withPloy(index, ploy.name, cost)) }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(on, null, enabled = !locked)
                            Text(ploy.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text("-$cost CP", color = Palette.Accent, fontWeight = FontWeight.Bold)
                        }
                        if (ploy.paras.isNotEmpty()) RichParas(ploy.paras) else KeywordText(ploy.text)
                        if (poor && game.readyDone) Text("not enough CP", color = Palette.Accent, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            AppButton(if (open) "Hide strategy ploys" else "Show strategy ploys", { open = !open }, kind = ButtonKind.Secondary, fullWidth = true)
        }
    }
}

/** Equipment as the same cards as in the Teams tab; universal equipment shows its rules from [UniversalEquipment]. */
@Composable
private fun EquipmentCards(app: AppState, team: KillTeam?, names: List<String>) {
    if (names.isEmpty()) {
        Text("No equipment selected.", style = MaterialTheme.typography.bodySmall)
        return
    }
    names.forEach { e ->
        val item = team?.equipment?.firstOrNull { it.name.equals(e, ignoreCase = true) } ?: UniversalEquipment.card(e)
        if (item != null) NamedTextCard(item, Palette.Equipment) else Text(e, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerPanel(app: AppState, game: GameState, index: Int) {
    val p = game.players[index]
    val opponent = game.players[1 - index]
    val update: (GameState) -> Unit = { app.game.update(it) }
    val score = Scoring.score(game, index)
    var openGear by remember(game.id, index) { mutableStateOf(setOf<Int>()) }
    var view by remember(game.id, index) { mutableStateOf<PlayerView?>(null) }
    var showTac by remember(game.id, index) { mutableStateOf(false) }
    var openCards by remember(game.id, index) { mutableStateOf(setOf<Int>()) }
    var allWeapons by remember(game.id, index) { mutableStateOf(setOf<Int>()) }
    val tac = p.tacOpId?.let { id -> ApprovedOps.tacOps.firstOrNull { it.id == id } }
    val team = app.team(p.teamSlug)

    SectionCard("${p.name}${team?.let { " - ${it.name}" } ?: ""}  |  ${score.total} VP") {
        Stepper("Crit op VP", p.critVp, { v -> update(game.updatePlayer(index) { it.copy(critVp = v) }) }, 0, ApprovedOps.MAX_VP_PER_OP)
        Text("Kill op: grade ${p.killGrade}/5 = ${score.kill} VP (+1 VP at the end if higher than the opponent's grade)")
        Stepper("Enemy start", p.enemyStartCount, { v -> update(game.withEnemyStart(index, v)) }, KillOp.minOperatives, KillOp.maxOperatives)
        Stepper("Incapacitated", p.kills, { v -> update(game.updatePlayer(index) { it.copy(kills = v) }) }, 0, p.enemyStartCount)
        Text("Grade thresholds: ${KillOp.thresholds(p.enemyStartCount).joinToString(" / ")}")
        Stepper("Tac op VP", p.tacVp, { v -> update(game.updatePlayer(index) { it.copy(tacVp = v) }) }, 0, ApprovedOps.MAX_VP_PER_OP)
        if (tac != null) {
            if (p.tacRevealed) {
                Text("Tac op: ${tac.name}" + (tac.maxVpPerTurningPoint?.let { " (max. $it VP per turning point)" } ?: ""), fontWeight = FontWeight.Bold)
                AppButton(if (showTac) "Hide tac op rules" else "Show tac op rules", { showTac = !showTac }, kind = ButtonKind.Secondary, fullWidth = true)
                if (showTac) OpSectionsView(tac.sections)
            } else {
                AppButton("Reveal tac op (reveal condition met)", { update(game.updatePlayer(index) { it.copy(tacRevealed = true) }) }, kind = ButtonKind.Secondary, fullWidth = true)
            }
        } else Text("No tac op selected.")
        Stepper("CP", p.cp, { v -> update(game.updatePlayer(index) { it.copy(cp = v) }) }, 0, 20)
        Text("Primary op: " + when {
            p.primary == null -> "not selected"
            p.primaryRevealed -> p.primary.label
            else -> "selected (hidden)"
        })
        val views = PlayerView.entries.filter { it != PlayerView.TeamEquipment || p.rosterEquipment.isNotEmpty() }
        ChipRow(views, { it == view }, { v ->
            when (v) {
                PlayerView.Operatives -> "Operatives (${p.operatives.count { !it.incapacitated }}/${p.operatives.size})"
                PlayerView.TeamEquipment -> "Other equipment (${p.rosterEquipment.size})"
                else -> v.label
            }
        }, { view = if (view == it) null else it })

        when (view) {
            PlayerView.Strategy -> if (team == null || team.strategyPloys.isEmpty()) Text("-") else team.strategyPloys.forEach { NamedTextCard(it, Palette.StrategyPloy) }
            PlayerView.Firefight -> if (team == null || team.firefightPloys.isEmpty()) Text("-") else team.firefightPloys.forEach { NamedTextCard(it, Palette.FirefightPloy) }
            PlayerView.FactionRule -> when {
                team == null -> Text("-")
                team.factionRuleItems.isNotEmpty() -> team.factionRuleItems.forEach { NamedTextCard(it, Palette.Accent) }
                else -> SectionCard("Faction rules") { KeywordText(team.factionRules.ifBlank { "-" }) }
            }
            PlayerView.TeamEquipment -> EquipmentCards(app, team, p.rosterEquipment)
            else -> Unit
        }

        if (view == PlayerView.Operatives) {
            if (p.operatives.isEmpty()) Text("No operatives: the game was started without a roster.")
            else {
                p.operatives.forEachIndexed { oi, o ->
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "${o.name}  ${o.wounds}/${o.maxWounds} W" + if (o.incapacitated) "  [incapacitated]" else if (o.injured) "  [injured]" else if (o.wounded) "  [wounded]" else "",
                            fontWeight = FontWeight.Bold,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val s = ButtonSize.S
                            AppButton("-1 W", { update(game.changeOperative(index, oi) { it.copy(wounds = (it.wounds - 1).coerceAtLeast(0)) }) }, kind = ButtonKind.Secondary, size = s)
                            AppButton("+1 W", { update(game.changeOperative(index, oi) { it.copy(wounds = (it.wounds + 1).coerceAtMost(it.maxWounds)) }) }, kind = ButtonKind.Secondary, size = s)
                            AppButton(if (o.expended) "Expended" else "Ready", { update(game.changeOperative(index, oi) { it.copy(expended = !it.expended) }) }, kind = ButtonKind.Secondary, size = s)
                            AppButton(
                                if (o.incapacitated) "Revive" else "Out", { update(game.setIncapacitated(index, oi, !o.incapacitated)) },
                                kind = if (o.incapacitated) ButtonKind.Secondary else ButtonKind.Danger, size = s,
                            )
                        }
                        if (o.equipment.isNotEmpty()) {
                            val open = oi in openGear
                            AppButton(
                                (if (open) "Hide equipment: " else "Equipment: ") + o.equipment.joinToString(), { openGear = if (open) openGear - oi else openGear + oi },
                                kind = ButtonKind.Secondary, size = ButtonSize.S, fullWidth = true,
                            )
                            if (open) EquipmentCards(app, team, o.equipment)
                        }
                        val datacard = team?.operatives?.firstOrNull { it.name == o.card }
                        if (datacard != null) {
                            val open = oi in openCards
                            AppButton(
                                if (open) "Hide datacard" else "Show datacard", { openCards = if (open) openCards - oi else openCards + oi },
                                kind = ButtonKind.Secondary, size = ButtonSize.S, fullWidth = true,
                            )
                            if (open) {
                                val showAll = o.weapons.isEmpty() || oi in allWeapons
                                OperativeDatacard(if (showAll) datacard else datacard.withWeapons(o.weapons))
                                if (o.weapons.isNotEmpty()) {
                                    AppButton(
                                        if (showAll) "Show chosen weapons" else "Show all weapons", { allWeapons = if (showAll) allWeapons - oi else allWeapons + oi },
                                        kind = ButtonKind.Secondary, size = ButtonSize.S, fullWidth = true,
                                    )
                                }
                            }
                        }
                    }
                }
                Text("'Out' / 'Revive' adjust ${opponent.name}'s incapacitated count for the kill op.")
            }
        }
    }
}
