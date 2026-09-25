package com.alpkcgl.rapidquizmobile.ui.nickname

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpkcgl.rapidquizmobile.R
import com.alpkcgl.rapidquizmobile.core.Nickname
import com.alpkcgl.rapidquizmobile.data.repository.QuizStatus
import com.alpkcgl.rapidquizmobile.ui.components.AppLogo
import com.alpkcgl.rapidquizmobile.ui.components.GhostButton
import com.alpkcgl.rapidquizmobile.ui.components.PrimaryButton
import com.alpkcgl.rapidquizmobile.ui.components.RqCard
import com.alpkcgl.rapidquizmobile.ui.components.formatNumber
import com.alpkcgl.rapidquizmobile.ui.components.htmlString
import com.alpkcgl.rapidquizmobile.ui.components.userMessage
import com.alpkcgl.rapidquizmobile.ui.theme.RqColors

@Composable
fun NicknameScreen(
    onDone: (categorySlug: String?) -> Unit,
    onHome: () -> Unit,
    viewModel: NicknameViewModel = viewModel(factory = NicknameViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val quiz by viewModel.quiz.collectAsStateWithLifecycle()
    val categorySlug = quiz.category?.slug ?: quiz.summary?.category?.slug

    LaunchedEffect(state.done) { if (state.done) onDone(categorySlug) }
    LaunchedEffect(quiz.sessionId, quiz.status) {
        if (quiz.sessionId == null && quiz.status == QuizStatus.IDLE) onHome()
    }

    val error = state.serverError
        ?: state.serverException?.userMessage()
        ?: when (state.clientError) {
            Nickname.Error.LENGTH -> stringResource(R.string.nickname_error_length)
            Nickname.Error.CHARACTERS -> stringResource(R.string.nickname_error_chars)
            null -> null
        }

    Column(
        Modifier
            .fillMaxSize()
            .background(RqColors.Background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppLogo(Modifier.padding(vertical = 8.dp))
        RqCard(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                quiz.summary?.let { summary ->
                    Text(
                        htmlString(R.string.nickname_score, formatNumber(summary.score)),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = viewModel::onNicknameChange,
                    label = { Text(stringResource(R.string.nickname_label)) },
                    supportingText = { Text(error ?: stringResource(R.string.nickname_hint)) },
                    isError = error != null,
                    singleLine = true,
                    enabled = !state.saving,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.save() }),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = RqColors.Border,
                        focusedBorderColor = RqColors.Primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton(
                    stringResource(R.string.nickname_save),
                    onClick = viewModel::save,
                    loading = state.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        GhostButton(
            stringResource(R.string.nickname_skip),
            onClick = { onDone(categorySlug) },
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
        )
    }
}
