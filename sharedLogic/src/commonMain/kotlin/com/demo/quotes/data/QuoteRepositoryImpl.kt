package com.demo.quotes.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.demo.quotes.data.remote.QuoteApi
import com.demo.quotes.data.remote.toQuote
import com.demo.quotes.db.QuoteDatabase
import com.demo.quotes.db.SavedQuote
import com.demo.quotes.domain.Quote
import com.demo.quotes.domain.QuoteRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class QuoteRepositoryImpl(
    private val api: QuoteApi,
    private val database: QuoteDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.System,
) : QuoteRepository {

    private val queries get() = database.savedQuoteQueries

    override suspend fun fetchRandomQuote(): Quote =
        api.getRandomQuote().toQuote()

    override suspend fun randomSavedQuote(): Quote? = withContext(dispatcher) {
        queries.selectRandom().executeAsOneOrNull()?.toQuote()
    }

    override fun observeSaved(): Flow<List<Quote>> =
        queries.selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toQuote() } }

    override fun observeIsSaved(id: Long): Flow<Boolean> =
        queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it != null }

    override suspend fun save(quote: Quote) {
        withContext(dispatcher) {
            queries.insertOrReplace(
                id = quote.id,
                text = quote.text,
                author = quote.author,
                savedAt = clock.now().toEpochMilliseconds(),
            )
        }
    }

    override suspend fun delete(id: Long) {
        withContext(dispatcher) {
            queries.deleteById(id)
        }
    }
}

private fun SavedQuote.toQuote(): Quote = Quote(
    id = id,
    text = text,
    author = author,
)
