package com.example.uigallary01

import DigitalRainBackground
import GlyphVersion
import RainMode
import WrapMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// DigitalRainBackground をギャラリーカードとして体験できるように構成
@Composable
fun DigitalRainBackgroundItem(modifier: Modifier = Modifier) {
    // 表示モードとグリフバージョンを切り替えられるように保持
    var mode by rememberSaveable { mutableStateOf(RainMode.Scroll) }
    var glyphVersion by rememberSaveable { mutableStateOf(GlyphVersion.Classic) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
            ) {
                // 雨粒の背景そのものをキャンバスへ描画
                DigitalRainBackground(
                    modifier = Modifier.fillMaxSize(),
                    version = glyphVersion,
                    mode = mode,
                    wrapMode = WrapMode.Circular,
                    columnWidthDp = 14.dp,
                    fontSizeSp = 15.sp,
                    seed = 2024L,
                )
            }

            Text(
                text = "緑のデジタルレインを Box 背景として活用する例です。",
                style = MaterialTheme.typography.bodyMedium
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 雨粒の動き方を切り替え
                    DigitalRainToggleButton(
                        label = "Scroll", 
                        selected = mode == RainMode.Scroll
                    ) { mode = RainMode.Scroll }
                    DigitalRainToggleButton(
                        label = "Illumination",
                        selected = mode == RainMode.Illumination
                    ) { mode = RainMode.Illumination }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // グリフ集合のバリエーションを切り替え
                    DigitalRainToggleButton(
                        label = "Classic",
                        selected = glyphVersion == GlyphVersion.Classic
                    ) { glyphVersion = GlyphVersion.Classic }
                    DigitalRainToggleButton(
                        label = "Resurrections",
                        selected = glyphVersion == GlyphVersion.Resurrections
                    ) { glyphVersion = GlyphVersion.Resurrections }
                }
            }
        }
    }
}

// 選択状態に応じてボタンスタイルを使い分ける拡張
@Composable
private fun RowScope.DigitalRainToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val modifier = Modifier.weight(1f)
    if (selected) {
        FilledTonalButton(onClick = onClick, modifier = modifier) {
            Text(text = label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(text = label)
        }
    }
}
