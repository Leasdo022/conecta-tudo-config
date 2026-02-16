package com.example.a1projeto

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun PosterImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url?.takeIf { it.isNotBlank() })
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        placeholder = painterResource(id = R.drawable.poster_placeholder),
        error = painterResource(id = R.drawable.poster_error),
        fallback = painterResource(id = R.drawable.poster_placeholder),
    )
}
