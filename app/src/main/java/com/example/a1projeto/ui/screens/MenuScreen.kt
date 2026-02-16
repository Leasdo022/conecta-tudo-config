package com.example.a1projeto.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    padding: PaddingValues,
    onGoLive: () -> Unit,
    onGoMovies: () -> Unit,
    onGoSeries: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Conecta Tudo") }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(inner)
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BigTile("LIVE TV", "Canais ao vivo", Modifier.weight(1f), onGoLive)
                BigTile("MOVIES", "Filmes", Modifier.weight(1f), onGoMovies)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BigTile("SERIES", "Séries", Modifier.weight(1f), onGoSeries)
                BigTile("CATCH UP", "Em breve", Modifier.weight(1f)) { }
            }
        }
    }
}

@Composable
private fun BigTile(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .height(170.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Text("Abrir", style = MaterialTheme.typography.labelLarge)
        }
    }
}
