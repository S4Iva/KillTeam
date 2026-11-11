package com.example.killteam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import com.example.killteam.logic.AppState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.killteam.data.Archetype
import com.example.killteam.data.OpSection
import killteam.shared.generated.resources.Res
import killteam.shared.generated.resources.ic_nav_cards
import killteam.shared.generated.resources.ic_nav_index
import killteam.shared.generated.resources.ic_nav_top
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Own theme (anthracite or paper with Kill-Team orange). Colors that differ by mode read [dark], so composables recompose when it changes; headers stay dark in both modes. */
object Palette {
    /** Set once per composition by the app shell from the Appearance option. */
    var dark: Boolean by mutableStateOf(true)

    val Accent get() = if (dark) Color(0xFFE8650A) else Color(0xFFB84A08)
    val Danger get() = if (dark) Color(0xFFE5645A) else Color(0xFFB3342B)
    val Highlight get() = if (dark) Color(0xFFF2C14E) else Color(0xFF9A6B00)
    val Header = Color(0xFF1F1F1F)
    val OnHeader = Color(0xFFF2F2F2)
    val Card get() = if (dark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val CardHigh get() = if (dark) Color(0xFF2A2A2A) else Color(0xFFECE7DC)
    val CardLine = Color(0xFFD6CFBF)
    val Background get() = if (dark) Color(0xFF121212) else Color(0xFFF4F1EA)
    val Text get() = if (dark) Color(0xFFF2F2F2) else Color(0xFF23211C)
    val Link get() = if (dark) Color(0xFF6FB6FF) else Color(0xFF0B63B5)
    val LinkOnDark = Color(0xFF6FB6FF)
    val Infiltration = Color(0xFF5E7C9A)
    val Recon = Color(0xFFC98A2B)
    val Security = Color(0xFF3F6FB5)
    val SeekAndDestroy = Color(0xFFB5443B)
    val StrategyPloy = Color(0xFF3F6F63)
    val FirefightPloy = Color(0xFF2A2F4A)
    val Equipment = Color(0xFF4A5562)
    val Effect = Color(0xFF4CAF7A)
    val Condition = Color(0xFFE5645A)

    fun archetype(a: Archetype) = when (a) {
        Archetype.Infiltration -> Infiltration
        Archetype.Recon -> Recon
        Archetype.Security -> Security
        Archetype.SeekAndDestroy -> SeekAndDestroy
    }
}

/** Page with an optional back button and a scrolling list of content. */
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    onContents: (() -> Unit)? = null,
    onList: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(Palette.Header).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) AppButton("< Back", onBack, kind = ButtonKind.Text)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(start = 4.dp))
        }
        HorizontalDivider(thickness = 2.dp, color = Palette.Accent.copy(alpha = 0.6f))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 900.dp).fillMaxSize().dragToScroll(listState),
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
            ListScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            Column(Modifier.align(Alignment.BottomEnd).padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = if (isDesktop) 32.dp else 12.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onContents != null) NavButton("Index", Res.drawable.ic_nav_index, onContents)
                if (onList != null) NavButton("Cards", Res.drawable.ic_nav_cards, onList)
                if (listState.canScrollBackward) NavButton("Top", Res.drawable.ic_nav_top) { scope.launch { listState.animateScrollToItem(0) } }
            }
        }
    }
}

/** Spinner while the bundled data loads; shows the error with a retry button if loading failed. */
@Composable
fun LoadStatus(app: AppState, what: String) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (app.loading) {
            CircularProgressIndicator(color = Palette.Accent)
            Text("Loading $what...")
        } else {
            Text("Could not load $what: ${app.loadError ?: "unknown error"}", color = MaterialTheme.colorScheme.error)
            AppButton("Retry", { scope.launch { app.load() } })
        }
    }
}

/** Asks before an action that cannot be undone; [danger] makes the confirm button red for actions that delete or discard data. */
@Composable
fun ConfirmDialog(text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { AppButton(confirmLabel, { onDismiss(); onConfirm() }, kind = if (danger) ButtonKind.DangerSolid else ButtonKind.Primary) },
        dismissButton = { AppButton("Cancel", onDismiss, kind = ButtonKind.Text) },
    )
}

/** Dialog with a list of entries to jump to. */
@Composable
fun JumpDialog(title: String, entries: List<String>, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title.uppercase()) },
        text = {
            val scroll = rememberScrollState()
            Column(Modifier.heightIn(max = 420.dp).dragToScroll(scroll).verticalScroll(scroll)) {
                entries.forEachIndexed { i, e ->
                    Text(
                        e, Modifier.fillMaxWidth().clickable { onPick(i); onDismiss() }.padding(horizontal = 4.dp, vertical = 12.dp),
                        color = Palette.Accent, textAlign = TextAlign.Start,
                    )
                }
            }
        },
        confirmButton = { AppButton("Close", onDismiss, kind = ButtonKind.Text) },
    )
}


/** Rounded card with a dark title bar and a coloured stripe on its left edge. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    headerColor: Color = Palette.Accent,
    icon: DrawableResource? = null,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Card),
        border = if (Palette.dark) null else BorderStroke(1.dp, Palette.CardLine),
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(Palette.Header), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(headerColor))
            Row(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) Image(painterResource(icon), null, Modifier.padding(end = 8.dp).size(36.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1f))
                actions?.invoke()
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

/** Frames a card briefly after a link (search hit, favorite) led to it. */
@Composable
fun HitFrame(on: Boolean, content: @Composable () -> Unit) {
    Box(if (on) Modifier.border(3.dp, Palette.Highlight, RoundedCornerShape(14.dp)) else Modifier) { content() }
}

@Composable
fun OpSectionsView(sections: List<OpSection>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sections.forEach { s ->
            if (s.title.startsWith("MISSION ACTION")) {
                ActionBlock(s)
            } else {
                Text(s.title, style = MaterialTheme.typography.labelLarge, color = Palette.Accent, fontWeight = FontWeight.Bold)
                s.lines.forEach { KeywordText(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

/** Mission action: accent bar with name and AP, green triangle for the effect, red diamond for the conditions. */
@Composable
private fun ActionBlock(s: OpSection) {
    val body = s.title.removePrefix("MISSION ACTION: ")
    val name = body.substringBeforeLast(" (")
    val ap = body.substringAfterLast("(", "").removeSuffix(")")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionBar(name, ap)
        s.lines.forEachIndexed { i, line ->
            when {
                i == 0 -> BulletRow(true, line)
                i == s.lines.lastIndex -> BulletRow(false, line)
                else -> KeywordText(line, Modifier.padding(start = 26.dp), MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ActionBar(name: String, ap: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Palette.Accent).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(ap, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** Effect (green triangle) or condition (red diamond) line of an action or rule. */
@Composable
fun BulletRow(effect: Boolean, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            if (effect) "\u25B6" else "\u25C6", Modifier.width(26.dp),
            color = if (effect) Palette.Effect else Palette.Condition, style = MaterialTheme.typography.bodyMedium,
        )
        KeywordText(text, Modifier.weight(1f), MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun <T> Dropdown(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        AppButton("$label: ${text(selected)}", { open = true }, Modifier.fillMaxWidth(), ButtonKind.Secondary)
        DropdownMenu(open, { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(text(o)) }, onClick = { open = false; onSelect(o) })
            }
        }
    }
}

@Composable
fun NoteField(key: String, current: String, onSave: (String) -> Unit) {
    var text by remember(key, current) { mutableStateOf(current) }
    OutlinedTextField(
        text, { text = it; onSave(it) },
        Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 2,
    )
}
