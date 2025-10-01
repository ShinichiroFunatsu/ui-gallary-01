package com.example.uigallary01

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.runtime.withFrameNanos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun MatrixDigitalRainItem(
    modifier: Modifier = Modifier,
    state: MatrixDigitalRainState = rememberMatrixDigitalRainState(),
) {
    // 背景のキャンバスと簡単な説明テキストを重ねる
    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF010805))
            .drawBehind { drawRect(Color(0xFF010805)) },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawMatrixRain(state, textPaint)
        }
        Text(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            text = "Matrix Digital Rain",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFF6EF0B6),
                textAlign = TextAlign.Start,
            )
        )
    }
}

@Composable
fun rememberMatrixDigitalRainState(
    columnCount: Int = 48,
    fallSpeed: Float = 260f,
    glyphShiftSpeed: Float = 1.8f,
): MatrixDigitalRainState {
    // 雨粒のばらつきや経過時間をまとめて管理する状態を保持
    val elapsedTime = rememberElapsedSeconds()
    val columnSeeds = remember(columnCount) {
        val random = Random(2049)
        List(columnCount) {
            MatrixColumnSeed(
                speedMultiplier = lerp(0.72f, 1.32f, random.nextFloat()),
                headOffset = random.nextFloat(),
                dropLengthFactor = lerp(0.25f, 0.6f, random.nextFloat()),
                brightnessBias = lerp(0.2f, 0.9f, random.nextFloat()),
                glyphPhase = random.nextFloat(),
            )
        }
    }
    return remember {
        MatrixDigitalRainState(
            glyphs = MatrixGlyphSequence.Resurrections,
            columns = columnSeeds,
            elapsedSeconds = elapsedTime,
            fallSpeed = fallSpeed,
            glyphShiftSpeed = glyphShiftSpeed,
        )
    }
}

@Stable
class MatrixDigitalRainState internal constructor(
    internal val glyphs: List<Char>,
    internal val columns: List<MatrixColumnSeed>,
    internal val elapsedSeconds: State<Float>,
    internal val fallSpeed: Float,
    internal val glyphShiftSpeed: Float,
)

internal data class MatrixColumnSeed(
    val speedMultiplier: Float,
    val headOffset: Float,
    val dropLengthFactor: Float,
    val brightnessBias: Float,
    val glyphPhase: Float,
)

private object MatrixGlyphSequence {
    val Resurrections: List<Char> = """モエヤキオカ7ケサスz152ヨタワ4ネヌナ98ヒ0ホア3ウ セ¦:"꞊ミラリ╌ツテニハソコ—<ム0|*▪メシマ>+""".toList()
}

private fun DrawScope.drawMatrixRain(
    state: MatrixDigitalRainState,
    paint: Paint,
) {
    val columnCount = state.columns.size
    if (columnCount == 0) return

    val columnSpacing = size.width / columnCount
    val glyphSize = columnSpacing * 0.78f
    val rowSpacing = glyphSize * 1.12f
    val rowCount = (size.height / rowSpacing).toInt() + 2
    val totalHeight = rowCount * rowSpacing

    val elapsedSeconds by state.elapsedSeconds
    val baseSpeed = state.fallSpeed

    drawRect(Color(0xFF010805))

    val glyphs = state.glyphs
    val glyphCount = glyphs.size
    paint.textSize = glyphSize

    state.columns.forEachIndexed { index, seed ->
        val columnX = columnSpacing * (index + 0.5f)
        val dropLength = max(1f, seed.dropLengthFactor * rowCount)
        val totalTravelRows = rowCount + dropLength
        val pixelsPerSecond = baseSpeed * seed.speedMultiplier
        val rowsPerSecond = pixelsPerSecond / rowSpacing
        val travel = (elapsedSeconds * rowsPerSecond + seed.headOffset * totalTravelRows) % totalTravelRows
        val headRow = travel - dropLength

        for (row in 0 until rowCount) {
            val distanceFromHead = headRow - row
            if (distanceFromHead > 0f || distanceFromHead < -dropLength) continue

            val normalizedTail = min(1f, -distanceFromHead / dropLength)
            val glowStrength = 1f - normalizedTail
            val brightness = lerp(seed.brightnessBias, 1f, glowStrength)
            val color = matrixColorFor(brightness, glowStrength)
            paint.color = color

            val glyphPhase = elapsedSeconds * state.glyphShiftSpeed + seed.glyphPhase + row * 0.035f
            val glyphIndex = wrapUnit(glyphPhase) * glyphCount
            val glyph = glyphs[min(glyphCount - 1, glyphIndex.toInt())]
            val baseline = row * rowSpacing + glyphSize + (totalHeight - size.height) * 0.5f
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(glyph.toString(), columnX, baseline, paint)
            }
        }
    }
}

private fun matrixColorFor(brightness: Float, glow: Float): Int {
    // 尾の長さに応じて色調と輝度を滑らかに補間する
    val base = Color(0xFF0F2712)
    val mid = Color(0xFF1F8F3B)
    val head = Color(0xFFC7FFD8)
    val tone = when {
        glow > 0.85f -> colorLerp(mid, head, (glow - 0.85f) / 0.15f)
        glow > 0.35f -> colorLerp(base, mid, (glow - 0.35f) / 0.5f)
        else -> colorLerp(Color(0xFF04120A), base, glow / 0.35f)
    }
    val adjusted = colorLerp(Color.Black, tone, sqrt(brightness.coerceIn(0f, 1f)))
    return adjusted.toArgb()
}

private fun wrapUnit(value: Float): Float {
    val wrapped = value % 1f
    return if (wrapped < 0f) wrapped + 1f else wrapped
}

@Composable
private fun rememberElapsedSeconds(): State<Float> {
    // フレームごとの経過時間を蓄積して連続的な時間軸を作る
    val elapsed = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var previousFrameTime = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (previousFrameTime != 0L) {
                    val delta = (frameTime - previousFrameTime) / 1_000_000_000f
                    elapsed.floatValue += delta
                }
                previousFrameTime = frameTime
            }
        }
    }
    return elapsed
}
