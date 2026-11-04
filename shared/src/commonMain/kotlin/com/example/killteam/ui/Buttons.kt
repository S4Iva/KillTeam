package com.example.killteam.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Role of a button; see anforderungs.md 4.21 (BT-1, BT-2). Choice, segmented and icon buttons have their own composables below. */
enum class ButtonKind {
    /** The one main action of a card or page. */
    Primary,

    /** Further actions, show/hide, opening sub flows. */
    Secondary,

    /** Cancel, close, back and link-like actions. */
    Text,

    /** Deletes or discards data. */
    Danger,

    /** Confirm button of a dialog that deletes or discards data. */
    DangerSolid,
}

/** Fixed heights (BT-3). S for rows of actions and steppers, M is the default, L only for hand-over and finishing actions. */
enum class ButtonSize(val height: Dp) { S(40.dp), M(48.dp), L(56.dp) }

private val ButtonShape = RoundedCornerShape(50)

/** The only way to put a text button on screen: [kind] sets the look, [size] the height, [fullWidth] fills the card. */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    size: ButtonSize = ButtonSize.M,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
) {
    val m = modifier.heightIn(min = size.height).let { if (fullWidth) it.fillMaxWidth() else it }
    val padding = PaddingValues(horizontal = if (size == ButtonSize.S) 14.dp else 20.dp, vertical = 6.dp)
    val label: @Composable () -> Unit = {
        Text(
            text, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold,
            style = if (size == ButtonSize.L) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
        )
    }
    val off = 0.38f
    when (kind) {
        ButtonKind.Primary -> Button(
            onClick, m, enabled, shape = ButtonShape, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.Accent, contentColor = Color.White,
                disabledContainerColor = Palette.Accent.copy(alpha = off), disabledContentColor = Color.White.copy(alpha = off),
            ),
        ) { label() }
        ButtonKind.DangerSolid -> Button(
            onClick, m, enabled, shape = ButtonShape, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.Danger, contentColor = Color.White,
                disabledContainerColor = Palette.Danger.copy(alpha = off), disabledContentColor = Color.White.copy(alpha = off),
            ),
        ) { label() }
        ButtonKind.Secondary -> OutlinedButton(
            onClick, m, enabled, shape = ButtonShape, contentPadding = padding,
            border = BorderStroke(1.5.dp, Palette.Accent.copy(alpha = if (enabled) 1f else off)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Accent, disabledContentColor = Palette.Accent.copy(alpha = off)),
        ) { label() }
        ButtonKind.Danger -> OutlinedButton(
            onClick, m, enabled, shape = ButtonShape, contentPadding = padding,
            border = BorderStroke(1.5.dp, Palette.Danger.copy(alpha = if (enabled) 1f else off)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Danger, disabledContentColor = Palette.Danger.copy(alpha = off)),
        ) { label() }
        ButtonKind.Text -> TextButton(
            onClick, m, enabled, shape = ButtonShape, contentPadding = padding,
            colors = ButtonDefaults.textButtonColors(contentColor = Palette.Accent, disabledContentColor = Palette.Accent.copy(alpha = off)),
        ) { label() }
    }
}

/** Equally wide group of 2 to 3 options that are always visible; the picked one is filled. */
@Composable
fun <T> SegmentedChoice(options: List<T>, selected: T?, label: (T) -> String, onPick: (T) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            AppButton(
                label(o), { onPick(o) }, Modifier.weight(1f),
                kind = if (o == selected) ButtonKind.Primary else ButtonKind.Secondary, enabled = enabled,
            )
        }
    }
}

/** Symbol-only button with a 48 dp touch area. */
@Composable
fun IconAction(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = Palette.Accent, fontSize = 24.sp)
    }
}

@Composable
fun FavoriteButton(active: Boolean, onToggle: () -> Unit) {
    IconAction(if (active) "\u2605" else "\u2606", if (active) "Remove favorite" else "Add favorite", onToggle)
}

/** Choice buttons (chips): one or more options, the chosen ones are filled. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipRow(options: List<T>, isSelected: (T) -> Boolean, text: (T) -> String, onClick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { o ->
            FilterChip(
                isSelected(o), { onClick(o) }, { Text(text(o)) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Palette.Accent, selectedLabelColor = Color.White),
            )
        }
    }
}

/** Counter: label on the left, `-` value `+` at the right edge (DS-7). */
@Composable
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f))
        AppButton("-", { onChange((value - 1).coerceAtLeast(min)) }, Modifier.widthIn(min = 48.dp), ButtonKind.Secondary, ButtonSize.S, enabled = value > min)
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 32.dp))
        AppButton("+", { onChange((value + 1).coerceAtMost(max)) }, Modifier.widthIn(min = 48.dp), ButtonKind.Secondary, ButtonSize.S, enabled = value < max)
    }
}

/** Small pill floating over the page (index, card list, back to top). */
@Composable
fun NavButton(label: String, icon: DrawableResource, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Palette.Header).clickable(onClick = onClick).padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(18.dp))
        Text(label, color = Palette.Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}
