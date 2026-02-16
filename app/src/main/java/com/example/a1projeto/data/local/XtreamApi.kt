package com.example.a1projeto.data.local

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object XtreamApi {

    private val client = OkHttpClient()

    suspend fun getSeriesInfo(
        baseUrl: String,
        user: String,
        pass: String,
        seriesId: String
    ): JSONObject {
        val b = baseUrl.trim().trimEnd('/')
        val url = "$b/player_api.php?username=$user&password=$pass&action=get_series_info&series_id=$seriesId"
        val txt = httpGet(url)
        return JSONObject(txt)
    }

    suspend fun getVodInfo(
        baseUrl: String,
        user: String,
        pass: String,
        vodId: String
    ): JSONObject {
        val b = baseUrl.trim().trimEnd('/')
        val url = "$b/player_api.php?username=$user&password=$pass&action=get_vod_info&vod_id=$vodId"
        val txt = httpGet(url)
        return JSONObject(txt)
    }

    private fun buildUrl(
        baseUrl: String,
        username: String,
        password: String,
        action: String,
        categoryId: String? = null
    ): String {
        val b = baseUrl.trim().trimEnd('/')
        val extra = if (categoryId.isNullOrBlank()) "" else "&category_id=$categoryId"
        return "$b/player_api.php?username=$username&password=$password&action=$action$extra"
    }

    private fun getArray(url: String): JSONArray {
        val req = Request.Builder().url(url).get().build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
        val body = resp.body?.string().orEmpty()
        return JSONArray(body)
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val code = try { conn.responseCode } catch (_: Exception) { -1 }
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream

            val body = if (stream != null) {
                stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                ""
            }

            if (body.isBlank()) {
                throw Exception("Resposta vazia (HTTP $code) em: $urlStr")
            }

            body
        } finally {
            conn.disconnect()
        }
    }

    // ✅ EPG curto (2 programas por padrão)
    suspend fun getShortEpg(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        limit: Int = 2
    ): JSONArray {
        val base = serverUrl.trim().trimEnd('/')
        val url =
            "$base/player_api.php?username=$username&password=$password&action=get_short_epg&stream_id=$streamId&limit=$limit"

        val txt = httpGet(url)
        val obj = JSONObject(txt)

        // Xtream geralmente volta: { "epg_listings": [ ... ] }
        return if (obj.has("epg_listings")) obj.getJSONArray("epg_listings") else JSONArray()
    }

    fun getLiveCategories(baseUrl: String, user: String, pass: String): JSONArray =
        getArray(buildUrl(baseUrl, user, pass, "get_live_categories"))

    fun getVodCategories(baseUrl: String, user: String, pass: String): JSONArray =
        getArray(buildUrl(baseUrl, user, pass, "get_vod_categories"))

    fun getSeriesCategories(baseUrl: String, user: String, pass: String): JSONArray =
        getArray(buildUrl(baseUrl, user, pass, "get_series_categories"))

    fun getLiveStreams(baseUrl: String, user: String, pass: String, categoryId: String?): JSONArray =
        getArray(buildUrl(baseUrl, user, pass, "get_live_streams", categoryId))

    fun getVodStreams(baseUrl: String, user: String, pass: String, categoryId: String?): JSONArray =
        getArray(buildUrl(baseUrl, user, pass, "get_vod_streams", categoryId))

    fun getSeriesList(baseUrl: String, user: String, pass: String, categoryId: String?): JSONArray =
        getArray(buildUrl(baseUrl, user, pass, "get_series", categoryId))
}
