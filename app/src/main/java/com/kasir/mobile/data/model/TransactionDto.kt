package com.kasir.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: String,
    val no: Long = 0L,
    val queueNo: Int = 0,
    val nama: String = "",
    val tanggal: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val items: String = "",
    val ot: String = "-",
    val otDur: String = "-",
    val totalBase: Double = 0.0,
    val totalOT: Double = 0.0,
    val totalTol: Double = 0.0,
    val grandTotal: Double = 0.0,
    val totalAll: Double = 0.0,
    val payAwal: String = "cash",
    val cash: Double = 0.0,
    val qris: Double = 0.0,
    val shift: String = "-"
)

@Serializable
data class DeletionLogDto(
    val id: Long? = null,
    val txnId: String? = null,
    val txnNo: Long? = null,
    val txnNama: String = "",
    val txnTanggal: String = "",
    val txnTotalAll: Double = 0.0,
    val deletedAt: Long = System.currentTimeMillis(),
    val deletedBy: String = "admin"
)
