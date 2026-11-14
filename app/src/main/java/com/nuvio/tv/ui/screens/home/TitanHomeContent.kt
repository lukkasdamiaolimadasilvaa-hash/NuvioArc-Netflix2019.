package com.nuvio.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.domain.model.MetaPreview

/**
 * Titan 2019 home: dark streaming layout with a focused-title backdrop,
 * compact metadata and horizontally browsable poster rows.
 * It uses the app's existing detail-navigation callback.
 */
private val TitanGreen = Color(0xFF46D369)
private val TitanRed = Color(0xFFE50914)

private data class TitanCard(
    val key: String,
    val id: String,
    val type: String,
    val addonBaseUrl: String,
    val name: String,
    val poster: String?,
    val backdrop: String?,
    val description: String?,
    val year: String?,
    val rating: Float?,
    val ageRating: String?,
    val extra: String?,
    val progress: Float?,
    val continueWatchingItem: ContinueWatchingItem? = null
)

private data class TitanRow(
    val key: String,
    val title: String,
    val cards: List<TitanCard>
)

private fun MetaPreview.toTitanCard(rowKey: String, index: Int, addonBaseUrl: String): TitanCard {
    val seasons = seasonCount
    val extraText = when {
        seasons != null && seasons > 0 -> if (seasons == 1) "1 Temporada" else "$seasons Temporadas"
        !runtime.isNullOrBlank() -> runtime
        else -> null
    }
    return TitanCard(
        key = "$rowKey|$index|$id",
        id = id,
        type = apiType,
        addonBaseUrl = addonBaseUrl,
        name = name,
        poster = poster,
        backdrop = backdropUrl,
        description = description,
        year = releaseInfo,
        rating = imdbRating,
        ageRating = ageRating,
        extra = extraText,
        progress = null
    )
}

private fun buildTitanRows(uiState: HomeUiState): List<TitanRow> {
    val rows = ArrayList<TitanRow>()

    if (uiState.continueWatchingEnabled) {
        val cwCards = uiState.continueWatchingItems.mapNotNull { item ->
            when (item) {
                is ContinueWatchingItem.InProgress -> {
                    val progress = item.progress
                    TitanCard(
                        key = "cw|${progress.contentId}|${progress.videoId}",
                        id = progress.contentId,
                        type = progress.contentType,
                        addonBaseUrl = "",
                        name = progress.name,
                        poster = item.originalPoster ?: progress.poster,
                        backdrop = progress.backdrop ?: progress.poster,
                        description = item.episodeDescription,
                        year = item.releaseInfo,
                        rating = item.episodeImdbRating,
                        ageRating = null,
                        extra = if (progress.season != null && progress.episode != null) "T${progress.season}:E${progress.episode}" else null,
                        progress = progress.progressPercentage.coerceIn(0f, 1f),
                        continueWatchingItem = item
                    )
                }
                else -> null
            }
        }
        if (cwCards.isNotEmpty()) {
            rows.add(TitanRow(key = "cw", title = "Continuar assistindo", cards = cwCards))
        }
    }

    uiState.catalogRows.forEach { row ->
        if (row.items.isEmpty()) return@forEach
        val rowKey = "${row.addonId}|${row.apiType}|${row.catalogId}"
        rows.add(
            TitanRow(
                key = rowKey,
                title = row.catalogName,
                cards = row.items.mapIndexed { index, item ->
                    item.toTitanCard(rowKey, index, row.addonBaseUrl)
                }
            )
        )
    }
    return rows
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TitanHomeContent(
    uiState: HomeUiState,
    onNavigateToDetail: (String, String, String) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = remember(uiState.catalogRows, uiState.continueWatchingItems, uiState.continueWatchingEnabled) {
        buildTitanRows(uiState)
    }
    var focusedCard by remember { mutableStateOf<TitanCard?>(null) }
    val shown = focusedCard ?: rows.firstOrNull()?.cards?.firstOrNull()
    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (shown?.backdrop != null) {
            AsyncImage(
                model = shown.backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth(0.72f)
                    .fillMaxHeight(0.72f)
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0.0f to Color.Black,
                    0.30f to Color.Black.copy(alpha = 0.85f),
                    0.62f to Color.Transparent
                )
            )
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.45f to Color.Transparent,
                    0.70f to Color.Black
                )
            )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TitanInfo(
                card = shown,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.50f)
                    .padding(start = 56.dp, top = 40.dp, end = 56.dp)
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.50f)
                    .focusRestorer(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(rows, key = { _, row -> row.key }) { _, row ->
                    TitanRowView(
                        row = row,
                        onFocused = { focusedCard = it },
                        onClick = { card ->
                            val continueWatchingItem = card.continueWatchingItem
                            if (continueWatchingItem != null) {
                                onContinueWatchingClick(continueWatchingItem)
                            } else {
                                onNavigateToDetail(card.id, card.type, card.addonBaseUrl)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TitanInfo(card: TitanCard?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Top) {
        if (card == null) return@Column
        Text(
            text = card.name,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.55f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (card.rating != null) {
                Text(
                    text = "★ " + String.format("%.1f", card.rating),
                    color = TitanGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!card.year.isNullOrBlank()) {
                Text(text = card.year, color = Color(0xFFE5E5E5), fontSize = 13.sp)
            }
            if (!card.ageRating.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4D4D4D), RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(text = card.ageRating, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (!card.extra.isNullOrBlank()) {
                Text(text = card.extra, color = Color(0xFFE5E5E5), fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (!card.description.isNullOrBlank()) {
            Text(
                text = card.description,
                color = Color(0xFFF2F2F2),
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.42f)
            )
        }
    }
}

@Composable
private fun TitanRowView(
    row: TitanRow,
    onFocused: (TitanCard) -> Unit,
    onClick: (TitanCard) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = row.title,
            color = Color(0xFFE5E5E5),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 56.dp, bottom = 6.dp)
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 56.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(row.cards, key = { _, card -> card.key }) { _, card ->
                TitanPoster(card = card, onFocused = onFocused, onClick = onClick)
            }
        }
    }
}

@Composable
private fun TitanPoster(
    card: TitanCard,
    onFocused: (TitanCard) -> Unit,
    onClick: (TitanCard) -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = Modifier
            .width(104.dp)
            .height(150.dp)
            .onFocusChanged {
                hasFocus = it.isFocused
                if (it.isFocused) onFocused(card)
            }
            .border(
                width = if (hasFocus) 3.dp else 0.dp,
                color = if (hasFocus) Color.White else Color.Transparent,
                shape = shape
            )
            .padding(if (hasFocus) 3.dp else 0.dp)
            .background(Color(0xFF2A2A2A), shape)
            .clickable { onClick(card) }
    ) {
        AsyncImage(
            model = card.poster ?: card.backdrop,
            contentDescription = card.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (card.progress != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0x55FFFFFF))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(card.progress)
                    .height(4.dp)
                    .background(TitanRed)
            )
        }
    }
}
