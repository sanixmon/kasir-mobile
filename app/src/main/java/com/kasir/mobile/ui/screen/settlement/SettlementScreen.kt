package com.kasir.mobile.ui.screen.settlement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kasir.mobile.data.model.ItemCatalog
import com.kasir.mobile.data.model.TransactionDto
import com.kasir.mobile.domain.usecase.ShiftDateUtil
import com.kasir.mobile.ui.navigation.KasirResponsiveScaffold
import com.kasir.mobile.ui.navigation.NavRoutes
import com.kasir.mobile.ui.theme.*
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import java.text.NumberFormat
import java.util.Locale

data class AggregatedRentalItem(
    val code: String,
    val name: String,
    val emoji: String,
    val totalQty: Int,
    val estimatedRevenue: Double
)

fun parseAggregatedItems(txns: List<TransactionDto>): List<AggregatedRentalItem> {
    val map = mutableMapOf<String, Int>()
    txns.forEach { txn ->
        if (txn.items.isNotBlank() && txn.items != "-") {
            txn.items.split(",").forEach { rawPart ->
                val part = rawPart.trim()
                if (part.isNotBlank()) {
                    val code = part.substringBefore("×").substringBefore("x").trim()
                    val qty = part.substringAfter("×").substringAfter("x").trim().toIntOrNull() ?: 1
                    if (code.isNotBlank()) {
                        map[code] = (map[code] ?: 0) + qty
                    }
                }
            }
        }
    }
    return map.entries.map { (code, qty) ->
        val def = ItemCatalog.findByCode(code)
        val name = def?.name ?: code
        val emoji = def?.emoji ?: "📦"
        val unitPrice = def?.priceHour ?: 0.0
        AggregatedRentalItem(
            code = code,
            name = name,
            emoji = emoji,
            totalQty = qty,
            estimatedRevenue = unitPrice * qty
        )
    }.sortedByDescending { it.totalQty }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    navController: NavController,
    viewModel: KasirViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val isAdmin = uiState.currentUserRole == "admin"
    val todayShift = remember { ShiftDateUtil.getShiftDateFromNow() }

    val idrFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    // Filter transactions for current cashier shift
    val shiftTxns = remember(uiState.transactions, uiState.currentShiftUser, todayShift, isAdmin) {
        uiState.transactions.filter { t ->
            val matchesUser = if (isAdmin) true else t.shift.equals(uiState.currentShiftUser, ignoreCase = true)
            val matchesDate = t.tanggal == todayShift || (t.endTime > 0 && ShiftDateUtil.getShiftDate(t.endTime) == todayShift)
            matchesUser && matchesDate
        }
    }

    val totalRevenue = remember(shiftTxns) { shiftTxns.sumOf { it.totalAll } }
    val totalCash = remember(shiftTxns) { shiftTxns.sumOf { it.cash } }
    val totalQris = remember(shiftTxns) { shiftTxns.sumOf { it.qris } }
    val totalPokok = remember(shiftTxns) { shiftTxns.sumOf { it.totalBase } }
    val totalOt = remember(shiftTxns) { shiftTxns.sumOf { it.totalOT } }
    val totalTol = remember(shiftTxns) { shiftTxns.sumOf { it.totalTol } }
    val aggregatedItems = remember(shiftTxns) { parseAggregatedItems(shiftTxns) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }
    var closingFinished by remember { mutableStateOf(false) }
    var printErrorMessage by remember { mutableStateOf<String?>(null) }
    var isReprinting by remember { mutableStateOf(false) }

    KasirResponsiveScaffold(
        isLandscape = isLandscape,
        selectedTab = "settlement",
        onSelectTab = { tab ->
            viewModel.setTab(tab)
            when (tab) {
                "dashboard" -> navController.navigate(NavRoutes.DASHBOARD) { launchSingleTop = true }
                "history" -> navController.navigate(NavRoutes.POS) { launchSingleTop = true }
                "settlement" -> {}
            }
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (isAdmin) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke Menu Admin")
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Tutup Shift & Settlement",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${if (isAdmin) "ADMIN" else "KASIR"} · ${uiState.currentShiftUser ?: "-"} · $todayShift",
                            fontFamily = KasirMono,
                            style = MaterialTheme.typography.labelSmall,
                            color = KasirTextLow
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Data")
                    }
                    IconButton(onClick = { navController.navigate(NavRoutes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
            )
        },
        containerColor = KasirSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Active Session Guard Alert ──────────────────────────────────
            if (uiState.activeSessions.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KasirErrorContainer,
                    border = BorderStroke(1.dp, KasirError.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = KasirError,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Shift Belum Dapat Ditutup",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = KasirError
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Masih terdapat ${uiState.activeSessions.size} sesi sewa aktif yang berjalan di lapangan. Sesuai aturan operasional, seluruh unit wajib dikembalikan atau diselesaikan terlebih dahulu sebelum menutup shift.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KasirOnSurface
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.setTab("dashboard")
                                navController.navigate(NavRoutes.DASHBOARD) { launchSingleTop = true }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KasirError),
                            border = BorderStroke(1.dp, KasirError)
                        ) {
                            Text("Buka Sesi Aktif di Dashboard")
                        }
                    }
                }
            }

            // ── Shift Information Strip ──────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = KasirSurfaceCard,
                border = BorderStroke(1.dp, KasirLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OPERATOR SHIFT",
                            style = MaterialTheme.typography.labelSmall,
                            color = KasirTextLow,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = uiState.currentShiftUser ?: "Kasir",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KasirOnSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TANGGAL SHIFT",
                            style = MaterialTheme.typography.labelSmall,
                            color = KasirTextLow,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = todayShift,
                            fontFamily = KasirMono,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = KasirPrimary
                        )
                    }
                }
            }

            // ── Primary Financial Metric Cards ──────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KasirSurfaceVariant,
                border = BorderStroke(1.dp, KasirLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RINGKASAN PENERIMAAN SHIFT",
                        style = MaterialTheme.typography.labelSmall,
                        color = KasirTextLow,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = idrFormat.format(totalRevenue),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = KasirPrimary
                    )
                    Text(
                        text = "${shiftTxns.size} transaksi selesai dicatat pada shift ini",
                        style = MaterialTheme.typography.bodySmall,
                        color = KasirTextLow
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = KasirLine)
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Cash Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = KasirCashContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Payments,
                                        contentDescription = null,
                                        tint = KasirCash,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Tunai (Cash)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KasirCash
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = idrFormat.format(totalCash),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = KasirOnSurface
                                )
                            }
                        }

                        // Total QRIS Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = KasirQrisContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.QrCode,
                                        contentDescription = null,
                                        tint = KasirQris,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Non-Tunai QRIS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KasirQris
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = idrFormat.format(totalQris),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = KasirOnSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Secondary breakdown: Pokok vs Overtime vs Toleransi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Sewa Pokok", style = MaterialTheme.typography.labelSmall, color = KasirTextLow)
                            Text(idrFormat.format(totalPokok), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Overtime", style = MaterialTheme.typography.labelSmall, color = KasirTextLow)
                            Text(idrFormat.format(totalOt), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = KasirAccent)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Toleransi Kasir", style = MaterialTheme.typography.labelSmall, color = KasirTextLow)
                            Text(idrFormat.format(totalTol), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Aggregated Rental Products Breakdown ─────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KasirSurfaceCard,
                border = BorderStroke(1.dp, KasirLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RINGKASAN UNIT SEWA TERJUAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = KasirTextLow,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${aggregatedItems.sumOf { it.totalQty }} Total Unit",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = KasirMono,
                            fontWeight = FontWeight.Bold,
                            color = KasirPrimary
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    if (aggregatedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada transaksi sewa selesai pada shift ini",
                                style = MaterialTheme.typography.bodySmall,
                                color = KasirTextLow
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            aggregatedItems.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = KasirSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(item.emoji, fontSize = 20.sp)
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = KasirOnSurface
                                                )
                                                Text(
                                                    text = "${item.totalQty} kali sewa",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = KasirTextLow
                                                )
                                            }
                                        }
                                        Text(
                                            text = idrFormat.format(item.estimatedRevenue),
                                            fontFamily = KasirMono,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = KasirOnSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Primary Action: Tutup Shift ──────────────────────────────────
            val canClose = uiState.activeSessions.isEmpty()

            Button(
                onClick = { showConfirmDialog = true },
                enabled = canClose && !isAdmin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KasirPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = KasirLine,
                    disabledContentColor = KasirTextLow
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isAdmin) "Tutup Shift (Hanya Akun Kasir)" else "Tutup Shift & Cetak Struk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Dialog Konfirmasi Tutup Shift ────────────────────────────────────────
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClosing) showConfirmDialog = false },
            title = {
                Text(
                    text = "Konfirmasi Tutup Shift?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Shift operator \"${uiState.currentShiftUser}\" akan ditutup secara permanen.")
                    Spacer(Modifier.height(8.dp))
                    Text("Ringkasan yang akan disimpan:")
                    Spacer(Modifier.height(4.dp))
                    Text("• Total Omzet: ${idrFormat.format(totalRevenue)}", fontWeight = FontWeight.SemiBold)
                    Text("• Penerimaan Tunai: ${idrFormat.format(totalCash)}")
                    Text("• Penerimaan QRIS: ${idrFormat.format(totalQris)}")
                    Text("• Transaksi Selesai: ${shiftTxns.size} transaksi")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Data settlement akan dicatat ke database server dan struk closing akan otomatis dikirim ke printer thermal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KasirTextLow
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isClosing = true
                        val itemsSummaryJson = aggregatedItems.joinToString(", ") { "${it.name} x${it.totalQty}" }
                        viewModel.submitSettlement(
                            shiftUser = uiState.currentShiftUser ?: "Kasir",
                            tanggal = todayShift,
                            totalTxns = shiftTxns.size,
                            revenue = totalRevenue,
                            cash = totalCash,
                            qris = totalQris,
                            itemsSummary = itemsSummaryJson
                        ) { submitResult ->
                            submitResult.onSuccess {
                                // Auto print thermal receipt
                                viewModel.printSettlementReceipt(
                                    shiftUser = uiState.currentShiftUser ?: "Kasir",
                                    tanggal = todayShift,
                                    totalTxns = shiftTxns.size,
                                    totalRevenue = totalRevenue.toLong(),
                                    totalCash = totalCash.toLong(),
                                    totalQris = totalQris.toLong(),
                                    totalPokok = totalPokok.toLong(),
                                    totalOvertime = totalOt.toLong()
                                ) { printResult ->
                                    isClosing = false
                                    closingFinished = true
                                    showConfirmDialog = false
                                    printResult.onFailure { err ->
                                        printErrorMessage = err.message ?: "Printer belum terhubung"
                                    }
                                }
                            }.onFailure { err ->
                                isClosing = false
                                viewModel.setErrorMessage("Gagal menyimpan settlement: ${err.message}")
                                showConfirmDialog = false
                            }
                        }
                    },
                    enabled = !isClosing,
                    colors = ButtonDefaults.buttonColors(containerColor = KasirPrimary)
                ) {
                    if (isClosing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Memproses...")
                    } else {
                        Text("Ya, Tutup Shift")
                    }
                }
            },
            dismissButton = {
                if (!isClosing) {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Batal")
                    }
                }
            }
        )
    }

    // ── Dialog Hasil Tutup Shift (Cetak Ulang / Selesai & Keluar) ────────────
    if (closingFinished) {
        AlertDialog(
            onDismissRequest = { /* Modal: require explicit button click */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (printErrorMessage == null) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (printErrorMessage == null) KasirGreen else KasirAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (printErrorMessage == null) "Tutup Shift Sukses!" else "Tutup Shift Tercatat",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text("Data settlement shift telah berhasil disimpan ke database server.")
                    Spacer(Modifier.height(6.dp))
                    if (printErrorMessage != null) {
                        Text(
                            text = "Peringatan printer: $printErrorMessage\nPastikan printer menyala dan terhubung melalui Bluetooth.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KasirAccent
                        )
                    } else {
                        Text(
                            text = "Struk closing thermal berhasil dikirim ke printer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KasirTextLow
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        closingFinished = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KasirPrimary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Selesai & Keluar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        isReprinting = true
                        viewModel.printSettlementReceipt(
                            shiftUser = uiState.currentShiftUser ?: "Kasir",
                            tanggal = todayShift,
                            totalTxns = shiftTxns.size,
                            totalRevenue = totalRevenue.toLong(),
                            totalCash = totalCash.toLong(),
                            totalQris = totalQris.toLong(),
                            totalPokok = totalPokok.toLong(),
                            totalOvertime = totalOt.toLong()
                        ) { res ->
                            isReprinting = false
                            if (res.isSuccess) {
                                printErrorMessage = null
                            } else {
                                printErrorMessage = res.exceptionOrNull()?.message ?: "Gagal cetak ulang"
                            }
                        }
                    },
                    enabled = !isReprinting
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isReprinting) "Mencetak..." else "Cetak Ulang Struk")
                }
            }
        )
    }
}
