package com.alpkcgl.rapidquizmobile.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.rapidquizmobile.data.repository.QuizSessionManager
import com.alpkcgl.rapidquizmobile.data.repository.QuizState
import com.alpkcgl.rapidquizmobile.data.repository.QuizStatus
import com.alpkcgl.rapidquizmobile.ui.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Soru ekranı. Oyun durumu [QuizSessionManager]'dadır; bu ViewModel yalnızca iki zamanlayıcıyı
 * yönetir: süre dolunca cevapsız gönderim ve geri bildirim bitince sonraki soruya geçiş.
 * ViewModel ekran döndürmede yaşadığı için zamanlayıcılar kesilmez.
 */
class QuizViewModel(
    private val session: QuizSessionManager,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val state: StateFlow<QuizState> = session.state

    /**
     * Uygulama öne her geldiğinde artar ve zamanlayıcıları duvar saatine göre yeniden kurar.
     * (`delay` cihaz uykusunu saymaz; arka plandayken süre dolmuş olabilir.)
     */
    private val foregroundTick = MutableStateFlow(0)
    private var resyncJob: Job? = null

    init {
        viewModelScope.launch {
            combine(session.state, foregroundTick, ::timerKey)
                .distinctUntilChanged()
                .collectLatest { key -> key?.let { runTimer(it) } }
        }
    }

    fun choose(optionId: Long) {
        // Durum kontrolü QuizSessionManager.answer içinde (çift dokunma koruması).
        viewModelScope.launch { session.answer(optionId) }
    }

    /** "Devam et" / "Tekrar dene": sunucuyla eşitle. */
    fun resync() {
        if (resyncJob?.isActive == true) return
        resyncJob = viewModelScope.launch { session.resync() }
    }

    fun onForeground() {
        foregroundTick.value++
    }

    /** Onaylı çıkış: skor kaydedilmez. */
    fun exit() = session.reset()

    private sealed interface TimerKey {
        data class Expire(val questionId: Long, val deadline: Long, val tick: Int) : TimerKey
        data class Feedback(val until: Long, val tick: Int) : TimerKey
    }

    private fun timerKey(s: QuizState, tick: Int): TimerKey? =
        when (s.status) {
            QuizStatus.QUESTION -> {
                val question = s.question
                val deadline = s.deadlineAtLocalMs
                if (question != null && deadline != null) TimerKey.Expire(question.id, deadline, tick) else null
            }
            QuizStatus.FEEDBACK -> TimerKey.Feedback(s.feedbackUntilLocalMs ?: clock(), tick)
            else -> null
        }

    private suspend fun runTimer(key: TimerKey) {
        when (key) {
            is TimerKey.Expire -> {
                delay((key.deadline - clock()).coerceAtLeast(0))
                // Cevap isteği ayrı işte: durum değişince collectLatest bu bloğu iptal eder.
                viewModelScope.launch { session.answer(null, key.questionId) }
            }
            is TimerKey.Feedback -> {
                delay((key.until - clock()).coerceAtLeast(0))
                session.advance()
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { QuizViewModel(appContainer().quizSession) }
        }
    }
}
