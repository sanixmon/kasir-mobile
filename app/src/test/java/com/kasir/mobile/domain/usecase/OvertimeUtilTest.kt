package com.kasir.mobile.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class OvertimeUtilTest {

    // checkIn at t=0, checkOut at (normalHours + extra) in millis
    private fun ms(hours: Int, minutes: Int = 0): Long = (hours * 60L + minutes) * 60_000L

    @Test
    fun `no overtime when working exactly normal hours`() {
        assertEquals(0.0, OvertimeUtil.calcOT(0L, ms(8), 8), 0.0)
    }

    @Test
    fun `no overtime when less than 11 minutes over`() {
        assertEquals(0.0, OvertimeUtil.calcOT(0L, ms(8, 10), 8), 0.0)
    }

    @Test
    fun `half hour OT when 11 to 40 minutes over`() {
        assertEquals(0.5, OvertimeUtil.calcOT(0L, ms(8, 11), 8), 0.0)
        assertEquals(0.5, OvertimeUtil.calcOT(0L, ms(8, 30), 8), 0.0)
        assertEquals(0.5, OvertimeUtil.calcOT(0L, ms(8, 40), 8), 0.0)
    }

    @Test
    fun `one hour OT when 41 to 60 minutes over`() {
        assertEquals(1.0, OvertimeUtil.calcOT(0L, ms(8, 41), 8), 0.0)
        assertEquals(1.0, OvertimeUtil.calcOT(0L, ms(8, 59), 8), 0.0)
        assertEquals(1.0, OvertimeUtil.calcOT(0L, ms(9, 0), 8), 0.0)
    }

    @Test
    fun `one and half hour OT when 1h11m to 1h40m over`() {
        assertEquals(1.0, OvertimeUtil.calcOT(0L, ms(9, 10), 8), 0.0)
        assertEquals(1.5, OvertimeUtil.calcOT(0L, ms(9, 11), 8), 0.0)
        assertEquals(1.5, OvertimeUtil.calcOT(0L, ms(9, 40), 8), 0.0)
    }

    @Test
    fun `two hours OT when 1h41m over`() {
        assertEquals(2.0, OvertimeUtil.calcOT(0L, ms(9, 41), 8), 0.0)
    }

    @Test
    fun `OT cost is hours times rate`() {
        assertEquals(50_000.0, OvertimeUtil.calcOTCost(1.0, 50_000.0), 0.0)
        assertEquals(25_000.0, OvertimeUtil.calcOTCost(0.5, 50_000.0), 0.0)
        assertEquals(75_000.0, OvertimeUtil.calcOTCost(1.5, 50_000.0), 0.0)
    }
}
