package com.demo.quotes.di

import com.demo.quotes.data.QuoteRepositoryImpl
import com.demo.quotes.data.remote.QuoteApi
import com.demo.quotes.data.remote.createHttpClient
import com.demo.quotes.data.remote.createHttpClientEngine
import com.demo.quotes.db.QuoteDatabase
import com.demo.quotes.domain.QuoteRepository
import com.demo.quotes.platform.DatabaseDriverFactory
import com.demo.quotes.presentation.HomeViewModel
import io.ktor.client.HttpClient
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

val quotesModule = module {
    single { createHttpClient(createHttpClientEngine()) }
    single { QuoteApi(get()) }
    single<QuoteRepository> { QuoteRepositoryImpl(get()) }
    single { QuoteDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single { HomeViewModel(get()) }
}

/**
 * Starts Koin once with the platform and shared modules.
 * Android callers pass the application Context via [appDeclaration].
 */
fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) {
    if (KoinPlatform.getKoinOrNull() == null) {
        startKoin {
            appDeclaration()
            modules(platformModule, quotesModule)
        }
    }
}
