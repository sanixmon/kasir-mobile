package com.kasir.mobile.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class KasirRpcRequest(
    val action: String,
    val payload: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class FetchAllDataResponse(
    val sessions: List<SessionDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val users: List<UserDto> = emptyList(),
    val settings: Map<String, String> = emptyMap()
)

@Serializable
data class ActionSuccessResponse(
    val success: Boolean = false,
    val error: String? = null,
    val session: SessionDto? = null,
    val transaction: TransactionDto? = null
)

@Serializable
data class VerifyAdminResponse(
    val valid: Boolean = false,
    val token: String? = null
)

@Serializable
data class LoginCashierResponse(
    val success: Boolean = false,
    val error: String? = null,
    val user: UserDto? = null,
    val token: String? = null
)

@Serializable
data class LoginAdminResponse(
    val success: Boolean = false,
    val error: String? = null,
    val user: UserDto? = null,
    val token: String? = null
)

@Serializable
data class DeletionLogsResponse(
    val logs: List<DeletionLogDto> = emptyList()
)
