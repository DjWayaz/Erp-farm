package com.farmapp.repository

import app.cash.turbine.test
import com.farmapp.data.local.dao.InventoryDao
import com.farmapp.data.local.entity.InventoryItemEntity
import com.farmapp.data.repository.InventoryRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class InventoryRepositoryTest {

    private lateinit var dao: InventoryDao
    private lateinit var repo: InventoryRepository

    private fun item(id: Long = 0L, name: String = "Layer Mash", qty: Double = 10.0, rate: Double = 1.0) =
        InventoryItemEntity(id = id, name = name, unit = "bags", currentQuantity = qty, consumptionRatePerDay = rate)

    @Before fun setUp() {
        dao = mock()
        repo = InventoryRepository(dao)
    }

    // ── saveItem ──────────────────────────────────────────────────────────────

    @Test fun `saveItem delegates to dao and returns id`() = runTest {
        whenever(dao.insertItem(any())).thenReturn(1L)
        assertEquals(1L, repo.saveItem(item()))
        verify(dao).insertItem(any())
    }

    @Test fun `saveItem with custom threshold passes threshold to dao`() = runTest {
        val i = item().copy(lowStockThresholdDays = 14)
        whenever(dao.insertItem(i)).thenReturn(1L)
        repo.saveItem(i)
        verify(dao).insertItem(argThat { lowStockThresholdDays == 14 })
    }

    // ── updateItem ────────────────────────────────────────────────────────────

    @Test fun `updateItem delegates to dao`() = runTest {
        val i = item(id = 1L)
        repo.updateItem(i)
        verify(dao).updateItem(i)
    }

    // ── deleteItem ────────────────────────────────────────────────────────────

    @Test fun `deleteItem delegates to dao`() = runTest {
        val i = item(id = 2L)
        repo.deleteItem(i)
        verify(dao).deleteItem(i)
    }

    // ── getAllItems ───────────────────────────────────────────────────────────

    @Test fun `getAllItems emits sorted list from dao`() = runTest {
        val items = listOf(item(1L, "Chick Starter"), item(2L, "Layer Mash"))
        whenever(dao.getAllItems()).thenReturn(flowOf(items))
        repo.getAllItems().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Chick Starter", result[0].name)
            awaitComplete()
        }
    }

    @Test fun `getAllItems emits empty when no items`() = runTest {
        whenever(dao.getAllItems()).thenReturn(flowOf(emptyList()))
        repo.getAllItems().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // ── getItemById ───────────────────────────────────────────────────────────

    @Test fun `getItemById returns item when found`() = runTest {
        whenever(dao.getItemById(3L)).thenReturn(flowOf(item(3L)))
        repo.getItemById(3L).test {
            assertEquals(3L, awaitItem()!!.id)
            awaitComplete()
        }
    }

    @Test fun `getItemById returns null when not found`() = runTest {
        whenever(dao.getItemById(99L)).thenReturn(flowOf(null))
        repo.getItemById(99L).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    // ── addStock ──────────────────────────────────────────────────────────────

    @Test fun `addStock delegates to dao with correct itemId and quantity`() = runTest {
        repo.addStock(1L, 5.0)
        verify(dao).addStock(1L, 5.0)
    }

    @Test fun `addStock with fractional quantity`() = runTest {
        repo.addStock(2L, 0.5)
        verify(dao).addStock(2L, 0.5)
    }

    // ── removeStock ───────────────────────────────────────────────────────────

    @Test fun `removeStock delegates to dao with correct itemId and quantity`() = runTest {
        repo.removeStock(1L, 2.0)
        verify(dao).removeStock(1L, 2.0)
    }

    @Test fun `removeStock with fractional quantity`() = runTest {
        repo.removeStock(3L, 1.5)
        verify(dao).removeStock(3L, 1.5)
    }
}
