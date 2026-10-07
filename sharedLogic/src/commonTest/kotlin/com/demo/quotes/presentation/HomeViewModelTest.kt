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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val quote = Quote(id = 1L, text = "text", author = "author")
    private val savedQuote = Quote(id = 9L, text = "saved", author = "saved author")
    private val onlineQuote = Quote(id = 2L, text = "online", author = "online author")

    @Test
    fun loadingThenSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply { outcomes += Result.success(quote) }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Success(quote, isSaved = false, isOffline = false), awaitItem())
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
                assertEquals(HomeUiState.Success(quote, isSaved = false, isOffline = false), awaitItem())
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
                assertEquals(HomeUiState.Success(quote, isSaved = false, isOffline = false), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun onlineQuoteAlreadySavedShowsIsSavedTrueOnFirstSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.success(quote)
            savedIds.value = setOf(quote.id)
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Success(quote, isSaved = true, isOffline = false), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun externalSaveUpdatesHomeState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply { outcomes += Result.success(quote) }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Success(quote, isSaved = false, isOffline = false), awaitItem())

                repository.save(quote)
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Success(quote, isSaved = true, isOffline = false), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun toggleSaveDuringLoadingDoesNothing() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.success(quote)
            fetchGate = CompletableDeferred()
        }
        val viewModel = HomeViewModel(repository)
        try {
            assertEquals(HomeUiState.Loading, viewModel.uiState.value)

            viewModel.toggleSave()
            this@runTest.advanceUntilIdle()

            assertEquals(HomeUiState.Loading, viewModel.uiState.value)
            assertEquals(0, repository.saveCalls)
            assertEquals(0, repository.deleteCalls)

            repository.fetchGate?.complete(Unit)
            this@runTest.advanceUntilIdle()
            assertEquals(
                HomeUiState.Success(quote, isSaved = false, isOffline = false),
                viewModel.uiState.value,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun toggleSaveDuringErrorDoesNothing() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.failure(RuntimeException("raw internal failure"))
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Error("Something went wrong. Please try again."), awaitItem())
            }

            viewModel.toggleSave()
            this@runTest.advanceUntilIdle()

            assertEquals(HomeUiState.Error("Something went wrong. Please try again."), viewModel.uiState.value)
            assertEquals(0, repository.saveCalls)
            assertEquals(0, repository.deleteCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun secondLoadWhileIsSavedReadSuspendedMakesNoExtraFetch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.success(quote)
            isSavedGate = CompletableDeferred()
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()

                viewModel.loadNewQuote()
                this@runTest.advanceUntilIdle()
                assertEquals(1, repository.fetchCalls)

                repository.isSavedGate?.complete(Unit)
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Success(quote, isSaved = false, isOffline = false), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun fetchFailureFallsBackToRandomSavedQuote() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.failure(IOException("connection refused"))
            randomSaved = savedQuote
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(
                    HomeUiState.Success(savedQuote, isSaved = true, isOffline = true),
                    awaitItem(),
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun unsaveOfflineQuoteKeepsOfflineLabel() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.failure(IOException("connection refused"))
            randomSaved = savedQuote
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(
                    HomeUiState.Success(savedQuote, isSaved = true, isOffline = true),
                    awaitItem(),
                )

                viewModel.toggleSave()
                this@runTest.advanceUntilIdle()
                assertEquals(
                    HomeUiState.Success(savedQuote, isSaved = false, isOffline = true),
                    awaitItem(),
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retryFromOfflineReturnsToOnlineQuote() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.failure(IOException("connection refused"))
            outcomes += Result.success(onlineQuote)
            randomSaved = savedQuote
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(
                    HomeUiState.Success(savedQuote, isSaved = true, isOffline = true),
                    awaitItem(),
                )

                viewModel.loadNewQuote()
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(
                    HomeUiState.Success(onlineQuote, isSaved = false, isOffline = false),
                    awaitItem(),
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun isSavedReadFailureFallsBackToOfflineQuote() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.success(quote)
            isSavedReadError = RuntimeException("database read failed")
            randomSaved = savedQuote
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(
                    HomeUiState.Success(savedQuote, isSaved = true, isOffline = true),
                    awaitItem(),
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun isSavedReadFailureWithNothingSavedShowsError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.success(quote)
            isSavedReadError = RuntimeException("database read failed")
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
    fun randomSavedQuoteFailureShowsError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.failure(IOException("connection refused"))
            randomSavedError = RuntimeException("database read failed")
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
    fun toggleFailureShowsError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            outcomes += Result.success(quote)
            saveError = RuntimeException("database write failed")
        }
        val viewModel = HomeViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(HomeUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(HomeUiState.Success(quote, isSaved = false, isOffline = false), awaitItem())

                viewModel.toggleSave()
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
