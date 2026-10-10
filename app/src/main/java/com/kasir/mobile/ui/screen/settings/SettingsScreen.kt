package com.kasir.mobile.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kasir.mobile.BuildConfig
import com.kasir.mobile.data.ServiceLocator
import com.kasir.mobile.data.local.SessionData
import com.kasir.mobile.data.local.SessionManager
import com.kasir.mobile.data.remote.UpdateInfo
import com.kasir.mobile.data.remote.UpdateManager
import com.kasir.mobile.ui.navigation.NavRoutes
import com.kasir.mobile.ui.theme.*
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: KasirViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdmin = uiState.currentUserRole == "admin"

    var selectedTab by remember { mutableStateOf(0) } // 0: Preferensi, 1: Admin & Laporan
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupError by remember { mutableStateOf(false) }
    var backupLoading by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val checkUpdateScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf<String?>(null) }
    var updateAvailableInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val sessionManager = remember { SessionManager(context) }
    val sessionData by sessionManager.sessionDataFlow.collectAsState(
        initial = SessionData(
            serverUrl = "",
            currentUserId = "",
            currentUserName = "",
            isAdmin = false,
            token = "",
            isDarkTheme = false
        )
    )

    val isPrinterConnected = remember {
        runCatching { ServiceLocator.printerRepository().isConnected() }.getOrDefault(false)
    }

    // Refresh analytics data silently when opening screen
    LaunchedEffect(Unit) { viewModel.loadData(showSync = false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Pengaturan",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
                )

                // Only show role-aware TabRow for Admin users
                if (isAdmin) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = KasirSurfaceVariant,
                        contentColor = KasirPrimary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Preferensi", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Admin & Laporan", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        },
        containerColor = KasirSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isAdmin || selectedTab == 0) {
                // ═════════════════════════════════════════════════════════════
                // TAB 0: PREFERENSI (Akses: Kasir & Admin)
                // ═════════════════════════════════════════════════════════════

                // Group 1: Perangkat & Struk Kasir
                SettingGroupCard(title = "Perangkat & Cetak Struk") {
                    SettingTile(
                        icon = Icons.Filled.Print,
                        iconTint = KasirPrimary,
                        iconContainerColor = KasirPrimaryContainer,
                        title = "Printer Thermal Kasir",
                        subtitle = if (isPrinterConnected) "Printer terhubung dan siap mencetak" else "Belum terhubung · Tap untuk atur printer",
                        trailing = {
                            Button(
                                onClick = { navController.navigate(NavRoutes.PRINTER) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KasirPrimaryContainer,
                                    contentColor = KasirPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Atur Printer", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    )

                    SettingDivider()

                    SettingTile(
                        icon = Icons.Filled.Receipt,
                        iconTint = KasirCash,
                        iconContainerColor = KasirCashContainer,
                        title = "Cetak Struk Mulai Sewa",
                        subtitle = "Cetak otomatis saat sewa baru dimulai",
                        trailing = {
                            Switch(
                                checked = uiState.printMulai,
                                onCheckedChange = { viewModel.togglePrintMulai(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KasirPrimary
                                )
                            )
                        }
                    )

                    SettingDivider()

                    SettingTile(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        iconTint = KasirQris,
                        iconContainerColor = KasirQrisContainer,
                        title = "Cetak Struk Selesai Sewa",
                        subtitle = "Cetak otomatis saat pembayaran diverifikasi",
                        trailing = {
                            Switch(
                                checked = uiState.printSelesai,
                                onCheckedChange = { viewModel.togglePrintSelesai(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KasirPrimary
                                )
                            )
                        }
                    )
                }

                // Group 2: Tampilan & Kenyamanan
                SettingGroupCard(title = "Tampilan") {
                    SettingTile(
                        icon = Icons.Filled.DarkMode,
                        iconTint = KasirAccent,
                        iconContainerColor = KasirAccentContainer,
                        title = "Tema Gelap (Night Counter)",
                        subtitle = "Mengurangi silau saat bertugas di malam hari",
                        trailing = {
                            Switch(
                                checked = sessionData.isDarkTheme,
                                onCheckedChange = { isDark ->
                                    checkUpdateScope.launch {
                                        sessionManager.setDarkTheme(isDark)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KasirPrimary
                                )
                            )
                        }
                    )
                }

                // Group 3: Sistem & Pembaruan
                SettingGroupCard(title = "Sistem & Pembaruan") {
                    SettingTile(
                        icon = Icons.Filled.Dns,
                        iconTint = KasirPrimary,
                        iconContainerColor = KasirPrimaryContainer,
                        title = "Server Aktif",
                        subtitle = ServiceLocator.activeServerUrl,
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = if (uiState.apiConnected) KasirCashContainer else KasirAccentContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (uiState.apiConnected) KasirCash else KasirAccent, CircleShape)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.apiConnected) "ONLINE" else "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.apiConnected) KasirCash else KasirAccent
                                )
                            }
                        }
                    )

                    SettingDivider()

                    SettingTile(
                        icon = Icons.Filled.SystemUpdate,
                        iconTint = KasirCash,
                        iconContainerColor = KasirCashContainer,
                        title = "Pembaruan Aplikasi",
                        subtitle = "Versi v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                        trailing = {
                            Button(
                                onClick = {
                                    checkUpdateScope.launch {
                                        isCheckingUpdate = true
                                        updateStatusText = null
                                        val res = UpdateManager.checkForUpdates(isManualCheck = true)
                                        isCheckingUpdate = false
                                        res.onSuccess { info ->
                                            if (info?.hasUpdate == true) {
                                                updateAvailableInfo = info
                                                updateStatusText = "v${info.latestVersion} tersedia"
                                                showUpdateDialog = true
                                            } else {
                                                updateAvailableInfo = null
                                                updateStatusText = "Aplikasi versi terbaru"
                                            }
                                        }.onFailure {
                                            updateStatusText = "Gagal (cek internet)"
                                        }
                                    }
                                },
                                enabled = !isCheckingUpdate,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KasirPrimary,
                                    contentColor = KasirOnPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isCheckingUpdate) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = KasirOnPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text("Periksa", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    )

                    updateStatusText?.let { msg ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (updateAvailableInfo != null) KasirCash else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 50.dp)
                        )
                    }
                }

                // Group 4: Sesi Kasir / Operator
                SettingGroupCard(title = "Sesi Akun") {
                    SettingTile(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconTint = KasirError,
                        iconContainerColor = KasirErrorContainer,
                        title = if (isAdmin) "Keluar dari Sesi" else "Keluar dari Shift (${uiState.currentShiftUser ?: "Kasir"})",
                        subtitle = "Menutup sesi kasir aktif dan kembali ke halaman login",
                        trailing = {
                            Button(
                                onClick = { showLogoutConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Keluar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }
            } else {
                // ═════════════════════════════════════════════════════════════
                // TAB 1: ADMIN & LAPORAN (Akses: Khusus Admin)
                // ═════════════════════════════════════════════════════════════

                // Section 1: Dashboard Analytics Laporan
                SettingsAnalyticsSection(
                    transactions = uiState.transactions,
                    activeSessionsCount = uiState.activeSessions.size,
                    currentShiftUser = uiState.currentShiftUser
                )

                // Section 2: Keamanan PIN Admin
                SettingGroupCard(title = "Keamanan & Autentikasi") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(KasirPrimaryContainer, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = KasirPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Ubah PIN Keamanan Admin", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Digunakan untuk otorisasi aksi sensitif", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    var oldPinVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { oldPin = it },
                        label = { Text("PIN Lama") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (oldPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { oldPinVisible = !oldPinVisible }) {
                                Icon(
                                    imageVector = if (oldPinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirPrimary),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    var newPinVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("PIN Baru") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (newPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { newPinVisible = !newPinVisible }) {
                                Icon(
                                    imageVector = if (newPinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirPrimary),
                        shape = RoundedCornerShape(12.dp)
                    )

                    pinMessage?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = msg,
                            color = if (pinError) MaterialTheme.colorScheme.error else KasirCash,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (oldPin.isNotBlank() && newPin.isNotBlank()) {
                                viewModel.changeAdminPassword(oldPin, newPin) { ok, msg ->
                                    pinError = !ok
                                    pinMessage = msg
                                    if (ok) {
                                        oldPin = ""
                                        newPin = ""
                                    }
                                }
                            }
                        },
                        enabled = oldPin.isNotBlank() && newPin.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KasirPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan PIN Baru", fontWeight = FontWeight.Bold)
                    }
                }

                // Section 3: Cadangan Database
                SettingGroupCard(title = "Pemeliharaan Database") {
                    SettingTile(
                        icon = Icons.Filled.CloudDownload,
                        iconTint = KasirCash,
                        iconContainerColor = KasirCashContainer,
                        title = "Cadangan Database Server",
                        subtitle = "Buat salinan SQLite server ke folder backups",
                        trailing = {
                            Button(
                                onClick = {
                                    backupLoading = true
                                    viewModel.backupDatabase { ok, msg ->
                                        backupLoading = false
                                        backupError = !ok
                                        backupMessage = msg
                                    }
                                },
                                enabled = !backupLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = KasirCash, contentColor = KasirOnCash),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                if (backupLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = KasirOnCash,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(if (backupLoading) "Memproses..." else "Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )

                    backupMessage?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = msg,
                            color = if (backupError) KasirError else KasirCash,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 50.dp)
                        )
                    }
                }

                // Section 4: Sesi Admin
                SettingGroupCard(title = "Sesi Admin") {
                    SettingTile(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        iconTint = KasirError,
                        iconContainerColor = KasirErrorContainer,
                        title = "Keluar dari Sesi Admin",
                        subtitle = "Sesi admin akan ditutup dan kembali ke halaman login",
                        trailing = {
                            Button(
                                onClick = { showLogoutConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Keluar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }
            }
        }
    }

    // ── Dialog Konfirmasi Logout ─────────────────────────────────────────────
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(if (isAdmin) "Keluar dari Sesi Admin?" else "Keluar dari Shift Kasir?") },
            text = { Text("Sesi operasional ${uiState.currentShiftUser ?: "kasir"} akan ditutup dan kembali ke halaman login.") },
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

    // ── Dialog Catatan Pembaruan ─────────────────────────────────────────────
    if (showUpdateDialog && updateAvailableInfo != null) {
        val update = updateAvailableInfo!!
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Pembaruan Tersedia") },
            text = {
                Column {
                    Text(
                        "Versi baru ${update.latestVersion} telah dirilis.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (update.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            update.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KasirPrimary)
                ) {
                    Text("Unduh Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun SettingGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = KasirPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KasirSurfaceCard,
            border = BorderStroke(1.dp, KasirLine.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingTile(
    icon: ImageVector,
    iconTint: Color = KasirPrimary,
    iconContainerColor: Color = KasirPrimaryContainer,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconContainerColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KasirOnSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = KasirLine.copy(alpha = 0.5f)
    )
}
