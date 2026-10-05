package com.bk.quoteskmp

import android.app.Application
import com.demo.quotes.di.initKoin

class QuotesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
