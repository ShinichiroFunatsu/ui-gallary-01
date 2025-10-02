package com.example.uigallary01

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned

// 展開時に対象要素が必ず画面内に収まるようスクロールを依頼する拡張関数
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.ensureVisibleOnExpand(isExpanded: Boolean): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    // 折りたたみ時と展開時の高さを保持して展開後の変化を検知する
    var collapsedHeight by remember { mutableIntStateOf(0) }
    var expandedHeight by remember { mutableIntStateOf(0) }
    val shouldRequestScroll = isExpanded && expandedHeight > collapsedHeight

    LaunchedEffect(shouldRequestScroll) {
        if (shouldRequestScroll) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    bringIntoViewRequester(bringIntoViewRequester)
        .onGloballyPositioned { coordinates ->
            val measuredHeight = coordinates.size.height
            if (isExpanded) {
                expandedHeight = measuredHeight
            } else {
                collapsedHeight = measuredHeight
                expandedHeight = 0 // 次回の展開で改めて高さ変化を検知するためリセット
            }
        }
}
