package com.kasir.mobile.ui.screen.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kasir.mobile.data.model.ItemCatalog
import com.kasir.mobile.data.model.ItemDto
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.ui.theme.*
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * AddItemDialog (Additional Order) - Mirrors kasir-db AddItemModal.jsx:
 * Allows cashiers to add rental items to an active session, collect upfront base
 * rental payment (Cash/QRIS), and print the *** ADDITIONAL ORDER *** receipt.
 * Supports responsive 2-column layout on landscape orientation.
 */
@Composable
fun AddItemDialog(
    session: SessionDto,
    viewModel: KasirViewModel,
    onClose: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }

    var selectedQty by remember(session.id) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var payAwal by remember(session.id) { mutableStateOf("cash") }
    var cashGiven by remember(session.id) { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val handleChgQty = { code: String, delta: Int ->
        val current = selectedQty[code] ?: 0
        val next = (current + delta).coerceAtLeast(0)
        selectedQty = selectedQty + (code to next)
    }

    val totalAdditional = ItemCatalog.ITEMS.sumOf { item ->
        val q = selectedQty[item.code] ?: 0
        q * item.priceHour
    }

    val cashNum = cashGiven.toDoubleOrNull() ?: 0.0
    val changeAmt = (cashNum - totalAdditional).coerceAtLeast(0.0)
    val canSave = totalAdditional > 0.0 && !isSubmitting

    val onSave = {
        if (canSave) {
            val itemsToAdd = ItemCatalog.ITEMS
                .filter { (selectedQty[it.code] ?: 0) > 0 }
                .map { item ->
                    ItemDto(
                        code = item.code,
                        qty = selectedQty[item.code] ?: 1,
                        priceBase = item.priceHour
                    )
                }
            if (itemsToAdd.isNotEmpty()) {
                isSubmitting = true
                viewModel.addItemsToRental(session, itemsToAdd, payAwal) { success ->
                    isSubmitting = false
                    if (success) onClose()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = !isLandscape)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KasirSurfaceCard,
            border = BorderStroke(1.dp, KasirLine),
            modifier = if (isLandscape) {
                Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.94f).padding(8.dp)
            } else {
                Modifier.fillMaxWidth().padding(8.dp)
            }
        ) {
            if (isLandscape) {
                // ── 2-Column Landscape Layout ──────────────────────────────────────────
                Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null, tint = KasirAccent)
                            Spacer(Modifier.width(8.dp))
                            Text("Tambah Barang Sewa (Additional Order)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onClose, enabled = !isSubmitting) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Customer info & Item Catalog
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            SessionInfoBadge(session)

                            Spacer(Modifier.height(12.dp))
                            Text("Pilih Barang Tambahan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = KasirOnSurfaceVariant)
                            Spacer(Modifier.height(8.dp))

                            ItemCatalogList(
                                selectedQty = selectedQty,
                                onChgQty = handleChgQty,
                                idrFormat = idrFormat,
                                isSubmitting = isSubmitting
                            )
                        }

                        // Right Column: Summary, Payment method, Cash handling, Actions
                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                TotalPokokSummaryBox(totalAdditional, idrFormat)

                                Spacer(Modifier.height(12.dp))
                                PaymentMethodSelector(payAwal = payAwal, onSelect = { payAwal = it })

                                if (payAwal == "cash" && totalAdditional > 0.0) {
                                    Spacer(Modifier.height(12.dp))
                                    CashHandlingSection(
                                        totalAdditional = totalAdditional,
                                        cashGiven = cashGiven,
                                        onCashGivenChange = { cashGiven = it },
                                        cashNum = cashNum,
                                        changeAmt = changeAmt,
                                        idrFormat = idrFormat
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            ActionButtons(
                                onClose = onClose,
                                onSave = onSave,
                                isSubmitting = isSubmitting,
                                canSave = canSave
                            )
                        }
                    }
                }
            } else {
                // ── Single Column Portrait Layout ──────────────────────────────────────
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null, tint = KasirAccent)
                            Spacer(Modifier.width(8.dp))
                            Text("Tambah Barang Sewa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onClose, enabled = !isSubmitting) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    SessionInfoBadge(session)

                    Spacer(Modifier.height(12.dp))
                    Text("Pilih Barang Tambahan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = KasirOnSurfaceVariant)
                    Spacer(Modifier.height(8.dp))

                    ItemCatalogList(
                        selectedQty = selectedQty,
                        onChgQty = handleChgQty,
                        idrFormat = idrFormat,
                        isSubmitting = isSubmitting
                    )

                    Spacer(Modifier.height(14.dp))
                    TotalPokokSummaryBox(totalAdditional, idrFormat)

                    Spacer(Modifier.height(14.dp))
                    PaymentMethodSelector(payAwal = payAwal, onSelect = { payAwal = it })

                    if (payAwal == "cash" && totalAdditional > 0.0) {
                        Spacer(Modifier.height(12.dp))
                        CashHandlingSection(
                            totalAdditional = totalAdditional,
                            cashGiven = cashGiven,
                            onCashGivenChange = { cashGiven = it },
                            cashNum = cashNum,
                            changeAmt = changeAmt,
                            idrFormat = idrFormat
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    ActionButtons(
                        onClose = onClose,
                        onSave = onSave,
                        isSubmitting = isSubmitting,
                        canSave = canSave
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionInfoBadge(session: SessionDto) {
    Surface(
        color = KasirSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, KasirLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = KasirAccent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, KasirAccent.copy(alpha = 0.35f))
            ) {
                Text(
                    "NO. ${session.queueNo.toString().padStart(3, '0')}",
                    fontFamily = KasirMono,
                    fontWeight = FontWeight.Bold,
                    color = KasirAccent,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Penyewa", style = MaterialTheme.typography.labelSmall, color = KasirTextLow)
                Text(session.nama.ifBlank { "Penyewa" }, fontWeight = FontWeight.Bold, color = KasirOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ItemCatalogList(
    selectedQty: Map<String, Int>,
    onChgQty: (String, Int) -> Unit,
    idrFormat: NumberFormat,
    isSubmitting: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ItemCatalog.ITEMS.forEach { item ->
            val qty = selectedQty[item.code] ?: 0
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (qty > 0) KasirSurfaceVariant else KasirSurfaceCard,
                border = BorderStroke(1.dp, if (qty > 0) KasirAccent.copy(alpha = 0.5f) else KasirLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item.code,
                                fontFamily = KasirMono,
                                fontWeight = FontWeight.Bold,
                                color = KasirAccent,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(item.name, fontWeight = FontWeight.SemiBold, color = KasirOnSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${idrFormat.format(item.priceHour)} / jam",
                            style = MaterialTheme.typography.labelSmall,
                            color = KasirTextLow
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { onChgQty(item.code, -1) },
                            enabled = qty > 0 && !isSubmitting,
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Kurangi", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            "$qty",
                            fontFamily = KasirMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (qty > 0) KasirAccent else KasirOnSurface,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        FilledTonalIconButton(
                            onClick = { onChgQty(item.code, 1) },
                            enabled = !isSubmitting,
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (qty > 0) KasirAccent else KasirSurfaceVariant,
                                contentColor = if (qty > 0) KasirOnAccent else KasirOnSurface
                            )
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalPokokSummaryBox(totalAdditional: Double, idrFormat: NumberFormat) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KasirAccent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, KasirAccent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Sewa Pokok:", fontWeight = FontWeight.SemiBold, color = KasirOnSurface)
            Text(
                idrFormat.format(totalAdditional),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = KasirAccent
            )
        }
    }
}

@Composable
private fun PaymentMethodSelector(payAwal: String, onSelect: (String) -> Unit) {
    Column {
        Text("Metode Pembayaran Tambahan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = KasirOnSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isCash = payAwal == "cash"
            Surface(
                onClick = { onSelect("cash") },
                shape = RoundedCornerShape(10.dp),
                color = if (isCash) KasirCash.copy(alpha = 0.2f) else KasirSurfaceVariant,
                border = BorderStroke(1.dp, if (isCash) KasirCash else KasirLine),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Payments, contentDescription = null, tint = if (isCash) KasirCash else KasirTextLow, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CASH", fontWeight = FontWeight.Bold, color = if (isCash) KasirCash else KasirOnSurfaceVariant)
                }
            }

            val isQris = payAwal == "qris"
            Surface(
                onClick = { onSelect("qris") },
                shape = RoundedCornerShape(10.dp),
                color = if (isQris) KasirQris.copy(alpha = 0.2f) else KasirSurfaceVariant,
                border = BorderStroke(1.dp, if (isQris) KasirQris else KasirLine),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.QrCode, contentDescription = null, tint = if (isQris) KasirQris else KasirTextLow, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("QRIS", fontWeight = FontWeight.Bold, color = if (isQris) KasirQris else KasirOnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CashHandlingSection(
    totalAdditional: Double,
    cashGiven: String,
    onCashGivenChange: (String) -> Unit,
    cashNum: Double,
    changeAmt: Double,
    idrFormat: NumberFormat
) {
    Column {
        Text("Uang Diterima (Opsional)", style = MaterialTheme.typography.labelSmall, color = KasirTextLow)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = cashGiven,
            onValueChange = onCashGivenChange,
            placeholder = { Text("Contoh: ${totalAdditional.toLong()}") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirCash)
        )

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(
                onClick = { onCashGivenChange(totalAdditional.toLong().toString()) },
                label = { Text("Uang Pas", fontSize = 11.sp) }
            )
            if (totalAdditional <= 50000.0) {
                AssistChip(
                    onClick = { onCashGivenChange("50000") },
                    label = { Text("Rp 50.000", fontSize = 11.sp) }
                )
            }
            if (totalAdditional <= 100000.0) {
                AssistChip(
                    onClick = { onCashGivenChange("100000") },
                    label = { Text("Rp 100.000", fontSize = 11.sp) }
                )
            }
        }

        if (cashNum > totalAdditional) {
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = KasirGreen.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, KasirGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kembalian:", style = MaterialTheme.typography.labelSmall, color = KasirGreen)
                    Text(idrFormat.format(changeAmt), fontWeight = FontWeight.Bold, color = KasirGreen)
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onClose: () -> Unit,
    onSave: () -> Unit,
    isSubmitting: Boolean,
    canSave: Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onClose,
            enabled = !isSubmitting,
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Batal")
        }

        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.weight(1.5f).height(46.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KasirAccent,
                contentColor = KasirOnAccent
            )
        ) {
            Text(
                if (isSubmitting) "Menyimpan..." else "Simpan & Cetak Struk",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
