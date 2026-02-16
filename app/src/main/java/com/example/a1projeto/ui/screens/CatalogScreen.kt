package com.example.a1projeto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class HomeTab {
    HOME,
    FAVORITES,
    CONFIG
}

@Composable
fun CatalogScreen() {
    var currentTab by remember { mutableStateOf(HomeTab.HOME) }

    Scaffold(
        bottomBar = {
            BottomBarIptv(
                selected = currentTab,
                onSelect = { currentTab = it }
            )
        }
    ) { padding ->
        when (currentTab) {
            HomeTab.HOME -> {
                HomeScreen(padding)
            }

            HomeTab.FAVORITES -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Favoritos (em breve)")
                }
            }

            HomeTab.CONFIG -> {
                ConfigScreen()
            }
        }
    }
}

@Composable
fun BottomBarIptv(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == HomeTab.HOME,
            onClick = { onSelect(HomeTab.HOME) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Início") }
        )
        NavigationBarItem(
            selected = selected == HomeTab.FAVORITES,
            onClick = { onSelect(HomeTab.FAVORITES) },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            label = { Text("Favoritos") }
        )
        NavigationBarItem(
            selected = selected == HomeTab.CONFIG,
            onClick = { onSelect(HomeTab.CONFIG) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Config") }
        )
    }
}
