package com.farmapp.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.farmapp.data.local.entity.InventoryItemEntity
import com.farmapp.data.repository.InventoryRepository
import com.farmapp.ui.inventory.InventoryViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: InventoryRepository
    private lateinit var vm: InventoryViewModel

    private fun item(id: Long = 0L, name: String = "Layer Mash", qty: Double = 10.0, rate: Double = 1.0, threshold: Int = 7) =
        InventoryItemEntity(id = id, name = name, unit = "bags", currentQuantity = qty, consumptionRatePerDay = rate, lowStockThresholdDays = threshold)

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mock()
        whenever(repo.getAllItems()).thenReturn(flowOf(emptyList()))
        vm = InventoryViewModel(repo)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    // ── items StateFlow ───────────────────────────────────────────────────────

    @Test fun `items starts empty`() = runTest {
        vm.items.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `items emits list from repo`() = runTest {
        whenever(repo.getAllItems()).thenReturn(flowOf(listOf(item(1), item(2, "Chick Starter"))))
        val vm2 = InventoryViewModel(repo)
        vm2.items.test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── saveItem ──────────────────────────────────────────────────────────────

    @Test fun `saveItem calls repo with correct fields`() = runTest {
        whenever(repo.saveItem(any())).thenReturn(1L)
        vm.saveItem("Grower Mash", "bags", 20.0, 2.0, 7)
        advanceUntilIdle()
        verify(repo).saveItem(argThat {
            name == "Grower Mash" && unit == "bags" && currentQuantity == 20.0
                && consumptionRatePerDay == 2.0 && lowStockThresholdDays == 7
        })
    }

    @Test fun `saveItem with custom threshold`() = runTest {
        whenever(repo.saveItem(any())).thenReturn(1L)
        vm.saveItem("Feed", "kg", 50.0, 5.0, 14)
        advanceUntilIdle()
        verify(repo).saveItem(argThat { lowStockThresholdDays == 14 })
    }

    @Test fun `saveItem with zero consumption rate`() = runTest {
        whenever(repo.saveItem(any())).thenReturn(1L)
        vm.saveItem("Spare Parts", "units", 5.0, 0.0, 7)
        advanceUntilIdle()
        verify(repo).saveItem(argThat { consumptionRatePerDay == 0.0 })
    }

    // ── addStock ──────────────────────────────────────────────────────────────

    @Test fun `addStock calls repo with correct itemId and quantity`() = runTest {
        vm.addStock(1L, 5.0)
        advanceUntilIdle()
        verify(repo).addStock(1L, 5.0)
    }

    @Test fun `addStock with fractional bags`() = runTest {
        vm.addStock(2L, 0.5)
        advanceUntilIdle()
        verify(repo).addStock(2L, 0.5)
    }

    @Test fun `addStock does not call removeStock`() = runTest {
        vm.addStock(1L, 3.0)
        advanceUntilIdle()
        verify(repo, never()).removeStock(any(), any())
    }

    // ── removeStock ───────────────────────────────────────────────────────────

    @Test fun `removeStock calls repo with correct itemId and quantity`() = runTest {
        vm.removeStock(1L, 2.0)
        advanceUntilIdle()
        verify(repo).removeStock(1L, 2.0)
    }

    @Test fun `removeStock does not call addStock`() = runTest {
        vm.removeStock(1L, 1.0)
        advanceUntilIdle()
        verify(repo, never()).addStock(any(), any())
    }

    // ── deleteItem ────────────────────────────────────────────────────────────

    @Test fun `deleteItem calls repo deleteItem`() = runTest {
        val i = item(id = 3L)
        vm.deleteItem(i)
        advanceUntilIdle()
        verify(repo).deleteItem(i)
    }

    @Test fun `deleteItem does not call saveItem or updateItem`() = runTest {
        vm.deleteItem(item(1L))
        advanceUntilIdle()
        verify(repo, never()).saveItem(any())
        verify(repo, never()).updateItem(any())
    }

    // ── daysRemaining business logic ──────────────────────────────────────────

    @Test fun `daysRemaining calculation is correct`() {
        val i = item(qty = 14.0, rate = 2.0)
        val daysRemaining = i.currentQuantity / i.consumptionRatePerDay
        assertEquals(7.0, daysRemaining, 0.001)
    }

    @Test fun `item is low stock when days remaining at threshold`() {
        val i = item(qty = 7.0, rate = 1.0, threshold = 7)
        val daysRemaining = i.currentQuantity / i.consumptionRatePerDay
        assertTrue(daysRemaining <= i.lowStockThresholdDays)
    }

    @Test fun `item is not low stock when well stocked`() {
        val i = item(qty = 30.0, rate = 1.0, threshold = 7)
        val daysRemaining = i.currentQuantity / i.consumptionRatePerDay
        assertFalse(daysRemaining <= i.lowStockThresholdDays)
    }
}
