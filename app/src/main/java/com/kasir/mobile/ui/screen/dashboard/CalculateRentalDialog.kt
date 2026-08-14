package com.kasir.mobile.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.domain.usecase.OvertimeUtil
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.viewmodel.ItemCalcState
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import com.kasir.mobile.ui.viewmodel.PaymentCalcData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalculateRentalDialog(
    session: SessionDto,
    viewModel: KasirViewModel,
    onClose: () -> Unit,
    onProceedPayment: (PaymentCalcData) -> Unit
) {
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val now = System.currentTimeMillis()
    val safeStart = if (session.startTime > 1577836800000L) session.startTime else now

    var paymentCalc by remember { mutableStateOf(viewModel.preparePayment(session)) }
    val isOvertime = paymentCalc.itemsCalc.any { it.otFullCount > 0 || it.otHalfCount > 0 }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Calculate, contentDescription = null, tint = KasirAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Hitung Sewa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Info boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoCard("Penyewa", session.nama, Modifier.weight(1f))
                    InfoCard("Mulai", timeFormat.format(Date(safeStart)), Modifier.weight(1f))
                    InfoCard("Sekarang", timeFormat.format(Date(now)), Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))

                // Base cost already lunas
                Surface(
                    color = KasirGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = KasirGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Tarif Sewa Pokok — Lunas", fontWeight = FontWeight.SemiBold, color = KasirGreen)
                        }
                        Text(idrFormat.format(paymentCalc.baseSum), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Overtime alert
                Surface(
                    color = if (isOvertime) KasirAccent.copy(alpha = 0.15f) else KasirGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOvertime) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isOvertime) KasirAccent else KasirGreen
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isOvertime) "Ada item melewati batas waktu!" else "Durasi dalam batas normal.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOvertime) KasirAccent else KasirGreen
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Biaya Overtime Per Item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                // Items list with return qty controls
                paymentCalc.itemsCalc.forEach { calcItem ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${calcItem.catalogDef.name} (${calcItem.item.code})", fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (calcItem.otFullCount > 0 || calcItem.otHalfCount > 0)
                                        "OT: ${calcItem.otFullCount}j ${if (calcItem.otHalfCount > 0) "30m" else ""} (${idrFormat.format(calcItem.otCost)})"
                                    else "Tidak ada Overtime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (calcItem.otCost > 0) KasirAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                idrFormat.format(calcItem.otCost),
                                fontWeight = FontWeight.Bold,
                                color = if (calcItem.otCost > 0) KasirAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Total Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Overtime:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        idrFormat.format(paymentCalc.otSum),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = KasirAccent
                    )
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onProceedPayment(paymentCalc) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
                ) {
                    Text("Proses Pembayaran", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
