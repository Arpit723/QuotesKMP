package com.demo.quotes.domain

interface QuoteRepository {

    /** Fetches a random quote from the remote source. */
    suspend fun fetchRandomQuote(): Quote
}
