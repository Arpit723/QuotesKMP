package com.demo.quotes.presentation

import com.demo.quotes.domain.Quote

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val quote: Quote, val isSaved: Boolean, val isOffline: Boolean) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
