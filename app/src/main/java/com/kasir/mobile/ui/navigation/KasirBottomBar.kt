package com.kasir.mobile.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kasir.mobile.ui.theme.KasirSurfaceVariant

/**
 * Persistent bottom navigation shared by the two main tabs (Dashboard & Riwayat).
 * Portrait-only: on landscape the nav is hidden entirely so the tablet content
 * uses the full height.
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

/**
 * Scaffold wrapper that shows the bottom nav only in portrait. On landscape the
 * nav (rail/bottom bar) is hidden so the screen is used edge-to-edge.
 */
@Composable
fun KasirResponsiveScaffold(
    isLandscape: Boolean,
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    topBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = topBar,
        bottomBar = {
            if (!isLandscape) {
                KasirBottomBar(selectedTab = selectedTab, onSelectTab = onSelectTab)
            }
        },
        snackbarHost = snackbarHost,
        containerColor = containerColor
    ) { padding -> content(padding) }
}
