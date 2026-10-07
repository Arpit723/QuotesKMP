package com.demo.quotes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.quotes.domain.QuoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Presents the list of saved quotes and forwards deletions to the repository. */
class SavedViewModel(
    private val repository: QuoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavedUiState>(SavedUiState.Loading)
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observe()
    }

    /** Restarts observing saved quotes; recovers from an error shown by [uiState]. */
    fun retry() {
        observe()
    }

    /** Deletes the saved quote with [id]; list updates arrive via [QuoteRepository.observeSaved]. */
    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repository.delete(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = SavedUiState.Error(e.toUserMessage())
            }
        }
    }

    private fun observe() {
        observeJob?.cancel()
        _uiState.value = SavedUiState.Loading
        observeJob = viewModelScope.launch {
            try {
                repository.observeSaved().collect { quotes ->
                    _uiState.value =
                        if (quotes.isEmpty()) SavedUiState.Empty else SavedUiState.Content(quotes)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = SavedUiState.Error(e.toUserMessage())
            }
        }
    }
}
