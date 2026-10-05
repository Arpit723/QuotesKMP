package com.demo.quotes.data

import com.demo.quotes.data.remote.QuoteApi
import com.demo.quotes.data.remote.createHttpClient
import com.demo.quotes.domain.Quote
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class QuoteRepositoryImplTest {

    private fun repositoryWith(body: String, status: HttpStatusCode = HttpStatusCode.OK): QuoteRepositoryImpl {
        val engine = MockEngine {
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return QuoteRepositoryImpl(QuoteApi(createHttpClient(engine)))
    }

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
}
