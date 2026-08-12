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
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
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
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

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
        containerColor = KasirSurface
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
                            onDelete = { viewModel.deleteTransaction(txn) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (highlight) KasirGreen.copy(alpha = 0.2f) else KasirSurfaceCard,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
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
        shape = RoundedCornerShape(12.dp),
        color = KasirSurfaceCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = KasirGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "#${txn.no}",
                            fontWeight = FontWeight.Bold,
                            color = KasirGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(txn.nama, fontWeight = FontWeight.Bold)
                }
                Text(txn.tanggal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(6.dp))
            Text("Items: ${txn.items}", style = MaterialTheme.typography.bodyMedium)

            if (txn.ot != "-") {
                Text("OT: ${txn.ot} (${txn.otDur})", style = MaterialTheme.typography.bodySmall, color = KasirAccent)
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
                    Text("Pokok: ${idrFormat.format(txn.totalBase)} (${txn.payAwal.uppercase()})", style = MaterialTheme.typography.bodySmall)
                    if (txn.totalOT > 0) {
                        Text("Overtime: ${idrFormat.format(txn.totalOT)}", style = MaterialTheme.typography.bodySmall, color = KasirAccent)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TOTAL: ${idrFormat.format(txn.totalAll)}", fontWeight = FontWeight.Bold, color = KasirGreen, fontSize = 16.sp)
                    Text("Shift: ${txn.shift}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
