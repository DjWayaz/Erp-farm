package com.farmapp.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.farmapp.data.local.entity.*
import com.farmapp.data.repository.DashboardRepository
import com.farmapp.ui.dashboard.DashboardUiState
import com.farmapp.ui.dashboard.DashboardViewModel
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
class DashboardViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: DashboardRepository
    private lateinit var vm: DashboardViewModel

    private val today = LocalDate.now()

    private fun stubRepo(
        birds: Int = 0,
        expenses: Double = 0.0,
        income: Double = 0.0,
        fields: List<FieldEntity> = emptyList(),
        batches: List<PoultryBatchEntity> = emptyList(),
        vaccinations: List<VaccinationEntity> = emptyList(),
        lowStock: List<InventoryItemEntity> = emptyList()
    ) {
        whenever(repo.getTotalLiveBirds()).thenReturn(flowOf(birds))
        whenever(repo.getMonthlyExpenses(any(), any())).thenReturn(flowOf(expenses))
        whenever(repo.getMonthlyIncome(any(), any())).thenReturn(flowOf(income))
        whenever(repo.getActiveFields()).thenReturn(flowOf(fields))
        whenever(repo.getActiveBatches()).thenReturn(flowOf(batches))
        whenever(repo.getUpcomingVaccinations(any(), any())).thenReturn(flowOf(vaccinations))
        whenever(repo.getLowStockItems()).thenReturn(flowOf(lowStock))
    }

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mock()
        stubRepo()
        vm = DashboardViewModel(repo)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test fun `uiState starts with all zero and empty defaults`() = runTest {
        vm.uiState.test {
            val s = awaitItem()
            assertEquals(0, s.totalLiveBirds)
            assertEquals(0.0, s.monthlyIncome, 0.0)
            assertEquals(0.0, s.monthlyExpenses, 0.0)
            assertEquals(0.0, s.monthlyProfit, 0.0)
            assertTrue(s.activeFields.isEmpty())
            assertTrue(s.activeBatches.isEmpty())
            assertTrue(s.upcomingVaccinations.isEmpty())
            assertTrue(s.lowStockItems.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── live birds ────────────────────────────────────────────────────────────

    @Test fun `uiState reflects totalLiveBirds from repo`() = runTest {
        stubRepo(birds = 450)
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            assertEquals(450, awaitItem().totalLiveBirds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `totalLiveBirds is zero with no active batches`() = runTest {
        stubRepo(birds = 0)
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            assertEquals(0, awaitItem().totalLiveBirds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── finances ──────────────────────────────────────────────────────────────

    @Test fun `uiState reflects monthlyIncome and monthlyExpenses`() = runTest {
        stubRepo(income = 700.0, expenses = 420.0)
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            val s = awaitItem()
            assertEquals(700.0, s.monthlyIncome, 0.001)
            assertEquals(420.0, s.monthlyExpenses, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `monthlyProfit is income minus expenses`() = runTest {
        stubRepo(income = 600.0, expenses = 350.0)
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            assertEquals(250.0, awaitItem().monthlyProfit, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `monthlyProfit is negative when expenses exceed income`() = runTest {
        stubRepo(income = 100.0, expenses = 300.0)
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            assertEquals(-200.0, awaitItem().monthlyProfit, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `monthlyProfit is zero when balanced`() = runTest {
        stubRepo(income = 200.0, expenses = 200.0)
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            assertEquals(0.0, awaitItem().monthlyProfit, 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── active fields ─────────────────────────────────────────────────────────

    @Test fun `uiState reflects active fields`() = runTest {
        val fields = listOf(
            FieldEntity(id = 1, name = "North Plot", sizeHectares = 2.0, cropType = "Maize", plantingDate = today),
            FieldEntity(id = 2, name = "South Plot", sizeHectares = 1.5, cropType = "Beans", plantingDate = today)
        )
        stubRepo(fields = fields)
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            val s = awaitItem()
            assertEquals(2, s.activeFields.size)
            assertEquals("North Plot", s.activeFields[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `uiState has empty fields when no crops planted`() = runTest {
        stubRepo(fields = emptyList())
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            assertTrue(awaitItem().activeFields.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── active batches ────────────────────────────────────────────────────────

    @Test fun `uiState reflects active poultry batches`() = runTest {
        val batches = listOf(
            PoultryBatchEntity(id = 1, name = "Flock A", type = PoultryType.BROILER, dateAcquired = today, initialCount = 200, aliveCount = 195)
        )
        stubRepo(batches = batches)
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            assertEquals(1, awaitItem().activeBatches.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── upcoming vaccinations ─────────────────────────────────────────────────

    @Test fun `uiState shows upcoming vaccinations within 7 days`() = runTest {
        val vax = listOf(
            VaccinationEntity(batchId = 1, vaccineName = "Newcastle", dueDate = today.plusDays(3))
        )
        stubRepo(vaccinations = vax)
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            assertEquals(1, awaitItem().upcomingVaccinations.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `uiState has no vaccinations when none due soon`() = runTest {
        stubRepo(vaccinations = emptyList())
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            assertTrue(awaitItem().upcomingVaccinations.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── low stock items ───────────────────────────────────────────────────────

    @Test fun `uiState reflects low stock items`() = runTest {
        val lowStock = listOf(
            InventoryItemEntity(id = 1, name = "Layer Mash", unit = "bags", currentQuantity = 3.0, consumptionRatePerDay = 1.0)
        )
        stubRepo(lowStock = lowStock)
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            val s = awaitItem()
            assertEquals(1, s.lowStockItems.size)
            assertEquals("Layer Mash", s.lowStockItems[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `uiState has empty lowStockItems when all well stocked`() = runTest {
        stubRepo(lowStock = emptyList())
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            assertTrue(awaitItem().lowStockItems.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── combined state ────────────────────────────────────────────────────────

    @Test fun `uiState combines all repo sources correctly`() = runTest {
        stubRepo(
            birds = 300,
            income = 500.0,
            expenses = 200.0,
            fields = listOf(FieldEntity(name = "Plot", sizeHectares = 1.0, cropType = "Maize", plantingDate = today)),
            batches = listOf(PoultryBatchEntity(name = "B", type = PoultryType.LAYER, dateAcquired = today, initialCount = 100, aliveCount = 100)),
            lowStock = listOf(InventoryItemEntity(name = "Feed", unit = "kg", currentQuantity = 2.0, consumptionRatePerDay = 1.0))
        )
        val vm2 = DashboardViewModel(repo)
        vm2.uiState.test {
            val s = awaitItem()
            assertEquals(300, s.totalLiveBirds)
            assertEquals(500.0, s.monthlyIncome, 0.001)
            assertEquals(300.0, s.monthlyProfit, 0.001)
            assertEquals(1, s.activeFields.size)
            assertEquals(1, s.activeBatches.size)
            assertEquals(1, s.lowStockItems.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── DashboardUiState data class ───────────────────────────────────────────

    @Test fun `DashboardUiState monthlyProfit is computed not stored`() {
        val s = DashboardUiState(monthlyIncome = 1000.0, monthlyExpenses = 600.0)
        assertEquals(400.0, s.monthlyProfit, 0.001)
        // Changing the copy recomputes
        val s2 = s.copy(monthlyExpenses = 800.0)
        assertEquals(200.0, s2.monthlyProfit, 0.001)
    }
}
