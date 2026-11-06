package com.example.killteam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val expected = RosterRules.expectedSize(team)
    val room = if (expected > 0) (expected - roster.operatives.size).coerceAtLeast(0) else 99

    Text("Operatives (${roster.operatives.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Palette.Accent)
    RosterBudget(roster, team)
    team.operatives.distinctBy { it.name }.forEach { op ->
        val count = roster.operatives.count { it == op.name }
        val open = op.name in openCards
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Stepper(op.name, count, { v -> onChange(if (v > count) roster.withAdded(op.name) else roster.withRemovedLast(op.name)) }, 0, count + room)
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
        val hasOptions = team.weaponOptions[roster.operatives[slot]] != null
        val option = roster.optionAt(slot)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(roster.slotLabel(slot), fontWeight = FontWeight.Bold)
            Text(
                if (!hasOptions) "Standard weapons" else option.ifBlank { "Choose weapons" },
                style = MaterialTheme.typography.bodySmall,
                color = if (hasOptions && option.isBlank()) Palette.Highlight else Palette.Text.copy(alpha = 0.7f),
            )
            val carried = roster.equipmentOf(slot)
            if (carried.isEmpty()) Text("No equipment", style = MaterialTheme.typography.labelSmall, color = Palette.Text.copy(alpha = 0.5f))
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { carried.forEach { Pill(it) } }
            AppButton("Weapons and equipment", { onEdit(slot) }, kind = ButtonKind.Secondary, size = ButtonSize.S, fullWidth = true)
        }
    }
    RosterRules.problems(team, roster).forEach { Text(it, color = MaterialTheme.colorScheme.error) }
    RosterRules.warnings(team, roster).forEach { Text(it, color = Palette.Highlight) }
}
