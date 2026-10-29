package com.example.killteam.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.killteam.data.LookupTerm
import com.example.killteam.data.TermMatcher
import com.example.killteam.logic.RuleRef

class KeywordContext(val matcher: TermMatcher?, val openRule: (RuleRef) -> Unit)

val LocalKeywords = compositionLocalOf { KeywordContext(null) {} }

private val CapsRun = Regex("[A-Z][A-Z0-9.*'-]+(?: [A-Z][A-Z0-9.*'-]*)*")

/** Appends plain text; when [accent] is set, capitalised keywords become bold and the accent phrase orange. */
private fun AnnotatedString.Builder.appendPlain(s: String, accent: String?) {
    if (accent == null) { append(s); return }
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    var pos = 0
    CapsRun.findAll(s).forEach { m ->
        append(s.substring(pos, m.range.first))
        val run = m.value
        val i = if (accent.isNotEmpty()) run.indexOf(accent) else -1
        if (i < 0) {
            withStyle(bold) { append(run) }
        } else {
            withStyle(bold) { append(run.substring(0, i)) }
            withStyle(bold.copy(color = Waha.Orange)) { append(accent) }
            withStyle(bold) { append(run.substring(i + accent.length)) }
        }
        pos = m.range.last + 1
    }
    append(s.substring(pos))
}

/** Text whose keywords are tappable: a small popup shows the definition, its arrow opens the rule. */
@Composable
fun KeywordText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    boldPrefix: String = "",
    linkColor: Color = Waha.Link,
    capsAccent: String? = null,
) {
    val ctx = LocalKeywords.current
    val selected = remember { mutableStateOf<LookupTerm?>(null) }
    val annotated = remember(text, boldPrefix, linkColor, capsAccent, ctx.matcher) {
        buildAnnotatedString {
            if (boldPrefix.isNotEmpty()) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(boldPrefix) }
            var pos = 0
            ctx.matcher?.find(text).orEmpty().forEach { m ->
                if (m.start < pos) return@forEach
                appendPlain(text.substring(pos, m.start), capsAccent)
                val link = LinkAnnotation.Clickable(
                    tag = m.term.phrase,
                    styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                ) { selected.value = m.term }
                withLink(link) { append(text.substring(m.start, m.end)) }
                pos = m.end
            }
            appendPlain(text.substring(pos), capsAccent)
        }
    }
    Text(annotated, modifier, style = style, color = color)

    selected.value?.let { term ->
        AlertDialog(
            onDismissRequest = { selected.value = null },
            title = { Text(term.title.ifBlank { term.phrase }.uppercase()) },
            text = {
                val scroll = rememberScrollState()
                Text(
                    term.text.ifBlank { "No description available." },
                    Modifier.heightIn(max = 320.dp).dragToScroll(scroll).verticalScroll(scroll),
                )
            },
            confirmButton = {
                if (term.hasRule) {
                    Button({
                        selected.value = null
                        ctx.openRule(RuleRef(term.page!!, term.anchor!!))
                    }) { Text("Open rule  \u2192") }
                }
            },
            dismissButton = { TextButton({ selected.value = null }) { Text("Close") } },
        )
    }
}
