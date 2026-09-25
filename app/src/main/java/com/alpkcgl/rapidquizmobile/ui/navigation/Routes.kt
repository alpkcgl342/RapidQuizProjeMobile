package com.alpkcgl.rapidquizmobile.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class CountdownRoute(val categorySlug: String)

@Serializable
data object QuizRoute

@Serializable
data object ResultRoute

@Serializable
data object NicknameRoute

/** [categorySlug] null ise "Genel" sekmesi açılır. */
@Serializable
data class LeaderboardRoute(val categorySlug: String? = null)
