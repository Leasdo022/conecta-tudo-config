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

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(audioAttributes, true)

            if (url.isNotBlank()) {
                setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                prepare()
            }
        }
    }

    // ✅ controla som/pausa do preview
    LaunchedEffect(isPreviewActive) {
        exoPlayer.volume = if (isPreviewActive) 1f else 0f
        exoPlayer.playWhenReady = isPreviewActive
        if (!isPreviewActive) exoPlayer.pause() else exoPlayer.play()
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
        },
        update = { view ->
            view.player = exoPlayer
        }
    )
}
