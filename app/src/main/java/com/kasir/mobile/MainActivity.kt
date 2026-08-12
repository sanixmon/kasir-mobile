package com.kasir.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.kasir.mobile.ui.navigation.KasirNavHost
import com.kasir.mobile.ui.theme.KasirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KasirTheme {
                Surface {
                    val navController = rememberNavController()
                    KasirNavHost(navController = navController)
                }
            }
        }
    }
}
