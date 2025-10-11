package com.example.uigallary01

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AmbientParticlePhotoItem(
    modifier: Modifier = Modifier,
    state: AmbientParticleFieldState = rememberAmbientParticleFieldState(),
) {
    // PhotoPickerで写真を選択するためのLauncherを用意
    var selectedUriPath by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedUri: Uri? = remember(selectedUriPath) { selectedUriPath?.let(Uri::parse) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            // 選択されたURIは文字列として保存し、状態復元に備える
            selectedUriPath = uri?.toString()
        }
    )

    // PhotoPickerの結果が変わったらBitmapをデコードする
    LaunchedEffect(selectedUri) {
        imageBitmap = selectedUri?.let { uri ->
            withContext(Dispatchers.IO) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source).asImageBitmap()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                pickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) {
            Text(text = "写真を選択")
        }

        val previewShape = RoundedCornerShape(28.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .aspectRatio(3f / 4f)
                .clip(previewShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
        ) {
            imageBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Text(
                text = "写真を選択すると神秘的なパーティクルが重なります", // ユーザーへの説明
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            AmbientParticleLayer(
                modifier = Modifier.fillMaxSize(),
                state = state
            )
        }
    }
}

@Composable
fun rememberAmbientParticleFieldState(
    particleCount: Int = 28,
    seed: Int = 0,
): AmbientParticleFieldState {
    // パーティクルのランダム配置を安定させるためのシードを決定
    val actualSeed = remember(particleCount, seed) {
        if (seed != 0) seed else Random.Default.nextInt()
    }

    return remember(particleCount, actualSeed) {
        val random = Random(actualSeed)
        val particles = List(particleCount) {
            val anchor = Offset(random.nextFloat(), random.nextFloat())
            val baseRadius = lerp(0.014f, 0.05f, random.nextFloat())
            val depth = lerp(0.1f, 0.85f, random.nextFloat())
            val drift = lerp(0.02f, 0.08f, random.nextFloat())
            val orbitSpeed = lerp(0.35f, 1.15f, random.nextFloat())
            val intensity = lerp(0.45f, 0.9f, random.nextFloat())
            val hue = lerp(200f, 255f, random.nextFloat())
            val saturation = lerp(0.35f, 0.75f, random.nextFloat())
            val lightness = lerp(0.55f, 0.85f, random.nextFloat())
            val tint = Color.hsl(hue, saturation, lightness)
            AmbientParticle(
                anchor = anchor,
                baseRadius = baseRadius,
                depth = depth,
                drift = drift,
                orbitSpeed = orbitSpeed,
                phase = random.nextFloat() * (PI.toFloat() * 2f),
                tint = tint,
                intensity = intensity
            )
        }
        AmbientParticleFieldState(particles)
    }
}

@Composable
private fun AmbientParticleLayer(
    modifier: Modifier = Modifier,
    state: AmbientParticleFieldState,
) {
    // 永続的にゆっくり動くように無限アニメーションを利用
    val infiniteTransition = rememberInfiniteTransition(label = "ambientParticles")
    val driftProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "driftProgress"
    )
    val parallaxProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "parallaxProgress"
    )

    Canvas(modifier = modifier) {
        // ふわっとした霧感を先に塗り、パーティクルの輝きを馴染ませる
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.05f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            ),
            blendMode = BlendMode.Screen
        )

        val driftAngle = driftProgress * (2f * PI).toFloat()
        val parallaxAngle = parallaxProgress * (2f * PI).toFloat()
        state.particles.forEach { particle ->
            val baseCenter = computeParticleCenter(
                particle = particle,
                driftAngle = driftAngle,
                parallaxAngle = parallaxAngle,
                canvasSize = size
            )
            val radius = particle.baseRadius * size.minDimension
            drawGlowingParticle(
                center = baseCenter,
                radius = radius,
                tint = particle.tint,
                depth = particle.depth,
                intensity = particle.intensity
            )
        }
    }
}

private fun computeParticleCenter(
    particle: AmbientParticle,
    driftAngle: Float,
    parallaxAngle: Float,
    canvasSize: androidx.compose.ui.geometry.Size,
): Offset {
    // 振幅を深度で調整し、奥行き感のある擬似パララックスを作る
    val parallaxFactor = (1f - particle.depth) * 0.04f
    val slowWave = sin(parallaxAngle + particle.phase * 0.5f)
    val slowWaveY = cos(parallaxAngle * 0.75f + particle.phase)
    val driftWave = driftAngle * particle.orbitSpeed + particle.phase

    val normalizedX = (particle.anchor.x + sin(driftWave) * particle.drift + slowWave * parallaxFactor)
        .wrapUnitInterval()
    val normalizedY = (particle.anchor.y + cos(driftWave * 0.8f) * particle.drift + slowWaveY * parallaxFactor)
        .wrapUnitInterval()

    return Offset(normalizedX * canvasSize.width, normalizedY * canvasSize.height)
}

private fun DrawScope.drawGlowingParticle(
    center: Offset,
    radius: Float,
    tint: Color,
    depth: Float,
    intensity: Float,
) {
    // グローの大きさを深度で緩やかに変化させる
    val depthFade = lerp(0.6f, 1f, 1f - depth)
    val glowRadius = radius * 2.4f * depthFade
    val haloRadius = radius * 1.5f * depthFade

    drawCircle(
        color = tint.copy(alpha = 0.16f * intensity),
        radius = glowRadius,
        center = center,
        blendMode = BlendMode.Screen
    )
    drawCircle(
        color = tint.copy(alpha = 0.28f * intensity),
        radius = haloRadius,
        center = center,
        blendMode = BlendMode.Screen
    )
    drawCircle(
        color = tint.copy(alpha = 0.7f),
        radius = radius * 0.95f,
        center = center,
        blendMode = BlendMode.Plus
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.6f * intensity),
        radius = radius * 0.45f,
        center = center,
        blendMode = BlendMode.Screen
    )
}

private fun Float.wrapUnitInterval(): Float {
    val value = this % 1f
    return if (value < 0f) value + 1f else value
}

internal data class AmbientParticle(
    val anchor: Offset,
    val baseRadius: Float,
    val depth: Float,
    val drift: Float,
    val orbitSpeed: Float,
    val phase: Float,
    val tint: Color,
    val intensity: Float,
)

class AmbientParticleFieldState internal constructor(
    internal val particles: List<AmbientParticle>,
)
