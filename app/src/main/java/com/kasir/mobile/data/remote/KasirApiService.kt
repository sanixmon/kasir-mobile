package com.kasir.mobile.data.remote

import com.kasir.mobile.data.model.ActionSuccessResponse
import com.kasir.mobile.data.model.DeletionLogsResponse
import com.kasir.mobile.data.model.FetchAllDataResponse
import com.kasir.mobile.data.model.KasirRpcRequest
import com.kasir.mobile.data.model.LoginCashierResponse
import com.kasir.mobile.data.model.VerifyAdminResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface KasirApiService {
    @GET("api")
    suspend fun fetchAllDataGet(): FetchAllDataResponse

    @POST("api")
    suspend fun fetchAllDataPost(@Body request: KasirRpcRequest): FetchAllDataResponse

    @POST("api")
    suspend fun addSession(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("api")
    suspend fun editSession(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("api")
    suspend fun claimSession(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("api")
    suspend fun deleteSession(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("api")
    suspend fun deleteTxn(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("api")
    suspend fun clearAllTxns(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("api")
    suspend fun verifyAdmin(@Body request: KasirRpcRequest): VerifyAdminResponse

    @POST("api")
    suspend fun changeAdminPass(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("api")
    suspend fun loginCashier(@Body request: KasirRpcRequest): LoginCashierResponse

    @POST("api")
    suspend fun addDeletionLog(@Body request: KasirRpcRequest): ActionSuccessResponse

    @POST("api")
    suspend fun getDeletionLogs(@Body request: KasirRpcRequest): DeletionLogsResponse
}
