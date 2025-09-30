package com.example.uigallary01

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uigallary01.ui.theme.UiGallary01Theme

@Composable
fun GalleryScreen(modifier: Modifier = Modifier) {
    // Moody Snow をフルスクリーン表示するかどうかを保持
    var isMoodySnowExpanded by rememberSaveable { mutableStateOf(false) }
    val moodySnowState = rememberMoodySnowBackgroundState()

    // リスト表示時のカード位置を記録して、拡大アニメーションの開始点に利用
    var moodySnowCardBounds by remember { mutableStateOf<Rect?>(null) }
    var animatedBounds by remember { mutableStateOf<Rect?>(null) }

    val galleryItems = remember {
        listOf(
            GalleryItem(
                title = "Hello World Dialog",
                content = { HelloWorldDialogItem() }
            )
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        val density = LocalDensity.current

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(galleryItems) { item ->
                item.ListItem()
            }

            item {
                GalleryItem(
                    title = "Moody Snow Background",
                    content = { MoodySnowBackgroundItem(state = moodySnowState) },
                    onClick = {
                        val bounds = requireNotNull(moodySnowCardBounds)
                        animatedBounds = bounds
                        isMoodySnowExpanded = true
                    }
                ).ListItem(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        if (!isMoodySnowExpanded) {
                            moodySnowCardBounds = coordinates.boundsInRoot()
                        }
                    }
                )
            }
        }

        val startBounds = animatedBounds
        if (startBounds != null || isMoodySnowExpanded) {
            val activeBounds = startBounds ?: moodySnowCardBounds
            if (activeBounds != null) {
                val startWidth = with(density) { activeBounds.width.toDp() }
                val startHeight = with(density) { activeBounds.height.toDp() }
                val startLeft = with(density) { activeBounds.left.toDp() }
                val startTop = with(density) { activeBounds.top.toDp() }

                val overlayAnimationSpec = tween<Dp>(durationMillis = 520, easing = FastOutSlowInEasing)

                val animatedWidth by animateDpAsState(
                    targetValue = if (isMoodySnowExpanded) maxWidth else startWidth,
                    animationSpec = overlayAnimationSpec,
                    label = "moodySnowWidth",
                    finishedListener = { value ->
                        if (!isMoodySnowExpanded && value == startWidth) {
                            animatedBounds = null
                        }
                    }
                )
                val animatedHeight by animateDpAsState(
                    targetValue = if (isMoodySnowExpanded) maxHeight else startHeight,
                    animationSpec = overlayAnimationSpec,
                    label = "moodySnowHeight"
                )
                val animatedOffsetX by animateDpAsState(
                    targetValue = if (isMoodySnowExpanded) 0.dp else startLeft,
                    animationSpec = overlayAnimationSpec,
                    label = "moodySnowOffsetX"
                )
                val animatedOffsetY by animateDpAsState(
                    targetValue = if (isMoodySnowExpanded) 0.dp else startTop,
                    animationSpec = overlayAnimationSpec,
                    label = "moodySnowOffsetY"
                )
                val animatedCornerRadius by animateDpAsState(
                    targetValue = if (isMoodySnowExpanded) 0.dp else 24.dp,
                    animationSpec = overlayAnimationSpec,
                    label = "moodySnowCornerRadius"
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .offset(x = animatedOffsetX, y = animatedOffsetY)
                            .requiredWidth(animatedWidth)
                            .requiredHeight(animatedHeight)
                            .graphicsLayer {
                                clip = true
                                shape = RoundedCornerShape(animatedCornerRadius)
                            }
                    ) {
                        MoodySnowBackgroundFullScreen(
                            modifier = Modifier.fillMaxSize(),
                            state = moodySnowState,
                            onDismiss = {
                                animatedBounds = moodySnowCardBounds
                                isMoodySnowExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryScreenPreview() {
    UiGallary01Theme {
        GalleryScreen()
    }
}

// ギャラリーに表示する要素の定義
private data class GalleryItem(
    val title: String,
    val content: @Composable () -> Unit,
    val onClick: (() -> Unit)? = null,
)

// データクラス自身が描画手段を提供できるように拡張関数化
@Composable
private fun GalleryItem.ListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = this.onClick,
) {
    val cardContent: @Composable () -> Unit = {
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

    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
            cardContent()
        }
    } else {
        Card(modifier = modifier.fillMaxWidth()) {
            cardContent()
        }
    }
}
