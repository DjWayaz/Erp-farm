package com.farmapp.repository

import app.cash.turbine.test
import com.farmapp.data.local.dao.CategorySummary
import com.farmapp.data.local.dao.FinanceDao
import com.farmapp.data.local.entity.*
import com.farmapp.data.repository.FinanceRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.time.LocalDate

class FinanceRepositoryTest {

    private lateinit var dao: FinanceDao
    private lateinit var repo: FinanceRepository

    private val today = LocalDate.of(2025, 6, 15)
    private val monthStart = today.withDayOfMonth(1).toEpochDay()
    private val monthEnd = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()

    private fun tx(
        type: TransactionType = TransactionType.EXPENSE,
        category: TransactionCategory = TransactionCategory.FEED,
        amount: Double = 50.0,
        desc: String = "Test"
    ) = TransactionEntity(date = today, type = type, category = category, amount = amount, description = desc)

    @Before fun setUp() {
        dao = mock()
        repo = FinanceRepository(dao)
    }

    // ── addTransaction ────────────────────────────────────────────────────────

    @Test fun `addTransaction delegates to dao and returns id`() = runTest {
        whenever(dao.insertTransaction(any())).thenReturn(1L)
        assertEquals(1L, repo.addTransaction(tx()))
        verify(dao).insertTransaction(any())
    }

    @Test fun `addTransaction INCOME type is stored correctly`() = runTest {
        val income = tx(type = TransactionType.INCOME, category = TransactionCategory.SALES, amount = 200.0)
        whenever(dao.insertTransaction(income)).thenReturn(2L)
        repo.addTransaction(income)
        verify(dao).insertTransaction(argThat { type == TransactionType.INCOME && amount == 200.0 })
    }

    @Test fun `addTransaction EXPENSE with SEEDS category`() = runTest {
        val expense = tx(category = TransactionCategory.SEEDS, amount = 35.0)
        whenever(dao.insertTransaction(any())).thenReturn(3L)
        repo.addTransaction(expense)
        verify(dao).insertTransaction(argThat { category == TransactionCategory.SEEDS })
    }

    // ── deleteTransaction ─────────────────────────────────────────────────────

    @Test fun `deleteTransaction delegates to dao`() = runTest {
        val t = tx()
        repo.deleteTransaction(t)
        verify(dao).deleteTransaction(t)
    }

    // ── getAllTransactions ────────────────────────────────────────────────────

    @Test fun `getAllTransactions emits all from dao`() = runTest {
        val txns = listOf(tx(amount = 100.0), tx(type = TransactionType.INCOME, amount = 300.0))
        whenever(dao.getAllTransactions()).thenReturn(flowOf(txns))
        repo.getAllTransactions().test {
            assertEquals(2, awaitItem().size)
            awaitComplete()
        }
    }

    // ── getTransactionsInPeriod ───────────────────────────────────────────────

    @Test fun `getTransactionsInPeriod delegates with correct epoch range`() = runTest {
        whenever(dao.getTransactionsInPeriod(monthStart, monthEnd)).thenReturn(flowOf(emptyList()))
        repo.getTransactionsInPeriod(monthStart, monthEnd).test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
        verify(dao).getTransactionsInPeriod(monthStart, monthEnd)
    }

    @Test fun `getTransactionsInPeriod returns transactions in range`() = runTest {
        val txns = listOf(tx(amount = 45.0), tx(amount = 80.0))
        whenever(dao.getTransactionsInPeriod(any(), any())).thenReturn(flowOf(txns))
        repo.getTransactionsInPeriod(monthStart, monthEnd).test {
            assertEquals(2, awaitItem().size)
            awaitComplete()
        }
    }

    // ── getTotalByTypeInPeriod ────────────────────────────────────────────────

    @Test fun `getTotalByTypeInPeriod for EXPENSE returns expense total`() = runTest {
        whenever(dao.getTotalByTypeInPeriod("EXPENSE", monthStart, monthEnd)).thenReturn(flowOf(350.0))
        repo.getTotalByTypeInPeriod("EXPENSE", monthStart, monthEnd).test {
            assertEquals(350.0, awaitItem(), 0.001)
            awaitComplete()
        }
    }

    @Test fun `getTotalByTypeInPeriod for INCOME returns income total`() = runTest {
        whenever(dao.getTotalByTypeInPeriod("INCOME", monthStart, monthEnd)).thenReturn(flowOf(600.0))
        repo.getTotalByTypeInPeriod("INCOME", monthStart, monthEnd).test {
            assertEquals(600.0, awaitItem(), 0.001)
            awaitComplete()
        }
    }

    @Test fun `getTotalByTypeInPeriod returns zero when no transactions`() = runTest {
        whenever(dao.getTotalByTypeInPeriod(any(), any(), any())).thenReturn(flowOf(0.0))
        repo.getTotalByTypeInPeriod("EXPENSE", monthStart, monthEnd).test {
            assertEquals(0.0, awaitItem(), 0.0)
            awaitComplete()
        }
    }

    // ── getSummaryByCategory ──────────────────────────────────────────────────

    @Test fun `getSummaryByCategory returns grouped totals`() = runTest {
        val summaries = listOf(
            CategorySummary(TransactionCategory.FEED, 120.0),
            CategorySummary(TransactionCategory.SEEDS, 45.0)
        )
        whenever(dao.getSummaryByCategory("EXPENSE", monthStart, monthEnd)).thenReturn(flowOf(summaries))
        repo.getSummaryByCategory("EXPENSE", monthStart, monthEnd).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals(120.0, result[0].total, 0.001)
            assertEquals(TransactionCategory.SEEDS, result[1].category)
            awaitComplete()
        }
    }

    @Test fun `getSummaryByCategory empty when no expenses`() = runTest {
        whenever(dao.getSummaryByCategory(any(), any(), any())).thenReturn(flowOf(emptyList()))
        repo.getSummaryByCategory("EXPENSE", monthStart, monthEnd).test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }
}
