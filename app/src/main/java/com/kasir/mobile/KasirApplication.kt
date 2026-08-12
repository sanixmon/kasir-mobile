package com.kasir.mobile

import android.app.Application
import com.kasir.mobile.data.ServiceLocator
import com.kasir.mobile.data.local.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KasirApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Restore the server URL the user last logged in with
        val sessionManager = SessionManager(this)
        appScope.launch {
            val session = sessionManager.sessionDataFlow.first()
            ServiceLocator.setServerUrl(session.serverUrl)
        }
    }
}
