package com.kasir.mobile.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kasir.mobile.data.model.CatalogItem
import com.kasir.mobile.data.model.ItemCatalog
import com.kasir.mobile.data.model.ItemDto
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.domain.usecase.ShiftDateUtil
import com.kasir.mobile.ui.navigation.NavRoutes
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import com.kasir.mobile.ui.viewmodel.PaymentCalcData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: KasirViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeCheckoutSession by viewModel.activeCheckoutSession.collectAsState()
    val activePaymentData by viewModel.activePaymentData.collectAsState()
    val activeQrSession by viewModel.activeQrSession.collectAsState()

    var inputNama by remember { mutableStateOf("") }
    var payAwal by remember { mutableStateOf("cash") }
    var selectedQty by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var mobileSelectedTab by remember { mutableStateOf(0) } // 0: Sewa Baru, 1: Sesi Aktif

    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 600

    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val filteredSessions = remember(uiState.activeSessions, searchQuery) {
        uiState.activeSessions
            .filter { it.nama.contains(searchQuery, ignoreCase = true) }
            .sortedByDescending { it.startTime }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💰 Kasir Mobile", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = KasirGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Shift: ${uiState.currentShiftUser ?: "Kasir"} | ${ShiftDateUtil.getShiftDateFromNow()}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = KasirGreen
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(NavRoutes.DELETION_LOGS) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Log Hapus")
                    }
                    IconButton(onClick = { navController.navigate(NavRoutes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = KasirSurfaceVariant) {
                NavigationBarItem(
                    selected = uiState.activeTab == "dashboard",
                    onClick = { viewModel.setTab("dashboard") },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = uiState.activeTab == "history",
                    onClick = {
                        viewModel.setTab("history")
                        navController.navigate(NavRoutes.POS)
                    },
                    icon = { Icon(Icons.Filled.History, contentDescription = "Riwayat") },
                    label = { Text("Riwayat") }
                )
            }
        },
        containerColor = KasirSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isSmallScreen) {
                // Mobile layout with tabs
                TabRow(
                    selectedTabIndex = mobileSelectedTab,
                    containerColor = KasirSurfaceVariant,
                    contentColor = KasirGreen
                ) {
                    Tab(
                        selected = mobileSelectedTab == 0,
                        onClick = { mobileSelectedTab = 0 },
                        text = { Text("✨ Sewa Baru", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = mobileSelectedTab == 1,
                        onClick = { mobileSelectedTab = 1 },
                        text = { Text("⏱️ Sesi Aktif (${filteredSessions.size})", fontWeight = FontWeight.Bold) }
                    )
                }

                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    if (mobileSelectedTab == 0) {
                        SewaBaruContent(
                            inputNama = inputNama,
                            onNamaChange = { inputNama = it },
                            payAwal = payAwal,
                            onPayAwalChange = { payAwal = it },
                            selectedQty = selectedQty,
                            onQuantityChange = { code, delta ->
                                val current = selectedQty[code] ?: 0
                                selectedQty = selectedQty + (code to (current + delta).coerceAtLeast(0))
                            },
                            idrFormat = idrFormat,
                            onStartRental = {
                                val items = ItemCatalog.ITEMS
                                    .filter { (selectedQty[it.code] ?: 0) > 0 }
                                    .map { ItemDto(code = it.code, qty = selectedQty[it.code] ?: 1) }
                                if (inputNama.isNotBlank() && items.isNotEmpty()) {
                                    viewModel.startRental(inputNama.trim(), items, payAwal)
                                    inputNama = ""
                                    selectedQty = emptyMap()
                                }
                            }
                        )
                    } else {
                        SesiAktifContent(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            filteredSessions = filteredSessions,
                            onSelesai = { viewModel.activeCheckoutSession.value = it },
                            onShowQR = { viewModel.activeQrSession.value = it }
                        )
                    }
                }
            } else {
                // Tablet / Desktop side-by-side layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(0.45f).fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        color = KasirSurfaceVariant
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            SewaBaruContent(
                                inputNama = inputNama,
                                onNamaChange = { inputNama = it },
                                payAwal = payAwal,
                                onPayAwalChange = { payAwal = it },
                                selectedQty = selectedQty,
                                onQuantityChange = { code, delta ->
                                    val current = selectedQty[code] ?: 0
                                    selectedQty = selectedQty + (code to (current + delta).coerceAtLeast(0))
                                },
                                idrFormat = idrFormat,
                                onStartRental = {
                                    val items = ItemCatalog.ITEMS
                                        .filter { (selectedQty[it.code] ?: 0) > 0 }
                                        .map { ItemDto(code = it.code, qty = selectedQty[it.code] ?: 1) }
                                    if (inputNama.isNotBlank() && items.isNotEmpty()) {
                                        viewModel.startRental(inputNama.trim(), items, payAwal)
                                        inputNama = ""
                                        selectedQty = emptyMap()
                                    }
                                }
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(0.55f).fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        color = KasirSurfaceVariant
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            SesiAktifContent(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                filteredSessions = filteredSessions,
                                onSelesai = { viewModel.activeCheckoutSession.value = it },
                                onShowQR = { viewModel.activeQrSession.value = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modals
    activeCheckoutSession?.let { session ->
        CalculateRentalDialog(
            session = session,
            viewModel = viewModel,
            onClose = { viewModel.activeCheckoutSession.value = null },
            onProceedPayment = { calcData ->
                viewModel.activeCheckoutSession.value = null
                viewModel.activePaymentData.value = calcData
            }
        )
    }

    activePaymentData?.let { paymentData ->
        PaymentDialog(
            paymentData = paymentData,
            viewModel = viewModel,
            onClose = { viewModel.activePaymentData.value = null },
            onConfirm = { cash, qris ->
                viewModel.finalizePayment(paymentData, cash, qris)
            }
        )
    }

    activeQrSession?.let { session ->
        QrCodeDialog(
            session = session,
            onClose = { viewModel.activeQrSession.value = null }
        )
    }
}

@Composable
fun SewaBaruContent(
    inputNama: String,
    onNamaChange: (String) -> Unit,
    payAwal: String,
    onPayAwalChange: (String) -> Unit,
    selectedQty: Map<String, Int>,
    onQuantityChange: (String, Int) -> Unit,
    idrFormat: NumberFormat,
    onStartRental: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("✨ Sewa Baru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KasirGreen)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = inputNama,
            onValueChange = onNamaChange,
            label = { Text("Nama Penyewa") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirGreen)
        )

        Spacer(Modifier.height(8.dp))
        Text("Metode Bayar Awal (Pokok)", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = payAwal == "cash",
                onClick = { onPayAwalChange("cash") },
                label = { Text("Cash") },
                leadingIcon = { Icon(Icons.Filled.Payments, contentDescription = null) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KasirGreen)
            )
            FilterChip(
                selected = payAwal == "qris",
                onClick = { onPayAwalChange("qris") },
                label = { Text("QRIS") },
                leadingIcon = { Icon(Icons.Filled.QrCode, contentDescription = null) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KasirGreen)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("Pilih Item & Jumlah", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(ItemCatalog.ITEMS.size) { index ->
                val item = ItemCatalog.ITEMS[index]
                val qty = selectedQty[item.code] ?: 0
                ItemCatalogCard(
                    item = item,
                    qty = qty,
                    idrFormat = idrFormat,
                    onQuantityChange = { delta -> onQuantityChange(item.code, delta) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onStartRental,
            enabled = inputNama.isNotBlank() && selectedQty.values.any { it > 0 },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Mulai Sewa", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SesiAktifContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredSessions: List<SessionDto>,
    onSelesai: (SessionDto) -> Unit,
    onShowQR: (SessionDto) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⏱️ Sesi Aktif (${filteredSessions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Cari...") },
                singleLine = true,
                modifier = Modifier.width(140.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(8.dp))

        if (filteredSessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada sesi sewa aktif", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredSessions, key = { it.id }) { session ->
                    ActiveSessionCard(
                        session = session,
                        onSelesai = { onSelesai(session) },
                        onShowQR = { onShowQR(session) }
                    )
                }
            }
        }
    }
}
