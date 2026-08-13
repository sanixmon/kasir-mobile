package com.kasir.mobile.data.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * Builds the diagnostic test sheet for the X583 V2. Exercises: text, bold,
 * alignment, a Floyd-Steinberg dithered raster bitmap, QR code, then feed/cut.
 * The image is generated programmatically (no external resource) and is sent to
 * the printer as 1-bit raster data — never as `█░▒▓` characters.
 */
class TestPrintGenerator(
    private val profile: PrinterProfile,
    private val charset: PrinterCharset,
    private val imageProcessor: ThermalImageProcessor = ThermalImageProcessor()
) {
    private val width: Int get() = profile.paper.charactersPerLine
    private val dashes: String get() = "-".repeat(width)

    fun testPrintCommands(): ByteArray {
        val e = EscPosEncoder(charset).init()
        val w = width
        val eq = "=".repeat(w)

        // ── Header ──────────────────────────────────────────────────────────
        e.text(eq).line()
        e.alignCenter().bold(true).size(1, 2).text("EVREN HOUSE").size(1, 1).bold(false).line()
        e.alignCenter().text("PRINTER TEST").line()
        e.alignLeft().text(eq).line()

        // ── System info ─────────────────────────────────────────────────────
        e.text("Printer : ${profile.name}").line()
        e.text("System  : ${profile.system}").line()
        e.text(eq).line()

        // ── Text / bold / alignment ────────────────────────────────────────
        e.alignCenter().bold(true).text("TEXT / BOLD / ALIGN").bold(false).line()
        e.text("ABCDEFGHIJKLMNOPQRSTUVWXYZ").line()
        e.text("0123456789").line()
        e.text("Sewa Scooter - Sewa Stroller").line()
        e.bold(true).text("Terima Kasih (Bold)").bold(false).line()
        e.alignLeft().text("LEFT").line()
        e.alignCenter().text("CENTER").line()
        e.alignRight().text("RIGHT").line()
        e.alignLeft()
        e.text(eq).line()

        // ── Dithered bitmap ─────────────────────────────────────────────────
        e.alignCenter().bold(true).text("BITMAP / DITHER").bold(false).line()
        val pixels = imageProcessor.toThermalPixels(createTestBitmap(), profile.rasterWidthDots)
        e.rasterImage(pixels)
        e.alignLeft().line()
        e.text(eq).line()

        // ── QR ──────────────────────────────────────────────────────────────
        e.alignCenter().bold(true).text("QR CODE").bold(false).line()
        if (profile.supportsQr) e.qr("https://example.com", 6, QrErrorCorrection.M)
        e.alignLeft().line()

        // ── Result checklist ────────────────────────────────────────────────
        e.text("[OK] TEXT").line()
        e.text("[OK] BOLD").line()
        e.text("[OK] ALIGNMENT").line()
        e.text("[OK] BITMAP").line()
        e.text("[OK] DITHER").line()
        e.text("[OK] QR CODE").line()
        e.text(eq).line()

        e.alignCenter().bold(true).text("THANK YOU").bold(false).line()
        e.text(eq).line()

        e.feed(2)
        if (profile.supportsCut) e.cut()
        return e.build()
    }

    /**
     * Programmatic test image: a smooth radial gradient (best showcase for
     * dithering) plus solid black and white squares to verify pure tones.
     */
    private fun createTestBitmap(): Bitmap {
        val w = 256
        val h = 128
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val grad = Paint(Paint.ANTI_ALIAS_FLAG)
        grad.shader = RadialGradient(
            w / 2f, h / 2f, h * 0.48f,
            intArrayOf(Color.BLACK, Color.rgb(90, 90, 90), Color.rgb(210, 210, 210), Color.WHITE),
            floatArrayOf(0f, 0.45f, 0.85f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(w / 2f, h / 2f, h * 0.48f, grad)

        val solid = Paint()
        solid.color = Color.BLACK
        canvas.drawRect(6f, 6f, 26f, 26f, solid)
        solid.color = Color.WHITE
        canvas.drawRect(w - 26f, h - 26f, w - 6f, h - 6f, solid)

        return bmp
    }
}
