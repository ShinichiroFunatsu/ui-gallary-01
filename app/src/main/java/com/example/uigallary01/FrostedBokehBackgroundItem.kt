package com.example.uigallary01

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FrostedBokehBackgroundItem(
    modifier: Modifier = Modifier,
    state: FrostedBokehBackgroundState = rememberFrostedBokehBackgroundState(),
) {
    // タップで詳細 UI の表示状態を切り替える
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    // ブラーの強さを dp 単位で保持
    var blurRadius by rememberSaveable { mutableFloatStateOf(DefaultBlurRadius.value) }
    // 選択中の色を ARGB で保持し、Color に変換
    var tintArgb by rememberSaveable { mutableIntStateOf(DefaultBokehColor.toArgb()) }
    val tintColor = remember(tintArgb) { Color(tintArgb) }

    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) 360.dp else 160.dp,
        label = "frostedBokehHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .ensureVisibleOnExpand(isExpanded)
            .clip(RoundedCornerShape(24.dp))
            .clickable { isExpanded = !isExpanded },
    ) {
        FrostedBokehBackground(
            modifier = Modifier.matchParentSize(),
            state = state,
            tintColor = tintColor,
            blurRadius = blurRadius.dp,
        )

        FrostedGlassOverlay(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            isExpanded = isExpanded,
            blurRadius = blurRadius,
            onBlurChange = { blurRadius = it },
            tintColor = tintColor,
            onTintSelected = { tintArgb = it.toArgb() },
        )
    }
}

@Composable
fun rememberFrostedBokehBackgroundState(): FrostedBokehBackgroundState {
    // 背景の粒子情報を生成して使い回す
    val particles = remember { createBokehField(count = 36) }
    val progress = rememberInfiniteTransition(label = "bokehDrift").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bokehProgress",
    )
    return remember { FrostedBokehBackgroundState(particles = particles, progress = progress) }
}

@Stable
class FrostedBokehBackgroundState internal constructor(
    internal val particles: List<BokehParticle>,
    internal val progress: State<Float>,
)

@Composable
private fun FrostedBokehBackground(
    modifier: Modifier,
    state: FrostedBokehBackgroundState,
    tintColor: Color,
    blurRadius: Dp,
) {
    // 背景のノイズ感を足すために描画内容に微かな暗部を足す
    Canvas(
        modifier = modifier.drawWithContent {
            drawRect(color = Color.Black.copy(alpha = 0.08f))
            drawContent()
        }
    ) {
        drawBokehParticles(
            drawScope = this,
            particles = state.particles,
            tintColor = tintColor,
            blurRadius = blurRadius,
            progress = state.progress.value,
        )
    }
}

@Composable
private fun FrostedGlassOverlay(
    modifier: Modifier,
    isExpanded: Boolean,
    blurRadius: Float,
    onBlurChange: (Float) -> Unit,
    tintColor: Color,
    onTintSelected: (Color) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // 縮小表示時はラベルのみを表示
        Text(
            text = "Frosted Drift Bokeh",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.9f),
        )

        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 色を切り替える丸型セレクター
                Text(
                    text = "Palette",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FrostedBokehColorOptions.forEach { option ->
                        val isSelected = option == tintColor
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 32.dp else 28.dp)
                                .clip(CircleShape)
                                .background(option.copy(alpha = 0.75f))
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    shape = CircleShape,
                                )
                                .clickable { onTintSelected(option) },
                        )
                    }
                }

                // ブラーの強さを調整するスライダー
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Blur Strength: ${blurRadius.toInt()}dp",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Slider(
                        value = blurRadius,
                        onValueChange = onBlurChange,
                        valueRange = 8f..48f,
                    )
                }
            }
        }
    }
}

private fun drawBokehParticles(
    drawScope: DrawScope,
    particles: List<BokehParticle>,
    tintColor: Color,
    blurRadius: Dp,
    progress: Float,
) {
    with(drawScope) {
        val blurPx = blurRadius.toPx()
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint().apply {
            style = android.graphics.Paint.Style.FILL
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }

        drawIntoCanvas { canvas ->
            particles.forEach { particle ->
                val animatedProgress = (progress * particle.speedMultiplier + particle.phase).wrapToUnit()
                val center = particle.center(animatedProgress, size.width, size.height)
                val radius = particle.radiusDp.dp.toPx()
                val color = tintColor.copy(alpha = particle.alpha)
                paint.color = color
                frameworkPaint.color = color.toArgb()
                canvas.drawCircle(center, radius, paint)
            }
        }
    }
}

private fun Float.wrapToUnit(): Float {
    val wrapped = this % 1f
    return if (wrapped < 0f) wrapped + 1f else wrapped
}

private fun BokehParticle.center(progress: Float, canvasWidth: Float, canvasHeight: Float) =
    Offset(
        x = ((anchor.x + sin(progress * 2f * PI).toFloat() * horizontalAmplitude).wrapToUnit()) * canvasWidth,
        y = ((anchor.y + cos(progress * 2f * PI).toFloat() * verticalAmplitude).wrapToUnit()) * canvasHeight,
    )

internal data class BokehParticle(
    val anchor: Offset,
    val horizontalAmplitude: Float,
    val verticalAmplitude: Float,
    val radiusDp: Float,
    val speedMultiplier: Float,
    val phase: Float,
    val alpha: Float,
)

private fun createBokehField(count: Int): List<BokehParticle> {
    val random = Random(2025)
    return List(count) {
        val sizeRoll = random.nextFloat()
        val (radius, alpha) = when {
            sizeRoll < 0.2f -> random.lerpRadiusAlpha(42f, 58f, 0.35f, 0.55f)
            sizeRoll < 0.6f -> random.lerpRadiusAlpha(28f, 40f, 0.25f, 0.45f)
            else -> random.lerpRadiusAlpha(16f, 26f, 0.18f, 0.32f)
        }
        BokehParticle(
            anchor = Offset(random.nextFloat(), random.nextFloat()),
            horizontalAmplitude = random.nextFloat().lerp(0.04f, 0.18f),
            verticalAmplitude = random.nextFloat().lerp(0.03f, 0.22f),
            radiusDp = radius,
            speedMultiplier = random.nextFloat().lerp(0.25f, 0.75f),
            phase = random.nextFloat(),
            alpha = alpha,
        )
    }
}

private fun Random.lerpRadiusAlpha(
    minRadius: Float,
    maxRadius: Float,
    minAlpha: Float,
    maxAlpha: Float,
): Pair<Float, Float> {
    val radius = nextFloat().lerp(minRadius, maxRadius)
    val alpha = nextFloat().lerp(minAlpha, maxAlpha)
    return radius to alpha
}

private fun Float.lerp(start: Float, end: Float): Float = start + (end - start) * this

private val DefaultBokehColor = Color(0xFF7FC8FF)
private val DefaultBlurRadius = 24.dp
private val FrostedBokehColorOptions = listOf(
    Color(0xFF7FC8FF),
    Color(0xFF9C89FF),
    Color(0xFFFF92D9),
    Color(0xFFFFC27A),
)

