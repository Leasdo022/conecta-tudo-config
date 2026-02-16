package com.example.a1projeto.model


data class Episode(
    val id: Int,
    val title: String,
    val season: Int,
    val episodeNum: Int,
    val ext: String = "mp4",
    val thumb: String? = null
)
