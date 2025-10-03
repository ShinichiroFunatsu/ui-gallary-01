package com.example.uigallary01

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uigallary01.ui.theme.UiGallary01Theme

@Composable
fun GalleryScreen(modifier: Modifier = Modifier) {
    val moodySnowState = rememberMoodySnowBackgroundState()

    // ギャラリーに表示する要素を定義
    val galleryItems = listOf(
        GalleryItem(
            title = "Hello World Dialog",
            content = { HelloWorldDialogItem() }
        ),
        GalleryItem(
            title = "Scramble Text",
            content = { ScrambleTextItem() }
        ),
        GalleryItem(
            title = "Digital Rain Background",
            content = { DigitalRainBackgroundItem() }
        ),
        GalleryItem(
            title = "Moody Snow Background",
            content = { MoodySnowBackgroundItem(state = moodySnowState) },
        ),
        GalleryItem(
            title = "Scrollable Rounded Tab Navigation",
            content = { ScrollableRoundedNavigationItem() }
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(galleryItems) { item ->
            item.ListItem()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryScreenPreview() {
    UiGallary01Theme {
        GalleryScreen()
    }
}

// ギャラリーに表示する要素の定義
private data class GalleryItem(
    val title: String,
    val content: @Composable () -> Unit,
)

// データクラス自身が描画手段を提供できるように拡張関数化
@Composable
private fun GalleryItem.ListItem(
    modifier: Modifier = Modifier,
) {
    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ギャラリー要素のタイトルを表示
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        cardContent()
    }
}
