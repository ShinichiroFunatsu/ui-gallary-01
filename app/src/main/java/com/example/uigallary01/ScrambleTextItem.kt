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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uigallary01.ui.theme.UiGallary01Theme

@Composable
fun ScrambleTextItem(
    modifier: Modifier = Modifier,
) {
    // ギャラリー表示用の文面を定義
    val targetText = ScrambleDemoText
    var playSeed by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 再生シードをキーにスクランブル表示を更新
            ScrambleText(
                text = targetText,
                playKey = playSeed,
                scrambleSeed = playSeed,
                lineAnimationMode = ScrambleLineAnimationMode.Individually,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = { playSeed++ }) {
            Text(text = "Scramble Again")
        }
    }
}

private val ScrambleDemoLines = listOf(
    "Signals weave through the dark.",
    "Fragments align into a message.",
    "Embrace the moment decoding you.",
)

private val ScrambleDemoText = ScrambleDemoLines.joinToString(separator = "\n")

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
