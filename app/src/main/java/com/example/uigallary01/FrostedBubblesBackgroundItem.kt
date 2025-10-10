package com.example.uigallary01

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FrostedBubblesBackgroundItem(
    modifier: Modifier = Modifier,
    state: FrostedBubblesBackgroundState = rememberFrostedBubblesBackgroundState(),
) {
    // タップで展開/折りたたみを切り替える
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val backgroundHeight by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else 140.dp,
        label = "frostedBackgroundHeight",
    )
    var hue by rememberSaveable { mutableFloatStateOf(210f) }
    var blurRadiusDp by rememberSaveable { mutableFloatStateOf(28f) }

    val accentColor = remember(hue) {
        // 彩度と輝度を固定し、色相のみをスライダーで調整できるようにする
        Color.hsl(hue, 0.55f, 0.58f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .ensureVisibleOnExpand(isExpanded),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(backgroundHeight)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { isExpanded = !isExpanded }
            ) {
                FrostedBubblesBackground(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    accentColor = accentColor,
                    blurRadiusDp = blurRadiusDp,
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Frosted Drift",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                FrostedBubbleControls(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    hue = hue,
                    onHueChange = { hue = it },
                    blurRadiusDp = blurRadiusDp,
                    onBlurChange = { blurRadiusDp = it },
                )
            }
        }
    }
}

@Composable
fun rememberFrostedBubblesBackgroundState(
    bubbleCount: Int = 36,
    seed: Int = 2025,
): FrostedBubblesBackgroundState {
    val bubbles = remember(bubbleCount, seed) {
        val random = Random(seed)
        List(bubbleCount) {
            FrostedBubble(
                originX = random.nextFloat(),
                originY = random.nextFloat(),
                radiusFactor = lerp(0.08f, 0.22f, random.nextFloat()),
                driftFactor = lerp(0.015f, 0.05f, random.nextFloat()),
                speed = lerp(0.05f, 0.16f, random.nextFloat()),
                orbitRatio = lerp(0.6f, 1.4f, random.nextFloat()),
                phase = random.nextFloat(),
                depth = lerp(0.25f, 1f, random.nextFloat()),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "frostedDrift")
    val loopProgress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
        ),
        label = "frostedProgress",
    )
    val continuousProgress = loopProgress.rememberContinuousProgress()

    return remember {
        FrostedBubblesBackgroundState(
            bubbles = bubbles,
            progress = continuousProgress,
        )
    }
}

@Stable
class FrostedBubblesBackgroundState internal constructor(
    internal val bubbles: List<FrostedBubble>,
    internal val progress: State<Float>,
)

@Composable
private fun FrostedBubblesBackground(
    modifier: Modifier,
    state: FrostedBubblesBackgroundState,
    accentColor: Color,
    blurRadiusDp: Float,
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = blurRadiusDp.dp)
        ) {
            drawRect(color = Color(0xFF0A1222))
            drawFrostedBubbles(
                bubbles = state.bubbles,
                progress = state.progress.value,
                accentColor = accentColor,
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.06f))
        )
    }
}

@Composable
private fun FrostedBubbleControls(
    modifier: Modifier,
    hue: Float,
    onHueChange: (Float) -> Unit,
    blurRadiusDp: Float,
    onBlurChange: (Float) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ControlSection(
            title = "色相",
            valueText = "${hue.toInt()}°",
        ) {
            Slider(
                value = hue,
                onValueChange = onHueChange,
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.9f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                )
            )
        }
        ControlSection(
            title = "ブラー", 
            valueText = "${blurRadiusDp.toInt()}dp",
        ) {
            Slider(
                value = blurRadiusDp,
                onValueChange = onBlurChange,
                valueRange = 8f..48f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.9f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                )
            )
        }
        Text(
            text = "背後で漂う光の粒が柔らかい色でにじみます。\n好みの色合いとブラーで雰囲気を整えてください。",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            lineHeight = 18.sp,
        )
        TextButton(
            onClick = {
                onHueChange(210f)
                onBlurChange(28f)
            }
        ) {
            Text(text = "デフォルトに戻す", color = Color.White)
        }
    }
}

@Composable
private fun ControlSection(
    title: String,
    valueText: String,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RowHeader(title = title, valueText = valueText)
        content()
    }
}

@Composable
private fun RowHeader(
    title: String,
    valueText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.End,
        )
    }
}

private fun DrawScope.drawFrostedBubbles(
    bubbles: List<FrostedBubble>,
    progress: Float,
    accentColor: Color,
) {
    bubbles.forEach { bubble ->
        val travel = progress * bubble.speed + bubble.phase
        val angle = (travel % 1f) * TwoPi
        val driftX = cos(angle) * bubble.driftFactor * size.width
        val driftY = sin(angle * bubble.orbitRatio) * bubble.driftFactor * size.height
        val baseX = bubble.originX * size.width
        val baseY = bubble.originY * size.height
        val center = Offset(baseX + driftX, baseY + driftY)
        val radius = bubble.radiusFactor * size.minDimension
        val intensity = lerp(0.35f, 0.8f, 1f - bubble.depth)
        val tint = accentColor.copy(alpha = intensity)

        drawCircle(
            color = tint.copy(alpha = tint.alpha * 0.6f),
            radius = radius,
            center = center,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = 0.9f),
                    tint.copy(alpha = 0.2f),
                    Color.Transparent,
                ),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

internal data class FrostedBubble(
    val originX: Float,
    val originY: Float,
    val radiusFactor: Float,
    val driftFactor: Float,
    val speed: Float,
    val orbitRatio: Float,
    val phase: Float,
    val depth: Float,
)

private const val TwoPi: Float = (PI * 2.0).toFloat()
