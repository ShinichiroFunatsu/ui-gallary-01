package com.example.uigallary01

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember

/**
 * 0〜1 の進行度を取り続ける State を途切れない累積値へ変換する拡張関数。
 * ループ系アニメーションの値を連続した時間軸として扱いたい場合に利用する。
 */
@Composable
internal fun State<Float>.rememberContinuousProgress(): State<Float> {
    val totalProgress = remember { mutableFloatStateOf(value) }
    val previousProgress = remember { mutableFloatStateOf(value) }

    LaunchedEffect(value) {
        val current = value
        val last = previousProgress.floatValue
        val delta = if (current < last) {
            (1f - last) + current
        } else {
            current - last
        }
        totalProgress.floatValue += delta
        previousProgress.floatValue = current
    }

    return totalProgress
}
