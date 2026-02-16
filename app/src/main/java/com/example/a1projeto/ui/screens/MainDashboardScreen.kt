package com.example.a1projeto.ui.screens


import com.example.a1projeto.ui.components.SideMenu
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.example.a1projeto.ui.components.MenuEntry


data class DashboardTile(
    val title: String,
    val color: Color,
    val onOpen: () -> Unit
)

@Composable
fun MainDashboardScreen(
    tiles: List<DashboardTile>,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTv =
        ((context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
                Configuration.UI_MODE_TYPE_TELEVISION)

    val outerPadding = if (isTv) 20.dp else 10.dp
    val titleSize = if (isTv) 26.sp else 18.sp
    val gridSpacing = if (isTv) 18.dp else 10.dp
    val menuWidth = if (isTv) 260.dp else 220.dp

    // ✅ menu aqui (dentro do composable)
    val menu = listOf(
        MenuEntry("live", "TV Ao Vivo") { Icon(Icons.Filled.LiveTv, null) },
        MenuEntry("vod", "Filmes") { Icon(Icons.Filled.Movie, null) },
        MenuEntry("series", "Séries") { Icon(Icons.Filled.Tv, null) },
        MenuEntry("settings", "Configurações") { Icon(Icons.Filled.Settings, null) },
    )




    var selected by remember { mutableStateOf("live") }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050A30), Color(0xFF0A1A5E), Color(0xFF050A30))
                )
            )
            .padding(outerPadding)
    ) {
        // ✅ menu lateral animado
        SideMenu(
            items = menu,
            selectedId = selected,
            onSelect = { entry ->
                selected = entry.id
                if (entry.id == "settings") onOpenSettings()
                // se quiser, aqui você navega pra "live/vod/series"
            },
            modifier = Modifier.width(menuWidth)
        )

        Spacer(Modifier.width(if (isTv) 18.dp else 12.dp))

        // ✅ conteúdo da direita (seu layout atual)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Conecta Tudo",
                color = Color.White,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = if (isTv) 18.dp else 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tiles) { tile ->
                    DashboardTileCard(
                        tile = tile,
                        isTv = isTv,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTileCard(
    tile: DashboardTile,
    isTv: Boolean,
    onOpenSettings: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }

    val targetScale = when {
        isTv && focused -> 1.06f
        !isTv && focused -> 1.03f
        else -> 1.00f
    }
    val scale by animateFloatAsState(targetScale, label = "tileScale")

    val corner = if (isTv) 22.dp else 14.dp
    val textSize = if (isTv) 22.sp else 14.sp

    Card(
        modifier = Modifier
            .aspectRatio(if (isTv) 1.2f else 1.05f) // ✅ celular mais compacto
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .scale(scale)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { tile.onOpen() },
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = tile.color),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (focused) (if (isTv) 14.dp else 8.dp) else (if (isTv) 6.dp else 4.dp)
        )
    ) {
        Box(Modifier.fillMaxSize()) {

            // Texto central
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = tile.title,
                    color = Color.White,
                    fontSize = textSize, // ✅ agora respeita TV/celular
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // ⚙️ ícone em cima do SERIES
            if (tile.title == "SERIES") {
                IconButton(
                    onClick = { onOpenSettings?.invoke() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (isTv) 10.dp else 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Configurações",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
