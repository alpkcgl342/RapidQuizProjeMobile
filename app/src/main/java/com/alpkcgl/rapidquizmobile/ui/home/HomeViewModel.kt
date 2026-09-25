package com.alpkcgl.rapidquizmobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.rapidquizmobile.core.UiState
import com.alpkcgl.rapidquizmobile.data.api.ApiException
import com.alpkcgl.rapidquizmobile.data.model.Category
import com.alpkcgl.rapidquizmobile.data.repository.CatalogRepository
import com.alpkcgl.rapidquizmobile.data.repository.QuizSessionManager
import com.alpkcgl.rapidquizmobile.data.repository.SessionLostReason
import com.alpkcgl.rapidquizmobile.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val catalog: CatalogRepository,
    private val session: QuizSessionManager,
) : ViewModel() {

    private val _categories = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val categories: StateFlow<UiState<List<Category>>> = _categories.asStateFlow()

    /** Oturum düştüğü için ana ekrana dönüldüyse gösterilecek uyarı. */
    val notice: StateFlow<SessionLostReason?> = session.lostNotice

    init {
        load(force = false)
    }

    /** Ana ekran her göründüğünde: bitmemiş oturum bırakılır (web HomeView ile aynı). */
    fun onShown() = session.resetIfNotOver()

    fun retry() = load(force = true)

    fun dismissNotice() = session.consumeLostNotice()

    private fun load(force: Boolean) {
        viewModelScope.launch {
            _categories.value = UiState.Loading
            _categories.value = try {
                UiState.Success(catalog.getCategories(force))
            } catch (e: ApiException) {
                UiState.Error(e)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                HomeViewModel(c.catalogRepository, c.quizSession)
            }
        }
    }
}
