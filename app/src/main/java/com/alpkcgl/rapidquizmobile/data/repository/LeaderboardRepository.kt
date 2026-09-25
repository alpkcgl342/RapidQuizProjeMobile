package com.alpkcgl.rapidquizmobile.data.repository

import com.alpkcgl.rapidquizmobile.core.QuizUiConfig
import com.alpkcgl.rapidquizmobile.data.api.RapidQuizApi
import com.alpkcgl.rapidquizmobile.data.model.LeaderboardRow

class LeaderboardRepository(private val api: RapidQuizApi) {
    /** [categorySlug] null ise genel tablo. */
    suspend fun getTop(categorySlug: String?): List<LeaderboardRow> =
        api.getLeaderboard(categorySlug, QuizUiConfig.LEADERBOARD_LIMIT)
}
