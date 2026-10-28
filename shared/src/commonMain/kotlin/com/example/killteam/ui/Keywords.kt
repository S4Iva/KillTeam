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

/** Text whose keywords are tappable: a small popup shows the definition, its arrow opens the rule. */
@Composable
fun KeywordText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    boldPrefix: String = "",
    linkColor: Color = Waha.Link,
) {
    val ctx = LocalKeywords.current
    val selected = remember { mutableStateOf<LookupTerm?>(null) }
    val annotated = remember(text, boldPrefix, linkColor, ctx.matcher) {
        buildAnnotatedString {
            if (boldPrefix.isNotEmpty()) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(boldPrefix) }
            var pos = 0
            ctx.matcher?.find(text).orEmpty().forEach { m ->
                if (m.start < pos) return@forEach
                append(text.substring(pos, m.start))
                val link = LinkAnnotation.Clickable(
                    tag = m.term.phrase,
                    styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                ) { selected.value = m.term }
                withLink(link) { append(text.substring(m.start, m.end)) }
                pos = m.end
            }
            append(text.substring(pos))
        }
    }
    Text(annotated, modifier, style = style, color = color)

    selected.value?.let { term ->
        AlertDialog(
            onDismissRequest = { selected.value = null },
            title = { Text(term.title.ifBlank { term.phrase }.uppercase()) },
            text = {
                Text(
                    term.text.ifBlank { "No description available." },
                    Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
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
