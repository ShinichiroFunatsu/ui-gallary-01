package com.example.uigallary01

import DigitalRainBackground
import GlyphVersion
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    // 開閉アニメーションとグリフバージョンの状態を保持
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var glyphVersion by rememberSaveable { mutableStateOf(GlyphVersion.Classic) }
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) 360.dp else 180.dp,
        label = "rainCardHeight"
    )

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
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .clickable { isExpanded = !isExpanded }
            ) {
                // 雨粒の背景そのものをキャンバスへ描画
                DigitalRainBackground(
                    modifier = Modifier.fillMaxSize(),
                    version = glyphVersion,
                    columnWidthDp = 14.dp,
                    fontSizeSp = 15.sp,
                    seed = 2024L,
                )
            }

            Text(
                text = if (isExpanded) {
                    "緑のグリフが光跡だけで流れるイルミネーション表現です。広げると雰囲気がゆっくり楽しめます。"
                } else {
                    "タップしてデジタルレインの光跡をじっくり眺めてみましょう。"
                },
                style = MaterialTheme.typography.bodyMedium
            )

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
