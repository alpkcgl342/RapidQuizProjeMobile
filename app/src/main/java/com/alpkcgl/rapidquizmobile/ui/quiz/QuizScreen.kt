package com.alpkcgl.rapidquizmobile.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.rapidquizmobile.R
import com.alpkcgl.rapidquizmobile.data.model.Outcome
import com.alpkcgl.rapidquizmobile.data.model.ServedQuestion
import com.alpkcgl.rapidquizmobile.data.repository.QuizState
import com.alpkcgl.rapidquizmobile.data.repository.QuizStatus
import com.alpkcgl.rapidquizmobile.ui.components.ErrorView
import com.alpkcgl.rapidquizmobile.ui.components.RqCard
import com.alpkcgl.rapidquizmobile.ui.components.SecondaryButton
import com.alpkcgl.rapidquizmobile.ui.components.formatNumber
import com.alpkcgl.rapidquizmobile.ui.components.userMessage
import com.alpkcgl.rapidquizmobile.ui.theme.RqColors
import com.alpkcgl.rapidquizmobile.ui.theme.TABULAR_NUMS
import com.alpkcgl.rapidquizmobile.ui.theme.categoryPalette

@Composable
fun QuizScreen(
    onFinished: () -> Unit,
    onHome: () -> Unit,
    viewModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmExit by rememberSaveable { mutableStateOf(false) }

    // Oyun bitti → sonuç; oturum yok (çıkış, düşme, süreç ölümü) → ana ekran.
    LaunchedEffect(state.status, state.sessionId) {
        when {
            state.isOver -> onFinished()
            state.sessionId == null && state.status == QuizStatus.IDLE -> onHome()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onForeground() }

    BackHandler(enabled = state.isPlaying) { confirmExit = true }

    val timedOut = state.status == QuizStatus.FEEDBACK && state.lastResult?.outcome == Outcome.TIMEOUT
    val background by animateColorAsState(
        if (timedOut) RqColors.WarningSoft else RqColors.Background,
        tween(240),
        label = "quizBackground",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            TopBar(state, onExit = { if (state.isPlaying) confirmExit = true else onHome() })
            Spacer(Modifier.height(16.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Body(state, viewModel, onHome)
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.quiz_exit_title)) },
            text = { Text(stringResource(R.string.quiz_exit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    viewModel.exit()
                }) { Text(stringResource(R.string.quiz_exit_confirm), color = RqColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.quiz_exit_cancel)) }
            },
            containerColor = RqColors.Surface,
        )
    }
}

@Composable
private fun TopBar(state: QuizState, onExit: () -> Unit) {
    val scoreText = formatNumber(state.score)
    val scoreA11y = stringResource(R.string.quiz_score_a11y, scoreText)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onExit) {
            Icon(
                painterResource(R.drawable.ic_x),
                contentDescription = stringResource(R.string.quiz_exit),
                tint = RqColors.Text,
            )
        }
        Spacer(Modifier.width(8.dp))
        QuestionProgress(state.questionIndex, state.totalQuestions, Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        Text(
            scoreText,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = TABULAR_NUMS),
            color = RqColors.Primary,
            modifier = Modifier
                .widthIn(min = 48.dp)
                .semantics { contentDescription = scoreA11y },
        )
    }
}

@Composable
private fun Body(state: QuizState, viewModel: QuizViewModel, onHome: () -> Unit) {
    when (state.status) {
        QuizStatus.DISCONNECTED -> Panel {
            ErrorView(
                title = stringResource(R.string.quiz_disconnected_title),
                message = stringResource(R.string.quiz_disconnected_message),
                retryLabel = stringResource(R.string.quiz_disconnected_action),
                onRetry = viewModel::resync,
            )
        }
        QuizStatus.ERROR -> Panel {
            ErrorView(
                title = stringResource(R.string.error_generic_title),
                message = state.error?.userMessage(),
                onRetry = viewModel::resync,
            ) {
                SecondaryButton(stringResource(R.string.action_home), onClick = onHome, modifier = Modifier.fillMaxWidth())
            }
        }
        else -> state.question?.let { question -> QuestionContent(state, question, viewModel::choose) }
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    RqCard(Modifier.fillMaxWidth().padding(top = 24.dp)) { content() }
}

@Composable
private fun QuestionContent(state: QuizState, question: ServedQuestion, onChoose: (Long) -> Unit) {
    val remaining = rememberRemainingMs(
        deadlineAtLocalMs = state.deadlineAtLocalMs,
        totalMs = state.timeLimitMs,
        running = state.status == QuizStatus.QUESTION,
    )
    val accent = state.category?.let { categoryPalette(it.slug, it.colorHex).base } ?: RqColors.Primary

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CountdownRing(remaining, state.timeLimitMs)
        Spacer(Modifier.height(20.dp))
        AnimatedContent(
            targetState = question,
            contentKey = { it.id },
            transitionSpec = {
                (slideInHorizontally(tween(240)) { it / 4 } + fadeIn(tween(240)))
                    .togetherWith(slideOutHorizontally(tween(160)) { -it / 4 } + fadeOut(tween(160)))
            },
            label = "question",
        ) { q ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    q.text,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
                q.options.forEach { option ->
                    OptionButton(
                        label = option.label,
                        text = option.text,
                        state = optionState(state, q, option.id),
                        accent = accent,
                        onClick = { onChoose(option.id) },
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            val result = state.lastResult
            if (state.status == QuizStatus.FEEDBACK && result != null) {
                // Her soruda yeniden oynasın
                key(state.questionIndex) {
                    FeedbackOverlay(result.outcome, result.pointsEarned, Modifier.size(width = 260.dp, height = 110.dp))
                }
            }
        }
    }
}

private fun optionState(state: QuizState, question: ServedQuestion, optionId: Long): OptionState {
    // Geçiş animasyonunda eski soru çizilirken durumlar karışmasın
    if (question.id != state.question?.id) return OptionState.DISABLED
    val result = state.lastResult
    return when (state.status) {
        QuizStatus.FEEDBACK -> when {
            result == null -> OptionState.DISABLED
            optionId == result.correctOptionId -> OptionState.CORRECT
            optionId == result.selectedOptionId -> OptionState.WRONG
            else -> OptionState.DISABLED
        }
        QuizStatus.ANSWERING -> if (optionId == state.selectedOptionId) OptionState.SELECTED else OptionState.DISABLED
        QuizStatus.QUESTION -> OptionState.IDLE
        else -> OptionState.DISABLED
    }
}
