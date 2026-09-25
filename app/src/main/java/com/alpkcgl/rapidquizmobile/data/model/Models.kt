package com.alpkcgl.rapidquizmobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Backend openapi.yaml şemalarının Kotlin karşılıkları. JSON snake_case, Kotlin camelCase.

// --- Katalog ---

@Serializable
data class Category(
    val slug: String,
    val name: String,
    val description: String = "",
    @SerialName("color_hex") val colorHex: String,
    val icon: String,
    @SerialName("questions_per_session") val questionsPerSession: Int,
    @SerialName("available_question_count") val availableQuestionCount: Int = 0,
    @SerialName("is_playable") val isPlayable: Boolean,
)

@Serializable
data class CategoryList(val results: List<Category>)

/** Diğer yanıtlara gömülen kısa kategori bilgisi. */
@Serializable
data class CategoryRef(
    val slug: String,
    val name: String,
    @SerialName("color_hex") val colorHex: String,
)

// --- Oturum ve sorular ---

@Serializable
data class ServedOption(val id: Long, val label: String, val text: String)

@Serializable
data class ServedQuestion(
    val id: Long,
    val index: Int,
    val text: String,
    val options: List<ServedOption>,
    @SerialName("time_limit_ms") val timeLimitMs: Long,
    @SerialName("served_at") val servedAt: String,
    @SerialName("deadline_at") val deadlineAt: String,
)

@Serializable
data class Session(
    val id: String,
    val token: String,
    val category: CategoryRef,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("current_index") val currentIndex: Int,
    val score: Int,
    val status: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class CreateSessionRequest(val category: String)

@Serializable
data class CreateSessionResponse(val session: Session, val question: ServedQuestion)

@Serializable
data class SessionProgress(
    @SerialName("current_index") val currentIndex: Int,
    val score: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("wrong_count") val wrongCount: Int,
    @SerialName("timeout_count") val timeoutCount: Int,
    val status: String,
)

@Serializable
enum class Outcome {
    @SerialName("correct") CORRECT,
    @SerialName("wrong") WRONG,
    @SerialName("timeout") TIMEOUT,
}

@Serializable
data class AnswerRequest(
    @SerialName("question_id") val questionId: Long,
    /** null = süre doldu (cevapsız). */
    @SerialName("selected_option_id") val selectedOptionId: Long?,
)

@Serializable
data class AnswerResult(
    val outcome: Outcome,
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("correct_option_id") val correctOptionId: Long,
    @SerialName("selected_option_id") val selectedOptionId: Long? = null,
    @SerialName("elapsed_ms") val elapsedMs: Long,
    @SerialName("points_earned") val pointsEarned: Int,
    val explanation: String = "",
)

@Serializable
data class AnswerResponse(
    val result: AnswerResult,
    val session: SessionProgress,
    @SerialName("next_question") val nextQuestion: ServedQuestion? = null,
    /** Yalnızca son soruda bulunur. */
    val summary: Summary? = null,
)

@Serializable
data class CurrentQuestionResponse(
    val question: ServedQuestion? = null,
    val session: SessionProgress,
)

@Serializable
data class Summary(
    @SerialName("session_id") val sessionId: String,
    val category: CategoryRef,
    val status: String,
    val score: Int,
    @SerialName("max_possible_score") val maxPossibleScore: Int,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("wrong_count") val wrongCount: Int,
    @SerialName("timeout_count") val timeoutCount: Int,
    @SerialName("accuracy_pct") val accuracyPct: Double,
    @SerialName("average_elapsed_ms") val averageElapsedMs: Long? = null,
    @SerialName("fastest_correct_ms") val fastestCorrectMs: Long? = null,
    @SerialName("total_elapsed_ms") val totalElapsedMs: Long = 0,
    @SerialName("score_submitted") val scoreSubmitted: Boolean,
    @SerialName("estimated_rank") val estimatedRank: Int,
    @SerialName("finished_at") val finishedAt: String? = null,
)

// --- Skor ---

@Serializable
data class SubmitScoreRequest(val nickname: String)

@Serializable
data class Entry(
    val id: Long,
    val nickname: String,
    val score: Int,
    val category: CategoryRef,
    @SerialName("rank_in_category") val rankInCategory: Int,
    @SerialName("rank_overall") val rankOverall: Int,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class SubmitScoreResponse(val entry: Entry)

@Serializable
data class LeaderboardRow(
    val id: Long,
    val rank: Int,
    val nickname: String,
    val score: Int,
    @SerialName("correct_count") val correctCount: Int = 0,
    val category: CategoryRef,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class LeaderboardResponse(val results: List<LeaderboardRow>)

// --- Hata ---

@Serializable
data class ErrorEnvelope(val error: ErrorBody)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val details: JsonObject = JsonObject(emptyMap()),
)
