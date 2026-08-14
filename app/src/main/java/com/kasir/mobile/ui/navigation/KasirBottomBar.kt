package com.kasir.mobile.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/** Vertical side rail used on landscape so the nav doesn't eat vertical space. */
@Composable
fun KasirNavigationRail(
    selectedTab: String,
    onSelectTab: (String) -> Unit
) {
    NavigationRail(containerColor = KasirSurfaceVariant) {
        NavigationRailItem(
            selected = selectedTab == "dashboard",
            onClick = { onSelectTab("dashboard") },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") }
        )
        NavigationRailItem(
            selected = selectedTab == "history",
            onClick = { onSelectTab("history") },
            icon = { Icon(Icons.Filled.History, contentDescription = "Riwayat") },
            label = { Text("Riwayat") }
        )
    }
}

/**
 * Scaffold wrapper that swaps the bottom NavigationBar for a side
 * NavigationRail in landscape, keeping vertical space for content.
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
    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            KasirNavigationRail(selectedTab = selectedTab, onSelectTab = onSelectTab)
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = topBar,
                snackbarHost = snackbarHost,
                containerColor = containerColor
            ) { padding -> content(padding) }
        }
    } else {
        Scaffold(
            topBar = topBar,
            bottomBar = { KasirBottomBar(selectedTab = selectedTab, onSelectTab = onSelectTab) },
            snackbarHost = snackbarHost,
            containerColor = containerColor
        ) { padding -> content(padding) }
    }
}
