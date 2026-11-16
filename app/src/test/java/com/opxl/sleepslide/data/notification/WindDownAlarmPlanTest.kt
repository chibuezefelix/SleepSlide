package com.opxl.sleepslide.data.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WindDownAlarmPlanTest {

    private val trigger = 1_794_861_000_000L

    @Test fun `exact allowed arms an exact alarm and a backup after the delay`() {
        val plan = planAlarms(exactAllowed = true, triggerAtMs = trigger)
        assertTrue(plan.exact)
        assertEquals(trigger + BACKUP_DELAY_MS, plan.backupAtMs)
    }

    @Test fun `without the permission the alarm is inexact and there is no backup`() {
        val plan = planAlarms(exactAllowed = false, triggerAtMs = trigger)
        assertFalse(plan.exact)
        assertNull(plan.backupAtMs)
    }

    @Test fun `backup is ten minutes late`() {
        assertEquals(10 * 60 * 1000L, BACKUP_DELAY_MS)
    }
}
