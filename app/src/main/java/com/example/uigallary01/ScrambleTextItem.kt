package com.example.uigallary01

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uigallary01.ui.theme.UiGallary01Theme
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ScrambleTextItem(
    modifier: Modifier = Modifier,
) {
    // スクランブル後に表示したいテキスト行を定義
    val targetLines = remember {
        listOf(
            "Signals weave through the dark.",
            "Fragments align into a message.",
            "Embrace the moment decoding you.",
        )
    }

    // 現在表示しているテキストの状態を保持
    var displayLines by remember { mutableStateOf(targetLines) }
    var playSeed by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(playSeed) {
        // 再生トリガーが変わるたびに新しいスクランブルを開始
        val random = Random(playSeed)
        repeat(ScrambleFrameCount) { frameIndex ->
            val progress = frameIndex.toScrambleProgress(totalFrames = ScrambleFrameCount)
            displayLines = targetLines.map { line ->
                line.scrambledCopy(
                    progress = progress,
                    random = random,
                )
            }
            delay(ScrambleFrameIntervalMillis)
        }
        // 最終フレームで確定したテキストを表示
        displayLines = targetLines
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // ターゲットのメッセージをスクランブル表示
            displayLines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = { playSeed++ }) {
            Text(text = "Scramble Again")
        }
    }
}

// 表示フレーム数に対して進捗率を算出する拡張関数
private fun Int.toScrambleProgress(totalFrames: Int): Float {
    require(totalFrames > 1)
    return (this.toFloat() / (totalFrames - 1)).coerceIn(0f, 1f)
}

// スクランブル用のテキストを生成する拡張関数
private fun String.scrambledCopy(
    progress: Float,
    random: Random,
): String {
    if (isEmpty() || progress >= 1f) return this
    val revealCount = (length * progress).toInt().coerceIn(0, length)
    val builder = StringBuilder(length)
    forEachIndexed { index, character ->
        val resolved = when {
            index < revealCount -> character
            character.isScrambleStable() -> character
            else -> ScrambleGlyphs.random(random)
        }
        builder.append(resolved)
    }
    return builder.toString()
}

// スクランブル中でも変化させない文字かどうかを判定する拡張関数
private fun Char.isScrambleStable(): Boolean {
    return isWhitespace() || !isLetterOrDigit()
}

private val ScrambleGlyphs: List<Char> = buildList {
    addAll('A'..'Z')
    addAll('a'..'z')
    addAll('0'..'9')
    addAll(listOf('-', '+', '*', '#', '%', '=', '@'))
}

private const val ScrambleFrameCount: Int = 28
private const val ScrambleFrameIntervalMillis: Long = 40L

@Preview
@Composable
private fun ScrambleTextItemPreview() {
    UiGallary01Theme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ScrambleTextItem(
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}
