package com.demo.quotes.data.local

import app.cash.sqldelight.db.SqlDriver
import com.demo.quotes.db.QuoteDatabase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SavedQuoteQueriesTest {

    private var driver: SqlDriver? = null

    private fun newDatabase(): QuoteDatabase {
        val newDriver = createInMemoryDriver()
        driver = newDriver
        return QuoteDatabase(newDriver)
    }

    @AfterTest
    fun tearDown() {
        driver?.close()
        driver = null
    }

    @Test
    fun insertOrReplaceThenSelectAllReturnsTheRow() {
        val database = newDatabase()

        database.savedQuoteQueries.insertOrReplace(id = 1L, text = "Stay hungry.", author = "Steve Jobs", savedAt = 100L)

        val rows = database.savedQuoteQueries.selectAll().executeAsList()
        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals(1L, row.id)
        assertEquals("Stay hungry.", row.text)
        assertEquals("Steve Jobs", row.author)
        assertEquals(100L, row.savedAt)
    }

    @Test
    fun selectAllOrdersNewestFirstBySavedAt() {
        val database = newDatabase()
        val queries = database.savedQuoteQueries
        queries.insertOrReplace(id = 1L, text = "Old", author = "A", savedAt = 100L)
        queries.insertOrReplace(id = 2L, text = "Newest", author = "B", savedAt = 300L)
        queries.insertOrReplace(id = 3L, text = "Middle", author = "C", savedAt = 200L)

        val ids = queries.selectAll().executeAsList().map { it.id }

        assertEquals(listOf(2L, 3L, 1L), ids)
    }

    @Test
    fun insertingTheSameIdTwiceReplacesTheRow() {
        val database = newDatabase()
        val queries = database.savedQuoteQueries
        queries.insertOrReplace(id = 1L, text = "First", author = "A", savedAt = 100L)
        queries.insertOrReplace(id = 1L, text = "Second", author = "B", savedAt = 200L)

        val rows = queries.selectAll().executeAsList()

        assertEquals(1, rows.size)
        assertEquals("Second", rows.single().text)
        assertEquals(200L, rows.single().savedAt)
    }

    @Test
    fun selectByIdReturnsTheRowAndNothingForUnknownId() {
        val database = newDatabase()
        val queries = database.savedQuoteQueries
        queries.insertOrReplace(id = 7L, text = "Known", author = "A", savedAt = 100L)

        val found = queries.selectById(7L).executeAsOneOrNull()
        val missing = queries.selectById(99L).executeAsOneOrNull()

        assertNotNull(found)
        assertEquals("Known", found.text)
        assertNull(missing)
    }

    @Test
    fun selectRandomReturnsNothingOnAnEmptyTable() {
        val database = newDatabase()

        assertNull(database.savedQuoteQueries.selectRandom().executeAsOneOrNull())
    }

    @Test
    fun selectRandomReturnsAStoredRowOtherwise() {
        val database = newDatabase()
        database.savedQuoteQueries.insertOrReplace(id = 5L, text = "Only", author = "A", savedAt = 100L)

        val row = database.savedQuoteQueries.selectRandom().executeAsOneOrNull()

        assertNotNull(row)
        assertEquals(5L, row.id)
    }

    @Test
    fun deleteByIdRemovesOnlyTheRequestedRow() {
        val database = newDatabase()
        val queries = database.savedQuoteQueries
        queries.insertOrReplace(id = 1L, text = "Keep", author = "A", savedAt = 100L)
        queries.insertOrReplace(id = 2L, text = "Delete", author = "B", savedAt = 200L)

        queries.deleteById(2L)

        assertEquals(listOf(1L), queries.selectAll().executeAsList().map { it.id })
    }
}
