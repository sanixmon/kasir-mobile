package com.kasir.mobile.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import com.kasir.mobile.ui.viewmodel.PaymentCalcData
import java.text.NumberFormat
import java.util.*

@Composable
fun PaymentDialog(
    paymentData: PaymentCalcData,
    viewModel: KasirViewModel,
    onClose: () -> Unit,
    onConfirm: (cash: Double, qris: Double) -> Unit
) {
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    var payMethod by remember { mutableStateOf(paymentData.session.payAwal) } // "cash" or "qris"
    var cashInput by remember { mutableStateOf("") }
    var qrisInput by remember { mutableStateOf("") }

    val cashVal = cashInput.toDoubleOrNull() ?: 0.0
    val qrisVal = qrisInput.toDoubleOrNull() ?: 0.0
    val totalPaid = cashVal + qrisVal
    val change = totalPaid - paymentData.otSum

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
                        Icon(Icons.Filled.Payment, contentDescription = null, tint = KasirGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Pembayaran Overtime", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Summary
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total Pokok (Sudah Lunas):", style = MaterialTheme.typography.bodySmall)
                            Text(idrFormat.format(paymentData.baseSum), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total Biaya Overtime:", fontWeight = FontWeight.Bold)
                            Text(idrFormat.format(paymentData.otSum), fontWeight = FontWeight.Bold, color = KasirAccent)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Payment Inputs
                Text("Input Pembayaran Overtime", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = cashInput,
                    onValueChange = { cashInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Bayar Cash (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirGreen)
                )

                Spacer(Modifier.height(8.dp))

                // Quick cash buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10000, 20000, 50000, 100000).forEach { amount ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val current = cashInput.toDoubleOrNull() ?: 0.0
                                cashInput = (current + amount).toLong().toString()
                            },
                            label = { Text("+${amount / 1000}k", fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = qrisInput,
                    onValueChange = { qrisInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Bayar QRIS (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirGreen)
                )

                Spacer(Modifier.height(16.dp))

                if (change >= 0) {
                    Surface(
                        color = KasirGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kembalian:", fontWeight = FontWeight.SemiBold, color = KasirGreen)
                            Text(idrFormat.format(change), fontWeight = FontWeight.Bold, color = KasirGreen)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onConfirm(cashVal, qrisVal) },
                    enabled = paymentData.otSum == 0.0 || totalPaid >= paymentData.otSum,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
                ) {
                    Text("Selesaikan Transaksi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
