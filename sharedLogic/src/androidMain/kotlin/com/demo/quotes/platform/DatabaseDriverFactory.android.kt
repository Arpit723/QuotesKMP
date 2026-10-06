package com.demo.quotes.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.demo.quotes.db.QuoteDatabase

// Android needs a Context to open the SQLite file in the app's private storage.
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(QuoteDatabase.Schema, context, "quotes.db")
}
