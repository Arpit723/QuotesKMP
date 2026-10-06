package com.bk.quoteskmp

import android.app.Application
import com.demo.quotes.di.initKoin
import org.koin.android.ext.koin.androidContext

class QuotesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@QuotesApplication)
        }
    }
}
