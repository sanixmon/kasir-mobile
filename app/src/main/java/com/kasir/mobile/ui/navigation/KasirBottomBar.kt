package com.kasir.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.kasir.mobile.ui.theme.KasirSurfaceVariant

/**
 * Persistent bottom navigation shared by the two main tabs (Dashboard & Riwayat).
 * Kept in one place so the bar looks and behaves identically on both screens.
 */
@Composable
fun KasirBottomBar(
    selectedTab: String,
    onSelectTab: (String) -> Unit
) {
    NavigationBar(containerColor = KasirSurfaceVariant) {
        NavigationBarItem(
            selected = selectedTab == "dashboard",
            onClick = { onSelectTab("dashboard") },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") }
        )
        NavigationBarItem(
            selected = selectedTab == "history",
            onClick = { onSelectTab("history") },
            icon = { Icon(Icons.Filled.History, contentDescription = "Riwayat") },
            label = { Text("Riwayat") }
        )
    }
}
