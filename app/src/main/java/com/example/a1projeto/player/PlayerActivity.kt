package com.example.a1projeto.player

import androidx.media3.ui.AspectRatioFrameLayout
import android.graphics.Color
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.a1projeto.data.local.LastPlayedStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerActivity : ComponentActivity() {

    private fun enterFullscreen() {
        // deixa o conteúdo ocupar a tela toda
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun exitFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    private var player: ExoPlayer? = null
    private lateinit var lastPlayedStore: LastPlayedStore

    private var primaryUrl: String = ""
    private var fallbackUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        primaryUrl = intent.getStringExtra("url") ?: return
        fallbackUrl = intent.getStringExtra("fallback_url") ?: ""

        val title = intent.getStringExtra("title") ?: "Conteúdo"
        val kind = intent.getStringExtra("kind") ?: "channel"

        lastPlayedStore = LastPlayedStore(this)

        val playerView = PlayerView(this)
        setContentView(playerView)

        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        playerView.setBackgroundColor(Color.BLACK)
        playerView.setShutterBackgroundColor(Color.BLACK)

        player = ExoPlayer.Builder(this).build().also {
            playerView.player = it
        }
        playerView.keepScreenOn = true
        enterFullscreen()

        // 🔁 voltar do ponto salvo
        CoroutineScope(Dispatchers.IO).launch {
            val savedId = lastPlayedStore.lastId.first()
            val savedPos = lastPlayedStore.lastPosition.first() ?: 0L

            runOnUiThread {
                if (savedId == primaryUrl && savedPos > 0) {
                    player?.seekTo(savedPos)
                }
            }
        }

        player?.setMediaItem(MediaItem.fromUri(Uri.parse(primaryUrl)))
        player?.prepare()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()

        val pos = player?.currentPosition ?: 0L
        val dur = player?.duration ?: 0L
        val safePos = if (dur > 0 && pos >= dur - 3000L) 0L else pos
        val safeDur = if (dur < 0) 0L else dur

        CoroutineScope(Dispatchers.IO).launch {
            lastPlayedStore.save(
                id = primaryUrl,
                title = intent.getStringExtra("title") ?: "Conteúdo",
                kind = intent.getStringExtra("kind") ?: "channel",
                position = safePos,
                duration = safeDur
            )

        }

        player?.release()
        player = null
    }
}
