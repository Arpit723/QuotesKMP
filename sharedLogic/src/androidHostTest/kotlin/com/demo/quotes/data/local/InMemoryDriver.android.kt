package com.demo.quotes.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.demo.quotes.db.QuoteDatabase

// The JDBC driver does not create the schema itself, so create it explicitly.
actual fun createInMemoryDriver(): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    QuoteDatabase.Schema.create(driver)
    return driver
}
