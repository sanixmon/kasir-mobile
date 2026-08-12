package com.kasir.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    val id: String,
    val queueNo: Int = 0,
    val nama: String = "",
    val items: List<ItemDto> = emptyList(),
    val startTime: Long = 0L,
    val tanggal: String = "",
    val payAwal: String = "cash"
)
