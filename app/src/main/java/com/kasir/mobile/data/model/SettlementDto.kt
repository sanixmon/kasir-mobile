package com.kasir.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SettlementDto(
    val id: String = "",
    val shiftUser: String = "",
    val tanggal: String = "",
    val closedAt: Long = 0L,
    val totalTxns: Int = 0,
    val revenue: Double = 0.0,
    val cash: Double = 0.0,
    val qris: Double = 0.0,
    val itemsSummary: String = ""
)

@Serializable
data class SettlementResponse(
    val success: Boolean = false,
    val error: String? = null,
    val settlement: SettlementDto? = null
)

@Serializable
data class GetSettlementsResponse(
    val success: Boolean = false,
    val error: String? = null,
    val settlements: List<SettlementDto> = emptyList()
)
