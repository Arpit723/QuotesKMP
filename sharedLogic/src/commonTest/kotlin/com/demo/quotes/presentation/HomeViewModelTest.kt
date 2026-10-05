package com.demo.quotes.presentation

import app.cash.turbine.test
import com.demo.quotes.domain.Quote
import com.demo.quotes.domain.QuoteRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private class FakeQuoteRepository : QuoteRepository {
        val outcomes = ArrayDeque<Result<Quote>>()
        override suspend fun fetchRandomQuote(): Quote = outcomes.removeFirst().getOrThrow()
    }

    private val quote = Quote(id = 1L, text = "text", author = "author")

    @Test
    fun loadingThenSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply { outcomes += Result.success(quote) }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Success(quote), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loadingThenError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.failure(RuntimeException("boom"))
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Error("boom"), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retryAfterErrorReturnsSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.failure(RuntimeException("boom"))
            outcomes += Result.success(quote)
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Error("boom"), awaitItem())

                viewModel.loadNewQuote()
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Success(quote), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
