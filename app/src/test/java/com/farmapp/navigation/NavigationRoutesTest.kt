package com.farmapp.navigation

import com.farmapp.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for all navigation route definitions.
 * Verifies static routes are correct strings and parameterised
 * routes produce the expected URL-style paths.
 */
class NavigationRoutesTest {

    @Test fun `Dashboard route is dashboard`() = assertEquals("dashboard", Screen.Dashboard.route)
    @Test fun `CropManager route is crop_manager`() = assertEquals("crop_manager", Screen.CropManager.route)
    @Test fun `AddField route is add_field`() = assertEquals("add_field", Screen.AddField.route)
    @Test fun `PoultryManager route is poultry_manager`() = assertEquals("poultry_manager", Screen.PoultryManager.route)
    @Test fun `AddBatch route is add_batch`() = assertEquals("add_batch", Screen.AddBatch.route)
    @Test fun `Inventory route is inventory`() = assertEquals("inventory", Screen.Inventory.route)
    @Test fun `Finance route is finance`() = assertEquals("finance", Screen.Finance.route)
    @Test fun `PestGuide route is pest_guide`() = assertEquals("pest_guide", Screen.PestGuide.route)
    @Test fun `Settings route is settings`() = assertEquals("settings", Screen.Settings.route)

    @Test fun `FieldDetail route template has fieldId param`() =
        assertTrue(Screen.FieldDetail.route.contains("{fieldId}"))

    @Test fun `FieldDetail createRoute id 1`() = assertEquals("field_detail/1", Screen.FieldDetail.createRoute(1L))
    @Test fun `FieldDetail createRoute id 42`() = assertEquals("field_detail/42", Screen.FieldDetail.createRoute(42L))
    @Test fun `FieldDetail createRoute large id`() = assertEquals("field_detail/99999", Screen.FieldDetail.createRoute(99999L))

    @Test fun `BatchDetail route template has batchId param`() =
        assertTrue(Screen.BatchDetail.route.contains("{batchId}"))

    @Test fun `BatchDetail createRoute id 5`() = assertEquals("batch_detail/5", Screen.BatchDetail.createRoute(5L))
    @Test fun `BatchDetail createRoute id 100`() = assertEquals("batch_detail/100", Screen.BatchDetail.createRoute(100L))

    @Test fun `GuideDetail route template has pestId param`() =
        assertTrue(Screen.GuideDetail.route.contains("{pestId}"))

    @Test fun `GuideDetail createRoute fall_armyworm`() =
        assertEquals("guide_detail/fall_armyworm", Screen.GuideDetail.createRoute("fall_armyworm"))

    @Test fun `GuideDetail createRoute newcastle_disease`() =
        assertEquals("guide_detail/newcastle_disease", Screen.GuideDetail.createRoute("newcastle_disease"))

    @Test fun `all 12 routes are unique`() {
        val routes = listOf(
            Screen.Dashboard.route, Screen.CropManager.route, Screen.AddField.route,
            Screen.FieldDetail.route, Screen.PoultryManager.route, Screen.AddBatch.route,
            Screen.BatchDetail.route, Screen.Inventory.route, Screen.Finance.route,
            Screen.PestGuide.route, Screen.GuideDetail.route, Screen.Settings.route
        )
        assertEquals("All routes must be unique", routes.size, routes.toSet().size)
    }

    @Test fun `no route is blank`() {
        listOf(
            Screen.Dashboard.route, Screen.CropManager.route, Screen.AddField.route,
            Screen.FieldDetail.route, Screen.PoultryManager.route, Screen.AddBatch.route,
            Screen.BatchDetail.route, Screen.Inventory.route, Screen.Finance.route,
            Screen.PestGuide.route, Screen.GuideDetail.route, Screen.Settings.route
        ).forEach { assertFalse("Route blank: $it", it.isBlank()) }
    }

    @Test fun `FieldDetail and BatchDetail do not collide for same id`() =
        assertNotEquals(Screen.FieldDetail.createRoute(1L), Screen.BatchDetail.createRoute(1L))

    @Test fun `different field ids produce different routes`() =
        assertNotEquals(Screen.FieldDetail.createRoute(1L), Screen.FieldDetail.createRoute(2L))

    @Test fun `different pest ids produce different routes`() =
        assertNotEquals(Screen.GuideDetail.createRoute("a"), Screen.GuideDetail.createRoute("b"))

    @Test fun `parameterised routes have correct base prefix`() {
        assertTrue(Screen.FieldDetail.createRoute(1L).startsWith("field_detail/"))
        assertTrue(Screen.BatchDetail.createRoute(1L).startsWith("batch_detail/"))
        assertTrue(Screen.GuideDetail.createRoute("x").startsWith("guide_detail/"))
    }
}
