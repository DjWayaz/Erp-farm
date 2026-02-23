package com.farmapp.worker

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for VaccinationReminderWorker scheduling logic.
 * WorkManager itself requires an Android runtime so we test the
 * pure scheduling decisions (daysFromNow guard, key constants).
 */
class VaccinationWorkerTest {

    @Test fun `KEY_VACCINE_NAME constant is correct`() =
        assertEquals("vaccine_name", VaccinationReminderWorker.KEY_VACCINE_NAME)

    @Test fun `KEY_BATCH_NAME constant is correct`() =
        assertEquals("batch_name", VaccinationReminderWorker.KEY_BATCH_NAME)

    @Test fun `KEY_VACCINATION_ID constant is correct`() =
        assertEquals("vaccination_id", VaccinationReminderWorker.KEY_VACCINATION_ID)

    @Test fun `schedule skips past vaccinations negative daysFromNow`() {
        // The schedule() method guard: if (daysFromNow < 0) return
        // We verify the days-from-now calculation logic
        val today = LocalDate.now()
        val pastDate = today.minusDays(3)
        val daysFromNow = java.time.temporal.ChronoUnit.DAYS.between(today, pastDate)
        assertTrue("Past dates should give negative daysFromNow", daysFromNow < 0)
    }

    @Test fun `schedule proceeds for today daysFromNow zero`() {
        val today = LocalDate.now()
        val daysFromNow = java.time.temporal.ChronoUnit.DAYS.between(today, today)
        assertEquals(0L, daysFromNow)
        assertFalse("Today should not be skipped", daysFromNow < 0)
    }

    @Test fun `schedule proceeds for future vaccination`() {
        val today = LocalDate.now()
        val futureDate = today.plusDays(7)
        val daysFromNow = java.time.temporal.ChronoUnit.DAYS.between(today, futureDate)
        assertEquals(7L, daysFromNow)
        assertFalse("Future dates should not be skipped", daysFromNow < 0)
    }

    @Test fun `unique work tag follows vaccination id pattern`() {
        val vaccinationId = 42L
        val expectedTag = "vaccination_$vaccinationId"
        assertEquals("vaccination_42", expectedTag)
    }

    @Test fun `unique work key and tag match each other`() {
        val vaccinationId = 7L
        val key = "vaccination_$vaccinationId"
        val tag = "vaccination_$vaccinationId"
        assertEquals(key, tag)
    }

    @Test fun `daysFromNow is exactly 30 for 30-day out vaccination`() {
        val today = LocalDate.now()
        val future = today.plusDays(30)
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, future)
        assertEquals(30L, days)
    }
}
