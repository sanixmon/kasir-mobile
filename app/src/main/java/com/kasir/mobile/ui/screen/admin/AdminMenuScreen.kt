package com.kasir.mobile.ui.screen.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kasir.mobile.domain.usecase.ShiftDateUtil
import com.kasir.mobile.ui.navigation.NavRoutes
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirDisplay
import com.kasir.mobile.ui.theme.KasirError
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirGreenDark
import com.kasir.mobile.ui.theme.KasirLine
import com.kasir.mobile.ui.theme.KasirMono
import com.kasir.mobile.ui.theme.KasirOnSurface
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.theme.KasirTextLow
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Landing screen after admin login — a bento launcher instead of the cashier
 * POS. Admins pick between the cashier dashboard (POS) and cashier management;
 * a status tile shows server connectivity and a logout button ends the session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(
    navController: NavController,
    viewModel: KasirViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Today's summary: revenue & transaction count from the current shift date.
    val todayShift = ShiftDateUtil.getShiftDateFromNow()
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
    val todayTxns = remember(uiState.transactions, todayShift) {
        uiState.transactions.filter { it.tanggal == todayShift }
    }
    val revenueToday = remember(todayTxns) { todayTxns.sumOf { it.totalAll } }

    // Refresh user count / connectivity without starting the 5s polling loop
    // (polling is Dashboard-scoped).
    LaunchedEffect(Unit) {
        viewModel.loadData(showSync = false)
    }

    Scaffold(containerColor = KasirSurface) { padding ->
        // Whole page scrolls so nothing gets squashed or clipped on small screens.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KasirSurfaceVariant)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(KasirGreenDark.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ElectricScooter,
                        contentDescription = null,
                        tint = KasirGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "EVREN HOUSE",
                        fontFamily = KasirDisplay,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 16.sp,
                        color = KasirOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Menu Admin",
                        style = MaterialTheme.typography.labelMedium,
                        color = KasirTextLow,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Connection status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (uiState.apiConnected) KasirGreen.copy(alpha = 0.12f) else KasirAccent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (uiState.apiConnected) KasirGreen else KasirAccent,
                                shape = CircleShape
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (uiState.apiConnected) "ONLINE" else "OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = KasirMono,
                        color = if (uiState.apiConnected) KasirGreen else KasirAccent
                    )
                }
            }

            // ── Bento grid ─────────────────────────────────────────────────
            // Fixed height (not weight): inside a scrollable column weight can't
            // stretch, and a fixed size keeps tiles from collapsing on small screens.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    .height(252.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminMenuTile(
                    title = "Dashboard Kasir",
                    subtitle = "Buka POS · Sewa Baru & Sesi Aktif",
                    icon = Icons.Filled.ElectricScooter,
                    tint = KasirGreen,
                    onClick = { navController.navigate(NavRoutes.DASHBOARD) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminMenuTile(
                        title = "Manajemen Kasir",
                        subtitle = "Tambah & hapus akun kasir",
                        icon = Icons.Filled.Group,
                        tint = KasirAccent,
                        onClick = { navController.navigate(NavRoutes.USERS) },
                        modifier = Modifier.weight(1f)
                    )
                    AdminMenuTile(
                        title = "Log Penghapusan",
                        subtitle = "Audit transaksi yang dihapus",
                        icon = Icons.Filled.Delete,
                        tint = KasirError,
                        onClick = {
                            viewModel.loadDeletionLogs()
                            navController.navigate(NavRoutes.DELETION_LOGS)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Ringkasan hari ini ────────────────────────────────────────
            StatsTile(
                revenueToday = revenueToday,
                txnCount = todayTxns.size,
                activeCount = uiState.activeSessions.size,
                idrFormat = idrFormat,
                onClick = { navController.navigate(NavRoutes.POS) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
            )

            // ── Status strip ──────────────────────────────────────────────
            StatusTile(
                isOnline = uiState.apiConnected,
                userCount = uiState.users.size,
                shiftUser = uiState.currentShiftUser,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
            )

            // ── Logout ─────────────────────────────────────────────────────
            Button(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KasirSurfaceCard,
                    contentColor = KasirOnSurface
                ),
                border = BorderStroke(1.dp, KasirLine)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Keluar dari Menu Admin", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Keluar?") },
            text = { Text("Sesi admin akan ditutup dan kembali ke halaman login.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout()
                        navController.navigate(NavRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                ) {
                    Text("Keluar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun AdminMenuTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = KasirSurfaceCard,
        border = BorderStroke(1.dp, KasirLine),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(tint.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = KasirOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = KasirTextLow,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatsTile(
    revenueToday: Double,
    txnCount: Int,
    activeCount: Int,
    idrFormat: NumberFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = KasirSurfaceCard,
        border = BorderStroke(1.dp, KasirLine),
        modifier = modifier.height(84.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell(
                label = "Pendapatan",
                value = idrFormat.format(revenueToday),
                valueColor = KasirGreen,
                modifier = Modifier.weight(1.3f)
            )
            StatDivider()
            StatCell(
                label = "Transaksi",
                value = "$txnCount",
                valueColor = KasirOnSurface,
                modifier = Modifier.weight(1f)
            )
            StatDivider()
            StatCell(
                label = "Sesi Aktif",
                value = "$activeCount",
                valueColor = KasirAccent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            color = KasirTextLow,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontFamily = KasirMono,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .padding(vertical = 6.dp)
            .background(KasirLine)
    )
}

@Composable
private fun StatusTile(
    isOnline: Boolean,
    userCount: Int,
    shiftUser: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = KasirSurfaceCard,
        border = BorderStroke(1.dp, KasirLine),
        modifier = modifier.height(76.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isOnline) KasirGreen else KasirAccent, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOnline) "Server Online" else "Server Offline",
                    fontFamily = KasirMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isOnline) KasirGreen else KasirAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Admin: ${shiftUser ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KasirTextLow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$userCount kasir",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = KasirOnSurface
                )
                Text(
                    "terdaftar",
                    style = MaterialTheme.typography.labelSmall,
                    color = KasirTextLow
                )
            }
        }
    }
}
