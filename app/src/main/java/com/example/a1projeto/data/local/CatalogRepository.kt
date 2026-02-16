package com.example.a1projeto.data.local

import android.content.Context
import com.example.a1projeto.model.Catalog
import com.google.gson.Gson

object CatalogRepository {
    fun loadCatalog(context: Context): Catalog {
        val json = context.assets.open("catalog.json")
            .bufferedReader()
            .use { it.readText() }

        return Gson().fromJson(json, Catalog::class.java)
    }
}