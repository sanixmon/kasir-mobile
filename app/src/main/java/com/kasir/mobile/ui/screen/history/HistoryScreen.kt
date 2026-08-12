package com.kasir.mobile.ui.screen.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kasir.mobile.data.model.TransactionDto
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirLine
import com.kasir.mobile.ui.theme.KasirMono
import com.kasir.mobile.ui.theme.KasirOnSurfaceVariant
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.theme.KasirTextLow
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: KasirViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var txnToDelete by remember { mutableStateOf<TransactionDto?>(null) }
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val msg = uiState.errorMessage ?: uiState.successMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    val filteredTxns = remember(uiState.transactions, searchQuery) {
        uiState.transactions.filter {
            it.nama.contains(searchQuery, ignoreCase = true) || it.items.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalPokok = filteredTxns.sumOf { it.totalBase }
    val totalOT = filteredTxns.sumOf { it.totalOT }
    val grandTotal = filteredTxns.sumOf { it.totalAll }
    val totalCash = filteredTxns.sumOf { it.cash }
    val totalQris = filteredTxns.sumOf { it.qris }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📜 Riwayat Transaksi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (uiState.currentUserRole == "admin") {
                        IconButton(onClick = { viewModel.clearAllHistory() }) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = "Bersihkan Semua", tint = MaterialTheme.colorScheme.error)
                        }
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
                placeholder = { Text("Cari berdasarkan nama penyewa atau item...") },
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
                    Text("Belum ada riwayat transaksi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredTxns, key = { it.id }) { txn ->
                        TransactionCard(
                            txn = txn,
                            idrFormat = idrFormat,
                            onDelete = { txnToDelete = txn }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation (mirrors kasir-db swalConfirm)
    txnToDelete?.let { txn ->
        AlertDialog(
            onDismissRequest = { txnToDelete = null },
            title = { Text("Hapus Riwayat Transaksi?") },
            text = { Text("Bill atas nama \"${txn.nama}\" akan dihapus secara permanen.${if (uiState.currentUserRole != "admin") "\n\nKasir perlu verifikasi password admin." else ""}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(txn)
                        txnToDelete = null
                    }
                ) {
                    Text("Ya, Hapus!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { txnToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) KasirGreen.copy(alpha = 0.12f) else KasirSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(
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
fun TransactionCard(
    txn: TransactionDto,
    idrFormat: NumberFormat,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = KasirSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, KasirLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = KasirGreen.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp), border = androidx.compose.foundation.BorderStroke(1.dp, KasirGreen.copy(alpha = 0.35f))) {
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
                    Text(txn.nama, fontWeight = FontWeight.Bold)
                }
                Text(txn.tanggal, fontFamily = KasirMono, style = MaterialTheme.typography.labelSmall, color = KasirTextLow)
            }

            Spacer(Modifier.height(6.dp))
            Text("Items: ${txn.items}", style = MaterialTheme.typography.bodyMedium, color = KasirOnSurfaceVariant)

            if (txn.ot != "-") {
                Text("OT: ${txn.ot} (${txn.otDur})", fontFamily = KasirMono, style = MaterialTheme.typography.bodySmall, color = KasirAccent)
            }

            Spacer(Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.surface)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Pokok: ${idrFormat.format(txn.totalBase)} (${txn.payAwal.uppercase()})", style = MaterialTheme.typography.bodySmall, color = KasirOnSurfaceVariant)
                    if (txn.totalOT > 0) {
                        Text("Overtime: ${idrFormat.format(txn.totalOT)}", style = MaterialTheme.typography.bodySmall, color = KasirAccent)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${idrFormat.format(txn.totalAll)}",
                        fontFamily = KasirMono,
                        fontWeight = FontWeight.Bold,
                        color = KasirGreen,
                        fontSize = 16.sp
                    )
                    Text("SHIFT ${txn.shift}", fontFamily = KasirMono, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = KasirTextLow)
                }
            }

            Spacer(Modifier.height(4.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End).size(32.dp)
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
