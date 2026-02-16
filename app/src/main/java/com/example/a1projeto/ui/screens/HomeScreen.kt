package com.example.a1projeto.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import com.example.a1projeto.data.local.FavKind
import com.example.a1projeto.ui.screens.ConfigActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.a1projeto.PosterImage
import com.example.a1projeto.data.local.AuthState
import com.example.a1projeto.data.local.AuthStore
import com.example.a1projeto.data.local.FavoritesStore
import com.example.a1projeto.data.local.LastPlayedStore
import com.example.a1projeto.data.local.XtreamApi
import com.example.a1projeto.model.Conteudo
import com.example.a1projeto.player.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private enum class TabKind(val label: String) {
    LIVE("Canais"),
    VOD("Filmes"),
    SERIES("Séries")
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    padding: PaddingValues,
    initialTab: String = "LIVE",
    onBackToMenu: () -> Unit = {},
    lockedTab: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val config = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current


    val isTv =
        ((context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
                Configuration.UI_MODE_TYPE_TELEVISION) ||
                (config.screenWidthDp >= 720)

    val gridSize = if (isTv) 220.dp else 150.dp

    val store = remember { LastPlayedStore(context) }

    val lastId by store.lastId.collectAsState(initial = null)
    val lastTitle by store.lastTitle.collectAsState(initial = null)
    val lastKind by store.lastKind.collectAsState(initial = null)
    val lastPos by store.lastPosition.collectAsState(initial = null)
    val lastDur by store.lastDuration.collectAsState(initial = null)

    val auth by AuthStore.authFlow(context).collectAsState(initial = AuthState())

    var search by remember { mutableStateOf("") }
    var onlyFavs by remember { mutableStateOf(false) }


    FilterChip(
        selected = onlyFavs,
        onClick = { onlyFavs = !onlyFavs },
        label = { Text("Só favoritos") }
    )




    var selectedTab by remember {
        mutableStateOf(
            when (initialTab) {
                "VOD" -> TabKind.VOD
                "SERIES" -> TabKind.SERIES
                else -> TabKind.LIVE
            }
        )
    }
    val currentKind = when (selectedTab) {
        TabKind.LIVE -> FavKind.LIVE
        TabKind.VOD -> FavKind.VOD
        TabKind.SERIES -> FavKind.SERIES
    }

    val favorites by FavoritesStore
        .favoritesFlow(context, currentKind)
        .collectAsState(initial = emptySet())

    // Botão voltar (controle/celular) volta pro menu
    BackHandler(enabled = true) { onBackToMenu() }

    // trava a aba quando veio do dashboard
    LaunchedEffect(lockedTab) {
        when (lockedTab) {
            "LIVE" -> selectedTab = TabKind.LIVE
            "VOD" -> selectedTab = TabKind.VOD
            "SERIES" -> selectedTab = TabKind.SERIES
        }
    }

    var categories by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    var allItems by remember { mutableStateOf<List<Conteudo>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val dur = lastDur ?: 0L
    val pos = lastPos ?: 0L
    val progress = if (dur > 0L) {
        val p = pos.toFloat() / dur.toFloat()
        kotlin.math.min(1f, kotlin.math.max(0f, p))
    } else 0f

    fun needAuthError(): Boolean = !auth.isLoggedIn

    // carrega categorias quando muda aba
    LaunchedEffect(selectedTab, auth.serverUrl, auth.username, auth.password) {
        if (needAuthError()) {
            error = "Sem login salvo. Volte e faça login."
            return@LaunchedEffect
        }

        loading = true
        error = null
        selectedCategoryId = null
        categories = emptyList()
        allItems = emptyList()

        try {
            val arr = withContext(Dispatchers.IO) {
                when (selectedTab) {
                    TabKind.LIVE -> XtreamApi.getLiveCategories(auth.serverUrl, auth.username, auth.password)
                    TabKind.VOD -> XtreamApi.getVodCategories(auth.serverUrl, auth.username, auth.password)
                    TabKind.SERIES -> XtreamApi.getSeriesCategories(auth.serverUrl, auth.username, auth.password)
                }
            }
            categories = parseCategories(arr)
        } catch (e: Exception) {
            error = e.message ?: "Falha ao carregar categorias"
        } finally {
            loading = false
        }
    }

    // carrega itens quando muda categoria
    LaunchedEffect(selectedCategoryId, selectedTab, auth.serverUrl, auth.username, auth.password) {
        if (needAuthError()) return@LaunchedEffect

        loading = true
        error = null

        try {
            val arr: JSONArray = withContext(Dispatchers.IO) {
                when (selectedTab) {
                    TabKind.LIVE -> XtreamApi.getLiveStreams(auth.serverUrl, auth.username, auth.password, selectedCategoryId)
                    TabKind.VOD -> XtreamApi.getVodStreams(auth.serverUrl, auth.username, auth.password, selectedCategoryId)
                    TabKind.SERIES -> XtreamApi.getSeriesList(auth.serverUrl, auth.username, auth.password, selectedCategoryId)
                }
            }

            allItems = when (selectedTab) {
                TabKind.LIVE -> parseLiveItems(arr, auth.serverUrl, auth.username, auth.password)
                TabKind.VOD -> parseVodItems(arr, auth.serverUrl, auth.username, auth.password)
                TabKind.SERIES -> parseSeriesItems(arr)
            }
        } catch (e: Exception) {
            error = e.message ?: "Falha ao carregar lista"
            allItems = emptyList()
        } finally {
            loading = false
        }
    }

    val filtered = remember(allItems, search) {
        val s = search.trim()
        if (s.isBlank()) allItems else allItems.filter { item ->
            item.titulo.contains(s, ignoreCase = true) ||
                    item.descricao.contains(s, ignoreCase = true) ||
                    item.categoria.contains(s, ignoreCase = true) ||
                    item.tipo.contains(s, ignoreCase = true)
        }
    }

    val filteredFinal = remember(filtered, favorites, onlyFavs) {
        if (!onlyFavs) filtered else filtered.filter { favorites.contains(it.id) }
    }

    // Preview TV: canal selecionado (somente no LIVE)
    var selectedChannel by remember { mutableStateOf<Conteudo?>(null) }

    LaunchedEffect(selectedTab, filtered) {
        if (selectedTab == TabKind.LIVE) {
            val stillExists = selectedChannel?.let { ch -> filtered.any { it.id == ch.id } } == true
            if (!stillExists) selectedChannel = filtered.firstOrNull()
        } else {
            selectedChannel = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conecta Tudo") },
                navigationIcon = {
                    TextButton(onClick = onBackToMenu) { Text("Menu") }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val itn = Intent(context, ConfigActivity::class.java)
                            context.startActivity(itn)
                        },
                        modifier = Modifier.focusable() // pra TV pegar foco
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações"
                        )

                    }
                }
            )
        }





            ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF050A30),
                            Color(0xFF0B163A),
                            Color(0xFF000000),
                        )
                    )
                )
                .padding(padding)
                .padding(innerPadding)
        ) {
            if (selectedTab == TabKind.LIVE) {
                if (isTv) {
                    LiveTvScreenTvLike(
                        modifier = Modifier.fillMaxSize(),
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategory = { selectedCategoryId = it },

                        // pode manter seu filtro assim:
                        channels = if (onlyFavs) filtered.filter { favorites.contains(it.id) } else filtered,

                        selectedChannel = selectedChannel,
                        onSelectChannel = { selectedChannel = it },
                        onPlay = { ch ->
                            val itn = Intent(context, PlayerActivity::class.java).apply {
                                putExtra("url", ch.url)
                                putExtra("fallback_url", ch.thumb ?: "")
                                putExtra("title", ch.titulo)
                                putExtra("kind", "channel")
                            }
                            context.startActivity(itn)
                        },

                        // ✅ ADICIONE ISSO:
                        search = search,
                        onSearchChange = { search = it },
                        onlyFavs = onlyFavs,
                        onToggleOnlyFavs = { onlyFavs = !onlyFavs }

                    )

                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        // ... seu conteúdo live celular
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ESQUERDA: categorias
                    Card(
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 10.dp)
                        ) {
                            item(key = "all") {
                                FilterChip(
                                    selected = selectedCategoryId == null && !onlyFavs,
                                    onClick = {
                                        if (onlyFavs) onlyFavs = false
                                        selectedCategoryId = null
                                    },
                                    label = { Text("Todos") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }


                            items(categories, key = { it.first }) { (id, name) ->
                                FilterChip(
                                    selected = selectedCategoryId == id && !onlyFavs,
                                    onClick = {
                                        if (onlyFavs) onlyFavs = false
                                        selectedCategoryId = id
                                    },
                                    label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (selectedCategoryId == id && !onlyFavs) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF1B2A5B),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }


                        }
                    }

                    // DIREITA: busca + tabs + grid  ✅ AGORA SIM dentro do Row
                    Column(modifier = Modifier.weight(1f)) {

                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Buscar filmes, séries...", color = Color.White.copy(alpha = 0.85f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                cursorColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ⭐ Favoritos SEMPRE visível
                            FilterChip(
                                selected = onlyFavs,
                                onClick = {
                                    if (!onlyFavs) selectedCategoryId = null
                                    onlyFavs = !onlyFavs
                                },
                                label = { Text("Favoritos ★") }
                            )

                            // 📁 Abas só quando não estiver travado
                            if (lockedTab == null) {
                                TabKind.entries
                                    .filter { it != TabKind.LIVE }
                                    .forEach { tab ->
                                        FilterChip(
                                            selected = tab == selectedTab,
                                            onClick = {
                                                selectedTab = tab
                                                search = ""
                                            },
                                            label = { Text(tab.label) }
                                        )
                                    }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {

                            when {
                                error != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text("Erro:\n${error!!}", color = MaterialTheme.colorScheme.error)
                                }
                                loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Carregando...",
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                }

                                filteredFinal.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                                    val msg = when {
                                        onlyFavs -> "Nenhum favorito ainda. Marque ★ em algum título."
                                        search.isNotBlank() -> "Nada encontrado para: \"$search\""
                                        else -> "Aguarde carregar..."
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = msg,
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    items(filteredFinal, key = { it.id }) { item ->
                                        val openItem: () -> Unit = {
                                            if (selectedTab == TabKind.SERIES) {
                                                context.startActivity(
                                                    Intent(context, SeriesActivity::class.java).apply {
                                                        putExtra("series_id", item.id.toString())
                                                        putExtra("series_name", item.titulo)
                                                    }
                                                )
                                            } else {
                                                context.startActivity(
                                                    Intent(context, VodDetailsActivity::class.java).apply {
                                                        putExtra("vod_id", item.id.toString())
                                                        putExtra("title", item.titulo)
                                                    }
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            TvFocusScale(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(2f / 3f)
                                                    .clickable { openItem() }
                                                    .onPreviewKeyEvent { e ->
                                                        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                                        if (e.key == androidx.compose.ui.input.key.Key.DirectionCenter || e.key == androidx.compose.ui.input.key.Key.Enter) {
                                                            openItem(); true
                                                        } else false
                                                    },
                                                focusedScale = 1.06f,
                                                showBorder = true
                                            ) {
                                                PosterImage(url = item.thumb, modifier = Modifier.fillMaxSize())
                                            }

                                            Text(
                                                text = item.titulo,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            TextButton(
                                                onClick = {
                                                    val kind = when (selectedTab) {
                                                        TabKind.LIVE -> FavKind.LIVE
                                                        TabKind.VOD -> FavKind.VOD
                                                        TabKind.SERIES -> FavKind.SERIES
                                                    }
                                                    scope.launch {
                                                        FavoritesStore.toggleFavorite(context, kind, item.id)
                                                    }
                                                }
                                            ) {
                                                Text(if (favorites.contains(item.id)) "★" else "☆", color = Color.White)
                                            }




                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}













@Composable
fun TvFocusScale(
    modifier: Modifier = Modifier,
    focusedScale: Float = 1.06f,
    corner: Int = 18,
    showBorder: Boolean = true,
    content: @Composable () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(corner.dp)

    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = tween(150),
        label = "scale"
    )

    val borderColor = if (focused) Color.White else Color.White.copy(alpha = 0.75f)
    val borderWidth = if (focused) 3.dp else 0.dp
    val glow = if (focused) 18.dp else 0.dp

    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .shadow(glow, shape, clip = false)      // glow
            .scale(scale)
            .clip(shape)
            .border(borderWidth, borderColor, shape) // borda forte
            .focusable()
    ) {
        content()
    }
}














@Composable
private fun VideoCard(
    item: Conteudo,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (!item.thumb.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumb,
                    contentDescription = item.titulo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onToggleFavorite) {
                    Text(if (isFavorite) "★" else "☆")
                }
            }

            Text(
                text = item.descricao,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Categoria: ${item.categoria}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** ---------- PARSERS ---------- **/

private fun parseCategories(arr: JSONArray): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val id = o.optString("category_id", o.optString("id", ""))
        val name = o.optString("category_name", o.optString("name", ""))
        if (id.isNotBlank() && name.isNotBlank()) out += id to name
    }
    return out
}

private fun parseLiveItems(arr: JSONArray, baseUrl: String, user: String, pass: String): List<Conteudo> {
    val out = mutableListOf<Conteudo>()
    val b = baseUrl.trim().trimEnd('/')

    for (i in 0 until arr.length()) {
        val o: JSONObject = arr.optJSONObject(i) ?: continue
        val streamId = o.optString("stream_id", "")
        val name = o.optString("name", "")
        val cat = o.optString("category_id", "")
        val icon = o.optString("stream_icon", o.optString("cover", ""))

        if (streamId.isBlank() || name.isBlank()) continue

        val urlM3u8 = "$b/live/$user/$pass/$streamId.m3u8"
        val idInt = streamId.toIntOrNull() ?: (1000000 + i)

        out += Conteudo(
            id = idInt,
            titulo = name,
            descricao = "Ao vivo",
            url = urlM3u8,
            categoria = cat.ifBlank { "Canais" },
            tipo = "LIVE",
            thumb = icon.ifBlank { null }
        )
    }
    return out
}

private fun parseVodItems(arr: JSONArray, baseUrl: String, user: String, pass: String): List<Conteudo> {
    val out = mutableListOf<Conteudo>()
    val b = baseUrl.trim().trimEnd('/')

    for (i in 0 until arr.length()) {
        val o: JSONObject = arr.optJSONObject(i) ?: continue
        val streamId = o.optString("stream_id", "")
        val name = o.optString("name", "")
        val cat = o.optString("category_id", "")
        val ext = o.optString("container_extension", "mp4").ifBlank { "mp4" }
        val icon = o.optString("stream_icon", o.optString("cover", ""))

        if (streamId.isBlank() || name.isBlank()) continue

        val idInt = streamId.toIntOrNull() ?: (2000000 + i)
        val url = "$b/movie/$user/$pass/$streamId.$ext"

        out += Conteudo(
            id = idInt,
            titulo = name,
            descricao = "Filme",
            url = url,
            categoria = cat.ifBlank { "Filmes" },
            tipo = "VOD",
            thumb = icon.ifBlank { null }
        )
    }
    return out
}

private fun parseSeriesItems(arr: JSONArray): List<Conteudo> {
    val out = mutableListOf<Conteudo>()

    for (i in 0 until arr.length()) {
        val o: JSONObject = arr.optJSONObject(i) ?: continue
        val seriesId = o.optString("series_id", "")
        val name = o.optString("name", "")
        val cat = o.optString("category_id", "")
        val cover = o.optString("cover", o.optString("cover_big", ""))

        if (seriesId.isBlank() || name.isBlank()) continue

        val idInt = seriesId.toIntOrNull() ?: (3000000 + i)

        out += Conteudo(
            id = idInt,
            titulo = name,
            descricao = "Série (episódios no próximo passo)",
            url = seriesId,
            categoria = cat.ifBlank { "Séries" },
            tipo = "SERIES",
            thumb = cover.ifBlank { null }
        )
    }
    return out
}
