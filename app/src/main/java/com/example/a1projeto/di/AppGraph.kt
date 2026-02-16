package com.example.a1projeto.di

import com.example.a1projeto.di.AppGraph
import android.content.Context
import androidx.room.Room
import com.example.a1projeto.data.db.AppDatabase

object AppGraph {

    lateinit var db: AppDatabase
        private set

    fun init(context: Context) {
        db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "iptv2.db"
        ).build()
    }
}
