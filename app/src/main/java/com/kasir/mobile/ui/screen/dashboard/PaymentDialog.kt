package com.kasir.mobile.ui.screen.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kasir.mobile.ui.components.rememberPressScale
import com.kasir.mobile.ui.theme.*
import com.kasir.mobile.ui.viewmodel.PaymentCalcData
import java.text.NumberFormat
import java.util.*

/**
 * Mirrors kasir-db PaymentModal.jsx: single Cash/QRIS toggle, cash change box,
 * QRIS scan box, and Konfirmasi Pembayaran + Batal.
 * Supports responsive 2-column layout on landscape orientation.
 */
@Composable
fun PaymentDialog(
    paymentData: PaymentCalcData,
    onClose: () -> Unit,
    onConfirm: (cash: Double, qris: Double) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
    val grand = paymentData.otSum
    val isNoOT = grand == 0.0

    var payMode by remember { mutableStateOf(if (isNoOT) paymentData.session.payAwal else "cash") }
    var cashAmt by remember { mutableStateOf(grand) }
    var submitting by remember { mutableStateOf(false) }

    val changeVal = (cashAmt - grand).coerceAtLeast(0.0)
    val payAwal = paymentData.session.payAwal
    val payAwalColor = if (payAwal == "qris") KasirQris else KasirCash
    val pressCash = rememberPressScale()
    val pressQris = rememberPressScale()
    val pressConfirm = rememberPressScale()

    Dialog(
        onDismissRequest = { if (!submitting) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = !isLandscape)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KasirSurfaceCard,
            border = BorderStroke(1.dp, KasirLine),
            modifier = if (isLandscape) {
                Modifier.fillMaxWidth(0.90f).fillMaxHeight(0.92f).padding(8.dp)
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
                            Icon(Icons.Filled.CreditCard, contentDescription = null, tint = KasirGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Pembayaran Sewa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onClose, enabled = !submitting) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Tagihan, Bayar Awal info, Pay Method Toggle
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Total box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = KasirSurfaceVariant,
                                border = BorderStroke(1.dp, KasirLine),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Total Tagihan (Overtime)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KasirOnSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        idrFormat.format(grand),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNoOT) KasirGreen else KasirAccent
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Initial payment method — how the rental was paid upfront
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Bayar Awal (Pokok):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = KasirOnSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = payAwalColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, payAwalColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        payAwal.uppercase(Locale.getDefault()),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = payAwalColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            if (isNoOT) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = KasirGreen.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, KasirGreen.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = KasirGreen, modifier = Modifier.size(26.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Pengembalian Tepat Waktu", fontWeight = FontWeight.Bold, color = KasirGreen)
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "Tarif sewa pokok sudah lunas via ${payAwal.uppercase(Locale.getDefault())}. Tidak ada tagihan overtime.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = KasirOnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "Pilih Jenis Pembayaran",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = KasirOnSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))

                                // Pay method toggle
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Cash
                                    OutlinedButton(
                                        onClick = { payMode = "cash"; cashAmt = grand },
                                        interactionSource = pressCash.interactionSource,
                                        modifier = pressCash.modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (payMode == "cash") KasirCash else KasirLine),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (payMode == "cash") KasirCash.copy(alpha = 0.14f) else KasirSurfaceCard
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.Payments,
                                            contentDescription = null,
                                            tint = if (payMode == "cash") KasirCash else KasirOnSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Cash",
                                            fontWeight = FontWeight.Bold,
                                            color = if (payMode == "cash") KasirCash else KasirOnSurfaceVariant
                                        )
                                    }
                                    // QRIS
                                    OutlinedButton(
                                        onClick = { payMode = "qris"; cashAmt = 0.0 },
                                        interactionSource = pressQris.interactionSource,
                                        modifier = pressQris.modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (payMode == "qris") KasirQris else KasirLine),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (payMode == "qris") KasirQris.copy(alpha = 0.14f) else KasirSurfaceCard
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.QrCode,
                                            contentDescription = null,
                                            tint = if (payMode == "qris") KasirQris else KasirOnSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "QRIS",
                                            fontWeight = FontWeight.Bold,
                                            color = if (payMode == "qris") KasirQris else KasirOnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Right Column: Cash Input / QRIS Scan, Actions
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                if (!isNoOT) {
                                    if (payMode == "cash") {
                                        OutlinedTextField(
                                            value = if (cashAmt == 0.0) "" else cashAmt.toLong().toString(),
                                            onValueChange = { input ->
                                                cashAmt = input.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                                            },
                                            label = { Text("Jumlah Uang Cash Diterima") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            enabled = !submitting,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirGreen)
                                        )

                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            AssistChip(
                                                onClick = { cashAmt = grand },
                                                label = { Text("Uang Pas", fontSize = 11.sp) }
                                            )
                                            if (grand <= 50000.0) {
                                                AssistChip(
                                                    onClick = { cashAmt = 50000.0 },
                                                    label = { Text("Rp 50.000", fontSize = 11.sp) }
                                                )
                                            }
                                            if (grand <= 100000.0) {
                                                AssistChip(
                                                    onClick = { cashAmt = 100000.0 },
                                                    label = { Text("Rp 100.000", fontSize = 11.sp) }
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        Surface(
                                            color = KasirGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Kembalian", fontWeight = FontWeight.SemiBold, color = KasirGreen)
                                                Text(idrFormat.format(changeVal), fontWeight = FontWeight.Bold, color = KasirGreen)
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = KasirSurfaceVariant,
                                            border = BorderStroke(1.dp, KasirLine),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(Icons.Filled.QrCode, contentDescription = null, tint = KasirQris, modifier = Modifier.size(36.dp))
                                                Spacer(Modifier.height(4.dp))
                                                Text("Scan QRIS", fontWeight = FontWeight.Bold, color = KasirOnSurface)
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    idrFormat.format(grand),
                                                    fontWeight = FontWeight.Bold,
                                                    color = KasirAccent,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Column {
                                Button(
                                    onClick = {
                                        submitting = true
                                        val finalCash = if (!isNoOT && payMode == "cash") grand else 0.0
                                        val finalQris = if (!isNoOT && payMode == "qris") grand else 0.0
                                        onConfirm(finalCash, finalQris)
                                    },
                                    enabled = !submitting,
                                    interactionSource = pressConfirm.interactionSource,
                                    modifier = pressConfirm.modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = KasirGreen, contentColor = KasirOnGreen)
                                ) {
                                    if (submitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = KasirOnGreen
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Menyimpan Transaksi...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (isNoOT) "Konfirmasi Selesai (Rp 0)" else "Konfirmasi Pembayaran",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = onClose,
                                    enabled = !submitting,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Batal", fontWeight = FontWeight.Bold)
                                }
                            }
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
                            Icon(Icons.Filled.CreditCard, contentDescription = null, tint = KasirGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Pembayaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onClose, enabled = !submitting) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Total box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = KasirSurfaceVariant,
                        border = BorderStroke(1.dp, KasirLine),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Total Tagihan (Overtime)",
                                style = MaterialTheme.typography.labelMedium,
                                color = KasirOnSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                idrFormat.format(grand),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isNoOT) KasirGreen else KasirAccent
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Initial payment method — how the rental was paid upfront
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Bayar Awal (Pokok):",
                            style = MaterialTheme.typography.labelMedium,
                            color = KasirOnSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = payAwalColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, payAwalColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                payAwal.uppercase(Locale.getDefault()),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                color = payAwalColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (isNoOT) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = KasirGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, KasirGreen.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = KasirGreen, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pengembalian Tepat Waktu", fontWeight = FontWeight.Bold, color = KasirGreen)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Tarif sewa pokok sudah lunas via ${payAwal.uppercase(Locale.getDefault())}. Tidak ada tagihan overtime.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KasirOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "Pilih Jenis Pembayaran",
                            style = MaterialTheme.typography.labelMedium,
                            color = KasirOnSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        // Pay method toggle
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Cash
                            OutlinedButton(
                                onClick = { payMode = "cash"; cashAmt = grand },
                                interactionSource = pressCash.interactionSource,
                                modifier = pressCash.modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (payMode == "cash") KasirCash else KasirLine),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (payMode == "cash") KasirCash.copy(alpha = 0.14f) else KasirSurfaceCard
                                )
                            ) {
                                Icon(
                                    Icons.Filled.Payments,
                                    contentDescription = null,
                                    tint = if (payMode == "cash") KasirCash else KasirOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Cash",
                                    fontWeight = FontWeight.Bold,
                                    color = if (payMode == "cash") KasirCash else KasirOnSurfaceVariant
                                )
                            }
                            // QRIS
                            OutlinedButton(
                                onClick = { payMode = "qris"; cashAmt = 0.0 },
                                interactionSource = pressQris.interactionSource,
                                modifier = pressQris.modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (payMode == "qris") KasirQris else KasirLine),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (payMode == "qris") KasirQris.copy(alpha = 0.14f) else KasirSurfaceCard
                                )
                            ) {
                                Icon(
                                    Icons.Filled.QrCode,
                                    contentDescription = null,
                                    tint = if (payMode == "qris") KasirQris else KasirOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "QRIS",
                                    fontWeight = FontWeight.Bold,
                                    color = if (payMode == "qris") KasirQris else KasirOnSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (payMode == "cash") {
                            OutlinedTextField(
                                value = if (cashAmt == 0.0) "" else cashAmt.toLong().toString(),
                                onValueChange = { input ->
                                    cashAmt = input.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                                },
                                label = { Text("Jumlah Uang Cash Diterima") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !submitting,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirGreen)
                            )

                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AssistChip(
                                    onClick = { cashAmt = grand },
                                    label = { Text("Uang Pas", fontSize = 11.sp) }
                                )
                                if (grand <= 50000.0) {
                                    AssistChip(
                                        onClick = { cashAmt = 50000.0 },
                                        label = { Text("Rp 50.000", fontSize = 11.sp) }
                                    )
                                }
                                if (grand <= 100000.0) {
                                    AssistChip(
                                        onClick = { cashAmt = 100000.0 },
                                        label = { Text("Rp 100.000", fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Surface(
                                color = KasirGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Kembalian", fontWeight = FontWeight.SemiBold, color = KasirGreen)
                                    Text(idrFormat.format(changeVal), fontWeight = FontWeight.Bold, color = KasirGreen)
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = KasirSurfaceVariant,
                                border = BorderStroke(1.dp, KasirLine),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.QrCode, contentDescription = null, tint = KasirQris, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Scan QRIS", fontWeight = FontWeight.Bold, color = KasirOnSurface)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        idrFormat.format(grand),
                                        fontWeight = FontWeight.Bold,
                                        color = KasirAccent,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            submitting = true
                            val finalCash = if (!isNoOT && payMode == "cash") grand else 0.0
                            val finalQris = if (!isNoOT && payMode == "qris") grand else 0.0
                            onConfirm(finalCash, finalQris)
                        },
                        enabled = !submitting,
                        interactionSource = pressConfirm.interactionSource,
                        modifier = pressConfirm.modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KasirGreen, contentColor = KasirOnGreen)
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = KasirOnGreen
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Menyimpan Transaksi...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isNoOT) "Konfirmasi Selesai (Rp 0)" else "Konfirmasi Pembayaran",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onClose,
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
