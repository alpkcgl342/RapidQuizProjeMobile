package com.alpkcgl.rapidquizmobile.ui.nickname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.rapidquizmobile.core.Nickname
import com.alpkcgl.rapidquizmobile.data.api.ApiException
import com.alpkcgl.rapidquizmobile.data.repository.QuizSessionManager
import com.alpkcgl.rapidquizmobile.data.repository.QuizState
import com.alpkcgl.rapidquizmobile.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NicknameUiState(
    val nickname: String = "",
    /** Kullanıcı en az bir kez kaydetmeyi denedi → istemci hatası gösterilir. */
    val touched: Boolean = false,
    val serverError: String? = null,
    val serverException: ApiException? = null,
    val saving: Boolean = false,
    /** Kaydedildi (veya zaten kaydedilmişti) → skor tablosuna geç. */
    val done: Boolean = false,
) {
    val clientError: Nickname.Error? get() = if (touched) Nickname.validate(nickname) else null
}

class NicknameViewModel(private val session: QuizSessionManager) : ViewModel() {

    private val _state = MutableStateFlow(NicknameUiState())
    val state: StateFlow<NicknameUiState> = _state.asStateFlow()

    val quiz: StateFlow<QuizState> = session.state

    fun onNicknameChange(value: String) {
        _state.update {
            it.copy(
                nickname = value.take(Nickname.MAX_LENGTH),
                serverError = null,
                serverException = null,
            )
        }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return // çift gönderim yok
        _state.update { it.copy(touched = true, serverError = null, serverException = null) }
        if (Nickname.validate(current.nickname) != null) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                session.submitScore(Nickname.normalize(current.nickname))
                _state.update { it.copy(saving = false, done = true) }
            } catch (e: ApiException) {
                _state.update {
                    when {
                        e.code == ApiException.SCORE_ALREADY_SUBMITTED -> it.copy(saving = false, done = true)
                        else -> it.copy(
                            saving = false,
                            serverError = e.fieldError("nickname"),
                            serverException = e.takeIf { ex -> ex.fieldError("nickname") == null },
                        )
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { NicknameViewModel(appContainer().quizSession) }
        }
    }
}
