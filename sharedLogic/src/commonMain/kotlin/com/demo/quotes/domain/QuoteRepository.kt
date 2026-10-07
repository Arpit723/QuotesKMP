package com.demo.quotes.domain

import kotlinx.coroutines.flow.Flow

interface QuoteRepository {

    /** Fetches a random quote from the remote source. */
    suspend fun fetchRandomQuote(): Quote

    /** Returns a random saved quote, or null if nothing is saved. */
    suspend fun randomSavedQuote(): Quote?

    /** Observes all saved quotes, newest first. */
    fun observeSaved(): Flow<List<Quote>>

    /** Observes whether the quote with [id] is currently saved. */
    fun observeIsSaved(id: Long): Flow<Boolean>

    /** Saves [quote], replacing any previously saved row with the same id. */
    suspend fun save(quote: Quote)

    /** Deletes the saved quote with [id], if present. */
    suspend fun delete(id: Long)
}
