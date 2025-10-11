package com.example.uigallary01

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
    val elapsedSeconds by state.elapsedSeconds
    // 時間経過で滑らかに変化させる各種パラメーターを計算
    val hue = HueRange.oscillate(elapsedSeconds, HuePeriodSeconds)
    val blurRadius = BlurRange.oscillate(elapsedSeconds, BlurPeriodSeconds)
    val orbAlphaMultiplier = OpacityRange.oscillate(elapsedSeconds, OpacityPeriodSeconds)
    val orbSizeMultiplier = SizeRange.oscillate(elapsedSeconds, SizePeriodSeconds)

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
        state = state,
    ) {
        DynamicGlassmorphismOverlay(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            palette = palette,
            isExpanded = isExpanded,
            hue = hue,
            blurRadius = blurRadius,
            orbAlphaMultiplier = orbAlphaMultiplier,
            orbSizeMultiplier = orbSizeMultiplier,
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
    blurRadius: Float,
    orbAlphaMultiplier: Float,
    orbSizeMultiplier: Float,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start,
    ) {
        AnimatedVisibility(visible = isExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                DynamicGlassmorphismReadout(
                    label = "Hue",
                    value = "${hue.toInt()}°",
                )
                DynamicGlassmorphismReadout(
                    label = "Blur",
                    value = "${blurRadius.toInt()}dp",
                )
                DynamicGlassmorphismReadout(
                    label = "Opacity",
                    value = "${(orbAlphaMultiplier * 100f).roundToInt()}%",
                )
                DynamicGlassmorphismReadout(
                    label = "Size",
                    value = "${(orbSizeMultiplier * 100f).roundToInt()}%",
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        ScrambleText(
            text = "Dynamic Glassmorphism",
            style = MaterialTheme.typography.headlineSmall,
            color = palette.titleColor,
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

    fun orbColor(shift: Float, band: Int): Color {
        val baseHue = triadicHues[band % triadicHues.size]
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

@Composable
private fun DynamicGlassmorphismReadout(
    label: String,
    value: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

// 各パラメーターが往復する範囲と周期を定義
private val HueRange = 0f..360f
private val BlurRange = 10f..56f
private val OpacityRange = 0.4f..1f
private val SizeRange = 0.6f..1.4f

private const val HuePeriodSeconds = 48f
private const val BlurPeriodSeconds = 36f
private const val OpacityPeriodSeconds = 44f
private const val SizePeriodSeconds = 52f

// 範囲と周期からサイン波で滑らかに往復する値を算出
private fun ClosedFloatingPointRange<Float>.oscillate(
    elapsedSeconds: Float,
    periodSeconds: Float,
    phaseOffset: Float = 0f,
): Float {
    require(periodSeconds > 0f)
    val center = (start + endInclusive) / 2f
    val amplitude = (endInclusive - start) / 2f
    val angularFrequency = (2f * PI.toFloat()) / periodSeconds
    return center + amplitude * sin(angularFrequency * elapsedSeconds + phaseOffset)
}
