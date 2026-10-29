package com.example.killteam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.killteam.data.AbilityPara
import com.example.killteam.data.KillTeam

private val Muted = Color(0xFFBBBBBB)

/** Rule text in the Wahapedia style: green arrow for effects, red diamond for conditions, orange squares for lists. */
@Composable
fun RichParas(paras: List<AbilityPara>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        paras.forEach { p ->
            when (p.kind) {
                "effect" -> BulletRow(effectIcon, p.text)
                "condition" -> BulletRow(conditionIcon, p.text)
                "bullet" -> DotRow(p.text)
                "flavor" -> KeywordText(p.text, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = Muted)
                "header" -> Text(
                    p.text.uppercase(),
                    Modifier.fillMaxWidth().background(Waha.Orange).padding(horizontal = 10.dp, vertical = 5.dp),
                    color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                )
                else -> KeywordText(p.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Team composition as on Wahapedia: orange arrows for the picks, squares for the options, faction keyword in orange. */
@Composable
fun CompositionList(team: KillTeam, style: TextStyle = MaterialTheme.typography.bodyMedium, modifier: Modifier = Modifier) {
    val accent = team.factionKeyword.ifBlank { team.name.uppercase() }
    if (team.compositionParas.isEmpty()) {
        Column(modifier) { team.composition.forEach { KeywordText(it, style = style) } }
        return
    }
    val paras = team.compositionParas
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        paras.forEachIndexed { i, p ->
            when (p.kind) {
                "arrow" -> Row(verticalAlignment = Alignment.Top) {
                    Text("\u2198", Modifier.padding(end = 8.dp), color = Waha.Orange, fontWeight = FontWeight.Bold, style = style)
                    KeywordText(p.text, Modifier.weight(1f), style, capsAccent = accent)
                }
                "bullet" -> Row(Modifier.padding(start = 26.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 7.dp, end = 8.dp).size(6.dp).background(Waha.Orange))
                    KeywordText(p.text, Modifier.weight(1f), style, capsAccent = accent)
                }
                else -> {
                    val inList = paras.drop(i + 1).any { it.kind == "arrow" }
                    KeywordText(p.text, Modifier.padding(start = if (inList) 26.dp else 0.dp), style, capsAccent = accent)
                }
            }
        }
    }
}

@Composable
fun DotRow(text: String) {
    Row(Modifier.padding(start = 6.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp, end = 8.dp).size(6.dp).background(Waha.Orange))
        KeywordText(text, Modifier.weight(1f), MaterialTheme.typography.bodyMedium)
    }
}
