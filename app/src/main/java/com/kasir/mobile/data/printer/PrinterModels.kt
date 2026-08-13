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
    val supportsDoubleSize: Boolean
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
            supportsDoubleSize = true
        )
    }
}

enum class QrErrorCorrection { L, M, Q, H }

enum class BarcodeType { CODE128, EAN13, CODE39 }

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Long,
    val total: Long
)

data class Receipt(
    val storeName: String,
    val address: String? = null,
    val phone: String? = null,
    val transactionId: String? = null,
    val dateTime: String,
    val cashier: String? = null,
    val items: List<ReceiptItem>,
    val subtotal: Long,
    val discount: Long = 0,
    val overtime: Long? = null,
    val total: Long,
    val payment: Long? = null,
    val change: Long? = null,
    val footer: String? = null
)
