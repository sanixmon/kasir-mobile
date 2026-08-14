package com.kasir.mobile.ui.screen.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kasir.mobile.data.model.CatalogItem
import com.kasir.mobile.data.model.ItemCatalog
import com.kasir.mobile.data.model.ItemDto
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.domain.usecase.ShiftDateUtil
import com.kasir.mobile.ui.components.rememberPressScale
import com.kasir.mobile.ui.navigation.KasirBottomBar
import com.kasir.mobile.ui.navigation.NavRoutes
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirCash
import com.kasir.mobile.ui.theme.KasirError
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirLine
import com.kasir.mobile.ui.theme.KasirMono
import com.kasir.mobile.ui.theme.KasirOnSurface
import com.kasir.mobile.ui.theme.KasirOnSurfaceVariant
import com.kasir.mobile.ui.theme.KasirQris
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.theme.KasirTextLow
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
    val activeEditSession by viewModel.activeEditSession.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show transient success/error messages
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val msg = uiState.errorMessage ?: uiState.successMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    var inputNama by remember { mutableStateOf("") }
    var payAwal by remember { mutableStateOf("cash") }
    var selectedQty by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var mobileSelectedTab by remember { mutableStateOf(0) } // 0: Sewa Baru, 1: Sesi Aktif

    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 600
    // Large screens keep the same 2-tab layout but show active sessions in a
    // 3-column grid instead of a single column.
    val sessionColumns = if (isSmallScreen) 1 else 3

    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val filteredSessions = remember(uiState.activeSessions, searchQuery) {
        uiState.activeSessions
            .filter { s ->
                searchQuery.isBlank() ||
                    s.nama.contains(searchQuery, ignoreCase = true) ||
                    s.queueNo.toString().contains(searchQuery) ||
                    s.items.any { it.code.contains(searchQuery, ignoreCase = true) }
            }
            .sortedByDescending { it.startTime }
    }

    // Poll the server only while Dashboard is visible (stops on logout / other screens)
    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Evren House",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${uiState.currentShiftUser ?: "Kasir"} · ${ShiftDateUtil.getShiftDateFromNow()}",
                            fontFamily = KasirMono,
                            style = MaterialTheme.typography.labelSmall,
                            color = KasirTextLow
                        )
                    }
                },
                actions = {
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
                            text = if (uiState.isSyncing) "SYNC" else if (uiState.apiConnected) "ONLINE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = KasirMono,
                            color = if (uiState.apiConnected) KasirGreen else KasirAccent
                        )
                    }
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Data")
                    }
                    if (uiState.currentUserRole == "admin") {
                        IconButton(onClick = {
                            viewModel.loadDeletionLogs()
                            navController.navigate(NavRoutes.DELETION_LOGS)
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Log Hapus")
                        }
                    }
                    IconButton(onClick = { navController.navigate(NavRoutes.PRINTER) }) {
                        Icon(Icons.Filled.Print, contentDescription = "Printer")
                    }
                    IconButton(onClick = { navController.navigate(NavRoutes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
            )
        },
        bottomBar = {
            KasirBottomBar(
                selectedTab = uiState.activeTab,
                onSelectTab = { tab ->
                    viewModel.setTab(tab)
                    if (tab == "history") {
                        navController.navigate(NavRoutes.POS) { launchSingleTop = true }
                    }
                }
            )
        },
        containerColor = KasirSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Unified layout: "Sewa Baru" + "Sesi Aktif" tabs on all screen sizes.
            TabRow(
                    selectedTabIndex = mobileSelectedTab,
                    containerColor = KasirSurfaceVariant,
                    contentColor = KasirGreen
                ) {
                    Tab(
                        selected = mobileSelectedTab == 0,
                        onClick = { mobileSelectedTab = 0 },
                        text = { Text("Sewa Baru", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = mobileSelectedTab == 1,
                        onClick = { mobileSelectedTab = 1 },
                        text = { Text("Sesi Aktif (${filteredSessions.size})", fontWeight = FontWeight.Bold) }
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
                            onShowQR = { viewModel.activeQrSession.value = it },
                            onPrint = { viewModel.printSessionReceipt(it) },
                            columnCount = sessionColumns
                        )
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

    // Admin PIN verification dialog (delete txn / clear history / edit session gate)
    AdminPinDialog(viewModel = viewModel)

    // Edit active session dialog (opens after admin PIN verified)
    activeEditSession?.let { session ->
        EditActiveSessionDialog(
            session = session,
            viewModel = viewModel,
            onClose = { viewModel.activeEditSession.value = null }
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
        val pressCash = rememberPressScale()
        val pressQris = rememberPressScale()
        val pressStart = rememberPressScale()
        Text("Sewa Baru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KasirGreen)
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
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onPayAwalChange("cash") },
                interactionSource = pressCash.interactionSource,
                modifier = pressCash.modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (payAwal == "cash") KasirCash else KasirLine),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (payAwal == "cash") KasirCash.copy(alpha = 0.15f) else KasirSurfaceCard
                )
            ) {
                Icon(
                    Icons.Filled.Payments,
                    contentDescription = null,
                    tint = if (payAwal == "cash") KasirCash else KasirOnSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Cash",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (payAwal == "cash") KasirCash else KasirOnSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = { onPayAwalChange("qris") },
                interactionSource = pressQris.interactionSource,
                modifier = pressQris.modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (payAwal == "qris") KasirQris else KasirLine),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (payAwal == "qris") KasirQris.copy(alpha = 0.15f) else KasirSurfaceCard
                )
            ) {
                Icon(
                    Icons.Filled.QrCode,
                    contentDescription = null,
                    tint = if (payAwal == "qris") KasirQris else KasirOnSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "QRIS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (payAwal == "qris") KasirQris else KasirOnSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Pilih Item & Jumlah", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
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
            interactionSource = pressStart.interactionSource,
            modifier = pressStart.modifier.fillMaxWidth().height(46.dp),
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
    onShowQR: (SessionDto) -> Unit,
    onPrint: (SessionDto) -> Unit,
    columnCount: Int = 1
) {
    // Single shared ticker drives every session card's timer (one coroutine,
    // not one per card).
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sesi Aktif (${filteredSessions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
        } else if (columnCount > 1) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    ActiveSessionCard(
                        session = session,
                        now = now,
                        onSelesai = { onSelesai(session) },
                        onShowQR = { onShowQR(session) },
                        onPrint = { onPrint(session) }
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredSessions, key = { it.id }) { session ->
                    ActiveSessionCard(
                        session = session,
                        now = now,
                        onSelesai = { onSelesai(session) },
                        onShowQR = { onShowQR(session) },
                        onPrint = { onPrint(session) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCatalogCard(
    item: CatalogItem,
    qty: Int,
    idrFormat: NumberFormat,
    onQuantityChange: (Int) -> Unit
) {
    val selected = qty > 0
    val pressMinus = rememberPressScale()
    val pressPlus = rememberPressScale()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) KasirGreen.copy(alpha = 0.12f) else KasirSurfaceCard,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) KasirGreen.copy(alpha = 0.6f) else KasirLine
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            // Full-bleed item image as the card background, dimmed so the
            // overlaid text stays readable.
            AsyncImage(
                model = item.defaultImg,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.35f,
                modifier = Modifier.matchParentSize()
            )
            // Subtle scrim to lift text contrast over the image
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(KasirSurfaceCard.copy(alpha = 0.30f))
            )
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.code,
                fontFamily = KasirMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selected) KasirGreen else KasirOnSurfaceVariant
            )
            Text(item.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = KasirOnSurface)
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (item.isPackage) "Paket ${item.packageHours}j" else "/ jam",
                style = MaterialTheme.typography.labelSmall,
                color = KasirTextLow
            )
            Text(
                text = idrFormat.format(item.priceHour),
                fontFamily = KasirMono,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = KasirGreen
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                // Decrease — left half of the card itself
                Surface(
                    onClick = { onQuantityChange(-1) },
                    enabled = qty > 0,
                    color = if (qty > 0) KasirSurfaceVariant else KasirSurfaceVariant.copy(alpha = 0.45f),
                    interactionSource = pressMinus.interactionSource,
                    modifier = pressMinus.modifier.weight(1f).fillMaxHeight()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "−",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (qty > 0) KasirOnSurfaceVariant else KasirTextLow
                        )
                    }
                }
                // Quantity (center divider)
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight()
                        .background(KasirSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$qty",
                        fontFamily = KasirMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (qty > 0) KasirGreen else KasirOnSurfaceVariant
                    )
                }
                // Increase — right half of the card itself
                Surface(
                    onClick = { onQuantityChange(1) },
                    color = KasirGreen,
                    interactionSource = pressPlus.interactionSource,
                    modifier = pressPlus.modifier.weight(1f).fillMaxHeight()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "+",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            }
            if (selected) {
                Surface(
                    color = KasirGreen,
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp).padding(3.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionCard(
    session: SessionDto,
    now: Long,
    onSelesai: () -> Unit,
    onShowQR: () -> Unit,
    onPrint: () -> Unit
) {
    val safeStart = if (session.startTime > 1577836800000L) session.startTime else now
    val elapsedSec = ((now - safeStart) / 1000).coerceAtLeast(0)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val pressSelesai = rememberPressScale()
    val elapsedMin = elapsedSec / 60
    val isZombie = elapsedSec > 28800 // > 8h

    // Status → one clear color signal: normal (teal) → grace (amber) → overtime (red) → zombie (red + tag)
    val statusColor = when {
        isZombie -> KasirError
        elapsedMin >= 71 -> KasirError
        elapsedMin >= 60 -> KasirAccent
        else -> KasirGreen
    }
    val statusLabel = when {
        isZombie -> "ZOMBIE"
        elapsedMin >= 71 -> "OVERTIME"
        elapsedMin >= 60 -> "GRACE"
        else -> "NORMAL"
    }
    val urgency = (elapsedMin.toFloat() / 60f).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = KasirSurfaceCard,
        border = BorderStroke(1.dp, KasirLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    // Ticket-style queue number
                    Surface(
                        color = KasirAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, KasirAccent.copy(alpha = 0.35f))
                    ) {
                        Text(
                            "NO. ${session.queueNo.toString().padStart(3, '0')}",
                            fontFamily = KasirMono,
                            fontWeight = FontWeight.Bold,
                            color = KasirAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(session.nama, fontWeight = FontWeight.Bold, color = KasirOnSurface, maxLines = 1)
                    if (isZombie) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = KasirError.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text("⚠ ZOMBIE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = KasirError, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                val payColor = if (session.payAwal == "qris") KasirQris else KasirCash
                Surface(
                    color = payColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, payColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        session.payAwal.uppercase(),
                        fontFamily = KasirMono,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = payColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                session.items.forEach { item ->
                    Surface(
                        color = KasirSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, KasirLine)
                    ) {
                        Text(
                            "${item.code}×${item.qty}",
                            fontFamily = KasirMono,
                            fontSize = 11.sp,
                            color = KasirOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Signature: the live rental timer ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "MULAI ${timeFormat.format(Date(safeStart))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = KasirTextLow,
                        fontFamily = KasirMono,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = String.format("%02d:%02d", elapsedSec / 3600, (elapsedSec % 3600) / 60),
                        fontFamily = KasirMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = 1.sp,
                        color = statusColor
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        statusLabel,
                        fontFamily = KasirMono,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Urgency bar — how much of the free 60-minute window is spent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(KasirSurfaceVariant, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(urgency)
                        .height(3.dp)
                        .background(statusColor, RoundedCornerShape(2.dp))
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelesai,
                    interactionSource = pressSelesai.interactionSource,
                    modifier = pressSelesai.modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isZombie || elapsedMin >= 71) KasirError else KasirGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text("Selesai & Bayar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = KasirSurfaceVariant,
                    border = BorderStroke(1.dp, KasirLine),
                    modifier = Modifier.size(42.dp),
                    onClick = onPrint
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Print, contentDescription = "Print Struk", tint = KasirOnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = KasirSurfaceVariant,
                    border = BorderStroke(1.dp, KasirLine),
                    modifier = Modifier.size(42.dp),
                    onClick = onShowQR
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.QrCode, contentDescription = "QR Code", tint = KasirOnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
