package com.example.a1projeto.ui.screens

import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.example.a1projeto.data.local.AuthStore
import com.example.a1projeto.data.local.AuthState
import com.example.a1projeto.data.local.XtreamApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.a1projeto.data.local.FavoritesStore
import com.example.a1projeto.data.local.FavKind
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.a1projeto.model.Conteudo
import com.example.a1projeto.ui.components.LivePreviewPlayer
import android.util.Base64

fun decodeIfBase64(s: String): String {
    val t = s.trim()
    if (t.isEmpty()) return t

    // Heurística simples: só caracteres válidos e tamanho múltiplo de 4
    val base64Regex = Regex("^[A-Za-z0-9+/=_-]+$") // inclui URL-safe (- _)
    val looksBase64 = t.length >= 8 && t.length % 4 == 0 && base64Regex.matches(t)

    if (!looksBase64) return t

    return try {
        // tenta normal e URL_SAFE
        val bytes = try {
            Base64.decode(t, Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            Base64.decode(t, Base64.URL_SAFE)


        }
        val decoded = String(bytes, Charsets.UTF_8).trim()
        if (decoded.isNotBlank()) decoded else t
    } catch (_: Exception) {
        t

    }
}


@Composable
fun LiveTvScreenTvLike(
    modifier: Modifier = Modifier,
    categories: List<Pair<String, String>>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    channels: List<Conteudo>,
    selectedChannel: Conteudo?,
    onSelectChannel: (Conteudo) -> Unit,
    onPlay: (Conteudo) -> Unit,
    search: String,
    onSearchChange: (String) -> Unit,
    onlyFavs: Boolean,
    onToggleOnlyFavs: () -> Unit,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var previewActive by remember { mutableStateOf(true) }




    val favLive by FavoritesStore
        .favoritesFlow(context, FavKind.LIVE)
        .collectAsState(initial = emptySet())
    val auth by AuthStore.authFlow(context).collectAsState(initial = AuthState())

    var epgArr by remember { mutableStateOf(JSONArray()) }
    var epgLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedChannel?.id, auth.serverUrl, auth.username, auth.password) {
        val ch = selectedChannel ?: return@LaunchedEffect
        if (!auth.isLoggedIn) return@LaunchedEffect

        epgLoading = true
        epgArr = JSONArray()

        try {
            epgArr = withContext(Dispatchers.IO) {
                XtreamApi.getShortEpg(
                    serverUrl = auth.serverUrl,
                    username = auth.username,
                    password = auth.password,
                    streamId = ch.id.toString(),
                    limit = 2
                )
            }
        }


        catch (_: Exception) {
            epgArr = JSONArray()
        } finally {
            epgLoading = false
        }
    }




    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // ===== COLUNA 1: CATEGORIAS =====
        Card(Modifier.weight(0.32f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {




                Spacer(Modifier.height(10.dp))


                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    // ✅ Favoritos fixo (aparece sempre)
                    item {
                        CategoryRow(
                            label = "Favoritos ★",
                            selected = onlyFavs,
                            onClick = {
                                if (!onlyFavs) onSelectCategory(null) // quando liga favoritos, não prende em categoria
                                onToggleOnlyFavs()
                            }
                        )
                    }

                    // ✅ Todos (categorias)
                    item {
                        CategoryRow(
                            label = "Todos",
                            selected = selectedCategoryId == null && !onlyFavs,
                            onClick = {
                                if (onlyFavs) onToggleOnlyFavs()
                                onSelectCategory(null)
                            }
                        )
                    }

                    items(categories, key = { it.first }) { (id, name) ->
                        CategoryRow(
                            label = name,
                            selected = selectedCategoryId == id && !onlyFavs,
                            onClick = {
                                if (onlyFavs) onToggleOnlyFavs()
                                onSelectCategory(id)
                            }
                        )
                    }

                }

            }
        }


        // ===== COLUNA 2: CANAIS =====
        Card(Modifier.weight(0.38f).fillMaxHeight()) {

            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar...") }
                )

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(channels, key = { it.id }) { ch ->
                        val isFavorite = favLive.contains(ch.id)

                        ChannelRow(
                            item = ch,
                            selected = selectedChannel?.id == ch.id,
                            isFavorite = isFavorite,
                            onToggleFavorite = {
                                scope.launch {
                                    FavoritesStore.toggleFavorite(context, FavKind.LIVE, ch.id)
                                }
                            },
                            onClick = {
                                previewActive = true
                                onSelectChannel(ch)
                            },
                            onPlay = {
                                previewActive = false
                                onPlay(ch)
                            }
                        )
                    }
                }
            }
        }


        // ===== COLUNA 3: PREVIEW =====
        Card(Modifier.weight(0.30f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {


                if (selectedChannel != null) {
                    LivePreviewPlayer(
                        url = selectedChannel.url,
                        isPreviewActive = previewActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )
                    Spacer(Modifier.height(10.dp))

                    Text("LIVE TV", style = MaterialTheme.typography.labelMedium)

                    Text(
                        text = selectedChannel.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = { onPlay(selectedChannel) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Assistir")
                    }

                    Spacer(Modifier.height(10.dp))

                    val now = epgArr.optJSONObject(0)
                    val next = epgArr.optJSONObject(1)

                    when {
                        epgLoading -> Text("EPG: carregando...", style = MaterialTheme.typography.bodySmall)
                        now == null && next == null -> Text("EPG indisponível", style = MaterialTheme.typography.bodySmall)
                        else -> {
                            val nowTitle = now?.optString("title").orEmpty()
                            val nextTitle = next?.optString("title").orEmpty()

                            if (nowTitle.isNotBlank()) {
                                Text(
                                    text = "Agora: " + decodeIfBase64(nowTitle),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (nextTitle.isNotBlank()) {
                                Text(
                                    text = "Depois: " + decodeIfBase64(nextTitle),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Dica extra: se mesmo assim não tiver nada pra mostrar
                            if (nowTitle.isBlank() && nextTitle.isBlank()) {
                                Text("Sem informação de EPG", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}



                    @Composable
private fun CategoryRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        tonalElevation = if (selected) 4.dp else 0.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelRow(
    item: Conteudo,
    selected: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(
        tonalElevation = if (selected) 4.dp else 0.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onPlay
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.categoria, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }

            TextButton(onClick = onToggleFavorite) {
                Text(if (isFavorite) "★" else "☆")
            }
        }
    }
}


