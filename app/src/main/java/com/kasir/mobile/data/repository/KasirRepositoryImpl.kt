package com.kasir.mobile.data.repository

import com.kasir.mobile.data.model.ActionSuccessResponse
import com.kasir.mobile.data.model.DeletionLogsResponse
import com.kasir.mobile.data.model.FetchAllDataResponse
import com.kasir.mobile.data.model.ItemDto
import com.kasir.mobile.data.model.KasirRpcRequest
import com.kasir.mobile.data.model.VerifyAdminResponse
import com.kasir.mobile.data.remote.KasirApiService
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.addJsonObject

class KasirRepositoryImpl(private val apiService: KasirApiService) : KasirRepository {
    override suspend fun fetchAllData(): Result<FetchAllDataResponse> = runCatching {
        apiService.fetchAllData(KasirRpcRequest(action = "fetch_data"))
    }

    override suspend fun verifyAdmin(pin: String): Result<VerifyAdminResponse> = runCatching {
        apiService.verifyAdmin(KasirRpcRequest(action = "verify_admin", payload = buildJsonObject { put("password", pin) }))
    }

    override suspend fun addItem(name: String, price: Double, category: String, stock: Int): Result<ActionSuccessResponse> = runCatching {
        apiService.addItem(KasirRpcRequest(action = "add_item", payload = buildJsonObject { 
            put("name", name)
            put("price", price)
            put("category", category)
            put("stock", stock)
        }))
    }

    override suspend fun updateItem(id: String, name: String, price: Double, category: String, stock: Int): Result<ActionSuccessResponse> = runCatching {
        apiService.updateItem(KasirRpcRequest(action = "update_item", payload = buildJsonObject { 
            put("id", id)
            put("name", name)
            put("price", price)
            put("category", category)
            put("stock", stock)
        }))
    }

    override suspend fun deleteItem(id: String): Result<ActionSuccessResponse> = runCatching {
        apiService.deleteItem(KasirRpcRequest(action = "delete_item", payload = buildJsonObject { put("id", id) }))
    }

    override suspend fun checkIn(userId: String, shiftDate: String): Result<ActionSuccessResponse> = runCatching {
        apiService.checkIn(KasirRpcRequest(action = "check_in", payload = buildJsonObject { 
            put("userId", userId)
            put("shiftDate", shiftDate)
        }))
    }

    override suspend fun checkOut(sessionId: String): Result<ActionSuccessResponse> = runCatching {
        apiService.checkOut(KasirRpcRequest(action = "check_out", payload = buildJsonObject { put("sessionId", sessionId) }))
    }

    override suspend fun sellItems(items: List<ItemDto>): Result<ActionSuccessResponse> = runCatching {
        apiService.sellItems(KasirRpcRequest(action = "sell_items", payload = buildJsonObject {
            putJsonArray("items") {
                items.forEach { item ->
                    addJsonObject {
                        put("itemId", item.code)
                        put("qty", item.qty)
                    }
                }
            }
        }))
    }

    override suspend fun rentItem(itemId: String, qty: Int, customerId: String): Result<ActionSuccessResponse> = runCatching {
        apiService.rentItem(KasirRpcRequest(action = "rent_item", payload = buildJsonObject { 
            put("itemId", itemId)
            put("qty", qty)
            put("customerId", customerId)
        }))
    }

    override suspend fun returnRental(rentalId: String): Result<ActionSuccessResponse> = runCatching {
        apiService.returnRental(KasirRpcRequest(action = "return_rental", payload = buildJsonObject { put("rentalId", rentalId) }))
    }

    override suspend fun addDeletion(targetType: String, targetId: String, reason: String): Result<ActionSuccessResponse> = runCatching {
        apiService.addDeletion(KasirRpcRequest(action = "add_deletion_log", payload = buildJsonObject { 
            put("targetType", targetType)
            put("targetId", targetId)
            put("reason", reason)
        }))
    }

    override suspend fun getDeletionLogs(): Result<DeletionLogsResponse> = runCatching {
        apiService.getDeletionLogs(KasirRpcRequest(action = "get_deletion_logs"))
    }
}
