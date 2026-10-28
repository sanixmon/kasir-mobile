package com.kasir.mobile.ui.screen.settlement

import com.kasir.mobile.data.model.TransactionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementScreenLogicTest {

    @Test
    fun testParseAggregatedItemsWithVariousFormats() {
        val txns = listOf(
            TransactionDto(
                id = "t1",
                items = "SD×1, SJ×2"
            ),
            TransactionDto(
                id = "t2",
                items = "sd x1, sj x1, st x 2"
            ),
            TransactionDto(
                id = "t3",
                items = "SD X 1"
            )
        )

        val aggregated = parseAggregatedItems(txns)

        // SD: 1 + 1 + 1 = 3
        val sd = aggregated.find { it.code == "SD" }
        assertEquals("Scooter Dewasa", sd?.name)
        assertEquals(3, sd?.totalQty)
        assertEquals(150000.0, sd?.estimatedRevenue ?: 0.0, 0.01)

        // SJ: 2 + 1 = 3
        val sj = aggregated.find { it.code == "SJ" }
        assertEquals("Scooter Jumbo", sj?.name)
        assertEquals(3, sj?.totalQty)
        assertEquals(180000.0, sj?.estimatedRevenue ?: 0.0, 0.01)

        // ST: 2
        val st = aggregated.find { it.code == "ST" }
        assertEquals("Stroller", st?.name)
        assertEquals(2, st?.totalQty)
        assertEquals(40000.0, st?.estimatedRevenue ?: 0.0, 0.01)
    }

    @Test
    fun testFinancialCalculationAccuratelySeparatesCashAndQris() {
        val shiftTxns = listOf(
            // Txn 1: Base paid in cash, finished on-time (cash = 0.0, qris = 0.0)
            TransactionDto(
                id = "t1",
                totalBase = 50000.0,
                totalOT = 0.0,
                totalAll = 50000.0,
                payAwal = "cash",
                cash = 0.0,
                qris = 0.0
            ),
            // Txn 2: Base paid in QRIS, finished on-time (cash = 0.0, qris = 0.0)
            TransactionDto(
                id = "t2",
                totalBase = 60000.0,
                totalOT = 0.0,
                totalAll = 60000.0,
                payAwal = "qris",
                cash = 0.0,
                qris = 0.0
            ),
            // Txn 3: Base paid in cash, overtime 20000 paid in QRIS
            TransactionDto(
                id = "t3",
                totalBase = 50000.0,
                totalOT = 20000.0,
                totalAll = 70000.0,
                payAwal = "cash",
                cash = 0.0,
                qris = 20000.0
            ),
            // Txn 4: Base paid in QRIS, overtime 10000 paid in Cash
            TransactionDto(
                id = "t4",
                totalBase = 20000.0,
                totalOT = 10000.0,
                totalAll = 30000.0,
                payAwal = "qris",
                cash = 10000.0,
                qris = 0.0
            )
        )

        val totalRevenue = shiftTxns.sumOf { it.totalAll }
        val totalCash = shiftTxns.sumOf { t ->
            val pokokCash = if (t.payAwal.trim().equals("qris", ignoreCase = true)) 0.0 else t.totalBase
            pokokCash + t.cash
        }
        val totalQris = shiftTxns.sumOf { t ->
            val pokokQris = if (t.payAwal.trim().equals("qris", ignoreCase = true)) t.totalBase else 0.0
            pokokQris + t.qris
        }

        assertEquals(210000.0, totalRevenue, 0.01)
        // Cash: Txn1(50k) + Txn3(50k) + Txn4 OT(10k) = 110,000
        assertEquals(110000.0, totalCash, 0.01)
        // QRIS: Txn2(60k) + Txn3 OT(20k) + Txn4(20k) = 100,000
        assertEquals(100000.0, totalQris, 0.01)
        assertEquals(totalRevenue, totalCash + totalQris, 0.01)
    }
}
