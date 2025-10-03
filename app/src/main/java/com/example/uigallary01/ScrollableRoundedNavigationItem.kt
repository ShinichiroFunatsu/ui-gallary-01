package com.example.uigallary01

import androidx.compose.animation.core.animateDp
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity

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
                Tab(
                    selected = isSelected,
                    onClick = { selectedTabIndex = index },
                    selectedContentColor = Color.White,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    // 選択状態に合わせてウェイトを変えることで視覚的な抑揚を付ける
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .onGloballyPositioned { coordinates ->
                                tabTextWidths[index] = coordinates.size.width
                            }
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        )
                    }
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
    highlightColor: Color = TabHighlightColor,
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
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(vertical = indicatorPadding)
                .offset(x = indicatorStart)
                .width(indicatorWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(cornerRadius))
                .background(highlightColor.copy(alpha = 0.85f))
        )
    }
}

private fun Int?.orZero(): Int = this ?: 0

private data class NavigationTab(
    val title: String,
    val description: String,
)

private val TabContentSpacing = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
