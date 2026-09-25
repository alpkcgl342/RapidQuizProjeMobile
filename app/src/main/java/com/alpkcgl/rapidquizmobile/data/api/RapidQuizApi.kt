package com.alpkcgl.rapidquizmobile.data.api

import com.alpkcgl.rapidquizmobile.data.model.AnswerRequest
import com.alpkcgl.rapidquizmobile.data.model.AnswerResponse
import com.alpkcgl.rapidquizmobile.data.model.Category
import com.alpkcgl.rapidquizmobile.data.model.CategoryList
import com.alpkcgl.rapidquizmobile.data.model.CreateSessionRequest
import com.alpkcgl.rapidquizmobile.data.model.CreateSessionResponse
import com.alpkcgl.rapidquizmobile.data.model.CurrentQuestionResponse
import com.alpkcgl.rapidquizmobile.data.model.ErrorEnvelope
import com.alpkcgl.rapidquizmobile.data.model.LeaderboardResponse
import com.alpkcgl.rapidquizmobile.data.model.LeaderboardRow
import com.alpkcgl.rapidquizmobile.data.model.SubmitScoreRequest
import com.alpkcgl.rapidquizmobile.data.model.SubmitScoreResponse
import com.alpkcgl.rapidquizmobile.data.model.Summary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Backend `/api/v1/` uç noktaları. Tüm hatalar [ApiException] olarak fırlatılır. */
class RapidQuizApi(private val http: HttpClient) {

    suspend fun getCategories(): List<Category> =
        request<CategoryList> { http.get("categories/") }.results

    suspend fun createSession(categorySlug: String): CreateSessionResponse =
        request { http.post("quiz-sessions/") { setBody(CreateSessionRequest(categorySlug)) } }

    suspend fun submitAnswer(
        sessionId: String,
        token: String,
        questionId: Long,
        selectedOptionId: Long?,
    ): AnswerResponse = request {
        http.post("quiz-sessions/$sessionId/answers/") {
            sessionToken(token)
            setBody(AnswerRequest(questionId, selectedOptionId))
        }
    }

    suspend fun getCurrentQuestion(sessionId: String, token: String): CurrentQuestionResponse =
        request { http.get("quiz-sessions/$sessionId/current-question/") { sessionToken(token) } }

    suspend fun getSummary(sessionId: String, token: String): Summary =
        request { http.get("quiz-sessions/$sessionId/summary/") { sessionToken(token) } }

    suspend fun submitScore(sessionId: String, token: String, nickname: String): SubmitScoreResponse =
        request {
            http.post("quiz-sessions/$sessionId/submit-score/") {
                sessionToken(token)
                setBody(SubmitScoreRequest(nickname))
            }
        }

    /** [categorySlug] null ise genel tablo. */
    suspend fun getLeaderboard(categorySlug: String?, limit: Int): List<LeaderboardRow> =
        request<LeaderboardResponse> {
            http.get("leaderboard/") {
                if (categorySlug != null) parameter("category", categorySlug)
                parameter("limit", limit)
            }
        }.results

    private fun HttpRequestBuilder.sessionToken(token: String) {
        header(HttpClientFactory.HEADER_SESSION_TOKEN, token)
    }

    private suspend inline fun <reified T> request(block: () -> HttpResponse): T {
        val response = try {
            block()
        } catch (e: CancellationException) {
            // Ktor zaman aşımını da CancellationException olarak fırlatır: çağıran iptal
            // edilmediyse bu bir ağ hatasıdır.
            currentCoroutineContext().ensureActive()
            throw ApiException.network(e)
        } catch (e: Exception) {
            throw ApiException.network(e)
        }
        if (!response.status.isSuccess()) throw response.toApiException()
        return try {
            response.body<T>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ApiException.unexpected(response.status.value, e)
        }
    }

    private suspend fun HttpResponse.toApiException(): ApiException {
        val status = status.value
        return try {
            val error = ApiJson.decodeFromString<ErrorEnvelope>(bodyAsText()).error
            ApiException(status, error.code, error.message, error.details)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiException.unexpected(status, e)
        }
    }
}
