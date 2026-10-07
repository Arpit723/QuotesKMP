package com.demo.quotes.presentation

import com.demo.quotes.domain.Quote
import com.demo.quotes.domain.QuoteRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** Test fake with knobs and gates to drive every repository outcome. */
internal class FakeQuoteRepository : QuoteRepository {
    val outcomes = ArrayDeque<Result<Quote>>()
    var fetchCalls = 0
    var saveCalls = 0
    var deleteCalls = 0

    val savedIds = MutableStateFlow(emptySet<Long>())
    val savedQuotes = MutableStateFlow(emptyList<Quote>())
    var randomSaved: Quote? = null
    var randomSavedError: Exception? = null
    var isSavedReadError: Exception? = null
    var saveError: Exception? = null
    var deleteError: Exception? = null
    var observeSavedError: Exception? = null

    var fetchGate: CompletableDeferred<Unit>? = null
    var isSavedGate: CompletableDeferred<Unit>? = null
    var deleteGate: CompletableDeferred<Unit>? = null

    var activeObservers = 0

    override suspend fun fetchRandomQuote(): Quote {
        fetchCalls++
        fetchGate?.await()
        return outcomes.removeFirst().getOrThrow()
    }

    override suspend fun randomSavedQuote(): Quote? {
        randomSavedError?.let { throw it }
        return randomSaved?.also { savedIds.value = savedIds.value + it.id }
    }

    override fun observeSaved(): Flow<List<Quote>> = flow {
        activeObservers++
        try {
            observeSavedError?.let { throw it }
            savedQuotes.collect { emit(it) }
        } finally {
            activeObservers--
        }
    }

    override fun observeIsSaved(id: Long): Flow<Boolean> = flow {
        isSavedGate?.await()
        isSavedReadError?.let { throw it }
        savedIds.collect { saved -> emit(id in saved) }
    }

    override suspend fun save(quote: Quote) {
        saveCalls++
        saveError?.let { throw it }
        savedIds.value = savedIds.value + quote.id
        savedQuotes.value = listOf(quote) + savedQuotes.value.filterNot { it.id == quote.id }
    }

    override suspend fun delete(id: Long) {
        deleteCalls++
        deleteGate?.await()
        deleteError?.let { throw it }
        savedIds.value = savedIds.value - id
        savedQuotes.value = savedQuotes.value.filterNot { it.id == id }
    }
}
