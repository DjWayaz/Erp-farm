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

/**
 * Tests for DashboardDao — verifies the aggregate SQL queries that power
 * the Dashboard screen. Uses a real in-memory Room database.
 */
@RunWith(AndroidJUnit4::class)
class DashboardDaoTest {

    private lateinit var db: FarmDatabase
    private val dao get() = db.dashboardDao()
    private val poultryDao get() = db.poultryDao()
    private val fieldDao get() = db.fieldDao()
    private val financeDao get() = db.financeDao()
    private val inventoryDao get() = db.inventoryDao()

    private val today = LocalDate.of(2025, 7, 10)
    private val start = today.withDayOfMonth(1).toEpochDay()
    private val end = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), FarmDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }

    // ── getTotalLiveBirds ─────────────────────────────────────────────────────

    @Test fun getTotalLiveBirdsIsZeroWhenEmpty() = runTest {
        dao.getTotalLiveBirds().test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getTotalLiveBirdsSumsAllActiveBatches() = runTest {
        poultryDao.insertBatch(PoultryBatchEntity(name = "A", type = PoultryType.BROILER, dateAcquired = today, initialCount = 200, aliveCount = 195))
        poultryDao.insertBatch(PoultryBatchEntity(name = "B", type = PoultryType.LAYER, dateAcquired = today, initialCount = 300, aliveCount = 290))
        dao.getTotalLiveBirds().test {
            assertEquals(485, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getTotalLiveBirdsExcludesArchivedBatches() = runTest {
        val id1 = poultryDao.insertBatch(PoultryBatchEntity(name = "Active", type = PoultryType.BROILER, dateAcquired = today, initialCount = 100, aliveCount = 100))
        val id2 = poultryDao.insertBatch(PoultryBatchEntity(name = "Archived", type = PoultryType.BROILER, dateAcquired = today, initialCount = 200, aliveCount = 200))
        poultryDao.archiveBatch(id2)
        dao.getTotalLiveBirds().test {
            assertEquals(100, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getMonthlyExpenses ────────────────────────────────────────────────────

    @Test fun getMonthlyExpensesIsZeroWhenEmpty() = runTest {
        dao.getMonthlyExpenses(start, end).test {
            assertEquals(0.0, awaitItem(), 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getMonthlyExpensesSumsCorrectly() = runTest {
        financeDao.insertTransaction(TransactionEntity(date = today, type = TransactionType.EXPENSE, category = TransactionCategory.FEED, amount = 100.0, description = "Feed"))
        financeDao.insertTransaction(TransactionEntity(date = today, type = TransactionType.EXPENSE, category = TransactionCategory.SEEDS, amount = 50.0, description = "Seeds"))
        dao.getMonthlyExpenses(start, end).test {
            assertEquals(150.0, awaitItem(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getMonthlyExpensesExcludesIncome() = runTest {
        financeDao.insertTransaction(TransactionEntity(date = today, type = TransactionType.INCOME, category = TransactionCategory.SALES, amount = 999.0, description = "Sales"))
        financeDao.insertTransaction(TransactionEntity(date = today, type = TransactionType.EXPENSE, category = TransactionCategory.FEED, amount = 80.0, description = "Feed"))
        dao.getMonthlyExpenses(start, end).test {
            assertEquals(80.0, awaitItem(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getMonthlyExpensesExcludesPreviousMonth() = runTest {
        val prevMonth = today.minusMonths(1)
        financeDao.insertTransaction(TransactionEntity(date = prevMonth, type = TransactionType.EXPENSE, category = TransactionCategory.FEED, amount = 999.0, description = "Old"))
        financeDao.insertTransaction(TransactionEntity(date = today, type = TransactionType.EXPENSE, category = TransactionCategory.FEED, amount = 50.0, description = "Current"))
        dao.getMonthlyExpenses(start, end).test {
            assertEquals(50.0, awaitItem(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getMonthlyIncome ──────────────────────────────────────────────────────

    @Test fun getMonthlyIncomeSumsCorrectly() = runTest {
        financeDao.insertTransaction(TransactionEntity(date = today, type = TransactionType.INCOME, category = TransactionCategory.SALES, amount = 200.0, description = "Eggs"))
        financeDao.insertTransaction(TransactionEntity(date = today, type = TransactionType.INCOME, category = TransactionCategory.SALES, amount = 150.0, description = "Broilers"))
        dao.getMonthlyIncome(start, end).test {
            assertEquals(350.0, awaitItem(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getMonthlyIncomeExcludesExpenses() = runTest {
        financeDao.insertTransaction(TransactionEntity(date = today, type = TransactionType.EXPENSE, category = TransactionCategory.FEED, amount = 999.0, description = "Feed"))
        dao.getMonthlyIncome(start, end).test {
            assertEquals(0.0, awaitItem(), 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getActiveFields ───────────────────────────────────────────────────────

    @Test fun getActiveFieldsExcludesArchivedFields() = runTest {
        val id1 = fieldDao.insertField(FieldEntity(name = "Active", sizeHectares = 1.0, cropType = "Maize", plantingDate = today))
        val id2 = fieldDao.insertField(FieldEntity(name = "Archived", sizeHectares = 2.0, cropType = "Beans", plantingDate = today))
        fieldDao.archiveField(id2)
        dao.getActiveFields().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Active", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getUpcomingVaccinations ───────────────────────────────────────────────

    @Test fun getUpcomingVaccinationsExcludesPastDates() = runTest {
        val batchId = poultryDao.insertBatch(PoultryBatchEntity(name = "B", type = PoultryType.BROILER, dateAcquired = today, initialCount = 100, aliveCount = 100))
        poultryDao.insertVaccination(VaccinationEntity(batchId = batchId, vaccineName = "Past", dueDate = today.minusDays(1)))
        poultryDao.insertVaccination(VaccinationEntity(batchId = batchId, vaccineName = "Future", dueDate = today.plusDays(3)))
        val todayEpoch = today.toEpochDay()
        val warningEpoch = today.plusDays(7).toEpochDay()
        dao.getUpcomingVaccinations(todayEpoch, warningEpoch).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Future", result[0].vaccineName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getUpcomingVaccinationsExcludesAlreadyAdministered() = runTest {
        val batchId = poultryDao.insertBatch(PoultryBatchEntity(name = "B", type = PoultryType.BROILER, dateAcquired = today, initialCount = 100, aliveCount = 100))
        val vaxId = poultryDao.insertVaccination(VaccinationEntity(batchId = batchId, vaccineName = "Newcastle", dueDate = today.plusDays(2)))
        // Mark as administered
        val vax = VaccinationEntity(id = vaxId, batchId = batchId, vaccineName = "Newcastle", dueDate = today.plusDays(2), administeredDate = today)
        poultryDao.updateVaccination(vax)
        val todayEpoch = today.toEpochDay()
        dao.getUpcomingVaccinations(todayEpoch, today.plusDays(7).toEpochDay()).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getUpcomingVaccinationsExcludesBeyondWarningWindow() = runTest {
        val batchId = poultryDao.insertBatch(PoultryBatchEntity(name = "B", type = PoultryType.BROILER, dateAcquired = today, initialCount = 100, aliveCount = 100))
        poultryDao.insertVaccination(VaccinationEntity(batchId = batchId, vaccineName = "Far Future", dueDate = today.plusDays(14)))
        val todayEpoch = today.toEpochDay()
        val warningEpoch = today.plusDays(7).toEpochDay()
        dao.getUpcomingVaccinations(todayEpoch, warningEpoch).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
