package com.example.a1projeto.data.repository


import com.example.a1projeto.data.db.WatchProgressDao
import com.example.a1projeto.data.db.WatchProgressEntity

class WatchProgressRepository(private val dao: WatchProgressDao) {
    val continueFlow = dao.observeContinueWatching()

    suspend fun save(contentId: String, title: String, poster: String?, pos: Long, dur: Long) {
        dao.upsert(
            WatchProgressEntity(
                contentId = contentId,
                title = title,
                poster = poster,
                lastPositionMs = pos,
                durationMs = dur
            )
        )
    }
}
