
package com.example.a1projeto.data.local

data class LoginResult(
    val ok: Boolean,
    val message: String? = null,
    val expDateSeconds: Long = 0L,
    val maxConnections: Int = 0,
    val activeConnections: Int = 0
)
