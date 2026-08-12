package com.kasir.mobile.data.remote

import com.kasir.mobile.data.model.ActionSuccessResponse
import com.kasir.mobile.data.model.DeletionLogsResponse
import com.kasir.mobile.data.model.FetchAllDataResponse
import com.kasir.mobile.data.model.KasirRpcRequest
import com.kasir.mobile.data.model.VerifyAdminResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface KasirApiService {
    @POST("/api")
    suspend fun fetchAllData(@Body request: KasirRpcRequest): FetchAllDataResponse

    @POST("/api")
    suspend fun verifyAdmin(@Body request: KasirRpcRequest): VerifyAdminResponse

    @POST("/api")
    suspend fun addItem(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("/api")
    suspend fun updateItem(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("/api")
    suspend fun deleteItem(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("/api")
    suspend fun checkIn(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("/api")
    suspend fun checkOut(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("/api")
    suspend fun sellItems(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("/api")
    suspend fun rentItem(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("/api")
    suspend fun returnRental(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("/api")
    suspend fun addDeletion(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("/api")
    suspend fun getDeletionLogs(@Body request: KasirRpcRequest): DeletionLogsResponse
}
