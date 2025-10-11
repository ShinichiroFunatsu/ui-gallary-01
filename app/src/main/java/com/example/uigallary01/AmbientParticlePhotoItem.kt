package com.example.uigallary01

import android.graphics.BlurMaskFilter
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

@Composable
fun AmbientParticlePhotoItem(modifier: Modifier = Modifier) {
    // 写真選択状態を保持
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedPhotoUri = uri
    }

    // 粒子フィールドの状態を保持
    val particleState = rememberAmbientParticleFieldState()

    // 疑似視差のアニメーションを保持
    val parallaxTarget = remember { mutableStateOf(Offset.Zero) }
    val parallaxX = remember { Animatable(0f) }
    val parallaxY = remember { Animatable(0f) }

    LaunchedEffect(parallaxTarget.value) {
        launch { parallaxX.animateTo(parallaxTarget.value.x, animationSpec = tween(durationMillis = 800)) }
        launch { parallaxY.animateTo(parallaxTarget.value.y, animationSpec = tween(durationMillis = 800)) }
    }

    val animatedParallax = Offset(parallaxX.value, parallaxY.value)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val shape = RoundedCornerShape(32.dp)
        val colorScheme = MaterialTheme.colorScheme
        val gradientPrimary = colorScheme.primary.copy(alpha = 0.12f)
        val gradientSecondary = colorScheme.secondary.copy(alpha = 0.08f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(shape)
                .drawBehind {
                    // 背景のゆらぎを演出するグラデーション
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                gradientPrimary,
                                gradientSecondary,
                                Color.Transparent
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    )
                }
                .background(colorScheme.surface.copy(alpha = 0.35f))
                .ambientParallaxInput { fraction ->
                    parallaxTarget.value = fraction * 0.8f
                }
        ) {
            val context = LocalContext.current
            // 選択された写真を表示
            selectedPhotoUri?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Photo Pickerで写真を選んでください", // 誘導メッセージ
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                )
            }

            // 光のベールを重ねて幻想感を演出
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset.Zero,
                            radius = 600f
                        )
                    )
            )

            // 粒子レイヤーを描画
            AmbientParticleLayer(
                state = particleState,
                parallaxOffset = animatedParallax,
                modifier = Modifier.fillMaxSize()
            )

            // ガラス調のパネルを下部に設置
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(18.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ambient Particle", // パネルタイトル
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "光の粒子が漂う幻想的な空気感を写真に重ねます。指先でゆっくり視差が変わります。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Button(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(imageVector = Icons.Filled.Photo, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "写真を選ぶ")
        }
    }
}

@Composable
private fun AmbientParticleLayer(
    state: AmbientParticleFieldState,
    parallaxOffset: Offset,
    modifier: Modifier = Modifier
) {
    val paint = remember { Paint() }
    val time = rememberFrameClockSeconds()

    Canvas(
        modifier = modifier.graphicsLayer {
            // 加算合成のためにオフスクリーンを強制
            compositingStrategy = CompositingStrategy.Offscreen
        }
    ) {
        val canvasSize = size
        val parallaxShift = Offset(
            x = parallaxOffset.x * canvasSize.width * 0.12f,
            y = parallaxOffset.y * canvasSize.height * 0.12f
        )

        drawIntoCanvas { canvas ->
            val layerPaint = Paint()
            canvas.saveLayer(size.toRect(), layerPaint)

            state.particles.forEach { particle ->
                val driftSeconds = time * particle.driftSpeed
                val normalizedDrift = (particle.basePosition + particle.driftDirection * driftSeconds).wrapUnit()
                val drifted = Offset(
                    x = normalizedDrift.x * canvasSize.width,
                    y = normalizedDrift.y * canvasSize.height
                ) + parallaxShift * (1f - particle.depth)

                val flicker = 0.5f + 0.5f * sin((time * particle.flickerSpeed + particle.phase) * 2f * PI).toFloat()
                val radiusPx = particle.radius.value * density * (0.9f + 0.2f * flicker)
                val glowPx = particle.glowRadius.value * density
                val alpha = particle.baseAlpha * (0.6f + 0.4f * flicker)

                paint.color = particle.color.copy(alpha = alpha)
                paint.blendMode = BlendMode.Screen

                val frameworkPaint = paint.asFrameworkPaint()
                frameworkPaint.isAntiAlias = true
                frameworkPaint.maskFilter = BlurMaskFilter(glowPx, BlurMaskFilter.Blur.NORMAL)

                canvas.drawCircle(drifted, radiusPx, paint)
            }

            canvas.restore()
        }
    }
}

private class AmbientParticleFieldState(
    val particles: List<AmbientParticle>
)

private data class AmbientParticle(
    val basePosition: Offset,
    val driftDirection: Offset,
    val radius: Dp,
    val glowRadius: Dp,
    val depth: Float,
    val baseAlpha: Float,
    val color: Color,
    val driftSpeed: Float,
    val flickerSpeed: Float,
    val phase: Float
)

@Composable
private fun rememberAmbientParticleFieldState(
    particleCount: Int = 48,
    seed: Int = 0
): AmbientParticleFieldState {
    val actualSeed = remember(seed) { if (seed == 0) Random.nextInt() else seed }
    val particles = remember(particleCount, actualSeed) {
        val random = Random(actualSeed)
        List(particleCount) {
            val basePosition = Offset(random.nextFloat(), random.nextFloat())
            val direction = random.nextDirection()
            val radius = lerp(36.dp, 96.dp, random.nextFloat())
            val glowRadius = radius * 1.6f
            val depth = 0.08f + random.nextFloat() * 0.75f
            val baseAlpha = 0.22f + random.nextFloat() * 0.35f
            val hue = 210f + random.nextFloat() * 40f
            val saturation = 0.45f + random.nextFloat() * 0.2f
            val lightness = 0.82f
            val color = Color.hsl(hue, saturation, lightness)
            val driftSpeed = 0.005f + random.nextFloat() * 0.01f
            val flickerSpeed = 0.15f + random.nextFloat() * 0.35f
            val phase = random.nextFloat() * 2f * PI.toFloat()

            AmbientParticle(
                basePosition = basePosition,
                driftDirection = direction,
                radius = radius,
                glowRadius = glowRadius,
                depth = depth,
                baseAlpha = baseAlpha,
                color = color,
                driftSpeed = driftSpeed,
                flickerSpeed = flickerSpeed,
                phase = phase
            )
        }
    }

    return remember(actualSeed) { AmbientParticleFieldState(particles) }
}

@Composable
private fun rememberFrameClockSeconds(): Float {
    val elapsed = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val frame = withFrameNanos { it }
            if (last != 0L) {
                val delta = (frame - last) / 1_000_000_000f
                elapsed.floatValue += delta
            }
            last = frame
        }
    }

    return elapsed.floatValue
}

private fun Modifier.ambientParallaxInput(onOffsetChanged: (Offset) -> Unit): Modifier =
    pointerInput(onOffsetChanged) {
        val width = size.width.toFloat().coerceAtLeast(1f)
        val height = size.height.toFloat().coerceAtLeast(1f)

        detectDragGestures(
            onDragStart = { offset ->
                onOffsetChanged(offset.toParallaxFraction(width, height))
            },
            onDrag = { change, _ ->
                onOffsetChanged(change.position.toParallaxFraction(width, height))
            },
            onDragCancel = {
                onOffsetChanged(Offset.Zero)
            },
            onDragEnd = {
                onOffsetChanged(Offset.Zero)
            }
        )
    }

private fun Random.nextDirection(): Offset {
    val x = nextFloat() * 2f - 1f
    val y = nextFloat() * 2f - 1f
    val length = hypot(x, y).coerceAtLeast(1e-3f)
    return Offset(x / length, y / length)
}

private operator fun Offset.times(scalar: Float): Offset = Offset(x * scalar, y * scalar)

private fun Offset.wrapUnit(): Offset {
    fun Float.wrap() = ((this % 1f) + 1f) % 1f
    return Offset(x.wrap(), y.wrap())
}

private fun Offset.toParallaxFraction(width: Float, height: Float): Offset {
    val normalized = Offset(
        x = (x / width) - 0.5f,
        y = (y / height) - 0.5f
    )
    return normalized * 2f
}
