package com.alpkcgl.rapidquizmobile.data.repository

import com.alpkcgl.rapidquizmobile.core.QuizUiConfig
import com.alpkcgl.rapidquizmobile.data.api.ApiException
import com.alpkcgl.rapidquizmobile.data.api.RapidQuizApi
import com.alpkcgl.rapidquizmobile.data.model.AnswerResult
import com.alpkcgl.rapidquizmobile.data.model.CategoryRef
import com.alpkcgl.rapidquizmobile.data.model.Entry
import com.alpkcgl.rapidquizmobile.data.model.ServedQuestion
import com.alpkcgl.rapidquizmobile.data.model.SessionProgress
import com.alpkcgl.rapidquizmobile.data.model.Summary
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Oturum durum makinesi — web `stores/quiz.ts`'in karşılığı.
 *
 *   IDLE → PREPARING → QUESTION → ANSWERING → FEEDBACK → QUESTION … → FINISHED → SUBMITTED
 *                          ↑            │ ağ hatası
 *                          └─ resync ── DISCONNECTED
 *
 * Süre sunucu otoritelidir; istemci yalnızca gösterir. Sunucu zamanları `clockSkewMs` ile
 * yerel saate çevrilir (yerel = sunucu − skew). Oturum birden çok ekran boyunca sürdüğü için
 * uygulama ömrü boyunca tek örnek olarak `AppContainer`'da yaşar. Token yalnızca bellekte tutulur.
 *
 * Tüm çağrılar ana iş parçacığından yapılmalıdır (durum kontrolü ile geçiş arasında askıya alma yok).
 */
class QuizSessionManager(
    private val api: RapidQuizApi,
    private val clock: () -> Long = System::currentTimeMillis,
    private val retryDelayMs: Long = QuizUiConfig.RETRY_DELAY_MS,
) {
    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state.asStateFlow()

    /** Oturum düştüğünde ana ekranda gösterilecek uyarı; ana ekran okuyunca temizler. */
    private val _lostNotice = MutableStateFlow<SessionLostReason?>(null)
    val lostNotice: StateFlow<SessionLostReason?> = _lostNotice.asStateFlow()

    private var token: String? = null
    private var clockSkewMs = 0L
    private var pendingQuestion: ServedQuestion? = null

    /** reset() sonrası gelen eski yanıtları yok saymak için. */
    private var generation = 0

    fun reset() {
        generation++
        token = null
        clockSkewMs = 0L
        pendingQuestion = null
        _state.value = QuizState()
    }

    /** Ana ekrana dönüşte: biten oturum (skor tablosu vurgusu için) korunur, gerisi sıfırlanır. */
    fun resetIfNotOver() {
        if (!_state.value.isOver) reset()
    }

    fun consumeLostNotice() {
        _lostNotice.value = null
    }

    suspend fun startSession(categorySlug: String): Boolean {
        reset()
        val gen = generation
        _state.value = QuizState(status = QuizStatus.PREPARING)
        return try {
            val res = api.createSession(categorySlug)
            if (gen != generation) return false
            val receivedAt = clock()
            token = res.session.token
            // İlk sorunun served_at'i yanıtın üretildiği an; aradaki fark saat sapması + ağ
            // gecikmesidir. Böylece sayaç, yanıt elimize ulaştığı anda tam süreyle başlar.
            clockSkewMs = parseInstant(res.question.servedAt) - receivedAt
            _state.update {
                it.copy(
                    sessionId = res.session.id,
                    category = res.session.category,
                    totalQuestions = res.session.totalQuestions,
                )
            }
            showQuestion(res.question)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (gen == generation) fail(e)
            false
        }
    }

    /**
     * Cevap gönderir; [optionId] null ise süre doldu. [questionId] verilirse yalnızca o soru hâlâ
     * gösteriliyorsa gönderilir (geç tetiklenen zamanlayıcıya karşı).
     */
    suspend fun answer(optionId: Long?, questionId: Long? = null) {
        val current = _state.value
        val question = current.question ?: return
        val sessionId = current.sessionId ?: return
        val token = token ?: return
        if (current.status != QuizStatus.QUESTION) return // çift dokunma / geç dokunma koruması
        if (questionId != null && questionId != question.id) return
        val gen = generation
        _state.update { it.copy(status = QuizStatus.ANSWERING, selectedOptionId = optionId) }
        try {
            val res = withRetry { api.submitAnswer(sessionId, token, question.id, optionId) }
            if (gen != generation) return
            pendingQuestion = res.nextQuestion
            val now = clock()
            val target = res.nextQuestion?.let { toLocal(it.servedAt) }
                ?: (now + QuizUiConfig.FEEDBACK_FALLBACK_MS)
            val feedbackUntil = target.coerceIn(
                now + QuizUiConfig.FEEDBACK_MIN_MS,
                now + QuizUiConfig.FEEDBACK_MAX_MS,
            )
            _state.update {
                it.applyProgress(res.session).copy(
                    status = QuizStatus.FEEDBACK,
                    lastResult = res.result,
                    summary = res.summary ?: it.summary,
                    feedbackUntilLocalMs = feedbackUntil,
                    error = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (gen != generation) return
            val code = (e as? ApiException)?.code
            if (code == ApiException.ALREADY_ANSWERED || code == ApiException.SESSION_ALREADY_FINISHED) {
                // Çift gönderim veya sunucunun soruyu zaten kapatması: sessizce eşitle.
                resync()
            } else {
                fail(e)
            }
        }
    }

    /** Geri bildirim bitince sonraki soruya ya da sonuca geçer. */
    fun advance() {
        if (_state.value.status != QuizStatus.FEEDBACK) return
        val next = pendingQuestion
        pendingQuestion = null
        if (next != null) {
            showQuestion(next)
        } else {
            _state.update { it.copy(status = QuizStatus.FINISHED, feedbackUntilLocalMs = null) }
        }
    }

    /** Sunucudaki duruma eşitlenir (kopma, arka plandan dönüş, çift cevap). */
    suspend fun resync() {
        val sessionId = _state.value.sessionId ?: return
        val token = token ?: return
        val gen = generation
        _state.update { it.copy(error = null) }
        try {
            val res = api.getCurrentQuestion(sessionId, token)
            if (gen != generation) return
            _state.update { it.applyProgress(res.session) }
            pendingQuestion = null
            val question = res.question
            if (question != null) {
                showQuestion(question)
                return
            }
            val summary = api.getSummary(sessionId, token)
            if (gen != generation) return
            _state.update {
                it.copy(
                    status = if (summary.scoreSubmitted) QuizStatus.SUBMITTED else QuizStatus.FINISHED,
                    question = null,
                    summary = summary,
                    feedbackUntilLocalMs = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (gen == generation) fail(e)
        }
    }

    /** Sonuç ekranı için özet (cevap yanıtında gelmediyse). */
    suspend fun loadSummary() {
        val current = _state.value
        if (current.summary != null) return
        val sessionId = current.sessionId ?: return
        val token = token ?: return
        val gen = generation
        _state.update { it.copy(error = null) }
        try {
            val summary = api.getSummary(sessionId, token)
            if (gen == generation) _state.update { it.copy(summary = summary) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (gen == generation) fail(e)
        }
    }

    /**
     * Skoru kaydeder. Hata durumunda [ApiException] fırlatır (ekran mesajı gösterir);
     * `SCORE_ALREADY_SUBMITTED` durumu da `SUBMITTED` sayılır.
     */
    suspend fun submitScore(nickname: String): Entry {
        val sessionId = _state.value.sessionId
        val token = token
        if (sessionId == null || token == null) {
            throw ApiException(0, ApiException.INVALID_SESSION_TOKEN, "Aktif oturum yok.")
        }
        try {
            val entry = api.submitScore(sessionId, token, nickname).entry
            _state.update {
                it.copy(
                    status = QuizStatus.SUBMITTED,
                    submittedEntry = entry,
                    summary = it.summary?.copy(scoreSubmitted = true),
                )
            }
            return entry
        } catch (e: ApiException) {
            when {
                e.code == ApiException.SCORE_ALREADY_SUBMITTED ->
                    _state.update { it.copy(status = QuizStatus.SUBMITTED) }
                e.isSessionLost -> onSessionLost(e)
            }
            throw e
        }
    }

    private fun showQuestion(question: ServedQuestion) {
        _state.update {
            it.copy(
                status = QuizStatus.QUESTION,
                question = question,
                questionIndex = question.index,
                deadlineAtLocalMs = toLocal(question.deadlineAt),
                timeLimitMs = question.timeLimitMs,
                selectedOptionId = null,
                lastResult = null,
                feedbackUntilLocalMs = null,
                error = null,
            )
        }
    }

    private fun fail(e: Exception) {
        val error = e as? ApiException ?: ApiException.unexpected(0, e)
        if (error.isSessionLost) {
            onSessionLost(error)
            return
        }
        _state.update {
            it.copy(
                status = if (error.isNetwork && it.sessionId != null) QuizStatus.DISCONNECTED else QuizStatus.ERROR,
                error = error,
            )
        }
    }

    /** Token geçersiz / oturum süresi dolmuş: oturumu bırak, ana ekrana uyarıyla dön. */
    private fun onSessionLost(error: ApiException) {
        reset()
        _lostNotice.value =
            if (error.code == ApiException.SESSION_EXPIRED) SessionLostReason.EXPIRED else SessionLostReason.NOT_FOUND
    }

    private suspend fun <T> withRetry(block: suspend () -> T): T = try {
        block()
    } catch (e: ApiException) {
        if (!e.isNetwork) throw e
        delay(retryDelayMs)
        block()
    }

    private fun toLocal(serverIso: String): Long = parseInstant(serverIso) - clockSkewMs

    private fun parseInstant(iso: String): Long = Instant.parse(iso).toEpochMilli()

    private fun QuizState.applyProgress(p: SessionProgress) = copy(
        score = p.score,
        correctCount = p.correctCount,
        wrongCount = p.wrongCount,
        timeoutCount = p.timeoutCount,
    )
}

enum class QuizStatus { IDLE, PREPARING, QUESTION, ANSWERING, FEEDBACK, DISCONNECTED, FINISHED, SUBMITTED, ERROR }

enum class SessionLostReason { EXPIRED, NOT_FOUND }

data class QuizState(
    val status: QuizStatus = QuizStatus.IDLE,
    val sessionId: String? = null,
    val category: CategoryRef? = null,
    val question: ServedQuestion? = null,
    val questionIndex: Int = 0,
    val totalQuestions: Int = QuizUiConfig.DEFAULT_TOTAL_QUESTIONS,
    val timeLimitMs: Long = QuizUiConfig.DEFAULT_TIME_LIMIT_MS,
    val score: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val timeoutCount: Int = 0,
    /** Cevap gönderilirken seçilen şık (null = süre doldu). */
    val selectedOptionId: Long? = null,
    val lastResult: AnswerResult? = null,
    val summary: Summary? = null,
    val submittedEntry: Entry? = null,
    /** Mevcut sorunun yerel saatle bitişi (epoch ms). */
    val deadlineAtLocalMs: Long? = null,
    /** Geri bildirimin yerel saatle bitişi (epoch ms). */
    val feedbackUntilLocalMs: Long? = null,
    val error: ApiException? = null,
) {
    val isPlaying: Boolean
        get() = status in setOf(QuizStatus.QUESTION, QuizStatus.ANSWERING, QuizStatus.FEEDBACK, QuizStatus.DISCONNECTED)

    val isOver: Boolean
        get() = status == QuizStatus.FINISHED || status == QuizStatus.SUBMITTED
}
