/**
 * Digital Rain 風の背景を Jetpack Compose の Canvas で描画するユーティリティ。
 * Box(modifier = Modifier.fillMaxSize().background(Color.Black)) { DigitalRainBackground(Modifier.fillMaxSize()) }
 *
 * @param modifier 背景として敷くための修飾子
 * @param version グリフ循環順（Classic/Resurrections）の選択
 * @param mode 雨筋の挙動（スクロール / 輝度波）
 * @param columnWidthDp 列のピッチ（dp）
 * @param fontSizeSp フォントサイズ（sp）
 * @param baseColor テール基調色
 * @param backgroundColor 背景塗りつぶし色
 * @param headHighlightColor ヘッド強調色
 * @param densityScale 列密度のスケール
 * @param minSpeedCps 列速度の下限（cells/sec）
 * @param maxSpeedCps 列速度の上限（cells/sec）
 * @param tailLengthCells 減衰テールのセル数
 * @param spawnChancePerSec Reset/Illumination 用のスポーン率
 * @param glyphMorphChancePerSec 文字モーフ頻度（両モード）
 * @param jitterPx 文字描画位置の微小ジッター
 * @param wrapMode スクロール時の終端処理（Circular / Reset）
 * @param morphBias グリフ循環順で前方向へ寄せる比率（0..1）
 * @param seed 乱数シード（0 で現在時刻）
 * @param enabled アニメーション制御
 *
 * RainMode.Scroll は文字列自体が下方へ流れる映画的描画、RainMode.Illumination は列を固定したまま輝度波だけを送る静動表現。
 * WrapMode.Circular はリングバッファで循環し続け、WrapMode.Reset はテールまで抜けた列を一旦消してから再スポーンさせる。
 * グリフ循環順は映画風の並びをハードコードし、next/prev 遷移でモーフを優先。含まれない文字はカテゴリ重み付き乱択へフォールバック。
 *
 * Portions adapted from ideas inspired by Rezmason/matrix (MIT).
 */

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class GlyphVersion { Classic, Resurrections }

enum class RainMode { Scroll, Illumination }

enum class WrapMode { Circular, Reset }

@Composable
fun DigitalRainBackground(
    modifier: Modifier = Modifier,
    version: GlyphVersion = GlyphVersion.Classic,
    mode: RainMode = RainMode.Scroll,
    columnWidthDp: Dp = 16.dp,
    fontSizeSp: TextUnit = 16.sp,
    baseColor: Color = Color(0xFF00FF41),
    backgroundColor: Color = Color(0xFF000000),
    headHighlightColor: Color = Color(0xFFE0FFE0),
    densityScale: Float = 1.0f,
    minSpeedCps: Float = 12f,
    maxSpeedCps: Float = 24f,
    tailLengthCells: Int = 18,
    spawnChancePerSec: Float = 0.8f,
    glyphMorphChancePerSec: Float = 1.5f,
    jitterPx: Float = 0f,
    wrapMode: WrapMode = WrapMode.Circular,
    morphBias: Float = 0.2f,
    seed: Long = 0L,
    enabled: Boolean = true
) {
    require(densityScale > 0f)
    require(minSpeedCps <= maxSpeedCps)
    require(tailLengthCells >= 0)
    require(morphBias in 0f..1f)

    val resolvedSeed = remember(seed) { if (seed != 0L) seed else System.currentTimeMillis() }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = remember(fontSizeSp) {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = fontSizeSp,
            letterSpacing = 0.sp
        )
    }
    val glyphOrder = remember(version) { GlyphOrder(version) }
    val glyphPicker = remember(version) { GlyphPicker() }
    val glyphLayoutCache = remember(textMeasurer, textStyle) { GlyphLayoutCache(textMeasurer, textStyle) }
    val glyphInspector = remember(glyphLayoutCache) { GlyphRenderInspector(glyphLayoutCache) }

    val fontMetrics = remember(textMeasurer, textStyle) { FontMetrics(textMeasurer, textStyle) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val scrollState = remember { ScrollRainState() }
    val illuminationState = remember { IlluminationRainState() }

    val latestMode by rememberUpdatedState(mode)
    val frameClock = remember { mutableLongStateOf(0L) }

    LaunchedEffect(enabled, latestMode, glyphOrder, glyphPicker, resolvedSeed) {
        if (!enabled) return@LaunchedEffect
        var previousTime = frameClock.longValue
        while (enabled) {
            withFrameNanos { time ->
                val deltaSec = if (previousTime == 0L) 0f else (time - previousTime) / 1_000_000_000f
                previousTime = time
                val spawnProbability = 1f - exp(-spawnChancePerSec * deltaSec)
                val morphProbability = 1f - exp(-glyphMorphChancePerSec * deltaSec)
                when (latestMode) {
                    RainMode.Scroll -> scrollState.onFrame(
                        time,
                        deltaSec,
                        spawnProbability,
                        morphProbability,
                        glyphOrder,
                        glyphPicker,
                        wrapMode,
                        morphBias
                    )
                    RainMode.Illumination -> illuminationState.onFrame(
                        time,
                        deltaSec,
                        spawnProbability,
                        morphProbability,
                        glyphOrder,
                        glyphPicker,
                        morphBias
                    )
                }
                frameClock.longValue = time
            }
        }
    }

    Canvas(
        modifier = modifier.onSizeChanged { size ->
            canvasSize = size
        }
    ) {
        @Suppress("UNUSED_VARIABLE")
        val frameStamp = frameClock.longValue
        drawRect(color = backgroundColor, size = size)
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@Canvas
        val cellWidthPx = (columnWidthDp.toPx() / densityScale).coerceAtLeast(1f)
        val rowHeight = fontMetrics.rowHeight
        if (rowHeight <= 0f || cellWidthPx <= 0f) return@Canvas
        val columns = max(1, floor(size.width / cellWidthPx).toInt())
        val visibleRows = max(1, ceil(size.height / rowHeight).toInt())
        when (mode) {
            RainMode.Scroll -> {
                scrollState.ensureLayout(
                    columnCount = columns,
                    visibleRows = visibleRows,
                    tailLength = tailLengthCells,
                    minSpeed = minSpeedCps,
                    maxSpeed = maxSpeedCps,
                    seed = resolvedSeed,
                    glyphOrder = glyphOrder,
                    glyphPicker = glyphPicker,
                    wrapMode = wrapMode
                )
                scrollState.draw(
                    scope = this,
                    metrics = fontMetrics,
                    glyphInspector = glyphInspector,
                    glyphLayoutCache = glyphLayoutCache,
                    cellWidthPx = cellWidthPx,
                    baseColor = baseColor,
                    headHighlightColor = headHighlightColor,
                    jitterPx = jitterPx
                )
            }
            RainMode.Illumination -> {
                illuminationState.ensureLayout(
                    columnCount = columns,
                    visibleRows = visibleRows,
                    tailLength = tailLengthCells,
                    minSpeed = minSpeedCps,
                    maxSpeed = maxSpeedCps,
                    seed = resolvedSeed,
                    glyphOrder = glyphOrder,
                    glyphPicker = glyphPicker
                )
                illuminationState.draw(
                    scope = this,
                    metrics = fontMetrics,
                    glyphInspector = glyphInspector,
                    glyphLayoutCache = glyphLayoutCache,
                    cellWidthPx = cellWidthPx,
                    baseColor = baseColor,
                    headHighlightColor = headHighlightColor,
                    jitterPx = jitterPx
                )
            }
        }
    }
}

private class GlyphOrder(version: GlyphVersion) {
    val sequence: CharArray
    private val positions: IntArray = IntArray(0x10000) { -1 }

    init {
        val raw = when (version) {
            GlyphVersion.Classic -> """
                モエヤキオカ7ケサスz152ヨタワ4ネヌナ98ヒ0ホア3ウセ¦:"꞊ミラリ╌ツテニハソ▪—<>0|+*コシマムメ
            """.trimIndent()
            GlyphVersion.Resurrections -> """
                モエヤキオカ7ケサスz152ヨタワ4ネヌナ98ヒ0ホア3ウセ¦:"꞊ミラリ╌ツテニハソコ—<ム0|*▪メシマ>+
            """.trimIndent()
        }
        val filtered = raw.filterNot { it.isWhitespace() }
        sequence = filtered.toCharArray()
        sequence.forEachIndexed { index, c ->
            if (c.code < positions.size) {
                positions[c.code] = index
            }
        }
    }

    fun nextOf(ch: Char): Char? {
        val idx = indexOf(ch)
        return if (idx >= 0) {
            sequence[(idx + 1) % sequence.size]
        } else null
    }

    fun prevOf(ch: Char): Char? {
        val idx = indexOf(ch)
        return if (idx >= 0) {
            sequence[(idx - 1).modPositive(sequence.size)]
        } else null
    }

    fun random(rng: Random): Char = if (sequence.isNotEmpty()) sequence[rng.nextInt(sequence.size)] else '·'

    private fun indexOf(ch: Char): Int = if (ch.code < positions.size) positions[ch.code] else -1
}

private class GlyphPicker {
    private val katakana: CharArray = buildList {
        for (code in 0x30A1..0x30FA) add(code.toChar())
        for (code in 0x30FD..0x30FF) add(code.toChar())
    }.toCharArray()
    private val digits: CharArray = CharArray(10) { index -> ('0'.code + index).toChar() }
    private val uppercase: CharArray = CharArray(26) { index -> ('A'.code + index).toChar() }
    private val symbols: CharArray = charArrayOf('.', ',', ':', ';', '+', '=', '-', '*', '>', '<', '|')
    private val categories = arrayOf(katakana, digits, uppercase, symbols)
    private val cumulativeWeights = floatArrayOf(0.7f, 0.825f, 0.95f, 1f)

    fun initialGlyph(rng: Random, order: GlyphOrder): Char {
        return if (rng.nextFloat() < 0.6f) order.random(rng) else randomGlyph(rng)
    }

    fun randomGlyph(rng: Random): Char {
        val r = rng.nextFloat()
        val categoryIndex = when {
            r < cumulativeWeights[0] -> 0
            r < cumulativeWeights[1] -> 1
            r < cumulativeWeights[2] -> 2
            else -> 3
        }
        val bucket = categories[categoryIndex]
        return bucket[rng.nextInt(bucket.size)]
    }

    fun morph(current: Char, rng: Random, order: GlyphOrder, morphBias: Float): Char {
        val usePrev = rng.nextFloat() < morphBias
        val candidate = if (usePrev) order.prevOf(current) else order.nextOf(current)
        if (candidate != null) return candidate
        return if (rng.nextFloat() < 0.5f) order.random(rng) else randomGlyph(rng)
    }
}

private class GlyphLayoutCache(
    private val textMeasurer: TextMeasurer,
    private val textStyle: TextStyle
) {
    private val cache = HashMap<Char, TextLayoutResult>()
    private val buffer = CharArray(1)

    fun layoutFor(ch: Char): TextLayoutResult {
        return cache.getOrPut(ch) {
            buffer[0] = ch
            textMeasurer.measure(AnnotatedString(buffer.concatToString()), style = textStyle)
        }
    }
}

private class GlyphRenderInspector(
    private val layoutCache: GlyphLayoutCache,
    private val fallbackChar: Char = '·'
) {
    private val states = ByteArray(0x10000)

    init {
        layoutCache.layoutFor(fallbackChar)
    }

    fun ensureRenderable(ch: Char): Char {
        val code = ch.code
        val state = if (code < states.size) states[code] else 0
        if (state == 1.toByte()) return ch
        if (state == 2.toByte()) return fallbackChar
        val layout = layoutCache.layoutFor(ch)
        val renderable = layout.size.width > 0 && layout.size.height > 0
        if (code < states.size) {
            states[code] = if (renderable) 1 else 2
        }
        return if (renderable) ch else fallbackChar
    }
}

private class FontMetrics(textMeasurer: TextMeasurer, textStyle: TextStyle) {
    val ascent: Float
    val rowHeight: Float

    init {
        val layout = textMeasurer.measure(AnnotatedString("ア"), style = textStyle)
        val top = layout.getLineTop(0)
        val bottom = layout.getLineBottom(0)
        ascent = -top
        rowHeight = bottom - top
    }
}

private data class Stream(
    val column: Int,
    var headRow: Float,
    val speedCps: Float,
    val tail: Int,
    val rng: Random
)

private class ScrollColumn(
    val stream: Stream,
    val chars: CharArray,
    val lastMorphNs: LongArray,
    var active: Boolean
)

private class ScrollRainState {
    private var columnCount: Int = 0
    private var visibleRows: Int = 0
    private var tailLength: Int = 0
    private var bufferRows: Int = 0
    private var minSpeed: Float = 0f
    private var maxSpeed: Float = 0f
    private var wrapMode: WrapMode = WrapMode.Circular
    private var glyphOrderToken: GlyphOrder? = null
    private var glyphPickerToken: GlyphPicker? = null
    private val columns = mutableListOf<ScrollColumn>()

    fun ensureLayout(
        columnCount: Int,
        visibleRows: Int,
        tailLength: Int,
        minSpeed: Float,
        maxSpeed: Float,
        seed: Long,
        glyphOrder: GlyphOrder,
        glyphPicker: GlyphPicker,
        wrapMode: WrapMode
    ) {
        val needsRebuild = columnCount != this.columnCount ||
            visibleRows != this.visibleRows ||
            tailLength != this.tailLength ||
            minSpeed != this.minSpeed ||
            maxSpeed != this.maxSpeed ||
            wrapMode != this.wrapMode ||
            glyphOrder !== glyphOrderToken ||
            glyphPicker !== glyphPickerToken
        if (!needsRebuild) return
        this.columnCount = columnCount
        this.visibleRows = visibleRows
        this.tailLength = tailLength
        bufferRows = visibleRows + tailLength + 8
        this.minSpeed = minSpeed
        this.maxSpeed = maxSpeed
        this.wrapMode = wrapMode
        this.glyphOrderToken = glyphOrder
        this.glyphPickerToken = glyphPicker
        columns.clear()
        for (column in 0 until columnCount) {
            val rng = Random(seed + column)
            val speed = minSpeed + (maxSpeed - minSpeed) * rng.nextFloat()
            val initialHead = if (wrapMode == WrapMode.Reset) {
                -rng.nextFloat() * (tailLength + 1)
            } else {
                rng.nextFloat() * (visibleRows + tailLength)
            }
            val stream = Stream(column, initialHead, speed, tailLength, rng)
            val chars = CharArray(bufferRows)
            val lastMorphNs = LongArray(bufferRows)
            fillInitial(chars, rng, glyphOrder, glyphPicker)
            columns.add(ScrollColumn(stream, chars, lastMorphNs, active = true))
        }
    }

    fun onFrame(
        timeNs: Long,
        deltaSec: Float,
        spawnProbability: Float,
        morphProbability: Float,
        glyphOrder: GlyphOrder,
        glyphPicker: GlyphPicker,
        wrapMode: WrapMode,
        morphBias: Float
    ) {
        if (deltaSec <= 0f) return
        val tail = tailLength
        val visible = visibleRows
        columns.forEach { column ->
            val stream = column.stream
            val rng = stream.rng
            if (!column.active) {
                if (wrapMode == WrapMode.Reset && rng.nextFloat() < spawnProbability) {
                    stream.headRow = -tail.toFloat()
                    fillInitial(column.chars, rng, glyphOrder, glyphPicker)
                    column.active = true
                }
                return@forEach
            }
            val previousHead = stream.headRow
            stream.headRow += stream.speedCps * deltaSec
            val previousIndex = floor(previousHead).toInt()
            val currentIndex = floor(stream.headRow).toInt()
            if (currentIndex > previousIndex) {
                var step = previousIndex + 1
                while (step <= currentIndex) {
                    val bufferIndex = step.modPositive(bufferRows)
                    column.chars[bufferIndex] = glyphPicker.initialGlyph(rng, glyphOrder)
                    column.lastMorphNs[bufferIndex] = timeNs
                    step++
                }
            }
            if (wrapMode == WrapMode.Reset && stream.headRow - tail > visible) {
                column.active = false
                return@forEach
            }
            if (morphProbability > 0f) {
                val maxDepth = tail
                var alphaStep = 0
                while (alphaStep <= maxDepth) {
                    val bufferIndex = (currentIndex - alphaStep).modPositive(bufferRows)
                    if (rng.nextFloat() < morphProbability) {
                        val currentChar = column.chars[bufferIndex]
                        val nextChar = glyphPicker.morph(currentChar, rng, glyphOrder, morphBias)
                        column.chars[bufferIndex] = nextChar
                        column.lastMorphNs[bufferIndex] = timeNs
                    }
                    alphaStep++
                }
            }
        }
    }

    fun draw(
        scope: DrawScope,
        metrics: FontMetrics,
        glyphInspector: GlyphRenderInspector,
        glyphLayoutCache: GlyphLayoutCache,
        cellWidthPx: Float,
        baseColor: Color,
        headHighlightColor: Color,
        jitterPx: Float
    ) {
        if (columnCount == 0) return
        val ascent = metrics.ascent
        val rowHeight = metrics.rowHeight
        val visible = visibleRows
        val buffer = bufferRows
        columns.forEach { column ->
            if (!column.active) return@forEach
            val stream = column.stream
            val rng = stream.rng
            val baseIndex = floor(stream.headRow).toInt()
            var alpha = 1f
            val maxDepth = stream.tail + visible
            var offset = 0
            while (offset <= maxDepth) {
                val row = baseIndex - offset
                if (row in 0 until visible) {
                    val charIndex = (baseIndex - offset).modPositive(buffer)
                    val rawChar = column.chars[charIndex]
                    val displayChar = glyphInspector.ensureRenderable(rawChar)
                    val layout = glyphLayoutCache.layoutFor(displayChar)
                    val color = if (offset <= 1) headHighlightColor else baseColor
                    val jitterX = if (jitterPx == 0f) 0f else (rng.nextFloat() - 0.5f) * 2f * jitterPx
                    val jitterY = if (jitterPx == 0f) 0f else (rng.nextFloat() - 0.5f) * 2f * jitterPx
                    val topLeft = Offset(
                        x = column.stream.column * cellWidthPx + jitterX,
                        y = row * rowHeight - ascent + jitterY
                    )
                    scope.drawText(
                        textLayoutResult = layout,
                        color = color,
                        topLeft = topLeft,
                        alpha = alpha
                    )
                }
                alpha *= 0.86f
                if (alpha < 0.001f) break
                offset++
            }
        }
    }

    private fun fillInitial(
        chars: CharArray,
        rng: Random,
        glyphOrder: GlyphOrder,
        glyphPicker: GlyphPicker
    ) {
        for (index in chars.indices) {
            chars[index] = glyphPicker.initialGlyph(rng, glyphOrder)
        }
    }
}

private data class Illumination(
    val column: Int,
    var phase: Float,
    val periodCells: Float,
    val speedCps: Float,
    val rng: Random
)

private class IlluminationColumn(
    val illumination: Illumination,
    val chars: CharArray,
    val lastMorphNs: LongArray
)

private class IlluminationRainState {
    private var columnCount: Int = 0
    private var visibleRows: Int = 0
    private var tailLength: Int = 0
    private var ringRows: Int = 0
    private var minSpeed: Float = 0f
    private var maxSpeed: Float = 0f
    private var glyphOrderToken: GlyphOrder? = null
    private var glyphPickerToken: GlyphPicker? = null
    private val columns = mutableListOf<IlluminationColumn>()

    fun ensureLayout(
        columnCount: Int,
        visibleRows: Int,
        tailLength: Int,
        minSpeed: Float,
        maxSpeed: Float,
        seed: Long,
        glyphOrder: GlyphOrder,
        glyphPicker: GlyphPicker
    ) {
        val needsRebuild = columnCount != this.columnCount ||
            visibleRows != this.visibleRows ||
            tailLength != this.tailLength ||
            minSpeed != this.minSpeed ||
            maxSpeed != this.maxSpeed ||
            glyphOrder !== glyphOrderToken ||
            glyphPicker !== glyphPickerToken
        if (!needsRebuild) return
        this.columnCount = columnCount
        this.visibleRows = visibleRows
        this.tailLength = tailLength
        ringRows = visibleRows + tailLength + 8
        this.minSpeed = minSpeed
        this.maxSpeed = maxSpeed
        this.glyphOrderToken = glyphOrder
        this.glyphPickerToken = glyphPicker
        columns.clear()
        for (column in 0 until columnCount) {
            val rng = Random(seed + column)
            val speed = minSpeed + (maxSpeed - minSpeed) * rng.nextFloat()
            val period = max(1f, tailLength.toFloat() + rng.nextFloat() * 4f)
            val illumination = Illumination(
                column = column,
                phase = rng.nextFloat(),
                periodCells = period,
                speedCps = speed,
                rng = rng
            )
            val chars = CharArray(ringRows)
            val lastMorphNs = LongArray(ringRows)
            fillInitial(chars, rng, glyphOrder, glyphPicker)
            columns.add(IlluminationColumn(illumination, chars, lastMorphNs))
        }
    }

    fun onFrame(
        timeNs: Long,
        deltaSec: Float,
        spawnProbability: Float,
        morphProbability: Float,
        glyphOrder: GlyphOrder,
        glyphPicker: GlyphPicker,
        morphBias: Float
    ) {
        if (deltaSec <= 0f) return
        val ring = ringRows
        val visible = visibleRows
        columns.forEach { column ->
            val illumination = column.illumination
            val rng = illumination.rng
            illumination.phase = ((illumination.phase + (illumination.speedCps * deltaSec) / illumination.periodCells) % 1f + 1f) % 1f
            if (spawnProbability > 0f && rng.nextFloat() < spawnProbability * 0.2f) {
                illumination.phase = (illumination.phase + rng.nextFloat() * 0.1f) % 1f
            }
            if (morphProbability > 0f) {
                var index = 0
                val limit = min(ring, visible + tailLength)
                while (index < limit) {
                    if (rng.nextFloat() < morphProbability) {
                        val currentChar = column.chars[index]
                        val nextChar = glyphPicker.morph(currentChar, rng, glyphOrder, morphBias)
                        column.chars[index] = nextChar
                        column.lastMorphNs[index] = timeNs
                    }
                    index++
                }
            }
        }
    }

    fun draw(
        scope: DrawScope,
        metrics: FontMetrics,
        glyphInspector: GlyphRenderInspector,
        glyphLayoutCache: GlyphLayoutCache,
        cellWidthPx: Float,
        baseColor: Color,
        headHighlightColor: Color,
        jitterPx: Float
    ) {
        if (columnCount == 0) return
        val ascent = metrics.ascent
        val rowHeight = metrics.rowHeight
        val visible = visibleRows
        columns.forEach { column ->
            val illumination = column.illumination
            val rng = illumination.rng
            val span = visible + illumination.periodCells.toInt() + tailLength
            val head = floor(illumination.phase * span).toInt()
            var alpha = 1f
            var offset = 0
            while (offset <= tailLength) {
                val row = head - offset
                if (row in 0 until visible) {
                    val index = (row).modPositive(ringRows)
                    val rawChar = column.chars[index]
                    val displayChar = glyphInspector.ensureRenderable(rawChar)
                    val layout = glyphLayoutCache.layoutFor(displayChar)
                    val color = if (offset <= 1) headHighlightColor else baseColor
                    val jitterX = if (jitterPx == 0f) 0f else (rng.nextFloat() - 0.5f) * 2f * jitterPx
                    val jitterY = if (jitterPx == 0f) 0f else (rng.nextFloat() - 0.5f) * 2f * jitterPx
                    val topLeft = Offset(
                        x = illumination.column * cellWidthPx + jitterX,
                        y = row * rowHeight - ascent + jitterY
                    )
                    scope.drawText(
                        textLayoutResult = layout,
                        color = color,
                        topLeft = topLeft,
                        alpha = alpha
                    )
                }
                alpha *= 0.86f
                if (alpha < 0.001f) break
                offset++
            }
        }
    }

    private fun fillInitial(
        chars: CharArray,
        rng: Random,
        glyphOrder: GlyphOrder,
        glyphPicker: GlyphPicker
    ) {
        for (index in chars.indices) {
            chars[index] = glyphPicker.initialGlyph(rng, glyphOrder)
        }
    }
}

private fun Int.modPositive(modulus: Int): Int {
    val result = this % modulus
    return if (result < 0) result + modulus else result
}

@Preview(name = "PreviewScroll")
@Composable
private fun PreviewDigitalRainScroll() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        DigitalRainBackground(
            modifier = Modifier.fillMaxSize(),
            mode = RainMode.Scroll,
            wrapMode = WrapMode.Circular,
            version = GlyphVersion.Classic
        )
    }
}

@Preview(name = "PreviewIllumination")
@Composable
private fun PreviewDigitalRainIllumination() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        DigitalRainBackground(
            modifier = Modifier.fillMaxSize(),
            mode = RainMode.Illumination,
            version = GlyphVersion.Resurrections
        )
    }
}

