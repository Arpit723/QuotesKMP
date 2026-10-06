package com.demo.quotes.data.local

import app.cash.sqldelight.db.SqlDriver

/** Creates a fresh in-memory driver with the database schema already created. */
expect fun createInMemoryDriver(): SqlDriver
