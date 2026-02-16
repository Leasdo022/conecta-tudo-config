package com.example.a1projeto.model

data class Catalog(val conteudos: List<Conteudo>)

data class Conteudo(
    val id: Int,
    val titulo: String,
    val descricao: String,
    val url: String,
    val categoria: String,
    val tipo: String,
    val thumb: String? = null
)
