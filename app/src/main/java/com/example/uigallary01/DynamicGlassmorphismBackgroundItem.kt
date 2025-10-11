package com.example.uigallary01

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DynamicGlassmorphismBackgroundItem(
    modifier: Modifier = Modifier,
    state: DynamicGlassmorphismBackgroundState = rememberDynamicGlassmorphismBackgroundState(),
) {
    // タップで展開／折りたたみを切り替える
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) 840.dp else 160.dp,
        label = "dynamicGlassmorphismHeight",
    )
    // 色相（Hue）を 0〜360 度で保持し、展開時に変更できるようにする
    var hue by rememberSaveable { mutableFloatStateOf(210f) }
    // ブラー半径は dp 単位で扱い、0 〜 56dp の範囲で調整する
    var blurRadius by rememberSaveable { mutableFloatStateOf(32f) }
    // 丸の透明度を 40% 〜 100% の範囲で調整できるようにする
    var orbAlphaMultiplier by rememberSaveable { mutableFloatStateOf(0.8f) }
    // 丸のサイズを基準比率で調整できるようにする
    var orbSizeMultiplier by rememberSaveable { mutableFloatStateOf(1f) }
    // 丸の配色モード（1色／3色）を切り替えられるようにする
    var colorMode by rememberSaveable { mutableStateOf(DynamicGlassColorMode.Monochrome) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val palette = remember(hue, surfaceColor) {
        DynamicGlassmorphismPalette(hue = hue, baseSurface = surfaceColor)
    }

    DynamicGlassmorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .ensureVisibleOnExpand(isExpanded)
            .clip(RoundedCornerShape(24.dp))
            .clickable { isExpanded = !isExpanded },
        palette = palette,
        blurRadius = blurRadius.dp,
        orbAlphaMultiplier = orbAlphaMultiplier,
        orbSizeMultiplier = orbSizeMultiplier,
        colorMode = colorMode,
        state = state,
    ) {
        DynamicGlassmorphismOverlay(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            palette = palette,
            isExpanded = isExpanded,
            hue = hue,
            onHueChange = { hue = it },
            blurRadius = blurRadius,
            onBlurChange = { blurRadius = it },
            orbAlphaMultiplier = orbAlphaMultiplier,
            onAlphaMultiplierChange = { orbAlphaMultiplier = it },
            orbSizeMultiplier = orbSizeMultiplier,
            onSizeMultiplierChange = { orbSizeMultiplier = it },
            colorMode = colorMode,
            onColorModeChange = { colorMode = it },
        )
    }
}

@Composable
fun rememberDynamicGlassmorphismBackgroundState(
    largeCount: Int = 6,
    mediumCount: Int = 10,
    smallCount: Int = 16,
    seed: Int = 2025,
): DynamicGlassmorphismBackgroundState {
    // 大小様々な丸を生成し、常に同じ動きになるようシードを指定
    val orbs = remember(largeCount, mediumCount, smallCount, seed) {
        val random = Random(seed)
        buildList {
            addAll(generateOrbs(random, largeCount, OrbSize.Large))
            addAll(generateOrbs(random, mediumCount, OrbSize.Medium))
            addAll(generateOrbs(random, smallCount, OrbSize.Small))
        }
    }
    val elapsed = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(orbs) {
        var previousTime = 0L
        while (true) {
            withFrameNanos { time ->
                if (previousTime == 0L) {
                    previousTime = time
                    return@withFrameNanos
                }
                val delta = (time - previousTime) / 1_000_000_000f
                previousTime = time
                // 十分大きくなると桁落ちを避けるため 1000 秒単位で折り返す
                val next = elapsed.floatValue + delta
                elapsed.floatValue = if (next > 1000f) next - 1000f else next
            }
        }
    }

    return remember { DynamicGlassmorphismBackgroundState(orbs = orbs, elapsedSeconds = elapsed) }
}

@Stable
class DynamicGlassmorphismBackgroundState internal constructor(
    internal val orbs: List<GlassOrb>,
    internal val elapsedSeconds: State<Float>,
)

@Composable
private fun DynamicGlassmorphismSurface(
    modifier: Modifier,
    palette: DynamicGlassmorphismPalette,
    blurRadius: Dp,
    orbAlphaMultiplier: Float,
    orbSizeMultiplier: Float,
    colorMode: DynamicGlassColorMode,
    state: DynamicGlassmorphismBackgroundState,
    overlay: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        DynamicGlassmorphismCanvas(
            modifier = Modifier.fillMaxSize(),
            palette = palette,
            blurRadius = blurRadius,
            orbAlphaMultiplier = orbAlphaMultiplier,
            orbSizeMultiplier = orbSizeMultiplier,
            colorMode = colorMode,
            state = state,
        )
        overlay()
    }
}

@Composable
private fun DynamicGlassmorphismCanvas(
    modifier: Modifier,
    palette: DynamicGlassmorphismPalette,
    blurRadius: Dp,
    orbAlphaMultiplier: Float,
    orbSizeMultiplier: Float,
    colorMode: DynamicGlassColorMode,
    state: DynamicGlassmorphismBackgroundState,
) {
    val blurPx = with(LocalDensity.current) { blurRadius.toPx() }
    val blurMaskFilter = remember(blurPx) {
        if (blurPx <= 0.5f) {
            null
        } else {
            BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }

    Canvas(modifier = modifier) {
        drawRect(color = palette.background)
        val elapsed = state.elapsedSeconds.value
        val shortestSide = min(size.width, size.height)
        drawIntoCanvas { canvas ->
            val paint = androidx.compose.ui.graphics.Paint()
            paint.asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = blurMaskFilter
            }
            state.orbs.forEach { orb ->
                val primary = elapsed * orb.primarySpeed + orb.primaryPhase
                val secondary = elapsed * orb.secondarySpeed + orb.secondaryPhase
                val centerX = (orb.anchor.x + sin(primary) * orb.amplitude.x) * size.width
                val centerY = (orb.anchor.y + cos(secondary) * orb.amplitude.y) * size.height
                val color = palette.orbColor(
                    shift = orb.tintShift,
                    colorMode = colorMode,
                    band = orb.colorBand,
                )
                val adjustedAlpha = (orb.alpha * orbAlphaMultiplier).coerceIn(0f, 1f)
                paint.color = color.copy(alpha = adjustedAlpha)
                canvas.drawCircle(
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    radius = shortestSide * orb.radiusFraction * orbSizeMultiplier,
                    paint = paint,
                )
            }
        }
    }
}

@Composable
private fun DynamicGlassmorphismOverlay(
    modifier: Modifier,
    palette: DynamicGlassmorphismPalette,
    isExpanded: Boolean,
    hue: Float,
    onHueChange: (Float) -> Unit,
    blurRadius: Float,
    onBlurChange: (Float) -> Unit,
    orbAlphaMultiplier: Float,
    onAlphaMultiplierChange: (Float) -> Unit,
    orbSizeMultiplier: Float,
    onSizeMultiplierChange: (Float) -> Unit,
    colorMode: DynamicGlassColorMode,
    onColorModeChange: (DynamicGlassColorMode) -> Unit,
) {
    val arrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Bottom
    Column(
        modifier = modifier,
        verticalArrangement = arrangement,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Dynamic Glassmorphism",
            style = MaterialTheme.typography.headlineSmall,
            color = palette.titleColor,
        )
        AnimatedVisibility(visible = isExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                DynamicGlassmorphismColorModeSelector(
                    mode = colorMode,
                    onModeChange = onColorModeChange,
                )
                DynamicGlassmorphismSlider(
                    label = "Hue",
                    value = hue,
                    onValueChange = onHueChange,
                    valueRange = 0f..360f,
                    valueFormatter = { value ->
                        // 角度として表示する
                        "${value.toInt()}°"
                    },
                )
                DynamicGlassmorphismSlider(
                    label = "Blur",
                    value = blurRadius,
                    onValueChange = onBlurChange,
                    valueRange = 0f..56f,
                    valueFormatter = { value ->
                        "${value.toInt()}dp"
                    },
                )
                DynamicGlassmorphismSlider(
                    label = "Opacity",
                    value = orbAlphaMultiplier,
                    onValueChange = onAlphaMultiplierChange,
                    valueRange = 0.4f..1f,
                    valueFormatter = { value ->
                        "${(value * 100f).roundToInt()}%"
                    },
                )
                DynamicGlassmorphismSlider(
                    label = "Size",
                    value = orbSizeMultiplier,
                    onValueChange = onSizeMultiplierChange,
                    valueRange = 0.6f..1.4f,
                    valueFormatter = { value ->
                        "${(value * 100f).roundToInt()}%"
                    },
                )
            }
        }
    }
}

@Composable
private fun DynamicGlassmorphismSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                textAlign = TextAlign.End,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            ),
        )
    }
}

private enum class OrbSize { Large, Medium, Small }

internal data class GlassOrb(
    val anchor: androidx.compose.ui.geometry.Offset,
    val amplitude: androidx.compose.ui.geometry.Offset,
    val radiusFraction: Float,
    val alpha: Float,
    val tintShift: Float,
    val primarySpeed: Float,
    val secondarySpeed: Float,
    val primaryPhase: Float,
    val secondaryPhase: Float,
    val colorBand: Int,
)

private fun generateOrbs(
    random: Random,
    count: Int,
    size: OrbSize,
): List<GlassOrb> {
    if (count == 0) return emptyList()
    val (radiusRange, speedRange, amplitudeRange, alphaRange) = when (size) {
        OrbSize.Large -> OrbRangeBundle(
            radius = 0.18f..0.26f,
            speed = 0.08f..0.16f,
            amplitude = 0.10f..0.18f,
            alpha = 0.28f..0.42f,
        )
        OrbSize.Medium -> OrbRangeBundle(
            radius = 0.10f..0.16f,
            speed = 0.12f..0.24f,
            amplitude = 0.12f..0.22f,
            alpha = 0.32f..0.52f,
        )
        OrbSize.Small -> OrbRangeBundle(
            radius = 0.04f..0.08f,
            speed = 0.18f..0.36f,
            amplitude = 0.18f..0.32f,
            alpha = 0.38f..0.68f,
        )
    }
    return List(count) {
        val anchor = androidx.compose.ui.geometry.Offset(random.nextFloat(), random.nextFloat())
        val amplitudeValue = lerp(amplitudeRange.start, amplitudeRange.endInclusive, random.nextFloat())
        GlassOrb(
            anchor = anchor,
            amplitude = androidx.compose.ui.geometry.Offset(
                x = amplitudeValue * random.nextFloat(),
                y = amplitudeValue * random.nextFloat(),
            ),
            radiusFraction = lerp(radiusRange.start, radiusRange.endInclusive, random.nextFloat()),
            alpha = lerp(alphaRange.start, alphaRange.endInclusive, random.nextFloat()),
            tintShift = lerp(-0.12f, 0.18f, random.nextFloat()),
            primarySpeed = lerp(speedRange.start, speedRange.endInclusive, random.nextFloat()),
            secondarySpeed = lerp(speedRange.start, speedRange.endInclusive, random.nextFloat()),
            primaryPhase = lerp(0f, (2f * PI).toFloat(), random.nextFloat()),
            secondaryPhase = lerp(0f, (2f * PI).toFloat(), random.nextFloat()),
            colorBand = random.nextInt(3),
        )
    }
}

private data class OrbRangeBundle(
    val radius: ClosedFloatingPointRange<Float>,
    val speed: ClosedFloatingPointRange<Float>,
    val amplitude: ClosedFloatingPointRange<Float>,
    val alpha: ClosedFloatingPointRange<Float>,
)

private class DynamicGlassmorphismPalette(
    hue: Float,
    private val baseSurface: Color,
) {
    // 色相から丸の色を計算し、背景にはテーマのサーフェスカラーを使うパレット
    private val normalizedHue = hue.wrapHue()
    private val baseSaturation = 0.58f
    private val baseLightness = 0.58f
    private val triadicHues: List<Float> = listOf(0f, 18f, -18f).map { offset ->
        (normalizedHue + offset).wrapHue()
    }

    val background: Color = baseSurface
    val titleColor: Color = Color.hsl(
        hue = normalizedHue,
        saturation = 0.40f,
        lightness = 0.28f,
    )

    fun orbColor(shift: Float, colorMode: DynamicGlassColorMode, band: Int): Color {
        val baseHue = when (colorMode) {
            DynamicGlassColorMode.Monochrome -> normalizedHue
            DynamicGlassColorMode.Tricolor -> triadicHues[band % triadicHues.size]
        }
        val saturation = (baseSaturation + shift * 0.25f).coerceIn(0.1f, 0.9f)
        val lightness = (baseLightness + shift * 0.45f).coerceIn(0.2f, 0.95f)
        return Color.hsl(
            hue = baseHue,
            saturation = saturation,
            lightness = lightness,
        )
    }
}

private fun Float.wrapHue(): Float {
    val raw = this % 360f
    return if (raw < 0f) raw + 360f else raw
}

private enum class DynamicGlassColorMode {
    Monochrome,
    Tricolor,
}

@Composable
private fun DynamicGlassmorphismColorModeSelector(
    mode: DynamicGlassColorMode,
    onModeChange: (DynamicGlassColorMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Color Mode",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // 一色／三色を選択するチップを横並びに配置
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DynamicGlassmorphismModeChip(
                label = "1 Color",
                selected = mode == DynamicGlassColorMode.Monochrome,
                onClick = { onModeChange(DynamicGlassColorMode.Monochrome) },
            )
            DynamicGlassmorphismModeChip(
                label = "3 Colors",
                selected = mode == DynamicGlassColorMode.Tricolor,
                onClick = { onModeChange(DynamicGlassColorMode.Tricolor) },
            )
        }
    }
}

@Composable
private fun DynamicGlassmorphismModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}
