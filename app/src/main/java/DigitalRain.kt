/**
 * デジタルレイン（The Matrix 風）を Jetpack Compose の Canvas で描画する背景コンポーネント。
 * `Box(modifier = Modifier.fillMaxSize().background(Color.Black)) { DigitalRainBackground(Modifier.fillMaxSize()) }`
 * のように任意の画面に敷いて利用する。各パラメータは降り方や密度、彩度などを制御するためのもの。
 *
 * @param modifier 背景に適用する Modifier。
 * @param version グリフ循環順（Classic/Resurrections）を切り替える。
 * @param columnWidthDp 列幅の基準（dp）。
 * @param fontSizeSp テキストサイズ（sp）。
 * @param baseColor 尾部の基調色。
 * @param backgroundColor 背景の塗りつぶし色。
 * @param headHighlightColor ヘッド 1〜2 文字の強調色。
 * @param densityScale 列密度のスケール。1.0 が基準。
 * @param minSpeedCps 各列の最小速度（cells per second）。
 * @param maxSpeedCps 各列の最大速度（cells per second）。
 * @param tailLengthCells テール長（セル数）。0 でヘッドのみ。
 * @param spawnChancePerSec 輝度波のリフレッシュ頻度（1 秒あたり）。
 * @param glyphMorphChancePerSec 1 秒あたりの文字モーフレート。
 * @param jitterPx 文字位置の微小ジッター（px）。0 で無効。
 * @param morphBias prev/next 遷移のバランス。0.2 なら 20% が prev、80% が next。
 * @param seed 0 で現在時刻、非 0 なら決定論的。
 * @param enabled true でアニメーションを実行。
 *
 * 雨筋はイルミネーションモードのみを採用し、列ごとの輝度波が流れ続ける。
 * グリフ循環（glyph order）は映画の連続性を再現する固定配列で、モーフ時に前後へ遷移させる。
 * 含まれない文字はカテゴリ別（カタカナ/英数/記号）の重み付きランダムで補完し、欠字は '·' にフォールバック。
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
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

enum class GlyphVersion { Classic, Resurrections }

@Composable
fun DigitalRainBackground(
    modifier: Modifier = Modifier,
    version: GlyphVersion = GlyphVersion.Classic,
    columnWidthDp: Dp = 16.dp,
    fontSizeSp: TextUnit = 16.sp,
    baseColor: Color = Color(0xFF00FF41),
    backgroundColor: Color = Color(0xFF000000),
    headHighlightColor: Color = Color(0xFFE0FFE0),
    densityScale: Float = 1.0f,
    minSpeedCps: Float = 6f,
    maxSpeedCps: Float = 12f,
    tailLengthCells: Int = 18,
    spawnChancePerSec: Float = 0.8f,
    glyphMorphChancePerSec: Float = 1.5f,
    jitterPx: Float = 0f,
    morphBias: Float = 0.2f,
    seed: Long = 0L,
    enabled: Boolean = true
) {
    require(tailLengthCells >= 0)
    require(maxSpeedCps >= minSpeedCps)
    require(densityScale > 0f)
    require(morphBias in 0f..1f)
    val density = LocalDensity.current
    val columnWidthPx = with(density) { columnWidthDp.toPx() }
    val actualSeed = remember(seed) { if (seed != 0L) seed else System.currentTimeMillis() }
    val glyphOrder = remember(version) { GlyphOrder(version) }
    val fallbackPicker = remember { WeightedGlyphPicker() }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = remember(fontSizeSp) {
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = fontSizeSp,
            letterSpacing = 0.sp
        )
    }
    val baselineInfo = remember(fontSizeSp, textMeasurer) {
        val layout = textMeasurer.measure(AnnotatedString("A"), style = textStyle)
        val baseline = layout.getLineBaseline(0)
        val height = layout.size.height.toFloat()
        BaselineInfo(height = height, baseline = baseline)
    }
    val paint = remember(fontSizeSp, density) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = with(density) { fontSizeSp.toPx() }
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }
    val glyphAtlas = remember(textMeasurer, textStyle, paint) {
        GlyphAtlas(textMeasurer, textStyle, paint)
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val engine = remember { DigitalRainEngine() }
    val frameTimeState = remember { mutableLongStateOf(0L) }
    val latestEnabled by rememberUpdatedState(enabled)
    LaunchedEffect(latestEnabled) {
        if (!latestEnabled) return@LaunchedEffect
        while (latestEnabled) {
            withFrameNanos { now ->
                frameTimeState.longValue = now
            }
        }
    }
    val latestConfig = rememberUpdatedState(
        EngineConfig(
            order = glyphOrder,
            fallback = fallbackPicker,
            densityScale = densityScale,
            minSpeed = minSpeedCps,
            maxSpeed = maxSpeedCps,
            tailLength = tailLengthCells,
            spawnChance = spawnChancePerSec,
            morphChance = glyphMorphChancePerSec,
            jitter = jitterPx,
            morphBias = morphBias,
            columnWidthPx = columnWidthPx,
            baseColor = baseColor,
            headColor = headHighlightColor,
            backgroundColor = backgroundColor,
            seed = actualSeed
        )
    )
    Canvas(
        modifier = modifier
            .onSizeChanged { size -> canvasSize = size }
    ) {
        val config = latestConfig.value
        engine.ensureLayout(
            size = canvasSize,
            baselineInfo = baselineInfo,
            config = config
        )
        engine.draw(
            scope = this,
            frameTimeNs = frameTimeState.longValue,
            glyphAtlas = glyphAtlas,
            baselineInfo = baselineInfo,
            config = config
        )
    }
}

private data class BaselineInfo(val height: Float, val baseline: Float)

private data class EngineConfig(
    val order: GlyphOrder,
    val fallback: WeightedGlyphPicker,
    val densityScale: Float,
    val minSpeed: Float,
    val maxSpeed: Float,
    val tailLength: Int,
    val spawnChance: Float,
    val morphChance: Float,
    val jitter: Float,
    val morphBias: Float,
    val columnWidthPx: Float,
    val baseColor: Color,
    val headColor: Color,
    val backgroundColor: Color,
    val seed: Long
)

private class DigitalRainEngine {
    private var layoutSpec: LayoutSpec? = null
    private var illuminationColumns: Array<IlluminationColumn> = emptyArray()
    private var previousFrameTime: Long = 0L
    private var alphaLut: FloatArray = floatArrayOf(1f)

    fun ensureLayout(size: IntSize, baselineInfo: BaselineInfo, config: EngineConfig) {
        val rows = max(1, ceil(size.height / baselineInfo.height).toInt())
        val baseColumns = max(1, floor(size.width / config.columnWidthPx).toInt())
        val scaledColumns = max(1, (baseColumns * config.densityScale).roundToInt())
        val spec = LayoutSpec(
            width = size.width,
            height = size.height,
            rows = rows,
            columns = scaledColumns,
            configHash = config.hash()
        )
        if (layoutSpec == spec) return
        layoutSpec = spec
        previousFrameTime = 0L
        prepareAlphaLut(config.tailLength)
        illuminationColumns = Array(spec.columns) { index ->
            createIlluminationColumn(index, spec.rows, config)
        }
    }

    fun draw(
        scope: DrawScope,
        frameTimeNs: Long,
        glyphAtlas: GlyphAtlas,
        baselineInfo: BaselineInfo,
        config: EngineConfig
    ) {
        val spec = layoutSpec ?: return
        val deltaNs = if (previousFrameTime == 0L) 0L else frameTimeNs - previousFrameTime
        previousFrameTime = frameTimeNs
        val deltaSec = deltaNs.coerceAtLeast(0L) / 1_000_000_000f
        scope.drawRect(config.backgroundColor)
        val columnSpacing = scope.size.width / spec.columns
        val spawnProbability = morphProbability(config.spawnChance, deltaSec)
        for (column in illuminationColumns) {
            column.advance(deltaSec)
            column.tryMorph(frameTimeNs, deltaSec, config)
            if (column.rng.nextFloat() < spawnProbability) {
                column.randomizeWave(config)
            }
            val head = column.headPosition(spec.rows)
            val total = spec.rows + column.periodCells
            val baselineOffset = baselineInfo.baseline
            val jitterRange = config.jitter
            for (row in 0 until spec.rows) {
                val distance = wrapDistance(head - row, total)
                if (distance > config.tailLength) continue
                val ch = column.chars[row]
                val layout = glyphAtlas.layoutFor(ch)
                val alpha = tailAlpha(distance)
                val color = when {
                    distance < 1f -> config.headColor.copy(alpha = alpha)
                    distance < 2f -> config.headColor.copy(alpha = alpha * 0.9f)
                    else -> config.baseColor.copy(alpha = alpha)
                }
                val jitterY = if (jitterRange > 0f) (column.rng.nextFloat() - 0.5f) * 2f * jitterRange else 0f
                val jitterX = if (jitterRange > 0f) (column.rng.nextFloat() - 0.5f) * 2f * jitterRange else 0f
                val topLeft = Offset(
                    x = column.column * columnSpacing,
                    y = row * baselineInfo.height - baselineOffset + jitterY
                ) + Offset(jitterX, 0f)
                scope.drawText(layout, topLeft = topLeft, color = color)
            }
        }
    }

    private fun prepareAlphaLut(tailLength: Int) {
        val size = tailLength + 4
        val lut = if (alphaLut.size == size) alphaLut else FloatArray(size)
        for (i in lut.indices) {
            lut[i] = 0.86f.pow(i.toFloat())
        }
        alphaLut = lut
    }

    private fun tailAlpha(distance: Float): Float {
        val lower = distance.toInt()
        val fraction = distance - lower
        val base = alphaLut.getOrElse(lower) { 0.86f.pow(distance) }
        val next = alphaLut.getOrElse(lower + 1) { 0.86f.pow(distance + 1f) }
        return base + (next - base) * fraction
    }

    private fun morphProbability(rate: Float, deltaSec: Float): Float {
        if (deltaSec <= 0f || rate <= 0f) return 0f
        return 1f - exp(-rate * deltaSec)
    }

    private fun createIlluminationColumn(
        column: Int,
        rows: Int,
        config: EngineConfig
    ): IlluminationColumn {
        val rng = Random(config.seed + column)
        val chars = CharArray(rows)
        val lastMorph = LongArray(rows)
        for (row in 0 until rows) {
            val ch = config.initialGlyph(rng)
            chars[row] = ch
            lastMorph[row] = 0L
        }
        return IlluminationColumn(
            column = column,
            chars = chars,
            lastMorphNs = lastMorph,
            phase = rng.nextFloat(),
            periodCells = max(1, config.tailLength).toFloat(),
            speedCps = randomSpeed(config, rng),
            rng = rng
        ).apply {
            randomizeWave(config)
        }
    }

    private fun randomSpeed(config: EngineConfig, rng: Random): Float {
        val min = config.minSpeed
        val max = config.maxSpeed
        return min + (max - min) * rng.nextFloat()
    }

    private fun EngineConfig.initialGlyph(rng: Random): Char {
        return if (order.size > 0 && rng.nextFloat() < 0.6f) {
            order.sequence[rng.nextInt(order.size)]
        } else {
            fallback.randomGlyph(rng)
        }
    }

    private fun IlluminationColumn.advance(deltaSec: Float) {
        if (deltaSec <= 0f) return
        val deltaPhase = speedCps * deltaSec / periodCells
        phase = (phase + deltaPhase) % 1f
        if (phase < 0f) phase += 1f
    }

    private fun IlluminationColumn.tryMorph(frameTimeNs: Long, deltaSec: Float, config: EngineConfig) {
        if (config.morphChance <= 0f) return
        for (i in chars.indices) {
            val last = lastMorphNs[i]
            val elapsedSec = if (last == 0L) deltaSec else (frameTimeNs - last) / 1_000_000_000f
            if (elapsedSec <= 0f) continue
            val probability = morphProbability(config.morphChance, elapsedSec)
            if (probability <= 0f) continue
            if (rng.nextFloat() < probability) {
                chars[i] = config.morphGlyph(chars[i], rng)
                lastMorphNs[i] = frameTimeNs
            }
        }
    }

    private fun EngineConfig.morphGlyph(current: Char, rng: Random): Char {
        val target = if (rng.nextFloat() < morphBias) order.prevOf(current) else order.nextOf(current)
        return target ?: fallback.randomGlyph(rng)
    }

    private fun IlluminationColumn.randomizeWave(config: EngineConfig) {
        periodCells = max(1, config.tailLength) * (0.8f + rng.nextFloat() * 0.6f)
        speedCps = randomSpeed(config, rng)
        phase = rng.nextFloat()
    }

    private fun IlluminationColumn.headPosition(rows: Int): Float {
        return phase * (rows + periodCells)
    }

    private fun EngineConfig.hash(): Int {
        var result = order.hashCode()
        result = 31 * result + densityScale.hashCode()
        result = 31 * result + minSpeed.hashCode()
        result = 31 * result + maxSpeed.hashCode()
        result = 31 * result + tailLength
        result = 31 * result + spawnChance.hashCode()
        result = 31 * result + morphChance.hashCode()
        result = 31 * result + jitter.hashCode()
        result = 31 * result + morphBias.hashCode()
        result = 31 * result + columnWidthPx.hashCode()
        result = 31 * result + baseColor.hashCode()
        result = 31 * result + headColor.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + seed.hashCode()
        return result
    }
}

private data class LayoutSpec(
    val width: Int,
    val height: Int,
    val rows: Int,
    val columns: Int,
    val configHash: Int
)

private data class IlluminationColumn(
    val column: Int,
    val chars: CharArray,
    val lastMorphNs: LongArray,
    var phase: Float,
    var periodCells: Float,
    var speedCps: Float,
    val rng: Random
)

private class GlyphOrder(version: GlyphVersion) {
    val sequence: CharArray
    val size: Int get() = sequence.size
    private val indexLookup: IntArray = IntArray(1 shl 16) { -1 }

    init {
        val raw = when (version) {
            GlyphVersion.Classic -> """モエヤキオカ7ケサスz152ヨタワ4ネヌナ98ヒ0ホア3ウセ¦:"꞊ミラリ╌ツテニハソ▪—<>0|+*コシマムメ"""
            GlyphVersion.Resurrections -> """モエヤキオカ7ケサスz152ヨタワ4ネヌナ98ヒ0ホア3ウセ¦:"꞊ミラリ╌ツテニハソコ—<ム0|*▪メシマ>+"""
        }
        val cleaned = raw.filterNot { it.isWhitespace() }
        sequence = cleaned.toCharArray()
        for (i in sequence.indices) {
            val code = sequence[i].code
            if (code < indexLookup.size) {
                indexLookup[code] = i
            }
        }
    }

    fun nextOf(ch: Char): Char? {
        if (sequence.isEmpty()) return null
        val index = indexOf(ch)
        if (index < 0) return null
        val nextIndex = (index + 1) % sequence.size
        return sequence[nextIndex]
    }

    fun prevOf(ch: Char): Char? {
        if (sequence.isEmpty()) return null
        val index = indexOf(ch)
        if (index < 0) return null
        val nextIndex = (index - 1).mod(sequence.size)
        return sequence[nextIndex]
    }

    private fun Int.mod(divisor: Int): Int {
        var result = this % divisor
        if (result < 0) result += divisor
        return result
    }

    private fun indexOf(ch: Char): Int {
        val code = ch.code
        return if (code < indexLookup.size) indexLookup[code] else -1
    }
}

private class GlyphAtlas(
    private val textMeasurer: TextMeasurer,
    private val textStyle: TextStyle,
    private val paint: android.graphics.Paint
) {
    private val layouts = HashMap<Char, TextLayoutResult>()
    private val renderable = HashMap<Char, Boolean>()
    private val buffer = CharArray(1)

    init {
        ensureGlyph('·')
    }

    fun layoutFor(ch: Char): TextLayoutResult {
        if (!renderable.getOrElse(ch) { ensureGlyph(ch) }) {
            return layouts.getValue('·')
        }
        return layouts.getValue(ch)
    }

    private fun ensureGlyph(ch: Char): Boolean {
        val layout = measure(ch)
        layouts[ch] = layout
        val glyphString = buffer.concatToString()
        val hasGlyph = paint.hasGlyph(glyphString)
        renderable[ch] = hasGlyph
        return hasGlyph
    }

    private fun measure(ch: Char): TextLayoutResult {
        buffer[0] = ch
        return textMeasurer.measure(
            text = AnnotatedString(buffer.concatToString()),
            style = textStyle
        )
    }
}

private class WeightedGlyphPicker {
    private val katakanaRanges = arrayOf(CharRange('\u30A1', '\u30FA'), CharRange('\u30FD', '\u30FF'))
    private val digitRange = '0'..'9'
    private val latinRange = 'A'..'Z'
    private val symbols = ".,:;+=-*><|"

    fun randomGlyph(rng: Random): Char {
        val choice = rng.nextFloat()
        return when {
            choice < 0.7f -> pickFromRanges(katakanaRanges, rng)
            choice < 0.82f -> pickFromRange(digitRange, rng)
            choice < 0.95f -> pickFromRange(latinRange, rng)
            else -> symbols[rng.nextInt(symbols.length)]
        }
    }

    private fun pickFromRanges(ranges: Array<CharRange>, rng: Random): Char {
        val totalLength = ranges.sumOf { it.count() }
        var target = rng.nextInt(totalLength)
        for (range in ranges) {
            val length = range.count()
            if (target < length) {
                return (range.first.code + target).toChar()
            }
            target -= length
        }
        return ranges.last().first
    }

    private fun pickFromRange(range: CharRange, rng: Random): Char {
        val offset = rng.nextInt(range.last.code - range.first.code + 1)
        return (range.first.code + offset).toChar()
    }
}

private fun wrapDistance(value: Float, total: Float): Float {
    if (total <= 0f) return value
    var result = value % total
    if (result < 0f) result += total
    return result
}

private fun CharRange.count(): Int = last.code - first.code + 1

@Preview(showBackground = true)
@Composable
private fun PreviewIlluminationClassic() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        DigitalRainBackground(
            modifier = Modifier.fillMaxSize(),
            version = GlyphVersion.Classic
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewIlluminationResurrections() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        DigitalRainBackground(
            modifier = Modifier.fillMaxSize(),
            version = GlyphVersion.Resurrections
        )
    }
}
