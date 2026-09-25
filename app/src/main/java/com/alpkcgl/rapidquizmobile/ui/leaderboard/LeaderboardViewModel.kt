package com.alpkcgl.rapidquizmobile.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.alpkcgl.rapidquizmobile.core.UiState
import com.alpkcgl.rapidquizmobile.data.api.ApiException
import com.alpkcgl.rapidquizmobile.data.model.Category
import com.alpkcgl.rapidquizmobile.data.model.Entry
import com.alpkcgl.rapidquizmobile.data.model.LeaderboardRow
import com.alpkcgl.rapidquizmobile.data.repository.CatalogRepository
import com.alpkcgl.rapidquizmobile.data.repository.LeaderboardRepository
import com.alpkcgl.rapidquizmobile.data.repository.QuizSessionManager
import com.alpkcgl.rapidquizmobile.ui.appContainer
import com.alpkcgl.rapidquizmobile.ui.navigation.LeaderboardRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val categories: List<Category> = emptyList(),
    /** null = Genel */
    val selectedSlug: String? = null,
    val rows: UiState<List<LeaderboardRow>> = UiState.Loading,
    /** Kullanıcının az önce kaydettiği skor (varsa). */
    val myEntry: Entry? = null,
    /** Oturumun kategorisi; "Tekrar Oyna" için. */
    val sessionCategorySlug: String? = null,
) {
    /** Kullanıcının bu sekmedeki sırası; bu sekmede yoksa null. */
    val myRankHere: Int?
        get() {
            val entry = myEntry ?: return null
            return when (selectedSlug) {
                null -> entry.rankOverall
                entry.category.slug -> entry.rankInCategory
                else -> null
            }
        }

    /** İlk 10'a giremediyse listenin altında ayrı satır. */
    val showMyRowSeparately: Boolean
        get() {
            val entry = myEntry ?: return false
            val list = (rows as? UiState.Success)?.data ?: return false
            return myRankHere != null && list.none { it.id == entry.id }
        }

    val replaySlug: String? get() = sessionCategorySlug ?: selectedSlug
}

class LeaderboardViewModel(
    initialSlug: String?,
    private val catalog: CatalogRepository,
    private val leaderboard: LeaderboardRepository,
    session: QuizSessionManager,
) : ViewModel() {

    private val local = MutableStateFlow(LeaderboardUiState(selectedSlug = initialSlug))
    private var loadJob: Job? = null

    val state: StateFlow<LeaderboardUiState> = combine(local, session.state) { s, quiz ->
        s.copy(
            myEntry = quiz.submittedEntry,
            sessionCategorySlug = quiz.category?.slug ?: quiz.summary?.category?.slug,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    init {
        viewModelScope.launch {
            val categories = try {
                catalog.getCategories()
            } catch (_: ApiException) {
                emptyList()
            }
            local.update { s ->
                // Bilinmeyen kategori slug'ı gelirse Genel'e düş.
                val selected = s.selectedSlug?.takeIf { slug -> categories.any { it.slug == slug } }
                s.copy(categories = categories, selectedSlug = selected)
            }
            load()
        }
    }

    fun select(slug: String?) {
        if (slug == local.value.selectedSlug) return
        local.update { it.copy(selectedSlug = slug) }
        load()
    }

    fun retry() = load()

    private fun load() {
        // Önceki sekmenin isteği iptal edilir; eski yanıt yeni sekmenin üstüne yazılmaz.
        loadJob?.cancel()
        val slug = local.value.selectedSlug
        loadJob = viewModelScope.launch {
            local.update { it.copy(rows = UiState.Loading) }
            val rows = try {
                UiState.Success(leaderboard.getTop(slug))
            } catch (e: ApiException) {
                UiState.Error(e)
            }
            local.update { it.copy(rows = rows) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route = createSavedStateHandle().toRoute<LeaderboardRoute>()
                val c = appContainer()
                LeaderboardViewModel(route.categorySlug, c.catalogRepository, c.leaderboardRepository, c.quizSession)
            }
        }
    }
}
