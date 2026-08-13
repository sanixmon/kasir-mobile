package com.kasir.mobile.ui.screen.dashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirCash
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirLine
import com.kasir.mobile.ui.theme.KasirOnSurface
import com.kasir.mobile.ui.theme.KasirOnSurfaceVariant
import com.kasir.mobile.ui.theme.KasirQris
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import com.kasir.mobile.ui.viewmodel.PaymentCalcData
import java.text.NumberFormat
import java.util.*

/**
 * Mirrors kasir-db PaymentModal.jsx: single Cash/QRIS toggle, cash change box,
 * QRIS scan box, and Konfirmasi Pembayaran + Batal.
 */
@Composable
fun PaymentDialog(
    paymentData: PaymentCalcData,
    viewModel: KasirViewModel,
    onClose: () -> Unit,
    onConfirm: (cash: Double, qris: Double) -> Unit
) {
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val grand = paymentData.otSum
    val isNoOT = grand == 0.0

    var payMode by remember { mutableStateOf(if (isNoOT) paymentData.session.payAwal else "cash") }
    var cashAmt by remember { mutableStateOf(grand) }
    var submitting by remember { mutableStateOf(false) }

    val changeVal = (cashAmt - grand).coerceAtLeast(0.0)

    Dialog(onDismissRequest = { if (!submitting) onClose() }) {
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CreditCard, contentDescription = null, tint = KasirGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Pembayaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                            color = KasirAccent
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Pay method toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Cash
                    OutlinedButton(
                        onClick = { payMode = "cash"; cashAmt = grand },
                        enabled = !(isNoOT && paymentData.session.payAwal != "cash"),
                        modifier = Modifier.weight(1f).height(48.dp),
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
                        enabled = !(isNoOT && paymentData.session.payAwal != "qris"),
                        modifier = Modifier.weight(1f).height(48.dp),
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

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        submitting = true
                        val finalCash = if (payMode == "cash") grand else 0.0
                        val finalQris = if (payMode == "qris") grand else 0.0
                        onConfirm(finalCash, finalQris)
                    },
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Menyimpan Transaksi...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Konfirmasi Pembayaran", fontWeight = FontWeight.Bold)
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
