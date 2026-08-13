package com.kasir.mobile.data.printer

/**
 * Turns a domain [Receipt] into ESC/POS bytes, sized to the printer paper.
 * No floating point anywhere — all amounts are [Long] (Rupiah).
 */
class ReceiptFormatter(
    private val profile: PrinterProfile,
    private val charset: PrinterCharset
) {
    private val width: Int get() = profile.paper.charactersPerLine
    private val itemNameWidth = 16
    private val qtyWidth = 6
    private val totalWidth = width - itemNameWidth - qtyWidth

    fun toCommands(receipt: Receipt): ByteArray {
        val e = EscPosEncoder(charset).init()
        val w = width

        e.alignCenter().bold(true).size(1, 2).text(receipt.storeName).size(1, 1).bold(false).line()
        e.alignCenter().text(receipt.address ?: "Scooter & Stroller").line()
        e.alignLeft().text("-".repeat(w)).line()

        e.text("Transaction").text(" : ").text(receipt.transactionId ?: "-").line()
        e.text("Date").text("        : ").text(receipt.dateTime).line()
        receipt.cashier?.let { e.text("Cashier").text("    : ").text(it).line() }
        e.text("-".repeat(w)).line()

        e.text(threeCol("Item", itemNameWidth, "Qty", qtyWidth, "Total", totalWidth)).line()
        e.text("-".repeat(w)).line()
        receipt.items.forEach { item ->
            val nameLines = wrap(item.name, itemNameWidth)
            nameLines.forEachIndexed { i, nameLine ->
                if (i == 0) {
                    e.text(threeCol(nameLine, itemNameWidth, item.quantity.toString(), qtyWidth, formatRupiah(item.total), totalWidth)).line()
                } else {
                    e.text(nameLine).line()
                }
            }
        }
        e.text("-".repeat(w)).line()
        if (receipt.discount > 0) {
            e.text(twoCol("Diskon", formatRupiah(receipt.discount), w)).line()
        }
        e.text(twoCol("Subtotal", formatRupiah(receipt.subtotal), w)).line()
        receipt.overtime?.takeIf { it > 0 }?.let {
            e.text(twoCol("Overtime", formatRupiah(it), w)).line()
        }
        e.bold(true).text(twoCol("TOTAL", formatRupiah(receipt.total), w)).bold(false).line()
        e.text("-".repeat(w)).line()
        receipt.payment?.let { e.text(twoCol("Bayar", formatRupiah(it), w)).line() }
        receipt.change?.let { e.text(twoCol("Kembali", formatRupiah(it), w)).line() }

        e.feed(1)
        e.alignCenter().text(receipt.footer ?: "Terima Kasih").line()
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

fun center(text: String, width: Int): String {
    val totalPad = (width - text.length).coerceAtLeast(0)
    val left = totalPad / 2
    return " ".repeat(left) + text + " ".repeat(totalPad - left)
}

fun twoCol(left: String, right: String, width: Int): String =
    left + " ".repeat((width - left.length - right.length).coerceAtLeast(1)) + right

fun threeCol(a: String, aw: Int, b: String, bw: Int, c: String, cw: Int): String =
    a.padEnd(aw) + b.padStart(bw) + c.padStart(cw)

fun wrap(text: String, width: Int): List<String> {
    if (text.length <= width) return listOf(text)
    val out = ArrayList<String>()
    var i = 0
    while (i < text.length) {
        out.add(text.substring(i, (i + width).coerceAtMost(text.length)))
        i += width
    }
    return out
}
