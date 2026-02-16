package com.example.a1projeto.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {

    @Query("SELECT * FROM watch_progress ORDER BY updatedAt DESC LIMIT :limit")
    fun observeContinueWatching(limit: Int = 20): Flow<List<WatchProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE contentId = :id")
    suspend fun delete(id: String)
}
