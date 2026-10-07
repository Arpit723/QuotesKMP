package com.demo.quotes.presentation

import app.cash.turbine.test
import com.demo.quotes.data.remote.RANDOM_QUOTE_ENDPOINT
import com.demo.quotes.data.remote.createHttpClient
import com.demo.quotes.domain.Quote
import com.demo.quotes.domain.QuoteRepository
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private class FakeQuoteRepository : QuoteRepository {
        val outcomes = ArrayDeque<Result<Quote>>()
        var fetchCalls = 0

        override suspend fun fetchRandomQuote(): Quote {
            fetchCalls++
            return outcomes.removeFirst().getOrThrow()
        }

        override suspend fun randomSavedQuote(): Quote? = null

        override fun observeSaved(): Flow<List<Quote>> = flowOf(emptyList())

        override fun observeIsSaved(id: Long): Flow<Boolean> = flowOf(false)

        override suspend fun save(quote: Quote) = Unit

        override suspend fun delete(id: Long) = Unit
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
            outcomes += Result.failure(RuntimeException("raw internal failure"))
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(
                    HomeUiState.Error("Something went wrong. Please try again."),
                    awaitItem(),
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun secondLoadWhileLoadingTriggersSingleFetch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply { outcomes += Result.success(quote) }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                viewModel.loadNewQuote()
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Success(quote), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
        assertEquals(1, repository.fetchCalls)
    }

    @Test
    fun http429ProducesFriendlyMessage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.failure(httpError(HttpStatusCode.TooManyRequests))
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(
                    HomeUiState.Error("Too many requests. Please wait a few seconds and try again."),
                    awaitItem(),
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun connectionErrorProducesFriendlyMessage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.failure(IOException("connection refused"))
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(
                    HomeUiState.Error("Can't reach the server. Check your connection."),
                    awaitItem(),
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retryAfterErrorReturnsSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.failure(RuntimeException("raw internal failure"))
            outcomes += Result.success(quote)
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(
                    HomeUiState.Error("Something went wrong. Please try again."),
                    awaitItem(),
                )

                viewModel.loadNewQuote()
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Success(quote), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private suspend fun httpError(status: HttpStatusCode): ClientRequestException {
        val engine = MockEngine {
            respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return try {
            createHttpClient(engine).get(RANDOM_QUOTE_ENDPOINT).body<String>()
            error("unreachable")
        } catch (e: ClientRequestException) {
            e
        }
    }
}
