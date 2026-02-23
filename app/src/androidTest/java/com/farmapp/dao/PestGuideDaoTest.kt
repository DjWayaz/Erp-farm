package com.farmapp.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.farmapp.data.local.FarmDatabase
import com.farmapp.data.local.entity.PestGuideEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PestGuideDaoTest {

    private lateinit var db: FarmDatabase
    private val dao get() = db.pestGuideDao()

    private val samplePests = listOf(
        PestGuideEntity("fall_armyworm", "Fall Armyworm", "Chipfukuto", "Maize", "Leaf damage", "Spray Cypermethrin", severity = "HIGH"),
        PestGuideEntity("aphids", "Aphids", "Tsvina", "Tobacco", "Yellow leaves", "Spray insecticide", severity = "MEDIUM"),
        PestGuideEntity("grey_leaf_spot", "Grey Leaf Spot", null, "Maize", "Grey lesions", "Fungicide", severity = "MEDIUM"),
        PestGuideEntity("newcastle", "Newcastle Disease", "Chirwere", "Poultry", "Nervous symptoms", "Vaccinate", severity = "HIGH"),
        PestGuideEntity("red_spider_mite", "Red Spider Mite", null, "Cotton", "Leaf curl", "Acaricide", severity = "LOW")
    )

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), FarmDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }

    // ── insertAll ─────────────────────────────────────────────────────────────

    @Test fun insertAllAndGetCount() = runTest {
        dao.insertAll(samplePests)
        assertEquals(samplePests.size, dao.getCount())
    }

    @Test fun insertAllIgnoresDuplicates() = runTest {
        dao.insertAll(samplePests)
        dao.insertAll(samplePests) // second insert should be ignored
        assertEquals(samplePests.size, dao.getCount())
    }

    @Test fun insertAllEmpty() = runTest {
        dao.insertAll(emptyList())
        assertEquals(0, dao.getCount())
    }

    // ── getAllPests ───────────────────────────────────────────────────────────

    @Test fun getAllPestsReturnsSortedByName() = runTest {
        dao.insertAll(samplePests)
        dao.getAllPests().test {
            val result = awaitItem()
            assertEquals(samplePests.size, result.size)
            // Verify alphabetical order
            val names = result.map { it.name }
            assertEquals(names.sorted(), names)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getAllPestsIsEmptyBeforeSeeding() = runTest {
        dao.getAllPests().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getPestById ───────────────────────────────────────────────────────────

    @Test fun getPestByIdReturnsCorrectPest() = runTest {
        dao.insertAll(samplePests)
        dao.getPestById("fall_armyworm").test {
            val p = awaitItem()!!
            assertEquals("Fall Armyworm", p.name)
            assertEquals("Chipfukuto", p.localName)
            assertEquals("Maize", p.affectedCrop)
            assertEquals("HIGH", p.severity)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getPestByIdReturnsNullForUnknownId() = runTest {
        dao.insertAll(samplePests)
        dao.getPestById("unknown_pest").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getPestByIdWithNullLocalName() = runTest {
        dao.insertAll(samplePests)
        dao.getPestById("grey_leaf_spot").test {
            assertNull(awaitItem()!!.localName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── searchPests ───────────────────────────────────────────────────────────

    @Test fun searchByPestName() = runTest {
        dao.insertAll(samplePests)
        dao.searchPests("Armyworm").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Fall Armyworm", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun searchByLocalShonaName() = runTest {
        dao.insertAll(samplePests)
        dao.searchPests("Chipfukuto").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("fall_armyworm", result[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun searchByAffectedCrop() = runTest {
        dao.insertAll(samplePests)
        dao.searchPests("Maize").test {
            val result = awaitItem()
            assertEquals(2, result.size) // Fall Armyworm + Grey Leaf Spot
            assertTrue(result.all { it.affectedCrop == "Maize" || it.name.contains("Maize") || it.localName?.contains("Maize") == true })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun searchIsCaseInsensitive() = runTest {
        dao.insertAll(samplePests)
        dao.searchPests("armyworm").test {
            assertEquals(1, awaitItem().size) // LIKE is case-insensitive in SQLite
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun searchWithNoMatchReturnsEmpty() = runTest {
        dao.insertAll(samplePests)
        dao.searchPests("Locust").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun searchWithBlankQueryMatchesAll() = runTest {
        dao.insertAll(samplePests)
        dao.searchPests("").test {
            // LIKE '%' || '' || '%' matches everything
            assertEquals(samplePests.size, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getPestsByCrop ────────────────────────────────────────────────────────

    @Test fun getPestsByCropFiltersCorrectly() = runTest {
        dao.insertAll(samplePests)
        dao.getPestsByCrop("Maize").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.affectedCrop == "Maize" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getPestsByCropPoultry() = runTest {
        dao.insertAll(samplePests)
        dao.getPestsByCrop("Poultry").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Newcastle Disease", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getPestsByCropUnknownCropReturnsEmpty() = runTest {
        dao.insertAll(samplePests)
        dao.getPestsByCrop("Quinoa").test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun getPestsByCropReturnsSortedByName() = runTest {
        dao.insertAll(samplePests)
        dao.getPestsByCrop("Maize").test {
            val names = awaitItem().map { it.name }
            assertEquals(names.sorted(), names)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
