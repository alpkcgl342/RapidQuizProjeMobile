package com.alpkcgl.rapidquizmobile.data.repository

import com.alpkcgl.rapidquizmobile.data.api.HttpClientFactory
import com.alpkcgl.rapidquizmobile.data.api.RapidQuizApi
import com.alpkcgl.rapidquizmobile.data.model.Outcome
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizSessionManagerTest {

    private val serverNow = Instant.parse("2026-09-26T10:00:00Z").toEpochMilli()

    /** Yerel saat sunucudan 3 sn ileride (saat sapması). */
    private var localNow = serverNow + 3_000

    private val requests = mutableListOf<HttpRequestData>()
    private var handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
        error("beklenmeyen istek: ${it.url}")
    }

    private val manager = QuizSessionManager(
        api = RapidQuizApi(
            HttpClientFactory.create(
                baseUrl = "http://test/api/v1/",
                clientVersion = "test",
                engine = MockEngine { request ->
                    requests += request
                    handler(request)
                },
            ),
        ),
        clock = { localNow },
        retryDelayMs = 0,
    )

    @Test
    fun `startSession sayaci yanitin ulastigi andan tam sureyle baslatir`() = runTest {
        handler = { json(createSession()) }

        assertTrue(manager.startSession("fizik"))

        val s = manager.state.value
        assertEquals(QuizStatus.QUESTION, s.status)
        assertEquals("sess-1", s.sessionId)
        assertEquals(localNow + 5_000, s.deadlineAtLocalMs)
        assertEquals("android", requests.single().headers[HttpClientFactory.HEADER_PLATFORM])
    }

    @Test
    fun `cevap geri bildirimi sonraki sorunun served_at anina kadar surer ve token gonderilir`() = runTest {
        handler = { json(createSession()) }
        manager.startSession("fizik")
        localNow += 1_000
        // Sonraki soru sunucuda 1200 ms sonra başlıyor
        handler = { json(answerResponse(outcome = "correct", points = 180, nextServedAt = serverNow + 2_200)) }

        manager.answer(optionId = 11)

        val s = manager.state.value
        assertEquals(QuizStatus.FEEDBACK, s.status)
        assertEquals(Outcome.CORRECT, s.lastResult?.outcome)
        assertEquals(180, s.score)
        assertEquals(localNow + 1_200, s.feedbackUntilLocalMs)
        assertEquals("tok", requests.last().headers[HttpClientFactory.HEADER_SESSION_TOKEN])
        assertTrue(requests.last().bodyText().contains("\"selected_option_id\":11"))

        manager.advance()
        assertEquals(QuizStatus.QUESTION, manager.state.value.status)
        assertEquals(2L, manager.state.value.question?.id)
    }

    @Test
    fun `sure dolunca selected_option_id null gonderilir`() = runTest {
        handler = { json(createSession()) }
        manager.startSession("fizik")
        handler = { json(answerResponse(outcome = "timeout", points = 0, nextServedAt = serverNow + 6_000)) }

        manager.answer(optionId = null, questionId = 1)

        assertTrue(requests.last().bodyText().contains("\"selected_option_id\":null"))
        assertEquals(Outcome.TIMEOUT, manager.state.value.lastResult?.outcome)
    }

    @Test
    fun `cevap verilirken ikinci dokunus ve eski sorunun zamanlayicisi yok sayilir`() = runTest {
        handler = { json(createSession()) }
        manager.startSession("fizik")
        handler = { json(answerResponse(outcome = "wrong", points = 0, nextServedAt = serverNow + 2_000)) }

        manager.answer(optionId = 12)
        manager.answer(optionId = 13) // FEEDBACK durumunda: yok sayılır
        manager.advance()
        manager.answer(optionId = null, questionId = 1) // eski sorunun zamanlayıcısı

        assertEquals(2, requests.size) // oturum + tek cevap
        assertEquals(QuizStatus.QUESTION, manager.state.value.status)
    }

    @Test
    fun `ag hatasinda bir kez tekrar dener, yine olmazsa baglanti koptu`() = runTest {
        handler = { json(createSession()) }
        manager.startSession("fizik")
        handler = { throw IOException("offline") }

        manager.answer(optionId = 11)

        assertEquals(3, requests.size) // oturum + 2 deneme
        assertEquals(QuizStatus.DISCONNECTED, manager.state.value.status)
    }

    @Test
    fun `ALREADY_ANSWERED gelirse sunucuyla esitlenir`() = runTest {
        handler = { json(createSession()) }
        manager.startSession("fizik")
        handler = { request ->
            if (request.url.encodedPath.endsWith("/answers/")) {
                error(409, "ALREADY_ANSWERED")
            } else {
                json("""{"question": ${question(id = 2, index = 1, servedAt = serverNow + 3_000)}, "session": ${progress(score = 150)}}""")
            }
        }

        manager.answer(optionId = 11)

        val s = manager.state.value
        assertTrue(requests.last().url.encodedPath.endsWith("/current-question/"))
        assertEquals(QuizStatus.QUESTION, s.status)
        assertEquals(2L, s.question?.id)
        assertEquals(150, s.score)
    }

    @Test
    fun `oturum suresi dolunca sifirlanir ve ana ekran uyarisi birakilir`() = runTest {
        handler = { json(createSession()) }
        manager.startSession("fizik")
        handler = { error(410, "SESSION_EXPIRED") }

        manager.answer(optionId = 11)

        assertEquals(QuizStatus.IDLE, manager.state.value.status)
        assertNull(manager.state.value.sessionId)
        assertEquals(SessionLostReason.EXPIRED, manager.lostNotice.value)
    }

    @Test
    fun `son sorudan sonra biter, skor kaydedilince SUBMITTED olur`() = runTest {
        handler = { json(createSession()) }
        manager.startSession("fizik")
        handler = { json(answerResponse(outcome = "correct", points = 200, nextServedAt = null, withSummary = true)) }

        manager.answer(optionId = 11)
        manager.advance()
        assertEquals(QuizStatus.FINISHED, manager.state.value.status)
        assertEquals(3, manager.state.value.summary?.estimatedRank)

        handler = { json(submitResponse()) }
        val entry = manager.submitScore("Ali")

        assertEquals(QuizStatus.SUBMITTED, manager.state.value.status)
        assertEquals(entry, manager.state.value.submittedEntry)
        assertEquals(true, manager.state.value.summary?.scoreSubmitted)
        assertTrue(requests.last().bodyText().contains("\"nickname\":\"Ali\""))
    }

    @Test
    fun `reset sonrasi gelen eski yanit durumu degistirmez`() = runTest {
        handler = { json(createSession()) }
        manager.startSession("fizik")
        handler = {
            manager.reset() // istek uçuştayken kullanıcı çıktı
            json(answerResponse(outcome = "correct", points = 180, nextServedAt = serverNow + 2_000))
        }

        manager.answer(optionId = 11)

        assertEquals(QuizStatus.IDLE, manager.state.value.status)
        assertEquals(0, manager.state.value.score)
    }

    // --- yardımcılar ---

    private fun iso(ms: Long) = Instant.ofEpochMilli(ms).toString()

    private fun question(id: Long = 1, index: Int = 0, servedAt: Long = serverNow) = """
        {"id": $id, "index": $index, "text": "Soru $id",
         "options": [{"id": 11, "label": "A", "text": "a"}, {"id": 12, "label": "B", "text": "b"},
                     {"id": 13, "label": "C", "text": "c"}, {"id": 14, "label": "D", "text": "d"}],
         "time_limit_ms": 5000, "served_at": "${iso(servedAt)}", "deadline_at": "${iso(servedAt + 5_000)}"}
    """

    private val category = """{"slug": "fizik", "name": "Fizik", "color_hex": "#FF5C8A"}"""

    private fun createSession() = """
        {"session": {"id": "sess-1", "token": "tok", "category": $category, "total_questions": 20,
                     "current_index": 0, "score": 0, "status": "in_progress", "expires_at": "${iso(serverNow + 1_800_000)}"},
         "question": ${question()}}
    """

    private fun progress(score: Int = 0) =
        """{"current_index": 1, "score": $score, "correct_count": 1, "wrong_count": 0, "timeout_count": 0, "status": "in_progress"}"""

    private fun answerResponse(outcome: String, points: Int, nextServedAt: Long?, withSummary: Boolean = false) = """
        {"result": {"outcome": "$outcome", "is_correct": ${outcome == "correct"}, "correct_option_id": 11,
                    "selected_option_id": null, "elapsed_ms": 1000, "points_earned": $points, "explanation": ""},
         "session": ${progress(score = points)},
         "next_question": ${nextServedAt?.let { question(id = 2, index = 1, servedAt = it) } ?: "null"}
         ${if (withSummary) ""","summary": ${summary()}""" else ""}}
    """

    private fun summary() = """
        {"session_id": "sess-1", "category": $category, "status": "completed", "score": 200,
         "max_possible_score": 4000, "total_questions": 20, "correct_count": 1, "wrong_count": 0,
         "timeout_count": 19, "accuracy_pct": 5.0, "average_elapsed_ms": 1000, "fastest_correct_ms": 1000,
         "total_elapsed_ms": 1000, "score_submitted": false, "estimated_rank": 3, "finished_at": "${iso(serverNow)}"}
    """

    private fun submitResponse() = """
        {"entry": {"id": 7, "nickname": "Ali", "score": 200, "category": $category, "rank_in_category": 3,
                   "rank_overall": 12, "created_at": "${iso(serverNow)}"},
         "leaderboard": {"scope": "category", "category": "fizik", "top": [], "user_entry_id": 7}}
    """

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.error(status: Int, code: String) =
        json("""{"error": {"code": "$code", "message": "Hata", "details": {}}}""", HttpStatusCode.fromValue(status))

    private fun HttpRequestData.bodyText(): String =
        (body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString() ?: ""
}
