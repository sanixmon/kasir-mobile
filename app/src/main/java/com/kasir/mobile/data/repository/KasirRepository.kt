package com.kasir.mobile.data.repository

import com.kasir.mobile.data.model.ActionSuccessResponse
import com.kasir.mobile.data.model.BackupResponse
import com.kasir.mobile.data.model.DeletionLogDto
import com.kasir.mobile.data.model.DeletionLogsResponse
import com.kasir.mobile.data.model.FetchAllDataResponse
import com.kasir.mobile.data.model.LoginAdminResponse
import com.kasir.mobile.data.model.LoginCashierResponse
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.data.model.TransactionDto
import com.kasir.mobile.data.model.VerifyAdminResponse
import com.kasir.mobile.data.model.GetSettlementsResponse
import com.kasir.mobile.data.model.SettlementDto
import com.kasir.mobile.data.model.SettlementResponse

interface KasirRepository {
    suspend fun fetchAllData(): Result<FetchAllDataResponse>
    suspend fun addSession(session: SessionDto): Result<ActionSuccessResponse>
    suspend fun editSession(session: SessionDto): Result<ActionSuccessResponse>
    suspend fun deleteSession(id: String): Result<ActionSuccessResponse>
    suspend fun claimSession(claimPayload: Map<String, Any?>): Result<ActionSuccessResponse>
    suspend fun deleteTxn(id: String?, no: Long?): Result<ActionSuccessResponse>
    suspend fun clearAllTxns(): Result<ActionSuccessResponse>
    suspend fun verifyAdmin(password: String): Result<VerifyAdminResponse>
    suspend fun loginCashier(username: String, password: String): Result<LoginCashierResponse>
    suspend fun loginAdmin(password: String): Result<LoginAdminResponse>
    suspend fun changeAdminPass(oldPass: String, newPass: String): Result<ActionSuccessResponse>
    suspend fun addDeletionLog(log: DeletionLogDto): Result<ActionSuccessResponse>
    suspend fun getDeletionLogs(): Result<DeletionLogsResponse>
    suspend fun saveUser(username: String, password: String, role: String): Result<ActionSuccessResponse>
    suspend fun deleteUser(username: String): Result<ActionSuccessResponse>
    suspend fun backupDatabase(): Result<BackupResponse>
    suspend fun saveSettlement(settlement: SettlementDto): Result<SettlementResponse>
    suspend fun getSettlements(limit: Int = 50): Result<GetSettlementsResponse>
    suspend fun logout(): Result<ActionSuccessResponse>
}
