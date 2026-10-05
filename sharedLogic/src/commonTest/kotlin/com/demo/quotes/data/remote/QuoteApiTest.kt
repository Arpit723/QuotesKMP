package com.demo.quotes.data.remote

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

class QuoteApiTest {

    private fun apiWith(body: String, status: HttpStatusCode = HttpStatusCode.OK): QuoteApi {
        val engine = MockEngine {
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return QuoteApi(createHttpClient(engine))
    }

    @Test
    fun validJsonReturnsCorrectDto() = runTest {
        val api = apiWith("""{"id":42,"quote":"Stay hungry, stay foolish.","author":"Steve Jobs"}""")

        val dto = api.getRandomQuote()

        assertEquals(42L, dto.id)
        assertEquals("Stay hungry, stay foolish.", dto.quote)
        assertEquals("Steve Jobs", dto.author)
    }

    @Test
    fun unknownExtraFieldsAreIgnored() = runTest {
        val api = apiWith(
            """{"id":7,"quote":"Simplicity is the ultimate sophistication.",""" +
                """"author":"Leonardo da Vinci","unknownField":"x","extraObject":{"y":1},"extraArray":[1,2]}""",
        )

        val dto = api.getRandomQuote()

        assertEquals(7L, dto.id)
        assertEquals("Simplicity is the ultimate sophistication.", dto.quote)
        assertEquals("Leonardo da Vinci", dto.author)
    }

    @Test
    fun http500ThrowsException() = runTest {
        val api = apiWith(
            body = """{"message":"Internal Server Error"}""",
            status = HttpStatusCode.InternalServerError,
        )

        assertFailsWith<ServerResponseException> { api.getRandomQuote() }
    }
}
