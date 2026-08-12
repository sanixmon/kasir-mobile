package com.kasir.mobile.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kasir.mobile.data.model.ItemCatalog
import com.kasir.mobile.data.model.ItemDto
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.viewmodel.KasirViewModel

/**
 * Mirrors kasir-db EditActiveSessionModal: update renter name, pay method and
 * item quantities of an active session, then persist via edit_session.
 */
@Composable
fun EditActiveSessionDialog(
    session: SessionDto,
    viewModel: KasirViewModel,
    onClose: () -> Unit
) {
    var nama by remember(session.id) { mutableStateOf(session.nama) }
    var payAwal by remember(session.id) { mutableStateOf(session.payAwal) }
    var editItems by remember(session.id) { mutableStateOf(session.items.map { it.copy() }) }

    val changeQty = { code: String, delta: Int ->
        val idx = editItems.indexOfFirst { it.code == code }
        editItems = if (idx >= 0) {
            val updated = editItems.toMutableList()
            val newQty = (updated[idx].qty + delta).coerceAtLeast(0)
            if (newQty == 0) {
                updated.removeAt(idx)
                updated
            } else {
                updated[idx] = updated[idx].copy(qty = newQty)
                updated
            }
        } else if (delta > 0) {
            editItems + ItemDto(code = code, qty = 1)
        } else {
            editItems
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KasirSurfaceCard,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = KasirGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Sesi Aktif", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Penyewa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirGreen)
                )

                Spacer(Modifier.height(12.dp))

                Text("Metode Bayar Pokok", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = payAwal == "cash",
                        onClick = { payAwal = "cash" },
                        label = { Text("Cash") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KasirGreen)
                    )
                    FilterChip(
                        selected = payAwal == "qris",
                        onClick = { payAwal = "qris" },
                        label = { Text("QRIS") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KasirGreen)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text("Item & Jumlah", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))

                ItemCatalog.ITEMS.forEach { item ->
                    val qty = editItems.firstOrNull { it.code == item.code }?.qty ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.code, style = MaterialTheme.typography.labelSmall, color = KasirGreen, fontWeight = FontWeight.Bold)
                            Text(item.name, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { changeQty(item.code, -1) }, modifier = Modifier.size(30.dp)) {
                                Text("−", fontWeight = FontWeight.Bold)
                            }
                            Text("$qty", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { changeQty(item.code, 1) }, modifier = Modifier.size(30.dp)) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val trimmed = nama.trim()
                            if (trimmed.isNotBlank() && editItems.isNotEmpty()) {
                                viewModel.saveEditedSession(
                                    session.copy(
                                        nama = trimmed,
                                        payAwal = payAwal,
                                        items = editItems
                                    )
                                )
                            }
                        },
                        enabled = nama.isNotBlank() && editItems.isNotEmpty(),
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
