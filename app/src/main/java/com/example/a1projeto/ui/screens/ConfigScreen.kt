package com.example.a1projeto.ui.screens

import androidx.compose.foundation.text.KeyboardOptions

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.a1projeto.data.local.AuthStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text("Configurações", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Servidor (URL)") },
            placeholder = { Text("http://seuservidor:porta") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            )
        )

        Button(
            onClick = {
                val fixed = url.trim()
                if (fixed.isBlank()) {
                    msg = "Digite uma URL válida."
                    return@Button
                }

                scope.launch {
                    AuthStore.updateServerUrl(context, fixed)
                    msg = "URL salva ✅"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvar URL")
        }


        Button(
            onClick = {
                scope.launch {
                    AuthStore.logout(context)
                    msg = "Saiu da conta ✅"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sair da conta")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }

        msg?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}
