package com.demo.quotes.data.remote

import com.demo.quotes.domain.Quote
import kotlinx.serialization.Serializable

@Serializable
data class QuoteDto(
    val id: Long,
    val quote: String,
    val author: String,
)

fun QuoteDto.toQuote(): Quote =
    Quote(
        id = id,
        text = quote,
        author = author,
    )
