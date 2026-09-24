package com.kasir.mobile.ui.screen.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.ui.theme.*
import com.kasir.mobile.ui.viewmodel.ItemCalcState
import com.kasir.mobile.ui.viewmodel.KasirViewModel
import com.kasir.mobile.ui.viewmodel.PaymentCalcData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

/**
 * Port of kasir-db `CalculateRentalModal.jsx` — the "Hitung Sewa" checkout step.
 * Supports partial return: the cashier can return only some of an item's units
 * (the rest keeps renting) and the bill recomputes live.
 * Supports responsive 2-column layout on landscape orientation.
 */
@Composable
fun CalculateRentalDialog(
    session: SessionDto,
    viewModel: KasirViewModel,
    onClose: () -> Unit,
    onProceedPayment: (PaymentCalcData) -> Unit,
    onCompleteOnTime: ((PaymentCalcData) -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val idrFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val now = System.currentTimeMillis()
    val safeStart = if (session.startTime > 1577836800000L) session.startTime else now
    val payAwalColor = if (session.payAwal == "qris") KasirQris else KasirCash

    var paymentCalc by remember { mutableStateOf(viewModel.preparePayment(session)) }
    val itemsCalc = paymentCalc.itemsCalc

    // Partial return only makes sense when there's more than one unit overall.
    val isMultiItem = session.items.size > 1 || session.items.any { it.qty > 1 }
    val isOT = itemsCalc.any { it.returnQty > 0 && floor(it.itemElapsedMin - it.limitMin) >= 11 }
    val maxOver = itemsCalc.maxOfOrNull { calc ->
        val over = calc.itemElapsedMin - calc.limitMin
        if (calc.returnQty > 0 && floor(over) >= 11) over else 0.0
    } ?: 0.0
    val totalReturnQty = itemsCalc.sumOf { it.returnQty }
    val canProceed = totalReturnQty > 0

    Dialog(
        onDismissRequest = onClose,
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
                            Icon(Icons.Filled.Calculate, contentDescription = null, tint = KasirAccent)
                            Spacer(Modifier.width(8.dp))
                            Text("Hitung Sewa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Session info, Base cost, Item breakdowns, Remaining status
                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InfoCard("Penyewa", session.nama, Modifier.weight(1f))
                                InfoCard("Mulai", timeFormat.format(Date(safeStart)), Modifier.weight(1f))
                                InfoCard("Sekarang", timeFormat.format(Date(now)), Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(12.dp))
                            BaseCostPaidBadge(payAwalColor, session.payAwal, paymentCalc.baseSum, idrFormat)

                            Spacer(Modifier.height(14.dp))
                            Text("Biaya Overtime Per Item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))

                            ItemCalculationList(
                                itemsCalc = itemsCalc,
                                isMultiItem = isMultiItem,
                                idrFormat = idrFormat,
                                onReturnQtyChange = { itemIndex, newQty ->
                                    paymentCalc = viewModel.updateReturnQty(paymentCalc, itemIndex, newQty)
                                }
                            )

                            if (isMultiItem) {
                                Spacer(Modifier.height(10.dp))
                                RemainingItemsStatus(itemsCalc = itemsCalc)
                            }
                        }

                        // Right Column: Overtime alert, Totals, Actions
                        Column(
                            modifier = Modifier
                                .weight(0.85f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                OvertimeAlertBanner(isOT, maxOver)

                                Spacer(Modifier.height(12.dp))
                                OvertimeSummaryBox(paymentCalc.otSum, idrFormat)

                                Spacer(Modifier.height(8.dp))
                                GrandTotalSummaryBox(paymentCalc.baseSum + paymentCalc.otSum, idrFormat)
                            }

                            Spacer(Modifier.height(16.dp))
                            CheckoutActions(
                                onClose = onClose,
                                isOT = paymentCalc.otSum > 0.0,
                                canProceed = canProceed,
                                onCompleteOnTime = onCompleteOnTime?.let { { it(paymentCalc) } },
                                onProceedPayment = { onProceedPayment(paymentCalc) }
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
                            Icon(Icons.Filled.Calculate, contentDescription = null, tint = KasirAccent)
                            Spacer(Modifier.width(8.dp))
                            Text("Hitung Sewa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

                    Spacer(Modifier.height(14.dp))
                    BaseCostPaidBadge(payAwalColor, session.payAwal, paymentCalc.baseSum, idrFormat)

                    Spacer(Modifier.height(12.dp))
                    OvertimeAlertBanner(isOT, maxOver)

                    Spacer(Modifier.height(16.dp))
                    Text("Biaya Overtime Per Item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    ItemCalculationList(
                        itemsCalc = itemsCalc,
                        isMultiItem = isMultiItem,
                        idrFormat = idrFormat,
                        onReturnQtyChange = { itemIndex, newQty ->
                            paymentCalc = viewModel.updateReturnQty(paymentCalc, itemIndex, newQty)
                        }
                    )

                    // Remaining-items status (partial return only)
                    if (isMultiItem) {
                        Spacer(Modifier.height(10.dp))
                        RemainingItemsStatus(itemsCalc = itemsCalc)
                    }

                    Spacer(Modifier.height(16.dp))
                    OvertimeSummaryBox(paymentCalc.otSum, idrFormat)

                    Spacer(Modifier.height(8.dp))
                    GrandTotalSummaryBox(paymentCalc.baseSum + paymentCalc.otSum, idrFormat)

                    Spacer(Modifier.height(20.dp))
                    CheckoutActions(
                        onClose = onClose,
                        isOT = paymentCalc.otSum > 0.0,
                        canProceed = canProceed,
                        onCompleteOnTime = onCompleteOnTime?.let { { it(paymentCalc) } },
                        onProceedPayment = { onProceedPayment(paymentCalc) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BaseCostPaidBadge(
    payAwalColor: Color,
    payAwal: String,
    baseSum: Double,
    idrFormat: NumberFormat
) {
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = KasirGreen)
                Spacer(Modifier.width(8.dp))
                Text("Tarif Sewa Pokok — Lunas", fontWeight = FontWeight.SemiBold, color = KasirGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = payAwalColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, payAwalColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        payAwal.uppercase(Locale.getDefault()),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = payAwalColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(idrFormat.format(baseSum), fontWeight = FontWeight.Bold, color = KasirGreen)
            }
        }
    }
}

@Composable
private fun OvertimeAlertBanner(isOT: Boolean, maxOver: Double) {
    Surface(
        color = if (isOT) KasirAccent.copy(alpha = 0.15f) else KasirGreen.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOT) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isOT) KasirAccent else KasirGreen
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isOT) "Ada item melewati batas! (${floor(maxOver).toInt()} menit overtime)"
                else "Durasi dalam batas normal.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isOT) KasirAccent else KasirGreen,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ItemCalculationList(
    itemsCalc: List<ItemCalcState>,
    isMultiItem: Boolean,
    idrFormat: NumberFormat,
    onReturnQtyChange: (itemIndex: Int, newQty: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsCalc.forEach { calcItem ->
            val overMin = calcItem.itemElapsedMin - calcItem.limitMin
            val overStatus = when {
                overMin <= 0 -> "Normal (${calcItem.itemElapsedMin.toInt()}m)"
                floor(overMin) < 11 -> "Over ${floor(overMin).toInt()}m — toleransi (${calcItem.itemElapsedMin.toInt()}m)"
                else -> "Over ${floor(overMin).toInt()}m (${calcItem.itemElapsedMin.toInt()}m)"
            }
            val isReturned = calcItem.returnQty > 0

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isReturned) KasirSurfaceVariant else KasirSurfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, KasirLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${calcItem.item.code} - ${calcItem.catalogDef.name} ×${calcItem.item.qty}",
                                fontWeight = FontWeight.Bold,
                                color = KasirOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                overStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = KasirTextLow
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isReturned) idrFormat.format(calcItem.otCost) else "—",
                            fontWeight = FontWeight.Bold,
                            color = if (isReturned && calcItem.otCost > 0) KasirAccent else KasirOnSurfaceVariant
                        )
                    }

                    if (isMultiItem) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Kembalikan:", style = MaterialTheme.typography.labelSmall, color = KasirTextLow)
                            Spacer(Modifier.width(8.dp))
                            QtyStepButton(
                                icon = { Icon(Icons.Filled.Remove, contentDescription = "Kurangi", modifier = Modifier.size(16.dp)) },
                                enabled = calcItem.returnQty > 0,
                                onClick = {
                                    onReturnQtyChange(calcItem.index, calcItem.returnQty - 1)
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${calcItem.returnQty}",
                                fontFamily = KasirMono,
                                fontWeight = FontWeight.Bold,
                                color = KasirOnSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            QtyStepButton(
                                icon = { Icon(Icons.Filled.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp)) },
                                enabled = calcItem.returnQty < calcItem.item.qty,
                                primary = true,
                                onClick = {
                                    onReturnQtyChange(calcItem.index, calcItem.returnQty + 1)
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("/ ${calcItem.item.qty}", style = MaterialTheme.typography.labelSmall, color = KasirTextLow)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    if (!isReturned) {
                        Surface(
                            color = KasirTextLow.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "TETAP DISEWA (Belum Dikembalikan)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = KasirOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        val otLabels = buildList {
                            if (calcItem.otFullCount > 0) {
                                add("${calcItem.otFullCount}× 1Jam (${idrFormat.format(calcItem.catalogDef.priceOT60 * calcItem.returnQty * calcItem.otFullCount)})")
                            }
                            if (calcItem.otHalfCount > 0) {
                                add("${calcItem.otHalfCount}× ½Jam (${idrFormat.format(calcItem.catalogDef.priceOT30 * calcItem.returnQty * calcItem.otHalfCount)})")
                            }
                        }
                        Text(
                            text = otLabels.joinToString(" + ").ifEmpty { "Tidak ada overtime" },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (otLabels.isNotEmpty()) KasirAccent else KasirOnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemainingItemsStatus(itemsCalc: List<ItemCalcState>) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = KasirSurfaceVariant,
        border = BorderStroke(1.dp, KasirLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = KasirOnSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Status Sisa Sewa Aktif (Akan Terus Berjalan):",
                    style = MaterialTheme.typography.labelSmall,
                    color = KasirOnSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            val remaining = itemsCalc.filter { it.item.qty - it.returnQty > 0 }
            if (remaining.isEmpty()) {
                Text(
                    "Semua item dikembalikan (Sesi akan ditutup)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = KasirGreen
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    remaining.forEach { calc ->
                        Surface(
                            color = KasirAccent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, KasirAccent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "${calc.item.code} ×${calc.item.qty - calc.returnQty}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = KasirAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OvertimeSummaryBox(otSum: Double, idrFormat: NumberFormat) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KasirSurfaceVariant,
        border = BorderStroke(1.dp, KasirLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Tagihan Overtime", fontWeight = FontWeight.SemiBold, color = KasirOnSurfaceVariant)
            Text(
                idrFormat.format(otSum),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = KasirAccent
            )
        }
    }
}

@Composable
private fun GrandTotalSummaryBox(grandTotal: Double, idrFormat: NumberFormat) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KasirGreen.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, KasirGreen.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Biaya Keseluruhan", fontWeight = FontWeight.Bold, color = KasirGreen, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Text(
                idrFormat.format(grandTotal),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = KasirGreen
            )
        }
    }
}

@Composable
private fun CheckoutActions(
    onClose: () -> Unit,
    isOT: Boolean,
    canProceed: Boolean,
    onCompleteOnTime: (() -> Unit)?,
    onProceedPayment: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Batal", fontWeight = FontWeight.Bold)
        }
        if (!isOT && onCompleteOnTime != null) {
            Button(
                onClick = onCompleteOnTime,
                enabled = canProceed,
                modifier = Modifier.weight(2f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KasirGreen, contentColor = KasirOnGreen)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Selesai (Tepat Waktu)", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onProceedPayment,
                enabled = canProceed,
                modifier = Modifier.weight(2f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KasirGreen, contentColor = KasirOnGreen)
            ) {
                Text(
                    if (isOT) "Bayar Overtime" else "Lanjut ke Pembayaran",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QtyStepButton(
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(7.dp),
        color = if (primary) KasirGreen else KasirSurfaceVariant,
        contentColor = if (primary) KasirOnGreen else KasirOnSurface,
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}

@Composable
fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = KasirSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, KasirLine),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = KasirTextLow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = KasirOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
