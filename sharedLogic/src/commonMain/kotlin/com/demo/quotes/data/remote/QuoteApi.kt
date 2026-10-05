package com.demo.quotes.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal const val RANDOM_QUOTE_ENDPOINT = "https://dummyjson.com/quotes/random"

class QuoteApi(
    private val client: HttpClient,
) {
    suspend fun getRandomQuote(): QuoteDto =
        client.get(RANDOM_QUOTE_ENDPOINT).body()
}

fun createHttpClient(engine: HttpClientEngine): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
        }
    }
