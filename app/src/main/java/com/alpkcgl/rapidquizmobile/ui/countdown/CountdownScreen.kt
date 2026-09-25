package com.alpkcgl.rapidquizmobile.ui.countdown

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.rapidquizmobile.R
import com.alpkcgl.rapidquizmobile.core.QuizUiConfig
import com.alpkcgl.rapidquizmobile.data.api.ApiException
import com.alpkcgl.rapidquizmobile.ui.components.ErrorView
import com.alpkcgl.rapidquizmobile.ui.components.RqCard
import com.alpkcgl.rapidquizmobile.ui.components.SecondaryButton
import com.alpkcgl.rapidquizmobile.ui.components.userMessage
import com.alpkcgl.rapidquizmobile.ui.theme.RqColors
import com.alpkcgl.rapidquizmobile.ui.theme.categoryPalette

@Composable
fun CountdownScreen(
    onStarted: () -> Unit,
    onHome: () -> Unit,
    viewModel: CountdownViewModel = viewModel(factory = CountdownViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.started) { if (state.started) onStarted() }
    LaunchedEffect(state.categoryMissing) { if (state.categoryMissing) onHome() }

    val palette = state.category?.let { categoryPalette(it.slug, it.colorHex) }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(palette?.strong ?: RqColors.PrimaryStrong, palette?.base ?: RqColors.Primary),
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            )
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val category = state.category ?: return@Box
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                category.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = RqColors.TextOnColor.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
            )
            val error = state.error
            if (error == null) {
                Stage(state.step)
                Text(
                    stringResource(R.string.countdown_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = RqColors.TextOnColor.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
            } else {
                StartError(error, onRetry = viewModel::retry, onHome = onHome)
            }
        }
    }
}

@Composable
private fun Stage(step: Int) {
    Box(
        Modifier
            .height(180.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val duration = if (targetState == 3) QuizUiConfig.COUNTDOWN_GO_MS else QuizUiConfig.COUNTDOWN_BEAT_MS
                (scaleIn(tween(duration.toInt() / 2), initialScale = 0.4f) + fadeIn(tween(duration.toInt() / 3)))
                    .togetherWith(fadeOut(tween(90)))
            },
            label = "countdownStep",
        ) { s ->
            if (s < 0) return@AnimatedContent
            val go = s == 3
            Text(
                if (go) stringResource(R.string.countdown_go) else "${3 - s}",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (go) 72.sp else 140.sp,
                    lineHeight = if (go) 76.sp else 140.sp,
                    shadow = Shadow(Color.Black.copy(alpha = 0.18f), Offset(0f, 8f), 30f),
                ),
                color = RqColors.TextOnColor,
            )
        }
    }
}

@Composable
private fun StartError(error: ApiException, onRetry: () -> Unit, onHome: () -> Unit) {
    val insufficient = error.code == ApiException.INSUFFICIENT_QUESTIONS
    RqCard(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
        ErrorView(
            title = if (insufficient) stringResource(R.string.countdown_insufficient) else error.userMessage(),
            onRetry = if (insufficient) null else onRetry,
        ) {
            SecondaryButton(
                stringResource(R.string.action_home),
                onClick = onHome,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
