package com.example.a1projeto.data.local

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object LoginValidator {

    private val client = OkHttpClient()

    fun validateXtream(
        baseUrl: String,
        username: String,
        password: String
    ): LoginResult {

        return try {

            // 1️⃣ LOGIN
            val loginUrl =
                "$baseUrl/player_api.php?username=$username&password=$password"

            val loginRequest = Request.Builder()
                .url(loginUrl)
                .get()
                .build()

            val loginResponse = client.newCall(loginRequest).execute()

            if (!loginResponse.isSuccessful) {
                return LoginResult(
                    ok = false,
                    message = "Erro HTTP ${loginResponse.code}"
                )
            }

            val body = loginResponse.body?.string().orEmpty()
            val json = JSONObject(body)
            val userInfo = json.optJSONObject("user_info")

            val auth = userInfo?.optInt("auth", 0) ?: 0
            val status = userInfo?.optString("status", "desconhecido")
            val expDate = userInfo?.optLong("exp_date", 0L) ?: 0L
            val maxConn = userInfo?.optInt("max_connections", 0) ?: 0
            val activeConn = userInfo?.optInt("active_cons", 0) ?: 0

            if (auth != 1) {
                return LoginResult(
                    ok = false,
                    message = "Login inválido (status=$status, auth=$auth)"
                )
            }

            if (!status.equals("Active", true)) {
                return LoginResult(
                    ok = false,
                    message = "Conta não ativa (status=$status)"
                )
            }

            // 2️⃣ TESTE REAL
            val vodUrl =
                "$baseUrl/player_api.php?username=$username&password=$password&action=get_vod_categories"

            val vodRequest = Request.Builder()
                .url(vodUrl)
                .get()
                .build()

            val vodResponse = client.newCall(vodRequest).execute()

            if (!vodResponse.isSuccessful) {
                return LoginResult(
                    ok = false,
                    message = "Login OK, mas sem acesso ao conteúdo"
                )
            }

            // ✅ TUDO OK
            LoginResult(
                ok = true,
                expDateSeconds = expDate,
                maxConnections = maxConn,
                activeConnections = activeConn
            )

        } catch (e: Exception) {
            LoginResult(
                ok = false,
                message = "Erro: ${e.message}"
            )
        }
    }
}
