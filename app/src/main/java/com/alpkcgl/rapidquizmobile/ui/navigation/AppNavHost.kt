package com.alpkcgl.rapidquizmobile.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alpkcgl.rapidquizmobile.ui.countdown.CountdownScreen
import com.alpkcgl.rapidquizmobile.ui.home.HomeScreen
import com.alpkcgl.rapidquizmobile.ui.leaderboard.LeaderboardScreen
import com.alpkcgl.rapidquizmobile.ui.nickname.NicknameScreen
import com.alpkcgl.rapidquizmobile.ui.quiz.QuizScreen
import com.alpkcgl.rapidquizmobile.ui.result.ResultScreen

/**
 * Home ──▶ Countdown ──▶ Quiz ──▶ Result ──▶ Nickname ──▶ Leaderboard
 *
 * Countdown, Quiz ve Result arası geçişlerde önceki ekran geri yığından çıkar; geri tuşu
 * bitmiş bir soruya ya da geri sayıma döndürmez. Yeni tur ve skor tablosu geçişleri Home'un
 * üstüne kurulur, böylece geri tuşu her zaman ana ekrana çıkar.
 */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    fun goHome() {
        if (!navController.popBackStack(HomeRoute, inclusive = false)) {
            navController.navigate(HomeRoute) { popUpTo(0) }
        }
    }

    fun startRound(slug: String) = navController.navigate(CountdownRoute(slug)) {
        popUpTo(HomeRoute)
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onCategoryClick = ::startRound,
                onLeaderboardClick = { navController.navigate(LeaderboardRoute()) },
            )
        }
        composable<CountdownRoute> {
            CountdownScreen(
                onStarted = {
                    navController.navigate(QuizRoute) {
                        popUpTo<CountdownRoute> { inclusive = true }
                    }
                },
                onHome = ::goHome,
            )
        }
        composable<QuizRoute> {
            QuizScreen(
                onFinished = {
                    navController.navigate(ResultRoute) {
                        popUpTo<QuizRoute> { inclusive = true }
                    }
                },
                onHome = ::goHome,
            )
        }
        composable<ResultRoute> {
            ResultScreen(
                onSaveScore = { navController.navigate(NicknameRoute) },
                onPlayAgain = ::startRound,
                onLeaderboard = { slug -> navController.navigate(LeaderboardRoute(slug)) },
                onHome = ::goHome,
            )
        }
        composable<NicknameRoute> {
            NicknameScreen(
                onDone = { slug ->
                    navController.navigate(LeaderboardRoute(slug)) {
                        popUpTo(HomeRoute)
                    }
                },
                onHome = ::goHome,
            )
        }
        composable<LeaderboardRoute> {
            LeaderboardScreen(
                onPlayAgain = ::startRound,
                onHome = ::goHome,
            )
        }
    }
}
