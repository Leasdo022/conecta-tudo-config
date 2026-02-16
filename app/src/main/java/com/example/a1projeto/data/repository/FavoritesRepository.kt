package com.example.a1projeto.data.repository


import com.example.a1projeto.data.db.FavoriteEntity
import com.example.a1projeto.data.db.FavoritesDao

data class ChannelUi(val id: String, val name: String, val logo: String?)

class FavoritesRepository(private val dao: FavoritesDao) {
    val favoritesFlow = dao.observeFavorites()

    suspend fun toggle(channel: ChannelUi) {
        if (dao.isFavorite(channel.id)) dao.remove(channel.id)
        else dao.add(FavoriteEntity(channel.id, channel.name, channel.logo))
    }

    suspend fun isFavorite(id: String) = dao.isFavorite(id)
}
