package com.kasir.mobile.ui.screen.history

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kasir.mobile.data.model.TransactionDto
import com.kasir.mobile.ui.navigation.KasirResponsiveScaffold
import com.kasir.mobile.domain.usecase.ShiftDateUtil
import com.kasir.mobile.ui.theme.*
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }

    var searchQuery by remember { mutableStateOf("") }

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

    val summaryData = remember(filteredTxns) {
        var sumCash = 0.0
        var sumQris = 0.0
        var sumGrand = 0.0
        filteredTxns.forEach { t ->
            val pokokCash = if (t.payAwal == "cash") t.totalBase else 0.0
            val pokokQris = if (t.payAwal == "qris") t.totalBase else 0.0
            sumCash += (pokokCash + t.cash)
            sumQris += (pokokQris + t.qris)
            sumGrand += t.totalAll
        }
        Triple(sumGrand, sumCash, sumQris)
    }
    val grandTotal = summaryData.first
    val totalCash = summaryData.second
    val totalQris = summaryData.third

    KasirResponsiveScaffold(
        isLandscape = isLandscape,
        selectedTab = uiState.activeTab,
        onSelectTab = { tab ->
            viewModel.setTab(tab)
            if (tab == "dashboard") {
                navController.popBackStack()
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Riwayat Transaksi", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = if (isAdmin) "ADMIN · ${uiState.currentShiftUser ?: "Admin"}"
                            else "KASIR · ${uiState.currentShiftUser ?: "-"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = KasirMono,
                            letterSpacing = 1.sp,
                            color = if (isAdmin) KasirPrimary else KasirTextLow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Riwayat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
            )
        },
        containerColor = KasirSurface,
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
            // Hero KPI Metrics Summary Card (Grand Total, Cash Fisik, QRIS)
            HistoryMetricsSummaryCard(
                grandTotal = grandTotal,
                totalCash = totalCash,
                totalQris = totalQris,
                txnCount = filteredTxns.size,
                idrFormat = idrFormat
            )
            Spacer(Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama penyewa atau item...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirPrimary),
                shape = RoundedCornerShape(12.dp)
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
                            onPrint = { viewModel.printTransactionReceipt(txn) }
                        )
                    }
                }
            }
        }
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
                    tint = KasirPrimary,
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
                        "Mode Kasir — riwayat shift berjalan",
                        style = MaterialTheme.typography.labelSmall,
                        color = KasirTextLow
                    )
                }
                Spacer(Modifier.width(8.dp))
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
    }
}

@Composable
private fun HistoryMetricsSummaryCard(
    grandTotal: Double,
    totalCash: Double,
    totalQris: Double,
    txnCount: Int,
    idrFormat: NumberFormat
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = KasirSurfaceCard,
        border = BorderStroke(1.dp, KasirLine.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Grand Total Hero + Txn Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL OMSET",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        color = KasirTextLow,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = idrFormat.format(grandTotal),
                        fontFamily = KasirMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = KasirPrimary
                    )
                }
                Surface(
                    color = KasirSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, KasirLine.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "$txnCount Transaksi",
                        fontFamily = KasirMono,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = KasirOnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = KasirLine.copy(alpha = 0.4f))
            Spacer(Modifier.height(14.dp))

            // Row 2: Cash Fisik & QRIS Pillars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cash Pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = KasirCashContainer,
                    border = BorderStroke(1.dp, KasirCash.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(KasirCash.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Payments,
                                contentDescription = null,
                                tint = KasirCash,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CASH FISIK",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.5.sp,
                                color = KasirCash,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = idrFormat.format(totalCash),
                                fontFamily = KasirMono,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = KasirOnSurface
                            )
                        }
                    }
                }

                // QRIS Pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = KasirQrisContainer,
                    border = BorderStroke(1.dp, KasirQris.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(KasirQris.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.QrCode,
                                contentDescription = null,
                                tint = KasirQris,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "QRIS",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.5.sp,
                                color = KasirQris,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = idrFormat.format(totalQris),
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

// SummaryCard replaced by HistoryMetricsSummaryCard

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
        Text(label, style = MaterialTheme.typography.bodySmall, color = KasirOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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
    onPrint: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val isCash = txn.payAwal == "cash"
    val pokokCash = if (isCash) txn.totalBase else 0.0
    val pokokQris = if (isCash) 0.0 else txn.totalBase
    val totalCash = pokokCash + txn.cash
    val totalQris = pokokQris + txn.qris
    val isSplit = totalCash > 0 && totalQris > 0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = KasirSurfaceCard,
        border = BorderStroke(1.dp, KasirLine.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: #001 · Nama · Shift · Tanggal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        color = KasirPrimaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, KasirPrimary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            "#${txn.no.toString().padStart(3, '0')}",
                            fontFamily = KasirMono,
                            fontWeight = FontWeight.Bold,
                            color = KasirPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        txn.nama,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = KasirSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
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
                Text(
                    txn.tanggal,
                    fontFamily = KasirMono,
                    style = MaterialTheme.typography.labelSmall,
                    color = KasirTextLow
                )
            }

            Spacer(Modifier.height(6.dp))

            // Jam rental: mulai → selesai
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
                    color = KasirPrimary
                )
            }

            Spacer(Modifier.height(4.dp))
            Text("Item: ${txn.items}", style = MaterialTheme.typography.bodySmall, color = KasirOnSurfaceVariant)

            // Overtime row: sembunyikan jika tidak ada overtime!
            if (txn.ot != "-" && txn.ot.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    "Overtime: ${txn.ot} · ${txn.otDur}",
                    fontFamily = KasirMono,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = KasirAccent
                )
            }

            // Sembunyikan breakdown jika hanya 1 metode dan tidak ada pecahan extra
            if (isSplit || (txn.cash > 0 && isCash) || (txn.qris > 0 && !isCash)) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = KasirLine.copy(alpha = 0.4f))
                Spacer(Modifier.height(6.dp))

                if (totalCash > 0) {
                    BreakdownRow(
                        label = if (isSplit) "Total Cash" else "Cash",
                        value = idrFormat.format(totalCash),
                        valueColor = KasirCash
                    )
                }
                if (totalQris > 0) {
                    BreakdownRow(
                        label = if (isSplit) "Total QRIS" else "QRIS",
                        value = idrFormat.format(totalQris),
                        valueColor = KasirQris
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = KasirLine.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            // Footer kompak: Payment Badge + Grand Total + Print Icon dalam 1 baris
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge tipe bayar
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isSplit -> KasirAccentContainer
                            totalCash > 0 -> KasirCashContainer
                            else -> KasirQrisContainer
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isSplit -> KasirAccent.copy(alpha = 0.4f)
                                totalCash > 0 -> KasirCash.copy(alpha = 0.4f)
                                else -> KasirQris.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Text(
                            text = when {
                                isSplit -> "SPLIT"
                                totalCash > 0 -> "CASH"
                                else -> "QRIS"
                            },
                            fontFamily = KasirMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = when {
                                isSplit -> KasirAccent
                                totalCash > 0 -> KasirCash
                                else -> KasirQris
                            },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = KasirTextLow
                        )
                        Text(
                            text = idrFormat.format(txn.totalAll),
                            fontFamily = KasirMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = KasirOnSurface
                        )
                    }
                }

                IconButton(
                    onClick = onPrint,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Filled.Print,
                        contentDescription = "Print Ulang Struk",
                        tint = KasirPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
