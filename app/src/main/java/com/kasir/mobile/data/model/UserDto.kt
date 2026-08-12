package com.kasir.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val username: String,
    val password: String = "",
    val role: String = "cashier"
)
