package com.kasir.mobile.data.remote

import com.kasir.mobile.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap

object RetrofitClient {

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
    fun apiService(baseUrl: String): KasirApiService {
        val normalized = sanitizeServerUrl(baseUrl)
        return cache.getOrPut(normalized) {
            Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(KasirApiService::class.java)
        }
    }

    /**
     * Normalizes a raw server URL and enforces HTTPS:
     * - missing scheme defaults to HTTPS (never HTTP)
     * - an explicit http:// host is rewritten to https://
     * - trailing slashes and a trailing "/api" segment are stripped
     */
    fun sanitizeServerUrl(url: String): String {
        var u = url.trim()
        if (u.isBlank()) return sanitizeServerUrl(BuildConfig.API_BASE_URL)

        if (u.startsWith("http://")) {
            u = "https://${u.removePrefix("http://")}"
        } else if (!u.startsWith("https://")) {
            u = "https://$u"
        }
        while (u.endsWith("/")) u = u.dropLast(1)
        if (u.endsWith("/api", ignoreCase = true)) u = u.dropLast(4)
        return "$u/"
    }
}
