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
class PoultryDaoTest {

    private lateinit var db: FarmDatabase
    private val dao get() = db.poultryDao()
    private val today = LocalDate.of(2025, 5, 1)

    private fun batch(name: String = "Batch A", type: PoultryType = PoultryType.BROILER, count: Int = 100) =
        PoultryBatchEntity(name = name, type = type, dateAcquired = today, initialCount = count, aliveCount = count, costPerBird = 2.50)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), FarmDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }

    // ── batch CRUD ────────────────────────────────────────────────────────────

    @Test fun insertBatchAndGetById() = runTest {
        val id = dao.insertBatch(batch("Flock A", PoultryType.LAYER, 500))
        dao.getBatchById(id).test {
            val b = awaitItem()!!
            assertEquals("Flock A", b.name)
            assertEquals(PoultryType.LAYER, b.type)
            assertEquals(500, b.initialCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getAllActiveBatchesOnlyReturnsActive() = runTest {
        val id1 = dao.insertBatch(batch("Active"))
        val id2 = dao.insertBatch(batch("Archived"))
        dao.archiveBatch(id2)
        dao.getAllActiveBatches().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Active", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun archiveBatchSetsIsActiveFalse() = runTest {
        val id = dao.insertBatch(batch())
        dao.archiveBatch(id)
        dao.getBatchById(id).test {
            assertFalse(awaitItem()!!.isActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun decrementAliveCount() = runTest {
        val id = dao.insertBatch(batch(count = 100))
        dao.decrementAliveCount(id, 5)
        dao.getBatchById(id).test {
            assertEquals(95, awaitItem()!!.aliveCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun allPoultryTypesCanBeInserted() = runTest {
        for (type in PoultryType.values()) {
            dao.insertBatch(batch(type = type))
        }
        dao.getAllActiveBatches().test {
            assertEquals(PoultryType.values().size, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── mortalities ───────────────────────────────────────────────────────────

    @Test fun insertMortalityAndRetrieve() = runTest {
        val batchId = dao.insertBatch(batch())
        dao.insertMortality(MortalityEntity(batchId = batchId, date = today, count = 3, cause = "Disease"))
        dao.getMortalitiesForBatch(batchId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(3, result[0].count)
            assertEquals("Disease", result[0].cause)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun mortalityWithNullCause() = runTest {
        val batchId = dao.insertBatch(batch())
        dao.insertMortality(MortalityEntity(batchId = batchId, date = today, count = 1, cause = null))
        dao.getMortalitiesForBatch(batchId).test {
            assertNull(awaitItem()[0].cause)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── vaccinations ──────────────────────────────────────────────────────────

    @Test fun insertVaccinationAndRetrieve() = runTest {
        val batchId = dao.insertBatch(batch())
        dao.insertVaccination(VaccinationEntity(batchId = batchId, vaccineName = "Newcastle", dueDate = today.plusDays(7)))
        dao.getVaccinationsForBatch(batchId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Newcastle", result[0].vaccineName)
            assertNull(result[0].administeredDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun markVaccinationAdministered() = runTest {
        val batchId = dao.insertBatch(batch())
        dao.insertVaccination(VaccinationEntity(batchId = batchId, vaccineName = "Gumboro", dueDate = today))
        dao.getVaccinationsForBatch(batchId).test {
            val v = awaitItem()[0]
            dao.updateVaccination(v.copy(administeredDate = today))
            val updated = awaitItem()[0]
            assertEquals(today, updated.administeredDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun deleteVaccination() = runTest {
        val batchId = dao.insertBatch(batch())
        dao.insertVaccination(VaccinationEntity(batchId = batchId, vaccineName = "IBD", dueDate = today))
        dao.getVaccinationsForBatch(batchId).test {
            val v = awaitItem()[0]
            dao.deleteVaccination(v)
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getUpcomingVaccinations() = runTest {
        val batchId = dao.insertBatch(batch())
        val todayEpoch = today.toEpochDay()
        dao.insertVaccination(VaccinationEntity(batchId = batchId, vaccineName = "Newcastle", dueDate = today.plusDays(3)))
        dao.insertVaccination(VaccinationEntity(batchId = batchId, vaccineName = "Gumboro", dueDate = today.minusDays(1)))
        dao.insertVaccination(VaccinationEntity(batchId = batchId, vaccineName = "Done", dueDate = today, administeredDate = today))
        dao.getUpcomingVaccinations(todayEpoch).test {
            val result = awaitItem()
            // Only future unadministered (Newcastle), not past or done
            assertEquals(1, result.size)
            assertEquals("Newcastle", result[0].vaccineName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── feed events ───────────────────────────────────────────────────────────

    @Test fun insertFeedEventAndRetrieve() = runTest {
        val batchId = dao.insertBatch(batch())
        dao.insertFeedEvent(FeedEventEntity(batchId = batchId, date = today, bagsUsed = 3.5, costPerBag = 18.0))
        dao.getFeedEventsForBatch(batchId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(3.5, result[0].bagsUsed, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getTotalFeedCostSumsAllEvents() = runTest {
        val batchId = dao.insertBatch(batch())
        dao.insertFeedEvent(FeedEventEntity(batchId = batchId, date = today, bagsUsed = 3.0, costPerBag = 18.0))
        dao.insertFeedEvent(FeedEventEntity(batchId = batchId, date = today.plusDays(1), bagsUsed = 2.0, costPerBag = 20.0))
        dao.getTotalFeedCostForBatch(batchId).test {
            // 3*18 + 2*20 = 54 + 40 = 94
            assertEquals(94.0, awaitItem(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getTotalFeedCostReturnsZeroWhenNoEvents() = runTest {
        val batchId = dao.insertBatch(batch())
        dao.getTotalFeedCostForBatch(batchId).test {
            assertEquals(0.0, awaitItem(), 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── egg counts ────────────────────────────────────────────────────────────

    @Test fun insertEggCountAndRetrieve() = runTest {
        val batchId = dao.insertBatch(batch())
        dao.insertEggCount(EggCountEntity(batchId = batchId, date = today, count = 450))
        dao.getRecentEggCountsForBatch(batchId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(450, result[0].count)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getTotalEggsForPeriod() = runTest {
        val batchId = dao.insertBatch(batch())
        dao.insertEggCount(EggCountEntity(batchId = batchId, date = today, count = 450))
        dao.insertEggCount(EggCountEntity(batchId = batchId, date = today.plusDays(1), count = 460))
        dao.insertEggCount(EggCountEntity(batchId = batchId, date = today.plusDays(10), count = 400))
        val from = today.toEpochDay()
        val to = today.plusDays(2).toEpochDay()
        dao.getTotalEggsForPeriod(batchId, from, to).test {
            assertEquals(910, awaitItem()) // 450+460, not the day 10 one
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun recentEggCountsLimitedTo30() = runTest {
        val batchId = dao.insertBatch(batch())
        repeat(35) { i ->
            dao.insertEggCount(EggCountEntity(batchId = batchId, date = today.plusDays(i.toLong()), count = 400))
        }
        dao.getRecentEggCountsForBatch(batchId).test {
            assertEquals(30, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
