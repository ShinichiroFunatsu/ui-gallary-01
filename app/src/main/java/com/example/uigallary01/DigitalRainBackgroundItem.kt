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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shadow
import com.example.uigallary01.ui.theme.UiGallary01Theme

@Composable
fun DigitalRainBackgroundItem(
    modifier: Modifier = Modifier,
) {
    // タップで高さを切り替えるアニメーションを保持
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val collapsedHeight = 160.dp
    val expandedHeight = 360.dp
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) expandedHeight else collapsedHeight,
        label = "digitalRainHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .clip(RoundedCornerShape(24.dp))
            .clipToBounds()
            .background(Color.Black)
            .clickable { isExpanded = !isExpanded }
    ) {
        DigitalRainBackground(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .viewportHeight(fullHeight = expandedHeight, visibleHeight = animatedHeight),
            version = GlyphVersion.Resurrections,
            backgroundColor = Color.Black,
        )
        DigitalRainCopy(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Bottom,
            showMessage = isExpanded,
            messageSpacing = 16.dp,
        )
    }
}

@Composable
private fun DigitalRainCopy(
    modifier: Modifier,
    verticalArrangement: Arrangement.Vertical,
    showMessage: Boolean,
    messageSpacing: Dp = 0.dp,
) {
    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = Alignment.Start,
    ) {
        // タイトルを所定の位置に保ったまま説明文を加える
        AnimatedVisibility(visible = showMessage) {
            Column {
                ScrambleText(
                    text = "その信号の一つひとつを感じ\nあたらしい世界へ踏み出す光景を想像して下さい",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB4FFB4),
                )
                Spacer(modifier = Modifier.height(messageSpacing))
            }
        }
        ScrambleText(
            text = "Digital Rain",
            style = MaterialTheme.typography.headlineSmall.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.65f),
                    blurRadius = 12f,
                )
            ),
            color = Color(0xFF90FF90),
        )
    }
}

// ViewPort の可視領域だけを変化させ、内部の描画サイズは最大値で固定する拡張関数
private fun Modifier.viewportHeight(
    fullHeight: Dp,
    visibleHeight: Dp,
): Modifier = layout { measurable, constraints ->
    val fullHeightPx = fullHeight.roundToPx()
    val placeable = measurable.measure(
        Constraints(
            minWidth = constraints.minWidth,
            maxWidth = constraints.maxWidth,
            minHeight = fullHeightPx,
            maxHeight = fullHeightPx,
        )
    )
    val visibleHeightPx = visibleHeight.roundToPx().coerceIn(
        minimumValue = constraints.minHeight,
        maximumValue = fullHeightPx,
    )
    layout(width = placeable.width, height = visibleHeightPx) {
        placeable.placeRelative(0, 0)
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
