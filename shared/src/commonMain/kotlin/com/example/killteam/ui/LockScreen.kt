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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
            code, { code = formatCodeInput(it); if (result == AccessResult.Wrong) result = null },
            Modifier.widthIn(max = 360.dp).fillMaxWidth(),
            label = { Text("Access code (XXXX-XXXX-XXXX)") }, singleLine = true, isError = result == AccessResult.Wrong,
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

/** Upper-case, only letters and digits, grouped as XXXX-XXXX-XXXX while typing. */
private fun formatCodeInput(raw: String): String =
    raw.uppercase().filter { it.isLetterOrDigit() }.take(12).chunked(4).joinToString("-")
