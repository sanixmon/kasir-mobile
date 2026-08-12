package com.kasir.mobile.data

import com.kasir.mobile.data.remote.RetrofitClient
import com.kasir.mobile.data.repository.KasirRepository
import com.kasir.mobile.data.repository.KasirRepositoryImpl
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple manual dependency container.
 *
 * The Android client is a pure client of the kasir-backend (PM2, port 3001 by
 * default). The server URL can be changed at login; repositories are cached per
 * URL so the whole app always talks to the same backend.
 */
object ServiceLocator {
    const val DEFAULT_SERVER_URL = "http://10.0.2.2:3001"

    @Volatile
    var activeServerUrl: String = DEFAULT_SERVER_URL

    private val repositoryCache = ConcurrentHashMap<String, KasirRepository>()

    fun repository(): KasirRepository = repositoryCache.getOrPut(activeServerUrl) {
        KasirRepositoryImpl(RetrofitClient.apiService(activeServerUrl))
    }

    fun setServerUrl(url: String) {
        val trimmed = url.trim().trimEnd('/')
        activeServerUrl = if (trimmed.endsWith("/api", ignoreCase = true)) {
            trimmed.dropLast(4)
        } else {
            trimmed
        }
    }
}
