package com.demo.quotes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.quotes.domain.Quote
import com.demo.quotes.domain.QuoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: QuoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var savedJob: Job? = null

    init {
        loadNewQuote()
    }

    /** Loads a new random quote. Does nothing while a load is already in progress. */
    fun loadNewQuote() {
        if (loadJob?.isActive == true) return
        savedJob?.cancel()
        _uiState.value = HomeUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                val quote = repository.fetchRandomQuote()
                val isSaved = repository.observeIsSaved(quote.id).first()
                show(quote, isSaved = isSaved, isOffline = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Failed to load quote: ${e.message}")
                fallbackToSavedQuote(e)
            }
        }
    }

    /** Saves or unsaves the current quote. Does nothing unless a quote is shown. */
    fun toggleSave() {
        val state = _uiState.value as? HomeUiState.Success ?: return
        viewModelScope.launch {
            try {
                if (state.isSaved) repository.delete(state.quote.id) else repository.save(state.quote)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                savedJob?.cancel()
                _uiState.value = HomeUiState.Error(e.toUserMessage())
            }
        }
    }

    private suspend fun fallbackToSavedQuote(error: Exception) {
        val savedQuote = try {
            repository.randomSavedQuote()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (savedQuote != null) {
            show(savedQuote, isSaved = true, isOffline = true)
        } else {
            savedJob?.cancel()
            _uiState.value = HomeUiState.Error(error.toUserMessage())
        }
    }

    private suspend fun show(quote: Quote, isSaved: Boolean, isOffline: Boolean) {
        savedJob?.cancel()
        _uiState.value = HomeUiState.Success(quote, isSaved, isOffline)
        savedJob = viewModelScope.launch {
            try {
                repository.observeIsSaved(quote.id).collect { saved ->
                    val state = _uiState.value
                    if (state is HomeUiState.Success && state.quote.id == quote.id) {
                        _uiState.value = state.copy(isSaved = saved)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Stopped observing saved state for quote ${quote.id}: ${e.message}")
            }
        }
    }
}
