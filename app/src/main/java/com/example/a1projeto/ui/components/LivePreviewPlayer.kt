package com.example.a1projeto.ui.components

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun LivePreviewPlayer(
    url: String,
    isPreviewActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current


    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            if (url.isNotBlank()) {
                setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                prepare()
            }
        }
    }

    // controla tocar/parar + volume
    LaunchedEffect(isPreviewActive) {
        exoPlayer.playWhenReady = isPreviewActive
        exoPlayer.volume = if (isPreviewActive) 1f else 0f
        if (!isPreviewActive) exoPlayer.pause()
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                player = exoPlayer
            }
        }
    )
}

