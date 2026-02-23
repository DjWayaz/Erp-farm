package com.farmapp.repository

import app.cash.turbine.test
import com.farmapp.data.local.dao.DashboardDao
import com.farmapp.data.local.entity.*
import com.farmapp.data.repository.DashboardRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.time.LocalDate

class DashboardRepositoryTest {

    private lateinit var dao: DashboardDao
    private lateinit var repo: DashboardRepository

    private val today = LocalDate.of(2025, 7, 15)
    private val start = today.withDayOfMonth(1).toEpochDay()
    private val end = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()

    @Before fun setUp() {
        dao = mock()
        repo = DashboardRepository(dao)
    }

    @Test fun `getTotalLiveBirds emits count from dao`() = runTest {
        whenever(dao.getTotalLiveBirds()).thenReturn(flowOf(350))
        repo.getTotalLiveBirds().test {
            assertEquals(350, awaitItem())
            awaitComplete()
        }
    }

    @Test fun `getTotalLiveBirds emits 0 when no active batches`() = runTest {
        whenever(dao.getTotalLiveBirds()).thenReturn(flowOf(0))
        repo.getTotalLiveBirds().test {
            assertEquals(0, awaitItem())
            awaitComplete()
        }
    }

    @Test fun `getMonthlyExpenses delegates range to dao`() = runTest {
        whenever(dao.getMonthlyExpenses(start, end)).thenReturn(flowOf(420.50))
        repo.getMonthlyExpenses(start, end).test {
            assertEquals(420.50, awaitItem(), 0.001)
            awaitComplete()
        }
        verify(dao).getMonthlyExpenses(start, end)
    }

    @Test fun `getMonthlyIncome delegates range to dao`() = runTest {
        whenever(dao.getMonthlyIncome(start, end)).thenReturn(flowOf(750.0))
        repo.getMonthlyIncome(start, end).test {
            assertEquals(750.0, awaitItem(), 0.001)
            awaitComplete()
        }
        verify(dao).getMonthlyIncome(start, end)
    }

    @Test fun `getActiveFields emits list from dao`() = runTest {
        val fields = listOf(
            FieldEntity(name = "Plot A", sizeHectares = 1.0, cropType = "Maize", plantingDate = today)
        )
        whenever(dao.getActiveFields()).thenReturn(flowOf(fields))
        repo.getActiveFields().test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test fun `getActiveBatches emits active batches from dao`() = runTest {
        val batches = listOf(
            PoultryBatchEntity(name = "Flock 1", type = PoultryType.BROILER, dateAcquired = today, initialCount = 200, aliveCount = 195)
        )
        whenever(dao.getActiveBatches()).thenReturn(flowOf(batches))
        repo.getActiveBatches().test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test fun `getUpcomingVaccinations delegates range to dao`() = runTest {
        val todayEpoch = today.toEpochDay()
        val sevenDays = today.plusDays(7).toEpochDay()
        val vax = listOf(VaccinationEntity(batchId = 1L, vaccineName = "Newcastle", dueDate = today.plusDays(3)))
        whenever(dao.getUpcomingVaccinations(todayEpoch, sevenDays)).thenReturn(flowOf(vax))
        repo.getUpcomingVaccinations(todayEpoch, sevenDays).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
        verify(dao).getUpcomingVaccinations(todayEpoch, sevenDays)
    }

    @Test fun `getLowStockItems emits items below threshold from dao`() = runTest {
        val lowStock = listOf(
            InventoryItemEntity(name = "Layer Mash", unit = "bags", currentQuantity = 3.0, consumptionRatePerDay = 1.0, lowStockThresholdDays = 7)
        )
        whenever(dao.getLowStockItems()).thenReturn(flowOf(lowStock))
        repo.getLowStockItems().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Layer Mash", result[0].name)
            awaitComplete()
        }
    }

    @Test fun `getLowStockItems emits empty when all items are well stocked`() = runTest {
        whenever(dao.getLowStockItems()).thenReturn(flowOf(emptyList()))
        repo.getLowStockItems().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }
}
