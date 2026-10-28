package com.example.killteam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import com.example.killteam.data.ApprovedOps
import com.example.killteam.logic.AppState
import com.example.killteam.logic.DamageCalc
import com.example.killteam.logic.GameResult
import com.example.killteam.logic.Pairing
import com.example.killteam.logic.Statistics
import com.example.killteam.logic.TPlayer
import com.example.killteam.logic.TRound
import com.example.killteam.logic.Tournament
import com.example.killteam.logic.TournamentLogic
import com.example.killteam.logic.formatDate
import com.example.killteam.logic.nowMillis
import org.jetbrains.compose.resources.painterResource

@Composable
fun MoreScreen(app: AppState) {
    val ui = LocalUi.current
    val back = { ui.morePage = null }
    when (ui.morePage) {
        null -> Page("More", listState = ui.list("more")) {
            MorePage.entries.forEach { p ->
                item { OutlinedButton({ ui.morePage = p }, Modifier.fillMaxWidth()) { Text(p.label) } }
            }
        }
        MorePage.History -> HistoryPage(app, back)
        MorePage.Tournament -> TournamentPage(app, back)
        MorePage.Dice -> DicePage(back)
        MorePage.Favorites -> FavoritesPage(app, back)
        MorePage.About -> AboutPage(app, back)
    }
}

@Composable
private fun HistoryPage(app: AppState, onBack: () -> Unit) {
    val results = app.history.value
    Page("Game history", onBack) {
        if (results.isEmpty()) item { Text("No finished games yet. Finish a game to save its result.") }
        else {
            item {
                SectionCard("Win rate per kill team") {
                    Statistics.byTeam(results).forEach { s ->
                        Text("${app.team(s.key)?.name ?: s.key}: ${s.games} games, ${s.wins}W ${s.draws}D ${s.losses}L, avg ${format1(s.avgVp)} VP")
                    }
                }
            }
            item {
                SectionCard("Per crit op") {
                    Statistics.byCritOp(results) { ApprovedOps.critOps[it].label }.forEach { s ->
                        Text("${s.key}: ${s.games} player-games, ${s.wins}W ${s.draws}D ${s.losses}L, avg ${format1(s.avgVp)} VP")
                    }
                }
            }
            item {
                SectionCard("Per tac op") {
                    Statistics.byTacOp(results) { id -> ApprovedOps.tacOps.firstOrNull { it.id == id }?.name ?: id }.forEach { s ->
                        Text("${s.key}: ${s.games} times, ${s.wins}W ${s.draws}D ${s.losses}L, avg ${format1(s.avgVp)} VP")
                    }
                }
            }
            items(results.size) { i -> ResultCard(app, results[i]) }
        }
    }
}

private fun format1(v: Double): String = ((v * 10).toInt() / 10.0).toString()

@Composable
private fun ResultCard(app: AppState, r: GameResult) {
    SectionCard("${formatDate(r.playedAt)}  |  ${ApprovedOps.critOps[r.critOpIndex].label}  |  ${r.killzone.label} Map ${r.mapIndex + 1}") {
        r.players.forEachIndexed { i, p ->
            val tac = ApprovedOps.tacOps.firstOrNull { it.id == p.tacOpId }?.name
            Text(
                "${p.name} (${app.team(p.teamSlug)?.name ?: "-"}): ${p.total} VP  [Crit ${p.crit}, Kill ${p.kill}, Tac ${p.tac}${tac?.let { " $it" } ?: ""}, Primary +${p.bonus}${p.primary?.let { " ${it.label}" } ?: ""}]",
                fontWeight = if (r.winner == i) FontWeight.Bold else FontWeight.Normal,
            )
        }
        Text(if (r.winner == -1) "Draw" else "Winner: ${r.players[r.winner].name}")
        OutlinedButton({ app.history.update(app.history.value.filter { it.id != r.id }) }) { Text("Delete") }
    }
}

@Composable
private fun TournamentPage(app: AppState, onBack: () -> Unit) {
    val ui = LocalUi.current
    var newName by remember { mutableStateOf("") }
    val all = app.tournaments.value
    val open = all.firstOrNull { it.id == ui.tournamentOpen }
    if (open != null) {
        TournamentDetail(app, open) { ui.tournamentOpen = null }
        return
    }
    Page("Tournament mode", onBack) {
        item {
            SectionCard("New tournament") {
                OutlinedTextField(newName, { newName = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                Button({
                    val t = Tournament(nowMillis(), newName.ifBlank { "Tournament" }, emptyList())
                    app.tournaments.update(all + t)
                    newName = ""
                    ui.tournamentOpen = t.id
                }) { Text("Create") }
            }
        }
        items(all.size) { i ->
            val t = all[i]
            SectionCard(t.name) {
                Text("${t.players.size} players, ${t.rounds.size} rounds")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ ui.tournamentOpen = t.id }) { Text("Open") }
                    OutlinedButton({ app.tournaments.update(all.filter { it.id != t.id }) }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun TournamentDetail(app: AppState, t: Tournament, onBack: () -> Unit) {
    var playerName by remember { mutableStateOf("") }
    val save: (Tournament) -> Unit = { n -> app.tournaments.update(app.tournaments.value.map { if (it.id == n.id) n else it }) }
    val name = { id: Int? -> t.players.firstOrNull { it.id == id }?.name ?: "-" }

    Page(t.name, onBack) {
        item {
            SectionCard("Players (${t.players.size})") {
                if (t.rounds.isEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(playerName, { playerName = it }, Modifier.weight(1f), label = { Text("Player name") }, singleLine = true)
                        Button({
                            if (playerName.isNotBlank()) {
                                save(t.copy(players = t.players + TPlayer((t.players.maxOfOrNull { it.id } ?: 0) + 1, playerName.trim())))
                                playerName = ""
                            }
                        }) { Text("Add") }
                    }
                }
                Text(t.players.joinToString { it.name }.ifEmpty { "No players yet." })
            }
        }
        item {
            val last = t.rounds.lastOrNull()
            val canNext = t.players.size >= 2 && (last == null || last.pairings.all { it.played })
            SectionCard("Next round") {
                Text("Crit op for all players this round (Tournament Companion groups):")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApprovedOps.critOpGroups.indices.forEach { g ->
                        Button({
                            val crit = com.example.killteam.logic.MissionGenerator.randomFromGroup(g)
                            save(t.copy(rounds = t.rounds + TRound(crit, TournamentLogic.nextPairings(t))))
                        }, enabled = canNext) { Text("Random from group ${g + 1}") }
                    }
                }
                if (!canNext && t.players.size >= 2) Text("Enter all results of the current round first.")
            }
        }
        t.rounds.forEachIndexed { ri, round ->
            item {
                SectionCard("Round ${ri + 1}  |  ${ApprovedOps.critOps[round.critOpIndex].label}") {
                    round.pairings.forEachIndexed { pi, p ->
                        PairingRow(p, name) { updated ->
                            save(t.copy(rounds = t.rounds.mapIndexed { r, rd -> if (r == ri) rd.copy(pairings = rd.pairings.mapIndexed { k, x -> if (k == pi) updated else x }) else rd }))
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Ranking") {
                val ranking = TournamentLogic.ranking(t)
                if (ranking.isEmpty()) Text("-")
                ranking.forEachIndexed { i, s ->
                    Text("${i + 1}. ${s.player.name}  ${s.wins}-${s.draws}-${s.losses}  path ${s.path.ifEmpty { "-" }}  SoS ${format1(s.strengthOfSchedule)}  primary ${s.primaryVp}")
                }
                Text("Record (win-draw-loss; two draws equal a win and a loss), strength of schedule, then primary-op VP.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PairingRow(p: Pairing, name: (Int?) -> String, onChange: (Pairing) -> Unit) {
    if (p.b == null) {
        Text("${name(p.a)}: bye (counts as a win)")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${name(p.a)}  vs  ${name(p.b)}", fontWeight = FontWeight.Bold)
        Stepper(name(p.a), p.vpA ?: 0, { onChange(p.copy(vpA = it, vpB = p.vpB ?: 0)) }, 0, 40)
        Stepper("  primary", p.primaryA, { onChange(p.copy(primaryA = it)) }, 0, 3)
        Stepper(name(p.b), p.vpB ?: 0, { onChange(p.copy(vpB = it, vpA = p.vpA ?: 0)) }, 0, 40)
        Stepper("  primary", p.primaryB, { onChange(p.copy(primaryB = it)) }, 0, 3)
        Text(if (p.played) "Result entered" else "Result not entered yet (change a score to enter it)", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DicePage(onBack: () -> Unit) {
    var dice by remember { mutableStateOf(listOf<Int>()) }
    var count by remember { mutableStateOf(3) }
    var atk by remember { mutableStateOf(4) }
    var hit by remember { mutableStateOf(3) }
    var normal by remember { mutableStateOf(3) }
    var crit by remember { mutableStateOf(4) }
    var def by remember { mutableStateOf(3) }
    var save by remember { mutableStateOf(4) }
    var wounds by remember { mutableStateOf(8) }
    val result = DamageCalc.calculate(atk, hit, normal, crit, def, save)

    Page("Dice and damage", onBack) {
        item {
            SectionCard("D6 roller") {
                Stepper("Dice", count, { count = it }, 1, 12)
                Button({ dice = List(count) { DamageCalc.rollD6() } }) { Text("Roll") }
                if (dice.isNotEmpty()) Text(dice.joinToString("  ") + "   (sum ${dice.sum()})", style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            SectionCard("Damage calculator") {
                Text("Simplified model: crit saves block crit hits first, then normal hits; normal saves block normal hits only. Special weapon rules are not modelled.", style = MaterialTheme.typography.bodySmall)
                Stepper("Attack dice", atk, { atk = it }, 1, 12)
                Stepper("Hit (X+)", hit, { hit = it }, 2, 6)
                Stepper("Normal dmg", normal, { normal = it }, 1, 10)
                Stepper("Crit dmg", crit, { crit = it }, 1, 12)
                Stepper("Defence dice", def, { def = it }, 0, 6)
                Stepper("Save (X+)", save, { save = it }, 2, 6)
                Stepper("Wounds", wounds, { wounds = it }, 1, 30)
                Text("Expected hits: ${format1(result.expectedHits)}")
                Text("Expected damage: ${format1(result.expectedDamage)}", fontWeight = FontWeight.Bold)
                Text("Chance to deal at least $wounds damage: ${(result.chanceAtLeast(wounds) * 100).toInt()} %")
            }
        }
    }
}

@Composable
private fun FavoritesPage(app: AppState, onBack: () -> Unit) {
    val favorites = app.favorites.value
    Page("Favorites and notes", onBack) {
        item {
            SectionCard("Favorites") {
                if (favorites.isEmpty()) Text("Mark crit ops, tac ops and kill teams with the Favorite button.")
                favorites.forEach { key -> Text(describeKey(app, key)) }
            }
        }
        val notes = app.notes.value.entries.toList()
        item {
            SectionCard("Notes") {
                if (notes.isEmpty()) Text("No notes yet.")
                notes.forEach { (key, text) ->
                    Column {
                        Text(describeKey(app, key), fontWeight = FontWeight.Bold)
                        Text(text)
                    }
                }
            }
        }
    }
}

private fun describeKey(app: AppState, key: String): String {
    val (kind, id) = key.substringBefore(':') to key.substringAfter(':')
    return when (kind) {
        "tac" -> "Tac op: " + (ApprovedOps.tacOps.firstOrNull { it.id == id }?.name ?: id)
        "crit" -> "Crit op: " + (id.toIntOrNull()?.let { ApprovedOps.critOps.getOrNull(it)?.label } ?: id)
        "team" -> "Kill team: " + (app.team(id)?.name ?: id)
        else -> key
    }
}

@Composable
private fun AboutPage(app: AppState, onBack: () -> Unit) {
    Page("Data sources", onBack) {
        item {
            SectionCard("Wahapedia") {
                Text("All rules texts, datacards, maps and images were imported once from wahapedia.ru (Kill Team 3rd edition, Approved Ops 2025) for private, non-commercial use.")
                Text("Content and images are property of Games Workshop and Wahapedia (c) Vyacheslav Maltsev. Do not redistribute this app.")
                Text("Kill team data: ${app.teams.size} teams, imported ${app.teamData?.importedAt ?: "-"}.")
                Text("The data is bundled with the app; re-run tools/import-wahapedia.ps1 (kill teams) and tools/import-rules.ps1 (rules and keywords) to refresh it.")
            }
        }
    }
}
