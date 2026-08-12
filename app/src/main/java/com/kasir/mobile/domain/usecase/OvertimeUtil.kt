package com.kasir.mobile.domain.usecase

import com.kasir.mobile.data.model.CatalogItem
import kotlin.math.floor

data class ItemOtResult(
    val otFullCount: Int,
    val otHalfCount: Int,
    val otCost: Double
)

object OvertimeUtil {

    /**
     * Calculate overtime for a specific item given elapsed minutes and limit minutes.
     * Port of kasir-db/src/lib/ot.js calcOT(elMin, limitMin)
     */
    fun calcItemOT(
        elapsedMin: Double,
        limitMin: Double,
        priceOT30: Double,
        priceOT60: Double,
        qty: Int = 1
    ): ItemOtResult {
        val overMin = elapsedMin - limitMin
        if (overMin < 0 || floor(overMin) < 11) {
            return ItemOtResult(0, 0, 0.0)
        }

        var otFull = floor(overMin / 60).toInt()
        var otHalf = 0

        val remainMin = floor(overMin % 60).toInt()
        when {
            remainMin in 11..40 -> otHalf = 1
            remainMin > 40 -> otFull += 1
        }

        val otCost = (otFull * priceOT60 + otHalf * priceOT30) * qty
        return ItemOtResult(otFull, otHalf, otCost)
    }

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
