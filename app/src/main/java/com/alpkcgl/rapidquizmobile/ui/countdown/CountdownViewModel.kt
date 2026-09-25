package com.alpkcgl.rapidquizmobile.ui.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.alpkcgl.rapidquizmobile.core.QuizUiConfig
import com.alpkcgl.rapidquizmobile.data.api.ApiException
import com.alpkcgl.rapidquizmobile.data.model.Category
import com.alpkcgl.rapidquizmobile.data.repository.CatalogRepository
import com.alpkcgl.rapidquizmobile.data.repository.QuizSessionManager
import com.alpkcgl.rapidquizmobile.ui.appContainer
import com.alpkcgl.rapidquizmobile.ui.navigation.CountdownRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CountdownUiState(
    val category: Category? = null,
    /** -1: henüz başlamadı, 0..2: "3", "2", "1", 3: "BAŞLA!" */
    val step: Int = -1,
    val error: ApiException? = null,
    /** Oturum açıldı → soru ekranına geç. */
    val started: Boolean = false,
    /** Kategori bulunamadı → ana ekrana dön. */
    val categoryMissing: Boolean = false,
)

/**
 * Hazırlık ekranı: 3 → 2 → 1 → BAŞLA!
 *
 * Oturum "BAŞLA!" anında oluşturulur: ilk sorunun süresi sunucuda oturum oluşturulunca
 * başladığı için daha erken istek atılırsa geri sayım oyuncunun 5 saniyesinden yerdi.
 */
class CountdownViewModel(
    private val slug: String,
    private val catalog: CatalogRepository,
    private val session: QuizSessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CountdownUiState())
    val state: StateFlow<CountdownUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            val category = try {
                catalog.getCategories().find { it.slug == slug }
            } catch (_: ApiException) {
                null
            }
            if (category == null) {
                _state.update { it.copy(categoryMissing = true) }
            } else {
                _state.update { it.copy(category = category) }
                run()
            }
        }
    }

    fun retry() = run()

    private fun run() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            _state.update { it.copy(error = null, step = -1) }
            for (i in 0..2) {
                _state.update { it.copy(step = i) }
                delay(QuizUiConfig.COUNTDOWN_BEAT_MS)
            }
            _state.update { it.copy(step = 3) }
            val ok = coroutineScope {
                val start = async { session.startSession(slug) }
                delay(QuizUiConfig.COUNTDOWN_GO_MS)
                start.await()
            }
            _state.update {
                if (ok) {
                    it.copy(started = true)
                } else {
                    it.copy(error = session.state.value.error ?: ApiException.unexpected(0))
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route = createSavedStateHandle().toRoute<CountdownRoute>()
                val c = appContainer()
                CountdownViewModel(route.categorySlug, c.catalogRepository, c.quizSession)
            }
        }
    }
}
