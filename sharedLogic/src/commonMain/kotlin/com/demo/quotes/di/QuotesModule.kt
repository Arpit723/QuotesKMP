package com.demo.quotes.di

import com.demo.quotes.data.QuoteRepositoryImpl
import com.demo.quotes.data.remote.QuoteApi
import com.demo.quotes.data.remote.createHttpClient
import com.demo.quotes.data.remote.createHttpClientEngine
import com.demo.quotes.domain.QuoteRepository
import com.demo.quotes.presentation.HomeViewModel
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

val quotesModule = module {
    single { createHttpClient(createHttpClientEngine()) }
    single { QuoteApi(get()) }
    single<QuoteRepository> { QuoteRepositoryImpl(get()) }
    factory { HomeViewModel(get()) }
}

fun initKoin() {
    if (KoinPlatform.getKoinOrNull() == null) {
        startKoin {
            modules(quotesModule)
        }
    }
}
