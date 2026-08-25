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

    @Test
    fun `calcItemOT applies 10m59s grace period on standard 60min rental`() {
        // Under limit (45 min) -> 0 OT
        val underLimit = OvertimeUtil.calcItemOT(45.0, 60.0, 25000.0, 50000.0)
        assertEquals(0, underLimit.otFullCount)
        assertEquals(0, underLimit.otHalfCount)
        assertEquals(0.0, underLimit.otCost, 0.0)

        // 70.9 minutes elapsed (10m54s over 60min limit) -> Grace period, 0 OT
        val gracePeriod = OvertimeUtil.calcItemOT(70.9, 60.0, 25000.0, 50000.0)
        assertEquals(0, gracePeriod.otFullCount)
        assertEquals(0, gracePeriod.otHalfCount)
        assertEquals(0.0, gracePeriod.otCost, 0.0)

        // 71.0 minutes elapsed (11m00s over 60min limit) -> Triggers 1 half-hour OT
        val triggerOT = OvertimeUtil.calcItemOT(71.0, 60.0, 25000.0, 50000.0)
        assertEquals(0, triggerOT.otFullCount)
        assertEquals(1, triggerOT.otHalfCount)
        assertEquals(25000.0, triggerOT.otCost, 0.0)

        // 100.0 minutes elapsed (40m00s over 60min limit) -> Still 1 half-hour OT
        val maxHalfOT = OvertimeUtil.calcItemOT(100.0, 60.0, 25000.0, 50000.0)
        assertEquals(0, maxHalfOT.otFullCount)
        assertEquals(1, maxHalfOT.otHalfCount)
        assertEquals(25000.0, maxHalfOT.otCost, 0.0)

        // 101.0 minutes elapsed (41m00s over 60min limit) -> Becomes 1 full-hour OT
        val fullOT = OvertimeUtil.calcItemOT(101.0, 60.0, 25000.0, 50000.0)
        assertEquals(1, fullOT.otFullCount)
        assertEquals(0, fullOT.otHalfCount)
        assertEquals(50000.0, fullOT.otCost, 0.0)

        // 130.9 minutes elapsed (70m54s over 60min limit) -> 1 full hour + grace period = 1 full hour OT
        val secondHourGrace = OvertimeUtil.calcItemOT(130.9, 60.0, 25000.0, 50000.0)
        assertEquals(1, secondHourGrace.otFullCount)
        assertEquals(0, secondHourGrace.otHalfCount)
        assertEquals(50000.0, secondHourGrace.otCost, 0.0)

        // 131.0 minutes elapsed (71m00s over 60min limit) -> 1 full hour + 1 half hour OT
        val secondHourHalf = OvertimeUtil.calcItemOT(131.0, 60.0, 25000.0, 50000.0)
        assertEquals(1, secondHourHalf.otFullCount)
        assertEquals(1, secondHourHalf.otHalfCount)
        assertEquals(75000.0, secondHourHalf.otCost, 0.0)
    }

    @Test
    fun `calcItemOT applies multi-unit multiplier and handles package 3 hours`() {
        // 2 units of Scooter Dewasa with 71 min elapsed -> 25000 * 2 = 50000
        val multiUnits = OvertimeUtil.calcItemOT(71.0, 60.0, 25000.0, 50000.0, qty = 2)
        assertEquals(0, multiUnits.otFullCount)
        assertEquals(1, multiUnits.otHalfCount)
        assertEquals(50000.0, multiUnits.otCost, 0.0)

        // Stroller Paket 3J (180 min limit):
        // 190.9 min elapsed -> grace period, 0 OT
        val pkgGrace = OvertimeUtil.calcItemOT(190.9, 180.0, 10000.0, 20000.0)
        assertEquals(0, pkgGrace.otFullCount)
        assertEquals(0, pkgGrace.otHalfCount)
        assertEquals(0.0, pkgGrace.otCost, 0.0)

        // 191.0 min elapsed -> 1 half hour OT (10000)
        val pkgTrigger = OvertimeUtil.calcItemOT(191.0, 180.0, 10000.0, 20000.0)
        assertEquals(0, pkgTrigger.otFullCount)
        assertEquals(1, pkgTrigger.otHalfCount)
        assertEquals(10000.0, pkgTrigger.otCost, 0.0)
    }
}
