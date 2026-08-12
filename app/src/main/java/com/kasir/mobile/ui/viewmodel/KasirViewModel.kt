package com.kasir.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasir.mobile.data.model.CatalogItem
import com.kasir.mobile.data.model.DeletionLogDto
import com.kasir.mobile.data.model.ItemCatalog
import com.kasir.mobile.data.model.ItemDto
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.data.model.TransactionDto
import com.kasir.mobile.domain.usecase.OvertimeUtil
import com.kasir.mobile.domain.usecase.ShiftDateUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KasirUiState(
    val currentShiftUser: String? = null,
    val currentUserRole: String? = null, // "cashier" or "admin"
    val activeSessions: List<SessionDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val deletionLogs: List<DeletionLogDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val activeTab: String = "dashboard",
    val printMulai: Boolean = false,
    val printSelesai: Boolean = false,
    val theme: String = "dark",
    val apiConnected: Boolean = true,
    val lastSyncTime: String = ""
)

data class ItemCalcState(
    val item: ItemDto,
    val catalogDef: CatalogItem,
    val limitMin: Int,
    var returnQty: Int,
    var baseCost: Double,
    var otFullCount: Int,
    var otHalfCount: Int,
    var otCost: Double
)

data class PaymentCalcData(
    val session: SessionDto,
    val itemsCalc: List<ItemCalcState>,
    val baseSum: Double,
    val otSum: Double,
    val grandTotal: Double,
    val otStr: String,
    val otDurStr: String,
    val endTime: Long
)

class KasirViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(KasirUiState())
    val uiState: StateFlow<KasirUiState> = _uiState.asStateFlow()

    // Modals
    var activeCheckoutSession = MutableStateFlow<SessionDto?>(null)
    var activePaymentData = MutableStateFlow<PaymentCalcData?>(null)
    var activeQrSession = MutableStateFlow<SessionDto?>(null)
    var activeEditSession = MutableStateFlow<SessionDto?>(null)
    var showAdminPinDialog = MutableStateFlow<Pair<Boolean, (() -> Unit)?>>(Pair(false, null))

    init {
        // Auto check shift expiration on start
        checkShiftExpiration()
    }

    fun setShiftUser(name: String, role: String = "cashier") {
        _uiState.update {
            it.copy(
                currentShiftUser = name,
                currentUserRole = role
            )
        }
    }

    fun logout() {
        _uiState.update {
            it.copy(
                currentShiftUser = null,
                currentUserRole = null
            )
        }
    }

    fun setTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun togglePrintMulai(value: Boolean) {
        _uiState.update { it.copy(printMulai = value) }
    }

    fun togglePrintSelesai(value: Boolean) {
        _uiState.update { it.copy(printSelesai = value) }
    }

    private fun checkShiftExpiration() {
        val currentShift = ShiftDateUtil.getShiftDateFromNow()
        // If needed reset on shift date change
    }

    fun startRental(nama: String, items: List<ItemDto>, payAwal: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val newQueueNo = (_uiState.value.activeSessions.maxOfOrNull { it.queueNo } ?: 0) + 1
            val newSession = SessionDto(
                id = "s-${System.currentTimeMillis()}",
                queueNo = newQueueNo,
                nama = nama,
                items = items,
                startTime = System.currentTimeMillis(),
                tanggal = ShiftDateUtil.getShiftDateFromNow(),
                payAwal = payAwal
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSessions = listOf(newSession) + it.activeSessions,
                    successMessage = "Sesi sewa atas nama $nama berhasil dimulai"
                )
            }
        }
    }

    fun preparePayment(session: SessionDto): PaymentCalcData {
        val safeStart = if (session.startTime > 1577836800000L) session.startTime else System.currentTimeMillis()
        val elapsedMin = (System.currentTimeMillis() - safeStart) / 60000.0

        val itemsCalc = session.items.mapNotNull { it ->
            val def = ItemCatalog.findByCode(it.code) ?: return@mapNotNull null
            val limitMin = if (def.isPackage) def.packageHours * 60 else 60
            val otRes = OvertimeUtil.calcItemOT(elapsedMin, limitMin.toDouble(), def.priceOT30, def.priceOT60, it.qty)

            ItemCalcState(
                item = it,
                catalogDef = def,
                limitMin = limitMin,
                returnQty = it.qty,
                baseCost = def.priceHour * it.qty,
                otFullCount = otRes.otFullCount,
                otHalfCount = otRes.otHalfCount,
                otCost = otRes.otCost
            )
        }

        val baseSum = itemsCalc.sumOf { it.baseCost }
        val otSum = itemsCalc.sumOf { it.otCost }
        val grandTotal = baseSum + otSum

        val otStr = itemsCalc.filter { it.otFullCount > 0 || it.otHalfCount > 0 }
            .joinToString(", ") { "${it.item.code}(${if (it.otFullCount > 0) "${it.otFullCount}×1j" else ""}${if (it.otHalfCount > 0) "+${it.otHalfCount}×½j" else ""})" }
            .ifEmpty { "-" }

        val otDurStr = itemsCalc.filter { it.otFullCount > 0 || it.otHalfCount > 0 }
            .joinToString(", ") { "${it.item.code}:${it.otFullCount * 60 + it.otHalfCount * 30}m" }
            .ifEmpty { "-" }

        return PaymentCalcData(
            session = session,
            itemsCalc = itemsCalc,
            baseSum = baseSum,
            otSum = otSum,
            grandTotal = grandTotal,
            otStr = otStr,
            otDurStr = otDurStr,
            endTime = System.currentTimeMillis()
        )
    }

    fun finalizePayment(paymentData: PaymentCalcData, cash: Double, qris: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val itemStr = paymentData.itemsCalc
                .filter { it.returnQty > 0 }
                .joinToString(", ") { "${it.item.code}×${it.returnQty}" }

            val remainingItems = paymentData.session.items.map { orig ->
                val calc = paymentData.itemsCalc.find { it.item.code == orig.code }
                val returned = calc?.returnQty ?: 0
                ItemDto(code = orig.code, qty = orig.qty - returned)
            }.filter { it.qty > 0 }

            val newTxn = TransactionDto(
                id = "t-${paymentData.session.id}",
                no = (_uiState.value.transactions.maxOfOrNull { it.no } ?: 0) + 1,
                queueNo = paymentData.session.queueNo,
                nama = paymentData.session.nama,
                tanggal = paymentData.session.tanggal,
                startTime = paymentData.session.startTime,
                endTime = paymentData.endTime,
                items = itemStr,
                ot = paymentData.otStr,
                otDur = paymentData.otDurStr,
                totalBase = paymentData.baseSum,
                totalOT = paymentData.otSum,
                totalTol = 0.0,
                grandTotal = paymentData.grandTotal,
                totalAll = paymentData.grandTotal,
                payAwal = paymentData.session.payAwal,
                cash = cash,
                qris = qris,
                shift = _uiState.value.currentShiftUser ?: "Kasir"
            )

            _uiState.update {
                val updatedSessions = if (remainingItems.isNotEmpty()) {
                    it.activeSessions.map { s -> if (s.id == paymentData.session.id) s.copy(items = remainingItems) else s }
                } else {
                    it.activeSessions.filter { s -> s.id != paymentData.session.id }
                }
                it.copy(
                    isLoading = false,
                    activeSessions = updatedSessions,
                    transactions = listOf(newTxn) + it.transactions,
                    successMessage = "Pembayaran transaksi #${newTxn.no} atas nama ${newTxn.nama} berhasil!"
                )
            }
            activeCheckoutSession.value = null
            activePaymentData.value = null
        }
    }

    fun deleteTransaction(txn: TransactionDto) {
        viewModelScope.launch {
            val log = DeletionLogDto(
                id = System.currentTimeMillis(),
                txnId = txn.id,
                txnNo = txn.no,
                txnNama = txn.nama,
                txnTanggal = txn.tanggal,
                txnTotalAll = txn.totalAll,
                deletedAt = System.currentTimeMillis(),
                deletedBy = _uiState.value.currentShiftUser ?: "admin"
            )
            _uiState.update {
                it.copy(
                    transactions = it.transactions.filter { t -> t.id != txn.id },
                    deletionLogs = listOf(log) + it.deletionLogs,
                    successMessage = "Transaksi #${txn.no} berhasil dihapus"
                )
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    transactions = emptyList(),
                    successMessage = "Semua riwayat transaksi berhasil dibersihkan"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
