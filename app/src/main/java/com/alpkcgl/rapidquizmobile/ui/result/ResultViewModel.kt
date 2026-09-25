package com.alpkcgl.rapidquizmobile.ui.result

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alpkcgl.rapidquizmobile.R
import com.alpkcgl.rapidquizmobile.data.model.Summary
import com.alpkcgl.rapidquizmobile.data.repository.QuizSessionManager
import com.alpkcgl.rapidquizmobile.data.repository.QuizState
import com.alpkcgl.rapidquizmobile.ui.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ResultViewModel(private val session: QuizSessionManager) : ViewModel() {

    val state: StateFlow<QuizState> = session.state
    private var loadJob: Job? = null

    init {
        loadSummary()
    }

    fun loadSummary() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch { session.loadSummary() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ResultViewModel(appContainer().quizSession) }
        }
    }
}

/** Performansa göre motive edici başlık (web ResultView ile aynı eşikler). */
@StringRes
fun headlineFor(summary: Summary): Int {
    val fast = (summary.averageElapsedMs ?: Long.MAX_VALUE) < 1500
    return when {
        summary.accuracyPct >= 90 && fast -> R.string.result_headline_lightning
        summary.accuracyPct >= 80 -> R.string.result_headline_great
        summary.accuracyPct >= 60 -> R.string.result_headline_good
        summary.accuracyPct >= 40 -> R.string.result_headline_ok
        else -> R.string.result_headline_warmup
    }
}
