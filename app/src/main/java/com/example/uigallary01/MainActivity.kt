package com.example.uigallary01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import com.example.uigallary01.ui.theme.UiGallary01Theme
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UiGallary01Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GalleryScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun GalleryScreen(modifier: Modifier = Modifier) {
    // ギャラリーに表示する要素を定義
    val galleryItems = listOf(
        GalleryItem(
            title = "Hello World Dialog",
            content = { HelloWorldDialogItem() }
        ),
        GalleryItem(
            title = "Moody Snow Background",
            content = { MoodySnowBackgroundItem() }
        )
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(galleryItems) { item ->
            item.ListItem()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GalleryScreenPreview() {
    UiGallary01Theme {
        GalleryScreen()
    }
}

// ギャラリーに表示する要素の定義
private data class GalleryItem(
    val title: String,
    val content: @Composable () -> Unit
)

// データクラス自身が描画手段を提供できるように拡張関数化
@Composable
private fun GalleryItem.ListItem(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ギャラリー要素のタイトルを表示
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun HelloWorldDialogItem() {
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

@Composable
private fun MoodySnowBackgroundItem() {
    // 雪が舞う背景の表現に必要な状態を初期化
    val snowParticles = rememberSnowField(count = 160)
    val snowProgress by rememberInfiniteTransition(label = "snowfall").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowProgress"
    )
    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF050713),
                Color(0xFF101E33),
                Color(0xFF253752)
            )
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
                progress = snowProgress
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
                        blurRadius = 12f
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
    val twinkleOffset: Float
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
                twinkleOffset = random.nextFloat()
            )
        }
    }
}

// 雪粒の描画ロジックを DrawScope に切り出して再利用性を高める
private fun DrawScope.drawSnowField(
    particles: List<SnowParticle>,
    progress: Float
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
            center = Offset(x, y)
        )
    }
}

private const val TwoPi: Float = (PI * 2.0).toFloat()

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
                        Color(0xFF001F8C)
                    ),
                    startY = startY,
                    endY = startY + verticalSpan
                )
                drawOutline(outline = outline, brush = brush)
            }
        }
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.35f),
            shape = shape
        )
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
            repeatMode = RepeatMode.Restart
        ),
        label = "waveShift"
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
            repeatMode = RepeatMode.Restart
        ),
        label = "glowRadius"
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
            repeatMode = RepeatMode.Restart
        ),
        label = "glowIntensity"
    )
    return WaveMotion(
        shift = waveShift,
        glowRadius = glowRadius,
        glowIntensity = glowIntensity
    )
}
