package com.enterprise.meetai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.enterprise.meetai.ui.theme.WaveformActive
import com.enterprise.meetai.ui.theme.WaveformInactive
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    amplitude: Int,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 28
) {
    val normalized = (amplitude.toFloat() / 32767f).coerceIn(0.05f, 1f)

    // Animated scale effect for smooth bouncing bars
    val animScale = remember { Animatable(0.2f) }

    LaunchedEffect(amplitude, isRecording) {
        if (isRecording) {
            animScale.animateTo(
                targetValue = normalized,
                animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing)
            )
        } else {
            animScale.snapTo(0.1f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val width = size.width
        val height = size.height
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(3f)

        for (i in 0 until barCount) {
            val x = i * (barWidth + barSpacing)
            
            // Variasi dinamis agar terlihat seperti gelombang audio alami
            val factor = if (isRecording) {
                val seed = (i * 37) % 100 / 100f
                (0.25f + 0.75f * animScale.value * (0.4f + 0.6f * seed)).coerceIn(0.1f, 1f)
            } else {
                0.12f
            }

            val barHeight = (height * factor).coerceAtLeast(6f)
            val y = (height - barHeight) / 2f

            drawRoundRect(
                color = if (isRecording) WaveformActive else WaveformInactive,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}
