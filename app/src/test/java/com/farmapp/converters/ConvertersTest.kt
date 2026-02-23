package com.farmapp.converters

import com.farmapp.data.local.converters.Converters
import com.farmapp.data.local.entity.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests every TypeConverter that Room uses to persist data.
 * These run on the JVM — no Android runtime needed.
 */
class ConvertersTest {

    private lateinit var c: Converters

    @Before fun setUp() { c = Converters() }

    // ── LocalDate ─────────────────────────────────────────────────────────────

    @Test fun `localDateToEpochDay then fromEpochDay roundtrip`() {
        val dates = listOf(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
            LocalDate.of(2025, 2, 28),
            LocalDate.of(2000, 6, 15),
            LocalDate.now()
        )
        for (d in dates) {
            val epoch = c.localDateToEpochDay(d)
            assertEquals("Roundtrip failed for $d", d, c.fromEpochDay(epoch))
        }
    }

    @Test fun `localDateToEpochDay null input returns null`() = assertNull(c.localDateToEpochDay(null))
    @Test fun `fromEpochDay null input returns null`() = assertNull(c.fromEpochDay(null))

    @Test fun `fromEpochDay epoch 0 is 1970-01-01`() =
        assertEquals(LocalDate.of(1970, 1, 1), c.fromEpochDay(0L))

    // ── ActivityType ─────────────────────────────────────────────────────────

    @Test fun `activityType name roundtrip for all values`() {
        for (t in ActivityType.values()) {
            assertEquals(t, c.fromActivityType(c.activityTypeToString(t)))
        }
    }

    @Test fun `activityTypeToString produces correct name for FERTILIZED`() =
        assertEquals("FERTILIZED", c.activityTypeToString(ActivityType.FERTILIZED))

    @Test fun `fromActivityType null returns null`() = assertNull(c.fromActivityType(null))
    @Test fun `activityTypeToString null returns null`() = assertNull(c.activityTypeToString(null))

    // ── PoultryType ───────────────────────────────────────────────────────────

    @Test fun `poultryType name roundtrip for all values`() {
        for (t in PoultryType.values()) {
            assertEquals(t, c.fromPoultryType(c.poultryTypeToString(t)))
        }
    }

    @Test fun `poultryTypeToString for BROILER is BROILER`() =
        assertEquals("BROILER", c.poultryTypeToString(PoultryType.BROILER))

    @Test fun `fromPoultryType LAYER string returns LAYER enum`() =
        assertEquals(PoultryType.LAYER, c.fromPoultryType("LAYER"))

    @Test fun `fromPoultryType null returns null`() = assertNull(c.fromPoultryType(null))

    // ── TransactionType ───────────────────────────────────────────────────────

    @Test fun `transactionType name roundtrip for INCOME and EXPENSE`() {
        for (t in TransactionType.values()) {
            assertEquals(t, c.fromTransactionType(c.transactionTypeToString(t)))
        }
    }

    @Test fun `transactionTypeToString null returns null`() = assertNull(c.transactionTypeToString(null))
    @Test fun `fromTransactionType null returns null`() = assertNull(c.fromTransactionType(null))

    // ── TransactionCategory ───────────────────────────────────────────────────

    @Test fun `transactionCategory name roundtrip for all values`() {
        for (cat in TransactionCategory.values()) {
            assertEquals(cat, c.fromTransactionCategory(c.transactionCategoryToString(cat)))
        }
    }

    @Test fun `transactionCategoryToString for FEED is FEED`() =
        assertEquals("FEED", c.transactionCategoryToString(TransactionCategory.FEED))

    @Test fun `fromTransactionCategory null returns null`() = assertNull(c.fromTransactionCategory(null))
}
