package com.example.uigallary01

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AmbientParticlePhotoItem(
    modifier: Modifier = Modifier,
    state: AmbientParticleFieldState = rememberAmbientParticleFieldState(),
) {
    // PhotoPicker で選択した写真を保持
    var selectedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current
    var useAddBlend by rememberSaveable { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedUri = uri }
    )

    // 写真の読み込みを非同期で行う
    LaunchedEffect(selectedUri) {
        val targetUri = selectedUri ?: run {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            loadBitmap(context.contentResolver.openInputStream(targetUri))
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ambient Particle Overlay",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "PhotoPickerで任意の写真を読み込み、加算/スクリーン合成の粒子層をリアルタイムに重ね合わせます。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                Text(text = "写真を選択")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = useAddBlend,
                    onCheckedChange = { checked -> useAddBlend = checked },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = if (useAddBlend) "加算合成" else "スクリーン合成",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        val aspectRatio = bitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: DefaultAspectRatio
        val blendMode = if (useAddBlend) BlendMode.Plus else BlendMode.Screen

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .aspectRatio(aspectRatio),
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(26.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "選択した写真",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "写真を選択するとエフェクトをプレビューできます",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                                )
                            )
                        )
                )

                AmbientParticleOverlay(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    blendMode = blendMode,
                )
            }
        }
    }
}

@Composable
fun rememberAmbientParticleFieldState(
    particleCount: Int = 60,
    seed: Int = 411,
): AmbientParticleFieldState {
    // 表示する粒子を初期生成
    val particles = remember(particleCount, seed) {
        val random = Random(seed)
        List(particleCount) { createAmbientParticle(random) }
    }
    val elapsed = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(particles) {
        var previous = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (previous == 0L) {
                    previous = frameTime
                    return@withFrameNanos
                }
                val delta = (frameTime - previous) / 1_000_000_000f
                previous = frameTime
                val next = elapsed.floatValue + delta
                elapsed.floatValue = if (next > ElapsedLoopSeconds) next - ElapsedLoopSeconds else next
            }
        }
    }

    return remember { AmbientParticleFieldState(particles = particles, elapsedSeconds = elapsed) }
}

@Stable
class AmbientParticleFieldState internal constructor(
    internal val particles: List<AmbientParticle>,
    internal val elapsedSeconds: State<Float>,
)

internal data class AmbientParticle(
    val anchor: Offset,
    val amplitude: Offset,
    val driftSpeed: Offset,
    val phase: Offset,
    val depth: Float,
    val radiusFactor: Float,
    val baseAlpha: Float,
    val color: Color,
    val twinkleSpeed: Float,
    val twinklePhase: Float,
)

@Composable
private fun AmbientParticleOverlay(
    modifier: Modifier,
    state: AmbientParticleFieldState,
    blendMode: BlendMode,
) {
    val palette = MaterialTheme.colorScheme
    val veilColor = remember(palette) {
        lerp(palette.primary, palette.secondary, 0.5f).copy(alpha = 0.3f)
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val elapsed = state.elapsedSeconds.value
        val width = size.width
        val height = size.height
        val shortest = min(width, height)
        val parallax = Offset(
            x = (sin((elapsed * ParallaxSpeedX).toDouble()) * ParallaxStrength * width).toFloat(),
            y = (cos((elapsed * ParallaxSpeedY).toDouble()) * ParallaxStrength * height).toFloat(),
        )

        drawIntoCanvas { canvas ->
            val paint = androidx.compose.ui.graphics.Paint().apply {
                isAntiAlias = true
            }
            state.particles.forEach { particle ->
                val depthFactor = 1f - particle.depth
                val driftX = (sin((elapsed * particle.driftSpeed.x + particle.phase.x).toDouble()) * particle.amplitude.x).toFloat()
                val driftY = (cos((elapsed * particle.driftSpeed.y + particle.phase.y).toDouble()) * particle.amplitude.y).toFloat()
                val center = Offset(
                    x = (particle.anchor.x + driftX) * width + parallax.x * depthFactor,
                    y = (particle.anchor.y + driftY) * height + parallax.y * depthFactor,
                )
                val twinkle = 0.5f + 0.5f * sin((elapsed * particle.twinkleSpeed + particle.twinklePhase).toDouble()).toFloat()
                val alpha = (particle.baseAlpha * twinkle).coerceIn(0f, 1f)
                if (alpha <= 0f) return@forEach
                val radius = particle.radiusFactor * shortest * (0.7f + 0.3f * depthFactor)
                drawGlowParticle(
                    center = center,
                    radius = radius,
                    color = particle.color,
                    alpha = alpha,
                    blendMode = blendMode,
                    paint = paint,
                    veilColor = veilColor,
                )
            }
        }
    }
}

private fun DrawScope.drawGlowParticle(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
    blendMode: BlendMode,
    paint: androidx.compose.ui.graphics.Paint,
    veilColor: Color,
) {
    val coreColor = color.copy(alpha = alpha)
    val haloColor = veilColor.copy(alpha = alpha * 0.35f)

    drawIntoCanvas { canvas ->
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = coreColor.toArgb()
        frameworkPaint.setShadowLayer(radius * GlowShadowMultiplier, 0f, 0f, haloColor.toArgb())
        canvas.drawCircle(center, radius, paint)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(coreColor, color.copy(alpha = 0f)),
            center = center,
            radius = radius * HaloRadiusMultiplier,
        ),
        center = center,
        radius = radius * HaloRadiusMultiplier,
        blendMode = blendMode,
    )
}

private fun createAmbientParticle(random: Random): AmbientParticle {
    val depth = random.nextFloat(DepthRange)
    return AmbientParticle(
        anchor = Offset(random.nextFloat(), random.nextFloat()),
        amplitude = Offset(
            random.nextFloat(0.02f, 0.12f) * (1f - depth),
            random.nextFloat(0.02f, 0.16f) * (1f - depth),
        ),
        driftSpeed = Offset(
            random.nextFloat(0.08f, 0.22f),
            random.nextFloat(0.05f, 0.18f),
        ),
        phase = Offset(
            random.nextFloat(0f, (2f * PI).toFloat()),
            random.nextFloat(0f, (2f * PI).toFloat()),
        ),
        depth = depth,
        radiusFactor = random.nextFloat(0.02f, 0.07f),
        baseAlpha = random.nextFloat(0.2f, 0.55f),
        color = Color.hsl(
            hue = random.nextFloat(ParticleHueRange),
            saturation = random.nextFloat(ParticleSaturationRange),
            lightness = random.nextFloat(ParticleLightnessRange),
        ),
        twinkleSpeed = random.nextFloat(0.25f, 0.65f),
        twinklePhase = random.nextFloat(0f, (2f * PI).toFloat()),
    )
}

private fun loadBitmap(stream: InputStream?): ImageBitmap? {
    if (stream == null) return null
    return stream.use { input ->
        BitmapFactory.decodeStream(input)?.asImageBitmap()
    }
}

private fun Random.nextFloat(range: ClosedFloatingPointRange<Float>): Float {
    require(range.endInclusive > range.start)
    return range.start + nextFloat() * (range.endInclusive - range.start)
}

private fun Random.nextFloat(min: Float, max: Float): Float = nextFloat(min..max)

private const val DefaultAspectRatio = 3f / 4f
private const val ElapsedLoopSeconds = 720f
private const val GlowShadowMultiplier = 1.6f
private const val HaloRadiusMultiplier = 1.6f
private const val ParallaxStrength = 0.035f
private const val ParallaxSpeedX = 0.18f
private const val ParallaxSpeedY = 0.14f
private val DepthRange = 0.1f..0.92f
private val ParticleHueRange = 185f..220f
private val ParticleSaturationRange = 0.35f..0.65f
private val ParticleLightnessRange = 0.55f..0.82f
