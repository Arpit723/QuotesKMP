package com.demo.quotes.presentation

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException

/** Maps an exception to a short user-facing message. */
internal fun Exception.toUserMessage(): String = when {
    this is ClientRequestException && response.status == HttpStatusCode.TooManyRequests ->
        "Too many requests. Please wait a few seconds and try again."
    this is IOException -> "Can't reach the server. Check your connection."
    else -> "Something went wrong. Please try again."
}
