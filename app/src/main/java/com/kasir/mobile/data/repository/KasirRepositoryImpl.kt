package com.kasir.mobile.data.repository

import com.kasir.mobile.data.model.ActionSuccessResponse
import com.kasir.mobile.data.model.DeletionLogDto
import com.kasir.mobile.data.model.DeletionLogsResponse
import com.kasir.mobile.data.model.FetchAllDataResponse
import com.kasir.mobile.data.model.KasirRpcRequest
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.data.model.VerifyAdminResponse
import com.kasir.mobile.data.remote.KasirApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class KasirRepositoryImpl(private val apiService: KasirApiService) : KasirRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchAllData(): Result<FetchAllDataResponse> = runCatching {
        apiService.fetchAllDataPost(KasirRpcRequest(action = "fetch_data"))
    }

    override suspend fun addSession(session: SessionDto): Result<ActionSuccessResponse> = runCatching {
        val payload = json.encodeToJsonElement(SessionDto.serializer(), session).jsonObject
        apiService.addSession(KasirRpcRequest(action = "add_session", payload = payload))
    }

    override suspend fun editSession(session: SessionDto): Result<ActionSuccessResponse> = runCatching {
        val payload = json.encodeToJsonElement(SessionDto.serializer(), session).jsonObject
        apiService.editSession(KasirRpcRequest(action = "edit_session", payload = payload))
    }

    override suspend fun deleteSession(id: String): Result<ActionSuccessResponse> = runCatching {
        apiService.deleteSession(KasirRpcRequest(action = "delete_session", payload = buildJsonObject { put("id", id) }))
    }

    override suspend fun claimSession(claimPayload: Map<String, Any?>): Result<ActionSuccessResponse> = runCatching {
        apiService.claimSession(KasirRpcRequest(action = "claim_session", payload = buildJsonObject {
            claimPayload.forEach { (k, v) ->
                when (v) {
                    is String -> put(k, v)
                    is Number -> put(k, v.toDouble())
                    is Boolean -> put(k, v)
                    else -> put(k, v.toString())
                }
            }
        }))
    }

    override suspend fun deleteTxn(id: String?, no: Long?): Result<ActionSuccessResponse> = runCatching {
        apiService.deleteTxn(KasirRpcRequest(action = "delete_txn", payload = buildJsonObject {
            id?.let { put("id", it) }
            no?.let { put("no", it) }
        }))
    }

    override suspend fun clearAllTxns(): Result<ActionSuccessResponse> = runCatching {
        apiService.clearAllTxns(KasirRpcRequest(action = "clear_all_txns"))
    }

    override suspend fun verifyAdmin(password: String): Result<VerifyAdminResponse> = runCatching {
        apiService.verifyAdmin(KasirRpcRequest(action = "verify_admin", payload = buildJsonObject { put("password", password) }))
    }

    override suspend fun changeAdminPass(oldPass: String, newPass: String): Result<ActionSuccessResponse> = runCatching {
        apiService.changeAdminPass(KasirRpcRequest(action = "change_admin_pass", payload = buildJsonObject {
            put("old_password", oldPass)
            put("new_password", newPass)
        }))
    }

    override suspend fun addDeletionLog(log: DeletionLogDto): Result<ActionSuccessResponse> = runCatching {
        val payload = json.encodeToJsonElement(DeletionLogDto.serializer(), log).jsonObject
        apiService.addDeletionLog(KasirRpcRequest(action = "add_deletion_log", payload = payload))
    }

    override suspend fun getDeletionLogs(): Result<DeletionLogsResponse> = runCatching {
        apiService.getDeletionLogs(KasirRpcRequest(action = "get_deletion_logs"))
    }
}
