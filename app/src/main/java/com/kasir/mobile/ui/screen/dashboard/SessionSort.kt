package com.kasir.mobile.ui.screen.dashboard

import com.kasir.mobile.data.model.ItemCatalog
import com.kasir.mobile.data.model.SessionDto

/** Same sanity floor the checkout uses: timestamps before 2020 are treated as missing. */
private const val MIN_VALID_TIME = 1577836800000L

/**
 * When the session's most urgent item runs out of its free window.
 * [freeUntil] is the deadline (ms); [windowMs] is that item's free window,
 * so `freeUntil - windowMs` is when that item started.
 */
data class SessionDeadline(val freeUntil: Long, val windowMs: Long)

/**
 * Per-item deadline = item start + free window (1 jam, or the package's
 * hours), mirroring KasirViewModel.preparePayment. The session's deadline is
 * the earliest one, i.e. the first item that will need an overtime charge.
 */
fun SessionDto.deadline(now: Long): SessionDeadline {
    val sessionStart = if (startTime > MIN_VALID_TIME) startTime else now
    return items.map { item ->
        val def = ItemCatalog.findByCode(item.code)
        val limitMin = if (def?.isPackage == true) def.packageHours * 60 else 60
        val itemStart = item.startTime?.takeIf { it > MIN_VALID_TIME } ?: sessionStart
        val windowMs = limitMin * 60_000L
        SessionDeadline(itemStart + windowMs, windowMs)
    }.minByOrNull { it.freeUntil } ?: SessionDeadline(sessionStart + 3_600_000L, 3_600_000L)
}

enum class SessionSort(val label: String) {
    MENDESAK("Mendesak"),
    TERBARU("Terbaru"),
    TERLAMA("Terlama");

    fun next(): SessionSort = entries[(ordinal + 1) % entries.size]
}

/**
 * Orders active sessions for the dashboard. Queue number breaks ties so cards
 * never swap places between polls.
 */
fun List<SessionDto>.sortedFor(sort: SessionSort, now: Long): List<SessionDto> = when (sort) {
    SessionSort.MENDESAK -> sortedWith(compareBy<SessionDto>({ it.deadline(now).freeUntil }, { it.queueNo }))
    SessionSort.TERBARU -> sortedWith(compareByDescending<SessionDto> { it.startTime }.thenBy { it.queueNo })
    SessionSort.TERLAMA -> sortedWith(compareBy<SessionDto>({ it.startTime }, { it.queueNo }))
}
