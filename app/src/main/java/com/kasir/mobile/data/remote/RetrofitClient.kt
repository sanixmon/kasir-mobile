package com.kasir.mobile.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap

object RetrofitClient {

    private const val DEFAULT_BASE_URL = "https://utara.evrenhouse.online"

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            // BASIC only — BODY would leak shift passwords in logs
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val cache = ConcurrentHashMap<String, KasirApiService>()

    /**
     * Returns a KasirApiService bound to [baseUrl]. Services are cached per URL.
     * Trailing slashes and a trailing "/api" path segment are normalized away.
     */
    fun apiService(baseUrl: String = DEFAULT_BASE_URL): KasirApiService {
        val normalized = normalize(baseUrl)
        return cache.getOrPut(normalized) {
            Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(KasirApiService::class.java)
        }
    }

    private fun normalize(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        while (u.endsWith("/")) u = u.dropLast(1)
        if (u.endsWith("/api", ignoreCase = true)) u = u.dropLast(4)
        return "$u/"
    }
}
