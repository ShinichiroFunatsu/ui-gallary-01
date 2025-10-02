package com.example.uigallary01

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned

// 展開時に対象要素が必ず画面内に収まるようスクロールを依頼する拡張関数
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.ensureVisibleOnExpand(isExpanded: Boolean): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    // 折りたたみ時の高さを保持し、展開で増えた領域の矩形を計算する
    var collapsedHeight by remember { mutableIntStateOf(0) }
    var targetRect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(targetRect) {
        val rect = targetRect ?: return@LaunchedEffect
        bringIntoViewRequester.bringIntoView(rect)
        targetRect = null
    }

    bringIntoViewRequester(bringIntoViewRequester)
        .onGloballyPositioned { coordinates ->
            val measuredHeight = coordinates.size.height
            if (isExpanded) {
                val collapsedTop = if (collapsedHeight == 0) 0 else collapsedHeight
                val shouldScroll = collapsedHeight == 0 || measuredHeight > collapsedHeight
                targetRect = if (shouldScroll) {
                    Rect(
                        left = 0f,
                        top = collapsedTop.toFloat(),
                        right = coordinates.size.width.toFloat(),
                        bottom = measuredHeight.toFloat()
                    )
                } else {
                    null
                }
            } else {
                collapsedHeight = measuredHeight
                targetRect = null // 次回の展開で再計算するためリセット
            }
        }
}
