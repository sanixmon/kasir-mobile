package com.kasir.mobile.data.printer

import java.util.Locale

/**
 * Turns a domain [Receipt] into ESC/POS bytes, sized to the printer paper.
 * Layout mirrors kasir-db (App.jsx handlePrintMulai / handlePrintSelesai):
 * "Struk Mulai Sewa" and "Struk Selesai Sewa". All amounts are [Long] (Rupiah).
 */
class ReceiptFormatter(
    private val profile: PrinterProfile,
    private val charset: PrinterCharset
) {
    private val width: Int get() = profile.paper.charactersPerLine
    private val dashes: String get() = "-".repeat(width)

    fun toCommands(receipt: Receipt): ByteArray {
        val e = EscPosEncoder(charset).init()
        val w = width
        val title = if (receipt.type == ReceiptType.MULAI) "Struk Mulai Sewa" else "Struk Selesai Sewa"

        // ── Header ──────────────────────────────────────────────────────────
        e.alignCenter().bold(true).size(1, 2).text(receipt.storeName).size(1, 1).bold(false).line()
        e.alignCenter().text(receipt.subtitle).line()
        e.alignCenter().text(title).line()
        e.alignLeft().text(dashes).line()

        // ── Info ────────────────────────────────────────────────────────────
        e.text("Queue Number: ${receipt.queueNo}").line()
        if (receipt.type == ReceiptType.SELESAI) {
            e.text("No: ${receipt.no} | ${receipt.tanggal}").line()
        } else {
            e.text("Tgl: ${receipt.tanggal} | ${receipt.startTime}").line()
        }
        e.text("Nama: ${receipt.nama}").line()
        e.text("Shift: ${receipt.shift ?: "-"}").line()
        if (receipt.type == ReceiptType.SELESAI) {
            e.text("Mulai: ${receipt.startTime} | Selesai: ${receipt.endTime ?: "-"}").line()
            receipt.durasi?.let { e.text("Durasi: $it").line() }
        }
        e.text(dashes).line()

        // ── Items ───────────────────────────────────────────────────────────
        if (receipt.type == ReceiptType.MULAI) {
            receipt.itemsText.split("\n").forEach { line ->
                if (line.isBlank()) return@forEach
                wrapWords(line, w).forEach { wrapped -> e.text(wrapped).line() }
            }
        } else {
            wrapWords("Item: ${receipt.itemsText}", w).forEach { e.text(it).line() }
            receipt.otText?.let { e.text("OT: $it").line() }
        }
        e.text(dashes).line()

        // ── Totals ──────────────────────────────────────────────────────────
        if (receipt.type == ReceiptType.MULAI) {
            e.bold(true).text(twoCol("Total Pokok:", rp(receipt.totalPokok), w)).bold(false).line()
        } else {
            val payLabel = receipt.payAwal?.uppercase(Locale.getDefault()) ?: "CASH"
            e.text(twoCol("Sewa Pokok:", "${rp(receipt.totalPokok)} ($payLabel)", w)).line()
            receipt.overtime?.takeIf { it > 0 }?.let {
                e.text(twoCol("Overtime:", rp(it), w)).line()
            }
            e.text(dashes).line()
            e.bold(true).text(twoCol("TOTAL:", rp(receipt.total), w)).bold(false).line()
            receipt.cash?.takeIf { it > 0 }?.let { e.text(twoCol("Cash:", rp(it), w)).line() }
            receipt.qris?.takeIf { it > 0 }?.let { e.text(twoCol("QRIS:", rp(it), w)).line() }
        }
        e.text(dashes).line()

        // ── QR ──────────────────────────────────────────────────────────────
        if (profile.supportsQr && receipt.qrText != null) {
            e.alignCenter().qr(receipt.qrText, 6, QrErrorCorrection.M)
            receipt.qrCaption?.let { e.alignCenter().text(it).line() }
            e.alignLeft().text(dashes).line()
        }

        // ── Footer ──────────────────────────────────────────────────────────
        e.feed(1)
        e.alignCenter().text(receipt.footer).line()
        e.feed(2)
        if (profile.supportsCut) e.cut()
        return e.build()
    }

    /** Diagnostic page that exercises connection, encoding, alignment and QR. */
    fun testPrintCommands(): ByteArray {
        val e = EscPosEncoder(charset).init()
        val eq = "=".repeat(30)

        e.text(eq).line()
        e.alignCenter().bold(true).text("EVREN HOUSE").bold(false).line()
        e.alignCenter().text("PRINTER TEST").line()
        e.alignLeft().text(eq).line()
        e.text("Printer : ${profile.name}").line()
        e.text("System  : ${profile.system}").line()
        e.line()
        e.text("ASCII TEST").line()
        e.text("ABCDEFGHIJKLMNOPQRSTUVWXYZ").line()
        e.text("abcdefghijklmnopqrstuvwxyz").line()
        e.text("0123456789").line()
        e.line()
        e.text("INDONESIAN TEXT").line()
        e.text("Sewa Scooter").line()
        e.text("Sewa Stroller").line()
        e.text("Terima Kasih").line()
        e.line()
        e.text("ALIGNMENT").line()
        e.alignLeft().text("LEFT").line()
        e.alignCenter().text("CENTER").line()
        e.alignRight().text("RIGHT").line()
        e.alignLeft()
        e.line()
        e.text("QR TEST").line()
        if (profile.supportsQr) e.qr("https://example.com", 6, QrErrorCorrection.M)
        e.line()
        e.text(eq).line()
        e.feed(2)
        if (profile.supportsCut) e.cut()
        return e.build()
    }
}

// ── Pure text helpers (unit-testable) ───────────────────────────────────────

/** 50000 -> "50.000", 1500000 -> "1.500.000". */
fun formatRupiah(value: Long): String {
    val negative = value < 0
    val s = if (negative) (-value).toString() else value.toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
        sb.append(s[i])
    }
    return if (negative) "-$sb" else sb.toString()
}

/** kasir-db fmtRp: 70000 -> "Rp 70.000". */
fun rp(value: Long): String = "Rp ${formatRupiah(value)}"

fun twoCol(left: String, right: String, width: Int): String =
    left + " ".repeat((width - left.length - right.length).coerceAtLeast(1)) + right

/** Word-boundary wrap so prices/tokens are never split mid-word. */
fun wrapWords(text: String, width: Int): List<String> {
    if (text.length <= width) return listOf(text)
    val words = text.split(" ")
    val out = ArrayList<String>()
    var cur = ""
    for (word in words) {
        cur = when {
            cur.isEmpty() -> word
            cur.length + 1 + word.length <= width -> "$cur $word"
            else -> { out.add(cur); word }
        }
    }
    if (cur.isNotEmpty()) out.add(cur)
    return out
}
