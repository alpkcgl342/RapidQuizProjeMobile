package com.alpkcgl.rapidquizmobile.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.rapidquizmobile.R
import com.alpkcgl.rapidquizmobile.core.UiState
import com.alpkcgl.rapidquizmobile.data.model.Category
import com.alpkcgl.rapidquizmobile.data.repository.SessionLostReason
import com.alpkcgl.rapidquizmobile.ui.components.AppLogo
import com.alpkcgl.rapidquizmobile.ui.components.ButtonSize
import com.alpkcgl.rapidquizmobile.ui.components.EmptyView
import com.alpkcgl.rapidquizmobile.ui.components.ErrorView
import com.alpkcgl.rapidquizmobile.ui.components.LoadingView
import com.alpkcgl.rapidquizmobile.ui.components.SecondaryButton
import com.alpkcgl.rapidquizmobile.ui.components.htmlString
import com.alpkcgl.rapidquizmobile.ui.components.userMessage
import com.alpkcgl.rapidquizmobile.ui.theme.RqColors
import com.alpkcgl.rapidquizmobile.ui.theme.RqShapes
import com.alpkcgl.rapidquizmobile.ui.theme.categoryIcon
import com.alpkcgl.rapidquizmobile.ui.theme.categoryPalette

@Composable
fun HomeScreen(
    onCategoryClick: (slug: String) -> Unit,
    onLeaderboardClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onShown()
        onPauseOrDispose { }
    }

    Scaffold(containerColor = RqColors.Background) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            // Liste durum çubuğunun altında başlar ve kırpılır; içerik saatle çakışmaz.
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 24.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            fullWidth { Hero() }
            notice?.let { reason -> fullWidth { Notice(reason, onDismiss = viewModel::dismissNotice) } }
            fullWidth {
                Text(
                    stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            when (val state = categories) {
                UiState.Loading -> fullWidth { LoadingView(stringResource(R.string.home_loading)) }
                is UiState.Error -> fullWidth {
                    ErrorView(
                        title = stringResource(R.string.home_error_title),
                        message = state.error.userMessage(),
                        onRetry = viewModel::retry,
                    )
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        fullWidth { EmptyView(stringResource(R.string.home_empty), null) }
                    }
                    items(state.data, key = { it.slug }) { category ->
                        CategoryCard(category, onClick = { onCategoryClick(category.slug) })
                    }
                }
            }
            fullWidth {
                SecondaryButton(
                    text = stringResource(R.string.action_leaderboard),
                    onClick = onLeaderboardClick,
                    size = ButtonSize.Large,
                    icon = R.drawable.ic_trophy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        }
    }
}

private fun LazyGridScope.fullWidth(
    content: @Composable () -> Unit,
) = item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun Hero() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppLogo(large = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_timer),
                contentDescription = null,
                tint = RqColors.Primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                htmlString(R.string.home_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = RqColors.TextMuted,
            )
        }
    }
}

@Composable
private fun Notice(reason: SessionLostReason, onDismiss: () -> Unit) {
    val text = stringResource(
        if (reason == SessionLostReason.EXPIRED) R.string.home_notice_expired else R.string.home_notice_lost,
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RqShapes.Medium)
            .background(RqColors.WarningSoft)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(
            painterResource(R.drawable.ic_x),
            contentDescription = null,
            tint = RqColors.TextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit) {
    val palette = categoryPalette(category.slug, category.colorHex)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "cardScale")
    val a11y = if (category.isPlayable) {
        stringResource(R.string.category_a11y_play, category.name, category.questionsPerSession)
    } else {
        stringResource(R.string.category_a11y_preparing, category.name)
    }

    Box(
        Modifier
            .scale(scale)
            .alpha(if (category.isPlayable) 1f else 0.55f)
            .clip(RqShapes.Large)
            // Metnin oturduğu sol üst bölge koyu tonda → beyaz metin okunur.
            .background(
                Brush.linearGradient(
                    0f to palette.strong,
                    0.45f to palette.strong,
                    1f to palette.base,
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = category.isPlayable,
                onClick = onClick,
            )
            .clearAndSetSemantics {
                contentDescription = a11y
                role = Role.Button
            }
            .heightIn(min = 168.dp),
    ) {
        // Dekoratif parlama
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = 55.dp.toPx(),
                center = Offset(size.width + 30.dp.toPx() - 55.dp.toPx(), size.height + 30.dp.toPx() - 55.dp.toPx()),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 168.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RqShapes.Medium)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(categoryIcon(category.icon)),
                    contentDescription = null,
                    tint = RqColors.TextOnColor,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                category.name,
                style = MaterialTheme.typography.titleLarge,
                color = RqColors.TextOnColor,
            )
            if (category.description.isNotBlank()) {
                Text(
                    category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = RqColors.TextOnColor.copy(alpha = 0.92f),
                )
            }
            Spacer(Modifier.weight(1f, fill = false))
            Text(
                if (category.isPlayable) {
                    stringResource(R.string.category_questions, category.questionsPerSession)
                } else {
                    stringResource(R.string.category_preparing)
                },
                style = MaterialTheme.typography.labelMedium,
                color = RqColors.TextOnColor,
                modifier = Modifier
                    .clip(RqShapes.Pill)
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }
}
