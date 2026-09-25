package com.alpkcgl.rapidquizmobile

import android.app.Application
import com.alpkcgl.rapidquizmobile.data.api.HttpClientFactory
import com.alpkcgl.rapidquizmobile.data.api.RapidQuizApi
import com.alpkcgl.rapidquizmobile.data.repository.CatalogRepository
import com.alpkcgl.rapidquizmobile.data.repository.LeaderboardRepository
import com.alpkcgl.rapidquizmobile.data.repository.QuizSessionManager

/** Manuel DI: uygulama ömrü boyunca yaşayan bağımlılıklar. */
class AppContainer {
    private val http = HttpClientFactory.create(
        baseUrl = BuildConfig.API_BASE_URL,
        clientVersion = BuildConfig.VERSION_NAME,
        enableLogging = BuildConfig.DEBUG,
    )
    private val api = RapidQuizApi(http)

    val catalogRepository = CatalogRepository(api)
    val leaderboardRepository = LeaderboardRepository(api)

    /** Ekranlar arası paylaşılan oturum. */
    val quizSession = QuizSessionManager(api)
}

class RapidQuizApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
