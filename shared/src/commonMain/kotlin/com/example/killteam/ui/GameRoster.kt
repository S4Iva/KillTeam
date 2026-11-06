package com.example.killteam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
import com.example.killteam.data.KillTeam
import com.example.killteam.logic.Roster
import com.example.killteam.logic.RosterRules

/** Operatives, weapons and equipment for a player who picked a kill team but no roster; [roster] is a temporary roster that is never saved. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoRosterOperatives(team: KillTeam, roster: Roster, onChange: (Roster) -> Unit, onEdit: (slot: Int) -> Unit) {
    var openCards by remember(team.slug) { mutableStateOf(setOf<String>()) }
    var statsOpen by remember(team.slug) { mutableStateOf(setOf<String>()) }
    var openUnits by remember(team.slug) { mutableStateOf(setOf<Int>()) }
    val room = { card: String -> RosterRules.room(team, roster, card) }

    Text("Operatives (${roster.operatives.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Palette.Accent)
    RosterBudget(roster, team)
    team.operatives.distinctBy { it.name }.forEach { op ->
        val count = roster.operatives.count { it == op.name }
        val open = op.name in openCards
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Stepper(op.name, count, { v -> onChange(if (v > count) roster.withAdded(op.name) else roster.withRemovedLast(op.name)) }, 0, count + room(op.name))
            AppButton(
                if (open) "Hide datacard" else "Show datacard", { openCards = if (open) openCards - op.name else openCards + op.name },
                kind = ButtonKind.Secondary, size = ButtonSize.S, fullWidth = true,
            )
            if (open) OperativeDatacard(op)
        }
    }

    if (roster.operatives.isEmpty()) {
        Text("No operatives selected: the game starts without operatives.", style = MaterialTheme.typography.bodySmall)
        return
    }
    Text("Selected operatives", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Palette.Accent)
    roster.operatives.indices.forEach { slot ->
        val datacard = team.operatives.firstOrNull { it.name == roster.operatives[slot] }
        val choices = team.weaponOptions[roster.operatives[slot]]
        val option = roster.optionAt(slot)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val unitOpen = slot in openUnits
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(roster.slotLabel(slot), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Chevron(unitOpen, { openUnits = openUnits.toggledInt(slot) }, "operative")
            }
            if (!unitOpen) {
                Text(
                    if (choices == null) "All weapons of the datacard" else option.ifBlank { "Choose weapons" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (choices != null && option.isBlank()) Palette.Highlight else Palette.Text.copy(alpha = 0.7f),
                )
                val held = roster.equipmentOf(slot)
                if (held.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { held.forEach { Pill(it) } }
                return@Column
            }
            if (choices == null) {
                Text("All weapons of the datacard", style = MaterialTheme.typography.bodySmall, color = Palette.Text.copy(alpha = 0.7f))
                if (datacard != null) WeaponStatsToggle("$slot:" in statsOpen, { statsOpen = statsOpen.toggled("$slot:") }, datacard.weapons)
            } else {
                Text("Weapons", style = MaterialTheme.typography.labelMedium, color = if (option.isBlank()) Palette.Highlight else Palette.Text.copy(alpha = 0.7f))
                choices.forEach { c ->
                    val chosen = option == c.label
                    val block = if (chosen) null else RosterRules.optionBlock(team, roster, slot, c.label)
                    ChoiceRow(chosen, true, c.label, null, block, block == null) { onChange(roster.withOption(slot, c.label)) }
                    val key = "$slot:${c.label}"
                    if (datacard != null) WeaponStatsToggle(key in statsOpen, { statsOpen = statsOpen.toggled(key) }, datacard.weapons.filter { it.name in c.weapons })
                }
            }
            val carried = roster.equipmentOf(slot)
            if (carried.isEmpty()) Text("No equipment", style = MaterialTheme.typography.labelSmall, color = Palette.Text.copy(alpha = 0.5f))
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { carried.forEach { Pill(it) } }
            AppButton("Equipment", { onEdit(slot) }, kind = ButtonKind.Secondary, size = ButtonSize.S, fullWidth = true)
        }
    }
    RosterRules.problems(team, roster).forEach { Text(it, color = MaterialTheme.colorScheme.error) }
    RosterRules.warnings(team, roster).forEach { Text(it, color = Palette.Highlight) }
}

private fun Set<String>.toggled(key: String) = if (key in this) this - key else this + key

private fun Set<Int>.toggledInt(key: Int) = if (key in this) this - key else this + key
