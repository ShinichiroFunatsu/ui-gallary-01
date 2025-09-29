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
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawOutline
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.uigallary01.ui.theme.UiGallary01Theme

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
            Box(
                modifier = Modifier
                    .widthIn(min = 280.dp)
                    .glowingDialogBackground()
                    .padding(vertical = 32.dp, horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val glowShadow = Shadow(
                        color = Color.White.copy(alpha = 0.8f),
                        offset = Offset.Zero,
                        blurRadius = 18f
                    )
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

// ダイアログの背景を幻想的なグラデーションにするための拡張関数
@Composable
private fun Modifier.glowingDialogBackground(): Modifier {
    // 波が押し寄せるような動きを表現するアニメーション値を用意
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                // 下から上へ迫り上がる時は素早く、海面に戻るときはゆっくりにする
                durationMillis = 7600
                0f at 0 using LinearOutSlowInEasing
                1f at 5400 using LinearOutSlowInEasing
                0f at durationMillis using FastOutLinearInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "waveShift"
    )

    val shape = RoundedCornerShape(28.dp)
    return this
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawBehind {
                // 波のトップが上下に揺れながら光るイメージの縦グラデーション
                val verticalSpan = size.height * 1.6f
                val startY = (waveShift - 0.5f) * size.height
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
