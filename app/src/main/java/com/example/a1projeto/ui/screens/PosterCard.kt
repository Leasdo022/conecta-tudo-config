package com.example.a1projeto.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex


@Composable
fun PosterCard(
    poster: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    // animações suaves (Netflix-like)
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.25f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "scale"
    )

    val shadow by animateDpAsState(
        targetValue = if (focused) 30.dp else 6.dp,
        animationSpec = tween(durationMillis = 140),
        label = "shadow"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (focused) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 140),
        label = "border"
    )

    Card(
        modifier = modifier
            .zIndex(if (focused) 10f else 0f) // ✅ vem pra frente
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(shadow, shape, clip = false)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = shape,
        border = if (focused) BorderStroke(borderWidth, Color.White) else null,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            poster()

            if (focused) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.08f)) // ✅ brilho mais “claro”
                )
            }
        }
    }
}

