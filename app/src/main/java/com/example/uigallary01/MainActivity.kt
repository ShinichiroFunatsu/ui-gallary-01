package com.example.uigallary01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.uigallary01.ui.theme.UiGallary01Theme
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset

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
private fun Modifier.glowingDialogBackground(): Modifier {
    val shape = RoundedCornerShape(28.dp)
    return this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black,
                    Color(0xFF001F8C)
                )
            )
        )
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.35f),
            shape = shape
        )
}
