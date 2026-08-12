package com.kasir.mobile.domain.usecase

import kotlin.math.floor

/**
 * Port of kasir-db/src/lib/ot.js
 * Overtime calculation rules:
 * - Less than 11 minutes over normal hours → 0 OT
 * - 11–40 minutes over → 0.5 hour
 * - 41–60 minutes over → 1 hour
 * - For each additional hour, same rounding applies
 */
object OvertimeUtil {

    fun calcOT(checkInMillis: Long, checkOutMillis: Long, normalHours: Int = 8): Double {
        val elapsedMillis = checkOutMillis - checkInMillis
        val elapsedMin = elapsedMillis / 60000.0
        val limitMin = normalHours * 60.0

        val actualOver = elapsedMin - limitMin
        if (actualOver < 0 || floor(actualOver) < 11) return 0.0

        var otFull = floor(actualOver / 60).toInt()
        var otHalf = 0

        val remainMin = floor(actualOver % 60).toInt()
        when {
            remainMin in 11..40 -> otHalf = 1
            remainMin > 40 -> otFull += 1
        }

        return otFull + (otHalf * 0.5)
    }

    fun calcOTCost(otHours: Double, hourlyRate: Double): Double = otHours * hourlyRate
}
