package com.example.a1projeto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    errorMessage: String?,
    onLogin: (url: String, user: String, pass: String) -> Unit
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Entrar", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))


        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Usuário") },
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(Modifier.height(12.dp))

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(10.dp))
        }

        Button(
            onClick = { onLogin("", user, pass) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }
    }
}
