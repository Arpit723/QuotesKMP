package com.demo.quotes.domain

import kotlinx.coroutines.flow.Flow

interface QuoteRepository {

    /** Fetches a random quote from the remote source. */
    suspend fun fetchRandomQuote(): Quote

    /** Returns a random locally saved quote, or null if none are saved. */
    suspend fun randomSavedQuote(): Quote?

    /** Emits the current list of saved quotes on every change. */
    fun observeSaved(): Flow<List<Quote>>

    /** Emits whether the quote with [id] is currently saved. */
    fun observeIsSaved(id: Long): Flow<Boolean>

    /** Persists [quote] locally. */
    suspend fun save(quote: Quote)

    /** Removes the saved quote with [id]. */
    suspend fun delete(id: Long)
}
