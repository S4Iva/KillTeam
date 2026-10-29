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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(onSubmit: (String) -> Boolean) {
    var password by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val submit = { wrong = !onSubmit(password) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("KILL TEAM", style = MaterialTheme.typography.headlineMedium, color = Waha.Orange)
        Text("Enter the password to unlock the app. This is only asked once.")
        OutlinedTextField(
            password, { password = it; wrong = false },
            Modifier.widthIn(max = 360.dp).fillMaxWidth(),
            label = { Text("Password") }, singleLine = true, isError = wrong,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton({ visible = !visible }) { Text(if (visible) "Hide" else "Show") } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        if (wrong) Text("Wrong password.", color = MaterialTheme.colorScheme.error)
        Button(submit, enabled = password.isNotEmpty()) { Text("Unlock") }
    }
}
