package com.example.a1projeto.ui.screens

import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.a1projeto.data.local.AuthState
import com.example.a1projeto.data.local.AuthStore
import com.example.a1projeto.data.local.LoginValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL

private val SERVER_CANDIDATES: List<String>
    get() = listOf(
        "http://equipedl.pro",
        "http://67amnb.vip",
    
        "http://ipsmart.icu",
        "http://onetvg.icu",
        "http://stvip.life",
        "http://esma26.top",
    )


private fun normalizeBaseUrl(raw: String): String {
    var s = raw.trim()
    if (s.isBlank()) return ""

    // se colou como lista/JSON: "http://x:80",
    s = s.trim('"', '\'', ',', ' ')

    // garante esquema
    if (!s.startsWith("http://") && !s.startsWith("https://")) {
        s = "http://$s"
    }

    // remove / no final
    s = s.trimEnd('/')

    // remove :80 no final (http padrão)
    if (s.endsWith(":80")) {
        s = s.removeSuffix(":80")
    }

    return s
}

private const val DNS_CONFIG_URL =
    "https://raw.githubusercontent.com/Leasdo022/conecta-tudo-config/main/servers.json"

private val remoteClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .callTimeout(5, TimeUnit.SECONDS)
    .build()

private suspend fun fetchRemoteServers(timeoutMs: Long = 5000L): List<String> {
    return withTimeoutOrNull(timeoutMs) {
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(DNS_CONFIG_URL)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "application/json")
                    .build()

                remoteClient.newCall(req).execute().use { resp ->
                    val code = resp.code
                    val body = resp.body?.string().orEmpty()

                    println("REMOTE HTTP = $code")
                    println("REMOTE BODY (first 120) = ${body.take(120)}")

                    if (!resp.isSuccessful) return@withContext emptyList()
                    if (body.isBlank()) return@withContext emptyList()

                    val arr = JSONArray(body)
                    buildList {
                        for (i in 0 until arr.length()) {
                            val s = normalizeBaseUrl(arr.optString(i))
                            if (s.isNotBlank()) add(s)
                        }
                    }
                }
            } catch (e: Exception) {
                println("REMOTE ERROR = ${e.javaClass.simpleName}: ${e.message}")
                emptyList()
            }
        }
    } ?: emptyList()
}

private suspend fun pickWorkingServer(
    candidates: List<String>,
    username: String,
    password: String,
    timeoutMs: Long = 2500L
): String? {
    val u = username.trim()
    val p = password.trim()
    if (u.isBlank() || p.isBlank()) return null

    suspend fun testEndpoint(base: String, action: String): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.IO) {
                try {
                    val url = "$base/player_api.php?username=$u&password=$p&action=$action"
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = timeoutMs.toInt()
                        readTimeout = timeoutMs.toInt()
                        requestMethod = "GET"
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                        setRequestProperty("Accept", "application/json")
                    }
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    conn.disconnect()

                    code in 200..299 && body.isNotBlank() && body.trim().startsWith("[")
                } catch (_: Exception) {
                    false
                }
            }
        } == true
    }

    for (c in candidates) {
        val base = normalizeBaseUrl(c)
        if (base.isBlank()) continue

        val okLive   = testEndpoint(base, "get_live_categories")
        val okVod    = testEndpoint(base, "get_vod_categories")
        val okSeries = testEndpoint(base, "get_series_categories")

        if (okLive && okVod && okSeries) return base
    }
    return null
}


@Composable
fun AppRoot(paddingValues: PaddingValues = PaddingValues()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val auth by AuthStore.authFlow(context).collectAsState(initial = AuthState())
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    // ✅ comece na tela certa
    var screen by remember(auth.isLoggedIn) {
        mutableStateOf(if (auth.isLoggedIn) "dashboard" else "login")
    }

    var initialTab by remember { mutableStateOf("LIVE") } // LIVE | VOD | SERIES

    when (screen) {
        "login" -> {
            LoginScreen(
                errorMessage = if (loading) "Validando..." else error,
                onLogin = { _urlIgnorada, user, pass ->

                    if (user.isBlank() || pass.isBlank()) {
                        error = "Preencha usuário e senha."
                        return@LoginScreen
                    }

                    loading = true
                    error = null

                    scope.launch {

                        val remote = fetchRemoteServers()
                        println("REMOTE SERVERS = $remote")
                        println("USANDO = ${if (remote.isNotEmpty()) "GITHUB" else "LOCAL"}")

                        val candidates = if (remote.isNotEmpty()) remote else SERVER_CANDIDATES

                        val server = pickWorkingServer(
                            candidates = candidates,
                            username = user,
                            password = pass,
                            timeoutMs = 2500L
                        )


                        if (server == null) {
                            loading = false
                            error = "Nenhum servidor respondeu. Tente novamente."
                            return@launch
                        }

                        val result = withContext(Dispatchers.IO) {
                            LoginValidator.validateXtream(
                                baseUrl = server,
                                username = user,
                                password = pass
                            )
                        }

                        loading = false

                        if (!result.ok) {
                            error = result.message ?: "Login inválido."
                            return@launch
                        }

                        // ✅ SALVA O LOGIN CORRETO (não vazio)
                        AuthStore.save(
                            context = context,
                            serverUrl = server,
                            username = user,
                            password = pass,
                            expDateSeconds = result.expDateSeconds ?: 0L,
                            maxConnections = result.maxConnections ?: 0,
                            activeConnections = result.activeConnections ?: 0
                        )

                        screen = "dashboard"
                    }
                }
            )
        }

        "dashboard" -> {
            MainDashboardScreen(
                tiles = listOf(
                    DashboardTile("LIVE TV", Color(0xFFE53935)) {
                        initialTab = "LIVE"
                        screen = "home"
                    },
                    DashboardTile("MOVIES", Color(0xFFFFB300)) {
                        initialTab = "VOD"
                        screen = "home"
                    },
                    DashboardTile("SERIES", Color(0xFFD81B60)) {
                        initialTab = "SERIES"
                        screen = "home"
                    }
                ),
                onOpenSettings = { screen = "settings" }
            )
        }



        "settings" -> {
            SettingsScreen(
                onBack = { screen = "dashboard" },
                onLogout = {
                    scope.launch {
                        AuthStore.save(
                            context = context,
                            serverUrl = "",
                            username = "",
                            password = "",
                            expDateSeconds = 0L,
                            maxConnections = 0,
                            activeConnections = 0
                        )
                        screen = "login"
                    }
                }
            )
        }

        "home" -> {
            HomeScreen(
                padding = paddingValues,
                initialTab = initialTab,
                lockedTab = initialTab,
                onBackToMenu = { screen = "dashboard" }
            )
        }
    }
}
