package com.plcoding.cameraxguide

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.plcoding.cameraxguide.model.CapturedPhoto
import com.plcoding.cameraxguide.ui.theme.AppShapes
import com.plcoding.cameraxguide.ui.theme.AppSpacing

@Composable
fun PhotoBottomSheetContent(
    photos: List<CapturedPhoto>,
    fallbackBitmaps: List<Bitmap>,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty() && fallbackBitmaps.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(AppSpacing.Gutter)
                .clip(AppShapes.Lg)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), AppShapes.Lg)
                .padding(AppSpacing.Gutter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No captures yet. Take a shot to populate the vault feed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val persistedEntries = photos
        val bitmapEntries = fallbackBitmaps.reversed()
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.Gutter),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Gutter)
        ) {
            item {
                CollectionHeader()
            }
            if (persistedEntries.isNotEmpty()) {
                itemsIndexed(
                    items = persistedEntries,
                    key = { _, item -> item.uri.toString() }
                ) { index, photo ->
                    ExposureFeedCard(
                        photo = photo,
                        index = index,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                itemsIndexed(bitmapEntries) { index, bitmap ->
                    FallbackBitmapCard(
                        bitmap = bitmap,
                        index = index,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(AppSpacing.Gutter))
            }
        }
    }
}

@Composable
private fun CollectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.Gutter),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Long Exposures",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "OPERATIONAL CLUSTER: A-04",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            shape = AppShapes.Default,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), AppShapes.Default)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppSpacing.PanelPadding, vertical = AppSpacing.Unit),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Unit),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = MaterialTheme.colorScheme.primaryContainer
                )
                Text("FILTERS", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ExposureFeedCard(photo: CapturedPhoto, index: Int, modifier: Modifier = Modifier) {
    val fileName = photo.displayName
    val shutter = when (index % 3) {
        0 -> "15.0s"
        1 -> "30.0s"
        else -> "08.5s"
    }

    Column(
        modifier = modifier
            .clip(AppShapes.Lg)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), AppShapes.Lg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (index % 3 == 2) 16f / 9f else 4f / 3f)
        ) {
            AsyncImage(
                model = photo.uri,
                contentDescription = fileName,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppSpacing.PanelPadding),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Unit)
            ) {
                FeedChip(text = if (index % 2 == 0) "4K RAW" else "HDR+")
                FeedChip(text = "ISO ${100 + (index * 100)}")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.PanelPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(fileName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(shutter, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Surface(
                shape = RoundedCornerShape(99),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(99))
            ) {
                androidx.compose.material3.IconButton(onClick = {}) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackBitmapCard(bitmap: Bitmap, index: Int, modifier: Modifier = Modifier) {
    val fileName = "VOLATILE_CAPTURE_${index.toString().padStart(3, '0')}.JPG"
    val shutter = when (index % 3) {
        0 -> "15.0s"
        1 -> "30.0s"
        else -> "08.5s"
    }

    Column(
        modifier = modifier
            .clip(AppShapes.Lg)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), AppShapes.Lg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (index % 3 == 2) 16f / 9f else 4f / 3f)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = fileName,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AppSpacing.PanelPadding),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Unit)
            ) {
                FeedChip(text = "RAM")
                FeedChip(text = "ISO ${100 + (index * 100)}")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.PanelPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(fileName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(shutter, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Surface(
                shape = RoundedCornerShape(99),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(99))
            ) {
                androidx.compose.material3.IconButton(onClick = {}) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedChip(text: String) {
    Surface(
        shape = AppShapes.Sm,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.Sm)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = AppSpacing.Unit * 2, vertical = AppSpacing.Unit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primaryContainer
        )
    }
}
