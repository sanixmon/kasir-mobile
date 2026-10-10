package com.kasir.mobile.data.printer

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class ReceiptFormatterTest {

    @Test
    fun testAdditionalReceiptFormatting() {
        val formatter = ReceiptFormatter(PrinterProfile.GENERIC_58MM, GbkPrinterCharset())
        val receipt = Receipt(
            type = ReceiptType.ADDITIONAL,
            queueNo = 7,
            nama = "Budi Tambahan",
            shift = "Shift 1",
            tanggal = "17/09/2026",
            startTime = "14:30",
            itemsText = "Scooter Dewasa x1  Rp 50.000",
            totalPokok = 50000L,
            total = 50000L,
            payAwal = "cash",
            footer = "Terima kasih!"
        )

        val commands = formatter.toCommands(receipt)
        val decoded = String(commands, Charset.forName("CP437"))

        assertTrue("Should contain ADDITIONAL ORDER banner", decoded.contains("*** ADDITIONAL ORDER ***"))
        assertTrue("Should contain Struk Tambahan Sewa title", decoded.contains("Struk Tambahan Sewa"))
        assertTrue("Should contain queue number", decoded.contains("Queue Number: 7"))
        assertTrue("Should contain customer name", decoded.contains("Budi Tambahan"))
        assertTrue("Should contain Total Tambahan with pay method", decoded.contains("Total Tambahan:"))
        assertTrue("Should contain CASH label", decoded.contains("CASH"))
    }

    @Test
    fun testMulaiReceiptFormatting() {
        val formatter = ReceiptFormatter(PrinterProfile.GENERIC_58MM, GbkPrinterCharset())
        val receipt = Receipt(
            type = ReceiptType.MULAI,
            queueNo = 3,
            nama = "Ani Sewa",
            tanggal = "17/09/2026",
            startTime = "10:00",
            itemsText = "Stroller x1  Rp 20.000",
            totalPokok = 20000L,
            total = 20000L,
            footer = "Terima kasih!"
        )

        val commands = formatter.toCommands(receipt)
        val decoded = String(commands, Charset.forName("CP437"))

        assertTrue("Should contain Struk Mulai Sewa", decoded.contains("Struk Mulai Sewa"))
        assertTrue("Should contain Total Pokok", decoded.contains("Total Pokok:"))
    }

    @Test
    fun testSelesaiReceiptFormatting() {
        val formatter = ReceiptFormatter(PrinterProfile.GENERIC_58MM, GbkPrinterCharset())
        val receipt = Receipt(
            type = ReceiptType.SELESAI,
            queueNo = 3,
            nama = "Ani Sewa",
            startTime = "10:00",
            endTime = "11:15",
            durasi = "75m",
            itemsText = "Stroller x1  Rp 20.000",
            totalPokok = 20000L,
            overtime = 10000L,
            total = 30000L,
            payAwal = "cash",
            cash = 10000L,
            footer = "Terima kasih!"
        )

        val commands = formatter.toCommands(receipt)
        val decoded = String(commands, Charset.forName("CP437"))

        assertTrue("Should contain Struk Selesai Sewa", decoded.contains("Struk Selesai Sewa"))
        assertTrue("Should contain Sewa Pokok", decoded.contains("Sewa Pokok:"))
        assertTrue("Should contain Overtime", decoded.contains("Overtime:"))
        assertTrue("Should contain TOTAL", decoded.contains("TOTAL:"))
    }

    @Test
    fun testSettlementReceiptFormatting() {
        val formatter = ReceiptFormatter(PrinterProfile.GENERIC_58MM, GbkPrinterCharset())
        val receipt = Receipt(
            type = ReceiptType.SETTLEMENT,
            shift = "Akbar Kasir",
            tanggal = "2026-10-10",
            endTime = "14:00",
            totalTxns = 8,
            totalPokok = 350000L,
            overtime = 50000L,
            total = 400000L,
            cash = 250000L,
            qris = 150000L,
            footer = "CLOSING SHIFT SELESAI"
        )

        val commands = formatter.toCommands(receipt)
        val decoded = String(commands, Charset.forName("CP437"))

        assertTrue("Should contain Struk Tutup Shift", decoded.contains("Struk Tutup Shift"))
        assertTrue("Should contain Shift date", decoded.contains("2026-10-10"))
        assertTrue("Should contain Petugas", decoded.contains("Akbar Kasir"))
        assertTrue("Should contain Total Trx", decoded.contains("8 Selesai"))
        assertTrue("Should contain TOTAL OMZET", decoded.contains("TOTAL OMZET:"))
        assertTrue("Should contain Total Tunai", decoded.contains("Total Tunai:"))
        assertTrue("Should contain Total QRIS", decoded.contains("Total QRIS:"))
    }
}
