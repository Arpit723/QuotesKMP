package com.demo.quotes.data

import com.demo.quotes.data.remote.QuoteApi
import com.demo.quotes.data.remote.toQuote
import com.demo.quotes.domain.Quote
import com.demo.quotes.domain.QuoteRepository

class QuoteRepositoryImpl(
    private val api: QuoteApi,
) : QuoteRepository {

    override suspend fun fetchRandomQuote(): Quote =
        api.getRandomQuote().toQuote()
}
