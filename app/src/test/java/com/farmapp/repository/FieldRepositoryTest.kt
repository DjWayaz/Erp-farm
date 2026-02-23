package com.farmapp.repository

import app.cash.turbine.test
import com.farmapp.data.local.dao.FieldDao
import com.farmapp.data.local.entity.*
import com.farmapp.data.repository.FieldRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.time.LocalDate

class FieldRepositoryTest {

    private lateinit var dao: FieldDao
    private lateinit var repo: FieldRepository

    private val today = LocalDate.of(2025, 4, 10)

    private fun field(id: Long = 0L, name: String = "Field A", crop: String = "Maize") =
        FieldEntity(id = id, name = name, sizeHectares = 1.0, cropType = crop, plantingDate = today)

    private fun activity(fieldId: Long = 1L, type: ActivityType = ActivityType.WEEDED, cost: Double = 0.0) =
        ActivityEntity(fieldId = fieldId, date = today, type = type, cost = cost)

    private fun harvest(fieldId: Long = 1L, qty: Double = 10.0) =
        HarvestEntity(fieldId = fieldId, date = today, yieldQuantity = qty, unit = "90kg bags")

    @Before fun setUp() {
        dao = mock()
        repo = FieldRepository(dao)
    }

    // ── saveField ─────────────────────────────────────────────────────────────

    @Test fun `saveField delegates to dao insertField`() = runTest {
        whenever(dao.insertField(any())).thenReturn(1L)
        val id = repo.saveField(field())
        verify(dao).insertField(any())
        assertEquals(1L, id)
    }

    @Test fun `saveField returns dao-assigned id`() = runTest {
        whenever(dao.insertField(any())).thenReturn(42L)
        assertEquals(42L, repo.saveField(field()))
    }

    @Test fun `saveField passes entity unchanged to dao`() = runTest {
        val f = field(name = "South Plot").copy(variety = "SC403", soilType = "Red Sandy")
        whenever(dao.insertField(f)).thenReturn(1L)
        repo.saveField(f)
        verify(dao).insertField(f)
    }

    // ── getAllActiveFields ─────────────────────────────────────────────────────

    @Test fun `getAllActiveFields emits dao flow`() = runTest {
        val list = listOf(field(1), field(2, "South Plot"))
        whenever(dao.getAllActiveFields()).thenReturn(flowOf(list))
        repo.getAllActiveFields().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Field A", result[0].name)
            awaitComplete()
        }
    }

    @Test fun `getAllActiveFields returns empty list when none exist`() = runTest {
        whenever(dao.getAllActiveFields()).thenReturn(flowOf(emptyList()))
        repo.getAllActiveFields().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // ── getFieldById ──────────────────────────────────────────────────────────

    @Test fun `getFieldById returns field when found`() = runTest {
        whenever(dao.getFieldById(5L)).thenReturn(flowOf(field(5L)))
        repo.getFieldById(5L).test {
            assertEquals(5L, awaitItem()!!.id)
            awaitComplete()
        }
    }

    @Test fun `getFieldById returns null when not found`() = runTest {
        whenever(dao.getFieldById(999L)).thenReturn(flowOf(null))
        repo.getFieldById(999L).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    // ── updateField ───────────────────────────────────────────────────────────

    @Test fun `updateField delegates to dao`() = runTest {
        val f = field(1L)
        repo.updateField(f)
        verify(dao).updateField(f)
    }

    // ── archiveField ──────────────────────────────────────────────────────────

    @Test fun `archiveField delegates to dao with correct id`() = runTest {
        repo.archiveField(3L)
        verify(dao).archiveField(3L)
    }

    @Test fun `archiveField does not call insertField or updateField`() = runTest {
        repo.archiveField(1L)
        verify(dao, never()).insertField(any())
        verify(dao, never()).updateField(any())
    }

    // ── addActivity ───────────────────────────────────────────────────────────

    @Test fun `addActivity delegates to dao and returns id`() = runTest {
        val a = activity()
        whenever(dao.insertActivity(a)).thenReturn(7L)
        assertEquals(7L, repo.addActivity(a))
        verify(dao).insertActivity(a)
    }

    @Test fun `addActivity with cost stores cost value`() = runTest {
        val a = activity(cost = 35.50)
        whenever(dao.insertActivity(a)).thenReturn(1L)
        repo.addActivity(a)
        verify(dao).insertActivity(argThat { cost == 35.50 })
    }

    // ── deleteActivity ────────────────────────────────────────────────────────

    @Test fun `deleteActivity delegates to dao`() = runTest {
        val a = activity()
        repo.deleteActivity(a)
        verify(dao).deleteActivity(a)
    }

    // ── getActivitiesForField ─────────────────────────────────────────────────

    @Test fun `getActivitiesForField emits correct list`() = runTest {
        val activities = listOf(activity(type = ActivityType.FERTILIZED), activity(type = ActivityType.SPRAYED))
        whenever(dao.getActivitiesForField(1L)).thenReturn(flowOf(activities))
        repo.getActivitiesForField(1L).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals(ActivityType.FERTILIZED, result[0].type)
            awaitComplete()
        }
    }

    // ── getTotalCostForField ──────────────────────────────────────────────────

    @Test fun `getTotalCostForField emits sum from dao`() = runTest {
        whenever(dao.getTotalCostForField(1L)).thenReturn(flowOf(125.75))
        repo.getTotalCostForField(1L).test {
            assertEquals(125.75, awaitItem(), 0.001)
            awaitComplete()
        }
    }

    @Test fun `getTotalCostForField emits zero when no activities`() = runTest {
        whenever(dao.getTotalCostForField(2L)).thenReturn(flowOf(0.0))
        repo.getTotalCostForField(2L).test {
            assertEquals(0.0, awaitItem(), 0.0)
            awaitComplete()
        }
    }

    // ── addHarvest ────────────────────────────────────────────────────────────

    @Test fun `addHarvest delegates to dao and returns id`() = runTest {
        val h = harvest()
        whenever(dao.insertHarvest(h)).thenReturn(9L)
        assertEquals(9L, repo.addHarvest(h))
        verify(dao).insertHarvest(h)
    }

    @Test fun `addHarvest with selling price passes price to dao`() = runTest {
        val h = HarvestEntity(fieldId = 1L, date = today, yieldQuantity = 8.0, unit = "kg", sellingPricePerUnit = 0.30)
        whenever(dao.insertHarvest(any())).thenReturn(1L)
        repo.addHarvest(h)
        verify(dao).insertHarvest(argThat { sellingPricePerUnit == 0.30 })
    }

    // ── deleteHarvest ─────────────────────────────────────────────────────────

    @Test fun `deleteHarvest delegates to dao`() = runTest {
        val h = harvest()
        repo.deleteHarvest(h)
        verify(dao).deleteHarvest(h)
    }

    // ── getHarvestsForField ───────────────────────────────────────────────────

    @Test fun `getHarvestsForField emits list from dao`() = runTest {
        val harvests = listOf(harvest(qty = 5.0), harvest(qty = 12.0))
        whenever(dao.getHarvestsForField(1L)).thenReturn(flowOf(harvests))
        repo.getHarvestsForField(1L).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals(5.0, result[0].yieldQuantity, 0.0)
            awaitComplete()
        }
    }
}
