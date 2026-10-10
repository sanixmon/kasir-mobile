package com.kasir.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kasir.mobile.data.local.SessionData
import com.kasir.mobile.data.local.SessionManager
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.kasir.mobile.ui.navigation.KasirNavHost
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Immersive fullscreen: hide the status/navigation bars. Swipe from the
        // screen edges to reveal them transiently (they auto-hide again).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        val sessionManager = SessionManager(this)

        setContent {
            val session by sessionManager.sessionDataFlow.collectAsState(
                initial = SessionData(
                    serverUrl = "",
                    currentUserId = "",
                    currentUserName = "",
                    isAdmin = false,
                    token = "",
                    isDarkTheme = false
                )
            )

            KasirTheme(darkTheme = session.isDarkTheme) {
                Surface(color = KasirSurface) {
                    val navController = rememberNavController()
                    KasirNavHost(navController = navController)
                }
            }
        }
    }
}
