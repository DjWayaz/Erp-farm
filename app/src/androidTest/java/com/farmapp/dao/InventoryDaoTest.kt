package com.farmapp.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.farmapp.data.local.FarmDatabase
import com.farmapp.data.local.entity.InventoryItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryDaoTest {

    private lateinit var db: FarmDatabase
    private val dao get() = db.inventoryDao()

    private fun item(name: String = "Layer Mash", qty: Double = 10.0, rate: Double = 1.0, threshold: Int = 7) =
        InventoryItemEntity(name = name, unit = "bags", currentQuantity = qty, consumptionRatePerDay = rate, lowStockThresholdDays = threshold)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), FarmDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Test fun insertItemAndGetById() = runTest {
        val id = dao.insertItem(item("Grower Mash", qty = 20.0))
        dao.getItemById(id).test {
            val result = awaitItem()!!
            assertEquals("Grower Mash", result.name)
            assertEquals(20.0, result.currentQuantity, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getAllItemsReturnsSortedByName() = runTest {
        dao.insertItem(item("Zinc Supplement"))
        dao.insertItem(item("Chick Starter"))
        dao.insertItem(item("Layer Mash"))
        dao.getAllItems().test {
            val result = awaitItem()
            assertEquals("Chick Starter", result[0].name)
            assertEquals("Layer Mash", result[1].name)
            assertEquals("Zinc Supplement", result[2].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun updateItem() = runTest {
        val id = dao.insertItem(item(qty = 10.0))
        dao.getItemById(id).test {
            val original = awaitItem()!!
            dao.updateItem(original.copy(currentQuantity = 25.0))
            assertEquals(25.0, awaitItem()!!.currentQuantity, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun deleteItem() = runTest {
        val id = dao.insertItem(item())
        dao.getItemById(id).test {
            val inserted = awaitItem()!!
            dao.deleteItem(inserted)
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getItemByIdReturnsNullForMissingId() = runTest {
        dao.getItemById(9999L).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── addStock / removeStock ────────────────────────────────────────────────

    @Test fun addStockIncreasesQuantity() = runTest {
        val id = dao.insertItem(item(qty = 10.0))
        dao.addStock(id, 5.0)
        dao.getItemById(id).test {
            assertEquals(15.0, awaitItem()!!.currentQuantity, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun removeStockDecreasesQuantity() = runTest {
        val id = dao.insertItem(item(qty = 10.0))
        dao.removeStock(id, 3.0)
        dao.getItemById(id).test {
            assertEquals(7.0, awaitItem()!!.currentQuantity, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun addStockMultipleTimes() = runTest {
        val id = dao.insertItem(item(qty = 5.0))
        dao.addStock(id, 3.0)
        dao.addStock(id, 2.0)
        dao.getItemById(id).test {
            assertEquals(10.0, awaitItem()!!.currentQuantity, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun addStockWithFractionalBags() = runTest {
        val id = dao.insertItem(item(qty = 10.0))
        dao.addStock(id, 0.5)
        dao.getItemById(id).test {
            assertEquals(10.5, awaitItem()!!.currentQuantity, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── low stock threshold ───────────────────────────────────────────────────

    @Test fun dashboardDaoDetectsLowStockItem() = runTest {
        val dashDao = db.dashboardDao()
        // 5 bags at 1 bag/day = 5 days remaining, below 7-day threshold
        dao.insertItem(item(qty = 5.0, rate = 1.0, threshold = 7))
        dashDao.getLowStockItems().test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun dashboardDaoDoesNotFlagWellStockedItem() = runTest {
        val dashDao = db.dashboardDao()
        // 30 bags at 1 bag/day = 30 days, above 7-day threshold
        dao.insertItem(item(qty = 30.0, rate = 1.0, threshold = 7))
        dashDao.getLowStockItems().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun dashboardDaoIgnoresItemWithZeroConsumptionRate() = runTest {
        val dashDao = db.dashboardDao()
        // consumptionRatePerDay = 0 → excluded from low stock query
        dao.insertItem(item(qty = 0.0, rate = 0.0, threshold = 7))
        dashDao.getLowStockItems().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun customThresholdIsRespected() = runTest {
        val dashDao = db.dashboardDao()
        // 10 days supply vs 14-day threshold → low stock
        dao.insertItem(item(qty = 10.0, rate = 1.0, threshold = 14))
        dashDao.getLowStockItems().test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
