package com.farmapp.repository

import app.cash.turbine.test
import com.farmapp.data.local.dao.PestGuideDao
import com.farmapp.data.local.entity.PestGuideEntity
import com.farmapp.data.repository.PestGuideRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class PestGuideRepositoryTest {

    private lateinit var dao: PestGuideDao
    private lateinit var repo: PestGuideRepository

    private fun pest(
        id: String = "fall_armyworm",
        name: String = "Fall Armyworm",
        crop: String = "Maize",
        severity: String = "HIGH"
    ) = PestGuideEntity(
        id = id, name = name, localName = "Chipfukuto",
        affectedCrop = crop, symptoms = "Leaf damage", treatment = "Spray", severity = severity
    )

    @Before fun setUp() {
        dao = mock()
        repo = PestGuideRepository(dao)
    }

    // ── getAllPests ───────────────────────────────────────────────────────────

    @Test fun `getAllPests emits full list from dao`() = runTest {
        val pests = listOf(pest("p1", "Armyworm"), pest("p2", "Aphids", severity = "MEDIUM"))
        whenever(dao.getAllPests()).thenReturn(flowOf(pests))
        repo.getAllPests().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Armyworm", result[0].name)
            awaitComplete()
        }
    }

    @Test fun `getAllPests returns empty when guide not seeded`() = runTest {
        whenever(dao.getAllPests()).thenReturn(flowOf(emptyList()))
        repo.getAllPests().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // ── searchPests ───────────────────────────────────────────────────────────

    @Test fun `searchPests delegates query to dao`() = runTest {
        whenever(dao.searchPests("Maize")).thenReturn(flowOf(listOf(pest(crop = "Maize"))))
        repo.searchPests("Maize").test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
        verify(dao).searchPests("Maize")
    }

    @Test fun `searchPests with empty string returns all`() = runTest {
        val all = listOf(pest("p1"), pest("p2"))
        whenever(dao.searchPests("")).thenReturn(flowOf(all))
        repo.searchPests("").test {
            assertEquals(2, awaitItem().size)
            awaitComplete()
        }
    }

    @Test fun `searchPests with no matches returns empty`() = runTest {
        whenever(dao.searchPests("xyz")).thenReturn(flowOf(emptyList()))
        repo.searchPests("xyz").test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    @Test fun `searchPests by local Shona name`() = runTest {
        val matched = listOf(pest().copy(localName = "Chipfukuto"))
        whenever(dao.searchPests("Chipfukuto")).thenReturn(flowOf(matched))
        repo.searchPests("Chipfukuto").test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    // ── getPestsByCrop ────────────────────────────────────────────────────────

    @Test fun `getPestsByCrop filters by crop correctly`() = runTest {
        val maizePests = listOf(pest(crop = "Maize"), pest("grey_leaf_spot", "Grey Leaf Spot", "Maize"))
        whenever(dao.getPestsByCrop("Maize")).thenReturn(flowOf(maizePests))
        repo.getPestsByCrop("Maize").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.affectedCrop == "Maize" })
            awaitComplete()
        }
    }

    @Test fun `getPestsByCrop returns empty for unknown crop`() = runTest {
        whenever(dao.getPestsByCrop("Quinoa")).thenReturn(flowOf(emptyList()))
        repo.getPestsByCrop("Quinoa").test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // ── getPestById ───────────────────────────────────────────────────────────

    @Test fun `getPestById returns pest when found`() = runTest {
        val p = pest("fall_armyworm")
        whenever(dao.getPestById("fall_armyworm")).thenReturn(flowOf(p))
        repo.getPestById("fall_armyworm").test {
            assertEquals("fall_armyworm", awaitItem()!!.id)
            awaitComplete()
        }
    }

    @Test fun `getPestById returns null when not found`() = runTest {
        whenever(dao.getPestById("unknown")).thenReturn(flowOf(null))
        repo.getPestById("unknown").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test fun `getPestById for Newcastle disease`() = runTest {
        val p = pest("newcastle", "Newcastle Disease", "Poultry", "HIGH")
        whenever(dao.getPestById("newcastle")).thenReturn(flowOf(p))
        repo.getPestById("newcastle").test {
            val result = awaitItem()
            assertEquals("Newcastle Disease", result!!.name)
            assertEquals("HIGH", result.severity)
            awaitComplete()
        }
    }
}
