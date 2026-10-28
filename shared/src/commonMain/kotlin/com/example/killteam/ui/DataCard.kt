package com.example.killteam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.killteam.data.AbilityBlock
import com.example.killteam.data.Operative
import com.example.killteam.data.Weapon
import killteam.shared.generated.resources.Res
import killteam.shared.generated.resources.ds_apl
import killteam.shared.generated.resources.ds_melee
import killteam.shared.generated.resources.ds_move
import killteam.shared.generated.resources.ds_ranged
import killteam.shared.generated.resources.ds_save
import killteam.shared.generated.resources.ds_wounds
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val CardBackground = Color(0xFF262626)
private val RowDark = Color(0xFF2E2E2E)
private val RowLight = Color(0xFF3B3B3B)
private val StatBackground = Color(0xFF1A1A1A)
private val Muted = Color(0xFFCCCCCC)

/** Operative datacard in the style of the Wahapedia datacards. */
@Composable
fun OperativeDatacard(o: Operative) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground)) {
        CardHeader(o)
        HorizontalDivider(thickness = 2.dp, color = Waha.Orange)
        WeaponTable(o.weapons)
        if (o.blocks.isNotEmpty()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                o.blocks.forEach { AbilityView(it) }
            }
        }
        CardFooter(o)
    }
}

@Composable
private fun CardHeader(o: Operative) {
    BoxWithConstraints(Modifier.fillMaxWidth().background(Waha.Black)) {
        val wide = maxWidth >= 560.dp
        val stats: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatBox("APL", Res.drawable.ds_apl, o.apl)
                StatBox("MOVE", Res.drawable.ds_move, o.move)
                StatBox("SAVE", Res.drawable.ds_save, o.save)
                StatBox("WOUNDS", Res.drawable.ds_wounds, o.wounds)
            }
        }
        val title: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(o.name.uppercase(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                if (o.flavor.isNotBlank()) Text(o.flavor, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = Muted)
            }
        }
        if (wide) {
            Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                title(Modifier.weight(1f))
                stats()
            }
        } else {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                title(Modifier.fillMaxWidth())
                stats()
            }
        }
    }
}

@Composable
private fun StatBox(label: String, icon: DrawableResource, value: String) {
    Column(
        Modifier.width(66.dp).clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)).background(StatBackground).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, letterSpacing = 1.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Image(painterResource(icon), null, Modifier.height(18.dp).width(18.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun WeaponTable(weapons: List<Weapon>) {
    if (weapons.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        val wide = maxWidth >= 560.dp
        val numberWidth: Dp = if (wide) 44.dp else 34.dp
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(34.dp)
                HeaderCell("NAME", Modifier.weight(if (wide) 1.3f else 1f), TextAlign.Start)
                HeaderCell("ATK", Modifier.width(numberWidth))
                HeaderCell("HIT", Modifier.width(numberWidth))
                HeaderCell("DMG", Modifier.width(numberWidth + 8.dp))
                if (wide) HeaderCell("WR", Modifier.weight(1f).padding(start = 8.dp), TextAlign.Start)
            }
            HorizontalDivider(thickness = 1.dp, color = Waha.Orange)
            weapons.forEachIndexed { i, w ->
                Row(
                    Modifier.fillMaxWidth().background(if (i % 2 == 0) RowDark else RowLight).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(if (w.isMelee) Res.drawable.ds_melee else Res.drawable.ds_ranged), null, Modifier.padding(end = 6.dp).size(width = 28.dp, height = 20.dp))
                    if (wide) {
                        Text(w.name, Modifier.weight(1.3f), style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    } else {
                        Column(Modifier.weight(1f)) {
                            Text(w.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            if (w.wr != "-") KeywordText(w.wr, style = MaterialTheme.typography.bodySmall, color = Muted, linkColor = Color.White)
                        }
                    }
                    ValueCell(w.atk, Modifier.width(numberWidth))
                    ValueCell(w.hit, Modifier.width(numberWidth))
                    ValueCell(w.dmg, Modifier.width(numberWidth + 8.dp))
                    if (wide) {
                        Box(Modifier.weight(1f).padding(start = 8.dp)) {
                            if (w.wr == "-") Text("-", color = Color.White) else KeywordText(w.wr, color = Color.White, linkColor = Color.White)
                        }
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = Waha.Orange)
        }
    }
}

@Composable
private fun Spacer(width: Dp) = Box(Modifier.width(width))

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign = TextAlign.Center) {
    Text(text, modifier, style = MaterialTheme.typography.labelLarge, color = Color.White, textAlign = align, letterSpacing = 1.sp)
}

@Composable
private fun ValueCell(text: String, modifier: Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
}

@Composable
private fun AbilityView(block: AbilityBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (block.isAction) {
            ActionBar(block.name, block.ap)
            block.paras.forEach { p ->
                when (p.kind) {
                    "effect" -> BulletRow(effectIcon, p.text)
                    "condition" -> BulletRow(conditionIcon, p.text)
                    "bullet" -> DotRow(p.text)
                    else -> KeywordText(p.text, Modifier.padding(start = 26.dp), MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            var named = false
            if (block.paras.isEmpty() && block.name.isNotBlank()) Text(block.name, fontWeight = FontWeight.Bold)
            block.paras.forEach { p ->
                when (p.kind) {
                    "bullet" -> DotRow(p.text)
                    else -> {
                        val prefix = if (!named && block.name.isNotBlank()) "${block.name}: " else ""
                        named = true
                        KeywordText(p.text, style = MaterialTheme.typography.bodyMedium, boldPrefix = prefix)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardFooter(o: Operative) {
    val keywords = o.keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    Row(Modifier.fillMaxWidth().background(Waha.Black).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        FlowRow(Modifier.weight(1f)) {
            keywords.forEachIndexed { i, k ->
                val color = if (i == 0) Waha.Orange else Color.White
                KeywordText(
                    k.uppercase() + if (i < keywords.lastIndex) ",  " else "",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = color, linkColor = color,
                )
            }
        }
        if (o.baseSize.isNotBlank()) Text("\u2300${o.baseSize}", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
