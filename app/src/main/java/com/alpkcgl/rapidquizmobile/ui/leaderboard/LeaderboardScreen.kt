package com.alpkcgl.rapidquizmobile.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.rapidquizmobile.R
import com.alpkcgl.rapidquizmobile.core.UiState
import com.alpkcgl.rapidquizmobile.data.model.CategoryRef
import com.alpkcgl.rapidquizmobile.ui.components.AppLogo
import com.alpkcgl.rapidquizmobile.ui.components.EmptyView
import com.alpkcgl.rapidquizmobile.ui.components.ErrorView
import com.alpkcgl.rapidquizmobile.ui.components.LoadingView
import com.alpkcgl.rapidquizmobile.ui.components.SecondaryButton
import com.alpkcgl.rapidquizmobile.ui.components.formatNumber
import com.alpkcgl.rapidquizmobile.ui.components.formatShortDate
import com.alpkcgl.rapidquizmobile.ui.components.userMessage
import com.alpkcgl.rapidquizmobile.ui.theme.RqColors
import com.alpkcgl.rapidquizmobile.ui.theme.RqShapes
import com.alpkcgl.rapidquizmobile.ui.theme.TABULAR_NUMS
import com.alpkcgl.rapidquizmobile.ui.theme.categoryPalette

@Composable
fun LeaderboardScreen(
    onPlayAgain: (categorySlug: String) -> Unit,
    onHome: () -> Unit,
    viewModel: LeaderboardViewModel = viewModel(factory = LeaderboardViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val overall = state.selectedSlug == null

    Column(
        Modifier
            .fillMaxSize()
            .background(RqColors.Background)
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppLogo()
            Text(stringResource(R.string.leaderboard_title), style = MaterialTheme.typography.headlineMedium)
        }
        Tabs(state, onSelect = viewModel::select)

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (val rows = state.rows) {
                UiState.Loading -> item { LoadingView(stringResource(R.string.leaderboard_loading)) }
                is UiState.Error -> item {
                    ErrorView(
                        title = stringResource(R.string.leaderboard_error_title),
                        message = rows.error.userMessage(),
                        onRetry = viewModel::retry,
                    )
                }
                is UiState.Success -> {
                    if (rows.data.isEmpty()) {
                        item {
                            EmptyView(
                                stringResource(R.string.leaderboard_empty_title),
                                stringResource(R.string.leaderboard_empty_text),
                            )
                        }
                    }
                    items(rows.data, key = { it.id }) { row ->
                        LeaderboardRowItem(
                            rank = row.rank,
                            nickname = row.nickname,
                            score = row.score,
                            createdAt = row.createdAt,
                            category = if (overall) row.category else null,
                            highlighted = row.id == state.myEntry?.id,
                        )
                    }
                    val mine = state.myEntry
                    val myRank = state.myRankHere
                    if (state.showMyRowSeparately && mine != null && myRank != null) {
                        item(key = "gap") {
                            Text(
                                "•••",
                                color = RqColors.TextMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clearAndSetSemantics { },
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                        item(key = "mine") {
                            LeaderboardRowItem(
                                rank = myRank,
                                nickname = mine.nickname,
                                score = mine.score,
                                createdAt = mine.createdAt,
                                category = if (overall) mine.category else null,
                                highlighted = true,
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.replaySlug?.let { slug ->
                SecondaryButton(
                    stringResource(R.string.action_play_again),
                    onClick = { onPlayAgain(slug) },
                    icon = R.drawable.ic_rotate_ccw,
                    modifier = Modifier.weight(1f),
                )
            }
            SecondaryButton(
                stringResource(R.string.action_home_title),
                onClick = onHome,
                icon = R.drawable.ic_house,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Tabs(state: LeaderboardUiState, onSelect: (String?) -> Unit) {
    val tabs = listOf<Pair<String?, String>>(null to stringResource(R.string.leaderboard_overall)) +
        state.categories.map { it.slug to it.name }
    val selectedIndex = tabs.indexOfFirst { it.first == state.selectedSlug }.coerceAtLeast(0)
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = RqColors.Background,
        contentColor = RqColors.Primary,
        edgePadding = 16.dp,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = true),
                width = Dp.Unspecified,
                color = RqColors.Primary,
            )
        },
        divider = {},
    ) {
        tabs.forEachIndexed { index, (slug, label) ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(slug) },
                text = { Text(label, style = MaterialTheme.typography.labelLarge) },
                selectedContentColor = RqColors.Primary,
                unselectedContentColor = RqColors.TextMuted,
            )
        }
    }
}

@Composable
private fun LeaderboardRowItem(
    rank: Int,
    nickname: String,
    score: Int,
    createdAt: String,
    category: CategoryRef?,
    highlighted: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RqShapes.Medium)
            .background(if (highlighted) RqColors.PrimarySoft else RqColors.Surface)
            .border(2.dp, if (highlighted) RqColors.Primary else RqColors.Surface, RqShapes.Medium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankMedal(rank)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    nickname,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (highlighted) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.leaderboard_you),
                        style = MaterialTheme.typography.labelSmall,
                        color = RqColors.TextOnColor,
                        modifier = Modifier
                            .clip(RqShapes.Pill)
                            .background(RqColors.Primary)
                            .padding(horizontal = 8.dp, vertical = 1.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (category != null) {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = RqColors.TextOnColor,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RqShapes.Pill)
                            .background(categoryPalette(category.slug, category.colorHex).strong)
                            .padding(horizontal = 8.dp, vertical = 1.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(formatShortDate(createdAt), style = MaterialTheme.typography.bodySmall, color = RqColors.TextMuted)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            formatNumber(score),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TABULAR_NUMS),
        )
    }
}

/** İlk 3 altın/gümüş/bronz; diğerleri düz. */
@Composable
private fun RankMedal(rank: Int) {
    val colors = when (rank) {
        1 -> RqColors.Gold
        2 -> RqColors.Silver
        3 -> RqColors.Bronze
        else -> null
    }
    val a11y = stringResource(R.string.leaderboard_rank_a11y, rank)
    Box(
        Modifier
            .size(36.dp)
            .clip(RqShapes.Pill)
            .background(
                if (colors != null) {
                    Brush.linearGradient(colors.take(2))
                } else {
                    Brush.linearGradient(listOf(RqColors.SurfaceAlt, RqColors.SurfaceAlt))
                },
            )
            .clearAndSetSemantics { contentDescription = a11y },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR_NUMS),
            color = colors?.get(2) ?: RqColors.TextMuted,
        )
    }
}
