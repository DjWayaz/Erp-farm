package com.farmapp.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.farmapp.data.local.FarmDatabase
import com.farmapp.data.local.entity.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class FinanceDaoTest {

    private lateinit var db: FarmDatabase
    private val dao get() = db.financeDao()
    private val today = LocalDate.of(2025, 6, 15)
    private val start = today.withDayOfMonth(1).toEpochDay()
    private val end = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()

    private fun tx(
        type: TransactionType = TransactionType.EXPENSE,
        cat: TransactionCategory = TransactionCategory.FEED,
        amount: Double = 50.0,
        desc: String = "Test",
        date: LocalDate = today
    ) = TransactionEntity(date = date, type = type, category = cat, amount = amount, description = desc)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), FarmDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }

    // ── insert and retrieve ───────────────────────────────────────────────────

    @Test fun insertTransactionAndGetAll() = runTest {
        dao.insertTransaction(tx(amount = 100.0, desc = "Chick mash"))
        dao.getAllTransactions().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(100.0, result[0].amount, 0.001)
            assertEquals("Chick mash", result[0].description)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun deleteTransaction() = runTest {
        dao.insertTransaction(tx())
        dao.getAllTransactions().test {
            val inserted = awaitItem()[0]
            dao.deleteTransaction(inserted)
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun allTransactionTypesCanBeInserted() = runTest {
        for (type in TransactionType.values()) {
            dao.insertTransaction(tx(type = type))
        }
        dao.getAllTransactions().test {
            assertEquals(TransactionType.values().size, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun allTransactionCategoriesCanBeInserted() = runTest {
        for (cat in TransactionCategory.values()) {
            dao.insertTransaction(tx(cat = cat))
        }
        dao.getAllTransactions().test {
            assertEquals(TransactionCategory.values().size, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getTransactionsInPeriod ───────────────────────────────────────────────

    @Test fun getTransactionsInPeriodFiltersCorrectly() = runTest {
        dao.insertTransaction(tx(date = today, amount = 50.0))
        dao.insertTransaction(tx(date = today.minusMonths(1), amount = 999.0)) // prior month
        dao.getTransactionsInPeriod(start, end).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(50.0, result[0].amount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getTransactionsInPeriodIncludesBoundaryDates() = runTest {
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        dao.insertTransaction(tx(date = monthStart, amount = 10.0))
        dao.insertTransaction(tx(date = monthEnd, amount = 20.0))
        dao.getTransactionsInPeriod(start, end).test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getTotalByTypeInPeriod ────────────────────────────────────────────────

    @Test fun getTotalExpensesInPeriod() = runTest {
        dao.insertTransaction(tx(type = TransactionType.EXPENSE, amount = 100.0))
        dao.insertTransaction(tx(type = TransactionType.EXPENSE, amount = 50.0))
        dao.insertTransaction(tx(type = TransactionType.INCOME, amount = 999.0)) // should not count
        dao.getTotalByTypeInPeriod("EXPENSE", start, end).test {
            assertEquals(150.0, awaitItem(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getTotalIncomeInPeriod() = runTest {
        dao.insertTransaction(tx(type = TransactionType.INCOME, cat = TransactionCategory.SALES, amount = 300.0))
        dao.insertTransaction(tx(type = TransactionType.EXPENSE, amount = 999.0)) // should not count
        dao.getTotalByTypeInPeriod("INCOME", start, end).test {
            assertEquals(300.0, awaitItem(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getTotalReturnsZeroWhenNoTransactions() = runTest {
        dao.getTotalByTypeInPeriod("EXPENSE", start, end).test {
            assertEquals(0.0, awaitItem(), 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getSummaryByCategory ──────────────────────────────────────────────────

    @Test fun getSummaryByCategory() = runTest {
        dao.insertTransaction(tx(cat = TransactionCategory.FEED, amount = 60.0))
        dao.insertTransaction(tx(cat = TransactionCategory.FEED, amount = 40.0))
        dao.insertTransaction(tx(cat = TransactionCategory.SEEDS, amount = 30.0))
        dao.getSummaryByCategory("EXPENSE", start, end).test {
            val result = awaitItem()
            val feedTotal = result.first { it.category == TransactionCategory.FEED }.total
            val seedsTotal = result.first { it.category == TransactionCategory.SEEDS }.total
            assertEquals(100.0, feedTotal, 0.001)
            assertEquals(30.0, seedsTotal, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getSummaryByCategoryIgnoresWrongType() = runTest {
        dao.insertTransaction(tx(type = TransactionType.INCOME, cat = TransactionCategory.SALES, amount = 999.0))
        dao.insertTransaction(tx(cat = TransactionCategory.FEED, amount = 50.0))
        dao.getSummaryByCategory("EXPENSE", start, end).test {
            val result = awaitItem()
            assertFalse(result.any { it.category == TransactionCategory.SALES })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── relatedEntity ─────────────────────────────────────────────────────────

    @Test fun getTransactionsForEntity() = runTest {
        dao.insertTransaction(tx().copy(relatedEntityId = 5L, relatedEntityType = "field"))
        dao.insertTransaction(tx().copy(relatedEntityId = 6L, relatedEntityType = "field")) // different field
        dao.insertTransaction(tx().copy(relatedEntityId = 5L, relatedEntityType = "batch")) // different type
        dao.getTransactionsForEntity(5L, "field").test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
