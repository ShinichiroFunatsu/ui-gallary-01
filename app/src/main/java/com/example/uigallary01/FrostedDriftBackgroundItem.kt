package com.example.uigallary01

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
import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FrostedDriftBackgroundItem(
    modifier: Modifier = Modifier,
    state: FrostedDriftBackgroundState = rememberFrostedDriftBackgroundState(),
) {
    // タップで展開／折りたたみを切り替える
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) 360.dp else 160.dp,
        label = "frostedDriftHeight",
    )
    // 色相（Hue）を 0〜360 度で保持し、展開時に変更できるようにする
    var hue by rememberSaveable { mutableFloatStateOf(210f) }
    // ブラー半径は dp 単位で扱い、0 〜 56dp の範囲で調整する
    var blurRadius by rememberSaveable { mutableFloatStateOf(32f) }
    // 丸の透明度を 40% 〜 100% の範囲で調整できるようにする
    var orbAlphaMultiplier by rememberSaveable { mutableFloatStateOf(0.8f) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val palette = remember(hue, surfaceColor) {
        FrostedDriftPalette(hue = hue, baseSurface = surfaceColor)
    }

    FrostedDriftSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .ensureVisibleOnExpand(isExpanded)
            .clip(RoundedCornerShape(24.dp))
            .clickable { isExpanded = !isExpanded },
        palette = palette,
        blurRadius = blurRadius.dp,
        orbAlphaMultiplier = orbAlphaMultiplier,
        state = state,
    ) {
        FrostedDriftOverlay(
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
        )
    }
}

@Composable
fun rememberFrostedDriftBackgroundState(
    largeCount: Int = 6,
    mediumCount: Int = 10,
    smallCount: Int = 16,
    seed: Int = 2025,
): FrostedDriftBackgroundState {
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

    return remember { FrostedDriftBackgroundState(orbs = orbs, elapsedSeconds = elapsed) }
}

@Stable
class FrostedDriftBackgroundState internal constructor(
    internal val orbs: List<FrostedOrb>,
    internal val elapsedSeconds: State<Float>,
)

@Composable
private fun FrostedDriftSurface(
    modifier: Modifier,
    palette: FrostedDriftPalette,
    blurRadius: Dp,
    orbAlphaMultiplier: Float,
    state: FrostedDriftBackgroundState,
    overlay: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        FrostedDriftCanvas(
            modifier = Modifier.fillMaxSize(),
            palette = palette,
            blurRadius = blurRadius,
            orbAlphaMultiplier = orbAlphaMultiplier,
            state = state,
        )
        overlay()
    }
}

@Composable
private fun FrostedDriftCanvas(
    modifier: Modifier,
    palette: FrostedDriftPalette,
    blurRadius: Dp,
    orbAlphaMultiplier: Float,
    state: FrostedDriftBackgroundState,
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
                val color = palette.orbColor(orb.tintShift)
                val adjustedAlpha = (orb.alpha * orbAlphaMultiplier).coerceIn(0f, 1f)
                paint.color = color.copy(alpha = adjustedAlpha)
                canvas.drawCircle(
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    radius = shortestSide * orb.radiusFraction,
                    paint = paint,
                )
            }
        }
    }
}

@Composable
private fun FrostedDriftOverlay(
    modifier: Modifier,
    palette: FrostedDriftPalette,
    isExpanded: Boolean,
    hue: Float,
    onHueChange: (Float) -> Unit,
    blurRadius: Float,
    onBlurChange: (Float) -> Unit,
    orbAlphaMultiplier: Float,
    onAlphaMultiplierChange: (Float) -> Unit,
) {
    val arrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Bottom
    Column(
        modifier = modifier,
        verticalArrangement = arrangement,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Frosted Drift",
            style = MaterialTheme.typography.headlineSmall,
            color = palette.titleColor,
        )
        AnimatedVisibility(visible = isExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                FrostedDriftSlider(
                    label = "Hue",
                    value = hue,
                    onValueChange = onHueChange,
                    valueRange = 0f..360f,
                    valueFormatter = { value ->
                        // 角度として表示する
                        "${value.toInt()}°"
                    },
                )
                FrostedDriftSlider(
                    label = "Blur",
                    value = blurRadius,
                    onValueChange = onBlurChange,
                    valueRange = 0f..56f,
                    valueFormatter = { value ->
                        "${value.toInt()}dp"
                    },
                )
                FrostedDriftSlider(
                    label = "Opacity",
                    value = orbAlphaMultiplier,
                    onValueChange = onAlphaMultiplierChange,
                    valueRange = 0.4f..1f,
                    valueFormatter = { value ->
                        "${(value * 100f).roundToInt()}%"
                    },
                )
            }
        }
    }
}

@Composable
private fun FrostedDriftSlider(
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

internal data class FrostedOrb(
    val anchor: androidx.compose.ui.geometry.Offset,
    val amplitude: androidx.compose.ui.geometry.Offset,
    val radiusFraction: Float,
    val alpha: Float,
    val tintShift: Float,
    val primarySpeed: Float,
    val secondarySpeed: Float,
    val primaryPhase: Float,
    val secondaryPhase: Float,
)

private fun generateOrbs(
    random: Random,
    count: Int,
    size: OrbSize,
): List<FrostedOrb> {
    if (count == 0) return emptyList()
    val (radiusRange, speedRange, amplitudeRange, alphaRange) = when (size) {
        OrbSize.Large -> RangeBundle(
            radius = 0.18f..0.26f,
            speed = 0.08f..0.16f,
            amplitude = 0.10f..0.18f,
            alpha = 0.28f..0.42f,
        )
        OrbSize.Medium -> RangeBundle(
            radius = 0.10f..0.16f,
            speed = 0.12f..0.24f,
            amplitude = 0.12f..0.22f,
            alpha = 0.32f..0.52f,
        )
        OrbSize.Small -> RangeBundle(
            radius = 0.04f..0.08f,
            speed = 0.18f..0.36f,
            amplitude = 0.18f..0.32f,
            alpha = 0.38f..0.68f,
        )
    }
    return List(count) {
        val anchor = androidx.compose.ui.geometry.Offset(random.nextFloat(), random.nextFloat())
        val amplitudeValue = lerp(amplitudeRange.start, amplitudeRange.endInclusive, random.nextFloat())
        FrostedOrb(
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
        )
    }
}

private data class RangeBundle(
    val radius: ClosedFloatingPointRange<Float>,
    val speed: ClosedFloatingPointRange<Float>,
    val amplitude: ClosedFloatingPointRange<Float>,
    val alpha: ClosedFloatingPointRange<Float>,
)

private class FrostedDriftPalette(
    hue: Float,
    private val baseSurface: Color,
) {
    // 色相から丸の色を計算し、背景にはテーマのサーフェスカラーを使うパレット
    private val normalizedHue = hue % 360f
    private val baseSaturation = 0.58f
    private val baseLightness = 0.58f

    val background: Color = baseSurface
    val titleColor: Color = Color.hsl(
        hue = normalizedHue,
        saturation = 0.40f,
        lightness = 0.28f,
    )

    fun orbColor(shift: Float): Color {
        val saturation = (baseSaturation + shift * 0.25f).coerceIn(0.1f, 0.9f)
        val lightness = (baseLightness + shift * 0.45f).coerceIn(0.2f, 0.95f)
        return Color.hsl(
            hue = normalizedHue,
            saturation = saturation,
            lightness = lightness,
        )
    }
}
