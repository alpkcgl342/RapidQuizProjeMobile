package com.alpkcgl.rapidquizmobile.data.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Sunucunun `{"error": {code, message, details}}` zarfının istemci karşılığı. */
class ApiException(
    val status: Int,
    val code: String,
    override val message: String,
    val details: JsonObject = JsonObject(emptyMap()),
    cause: Throwable? = null,
) : Exception(message, cause) {

    val isNetwork: Boolean get() = code == NETWORK_ERROR

    /** Oturumu geçersiz kılan hatalar: oturum sıfırlanır, ana ekrana dönülür. */
    val isSessionLost: Boolean get() = code == INVALID_SESSION_TOKEN || code == SESSION_EXPIRED

    /** `details.<field>[0]` — alan bazlı doğrulama hatası (ör. takma ad). */
    fun fieldError(field: String): String? =
        ((details[field] as? JsonArray)?.firstOrNull() as? JsonPrimitive)?.content

    companion object {
        // İstemcide üretilenler
        const val NETWORK_ERROR = "NETWORK_ERROR"
        const val HTTP_ERROR = "HTTP_ERROR"

        // Sunucu kodları (backend openapi.yaml)
        const val VALIDATION_ERROR = "VALIDATION_ERROR"
        const val INVALID_SESSION_TOKEN = "INVALID_SESSION_TOKEN"
        const val SESSION_EXPIRED = "SESSION_EXPIRED"
        const val ALREADY_ANSWERED = "ALREADY_ANSWERED"
        const val SESSION_ALREADY_FINISHED = "SESSION_ALREADY_FINISHED"
        const val SCORE_ALREADY_SUBMITTED = "SCORE_ALREADY_SUBMITTED"
        const val INSUFFICIENT_QUESTIONS = "INSUFFICIENT_QUESTIONS"

        fun network(cause: Throwable) =
            ApiException(0, NETWORK_ERROR, "Sunucuya ulaşılamadı. Bağlantını kontrol et.", cause = cause)

        fun unexpected(status: Int, cause: Throwable? = null) =
            ApiException(status, HTTP_ERROR, "Sunucu beklenmeyen bir yanıt verdi.", cause = cause)
    }
}
