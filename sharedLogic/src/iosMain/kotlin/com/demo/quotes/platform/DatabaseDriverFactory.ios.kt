package com.demo.quotes.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.demo.quotes.db.QuoteDatabase

// iOS has no Context; the native driver only needs the database file name.
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(QuoteDatabase.Schema, "quotes.db")
}
