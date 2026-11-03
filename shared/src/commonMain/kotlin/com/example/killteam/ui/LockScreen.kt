package com.example.killteam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.killteam.logic.AccessCode
import com.example.killteam.logic.AccessResult

@Composable
fun LockScreen(startReason: AccessResult?, onSubmit: (String) -> AccessResult) {
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf(startReason) }
    var showTerms by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val submit = { result = onSubmit(code) }

    Column(
        Modifier.fillMaxSize().dragToScroll(scroll).verticalScroll(scroll).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("KILL TEAM", style = MaterialTheme.typography.headlineMedium, color = Palette.Accent)
        Text("Enter your access code to unlock the app. Paste it from the message you received.")
        OutlinedTextField(
            code, { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(AccessCode.LENGTH); if (result == AccessResult.Wrong) result = null },
            Modifier.widthIn(max = 360.dp).fillMaxWidth(),
            label = { Text("Access code") }, minLines = 3, maxLines = 6, isError = result == AccessResult.Wrong,
            visualTransformation = CodeTransformation,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii),
        )
        when (result) {
            AccessResult.Wrong -> Text("Wrong code.", color = MaterialTheme.colorScheme.error)
            AccessResult.Expired -> Text("Code expired. Enter a new code.", color = MaterialTheme.colorScheme.error)
            AccessResult.ClockChanged -> Text("The system clock was set back. Correct the date and time to continue.", color = MaterialTheme.colorScheme.error)
            else -> Unit
        }
        Button(submit, enabled = code.isNotBlank()) { Text("Unlock") }
        Text("By entering the code you accept the terms of use.", style = MaterialTheme.typography.bodySmall)
        TextButton({ showTerms = !showTerms }) { Text(if (showTerms) "Hide terms of use" else "Show terms of use") }
        if (showTerms) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                termsOfUse.forEach { (title, text) ->
                    Text(title, fontWeight = FontWeight.Bold, color = Palette.Accent, style = MaterialTheme.typography.bodyMedium)
                    Text(text, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

internal val termsOfUse = listOf(
    "Purpose" to "This is a private, non-commercial test version, provided for trying out and for feedback.",
    "No sharing" to "Do not pass on, upload or publish the app, the installer, the APK or the access code, and do not share screenshots or videos that show larger parts of the content. Use the app only on your own devices.",
    "Access code" to "The code is for you only and may be time-limited. After it expires the app locks until you get a new code. Do not bypass the check (for example by setting the system clock back or by taking the app apart) and do not create your own codes.",
    "Copyright" to "Rules, texts, data and images belong to Games Workshop. Warhammer, Kill Team and related names are trademarks of Games Workshop. This app is unofficial and not affiliated with Games Workshop. You only get a simple, non-transferable right to use it privately. Using the content outside the app is not allowed.",
    "No reverse engineering" to "Do not decompile or modify the app, and do not extract content from it.",
    "Liability" to "The test version is provided as is and may contain errors. Rules may be outdated or wrong; the official rules apply. Liability is limited to intent and gross negligence as far as the law allows.",
    "Data" to "The app works offline. There are no accounts, no telemetry and no ads. Your data (rosters, notes, games) stays on your device.",
    "Ending access" to "Access can be ended at any time. When it expires or on request, delete the app.",
)

/** Shows the typed characters in groups of 5 separated by dashes while the text itself stays without dashes, so the cursor does not jump. */
private object CodeTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = text.text.chunked(5).joinToString("-")
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + (offset - 1).coerceAtLeast(0) / 5
            override fun transformedToOriginal(offset: Int) = offset - offset / 6
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}
