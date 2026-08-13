package com.kasir.mobile.data.printer

import java.nio.charset.Charset

enum class PrinterType { BLUETOOTH_CLASSIC }

data class PrinterDevice(
    val name: String,
    val address: String,
    val type: PrinterType = PrinterType.BLUETOOTH_CLASSIC
)

/** Structured, user-facing printer errors. Extends Exception so it can flow through Result. */
sealed class PrinterError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object BluetoothDisabled : PrinterError("Bluetooth dinonaktifkan")
    data object PermissionDenied : PrinterError("Izin Bluetooth ditolak")
    data object DeviceNotFound : PrinterError("Printer tidak ditemukan")
    data object ConnectionFailed : PrinterError("Gagal terhubung ke printer")
    data object NotConnected : PrinterError("Printer belum terhubung")
    data object PrintFailed : PrinterError("Gagal mencetak ke printer")
    data object UnsupportedCommand : PrinterError("Perintah tidak didukung printer")
    data object EncodingFailure : PrinterError("Gagal meng-encode teks struk")
    data class Unknown(override val cause: Throwable) : PrinterError(cause.message ?: "Terjadi kesalahan tak dikenal", cause)
}

sealed interface PrinterConnectionState {
    data object Disconnected : PrinterConnectionState
    data object Connecting : PrinterConnectionState
    data class Connected(val device: PrinterDevice) : PrinterConnectionState
    data class Error(val message: String) : PrinterConnectionState
}

data class PrinterPaperConfig(
    val paperWidthMm: Int,
    val charactersPerLine: Int
)

data class PrinterProfile(
    val name: String,
    val system: String,
    val encoding: Charset,
    val paper: PrinterPaperConfig,
    val supportsQr: Boolean,
    val supportsBarcode: Boolean,
    val supportsCut: Boolean,
    val supportsBold: Boolean,
    val supportsDoubleSize: Boolean,
    /** Printable dot width for raster images (58mm @ ~203 DPI ≈ 384 dots). */
    val rasterWidthDots: Int = 384
) {
    companion object {
        /** X583 V2 (ESC), Bluetooth name RP02N. */
        val X583_V2 = PrinterProfile(
            name = "RP02N",
            system = "X583 V2 (ESC)",
            encoding = Charset.forName("GBK"),
            paper = PrinterPaperConfig(paperWidthMm = 58, charactersPerLine = 32),
            supportsQr = true,
            supportsBarcode = false,
            supportsCut = true,
            supportsBold = true,
            supportsDoubleSize = true,
            rasterWidthDots = 384
        )
    }
}

enum class QrErrorCorrection { L, M, Q, H }

enum class BarcodeType { CODE128, EAN13, CODE39 }

/**
 * The two receipt layouts mirror kasir-db (App.jsx):
 *  - MULAI  = "Struk Mulai Sewa" (printed when a rental starts)
 *  - SELESAI = "Struk Selesai Sewa" (printed when a rental is closed/claimed)
 */
enum class ReceiptType { MULAI, SELESAI }

data class Receipt(
    val type: ReceiptType,
    val storeName: String = "EVREN HOUSE",
    val subtitle: String = "Scooter & Stroller",
    val queueNo: Int = 0,
    val no: Long? = null,
    val nama: String = "",
    val shift: String? = null,
    val tanggal: String = "",
    val startTime: String = "",
    val endTime: String? = null,
    val durasi: String? = null,
    val itemsText: String = "",
    val otText: String? = null,
    val totalPokok: Long = 0,
    val payAwal: String? = null,
    val overtime: Long? = null,
    val total: Long = 0,
    val cash: Long? = null,
    val qris: Long? = null,
    val qrText: String? = null,
    val qrCaption: String? = null,
    val footer: String = "Terima kasih telah berkunjung!"
)
