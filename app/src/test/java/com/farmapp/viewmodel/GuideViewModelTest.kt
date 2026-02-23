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

    // Use StandardTestDispatcher so debounce can be advanced manually
    private val testDispatcher = StandardTestDispatcher()
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

    @Test fun `setSearchQuery can be cleared`() = runTest {
        vm.setSearchQuery("aphids")
        vm.setSearchQuery("")
        vm.searchQuery.test {
            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── pests StateFlow with debounce ─────────────────────────────────────────

    @Test fun `pests calls getAllPests when query is blank`() = runTest {
        val allPests = listOf(pest("p1", "Armyworm"), pest("p2", "Aphids"))
        whenever(repo.getAllPests()).thenReturn(flowOf(allPests))
        val vm2 = GuideViewModel(repo)
        // No query set — should use getAllPests
        advanceUntilIdle()
        vm2.pests.test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `pests calls searchPests after debounce when query is set`() = runTest {
        val results = listOf(pest("fa", "Fall Armyworm"))
        whenever(repo.searchPests("armyworm")).thenReturn(flowOf(results))
        whenever(repo.getAllPests()).thenReturn(flowOf(emptyList()))
        val vm2 = GuideViewModel(repo)
        vm2.setSearchQuery("armyworm")
        advanceTimeBy(400) // past 300ms debounce
        advanceUntilIdle()
        verify(repo, atLeastOnce()).searchPests("armyworm")
    }

    @Test fun `pests does not search before debounce window`() = runTest {
        whenever(repo.getAllPests()).thenReturn(flowOf(emptyList()))
        val vm2 = GuideViewModel(repo)
        vm2.setSearchQuery("ma")
        advanceTimeBy(100) // inside debounce window
        verify(repo, never()).searchPests("ma")
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
}
