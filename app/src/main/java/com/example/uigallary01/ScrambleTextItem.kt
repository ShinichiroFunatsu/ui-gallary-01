package com.example.uigallary01

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uigallary01.ui.theme.UiGallary01Theme
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ScrambleTextItem(modifier: Modifier = Modifier) {
    // 断片的な信号が合わさって一つのメッセージになる演出を表現
    val primaryMessage = "Signal received"
    val supportingMessage = "静かな空間に流れ込むノイズを耳を澄ませて感じてください"
    val scrambled by rememberScrambledText(
        message = primaryMessage,
        cycleDelayMillis = 1600L,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF020202),
                        Color(0xFF11121A),
                        Color(0xFF1F2F3F),
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Scramble Text", 
            style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF5AC8FA))
        )
        Text(
            text = scrambled,
            style = MaterialTheme.typography.headlineSmall.copy(color = Color(0xFFE8F1FF))
        )
        Text(
            text = supportingMessage,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCAD6EA))
        )
    }
}

@Composable
private fun rememberScrambledText(
    message: String,
    cycleDelayMillis: Long,
): State<String> {
    // 生成するランダム文字列を一定周期で更新する
    return produceState(initialValue = message) {
        val random = Random(System.currentTimeMillis())
        while (true) {
            val characters = message.length
            for (revealed in 0..characters) {
                val lockedCount = revealed.coerceIn(0, characters)
                val jitterIterations = if (lockedCount == characters) 1 else 6
                repeat(jitterIterations) {
                    value = message.scrambledPreview(lockedCount, random)
                    delay(36L)
                }
            }
            delay(cycleDelayMillis)
        }
    }
}

private fun String.scrambledPreview(
    lockedCount: Int,
    random: Random,
): String {
    val safeLockedCount = lockedCount.coerceIn(0, length)
    val builder = StringBuilder(length)
    forEachIndexed { index, original ->
        val nextChar = when {
            original.isWhitespace() -> original
            index < safeLockedCount -> original
            else -> original.randomized(random)
        }
        builder.append(nextChar)
    }
    return builder.toString()
}

private fun Char.randomized(random: Random): Char {
    return when {
        isDigit() -> DigitCandidates.random(random)
        isUpperCase() -> UppercaseCandidates.random(random)
        isLowerCase() -> LowercaseCandidates.random(random)
        else -> SymbolCandidates.random(random)
    }
}

private val UppercaseCandidates = ('A'..'Z').toList()
private val LowercaseCandidates = ('a'..'z').toList()
private val DigitCandidates = ('0'..'9').toList()
private val SymbolCandidates = listOf('~', '#', '*', '+', '-', '?', '/', '=', '%', '§')

@Preview
@Composable
private fun ScrambleTextItemPreview() {
    UiGallary01Theme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ScrambleTextItem(
                modifier = Modifier
                    .padding(16.dp)
            )
        }
    }
}
