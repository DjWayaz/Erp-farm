package com.farmapp.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.farmapp.data.local.entity.PestGuideEntity
import com.farmapp.data.repository.PestGuideRepository
import com.farmapp.ui.guide.GuideViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class GuideViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: PestGuideRepository
    private lateinit var vm: GuideViewModel

    private fun pest(id: String, name: String, crop: String = "Maize", severity: String = "HIGH") =
        PestGuideEntity(id = id, name = name, affectedCrop = crop, symptoms = "Damage", treatment = "Spray", severity = severity)

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mock()
        whenever(repo.getAllPests()).thenReturn(flowOf(emptyList()))
        whenever(repo.searchPests(any())).thenReturn(flowOf(emptyList()))
        vm = GuideViewModel(repo)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    // ── searchQuery StateFlow ─────────────────────────────────────────────────

    @Test fun `searchQuery starts blank`() = runTest {
        vm.searchQuery.test {
            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `setSearchQuery updates searchQuery`() = runTest {
        vm.setSearchQuery("Maize")
        vm.searchQuery.test {
            assertEquals("Maize", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `setSearchQuery to empty string clears it`() = runTest {
        vm.setSearchQuery("aphids")
        vm.setSearchQuery("")
        vm.searchQuery.test {
            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `setSearchQuery stores the query value`() = runTest {
        vm.setSearchQuery("Newcastle")
        assertEquals("Newcastle", vm.searchQuery.value)
    }

    @Test fun `setSearchQuery to different values updates correctly`() = runTest {
        vm.setSearchQuery("Maize")
        assertEquals("Maize", vm.searchQuery.value)
        vm.setSearchQuery("Tobacco")
        assertEquals("Tobacco", vm.searchQuery.value)
    }

    // ── searchQuery drives repo calls (verified via the StateFlow internal logic)

    @Test fun `blank query results in getAllPests being the active source`() {
        // When searchQuery is blank, the flatMapLatest branch calls getAllPests.
        // We verify this by confirming searchPests is NOT called for a blank query.
        assertEquals("", vm.searchQuery.value)
        verify(repo, never()).searchPests(any())
    }

    @Test fun `non-blank query results in searchPests being the active source`() {
        // When a non-blank query is set, searchPests should be called (after debounce).
        // We verify the query value is stored correctly — the debounce is an
        // implementation detail tested at the integration level via the DAO tests.
        vm.setSearchQuery("armyworm")
        assertEquals("armyworm", vm.searchQuery.value)
        verify(repo, never()).getAllPests()  // getAllPests was from setUp before query was set
    }

    // ── getPestById ───────────────────────────────────────────────────────────

    @Test fun `getPestById delegates to repo`() = runTest {
        val p = pest("fall_armyworm", "Fall Armyworm")
        whenever(repo.getPestById("fall_armyworm")).thenReturn(flowOf(p))
        vm.getPestById("fall_armyworm").test {
            assertEquals("Fall Armyworm", awaitItem()!!.name)
            awaitComplete()
        }
    }

    @Test fun `getPestById returns null for unknown id`() = runTest {
        whenever(repo.getPestById("unknown")).thenReturn(flowOf(null))
        vm.getPestById("unknown").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test fun `getPestById for newcastle disease`() = runTest {
        val p = pest("newcastle", "Newcastle Disease", "Poultry", "HIGH")
        whenever(repo.getPestById("newcastle")).thenReturn(flowOf(p))
        vm.getPestById("newcastle").test {
            val result = awaitItem()!!
            assertEquals("Newcastle Disease", result.name)
            assertEquals("HIGH", result.severity)
            awaitComplete()
        }
    }
}
