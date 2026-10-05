package com.demo.quotes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.quotes.domain.QuoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: QuoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadNewQuote()
    }

    fun loadNewQuote() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                _uiState.value = HomeUiState.Success(repository.fetchRandomQuote())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load quote")
            }
        }
    }
}
