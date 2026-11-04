package com.kasir.mobile.ui.screen.dashboard

import com.kasir.mobile.data.model.ItemDto
import com.kasir.mobile.data.model.SessionDto
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSortTest {

    private val t0 = 1_800_000_000_000L // valid (post-2020) base time
    private val min = 60_000L

    private fun session(id: String, queueNo: Int, start: Long, vararg items: ItemDto) =
        SessionDto(id = id, queueNo = queueNo, startTime = start, items = items.toList())

    @Test
    fun packageItemGetsItsFullWindow() {
        // Stroller Paket 3J started at t0 → free until t0 + 180 min.
        val s = session("a", 1, t0, ItemDto("SB", 1))
        assertEquals(t0 + 180 * min, s.deadline(now = t0).freeUntil)
    }

    @Test
    fun earliestItemDeadlineWins_andUsesPerItemStart() {
        // Package from t0 (deadline t0+180m) and a stroller added at t0+30m (deadline t0+90m).
        val s = session("a", 1, t0, ItemDto("SB", 1), ItemDto("ST", 1, startTime = t0 + 30 * min))
        val d = s.deadline(now = t0)
        assertEquals(t0 + 90 * min, d.freeUntil)
        assertEquals(60 * min, d.windowMs)
    }

    @Test
    fun mendesakPutsNearestDeadlineFirst_evenIfStartedLater() {
        val pkgOld = session("pkg", 1, t0, ItemDto("SB", 1))            // due t0+180m
        val hourNewer = session("hour", 2, t0 + 20 * min, ItemDto("ST", 1)) // due t0+80m
        val sorted = listOf(pkgOld, hourNewer).sortedFor(SessionSort.MENDESAK, now = t0 + 30 * min)
        assertEquals(listOf("hour", "pkg"), sorted.map { it.id })
    }

    @Test
    fun tiesBreakByQueueNumber() {
        val b = session("b", 5, t0, ItemDto("ST", 1))
        val a = session("a", 2, t0, ItemDto("ST", 1))
        for (sort in SessionSort.entries) {
            assertEquals("$sort", listOf("a", "b"), listOf(b, a).sortedFor(sort, now = t0).map { it.id })
        }
    }

    @Test
    fun terbaruAndTerlamaOrderByStart() {
        val early = session("early", 1, t0, ItemDto("ST", 1))
        val late = session("late", 2, t0 + 10 * min, ItemDto("ST", 1))
        assertEquals(listOf("late", "early"), listOf(early, late).sortedFor(SessionSort.TERBARU, t0).map { it.id })
        assertEquals(listOf("early", "late"), listOf(late, early).sortedFor(SessionSort.TERLAMA, t0).map { it.id })
    }

    @Test
    fun sortCyclesThroughAllModes() {
        assertEquals(SessionSort.TERBARU, SessionSort.MENDESAK.next())
        assertEquals(SessionSort.TERLAMA, SessionSort.TERBARU.next())
        assertEquals(SessionSort.MENDESAK, SessionSort.TERLAMA.next())
    }
}
