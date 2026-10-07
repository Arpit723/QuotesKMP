package com.demo.quotes.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.turbine.test
import com.demo.quotes.data.local.createInMemoryDriver
import com.demo.quotes.data.remote.QuoteApi
import com.demo.quotes.data.remote.createHttpClient
import com.demo.quotes.db.QuoteDatabase
import com.demo.quotes.domain.Quote
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class QuoteRepositoryImplTest {

    private var driver: SqlDriver? = null

    private class MutableClock(var nowMs: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
    }

    private fun TestScope.repositoryWith(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        clock: MutableClock = MutableClock(nowMs = 0L),
    ): QuoteRepositoryImpl {
        val engine = MockEngine {
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val newDriver = createInMemoryDriver()
        driver = newDriver
        return QuoteRepositoryImpl(
            api = QuoteApi(createHttpClient(engine)),
            database = QuoteDatabase(newDriver),
            dispatcher = StandardTestDispatcher(testScheduler),
            clock = clock,
        )
    }

    @AfterTest
    fun tearDown() {
        driver?.close()
        driver = null
    }

    private val quoteA = Quote(id = 1L, text = "Stay hungry.", author = "Steve Jobs")
    private val quoteB = Quote(id = 2L, text = "Simplicity is the ultimate sophistication.", author = "Leonardo da Vinci")
    private val quoteC = Quote(id = 3L, text = "Talk is cheap.", author = "Linus Torvalds")

    @Test
    fun successReturnsQuote() = runTest {
        val repository = repositoryWith(
            """{"id":42,"quote":"Stay hungry, stay foolish.","author":"Steve Jobs"}""",
        )

        val quote = repository.fetchRandomQuote()

        assertEquals(Quote(id = 42L, text = "Stay hungry, stay foolish.", author = "Steve Jobs"), quote)
    }

    @Test
    fun failureThrows() = runTest {
        val repository = repositoryWith(
            body = """{"message":"Internal Server Error"}""",
            status = HttpStatusCode.InternalServerError,
        )

        assertFailsWith<ServerResponseException> { repository.fetchRandomQuote() }
    }

    @Test
    fun randomSavedQuoteReturnsNullWhenNothingIsSaved() = runTest {
        val repository = repositoryWith(body = "{}")

        assertNull(repository.randomSavedQuote())
    }

    @Test
    fun randomSavedQuoteReturnsASavedQuote() = runTest {
        val repository = repositoryWith(body = "{}")
        repository.save(quoteA)

        assertEquals(quoteA, repository.randomSavedQuote())
    }

    @Test
    fun observeSavedEmitsQuotesAsTheyAreSaved() = runTest {
        val clock = MutableClock(nowMs = 100L)
        val repository = repositoryWith(body = "{}", clock = clock)

        repository.observeSaved().test {
            assertEquals(emptyList(), awaitItem())

            repository.save(quoteA)
            this@runTest.advanceUntilIdle()
            assertEquals(listOf(quoteA), awaitItem())

            clock.nowMs = 200L
            repository.save(quoteB)
            this@runTest.advanceUntilIdle()
            assertEquals(listOf(quoteB, quoteA), awaitItem())
        }
    }

    @Test
    fun observeIsSavedReflectsSaveAndDelete() = runTest {
        val repository = repositoryWith(body = "{}")

        repository.observeIsSaved(quoteA.id).test {
            assertEquals(false, awaitItem())

            repository.save(quoteA)
            this@runTest.advanceUntilIdle()
            assertEquals(true, awaitItem())

            repository.delete(quoteA.id)
            this@runTest.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun savingTheSameIdTwiceReplacesTheRowAndRefreshesSavedAt() = runTest {
        val clock = MutableClock(nowMs = 100L)
        val repository = repositoryWith(body = "{}", clock = clock)

        repository.save(quoteA)
        repository.save(quoteB)
        clock.nowMs = 500L
        repository.save(quoteA)

        val saved = repository.observeSaved().first()
        assertEquals(listOf(quoteA, quoteB), saved)
    }

    @Test
    fun deleteLeavesOtherRowsUntouched() = runTest {
        val clock = MutableClock(nowMs = 100L)
        val repository = repositoryWith(body = "{}", clock = clock)

        repository.save(quoteA)
        repository.save(quoteB)
        repository.delete(quoteA.id)

        assertEquals(listOf(quoteB), repository.observeSaved().first())
        assertEquals(false, repository.observeIsSaved(quoteA.id).first())
    }

    @Test
    fun observeSavedOrdersNewestFirstWithControlledClock() = runTest {
        val clock = MutableClock(nowMs = 100L)
        val repository = repositoryWith(body = "{}", clock = clock)

        repository.save(quoteA)
        clock.nowMs = 300L
        repository.save(quoteC)
        clock.nowMs = 200L
        repository.save(quoteB)

        assertEquals(listOf(quoteC, quoteB, quoteA), repository.observeSaved().first())
    }
}
