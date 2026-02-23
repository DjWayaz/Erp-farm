package com.farmapp.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import com.farmapp.data.local.entity.*
import com.farmapp.data.repository.PoultryRepository
import com.farmapp.ui.poultry.PoultryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
@OptIn(ExperimentalCoroutinesApi::class)
class PoultryViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: PoultryRepository
    private lateinit var context: Context
    private lateinit var vm: PoultryViewModel

    private val today = LocalDate.of(2025, 5, 10)

    private fun batch(id: Long = 1L, name: String = "Batch A", type: PoultryType = PoultryType.BROILER, count: Int = 100) =
        PoultryBatchEntity(id = id, name = name, type = type, dateAcquired = today, initialCount = count, aliveCount = count)

    private fun vax(id: Long = 1L, batchId: Long = 1L, name: String = "Newcastle") =
        VaccinationEntity(id = id, batchId = batchId, vaccineName = name, dueDate = today.plusDays(7))

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mock()
        // Initialise WorkManager with test driver so cancel/schedule don't crash
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(appContext)
        context = appContext
        whenever(repo.getAllActiveBatches()).thenReturn(flowOf(emptyList()))
        vm = PoultryViewModel(repo, context)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    // ── batches StateFlow ─────────────────────────────────────────────────────

    @Test fun `batches starts empty`() = runTest {
        vm.batches.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `batches emits list from repo`() = runTest {
        whenever(repo.getAllActiveBatches()).thenReturn(flowOf(listOf(batch(1), batch(2, "Batch B"))))
        val vm2 = PoultryViewModel(repo, context)
        vm2.batches.test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── saveBatch ─────────────────────────────────────────────────────────────

    @Test fun `saveBatch calls repo saveBatch with correct fields`() = runTest {
        whenever(repo.saveBatch(any())).thenReturn(1L)
        vm.saveBatch("Flock 1", PoultryType.BROILER, 200, 2.50)
        advanceUntilIdle()
        verify(repo).saveBatch(argThat {
            name == "Flock 1" && type == PoultryType.BROILER
                && initialCount == 200 && aliveCount == 200 && costPerBird == 2.50
        })
    }

    @Test fun `saveBatch sets aliveCount equal to initialCount`() = runTest {
        whenever(repo.saveBatch(any())).thenReturn(1L)
        vm.saveBatch("Layers", PoultryType.LAYER, 500, 3.0)
        advanceUntilIdle()
        verify(repo).saveBatch(argThat { initialCount == aliveCount && initialCount == 500 })
    }

    @Test fun `saveBatch with zero cost per bird`() = runTest {
        whenever(repo.saveBatch(any())).thenReturn(1L)
        vm.saveBatch("Free Range", PoultryType.KIENYEJI, 50, 0.0)
        advanceUntilIdle()
        verify(repo).saveBatch(argThat { costPerBird == 0.0 })
    }

    // ── recordMortality ───────────────────────────────────────────────────────

    @Test fun `recordMortality calls repo with correct batchId, count and cause`() = runTest {
        vm.recordMortality(1L, 5, "Disease")
        advanceUntilIdle()
        verify(repo).recordMortality(1L, 5, "Disease")
    }

    @Test fun `recordMortality with null cause`() = runTest {
        vm.recordMortality(2L, 1, null)
        advanceUntilIdle()
        verify(repo).recordMortality(2L, 1, null)
    }

    // ── addFeedEvent ──────────────────────────────────────────────────────────

    @Test fun `addFeedEvent calls repo with correct batchId, bags and cost`() = runTest {
        whenever(repo.addFeedEvent(any())).thenReturn(1L)
        vm.addFeedEvent(1L, 3.5, 18.0)
        advanceUntilIdle()
        verify(repo).addFeedEvent(argThat { batchId == 1L && bagsUsed == 3.5 && costPerBag == 18.0 })
    }

    // ── addEggCount ───────────────────────────────────────────────────────────

    @Test fun `addEggCount calls repo with correct batchId and count`() = runTest {
        whenever(repo.addEggCount(any())).thenReturn(1L)
        vm.addEggCount(1L, 450)
        advanceUntilIdle()
        verify(repo).addEggCount(argThat { batchId == 1L && count == 450 })
    }

    // ── markVaccinationDone ───────────────────────────────────────────────────

    @Test fun `markVaccinationDone calls repo markVaccinationDone`() = runTest {
        val v = vax()
        vm.markVaccinationDone(v)
        advanceUntilIdle()
        verify(repo).markVaccinationDone(v)
    }

    // ── deleteVaccination ─────────────────────────────────────────────────────

    @Test fun `deleteVaccination calls repo deleteVaccination`() = runTest {
        val v = vax()
        vm.deleteVaccination(v)
        advanceUntilIdle()
        verify(repo).deleteVaccination(v)
    }

    // ── archiveBatch ──────────────────────────────────────────────────────────

    @Test fun `archiveBatch calls repo archiveBatch`() = runTest {
        vm.archiveBatch(5L)
        advanceUntilIdle()
        verify(repo).archiveBatch(5L)
    }

    // ── delegated flows ───────────────────────────────────────────────────────

    @Test fun `getBatchById delegates to repo`() = runTest {
        whenever(repo.getBatchById(1L)).thenReturn(flowOf(batch()))
        vm.getBatchById(1L).test {
            assertNotNull(awaitItem())
            awaitComplete()
        }
    }

    @Test fun `getMortalities delegates to repo`() = runTest {
        val mortalities = listOf(MortalityEntity(batchId = 1L, date = today, count = 2))
        whenever(repo.getMortalitiesForBatch(1L)).thenReturn(flowOf(mortalities))
        vm.getMortalities(1L).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test fun `getVaccinations delegates to repo`() = runTest {
        whenever(repo.getVaccinationsForBatch(1L)).thenReturn(flowOf(listOf(vax())))
        vm.getVaccinations(1L).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test fun `getFeedEvents delegates to repo`() = runTest {
        val events = listOf(FeedEventEntity(batchId = 1L, date = today, bagsUsed = 2.0, costPerBag = 15.0))
        whenever(repo.getFeedEventsForBatch(1L)).thenReturn(flowOf(events))
        vm.getFeedEvents(1L).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test fun `getEggCounts delegates to repo`() = runTest {
        val counts = listOf(EggCountEntity(batchId = 1L, date = today, count = 480))
        whenever(repo.getRecentEggCountsForBatch(1L)).thenReturn(flowOf(counts))
        vm.getEggCounts(1L).test {
            assertEquals(480, awaitItem()[0].count)
            awaitComplete()
        }
    }

    @Test fun `getTotalFeedCost delegates to repo`() = runTest {
        whenever(repo.getTotalFeedCostForBatch(1L)).thenReturn(flowOf(63.0))
        vm.getTotalFeedCost(1L).test {
            assertEquals(63.0, awaitItem(), 0.001)
            awaitComplete()
        }
    }
}
