package com.demo.quotes.di

import com.demo.quotes.presentation.HomeViewModel
import com.demo.quotes.presentation.SavedViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object KoinHelper : KoinComponent {

    fun start() {
        initKoin()
    }

    fun homeViewModel(): HomeViewModel = get()
    fun savedViewModel(): SavedViewModel = get()
}
