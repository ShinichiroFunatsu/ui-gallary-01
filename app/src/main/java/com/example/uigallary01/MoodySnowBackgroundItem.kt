package com.example.uigallary01

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MoodySnowBackgroundItem() {
    // 雪が舞う背景の表現に必要な状態を初期化
    val snowParticles = rememberSnowField(count = 160)
    val snowProgress by rememberInfiniteTransition(label = "snowfall").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "snowProgress",
    )
    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF050713),
                Color(0xFF101E33),
                Color(0xFF253752),
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // 夜空のグラデーションを先に描画
            drawRect(brush = backgroundBrush)
            // しんしんと降る雪の層を重ねる
            drawSnowField(
                particles = snowParticles,
                progress = snowProgress,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Moody Snowfall",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color(0xFFEBF7FF),
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        blurRadius = 12f,
                    )
                )
            )
            Text(
                text = "静かに舞い落ちる雪に包まれた夜の空気を想像してください。",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFD9E4FF)
                )
            )
        }
    }
}

// 静かな雪の粒を表現するための情報をまとめたデータクラス
private data class SnowParticle(
    val horizontalPosition: Float,
    val verticalOrigin: Float,
    val speedMultiplier: Float,
    val driftFactor: Float,
    val radiusDp: Float,
    val twinkleOffset: Float,
)

// 雪粒のリストを生成して再利用するための状態ホルダー
@Composable
private fun rememberSnowField(count: Int): List<SnowParticle> {
    return remember(count) {
        val random = Random(2024)
        List(count) {
            SnowParticle(
                horizontalPosition = random.nextFloat(),
                verticalOrigin = random.nextFloat(),
                speedMultiplier = lerp(0.65f, 1.25f, random.nextFloat()),
                driftFactor = lerp(0.02f, 0.08f, random.nextFloat()),
                radiusDp = lerp(1.6f, 3.8f, random.nextFloat()),
                twinkleOffset = random.nextFloat(),
            )
        }
    }
}

// 雪粒の描画ロジックを DrawScope に切り出して再利用性を高める
private fun DrawScope.drawSnowField(
    particles: List<SnowParticle>,
    progress: Float,
) {
    particles.forEach { particle ->
        val travel = (progress * particle.speedMultiplier + particle.verticalOrigin) % 1f
        val y = travel * size.height
        val drift = sin((travel + particle.twinkleOffset) * TwoPi) * particle.driftFactor * size.width
        val x = particle.horizontalPosition * size.width + drift
        val shimmer = (sin((travel * 1.3f + particle.twinkleOffset) * TwoPi) + 1f) * 0.5f
        val alpha = 0.25f + 0.75f * shimmer
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = particle.radiusDp.dp.toPx(),
            center = Offset(x, y),
        )
    }
}

private const val TwoPi: Float = (PI * 2.0).toFloat()
