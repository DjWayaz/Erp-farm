package com.farmapp.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.farmapp.data.local.dao.CategorySummary
import com.farmapp.data.local.entity.*
import com.farmapp.data.repository.FinanceRepository
import com.farmapp.ui.finance.FinanceUiState
import com.farmapp.ui.finance.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FinanceRepository
    private lateinit var vm: FinanceViewModel

    private val today = LocalDate.now()
    private val start = today.withDayOfMonth(1).toEpochDay()
    private val end = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()

    private fun stubRepo(
        txns: List<TransactionEntity> = emptyList(),
        income: Double = 0.0,
        expenses: Double = 0.0,
        catSummary: List<CategorySummary> = emptyList()
    ) {
        whenever(repo.getTransactionsInPeriod(any(), any())).thenReturn(flowOf(txns))
        whenever(repo.getTotalByTypeInPeriod(eq("INCOME"), any(), any())).thenReturn(flowOf(income))
        whenever(repo.getTotalByTypeInPeriod(eq("EXPENSE"), any(), any())).thenReturn(flowOf(expenses))
        whenever(repo.getSummaryByCategory(any(), any(), any())).thenReturn(flowOf(catSummary))
    }

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mock()
        stubRepo()
        vm = FinanceViewModel(repo)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    // ── uiState ───────────────────────────────────────────────────────────────

    @Test fun `uiState has default zero values initially`() = runTest {
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(0.0, state.totalIncome, 0.0)
            assertEquals(0.0, state.totalExpenses, 0.0)
            assertEquals(0.0, state.profit, 0.0)
            assertTrue(state.transactions.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `uiState reflects income and expenses from repo`() = runTest {
        stubRepo(income = 600.0, expenses = 400.0)
        val vm2 = FinanceViewModel(repo)
        vm2.uiState.test {
            val state = awaitItem()
            assertEquals(600.0, state.totalIncome, 0.001)
            assertEquals(400.0, state.totalExpenses, 0.001)
            assertEquals(200.0, state.profit, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `uiState profit is negative when expenses exceed income`() = runTest {
        stubRepo(income = 100.0, expenses = 300.0)
        val vm2 = FinanceViewModel(repo)
        vm2.uiState.test {
            assertEquals(-200.0, awaitItem().profit, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `uiState includes transactions list`() = runTest {
        val txns = listOf(
            TransactionEntity(date = today, type = TransactionType.INCOME, category = TransactionCategory.SALES, amount = 200.0, description = "Egg sales")
        )
        stubRepo(txns = txns, income = 200.0)
        val vm2 = FinanceViewModel(repo)
        vm2.uiState.test {
            assertEquals(1, awaitItem().transactions.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `uiState includes category breakdown`() = runTest {
        val summary = listOf(CategorySummary(TransactionCategory.FEED, 120.0))
        stubRepo(catSummary = summary)
        val vm2 = FinanceViewModel(repo)
        vm2.uiState.test {
            assertEquals(1, awaitItem().expensesByCategory.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── addTransaction ────────────────────────────────────────────────────────

    @Test fun `addTransaction calls repo with correct type and category`() = runTest {
        whenever(repo.addTransaction(any())).thenReturn(1L)
        vm.addTransaction(TransactionType.EXPENSE, TransactionCategory.FEED, 45.0, "Layer mash")
        advanceUntilIdle()
        verify(repo).addTransaction(argThat {
            type == TransactionType.EXPENSE && category == TransactionCategory.FEED && amount == 45.0
        })
    }

    @Test fun `addTransaction INCOME calls repo with INCOME type`() = runTest {
        whenever(repo.addTransaction(any())).thenReturn(2L)
        vm.addTransaction(TransactionType.INCOME, TransactionCategory.SALES, 250.0, "Egg sales")
        advanceUntilIdle()
        verify(repo).addTransaction(argThat { type == TransactionType.INCOME && amount == 250.0 })
    }

    @Test fun `addTransaction stores description`() = runTest {
        whenever(repo.addTransaction(any())).thenReturn(1L)
        vm.addTransaction(TransactionType.EXPENSE, TransactionCategory.LABOUR, 30.0, "Casual worker pay")
        advanceUntilIdle()
        verify(repo).addTransaction(argThat { description == "Casual worker pay" })
    }

    @Test fun `addTransaction for all expense categories`() = runTest {
        whenever(repo.addTransaction(any())).thenReturn(1L)
        for (cat in TransactionCategory.values()) {
            vm.addTransaction(TransactionType.EXPENSE, cat, 10.0, "Test")
            advanceUntilIdle()
        }
        verify(repo, times(TransactionCategory.values().size)).addTransaction(any())
    }

    // ── deleteTransaction ─────────────────────────────────────────────────────

    @Test fun `deleteTransaction calls repo deleteTransaction`() = runTest {
        val tx = TransactionEntity(
            date = today, type = TransactionType.EXPENSE,
            category = TransactionCategory.SEEDS, amount = 20.0, description = "Seeds"
        )
        vm.deleteTransaction(tx)
        advanceUntilIdle()
        verify(repo).deleteTransaction(tx)
    }

    // ── FinanceUiState computed property ──────────────────────────────────────

    @Test fun `FinanceUiState profit zero when balanced`() {
        val s = FinanceUiState(totalIncome = 150.0, totalExpenses = 150.0)
        assertEquals(0.0, s.profit, 0.0)
    }

    @Test fun `FinanceUiState default state has all zeros`() {
        val s = FinanceUiState()
        assertEquals(0.0, s.totalIncome, 0.0)
        assertEquals(0.0, s.totalExpenses, 0.0)
        assertEquals(0.0, s.profit, 0.0)
        assertTrue(s.transactions.isEmpty())
        assertTrue(s.expensesByCategory.isEmpty())
    }
}
