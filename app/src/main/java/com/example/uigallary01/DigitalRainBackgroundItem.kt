package com.example.uigallary01

import DigitalRainBackground
import GlyphVersion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uigallary01.ui.theme.UiGallary01Theme

@Composable
fun DigitalRainBackgroundItem(
    modifier: Modifier = Modifier,
) {
    // タップで高さを切り替えるアニメーションを保持
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) 360.dp else 200.dp,
        label = "digitalRainHeight"
    )

    Column(
        modifier = modifier,
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
            DigitalRainBackground(
                modifier = Modifier.fillMaxSize(),
                version = if (isExpanded) GlyphVersion.Resurrections else GlyphVersion.Classic,
                backgroundColor = Color.Black,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedVisibility(visible = isExpanded) {
                    Text(
                        text = "輝度波だけで構成したイルミネーションモードです。",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCCFFCC))
                    )
                }
                Text(
                    text = "Digital Rain Background",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                )
            }
        }
        Text(
            text = if (isExpanded) "タップでコンパクト表示に戻ります" else "タップで詳細を表示します",
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Preview
@Composable
private fun DigitalRainBackgroundItemPreview() {
    UiGallary01Theme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DigitalRainBackgroundItem(
                modifier = Modifier
                    .padding(16.dp)
            )
        }
    }
}
