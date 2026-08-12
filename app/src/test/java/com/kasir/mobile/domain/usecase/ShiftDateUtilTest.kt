package com.kasir.mobile.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ShiftDateUtilTest {

    @Test
    fun `before 6am returns previous day as shift date`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.AUGUST, 12, 5, 59, 59)
        assertEquals("2026-08-11", ShiftDateUtil.getShiftDate(cal.timeInMillis))
    }

    @Test
    fun `exactly 6am returns same day as shift date`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.AUGUST, 12, 6, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        assertEquals("2026-08-12", ShiftDateUtil.getShiftDate(cal.timeInMillis))
    }

    @Test
    fun `afternoon returns same day as shift date`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.AUGUST, 12, 15, 30, 0)
        assertEquals("2026-08-12", ShiftDateUtil.getShiftDate(cal.timeInMillis))
    }

    @Test
    fun `midnight returns previous day as shift date`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.AUGUST, 13, 0, 0, 0)
        assertEquals("2026-08-12", ShiftDateUtil.getShiftDate(cal.timeInMillis))
    }
}
