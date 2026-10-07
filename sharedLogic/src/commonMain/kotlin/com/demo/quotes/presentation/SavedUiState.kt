package com.demo.quotes.presentation

import com.demo.quotes.domain.Quote

/** State of the saved-quotes screen. */
sealed interface SavedUiState {
    data object Loading : SavedUiState
    data object Empty : SavedUiState
    data class Content(val quotes: List<Quote>) : SavedUiState
    data class Error(val message: String) : SavedUiState
}
