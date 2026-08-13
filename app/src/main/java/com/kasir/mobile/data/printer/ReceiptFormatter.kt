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
        e.alignCenter().text("Queue Number: ${receipt.queueNo}").line()
        e.alignLeft()
        if (receipt.type == ReceiptType.SELESAI) {
            e.text("No: ${receipt.no} | ${receipt.tanggal}").line()
        } else {
            e.text("Tgl: ${receipt.tanggal} | ${receipt.startTime}").line()
        }
        e.text("Shift: ${receipt.shift ?: "-"}").line()
        e.text("Nama: ${receipt.nama}").line()
        if (receipt.type == ReceiptType.SELESAI) {
            e.text("Mulai: ${receipt.startTime} | Selesai: ${receipt.endTime ?: "-"}").line()
            receipt.durasi?.let { e.text("Durasi: $it").line() }
        }
        e.text(dashes).line()

        // ── Items ───────────────────────────────────────────────────────────
        if (receipt.type == ReceiptType.MULAI) {
            receipt.itemsText.split("\n").forEach { line ->
                if (line.isBlank()) return@forEach
                itemLines(line, w).forEach { wrapped -> e.text(wrapped).line() }
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

        // ── QR (Struk Mulai Sewa only) ─────────────────────────────────────
        if (receipt.type == ReceiptType.MULAI && profile.supportsQr && receipt.qrText != null) {
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

    /**
     * Right-aligns the price (the part after a double space, per the item
     * format `name xQty  RpPrice`) on the last line of a wrapped item, keeping
     * every amount on the same right column as "Total Pokok:".
     */
    private fun itemLines(line: String, w: Int): List<String> {
        val idx = line.lastIndexOf("  ")
        if (idx < 0) return wrapWords(line, w)
        val left = line.substring(0, idx)
        val price = line.substring(idx + 2)
        val leftWidth = (w - price.length - 1).coerceAtLeast(1)
        val leftLines = wrapWords(left, leftWidth)
        return leftLines.mapIndexed { i, l ->
            if (i == leftLines.size - 1) twoCol(l, price, w) else l
        }
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
