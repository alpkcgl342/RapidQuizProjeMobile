package com.alpkcgl.rapidquizmobile.data.api

import android.util.Log
import com.alpkcgl.rapidquizmobile.core.QuizUiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Sunucu alan eklerse eski uygulama sürümleri bozulmasın. */
val ApiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

object HttpClientFactory {
    const val HEADER_PLATFORM = "X-Client-Platform"
    const val HEADER_VERSION = "X-Client-Version"
    const val HEADER_SESSION_TOKEN = "X-Session-Token"

    fun create(
        baseUrl: String,
        clientVersion: String,
        enableLogging: Boolean = false,
        engine: HttpClientEngine = OkHttp.create(),
    ): HttpClient = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(ApiJson) }
        install(HttpTimeout) { requestTimeoutMillis = QuizUiConfig.REQUEST_TIMEOUT_MS }
        if (enableLogging) {
            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("RapidQuizApi", message)
                    }
                }
                sanitizeHeader { it == HEADER_SESSION_TOKEN }
            }
        }
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            headers.append(HEADER_PLATFORM, "android")
            headers.append(HEADER_VERSION, clientVersion)
        }
    }
}
