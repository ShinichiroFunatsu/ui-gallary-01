package com.example.uigallary01

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ScrambleText(
    text: String,
    modifier: Modifier = Modifier,
    playKey: Any = text,
    scrambleSeed: Int = playKey.hashCode(),
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    glyphs: List<Char> = ScrambleTextDefaults.Glyphs,
    frameCount: Int = ScrambleTextDefaults.FrameCount,
    frameIntervalMillis: Long = ScrambleTextDefaults.FrameIntervalMillis,
    stableCharPredicate: (Char) -> Boolean = ScrambleTextDefaults.StableCharPredicate,
) {
    // 利用側で不正な値が渡されないように契約を明示
    require(frameCount > 1) { "frameCount must be greater than 1." }
    require(frameIntervalMillis >= 0L) { "frameIntervalMillis must be non-negative." }
    require(glyphs.isNotEmpty()) { "glyphs must not be empty." }

    var displayText by remember(playKey, text) { mutableStateOf(text) }

    LaunchedEffect(playKey, text, frameCount, frameIntervalMillis, scrambleSeed, glyphs, stableCharPredicate) {
        val targetText = text
        if (targetText.isEmpty()) {
            displayText = targetText
            return@LaunchedEffect
        }
        val random = Random(scrambleSeed)
        for (frameIndex in 0 until frameCount - 1) {
            val progress = frameIndex.toScrambleProgress(totalFrames = frameCount)
            displayText = targetText.scrambledCopy(
                progress = progress,
                random = random,
                glyphs = glyphs,
                stableCharPredicate = stableCharPredicate,
            )
            if (frameIntervalMillis > 0L) {
                delay(frameIntervalMillis)
            }
        }
        displayText = targetText
    }

    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = color,
    )
}

object ScrambleTextDefaults {
    // スクランブルに使用する文字一覧
    val Glyphs: List<Char> = buildList {
        addAll('A'..'Z')
        addAll('a'..'z')
        addAll('0'..'9')
        addAll(listOf('-', '+', '*', '#', '%', '=', '@'))
    }

    // アニメーションの総フレーム数
    const val FrameCount: Int = 28

    // 各フレーム間の待機時間
    const val FrameIntervalMillis: Long = 40L

    // スクランブル中でも固定表示したい文字の判定
    val StableCharPredicate: (Char) -> Boolean = { character ->
        character.isWhitespace() || !character.isLetterOrDigit()
    }
}

// 表示フレーム数に対して進捗率を算出する拡張関数
private fun Int.toScrambleProgress(totalFrames: Int): Float {
    return (this.toFloat() / (totalFrames - 1)).coerceIn(0f, 1f)
}

// スクランブル用のテキストを生成する拡張関数
private fun String.scrambledCopy(
    progress: Float,
    random: Random,
    glyphs: List<Char>,
    stableCharPredicate: (Char) -> Boolean,
): String {
    if (isEmpty() || progress >= 1f) return this
    val revealCount = (length * progress).toInt().coerceIn(0, length)
    val builder = StringBuilder(length)
    forEachIndexed { index, character ->
        val resolved = when {
            index < revealCount -> character
            stableCharPredicate(character) -> character
            else -> glyphs.random(random)
        }
        builder.append(resolved)
    }
    return builder.toString()
}
