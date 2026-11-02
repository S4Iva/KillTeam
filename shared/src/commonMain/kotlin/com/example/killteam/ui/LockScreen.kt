package com.example.killteam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.example.killteam.logic.AccessResult

@Composable
fun LockScreen(startReason: AccessResult?, onSubmit: (String) -> AccessResult) {
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf(startReason) }
    val submit = { result = onSubmit(code) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("KILL TEAM", style = MaterialTheme.typography.headlineMedium, color = Palette.Accent)
        Text("Enter your access code to unlock the app.")
        OutlinedTextField(
            code, { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(12); if (result == AccessResult.Wrong) result = null },
            Modifier.widthIn(max = 360.dp).fillMaxWidth(),
            label = { Text("Access code (XXXX-XXXX-XXXX)") }, singleLine = true, isError = result == AccessResult.Wrong,
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
    }
}

/** Shows the typed characters as XXXX-XXXX-XXXX while the text itself stays without dashes, so the cursor does not jump. */
private object CodeTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = text.text.chunked(4).joinToString("-")
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + (offset - 1).coerceAtLeast(0) / 4
            override fun transformedToOriginal(offset: Int) = offset - offset / 5
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}
