package com.alpkcgl.rapidquizmobile.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.rapidquizmobile.R
import com.alpkcgl.rapidquizmobile.data.model.Summary
import com.alpkcgl.rapidquizmobile.data.repository.QuizStatus
import com.alpkcgl.rapidquizmobile.ui.components.AppLogo
import com.alpkcgl.rapidquizmobile.ui.components.ErrorView
import com.alpkcgl.rapidquizmobile.ui.components.LoadingView
import com.alpkcgl.rapidquizmobile.ui.components.PrimaryButton
import com.alpkcgl.rapidquizmobile.ui.components.RqCard
import com.alpkcgl.rapidquizmobile.ui.components.SecondaryButton
import com.alpkcgl.rapidquizmobile.ui.components.formatNumber
import com.alpkcgl.rapidquizmobile.ui.components.formatSeconds
import com.alpkcgl.rapidquizmobile.ui.components.htmlString
import com.alpkcgl.rapidquizmobile.ui.components.userMessage
import com.alpkcgl.rapidquizmobile.ui.theme.RqColors
import com.alpkcgl.rapidquizmobile.ui.theme.RqShapes
import com.alpkcgl.rapidquizmobile.ui.theme.TABULAR_NUMS
import com.alpkcgl.rapidquizmobile.ui.theme.categoryPalette
import kotlin.math.roundToInt

@Composable
fun ResultScreen(
    onSaveScore: () -> Unit,
    onPlayAgain: (categorySlug: String) -> Unit,
    onLeaderboard: (categorySlug: String?) -> Unit,
    onHome: () -> Unit,
    viewModel: ResultViewModel = viewModel(factory = ResultViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Oturum yoksa (süreç ölümü, oturum düşmesi) gösterilecek sonuç da yok.
    LaunchedEffect(state.sessionId, state.status) {
        if (state.sessionId == null && state.status == QuizStatus.IDLE) onHome()
    }

    val summary = state.summary
    Column(
        Modifier
            .fillMaxSize()
            .background(RqColors.Background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppLogo(Modifier.padding(vertical = 8.dp))
        val error = state.error
        when {
            summary != null -> {
                SummaryCard(summary)
                Actions(
                    summary = summary,
                    onSaveScore = onSaveScore,
                    onPlayAgain = { onPlayAgain(summary.category.slug) },
                    onLeaderboard = { onLeaderboard(summary.category.slug) },
                )
            }
            error != null -> ErrorView(
                title = stringResource(R.string.error_generic_title),
                message = error.userMessage(),
                onRetry = viewModel::loadSummary,
            )
            else -> LoadingView(stringResource(R.string.result_loading))
        }
    }
}

@Composable
private fun SummaryCard(summary: Summary) {
    val palette = categoryPalette(summary.category.slug, summary.category.colorHex)
    RqCard(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                summary.category.name,
                style = MaterialTheme.typography.labelMedium,
                color = RqColors.TextOnColor,
                modifier = Modifier
                    .clip(RqShapes.Pill)
                    .background(palette.strong)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Text(
                stringResource(headlineFor(summary)),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            ScoreCounter(summary.score)
            Text(
                stringResource(R.string.result_max, formatNumber(summary.maxPossibleScore)),
                style = MaterialTheme.typography.bodyMedium,
                color = RqColors.TextMuted,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatTile(stringResource(R.string.result_correct), summary.correctCount, RqColors.SuccessStrong, RqColors.SuccessSoft, Modifier.weight(1f))
                StatTile(stringResource(R.string.result_wrong), summary.wrongCount, RqColors.Danger, RqColors.DangerSoft, Modifier.weight(1f))
                StatTile(stringResource(R.string.result_missed), summary.timeoutCount, RqColors.WarningText, RqColors.WarningSoft, Modifier.weight(1f))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Fact(
                    stringResource(R.string.result_accuracy),
                    stringResource(R.string.result_accuracy_value, summary.accuracyPct.roundToInt()),
                    Modifier.weight(1f),
                )
                Fact(
                    stringResource(R.string.result_avg_time),
                    summary.averageElapsedMs?.let { stringResource(R.string.result_avg_time_value, formatSeconds(it)) } ?: "—",
                    Modifier.weight(1f),
                )
            }
            Text(
                if (summary.scoreSubmitted) {
                    htmlString(R.string.result_saved)
                } else {
                    htmlString(R.string.result_estimated_rank, summary.estimatedRank)
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** 0'dan puana sayan animasyon (1.2 sn, ease-out). */
@Composable
private fun ScoreCounter(value: Int) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animated.animateTo(value.toFloat(), tween(1200, easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)))
    }
    val a11y = stringResource(R.string.result_score_a11y, formatNumber(value))
    Text(
        formatNumber(animated.value.roundToInt()),
        style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = TABULAR_NUMS, fontSize = 64.sp),
        color = RqColors.Primary,
        modifier = Modifier.clearAndSetSemantics { contentDescription = a11y },
    )
}

@Composable
private fun StatTile(label: String, value: Int, color: Color, background: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RqShapes.Medium)
            .background(background)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "$value",
            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = TABULAR_NUMS),
            color = color,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = RqColors.TextMuted)
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = RqColors.TextMuted)
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TABULAR_NUMS))
    }
}

@Composable
private fun Actions(
    summary: Summary,
    onSaveScore: () -> Unit,
    onPlayAgain: () -> Unit,
    onLeaderboard: () -> Unit,
) {
    Column(
        Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!summary.scoreSubmitted) {
            PrimaryButton(
                stringResource(R.string.result_save),
                onClick = onSaveScore,
                icon = R.drawable.ic_save,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(
                stringResource(R.string.action_play_again),
                onClick = onPlayAgain,
                icon = R.drawable.ic_rotate_ccw,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                stringResource(R.string.action_leaderboard),
                onClick = onLeaderboard,
                icon = R.drawable.ic_trophy,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
