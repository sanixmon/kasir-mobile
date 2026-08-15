package com.kasir.mobile.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasir.mobile.data.model.ItemCatalog
import com.kasir.mobile.data.model.TransactionDto
import com.kasir.mobile.domain.usecase.ShiftDateUtil
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirCash
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirLine
import com.kasir.mobile.ui.theme.KasirMono
import com.kasir.mobile.ui.theme.KasirOnSurface
import com.kasir.mobile.ui.theme.KasirOnSurfaceVariant
import com.kasir.mobile.ui.theme.KasirQris
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.theme.KasirTextLow
import java.text.NumberFormat
import java.util.Locale

/**
 * Port of kasir-db `SettingsAnalytics.jsx` — the admin executive dashboard:
 * shift revenue, pokok vs overtime, payment distribution, and per-item rental
 * performance. Pure data → UI; reads from already-fetched state, no I/O.
 */
@Composable
fun SettingsAnalyticsSection(
    transactions: List<TransactionDto>,
    activeSessionsCount: Int,
    currentShiftUser: String?
) {
    val idrFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    val todayShift = ShiftDateUtil.getShiftDateFromNow()

    // kasir-db filters by shift-date of endTime (not `tanggal`), rolling at 06:00.
    val todayTxns = remember(transactions, todayShift) {
        transactions.filter { t ->
            t.endTime > 0 && ShiftDateUtil.getShiftDate(t.endTime) == todayShift
        }
    }

    val todayRevenue = todayTxns.sumOf { it.totalAll }
    val todayPokok = todayTxns.sumOf { it.totalBase }
    val todayOT = todayTxns.sumOf { it.totalOT }
    val todayPokokCash = todayTxns.filter { it.payAwal == "cash" }.sumOf { it.totalBase }
    val todayPokokQris = todayTxns.filter { it.payAwal == "qris" }.sumOf { it.totalBase }
    val todayOTCash = todayTxns.sumOf { it.cash }
    val todayOTQris = todayTxns.sumOf { it.qris }

    val totalCashAll = todayPokokCash + todayOTCash
    val totalQrisAll = todayPokokQris + todayOTQris
    val cashPct = if (todayRevenue > 0) Math.round(totalCashAll / todayRevenue * 100).toInt() else 50
    val qrisPct = 100 - cashPct
    val cashFrac = (cashPct / 100f).coerceIn(0f, 1f)

    // Per-item performance — proportional split of totalBase across the qty of
    // each item in the bill (same formula as SettingsAnalytics.jsx).
    val itemStats = remember(todayTxns) {
        ItemCatalog.ITEMS.map { item ->
            var rentalCount = 0
            var revenueSum = 0.0
            todayTxns.forEach { t ->
                val qtys = parseItemQtys(t.items)
                val qty = qtys[item.code] ?: 0
                if (qty > 0) {
                    rentalCount += qty
                    val totalQty = qtys.values.sum()
                    if (totalQty > 0) revenueSum += (t.totalBase / totalQty) * qty
                }
            }
            Triple(item, rentalCount, revenueSum)
        }.sortedByDescending { it.third }
    }
    val totalUnitsRented = itemStats.sumOf { it.second }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KasirSurfaceCard,
        border = BorderStroke(1.dp, KasirLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BarChart, contentDescription = null, tint = KasirAccent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Dashboard Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = KasirSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, KasirLine)
                ) {
                    Text(
                        "SHIFT $todayShift",
                        fontFamily = KasirMono,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = KasirTextLow,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── 4 stat tiles (2×2) ────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnalyticsStatCard(
                    label = "Total Omzet Shift Ini",
                    value = idrFormat.format(todayRevenue),
                    valueColor = KasirOnSurface,
                    sub = "${todayTxns.size} Transaksi Selesai",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    label = "Sewa Pokok",
                    value = idrFormat.format(todayPokok),
                    valueColor = KasirGreen,
                    sub = "C: ${idrFormat.format(todayPokokCash)} | Q: ${idrFormat.format(todayPokokQris)}",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnalyticsStatCard(
                    label = "Overtime (Overstay)",
                    value = idrFormat.format(todayOT),
                    valueColor = KasirAccent,
                    sub = "Denda OT Terkumpul",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsStatCard(
                    label = "Armada Aktif Bekerja",
                    value = "$activeSessionsCount Sesi",
                    valueColor = KasirGreen,
                    sub = "Petugas Shift: ${currentShiftUser ?: "-"}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Payment distribution ──────────────────────────────────────
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "DISTRIBUSI PEMBAYARAN SHIFT",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        color = KasirTextLow
                    )
                    Text(
                        "Cash ${idrFormat.format(totalCashAll)} ($cashPct%) · QRIS ${idrFormat.format(totalQrisAll)} ($qrisPct%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = KasirOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(KasirSurfaceVariant, RoundedCornerShape(5.dp))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (cashFrac > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(cashFrac)
                                    .fillMaxHeight()
                                    .background(KasirCash)
                            )
                        }
                        if (cashFrac < 1f) {
                            Box(
                                modifier = Modifier
                                    .weight(1f - cashFrac)
                                    .fillMaxHeight()
                                    .background(KasirQris)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = KasirLine)
            Spacer(Modifier.height(12.dp))

            // ── Per-item performance ──────────────────────────────────────
            Text(
                "PERFORMA SEWA KENDARAAN SHIFT INI",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                color = KasirTextLow
            )
            Spacer(Modifier.height(8.dp))
            itemStats.forEach { (item, rentalCount, revenueSum) ->
                val sharePct = if (totalUnitsRented > 0) Math.round(rentalCount * 100.0 / totalUnitsRented).toInt() else 0
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = KasirSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.emoji, fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${item.code} - ${item.name}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = KasirOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "$rentalCount Unit Disewa ($sharePct%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = KasirTextLow
                            )
                        }
                        Text(
                            idrFormat.format(revenueSum),
                            fontFamily = KasirMono,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = KasirAccent
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    label: String,
    value: String,
    valueColor: Color,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KasirSurfaceVariant,
        border = BorderStroke(1.dp, KasirLine),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                color = KasirTextLow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontFamily = KasirMono,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = KasirOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** "ST×2, SB×1" → {ST:2, SB:1}. Tolerates 'x'/'×' and missing qty (defaults 1). */
private fun parseItemQtys(itemsStr: String): Map<String, Int> {
    val map = mutableMapOf<String, Int>()
    if (itemsStr.isBlank() || itemsStr == "-") return map
    itemsStr.split(",").forEach { part ->
        val p = part.trim()
        if (p.isBlank()) return@forEach
        val code = p.substringBefore("×").substringBefore("x").trim().uppercase()
        val qty = p.substringAfter("×").substringAfter("x").trim().toIntOrNull() ?: 1
        map[code] = (map[code] ?: 0) + qty
    }
    return map
}
