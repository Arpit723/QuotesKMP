package com.demo.quotes.presentation

import app.cash.turbine.test
import com.demo.quotes.domain.Quote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {

    private val q1 = Quote(id = 1L, text = "alpha", author = "zeta")
    private val q2 = Quote(id = 2L, text = "gamma", author = "beta")
    private val q3 = Quote(id = 3L, text = "epsilon", author = "delta")

    @Test
    fun startsAsLoading() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository()
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Empty, awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun emptyListGivesEmpty() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository()
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Empty, awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun savedQuotesGiveContentInRepositoryOrder() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            savedQuotes.value = listOf(q3, q1, q2)
        }
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q3, q1, q2)), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun savingThroughRepositoryAddsToContentLive() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            savedQuotes.value = listOf(q1)
        }
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q1)), awaitItem())

                repository.save(q2)
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q2, q1)), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deletingRemovesQuoteFromContent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            savedQuotes.value = listOf(q2, q1)
        }
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q2, q1)), awaitItem())

                viewModel.delete(q2.id)
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q1)), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deletingLastQuoteGivesEmpty() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            savedQuotes.value = listOf(q1)
        }
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q1)), awaitItem())

                viewModel.delete(q1.id)
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Empty, awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deleteFailureGivesError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            savedQuotes.value = listOf(q1)
            deleteError = RuntimeException("database write failed")
        }
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q1)), awaitItem())

                viewModel.delete(q1.id)
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Error("Something went wrong. Please try again."), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun observeSavedFailureGivesError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            observeSavedError = RuntimeException("database read failed")
        }
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Error("Something went wrong. Please try again."), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deleteDoesNotModifyStateDirectly() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            savedQuotes.value = listOf(q2, q1)
            deleteGate = CompletableDeferred()
        }
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q2, q1)), awaitItem())

                viewModel.delete(q2.id)
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q2, q1)), viewModel.uiState.value)
                assertEquals(1, repository.deleteCalls)

                repository.deleteGate?.complete(Unit)
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q1)), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retryAfterObserveSavedFailureGoesLoadingThenContent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            observeSavedError = RuntimeException("database read failed")
            savedQuotes.value = listOf(q1)
        }
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Error("Something went wrong. Please try again."), awaitItem())

                repository.observeSavedError = null
                viewModel.retry()
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q1)), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retryAfterDeleteFailureGoesLoadingThenContent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            savedQuotes.value = listOf(q1)
            deleteError = RuntimeException("database write failed")
        }
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q1)), awaitItem())

                viewModel.delete(q1.id)
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Error("Something went wrong. Please try again."), awaitItem())

                repository.deleteError = null
                viewModel.retry()
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q1)), awaitItem())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retryTwiceQuicklyLeavesExactlyOneActiveCollector() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeQuoteRepository().apply {
            observeSavedError = RuntimeException("database read failed")
            savedQuotes.value = listOf(q1)
        }
        val viewModel = SavedViewModel(repository)
        try {
            viewModel.uiState.test {
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Error("Something went wrong. Please try again."), awaitItem())

                repository.observeSavedError = null
                viewModel.retry()
                viewModel.retry()
                assertEquals(SavedUiState.Loading, awaitItem())
                this@runTest.advanceUntilIdle()
                assertEquals(SavedUiState.Content(listOf(q1)), awaitItem())
                assertEquals(1, repository.activeObservers)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
