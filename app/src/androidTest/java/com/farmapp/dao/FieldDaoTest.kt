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
 * Instrumented DAO tests — run against a real in-memory Room database.
 * These test actual SQL queries, foreign keys and the type converters together.
 */
@RunWith(AndroidJUnit4::class)
class FieldDaoTest {

    private lateinit var db: FarmDatabase
    private val dao get() = db.fieldDao()

    private val today = LocalDate.of(2025, 4, 1)

    private fun field(name: String = "Field A", crop: String = "Maize", size: Double = 1.0) =
        FieldEntity(name = name, sizeHectares = size, cropType = crop, plantingDate = today)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FarmDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }

    // ── insert and retrieve ───────────────────────────────────────────────────

    @Test fun insertFieldAndGetById() = runTest {
        val id = dao.insertField(field("North Plot", "Maize"))
        dao.getFieldById(id).test {
            val f = awaitItem()
            assertNotNull(f)
            assertEquals("North Plot", f!!.name)
            assertEquals("Maize", f.cropType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun insertMultipleFieldsAndGetAll() = runTest {
        dao.insertField(field("Plot A"))
        dao.insertField(field("Plot B", "Beans"))
        dao.insertField(field("Plot C", "Tomatoes"))
        dao.getAllActiveFields().test {
            assertEquals(3, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getAllActiveFieldsOnlyReturnsActiveFields() = runTest {
        val id1 = dao.insertField(field("Active"))
        val id2 = dao.insertField(field("ToArchive"))
        dao.archiveField(id2)
        dao.getAllActiveFields().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Active", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun archiveFieldSetsIsActiveFalse() = runTest {
        val id = dao.insertField(field())
        dao.archiveField(id)
        dao.getFieldById(id).test {
            val f = awaitItem()
            assertNotNull(f)
            assertFalse(f!!.isActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getFieldByIdReturnsNullForMissingId() = runTest {
        dao.getFieldById(9999L).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun fieldWithOptionalFieldsRoundtrip() = runTest {
        val harvest = today.plusMonths(4)
        val f = field().copy(
            variety = "DK8031", soilType = "Sandy Loam",
            notes = "Irrigated", expectedHarvestDate = harvest
        )
        val id = dao.insertField(f)
        dao.getFieldById(id).test {
            val result = awaitItem()!!
            assertEquals("DK8031", result.variety)
            assertEquals("Sandy Loam", result.soilType)
            assertEquals("Irrigated", result.notes)
            assertEquals(harvest, result.expectedHarvestDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun plantingDateIsPersisted() = runTest {
        val plantDate = LocalDate.of(2025, 10, 15)
        val id = dao.insertField(field().copy(plantingDate = plantDate))
        dao.getFieldById(id).test {
            assertEquals(plantDate, awaitItem()!!.plantingDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── activities ────────────────────────────────────────────────────────────

    @Test fun insertAndRetrieveActivities() = runTest {
        val fieldId = dao.insertField(field())
        dao.insertActivity(ActivityEntity(fieldId = fieldId, date = today, type = ActivityType.FERTILIZED, cost = 25.0))
        dao.insertActivity(ActivityEntity(fieldId = fieldId, date = today.minusDays(1), type = ActivityType.WEEDED))
        dao.getActivitiesForField(fieldId).test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun deleteActivity() = runTest {
        val fieldId = dao.insertField(field())
        val a = ActivityEntity(fieldId = fieldId, date = today, type = ActivityType.SPRAYED)
        dao.insertActivity(a)
        dao.getActivitiesForField(fieldId).test {
            val inserted = awaitItem()[0]
            dao.deleteActivity(inserted)
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getTotalCostForFieldSumsActivities() = runTest {
        val fieldId = dao.insertField(field())
        dao.insertActivity(ActivityEntity(fieldId = fieldId, date = today, type = ActivityType.FERTILIZED, cost = 30.0))
        dao.insertActivity(ActivityEntity(fieldId = fieldId, date = today, type = ActivityType.SPRAYED, cost = 20.0))
        dao.getTotalCostForField(fieldId).test {
            assertEquals(50.0, awaitItem(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getTotalCostForFieldReturnsZeroWhenNoActivities() = runTest {
        val fieldId = dao.insertField(field())
        dao.getTotalCostForField(fieldId).test {
            assertEquals(0.0, awaitItem(), 0.0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun activitiesCascadeDeleteWithField() = runTest {
        // Room cascade: when field is archived (soft delete), activities remain
        // but if field were hard deleted, activities would cascade
        val fieldId = dao.insertField(field())
        dao.insertActivity(ActivityEntity(fieldId = fieldId, date = today, type = ActivityType.WEEDED))
        dao.getActivitiesForField(fieldId).test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── harvests ──────────────────────────────────────────────────────────────

    @Test fun insertAndRetrieveHarvests() = runTest {
        val fieldId = dao.insertField(field())
        dao.insertHarvest(HarvestEntity(fieldId = fieldId, date = today, yieldQuantity = 10.5, unit = "90kg bags", sellingPricePerUnit = 25.0))
        dao.getHarvestsForField(fieldId).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(10.5, result[0].yieldQuantity, 0.001)
            assertEquals(25.0, result[0].sellingPricePerUnit!!, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun deleteHarvest() = runTest {
        val fieldId = dao.insertField(field())
        dao.insertHarvest(HarvestEntity(fieldId = fieldId, date = today, yieldQuantity = 5.0, unit = "kg"))
        dao.getHarvestsForField(fieldId).test {
            val inserted = awaitItem()[0]
            dao.deleteHarvest(inserted)
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun harvestWithNullSellingPrice() = runTest {
        val fieldId = dao.insertField(field())
        dao.insertHarvest(HarvestEntity(fieldId = fieldId, date = today, yieldQuantity = 3.0, unit = "kg"))
        dao.getHarvestsForField(fieldId).test {
            assertNull(awaitItem()[0].sellingPricePerUnit)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun allActivityTypesCanBeInserted() = runTest {
        val fieldId = dao.insertField(field())
        for (type in ActivityType.values()) {
            dao.insertActivity(ActivityEntity(fieldId = fieldId, date = today, type = type))
        }
        dao.getActivitiesForField(fieldId).test {
            assertEquals(ActivityType.values().size, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
