package com.farmapp.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.farmapp.data.local.entity.*
import com.farmapp.data.repository.FieldRepository
import com.farmapp.ui.crop.CropViewModel
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
class CropViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FieldRepository
    private lateinit var vm: CropViewModel

    private val today = LocalDate.of(2025, 4, 1)

    private fun field(id: Long = 1L, name: String = "Field A", crop: String = "Maize") =
        FieldEntity(id = id, name = name, sizeHectares = 1.5, cropType = crop, plantingDate = today)

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mock()
        whenever(repo.getAllActiveFields()).thenReturn(flowOf(emptyList()))
        vm = CropViewModel(repo)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    // ── fields StateFlow ──────────────────────────────────────────────────────

    @Test fun `fields StateFlow emits empty list initially`() = runTest {
        vm.fields.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `fields StateFlow emits list from repository`() = runTest {
        val fieldList = listOf(field(1), field(2, "South Plot"))
        whenever(repo.getAllActiveFields()).thenReturn(flowOf(fieldList))
        val vm2 = CropViewModel(repo)
        vm2.fields.test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── saveField ─────────────────────────────────────────────────────────────

    @Test fun `saveField calls repo saveField with correct entity`() = runTest {
        whenever(repo.saveField(any())).thenReturn(1L)
        vm.saveField("North Plot", 2.0, "Maize", "SC403", today, null, "Sandy Loam", "Irrigated")
        advanceUntilIdle()
        verify(repo).saveField(argThat {
            name == "North Plot" && sizeHectares == 2.0 && cropType == "Maize"
                && variety == "SC403" && soilType == "Sandy Loam"
        })
    }

    @Test fun `saveField strips blank variety to null`() = runTest {
        whenever(repo.saveField(any())).thenReturn(1L)
        vm.saveField("Field", 1.0, "Beans", "   ", today, null, null, null)
        advanceUntilIdle()
        verify(repo).saveField(argThat { variety == null })
    }

    @Test fun `saveField strips blank soilType to null`() = runTest {
        whenever(repo.saveField(any())).thenReturn(1L)
        vm.saveField("Field", 1.0, "Beans", null, today, null, "  ", null)
        advanceUntilIdle()
        verify(repo).saveField(argThat { soilType == null })
    }

    @Test fun `saveField strips blank notes to null`() = runTest {
        whenever(repo.saveField(any())).thenReturn(1L)
        vm.saveField("Field", 1.0, "Beans", null, today, null, null, "   ")
        advanceUntilIdle()
        verify(repo).saveField(argThat { notes == null })
    }

    @Test fun `saveField preserves expectedHarvestDate`() = runTest {
        val harvest = today.plusMonths(4)
        whenever(repo.saveField(any())).thenReturn(1L)
        vm.saveField("Field", 1.0, "Maize", null, today, harvest, null, null)
        advanceUntilIdle()
        verify(repo).saveField(argThat { expectedHarvestDate == harvest })
    }

    // ── addActivity ───────────────────────────────────────────────────────────

    @Test fun `addActivity calls repo addActivity with correct data`() = runTest {
        whenever(repo.addActivity(any())).thenReturn(1L)
        vm.addActivity(1L, ActivityType.FERTILIZED, "Applied NPK", 25.0, today)
        advanceUntilIdle()
        verify(repo).addActivity(argThat {
            fieldId == 1L && type == ActivityType.FERTILIZED && cost == 25.0
        })
    }

    @Test fun `addActivity strips blank notes to null`() = runTest {
        whenever(repo.addActivity(any())).thenReturn(1L)
        vm.addActivity(1L, ActivityType.WEEDED, "  ", 0.0, today)
        advanceUntilIdle()
        verify(repo).addActivity(argThat { notes == null })
    }

    @Test fun `addActivity preserves non-blank notes`() = runTest {
        whenever(repo.addActivity(any())).thenReturn(1L)
        vm.addActivity(1L, ActivityType.SPRAYED, "Used chlorpyrifos", 15.0, today)
        advanceUntilIdle()
        verify(repo).addActivity(argThat { notes == "Used chlorpyrifos" })
    }

    // ── deleteActivity ────────────────────────────────────────────────────────

    @Test fun `deleteActivity calls repo deleteActivity`() = runTest {
        val a = ActivityEntity(fieldId = 1L, date = today, type = ActivityType.WEEDED)
        vm.deleteActivity(a)
        advanceUntilIdle()
        verify(repo).deleteActivity(a)
    }

    // ── addHarvest ────────────────────────────────────────────────────────────

    @Test fun `addHarvest calls repo addHarvest with correct data`() = runTest {
        whenever(repo.addHarvest(any())).thenReturn(1L)
        vm.addHarvest(1L, 12.5, "90kg bags", 25.0, today)
        advanceUntilIdle()
        verify(repo).addHarvest(argThat {
            fieldId == 1L && yieldQuantity == 12.5 && unit == "90kg bags" && sellingPricePerUnit == 25.0
        })
    }

    @Test fun `addHarvest with null selling price`() = runTest {
        whenever(repo.addHarvest(any())).thenReturn(1L)
        vm.addHarvest(1L, 5.0, "kg", null, today)
        advanceUntilIdle()
        verify(repo).addHarvest(argThat { sellingPricePerUnit == null })
    }

    // ── archiveField ──────────────────────────────────────────────────────────

    @Test fun `archiveField calls repo archiveField`() = runTest {
        vm.archiveField(3L)
        advanceUntilIdle()
        verify(repo).archiveField(3L)
    }

    // ── delegated flows ───────────────────────────────────────────────────────

    @Test fun `getFieldById delegates to repo`() = runTest {
        whenever(repo.getFieldById(5L)).thenReturn(flowOf(field(5L)))
        vm.getFieldById(5L).test {
            assertEquals(5L, awaitItem()!!.id)
            awaitComplete()
        }
    }

    @Test fun `getActivities delegates to repo`() = runTest {
        val activities = listOf(ActivityEntity(fieldId = 1L, date = today, type = ActivityType.IRRIGATED))
        whenever(repo.getActivitiesForField(1L)).thenReturn(flowOf(activities))
        vm.getActivities(1L).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test fun `getHarvests delegates to repo`() = runTest {
        val harvests = listOf(HarvestEntity(fieldId = 1L, date = today, yieldQuantity = 8.0, unit = "kg"))
        whenever(repo.getHarvestsForField(1L)).thenReturn(flowOf(harvests))
        vm.getHarvests(1L).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test fun `getTotalCost delegates to repo`() = runTest {
        whenever(repo.getTotalCostForField(1L)).thenReturn(flowOf(200.0))
        vm.getTotalCost(1L).test {
            assertEquals(200.0, awaitItem(), 0.001)
            awaitComplete()
        }
    }
}
