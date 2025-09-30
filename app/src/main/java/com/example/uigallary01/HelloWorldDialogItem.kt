package com.example.uigallary01

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun HelloWorldDialogItem() {
    // 初期状態ではダイアログを非表示にする
    var isDialogVisible by rememberSaveable { mutableStateOf(false) }

    if (isDialogVisible) {
        Dialog(onDismissRequest = { isDialogVisible = false }) {
            // 幻想的な雰囲気を演出するためのグラデーション背景付きボックス
            val waveMotion = rememberWaveMotion()

            Box(
                modifier = Modifier
                    .widthIn(min = 280.dp)
                    .glowingDialogBackground(waveMotion)
                    .padding(vertical = 32.dp, horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val glowShadow = waveMotion.glowShadow()
                    // 波間に浮かぶ光を表現するテキスト装飾
                    Text(
                        text = "Hello World",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            shadow = glowShadow
                        )
                    )
                    Text(
                        text = "闇夜の波間に光るプランクトンのような幻想をお楽しみください。",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            shadow = glowShadow
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { isDialogVisible = false }) {
                        Text(
                            text = "閉じる",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                shadow = glowShadow
                            )
                        )
                    }
                }
            }
        }
    }

    // ダイアログを再度開けるようにボタンを配置
    Button(onClick = { isDialogVisible = true }) {
        Text(text = "ダイアログを表示")
    }
}

// 波の動きと光彩の大きさを同期させるための状態ホルダー
private data class WaveMotion(
    val shift: Float,
    val glowRadius: Float,
    val glowIntensity: Float,
)

// 光彩強度をシャドウとして取り出すための拡張関数
private fun WaveMotion.glowShadow(): Shadow {
    return Shadow(
        color = Color.White.copy(alpha = glowIntensity),
        offset = Offset.Zero,
        blurRadius = glowRadius
    )
}

@Composable
private fun rememberWaveMotion(): WaveMotion {
    // 波が押し寄せるような動きと光の広がりを同じタイムラインで制御する
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                // 波が押し寄せて上昇する区間は短く速く、引き波で下降する区間は長くゆったりにする
                durationMillis = 7600
                0f at 0 using FastOutLinearInEasing
                1f at 2800 using FastOutLinearInEasing
                0f at durationMillis using LinearOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "waveShift",
    )
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 28f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                // 競り上がる瞬間に光が大きく広がり、引き波ではゆったりと収束する
                durationMillis = 7600
                28f at 0 using FastOutLinearInEasing
                64f at 2200 using FastOutLinearInEasing
                28f at durationMillis using LinearOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "glowRadius",
    )
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                // 押し寄せる瞬間は眩しく、引き波で落ち着く光の強弱を付ける
                durationMillis = 7600
                0.9f at 0 using FastOutLinearInEasing
                1f at 2200 using FastOutLinearInEasing
                0.82f at durationMillis using LinearOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "glowIntensity",
    )
    return WaveMotion(
        shift = waveShift,
        glowRadius = glowRadius,
        glowIntensity = glowIntensity,
    )
}

// ダイアログの背景を幻想的なグラデーションにするための拡張関数
private fun Modifier.glowingDialogBackground(waveMotion: WaveMotion): Modifier {
    val shape = RoundedCornerShape(28.dp)
    return this
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawBehind {
                // 波のトップが上下に揺れながら光るイメージの縦グラデーション
                val verticalSpan = size.height * 1.6f
                val startY = (0.5f - waveMotion.shift) * size.height
                val brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF021032),
                        Color(0xFF1B63FF),
                        Color(0xFF001F8C),
                    ),
                    startY = startY,
                    endY = startY + verticalSpan,
                )
                drawOutline(outline = outline, brush = brush)
            }
        }
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.35f),
            shape = shape,
        )
}
