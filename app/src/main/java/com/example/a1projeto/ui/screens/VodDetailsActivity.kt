package com.example.a1projeto.ui.screens

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.a1projeto.data.local.AuthState
import com.example.a1projeto.data.local.AuthStore
import com.example.a1projeto.data.local.FavKind
import com.example.a1projeto.data.local.FavoritesStore
import com.example.a1projeto.data.local.XtreamApi
import com.example.a1projeto.player.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class VodDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vodId = intent.getStringExtra("vod_id") ?: ""
        val title = intent.getStringExtra("vod_title") ?: "Filme"

        setContent {
            MaterialTheme {
                VodDetailsScreen(vodId = vodId, fallbackTitle = title)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VodDetailsScreen(vodId: String, fallbackTitle: String) {
    val context = LocalContext.current
    val auth by AuthStore.authFlow(context).collectAsState(initial = AuthState())

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<JSONObject?>(null) }

    val vodIdInt = vodId.toIntOrNull()

    val favorites by FavoritesStore
        .favoritesFlow(context, FavKind.VOD)
        .collectAsState(initial = emptySet())

    val isFav = vodIdInt != null && favorites.contains(vodIdInt)


    LaunchedEffect(vodId, auth.serverUrl, auth.username, auth.password) {
        if (!auth.isLoggedIn) { error = "Sem login salvo."; return@LaunchedEffect }
        if (vodId.isBlank()) { error = "Vod ID vazio."; return@LaunchedEffect }

        loading = true
        error = null
        try {
            val obj = withContext(Dispatchers.IO) {
                XtreamApi.getVodInfo(auth.serverUrl, auth.username, auth.password, vodId)
            }
            data = obj
        } catch (e: Exception) {
            error = e.message ?: "Falha ao carregar detalhes"
        } finally {
            loading = false
        }
    }

    val info = data?.optJSONObject("info")
    val movieData = data?.optJSONObject("movie_data")

    val title = info?.optString("name").orEmpty().ifBlank { fallbackTitle }
    val poster = info?.optString("movie_image").orEmpty().ifBlank { info?.optString("cover").orEmpty() }
    val plot = info?.optString("plot").orEmpty()
    val genre = info?.optString("genre").orEmpty()
    val cast = info?.optString("cast").orEmpty()
    val release = info?.optString("releasedate").orEmpty().ifBlank { info?.optString("release_date").orEmpty() }
    val rating = info?.optString("rating").orEmpty()
    val duration = info?.optString("duration").orEmpty()
    val ext = movieData?.optString("container_extension").orEmpty().ifBlank { "mp4" }
    val streamId = movieData?.optString("stream_id").orEmpty().ifBlank { vodId }
    val scope = rememberCoroutineScope()

    fun play() {
        val b = auth.serverUrl.trim().trimEnd('/')
        val url = "$b/movie/${auth.username}/${auth.password}/$streamId.$ext"
        val itn = Intent(context, PlayerActivity::class.java).apply {
            putExtra("url", url)
            putExtra("title", title)
            putExtra("kind", "vod")
            putExtra("fallback_url", poster)
        }
        context.startActivity(itn)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0B163A), Color(0xFF050A30)),
                        radius = 1200f
                    )
                )
        ) {
            when {
                loading -> Text("Carregando...", color = Color.White, modifier = Modifier.padding(16.dp))
                error != null -> Text("Erro:\n${error!!}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Poster (esquerda)
                        Card(modifier = Modifier.width(280.dp)) {
                            AsyncImage(
                                model = poster,
                                contentDescription = title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f / 3f)
                            )
                        }

                        // Infos (direita)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)

                            InfoLine(label = "Gênero", value = genre)
                            InfoLine(label = "Elenco", value = cast)
                            InfoLine(label = "Data", value = release)
                            InfoLine(label = "Duração", value = duration)
                            InfoLine(label = "Nota", value = rating)

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { play() },
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("PLAY")
                                }

                                FilledTonalButton(
                                    onClick = {
                                        vodIdInt?.let { id ->
                                            scope.launch {
                                                FavoritesStore.toggleFavorite(context, FavKind.VOD, id)
                                            }
                                        }
                                    },
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = null
                                    )
                                }

                            }


                            if (plot.isNotBlank()) {
                                Text(plot, color = Color(0xFFEAEAF2))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    if (value.isBlank()) return
    Text(
        text = "• $label: $value",
        color = Color(0xFFEAEAF2),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
