package com.kasir.mobile.ui.screen.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kasir.mobile.data.model.TransactionDto
import com.kasir.mobile.ui.navigation.KasirBottomBar
import com.kasir.mobile.domain.usecase.ShiftDateUtil
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirCash
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirLine
import com.kasir.mobile.ui.theme.KasirMono
import com.kasir.mobile.ui.theme.KasirOnSurfaceVariant
import com.kasir.mobile.ui.theme.KasirQris
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.theme.KasirTextLow
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: KasirViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdmin = uiState.currentUserRole == "admin"
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    var searchQuery by remember { mutableStateOf("") }
    var txnToDelete by remember { mutableStateOf<TransactionDto?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Admin-only filter state
    var filterMode by remember { mutableStateOf("daily") } // "daily" | "monthly" | "yearly"
    var filterDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("desc") } // "desc" = terbaru, "asc" = terlama

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val msg = uiState.errorMessage ?: uiState.successMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    // Fetch fresh transactions when History opens (polling is Dashboard-scoped).
    LaunchedEffect(Unit) {
        viewModel.loadData(showSync = false)
    }

    // Cashier is locked to "today" (shift date) and read-only; admin gets full filters.
    val effectiveValue =
        if (isAdmin) formatFilterValue(filterDateMillis, filterMode)
        else ShiftDateUtil.getShiftDateFromNow()

    val filteredTxns = remember(uiState.transactions, effectiveValue, sortOrder, searchQuery) {
        val filtered = uiState.transactions.filter { t ->
            matchesPeriod(t, effectiveValue) &&
                (t.nama.contains(searchQuery, ignoreCase = true) ||
                    t.items.contains(searchQuery, ignoreCase = true))
        }
        if (sortOrder == "asc") filtered.sortedBy { it.endTime }
        else filtered.sortedByDescending { it.endTime }
    }

    val totals = remember(filteredTxns) {
        Triple(
            filteredTxns.sumOf { it.totalBase },
            filteredTxns.sumOf { it.totalOT },
            filteredTxns.sumOf { it.totalAll }
        )
    }
    val totalPokok = totals.first
    val totalOT = totals.second
    val grandTotal = totals.third

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Riwayat Transaksi", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isAdmin) "ADMIN · ${uiState.currentShiftUser ?: "Admin"}"
                            else "KASIR · ${uiState.currentShiftUser ?: "-"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = KasirMono,
                            letterSpacing = 1.sp,
                            color = if (isAdmin) KasirGreen else KasirTextLow
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Riwayat")
                    }
                    if (isAdmin) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(
                                Icons.Filled.DeleteForever,
                                contentDescription = "Bersihkan Semua",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
            )
        },
        containerColor = KasirSurface,
        bottomBar = {
            KasirBottomBar(
                selectedTab = uiState.activeTab,
                onSelectTab = { tab ->
                    viewModel.setTab(tab)
                    if (tab == "dashboard") {
                        navController.popBackStack()
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            HistoryFilterBar(
                isAdmin = isAdmin,
                filterMode = filterMode,
                filterValue = effectiveValue,
                sortOrder = sortOrder,
                onModeChange = { newMode -> filterMode = newMode },
                onPickDate = { showDatePicker = true },
                onToggleSort = { sortOrder = if (sortOrder == "desc") "asc" else "desc" }
            )

            Spacer(Modifier.height(12.dp))

            // Summary Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard("Transaksi", "${filteredTxns.size}", Modifier.weight(1f))
                SummaryCard("Total Pokok", idrFormat.format(totalPokok), Modifier.weight(1.2f))
                SummaryCard("Total Overtime", idrFormat.format(totalOT), Modifier.weight(1.2f))
                SummaryCard("Grand Total", idrFormat.format(grandTotal), Modifier.weight(1.4f), highlight = true)
            }

            Spacer(Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama penyewa atau item...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirGreen)
            )

            Spacer(Modifier.height(12.dp))

            if (filteredTxns.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isAdmin) "Tidak ada transaksi pada periode ini"
                            else "Belum ada transaksi hari ini",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredTxns, key = { it.id }) { txn ->
                        TransactionCard(
                            txn = txn,
                            idrFormat = idrFormat,
                            canDelete = isAdmin,
                            onDelete = { txnToDelete = txn },
                            onPrint = { viewModel.printTransactionReceipt(txn) }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation (admin only)
    txnToDelete?.let { txn ->
        AlertDialog(
            onDismissRequest = { txnToDelete = null },
            title = { Text("Hapus Transaksi?") },
            text = {
                Text("Bill #${txn.no} atas nama \"${txn.nama}\" (${idrFormat.format(txn.totalAll)}) akan dihapus permanen.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(txn)
                        txnToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { txnToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Clear-all confirmation (admin only)
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Bersihkan Semua Riwayat?") },
            text = {
                Text("Seluruh ${uiState.transactions.size} transaksi akan dihapus permanen. Tindakan ini tidak bisa dibatalkan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("Bersihkan", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Date picker (admin only)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = filterDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { filterDateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Port of kasir-db aggregateHistory period filter: value is a YYYY[-MM[-DD]] prefix. */
private fun matchesPeriod(t: TransactionDto, value: String): Boolean {
    if (value.isBlank()) return true
    if (t.tanggal.startsWith(value)) return true
    if (t.startTime > 0 && ShiftDateUtil.getShiftDate(t.startTime).startsWith(value)) return true
    if (t.endTime > 0 && ShiftDateUtil.getShiftDate(t.endTime).startsWith(value)) return true
    return false
}

private fun formatFilterValue(millis: Long, mode: String): String {
    val pattern = when (mode) {
        "monthly" -> "yyyy-MM"
        "yearly" -> "yyyy"
        else -> "yyyy-MM-dd"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

/** Port of kasir-db HistoryTab shift code mapping. */
private val SHIFT_CODE_MAP = mapOf(
    "Akbar" to "AK", "Rani" to "RN", "Monica" to "MO", "Aldy" to "AL",
    "Wahyu" to "WH", "Donny" to "DN", "Zumi" to "ZM", "Awang" to "AW"
)

private fun shiftCode(n: String?): String {
    if (n.isNullOrBlank() || n == "-") return "-"
    SHIFT_CODE_MAP[n]?.let { return it }
    val k = SHIFT_CODE_MAP.keys.firstOrNull { it.equals(n, ignoreCase = true) }
    return k?.let { SHIFT_CODE_MAP[it]!! } ?: n.take(2).uppercase()
}

private fun formatTimeMillis(millis: Long, fmt: SimpleDateFormat): String =
    if (millis > 0) fmt.format(Date(millis)) else "-"

@Composable
private fun HistoryFilterBar(
    isAdmin: Boolean,
    filterMode: String,
    filterValue: String,
    sortOrder: String,
    onModeChange: (String) -> Unit,
    onPickDate: () -> Unit,
    onToggleSort: () -> Unit
) {
    if (isAdmin) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterMode == "daily",
                    onClick = { onModeChange("daily") },
                    label = { Text("Harian") }
                )
                FilterChip(
                    selected = filterMode == "monthly",
                    onClick = { onModeChange("monthly") },
                    label = { Text("Bulanan") }
                )
                FilterChip(
                    selected = filterMode == "yearly",
                    onClick = { onModeChange("yearly") },
                    label = { Text("Tahunan") }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onPickDate) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(filterValue, fontFamily = KasirMono, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick = onToggleSort,
                    label = {
                        Text(
                            if (sortOrder == "desc") "Terbaru" else "Terlama",
                            fontFamily = KasirMono,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (sortOrder == "desc") Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    } else {
        // Cashier mode banner: today-only, read-only
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = KasirSurfaceCard,
            border = BorderStroke(1.dp, KasirLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = KasirGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Hari Ini · $filterValue",
                        fontFamily = KasirMono,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Mode Kasir — hanya lihat riwayat hari ini",
                        style = MaterialTheme.typography.labelSmall,
                        color = KasirTextLow
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) KasirGreen.copy(alpha = 0.12f) else KasirSurfaceCard,
        border = BorderStroke(
            1.dp,
            if (highlight) KasirGreen.copy(alpha = 0.4f) else KasirLine
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                color = KasirTextLow
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontFamily = KasirMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (highlight) KasirGreen else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    valueColor: Color,
    bold: Boolean = false,
    large: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = KasirOnSurfaceVariant)
        Text(
            value,
            fontFamily = KasirMono,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (large) 16.sp else 13.sp,
            color = valueColor
        )
    }
}

@Composable
fun TransactionCard(
    txn: TransactionDto,
    idrFormat: NumberFormat,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onPrint: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val isCash = txn.payAwal == "cash"
    val pokokCash = if (isCash) txn.totalBase else 0.0
    val pokokQris = if (isCash) 0.0 else txn.totalBase
    val cashExtra = txn.cash > 0
    val qrisExtra = txn.qris > 0
    val totalCash = pokokCash + txn.cash
    val totalQris = pokokQris + txn.qris

    Surface(
        shape = RoundedCornerShape(14.dp),
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
                    Surface(
                        color = KasirGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, KasirGreen.copy(alpha = 0.35f))
                    ) {
                        Text(
                            "#${txn.no.toString().padStart(3, '0')}",
                            fontFamily = KasirMono,
                            fontWeight = FontWeight.Bold,
                            color = KasirGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(txn.nama, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    Surface(color = KasirSurfaceVariant, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            shiftCode(txn.shift),
                            fontFamily = KasirMono,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = KasirOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(txn.tanggal, fontFamily = KasirMono, style = MaterialTheme.typography.labelSmall, color = KasirTextLow)
            }

            Spacer(Modifier.height(6.dp))
            // Waktu close bill (mulai → selesai)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTimeMillis(txn.startTime, timeFmt),
                    fontFamily = KasirMono,
                    style = MaterialTheme.typography.bodySmall,
                    color = KasirOnSurfaceVariant
                )
                Text(" → ", color = KasirTextLow, style = MaterialTheme.typography.bodySmall)
                Text(
                    formatTimeMillis(txn.endTime, timeFmt),
                    fontFamily = KasirMono,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = KasirQris
                )
            }

            Spacer(Modifier.height(4.dp))
            Text("Item: ${txn.items}", style = MaterialTheme.typography.bodySmall, color = KasirOnSurfaceVariant)

            if (txn.ot != "-" && txn.ot.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "OT: ${txn.ot}  ·  ${txn.otDur}",
                    fontFamily = KasirMono,
                    style = MaterialTheme.typography.bodySmall,
                    color = KasirAccent
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Spacer(Modifier.height(8.dp))

            // Payment breakdown — mirrors kasir-db HistoryTab columns
            BreakdownRow("Pokok (Cash)", if (isCash) idrFormat.format(pokokCash) else "—", KasirCash)
            BreakdownRow("Pokok (QRIS)", if (!isCash) idrFormat.format(pokokQris) else "—", KasirQris)
            BreakdownRow("Tambahan (Cash)", if (cashExtra) idrFormat.format(txn.cash) else "—", KasirCash)
            BreakdownRow("Tambahan (QRIS)", if (qrisExtra) idrFormat.format(txn.qris) else "—", KasirQris)
            BreakdownRow("Total Cash", idrFormat.format(totalCash), KasirCash, bold = true)
            BreakdownRow("Total QRIS", idrFormat.format(totalQris), KasirQris, bold = true)

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Spacer(Modifier.height(6.dp))

            BreakdownRow("Grand Total", idrFormat.format(txn.totalAll), KasirAccent, bold = true, large = true)

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrint,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Print,
                        contentDescription = "Print Ulang Struk",
                        tint = KasirGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Hapus Transaksi",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
