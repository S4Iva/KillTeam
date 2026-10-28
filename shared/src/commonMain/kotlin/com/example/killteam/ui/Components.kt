package com.example.killteam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.killteam.data.Archetype
import com.example.killteam.data.OpSection
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Colours taken from the Wahapedia stylesheet (dark theme). */
object Waha {
    val Orange = Color(0xFFF15C23)
    val Black = Color(0xFF000000)
    val Card = Color(0xFF302F2B)
    val Background = Color(0xFF202020)
    val Text = Color(0xFFDADADA)
    val Link = Color(0xFF8080FF)
    val Infiltration = Color(0xFF69707E)
    val Recon = Color(0xFFE26633)
    val Security = Color(0xFF3C4B80)
    val SeekAndDestroy = Color(0xFFE44130)
    val StrategyPloy = Color(0xFF6F7F76)
    val FirefightPloy = Color(0xFF141011)
    val Equipment = Color(0xFF4A4A4A)

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
        Row(Modifier.fillMaxWidth().background(Waha.Black).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) TextButton(onBack) { Text("< Back") }
            Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.padding(start = 4.dp))
        }
        HorizontalDivider(thickness = 3.dp, color = Waha.Orange)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 900.dp).fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
            Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onContents != null) NavButton(navContentsIcon, onContents)
                if (onList != null) NavButton(navListIcon, onList)
                if (listState.canScrollBackward) NavButton(navTopIcon) { scope.launch { listState.animateScrollToItem(0) } }
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
            CircularProgressIndicator(color = Waha.Orange)
            Text("Loading $what...")
        } else {
            Text("Could not load $what: ${app.loadError ?: "unknown error"}", color = MaterialTheme.colorScheme.error)
            Button({ scope.launch { app.load() } }) { Text("Retry") }
        }
    }
}

/** Octagon button like the Wahapedia side buttons (contents, datacard list, back to top). */
@Composable
fun NavButton(icon: DrawableResource, onClick: () -> Unit) {
    Image(painterResource(icon), null, Modifier.size(52.dp).clickable(onClick = onClick))
}

/** Dialog with a list of entries to jump to. */
@Composable
fun JumpDialog(title: String, entries: List<String>, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title.uppercase()) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                entries.forEachIndexed { i, e ->
                    TextButton({ onPick(i); onDismiss() }, Modifier.fillMaxWidth()) {
                        Text(e, Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Close") } },
    )
}


/** Card with a coloured header bar like the Wahapedia cards; the default header is black. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    headerColor: Color = Waha.Black,
    icon: DrawableResource? = null,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Waha.Card),
    ) {
        Row(Modifier.fillMaxWidth().background(headerColor).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Image(painterResource(icon), null, Modifier.padding(end = 8.dp).size(36.dp))
            Text(title.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            actions?.invoke()
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
fun OpSectionsView(sections: List<OpSection>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sections.forEach { s ->
            if (s.title.startsWith("MISSION ACTION")) {
                ActionBlock(s)
            } else {
                Text(s.title, style = MaterialTheme.typography.labelLarge, color = Waha.Orange, fontWeight = FontWeight.Bold)
                s.lines.forEach { KeywordText(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

/** Mission action like on Wahapedia: orange bar with name and AP, green arrow for the effect, red diamond for the conditions. */
@Composable
private fun ActionBlock(s: OpSection) {
    val body = s.title.removePrefix("MISSION ACTION: ")
    val name = body.substringBeforeLast(" (")
    val ap = body.substringAfterLast("(", "").removeSuffix(")")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionBar(name, ap)
        s.lines.forEachIndexed { i, line ->
            when {
                i == 0 -> BulletRow(effectIcon, line)
                i == s.lines.lastIndex -> BulletRow(conditionIcon, line)
                else -> KeywordText(line, Modifier.padding(start = 26.dp), MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ActionBar(name: String, ap: String) {
    Row(Modifier.fillMaxWidth().background(Waha.Orange).padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name.uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(ap.uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
fun BulletRow(icon: DrawableResource, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Image(painterResource(icon), null, Modifier.padding(top = 3.dp, end = 8.dp).size(18.dp))
        KeywordText(text, Modifier.weight(1f), MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun <T> Dropdown(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton({ open = true }, Modifier.fillMaxWidth()) {
            Text("$label: ${text(selected)}", maxLines = 1)
        }
        DropdownMenu(open, { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(text(o)) }, onClick = { open = false; onSelect(o) })
            }
        }
    }
}

@Composable
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.widthIn(min = 90.dp))
        OutlinedButton({ onChange((value - 1).coerceAtLeast(min)) }, enabled = value > min) { Text("-") }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 28.dp))
        OutlinedButton({ onChange((value + 1).coerceAtMost(max)) }, enabled = value < max) { Text("+") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipRow(options: List<T>, isSelected: (T) -> Boolean, text: (T) -> String, onClick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { o ->
            FilterChip(
                isSelected(o), { onClick(o) }, { Text(text(o)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Waha.Orange, selectedLabelColor = Color.White),
            )
        }
    }
}

@Composable
fun FavoriteButton(active: Boolean, onToggle: () -> Unit) {
    TextButton(onToggle) { Text(if (active) "Favorite *" else "Favorite") }
}

@Composable
fun NoteField(key: String, current: String, onSave: (String) -> Unit) {
    var text by remember(key, current) { mutableStateOf(current) }
    OutlinedTextField(
        text, { text = it; onSave(it) },
        Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 2,
    )
}
