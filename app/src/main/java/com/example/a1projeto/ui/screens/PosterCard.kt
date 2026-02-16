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
import androidx.compose.foundation.border



@Composable
fun PosterCard(
    poster: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    val focusColor = Color(0xFF00E5FF)

    val scale by animateFloatAsState(if (focused) 1.18f else 1f, label = "scale")
    val borderW by animateDpAsState(if (focused) 6.dp else 0.dp, label = "border")

    Box(
        modifier = modifier
            .zIndex(if (focused) 10f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            // ✅ BORDA FORA (mais visível que Card(border=...))
            .border(borderW, focusColor, shape)
            // ✅ brilho forte por trás
            .shadow(
                elevation = if (focused) 30.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = focusColor,
                spotColor = focusColor
            )
            .clip(shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        poster()

        if (focused) {
            // ✅ overlay mais forte
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
        }
    }
}
