package com.example.a1projeto.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.a1projeto.data.local.AuthState
import com.example.a1projeto.data.local.AuthStore
import com.example.a1projeto.data.local.XtreamApi
import com.example.a1projeto.model.Episode
import com.example.a1projeto.player.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SeriesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val seriesId = intent.getStringExtra("series_id") ?: ""
        val seriesName = intent.getStringExtra("series_name") ?: "Série"

        setContent {
            MaterialTheme {
                SeriesRoute(seriesId = seriesId, seriesName = seriesName, onBack = { finish() })
            }
        }
    }
}

@Composable
private fun SeriesRoute(
    seriesId: String,
    seriesName: String,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth by AuthStore.authFlow(context).collectAsState(initial = AuthState())

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var seasons by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var episodesBySeason by remember { mutableStateOf<Map<Int, List<Episode>>>(emptyMap()) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }

    // (Opcional) banner / poster do seriado
    var posterUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(seriesId, auth.serverUrl, auth.username, auth.password) {
        if (!auth.isLoggedIn) {
            error = "Sem login salvo. Volte e faça login."
            loading = false
            return@LaunchedEffect
        }
        if (seriesId.isBlank()) {
            error = "Series ID vazio."
            loading = false
            return@LaunchedEffect
        }

        loading = true
        error = null

        try {
            val obj: JSONObject = withContext(Dispatchers.IO) {
                XtreamApi.getSeriesInfo(auth.serverUrl, auth.username, auth.password, seriesId)
            }

            // pega um poster (quando existir)
            posterUrl = obj.optJSONObject("info")
                ?.optString("cover", null)
                ?.ifBlank { null }

            val parsed = parseSeriesInfoClean(obj)
            seasons = parsed.first
            episodesBySeason = parsed.second
            selectedSeason = seasons.firstOrNull()?.first
        } catch (e: Exception) {
            error = e.message ?: "Falha ao carregar séries"
        } finally {
            loading = false
        }
    }

    SeriesScreen(
        seriesName = seriesName,
        posterUrl = posterUrl,
        seasons = seasons,
        episodesBySeason = episodesBySeason,
        loading = loading,
        error = error,
        selectedSeason = selectedSeason,
        onSelectSeason = { selectedSeason = it },
        onBack = onBack,
        onPlayEpisode = { ep ->
            val base = auth.serverUrl.trim().trimEnd('/')
            val url = "$base/series/${auth.username}/${auth.password}/${ep.id}.${ep.ext}"

            val itn = Intent(context, PlayerActivity::class.java)
            itn.putExtra("url", url)
            itn.putExtra("fallback_url", "")
            context.startActivity(itn)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeriesScreen(
    seriesName: String,
    posterUrl: String?,
    seasons: List<Pair<Int, String>>,
    episodesBySeason: Map<Int, List<Episode>>,
    loading: Boolean,
    error: String?,
    selectedSeason: Int?,
    onSelectSeason: (Int) -> Unit,
    onBack: () -> Unit,
    onPlayEpisode: (Episode) -> Unit
) {
    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(seriesName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }

                }
            )
        }
    ) { inner ->
        val scroll = rememberScrollState()  // ✅ ADICIONA ISSO

        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                loading -> Text("Carregando...")
                error != null -> Text("Erro:\n$error", color = MaterialTheme.colorScheme.error)
                seasons.isEmpty() -> Text("Sem temporadas/episódios.")
                else -> {
                    // Linha de topo (poster + info básico)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!posterUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = posterUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(140.dp)
                                    .aspectRatio(2f / 3f)
                            )
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = seriesName,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Escolha a temporada abaixo e abra um episódio.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Temporadas (faixa)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(seasons) { (sn, label) ->
                            FilterChip(
                                selected = selectedSeason == sn,
                                onClick = { onSelectSeason(sn) },
                                label = {
                                    Text(
                                        text = label.ifBlank { "SEASON $sn" },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }

                    // Episódios (cards horizontais)
                    val eps = episodesBySeason[selectedSeason] ?: emptyList()
                    if (eps.isEmpty()) {
                        Text("Sem episódios nessa temporada.")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(eps) { ep ->
                                EpisodeCard(ep = ep, onPlay = { onPlayEpisode(ep) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    ep: Episode,
    onPlay: () -> Unit
) {
    Card(
        onClick = onPlay,
        modifier = Modifier.width(220.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            // mini thumb (se tiver)
            if (!ep.thumb.isNullOrBlank()) {
                AsyncImage(
                    model = ep.thumb,
                    contentDescription = ep.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("EP ${ep.episodeNum}")
                }
            }

            Column(Modifier.padding(10.dp)) {
                Text(
                    text = "E${ep.episodeNum} — ${ep.title}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * Lê:
 * seasons: [{season_number,name}, ...]
 * episodes: {"1":[{id,title,episode_num,container_extension,info{movie_image/cover_big}},...], ...}
 */
private fun parseSeriesInfoClean(obj: JSONObject): Pair<List<Pair<Int, String>>, Map<Int, List<Episode>>> {
    val seasonsOut = mutableListOf<Pair<Int, String>>()
    val episodesOut = mutableMapOf<Int, List<Episode>>()

    obj.optJSONArray("seasons")?.let { arr ->
        for (i in 0 until arr.length()) {
            val s = arr.optJSONObject(i) ?: continue
            val sn = s.optInt("season_number", -1)
            val name = s.optString("name", "SEASON $sn")
            if (sn > 0) seasonsOut += sn to name
        }
    }

    obj.optJSONObject("episodes")?.let { epsObj ->
        val keys = epsObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val seasonNum = key.toIntOrNull() ?: continue
            val epsArr = epsObj.optJSONArray(key) ?: continue

            val eps = mutableListOf<Episode>()
            for (i in 0 until epsArr.length()) {
                val e = epsArr.optJSONObject(i) ?: continue

                val id = e.optInt("id", 0)
                if (id == 0) continue

                val title = e.optString("title", "Episódio")
                val epNum = e.optInt("episode_num", i + 1)
                val ext = e.optString("container_extension", "mp4").ifBlank { "mp4" }

                val info = e.optJSONObject("info")
                val thumb = info?.optString("movie_image")
                    ?.ifBlank { null }
                    ?: info?.optString("cover_big")?.ifBlank { null }

                eps += Episode(
                    id = id,
                    title = title,
                    season = seasonNum,
                    episodeNum = epNum,
                    ext = ext,
                    thumb = thumb
                )
            }
            episodesOut[seasonNum] = eps
        }
    }

    if (seasonsOut.isEmpty() && episodesOut.isNotEmpty()) {
        seasonsOut += episodesOut.keys.sorted().map { it to "SEASON $it" }
    }

    return seasonsOut.sortedBy { it.first } to episodesOut
}
