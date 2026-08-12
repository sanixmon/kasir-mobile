package com.kasir.mobile.data.repository

import com.kasir.mobile.data.model.ActionSuccessResponse
import com.kasir.mobile.data.model.DeletionLogsResponse
import com.kasir.mobile.data.model.FetchAllDataResponse
import com.kasir.mobile.data.model.ItemDto
import com.kasir.mobile.data.model.VerifyAdminResponse

interface KasirRepository {
    suspend fun fetchAllData(): Result<FetchAllDataResponse>
    suspend fun verifyAdmin(pin: String): Result<VerifyAdminResponse>
    suspend fun addItem(name: String, price: Double, category: String, stock: Int): Result<ActionSuccessResponse>
    suspend fun updateItem(id: String, name: String, price: Double, category: String, stock: Int): Result<ActionSuccessResponse>
    suspend fun deleteItem(id: String): Result<ActionSuccessResponse>
    suspend fun checkIn(userId: String, shiftDate: String): Result<ActionSuccessResponse>
    suspend fun checkOut(sessionId: String): Result<ActionSuccessResponse>
    suspend fun sellItems(items: List<ItemDto>): Result<ActionSuccessResponse>
    suspend fun rentItem(itemId: String, qty: Int, customerId: String): Result<ActionSuccessResponse>
    suspend fun returnRental(rentalId: String): Result<ActionSuccessResponse>
    suspend fun addDeletion(targetType: String, targetId: String, reason: String): Result<ActionSuccessResponse>
    suspend fun getDeletionLogs(): Result<DeletionLogsResponse>
}
