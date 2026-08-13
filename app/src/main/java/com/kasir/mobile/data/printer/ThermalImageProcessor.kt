package com.kasir.mobile.data.printer

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Converts an Android [Bitmap] into a 1-bit thermal raster for ESC/POS printing.
 * Pipeline: resize → grayscale → Floyd-Steinberg dithering → Boolean grid.
 *
 * The printer receives a real 1-bit raster (via `GS v 0`), not ASCII block
 * characters — the X583 V2 is GBK/CP437, so `█░▒▓` are never sent as text.
 */
class ThermalImageProcessor {

    /** Scales to the printer's printable dot width, preserving aspect ratio. */
    fun resize(bitmap: Bitmap, targetWidth: Int): Bitmap {
        if (bitmap.width == targetWidth) return bitmap
        val ratio = targetWidth.toFloat() / bitmap.width
        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /** Luminance-only copy of the bitmap (0.299 R + 0.587 G + 0.114 B). */
    fun grayscale(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val gray = (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)).toInt()
                result.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        return result
    }

    /**
     * Floyd-Steinberg error-diffusion dither. Returns a y-major grid where
     * `true` = BLACK (print a dot) and `false` = WHITE.
     */
    fun floydSteinberg(bitmap: Bitmap): Array<BooleanArray> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = Array(height) { DoubleArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = bitmap.getPixel(x, y)
                pixels[y][x] = 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
            }
        }

        val result = Array(height) { BooleanArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val oldPixel = pixels[y][x]
                val newPixel = if (oldPixel < 128.0) 0.0 else 255.0
                result[y][x] = newPixel == 0.0
                val error = oldPixel - newPixel

                if (x + 1 < width) pixels[y][x + 1] += error * 7.0 / 16.0
                if (y + 1 < height) {
                    if (x > 0) pixels[y + 1][x - 1] += error * 3.0 / 16.0
                    pixels[y + 1][x] += error * 5.0 / 16.0
                    if (x + 1 < width) pixels[y + 1][x + 1] += error * 1.0 / 16.0
                }
            }
        }
        return result
    }

    /** Convenience: resize + dither straight to a 1-bit grid. */
    fun toThermalPixels(bitmap: Bitmap, targetWidth: Int): Array<BooleanArray> =
        floydSteinberg(resize(bitmap, targetWidth))
}
