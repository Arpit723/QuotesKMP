package com.demo.quotes.data.remote

import io.ktor.client.engine.HttpClientEngine

expect fun createHttpClientEngine(): HttpClientEngine
