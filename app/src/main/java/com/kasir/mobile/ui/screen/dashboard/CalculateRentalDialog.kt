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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val uiState by viewModel.uiState.collectAsState()
    val isSubmitting = uiState.isLoading
    var paymentCalc by remember { mutableStateOf(viewModel.preparePayment(session)) }
    val itemsCalc = paymentCalc.itemsCalc

    // Partial return only makes sense when there's more than one unit overall.
    val isMultiItem = session.items.size > 1 || session.items.any { it.qty > 1 }
    // Opt-in: the common case is "everything comes back", so return controls
    // stay hidden until the cashier says some units are still in use.
    var partialMode by remember { mutableStateOf(false) }
    val onPartialModeChange: (Boolean) -> Unit = { on ->
        partialMode = on
        if (!on) {
            // Turning it off always means "all returned" — no hidden leftover state.
            paymentCalc = paymentCalc.itemsCalc.fold(paymentCalc) { acc, calc ->
                viewModel.updateReturnQty(acc, calc.index, calc.item.qty)
            }
        }
    }
    val itemSectionTitle = if (partialMode) "Pilih Barang yang Dikembalikan" else "Biaya Overtime Per Item"
    val isOT = itemsCalc.any { it.returnQty > 0 && floor(it.itemElapsedMin - it.limitMin) >= 11 }
    val maxOver = itemsCalc.maxOfOrNull { calc ->
        val over = calc.itemElapsedMin - calc.limitMin
        if (calc.returnQty > 0 && floor(over) >= 11) over else 0.0
    } ?: 0.0
    val totalReturnQty = itemsCalc.sumOf { it.returnQty }
    // Display only: the pokok was paid up front for every unit, so the "Lunas"
    // badge must not shrink when some units stay out. paymentCalc.baseSum (what
    // this transaction records) is untouched.
    val paidPokok = itemsCalc.sumOf { it.catalogDef.priceHour * it.item.qty }
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
                            BaseCostPaidBadge(payAwalColor, session.payAwal, paidPokok, idrFormat)

                            Spacer(Modifier.height(14.dp))
                            if (isMultiItem) {
                                PartialReturnToggle(checked = partialMode, onCheckedChange = onPartialModeChange)
                                Spacer(Modifier.height(10.dp))
                            }
                            Text(itemSectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))

                            ItemCalculationList(
                                itemsCalc = itemsCalc,
                                showReturnControls = partialMode,
                                idrFormat = idrFormat,
                                onReturnQtyChange = { itemIndex, newQty ->
                                    paymentCalc = viewModel.updateReturnQty(paymentCalc, itemIndex, newQty)
                                }
                            )

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
                                PayNowSummaryBox(paymentCalc.baseSum, paymentCalc.otSum, idrFormat)
                            }

                            Spacer(Modifier.height(16.dp))
                            if (partialMode) {
                                ReturnSummaryLine(itemsCalc)
                                Spacer(Modifier.height(10.dp))
                            }
                            CheckoutActions(
                                onClose = onClose,
                                isOT = paymentCalc.otSum > 0.0,
                                otSum = paymentCalc.otSum,
                                allReturned = itemsCalc.all { it.returnQty == it.item.qty },
                                idrFormat = idrFormat,
                                canProceed = canProceed,
                                isSubmitting = isSubmitting,
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
                    BaseCostPaidBadge(payAwalColor, session.payAwal, paidPokok, idrFormat)

                    Spacer(Modifier.height(12.dp))
                    OvertimeAlertBanner(isOT, maxOver)

                    Spacer(Modifier.height(16.dp))
                    if (isMultiItem) {
                        PartialReturnToggle(checked = partialMode, onCheckedChange = onPartialModeChange)
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(itemSectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    ItemCalculationList(
                        itemsCalc = itemsCalc,
                        showReturnControls = partialMode,
                        idrFormat = idrFormat,
                        onReturnQtyChange = { itemIndex, newQty ->
                            paymentCalc = viewModel.updateReturnQty(paymentCalc, itemIndex, newQty)
                        }
                    )

                    // Remaining-items status (partial return only)

                    Spacer(Modifier.height(16.dp))
                    PayNowSummaryBox(paymentCalc.baseSum, paymentCalc.otSum, idrFormat)

                    Spacer(Modifier.height(20.dp))
                    if (partialMode) {
                        ReturnSummaryLine(itemsCalc)
                        Spacer(Modifier.height(10.dp))
                    }
                    CheckoutActions(
                        onClose = onClose,
                        isOT = paymentCalc.otSum > 0.0,
                        otSum = paymentCalc.otSum,
                        allReturned = itemsCalc.all { it.returnQty == it.item.qty },
                        idrFormat = idrFormat,
                        canProceed = canProceed,
                        isSubmitting = isSubmitting,
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
private fun PartialReturnToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(12.dp),
        color = if (checked) KasirAccent.copy(alpha = 0.12f) else KasirSurfaceVariant,
        border = BorderStroke(1.dp, if (checked) KasirAccent.copy(alpha = 0.4f) else KasirLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Ada barang yang masih dipakai?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KasirOnSurface
                )
                Text(
                    "Nyalakan kalau tidak semua barang dikembalikan sekarang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KasirOnSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
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
                text = if (isOT) "Ada barang yang lewat waktu, kena biaya overtime (paling lama ${floor(maxOver).toInt()} menit)"
                else "Semua barang masih dalam waktu, tidak ada biaya overtime.",
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
    showReturnControls: Boolean,
    idrFormat: NumberFormat,
    onReturnQtyChange: (itemIndex: Int, newQty: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsCalc.forEach { calcItem ->
            val overMin = calcItem.itemElapsedMin - calcItem.limitMin
            val overMinFloor = floor(overMin).toInt()
            val overStatus = when {
                overMin <= 0 -> "Dipakai ${calcItem.itemElapsedMin.toInt()} menit · masih dalam waktu"
                overMinFloor < 11 -> "Lewat $overMinFloor menit · belum kena denda (toleransi 10 menit)"
                else -> "Lewat $overMinFloor menit · kena overtime"
            }
            val isReturned = calcItem.returnQty > 0
            val stillInUse = calcItem.item.qty - calcItem.returnQty
            // Dim only the info, never the controls — a dimmed switch looks disabled.
            val infoAlpha = if (showReturnControls && !isReturned) 0.6f else 1f

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isReturned) KasirSurfaceVariant else KasirSurfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, KasirLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().alpha(infoAlpha),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${calcItem.catalogDef.name} (${calcItem.item.qty} unit)",
                                fontWeight = FontWeight.Bold,
                                color = KasirOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                overStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (overMinFloor >= 11) KasirAccent else KasirOnSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isReturned) idrFormat.format(calcItem.otCost) else "—",
                            fontWeight = FontWeight.Bold,
                            color = if (isReturned && calcItem.otCost > 0) KasirAccent else KasirOnSurfaceVariant
                        )
                    }

                    if (showReturnControls) {
                        Spacer(Modifier.height(8.dp))
                        if (calcItem.item.qty == 1) {
                            // A single unit is a yes/no decision — a switch reads better than 0/1 steps.
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isReturned) "Dikembalikan sekarang" else "Masih dipakai",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KasirOnSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = isReturned,
                                    onCheckedChange = { on -> onReturnQtyChange(calcItem.index, if (on) 1 else 0) }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Dikembalikan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KasirOnSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                QtyStepButton(
                                    icon = { Icon(Icons.Filled.Remove, contentDescription = "Kurangi", modifier = Modifier.size(22.dp)) },
                                    enabled = calcItem.returnQty > 0,
                                    onClick = {
                                        onReturnQtyChange(calcItem.index, calcItem.returnQty - 1)
                                    }
                                )
                                Text(
                                    "${calcItem.returnQty}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = KasirMono,
                                    fontWeight = FontWeight.Bold,
                                    color = KasirOnSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(min = 40.dp)
                                )
                                QtyStepButton(
                                    icon = { Icon(Icons.Filled.Add, contentDescription = "Tambah", modifier = Modifier.size(22.dp)) },
                                    enabled = calcItem.returnQty < calcItem.item.qty,
                                    primary = true,
                                    onClick = {
                                        onReturnQtyChange(calcItem.index, calcItem.returnQty + 1)
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "dari ${calcItem.item.qty}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KasirOnSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    if (showReturnControls && stillInUse > 0) {
                        Surface(
                            color = KasirTextLow.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (calcItem.item.qty == 1) "MASIH DIPAKAI · timer lanjut"
                                else "$stillInUse MASIH DIPAKAI · timer lanjut",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = KasirOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (isReturned) Spacer(Modifier.height(6.dp))
                    }
                    if (isReturned) {
                        val otLabels = buildList {
                            if (calcItem.otFullCount > 0) {
                                add("${calcItem.otFullCount}× 1 jam (${idrFormat.format(calcItem.catalogDef.priceOT60 * calcItem.returnQty * calcItem.otFullCount)})")
                            }
                            if (calcItem.otHalfCount > 0) {
                                add("${calcItem.otHalfCount}× ½ jam (${idrFormat.format(calcItem.catalogDef.priceOT30 * calcItem.returnQty * calcItem.otHalfCount)})")
                            }
                        }
                        Text(
                            text = if (otLabels.isEmpty()) "Tidak ada biaya overtime" else "Overtime: " + otLabels.joinToString(" + "),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (otLabels.isNotEmpty()) KasirAccent else KasirOnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** One-line confirmation right above the action button: what goes back, what keeps running. */
@Composable
private fun ReturnSummaryLine(itemsCalc: List<ItemCalcState>) {
    val returned = itemsCalc.filter { it.returnQty > 0 }
        .joinToString(", ") { "${it.catalogDef.name} ${it.returnQty}" }
    val inUse = itemsCalc.filter { it.item.qty - it.returnQty > 0 }
        .joinToString(", ") { "${it.catalogDef.name} ${it.item.qty - it.returnQty}" }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = KasirSurfaceVariant,
        border = BorderStroke(1.dp, KasirLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = when {
                inUse.isEmpty() -> "Semua barang dikembalikan, sewa ditutup."
                returned.isEmpty() -> "Masih dipakai: $inUse (timer lanjut)"
                else -> "Dikembalikan: $returned · Masih dipakai: $inUse (timer lanjut)"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (inUse.isEmpty()) KasirGreen else KasirOnSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

/**
 * What the customer pays now. Pokok was settled at the start, so it's listed
 * (greyed) only to explain the number — the big figure is the overtime.
 */
@Composable
private fun PayNowSummaryBox(returnedPokok: Double, otSum: Double, idrFormat: NumberFormat) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KasirGreen.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, KasirGreen.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pokok barang kembali (lunas)", style = MaterialTheme.typography.bodyMedium, color = KasirTextLow, modifier = Modifier.weight(1f))
                Text(idrFormat.format(returnedPokok), style = MaterialTheme.typography.bodyMedium, color = KasirTextLow)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Overtime", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = KasirOnSurfaceVariant, modifier = Modifier.weight(1f))
                Text(
                    idrFormat.format(otSum),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (otSum > 0) KasirAccent else KasirOnSurfaceVariant
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = KasirGreen.copy(alpha = 0.35f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bayar Sekarang", fontWeight = FontWeight.Bold, color = KasirGreen, modifier = Modifier.weight(1f))
                Text(
                    idrFormat.format(otSum),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = KasirGreen
                )
            }
        }
    }
}

@Composable
private fun CheckoutActions(
    onClose: () -> Unit,
    isOT: Boolean,
    otSum: Double,
    allReturned: Boolean,
    idrFormat: NumberFormat,
    canProceed: Boolean,
    isSubmitting: Boolean = false,
    onCompleteOnTime: (() -> Unit)?,
    onProceedPayment: () -> Unit
) {
    Column {
        if (!canProceed) {
            Text(
                "Pilih minimal 1 barang yang dikembalikan",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = KasirAccent,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }
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
                    enabled = canProceed && !isSubmitting,
                    modifier = Modifier.weight(2f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KasirGreen, contentColor = KasirOnGreen)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = KasirOnGreen
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Menyimpan...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (allReturned) "Selesai, Tutup Sewa" else "Simpan, Sisa Tetap Jalan",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        if (isOT) "Bayar ${idrFormat.format(otSum)}" else "Lanjut ke Pembayaran",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
        shape = RoundedCornerShape(10.dp),
        color = if (primary) KasirGreen else KasirSurfaceVariant,
        contentColor = if (primary) KasirOnGreen else KasirOnSurface,
        border = if (primary) null else BorderStroke(1.dp, KasirLine),
        modifier = Modifier.size(48.dp)
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
