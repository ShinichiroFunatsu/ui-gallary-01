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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
    // 表示モードの切り替え状態を保持
    var mode by rememberSaveable { mutableStateOf(RainMode.Scroll) }
    val glyphVersion = if (mode == RainMode.Scroll) GlyphVersion.Classic else GlyphVersion.Resurrections

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
        ) {
            DigitalRainBackground(
                modifier = Modifier.fillMaxSize(),
                version = glyphVersion,
                mode = mode,
                wrapMode = WrapMode.Circular,
                backgroundColor = Color.Black,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Matrix風のレインを背景にした演出サンプル",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                )
                Text(
                    text = "mode: ${mode.name}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB0FFB0))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeAssistChip(
                text = "Scroll",
                selected = mode == RainMode.Scroll,
                onClick = { mode = RainMode.Scroll }
            )
            ModeAssistChip(
                text = "Illumination",
                selected = mode == RainMode.Illumination,
                onClick = { mode = RainMode.Illumination }
            )
        }
    }
}

@Composable
private fun ModeAssistChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) Color(0xFF102B14) else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = if (selected) Color(0xFF00FF41) else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = null
    )
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
