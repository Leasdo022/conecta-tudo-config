package com.example.a1projeto.data.db


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val contentId: String, // canalId ou vodId
    val title: String,
    val poster: String?,
    val lastPositionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
