package com.example.uigallary01

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

// 展開時に対象要素が必ず画面内に収まるようスクロールを依頼する拡張関数
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.ensureVisibleOnExpand(isExpanded: Boolean): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            withFrameNanos { }
            bringIntoViewRequester.bringIntoView()
        }
    }

    bringIntoViewRequester(bringIntoViewRequester)
}
