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

@Composable
fun ItemCatalogCard(
    item: CatalogItem,
    qty: Int,
    idrFormat: NumberFormat,
    onQuantityChange: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (qty > 0) KasirGreen.copy(alpha = 0.15f) else KasirSurfaceCard,
        modifier = Modifier.fillMaxWidth().clickable { onQuantityChange(1) }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(item.emoji, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(item.code, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(item.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(
                text = if (item.isPackage) "Paket ${item.packageHours}j ${idrFormat.format(item.priceHour)}" else "${idrFormat.format(item.priceHour)}/j",
                style = MaterialTheme.typography.bodySmall,
                color = KasirGreen,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onQuantityChange(-1) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                Text("$qty", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                IconButton(onClick = { onQuantityChange(1) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun ActiveSessionCard(
    session: SessionDto,
    onSelesai: () -> Unit,
    onShowQR: () -> Unit
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val safeStart = if (session.startTime > 1577836800000L) session.startTime else now
    val elapsedSec = ((now - safeStart) / 1000).coerceAtLeast(0)
    val elapsedMin = elapsedSec / 60
    val isZombie = elapsedSec > 28800 // > 8h

    val timerColor = when {
        elapsedMin >= 71 -> Color(0xFFE53935) // Red
        elapsedMin >= 60 -> KasirAccent // Orange
        else -> Color(0xFF00E676) // Cyan/Green
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KasirSurfaceCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = KasirAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "#${session.queueNo}",
                            fontWeight = FontWeight.Bold,
                            color = KasirAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(session.nama, fontWeight = FontWeight.Bold)
                    if (isZombie) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = Color(0xFFFF9800), shape = RoundedCornerShape(4.dp)) {
                            Text("⚠️ ZOMBIE", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
                Surface(
                    color = if (session.payAwal == "qris") Color(0xFF7C4DFF).copy(alpha = 0.2f) else KasirGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        session.payAwal.uppercase(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                session.items.forEach { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "${item.code}×${item.qty}",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(safeStart)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%02d:%02d:%02d", elapsedSec / 3600, (elapsedSec % 3600) / 60, elapsedSec % 60),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = timerColor
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onSelesai,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
                ) {
                    Text("Selesai & Bayar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                IconButton(onClick = onShowQR, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Filled.QrCode, contentDescription = "QR Code")
                }
            }
        }
    }
}
