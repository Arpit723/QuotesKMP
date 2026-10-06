package com.demo.quotes.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.demo.quotes.db.QuoteDatabase

actual fun createInMemoryDriver(): SqlDriver = inMemoryDriver(QuoteDatabase.Schema)
