package com.demo.quotes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.quotes.domain.QuoteRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException

class HomeViewModel(
    private val repository: QuoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadNewQuote()
    }

    /** Loads a new random quote. Does nothing while a load is already in progress. */
    fun loadNewQuote() {
        if (loadJob?.isActive == true) return
        _uiState.value = HomeUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                _uiState.value = HomeUiState.Success(repository.fetchRandomQuote())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Failed to load quote: ${e.message}")
                _uiState.value = HomeUiState.Error(e.toUserMessage())
            }
        }
    }
}

private fun Exception.toUserMessage(): String = when {
    this is ClientRequestException && response.status == HttpStatusCode.TooManyRequests ->
        "Too many requests. Please wait a few seconds and try again."
    this is IOException -> "Can't reach the server. Check your connection."
    else -> "Something went wrong. Please try again."
}
