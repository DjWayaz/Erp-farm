package com.farmapp.entity

import com.farmapp.data.local.entity.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Tests entity data classes — default values, computed properties, and
 * enum display names used in the UI.
 */
class EntitiesTest {

    private val today = LocalDate.of(2025, 5, 1)

    // ── FieldEntity ───────────────────────────────────────────────────────────

    @Test fun `FieldEntity isActive defaults to true`() {
        val f = FieldEntity(name = "Plot A", sizeHectares = 1.0, cropType = "Maize", plantingDate = today)
        assertTrue(f.isActive)
    }

    @Test fun `FieldEntity optional fields default to null`() {
        val f = FieldEntity(name = "Plot A", sizeHectares = 1.0, cropType = "Maize", plantingDate = today)
        assertNull(f.soilType)
        assertNull(f.variety)
        assertNull(f.expectedHarvestDate)
        assertNull(f.notes)
    }

    @Test fun `FieldEntity id defaults to 0 for autoGenerate`() {
        val f = FieldEntity(name = "N", sizeHectares = 0.5, cropType = "Beans", plantingDate = today)
        assertEquals(0L, f.id)
    }

    @Test fun `FieldEntity copy produces independent instance`() {
        val original = FieldEntity(name = "A", sizeHectares = 1.0, cropType = "Maize", plantingDate = today)
        val copy = original.copy(name = "B", isActive = false)
        assertEquals("A", original.name)
        assertEquals("B", copy.name)
        assertFalse(copy.isActive)
    }

    // ── ActivityEntity ────────────────────────────────────────────────────────

    @Test fun `ActivityEntity cost defaults to 0`() {
        val a = ActivityEntity(fieldId = 1L, date = today, type = ActivityType.WEEDED)
        assertEquals(0.0, a.cost, 0.0)
    }

    @Test fun `ActivityEntity notes defaults to null`() {
        val a = ActivityEntity(fieldId = 1L, date = today, type = ActivityType.IRRIGATED)
        assertNull(a.notes)
    }

    // ── ActivityType enum ─────────────────────────────────────────────────────

    @Test fun `ActivityType has 7 values`() = assertEquals(7, ActivityType.values().size)

    @Test fun `ActivityType displayNames are human readable`() {
        assertEquals("Fertilized", ActivityType.FERTILIZED.displayName)
        assertEquals("Weeded", ActivityType.WEEDED.displayName)
        assertEquals("Irrigated", ActivityType.IRRIGATED.displayName)
        assertEquals("Other", ActivityType.OTHER.displayName)
    }

    // ── HarvestEntity ─────────────────────────────────────────────────────────

    @Test fun `HarvestEntity sellingPricePerUnit is nullable`() {
        val h = HarvestEntity(fieldId = 1L, date = today, yieldQuantity = 10.0, unit = "90kg bags")
        assertNull(h.sellingPricePerUnit)
    }

    @Test fun `HarvestEntity with selling price stores correctly`() {
        val h = HarvestEntity(fieldId = 1L, date = today, yieldQuantity = 5.0, unit = "kg", sellingPricePerUnit = 0.50)
        assertEquals(0.50, h.sellingPricePerUnit!!, 0.001)
    }

    // ── PoultryBatchEntity ────────────────────────────────────────────────────

    @Test fun `PoultryBatchEntity costPerBird defaults to 0`() {
        val b = PoultryBatchEntity(
            name = "Batch A", type = PoultryType.BROILER,
            dateAcquired = today, initialCount = 100, aliveCount = 100
        )
        assertEquals(0.0, b.costPerBird, 0.0)
    }

    @Test fun `PoultryBatchEntity isActive defaults to true`() {
        val b = PoultryBatchEntity(
            name = "Batch A", type = PoultryType.LAYER,
            dateAcquired = today, initialCount = 200, aliveCount = 200
        )
        assertTrue(b.isActive)
    }

    // ── PoultryType enum ──────────────────────────────────────────────────────

    @Test fun `PoultryType has 3 values`() = assertEquals(3, PoultryType.values().size)

    @Test fun `PoultryType displayNames are correct`() {
        assertEquals("Broiler", PoultryType.BROILER.displayName)
        assertEquals("Layer", PoultryType.LAYER.displayName)
        assertEquals("Kienyeji (Indigenous)", PoultryType.KIENYEJI.displayName)
    }

    // ── VaccinationEntity ─────────────────────────────────────────────────────

    @Test fun `VaccinationEntity administeredDate is null by default`() {
        val v = VaccinationEntity(batchId = 1L, vaccineName = "Newcastle", dueDate = today)
        assertNull(v.administeredDate)
    }

    @Test fun `VaccinationEntity copy with administeredDate marks as done`() {
        val v = VaccinationEntity(batchId = 1L, vaccineName = "Gumboro", dueDate = today)
        val done = v.copy(administeredDate = today)
        assertNotNull(done.administeredDate)
        assertEquals(today, done.administeredDate)
    }

    // ── FeedEventEntity ───────────────────────────────────────────────────────

    @Test fun `FeedEventEntity stores bagsUsed and costPerBag`() {
        val f = FeedEventEntity(batchId = 1L, date = today, bagsUsed = 3.5, costPerBag = 18.0)
        assertEquals(3.5, f.bagsUsed, 0.0)
        assertEquals(18.0, f.costPerBag, 0.0)
    }

    @Test fun `FeedEventEntity total cost calculation`() {
        val f = FeedEventEntity(batchId = 1L, date = today, bagsUsed = 4.0, costPerBag = 15.50)
        val total = f.bagsUsed * f.costPerBag
        assertEquals(62.0, total, 0.001)
    }

    // ── InventoryItemEntity ───────────────────────────────────────────────────

    @Test fun `InventoryItemEntity lowStockThresholdDays defaults to 7`() {
        val i = InventoryItemEntity(
            name = "Layer Mash", unit = "bags",
            currentQuantity = 10.0, consumptionRatePerDay = 1.0
        )
        assertEquals(7, i.lowStockThresholdDays)
    }

    @Test fun `InventoryItemEntity daysRemaining calculation`() {
        val i = InventoryItemEntity(
            name = "Chick Starter", unit = "bags",
            currentQuantity = 14.0, consumptionRatePerDay = 2.0
        )
        val daysRemaining = i.currentQuantity / i.consumptionRatePerDay
        assertEquals(7.0, daysRemaining, 0.001)
    }

    @Test fun `InventoryItemEntity is low stock when days remaining below threshold`() {
        val i = InventoryItemEntity(
            name = "Feed", unit = "bags",
            currentQuantity = 5.0, consumptionRatePerDay = 1.0,
            lowStockThresholdDays = 7
        )
        val daysRemaining = i.currentQuantity / i.consumptionRatePerDay
        assertTrue("Should be low stock", daysRemaining <= i.lowStockThresholdDays)
    }

    @Test fun `InventoryItemEntity is not low stock when days remaining above threshold`() {
        val i = InventoryItemEntity(
            name = "Feed", unit = "bags",
            currentQuantity = 30.0, consumptionRatePerDay = 1.0,
            lowStockThresholdDays = 7
        )
        val daysRemaining = i.currentQuantity / i.consumptionRatePerDay
        assertFalse("Should not be low stock", daysRemaining <= i.lowStockThresholdDays)
    }

    // ── TransactionEntity ─────────────────────────────────────────────────────

    @Test fun `TransactionEntity relatedEntityId is nullable`() {
        val t = TransactionEntity(
            date = today, type = TransactionType.EXPENSE,
            category = TransactionCategory.FEED,
            amount = 45.0, description = "Chick mash purchase"
        )
        assertNull(t.relatedEntityId)
        assertNull(t.relatedEntityType)
    }

    // ── TransactionType and Category enums ────────────────────────────────────

    @Test fun `TransactionType has INCOME and EXPENSE`() {
        assertEquals(2, TransactionType.values().size)
        assertEquals("Income", TransactionType.INCOME.displayName)
        assertEquals("Expense", TransactionType.EXPENSE.displayName)
    }

    @Test fun `TransactionCategory has 9 values`() =
        assertEquals(9, TransactionCategory.values().size)

    @Test fun `TransactionCategory displayNames are readable`() {
        assertEquals("Seeds", TransactionCategory.SEEDS.displayName)
        assertEquals("Veterinary", TransactionCategory.VET.displayName)
        assertEquals("Fertilizer", TransactionCategory.FERTILIZER.displayName)
    }

    // ── PestGuideEntity ───────────────────────────────────────────────────────

    @Test fun `PestGuideEntity severity defaults to MEDIUM`() {
        val p = PestGuideEntity(
            id = "fall_armyworm", name = "Fall Armyworm",
            affectedCrop = "Maize", symptoms = "Leaf damage",
            treatment = "Spray Cypermethrin"
        )
        assertEquals("MEDIUM", p.severity)
    }

    @Test fun `PestGuideEntity localName and prevention are nullable`() {
        val p = PestGuideEntity(
            id = "test", name = "Test", affectedCrop = "Maize",
            symptoms = "S", treatment = "T"
        )
        assertNull(p.localName)
        assertNull(p.prevention)
    }

    // ── DashboardUiState ──────────────────────────────────────────────────────

    @Test fun `DashboardUiState monthlyProfit is income minus expenses`() {
        val state = com.farmapp.ui.dashboard.DashboardUiState(
            monthlyIncome = 500.0,
            monthlyExpenses = 300.0
        )
        assertEquals(200.0, state.monthlyProfit, 0.001)
    }

    @Test fun `DashboardUiState monthlyProfit is negative when loss`() {
        val state = com.farmapp.ui.dashboard.DashboardUiState(
            monthlyIncome = 100.0,
            monthlyExpenses = 250.0
        )
        assertEquals(-150.0, state.monthlyProfit, 0.001)
    }

    @Test fun `DashboardUiState defaults all to zero and empty`() {
        val state = com.farmapp.ui.dashboard.DashboardUiState()
        assertEquals(0, state.totalLiveBirds)
        assertEquals(0.0, state.monthlyIncome, 0.0)
        assertEquals(0.0, state.monthlyExpenses, 0.0)
        assertEquals(0.0, state.monthlyProfit, 0.0)
        assertTrue(state.activeFields.isEmpty())
        assertTrue(state.activeBatches.isEmpty())
        assertTrue(state.upcomingVaccinations.isEmpty())
        assertTrue(state.lowStockItems.isEmpty())
    }

    // ── FinanceUiState ────────────────────────────────────────────────────────

    @Test fun `FinanceUiState profit is income minus expenses`() {
        val state = com.farmapp.ui.finance.FinanceUiState(totalIncome = 800.0, totalExpenses = 500.0)
        assertEquals(300.0, state.profit, 0.001)
    }

    @Test fun `FinanceUiState profit is negative for a loss month`() {
        val state = com.farmapp.ui.finance.FinanceUiState(totalIncome = 50.0, totalExpenses = 200.0)
        assertEquals(-150.0, state.profit, 0.001)
    }

    @Test fun `FinanceUiState profit zero when balanced`() {
        val state = com.farmapp.ui.finance.FinanceUiState(totalIncome = 100.0, totalExpenses = 100.0)
        assertEquals(0.0, state.profit, 0.0)
    }
}
