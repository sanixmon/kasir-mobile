package com.kasir.mobile.data

import android.content.Context
import com.kasir.mobile.BuildConfig
import com.kasir.mobile.data.printer.BluetoothPrinterRepository
import com.kasir.mobile.data.printer.PrinterRepository
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
    val DEFAULT_SERVER_URL: String = BuildConfig.API_BASE_URL

    @Volatile
    var activeServerUrl: String = DEFAULT_SERVER_URL

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var printerRepositoryInstance: PrinterRepository? = null

    private val repositoryCache = ConcurrentHashMap<String, KasirRepository>()

    /** Call once from Application.onCreate() so the printer module can resolve a Context. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Shared Bluetooth printer instance so the Printer screen and Dashboard print to the same device. */
    fun printerRepository(): PrinterRepository {
        val ctx = appContext ?: throw IllegalStateException("ServiceLocator.init(context) belum dipanggil")
        return printerRepositoryInstance ?: synchronized(this) {
            printerRepositoryInstance ?: BluetoothPrinterRepository(ctx).also { printerRepositoryInstance = it }
        }
    }

    fun repository(): KasirRepository = repositoryCache.getOrPut(activeServerUrl) {
        KasirRepositoryImpl(RetrofitClient.apiService(activeServerUrl))
    }

    fun setServerUrl(url: String) {
        activeServerUrl = RetrofitClient.sanitizeServerUrl(url).trimEnd('/')
    }
}
