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
        val choices = team.weaponOptions[roster.operatives[slot]]
        val option = roster.optionAt(slot)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(roster.slotLabel(slot), fontWeight = FontWeight.Bold)
            if (choices == null) {
                Text("Standard weapons", style = MaterialTheme.typography.bodySmall, color = Palette.Text.copy(alpha = 0.7f))
            } else {
                Text("Weapons", style = MaterialTheme.typography.labelMedium, color = if (option.isBlank()) Palette.Highlight else Palette.Text.copy(alpha = 0.7f))
                choices.forEach { c ->
                    val chosen = option == c.label
                    val block = if (chosen) null else RosterRules.optionBlock(team, roster, slot, c.label)
                    ChoiceRow(chosen, true, c.label, null, block, block == null) { onChange(roster.withOption(slot, c.label)) }
                }
            }
            val carried = roster.equipmentOf(slot)
            if (carried.isEmpty()) Text("No equipment", style = MaterialTheme.typography.labelSmall, color = Palette.Text.copy(alpha = 0.5f))
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { carried.forEach { Pill(it) } }
            AppButton("Weapon stats and equipment", { onEdit(slot) }, kind = ButtonKind.Secondary, size = ButtonSize.S, fullWidth = true)
        }
    }
    RosterRules.problems(team, roster).forEach { Text(it, color = MaterialTheme.colorScheme.error) }
    RosterRules.warnings(team, roster).forEach { Text(it, color = Palette.Highlight) }
}
