package com.farmapp.repository

import app.cash.turbine.test
import com.farmapp.data.local.FarmDatabase
import com.farmapp.data.local.dao.PoultryDao
import com.farmapp.data.local.entity.*
import com.farmapp.data.repository.PoultryRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.time.LocalDate

class PoultryRepositoryTest {

    private lateinit var dao: PoultryDao
    private lateinit var db: FarmDatabase
    private lateinit var repo: PoultryRepository

    private val today = LocalDate.of(2025, 3, 20)

    private fun batch(id: Long = 1L, name: String = "Batch A", type: PoultryType = PoultryType.BROILER, count: Int = 100) =
        PoultryBatchEntity(id = id, name = name, type = type, dateAcquired = today, initialCount = count, aliveCount = count, costPerBird = 2.50)

    private fun vax(id: Long = 0L, batchId: Long = 1L, name: String = "Newcastle", dueDate: LocalDate = today.plusDays(7)) =
        VaccinationEntity(id = id, batchId = batchId, vaccineName = name, dueDate = dueDate)

    @Before fun setUp() {
        dao = mock()
        db = mock()
        whenever(db.runInTransaction(any())).thenAnswer { (it.arguments[0] as Runnable).run() }
        repo = PoultryRepository(dao, db)
    }

    // ── saveBatch ─────────────────────────────────────────────────────────────

    @Test fun `saveBatch delegates to dao and returns id`() = runTest {
        whenever(dao.insertBatch(any())).thenReturn(1L)
        assertEquals(1L, repo.saveBatch(batch()))
        verify(dao).insertBatch(any())
    }

    @Test fun `saveBatch for LAYER type passes LAYER to dao`() = runTest {
        val b = batch(type = PoultryType.LAYER, count = 500)
        whenever(dao.insertBatch(any())).thenReturn(2L)
        repo.saveBatch(b)
        verify(dao).insertBatch(argThat { type == PoultryType.LAYER && initialCount == 500 })
    }

    @Test fun `saveBatch for KIENYEJI type is accepted`() = runTest {
        whenever(dao.insertBatch(any())).thenReturn(3L)
        repo.saveBatch(batch(type = PoultryType.KIENYEJI))
        verify(dao).insertBatch(argThat { type == PoultryType.KIENYEJI })
    }

    // ── getAllActiveBatches ────────────────────────────────────────────────────

    @Test fun `getAllActiveBatches emits dao flow`() = runTest {
        whenever(dao.getAllActiveBatches()).thenReturn(flowOf(listOf(batch(1), batch(2, "Batch B"))))
        repo.getAllActiveBatches().test {
            assertEquals(2, awaitItem().size)
            awaitComplete()
        }
    }

    @Test fun `getAllActiveBatches returns empty when none active`() = runTest {
        whenever(dao.getAllActiveBatches()).thenReturn(flowOf(emptyList()))
        repo.getAllActiveBatches().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // ── getBatchById ──────────────────────────────────────────────────────────

    @Test fun `getBatchById returns batch when found`() = runTest {
        whenever(dao.getBatchById(5L)).thenReturn(flowOf(batch(5L)))
        repo.getBatchById(5L).test {
            assertEquals(5L, awaitItem()!!.id)
            awaitComplete()
        }
    }

    @Test fun `getBatchById returns null when not found`() = runTest {
        whenever(dao.getBatchById(99L)).thenReturn(flowOf(null))
        repo.getBatchById(99L).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    // ── archiveBatch ──────────────────────────────────────────────────────────

    @Test fun `archiveBatch delegates to dao with correct id`() = runTest {
        repo.archiveBatch(4L)
        verify(dao).archiveBatch(4L)
    }

    // ── recordMortality ───────────────────────────────────────────────────────

    @Test fun `recordMortality inserts mortality with correct count`() = runTest {
        whenever(dao.insertMortality(any())).thenReturn(1L)
        repo.recordMortality(batchId = 1L, count = 5, cause = "Disease")
        verify(dao).insertMortality(argThat { count == 5 && batchId == 1L })
    }

    @Test fun `recordMortality decrements alive count by same number`() = runTest {
        whenever(dao.insertMortality(any())).thenReturn(1L)
        repo.recordMortality(batchId = 1L, count = 3, cause = null)
        verify(dao).decrementAliveCount(1L, 3)
    }

    @Test fun `recordMortality stores cause when provided`() = runTest {
        whenever(dao.insertMortality(any())).thenReturn(1L)
        repo.recordMortality(1L, 2, "Cold stress")
        verify(dao).insertMortality(argThat { cause == "Cold stress" })
    }

    @Test fun `recordMortality stores null cause when not provided`() = runTest {
        whenever(dao.insertMortality(any())).thenReturn(1L)
        repo.recordMortality(1L, 1, null)
        verify(dao).insertMortality(argThat { cause == null })
    }

    @Test fun `recordMortality runs inside db transaction`() = runTest {
        whenever(dao.insertMortality(any())).thenReturn(1L)
        repo.recordMortality(1L, 2, null)
        verify(db).runInTransaction(any())
    }

    @Test fun `recordMortality inserts before decrementing`() = runTest {
        val order = mutableListOf<String>()
        whenever(dao.insertMortality(any())).thenAnswer { order.add("insert"); 1L }
        whenever(dao.decrementAliveCount(any(), any())).thenAnswer { order.add("decrement") }
        repo.recordMortality(1L, 1, null)
        assertEquals(listOf("insert", "decrement"), order)
    }

    // ── addVaccination ────────────────────────────────────────────────────────

    @Test fun `addVaccination delegates to dao and returns id`() = runTest {
        whenever(dao.insertVaccination(any())).thenReturn(5L)
        assertEquals(5L, repo.addVaccination(vax()))
        verify(dao).insertVaccination(any())
    }

    // ── markVaccinationDone ───────────────────────────────────────────────────

    @Test fun `markVaccinationDone sets administeredDate on updated entity`() = runTest {
        repo.markVaccinationDone(vax(id = 3L))
        verify(dao).updateVaccination(argThat { administeredDate != null })
    }

    @Test fun `markVaccinationDone preserves vaccineName and batchId`() = runTest {
        repo.markVaccinationDone(vax(id = 1L, batchId = 7L, name = "Gumboro"))
        verify(dao).updateVaccination(argThat { vaccineName == "Gumboro" && batchId == 7L })
    }

    @Test fun `markVaccinationDone does not change dueDate`() = runTest {
        val due = today.plusDays(3)
        repo.markVaccinationDone(vax(dueDate = due))
        verify(dao).updateVaccination(argThat { dueDate == due })
    }

    // ── deleteVaccination ─────────────────────────────────────────────────────

    @Test fun `deleteVaccination delegates to dao`() = runTest {
        val v = vax()
        repo.deleteVaccination(v)
        verify(dao).deleteVaccination(v)
    }

    // ── addFeedEvent ──────────────────────────────────────────────────────────

    @Test fun `addFeedEvent delegates to dao and returns id`() = runTest {
        val e = FeedEventEntity(batchId = 1L, date = today, bagsUsed = 3.5, costPerBag = 18.0)
        whenever(dao.insertFeedEvent(e)).thenReturn(8L)
        assertEquals(8L, repo.addFeedEvent(e))
        verify(dao).insertFeedEvent(e)
    }

    // ── getTotalFeedCostForBatch ───────────────────────────────────────────────

    @Test fun `getTotalFeedCostForBatch emits total from dao`() = runTest {
        whenever(dao.getTotalFeedCostForBatch(1L)).thenReturn(flowOf(126.0))
        repo.getTotalFeedCostForBatch(1L).test {
            assertEquals(126.0, awaitItem(), 0.001)
            awaitComplete()
        }
    }

    @Test fun `getTotalFeedCostForBatch emits zero when no events`() = runTest {
        whenever(dao.getTotalFeedCostForBatch(2L)).thenReturn(flowOf(0.0))
        repo.getTotalFeedCostForBatch(2L).test {
            assertEquals(0.0, awaitItem(), 0.0)
            awaitComplete()
        }
    }

    // ── addEggCount ───────────────────────────────────────────────────────────

    @Test fun `addEggCount delegates to dao and returns id`() = runTest {
        val e = EggCountEntity(batchId = 1L, date = today, count = 450)
        whenever(dao.insertEggCount(e)).thenReturn(11L)
        assertEquals(11L, repo.addEggCount(e))
        verify(dao).insertEggCount(e)
    }

    // ── getMortalitiesForBatch ────────────────────────────────────────────────

    @Test fun `getMortalitiesForBatch emits list from dao`() = runTest {
        val mortalities = listOf(
            MortalityEntity(batchId = 1L, date = today, count = 2, cause = "Disease"),
            MortalityEntity(batchId = 1L, date = today.minusDays(1), count = 1, cause = null)
        )
        whenever(dao.getMortalitiesForBatch(1L)).thenReturn(flowOf(mortalities))
        repo.getMortalitiesForBatch(1L).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals(2, result[0].count)
            awaitComplete()
        }
    }

    // ── getVaccinationsForBatch ───────────────────────────────────────────────

    @Test fun `getVaccinationsForBatch emits vaccinations from dao`() = runTest {
        val vaxList = listOf(vax(name = "Newcastle"), vax(name = "Gumboro"))
        whenever(dao.getVaccinationsForBatch(1L)).thenReturn(flowOf(vaxList))
        repo.getVaccinationsForBatch(1L).test {
            assertEquals(2, awaitItem().size)
            awaitComplete()
        }
    }

    // ── getFeedEventsForBatch ─────────────────────────────────────────────────

    @Test fun `getFeedEventsForBatch emits events from dao`() = runTest {
        val events = listOf(FeedEventEntity(batchId = 1L, date = today, bagsUsed = 2.0, costPerBag = 15.0))
        whenever(dao.getFeedEventsForBatch(1L)).thenReturn(flowOf(events))
        repo.getFeedEventsForBatch(1L).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    // ── getRecentEggCountsForBatch ────────────────────────────────────────────

    @Test fun `getRecentEggCountsForBatch emits egg counts from dao`() = runTest {
        val counts = listOf(EggCountEntity(batchId = 1L, date = today, count = 480))
        whenever(dao.getRecentEggCountsForBatch(1L)).thenReturn(flowOf(counts))
        repo.getRecentEggCountsForBatch(1L).test {
            assertEquals(480, awaitItem()[0].count)
            awaitComplete()
        }
    }

    // ── getUpcomingVaccinations ───────────────────────────────────────────────

    @Test fun `getUpcomingVaccinations delegates to dao with today epoch`() = runTest {
        val todayEpoch = today.toEpochDay()
        whenever(dao.getUpcomingVaccinations(todayEpoch)).thenReturn(flowOf(emptyList()))
        repo.getUpcomingVaccinations(todayEpoch).test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
        verify(dao).getUpcomingVaccinations(todayEpoch)
    }
}
