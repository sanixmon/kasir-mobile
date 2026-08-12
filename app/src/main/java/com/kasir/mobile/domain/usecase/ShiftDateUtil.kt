package com.kasir.mobile.domain.usecase

import java.util.Calendar

object ShiftDateUtil {
    // Port of kasir-db/src/lib/shift.js getShiftDate()
    // A shift starts at 06:00 and rolls over at 06:00 next day
    // Return the shift date string YYYY-MM-DD for a given epoch millis
    fun getShiftDate(epochMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        calendar.add(Calendar.HOUR_OF_DAY, -6)
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    fun getShiftDateFromNow(): String = getShiftDate(System.currentTimeMillis())
}
