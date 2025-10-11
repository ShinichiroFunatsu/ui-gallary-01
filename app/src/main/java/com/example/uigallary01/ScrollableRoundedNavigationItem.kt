package com.example.uigallary01

import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabPosition
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex

private val TabHighlightColor = Color(0xFFEF5350)

@Composable
fun ScrollableRoundedNavigationItem(modifier: Modifier = Modifier) {
    // ナビゲーションに表示するトピックと説明文を定義
    val tabs = listOf(
        NavigationTab(
            title = "Aurora Drift",
            description = "極光が流れるように移ろう幻想的なサウンドスケープ。"
        ),
        NavigationTab(
            title = "Crimson Peak",
            description = "赤く染まる峰々を駆け抜けるシンセサイザーの躍動。"
        ),
        NavigationTab(
            title = "Silent Nebula",
            description = "静寂の宇宙に漂う淡いノイズとパッドの揺らぎ。"
        ),
        NavigationTab(
            title = "Ruby Pulse",
            description = "心拍のように脈動するベースラインとリズムの連なり。"
        ),
        NavigationTab(
            title = "Garnet Echo",
            description = "深紅の洞窟で響く残響をモチーフにしたアンビエント。"
        ),
    )

    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val tabTextWidths = remember { mutableStateListOf<Int>().apply { repeat(tabs.size) { add(0) } } }
    val density = LocalDensity.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 24.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            divider = {},
            indicator = { tabPositions ->
                SmoothRoundedTabIndicator(
                    tabPositions = tabPositions,
                    selectedIndex = selectedTabIndex,
                    tabContentWidths = tabTextWidths,
                    density = density,
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedTabIndex
                val interactionSource = remember { MutableInteractionSource() }
                // タブタップ時のリップル演出を抑制してインジケーター移動を際立たせる
                Box(
                    modifier = Modifier
                        .selectable(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            role = Role.Tab,
                            interactionSource = interactionSource,
                            indication = null
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .onGloballyPositioned { coordinates ->
                            tabTextWidths[index] = coordinates.size.width
                        }
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(vertical = 24.dp, horizontal = 20.dp)
        ) {
            val activeTab = tabs[selectedTabIndex]
            Column(verticalArrangement = TabContentSpacing) {
                Text(
                    text = activeTab.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TabHighlightColor
                )
                Text(
                    text = activeTab.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SmoothRoundedTabIndicator(
    tabPositions: List<TabPosition>,
    selectedIndex: Int,
    tabContentWidths: List<Int>,
    density: Density,
    indicatorPadding: Dp = 6.dp,
    cornerRadius: Dp = 18.dp,
) {
    // インジケーターはタブのコンテンツ幅に合わせて動くようにトランジションさせる
    if (tabPositions.isEmpty()) return
    val transition = updateTransition(targetState = selectedIndex, label = "tabIndicator")
    val indicatorStart by transition.animateDp(label = "indicatorStart") { index ->
        val tabPosition = tabPositions[index]
        val contentWidth = tabContentWidths.getOrNull(index).orZero()
        val contentWidthDp = if (contentWidth > 0) {
            with(density) { contentWidth.toDp() }
        } else {
            tabPosition.width
        }
        tabPosition.left + (tabPosition.width - contentWidthDp) / 2
    }
    val indicatorWidth by transition.animateDp(label = "indicatorWidth") { index ->
        val tabPosition = tabPositions[index]
        val contentWidth = tabContentWidths.getOrNull(index).orZero()
        if (contentWidth > 0) {
            with(density) { contentWidth.toDp() }
        } else {
            tabPosition.width
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(-1f)
    ) {
        // グロウ表現は三色を巡回しつつ、ぼかし・不透明度・スケールをゆっくり往復させる
        val halo = rememberTabHalo()
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(vertical = indicatorPadding)
                .offset(x = indicatorStart)
                .width(indicatorWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    val scale = halo.scale
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin.Center
                    alpha = halo.opacity
                }
                .blur(radius = halo.blur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    color = halo.color,
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }
}

private fun Int?.orZero(): Int = this ?: 0

@Composable
private fun rememberTabHalo(): TabHalo {
    // スライダー操作ではなく、一定周期でパラメータが循環するように無限トランジションを構成
    val infiniteTransition = rememberInfiniteTransition(label = "tabHalo")
    val haloColor by infiniteTransition.animateValue(
        initialValue = TabHaloColors.first(),
        targetValue = TabHaloColors[1],
        typeConverter = TabHaloColorConverter,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = HaloColorCycleDurationMillis
                TabHaloColors[0] at 0
                TabHaloColors[1] at HaloColorCycleDurationMillis / 3
                TabHaloColors[2] at HaloColorCycleDurationMillis * 2 / 3
                TabHaloColors[0] at durationMillis
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "haloColor",
    )
    val blur by infiniteTransition.animateFloat(
        initialValue = HaloBlurRange.start,
        targetValue = HaloBlurRange.endInclusive,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = HaloBreathingDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloBlur",
    )
    val opacity by infiniteTransition.animateFloat(
        initialValue = HaloOpacityRange.start,
        targetValue = HaloOpacityRange.endInclusive,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = HaloOpacityDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloOpacity",
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = HaloScaleRange.start,
        targetValue = HaloScaleRange.endInclusive,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = HaloScaleDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale",
    )

    return TabHalo(
        color = haloColor,
        blur = blur.dp,
        opacity = opacity,
        scale = scale,
    )
}

private data class TabHalo(
    val color: Color,
    val blur: Dp,
    val opacity: Float,
    val scale: Float,
)

private val TabHaloColors = listOf(
    Color(0xFFEF5350),
    Color(0xFFF06292),
    Color(0xFFFF8A65),
)

private val HaloBlurRange = 4f..14f
private val HaloOpacityRange = 0.55f..0.9f
private val HaloScaleRange = 0.94f..1.08f
private const val HaloColorCycleDurationMillis = 12000
private const val HaloBreathingDurationMillis = 6800
private const val HaloOpacityDurationMillis = 7600
private const val HaloScaleDurationMillis = 8400

private val TabHaloColorConverter: TwoWayConverter<Color, AnimationVector4D> = TwoWayConverter(
    convertToVector = { color -> AnimationVector4D(color.red, color.green, color.blue, color.alpha) },
    convertFromVector = { vector -> Color(vector.v1, vector.v2, vector.v3, vector.v4) },
)

private data class NavigationTab(
    val title: String,
    val description: String,
)

private val TabContentSpacing = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
