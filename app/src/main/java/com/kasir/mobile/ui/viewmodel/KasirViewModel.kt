package com.kasir.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasir.mobile.data.ServiceLocator
import com.kasir.mobile.data.model.CatalogItem
import com.kasir.mobile.data.model.DeletionLogDto
import com.kasir.mobile.data.model.ItemCatalog
import com.kasir.mobile.data.model.ItemDto
import com.kasir.mobile.data.model.SessionDto
import com.kasir.mobile.data.model.TransactionDto
import com.kasir.mobile.data.printer.Receipt
import com.kasir.mobile.data.printer.ReceiptType
import com.kasir.mobile.data.printer.rp
import com.kasir.mobile.data.remote.AuthTokenHolder
import com.kasir.mobile.data.repository.KasirRepository
import com.kasir.mobile.domain.usecase.OvertimeUtil
import com.kasir.mobile.domain.usecase.ShiftDateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val printMulai: Boolean = true,
    val printSelesai: Boolean = true,
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
    var activeEditSession = MutableStateFlow<SessionDto?>(null)

    // Admin PIN verification gate (non-null value = PIN dialog shown; the lambda
    // is the protected action to run once the PIN is verified)
    var pendingAdminAction = MutableStateFlow<(() -> Unit)?>(null)
    var adminPinError = MutableStateFlow<String?>(null)

    private var syncingNow = false
    private var pollingJob: Job? = null

    init {
        // Initial silent load. Ongoing polling is scoped to the Dashboard screen
        // (startPolling/stopPolling) instead of running on every screen.
        loadData(showSync = false)
    }

    /** Begin 5s background polling; no-op if already running. */
    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            loadData(showSync = false)
            while (isActive) {
                delay(5000)
                loadData(showSync = false)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun repository(): KasirRepository = ServiceLocator.repository()

    fun setShiftUser(name: String, role: String = "cashier") {
        _uiState.update {
            it.copy(
                currentShiftUser = name,
                currentUserRole = role
            )
        }
    }

    fun logout() {
        AuthTokenHolder.token = null
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

    fun loadData(showSync: Boolean = true) {
        if (syncingNow) return
        syncingNow = true
        viewModelScope.launch {
            if (showSync) _uiState.update { it.copy(isSyncing = true) }
            val result = withContext(Dispatchers.Default) {
                repository().fetchAllData().map { data ->
                    data.copy(
                        sessions = data.sessions.sortedByDescending { s -> s.startTime },
                        transactions = data.transactions.sortedBy { t -> t.no }
                    )
                }
            }
            result
                .onSuccess { data ->
                    val cur = _uiState.value
                    // Skip the state write (and the full recomposition it triggers)
                    // when nothing actually changed — common during idle polling.
                    val changed = cur.activeSessions != data.sessions ||
                        cur.transactions != data.transactions ||
                        !cur.apiConnected ||
                        cur.isSyncing
                    if (changed) {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                activeSessions = data.sessions,
                                transactions = data.transactions,
                                apiConnected = true,
                                lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            )
                        }
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            apiConnected = false
                        )
                    }
                }
            syncingNow = false
        }
    }

    fun startRental(nama: String, items: List<ItemDto>, payAwal: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val newSession = SessionDto(
                id = "s-${System.currentTimeMillis().toString(36)}",
                queueNo = 0,
                nama = nama,
                items = items,
                startTime = System.currentTimeMillis(),
                tanggal = ShiftDateUtil.getShiftDateFromNow(),
                payAwal = payAwal
            )

            repository().addSession(newSession)
                .onSuccess { res ->
                    val saved = res.session ?: newSession
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeSessions = listOf(saved) + it.activeSessions.filter { s -> s.id != saved.id },
                            successMessage = "Sesi sewa atas nama ${saved.nama} berhasil dimulai (Antrian #${saved.queueNo})"
                        )
                    }
                    if (_uiState.value.printMulai) {
                        printSessionReceipt(saved, announce = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Gagal memulai sesi: ${e.message ?: "Periksa koneksi ke server"}"
                        )
                    }
                }
        }
    }

    /**
     * "Struk Mulai Sewa" for an active session — mirrors kasir-db handlePrintMulai.
     * [announce] is false when triggered automatically right after start so it
     * doesn't clobber the "Sesi berhasil dimulai" message.
     */
    fun printSessionReceipt(session: SessionDto, announce: Boolean = true) {
        viewModelScope.launch {
            val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startMs = if (session.startTime > 1577836800000L) session.startTime else System.currentTimeMillis()

            val itemsText = session.items.joinToString("\n") { item ->
                val def = ItemCatalog.findByCode(item.code)
                val name = def?.name ?: item.code
                val unit = (def?.priceHour ?: 0.0).toLong()
                "$name x${item.qty}  ${rp(unit * item.qty)}"
            }
            val totalPokok = session.items.sumOf { item ->
                val def = ItemCatalog.findByCode(item.code)
                ((def?.priceHour ?: 0.0) * item.qty).toLong()
            }

            val receipt = Receipt(
                type = ReceiptType.MULAI,
                queueNo = session.queueNo,
                nama = session.nama,
                shift = _uiState.value.currentShiftUser,
                tanggal = dateFmt.format(Date(startMs)),
                startTime = timeFmt.format(Date(startMs)),
                itemsText = itemsText,
                totalPokok = totalPokok,
                total = totalPokok,
                qrText = "${ServiceLocator.activeServerUrl}/#track/${session.id}",
                qrCaption = "Scan QR untuk Cek Sisa Waktu",
                footer = "Terima kasih!"
            )
            ServiceLocator.printerRepository().printReceipt(receipt)
                .onSuccess {
                    if (announce) {
                        _uiState.update { it.copy(successMessage = "Struk dikirim ke printer") }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "Gagal cetak struk: ${e.message ?: "printer belum terhubung"}. Hubungkan printer di menu Printer."
                        )
                    }
                }
        }
    }

    /**
     * "Struk Selesai Sewa" — mirrors kasir-db handlePrintSelesai. [announce] is
     * false when triggered automatically right after finalize so it doesn't
     * clobber the "Pembayaran berhasil" message.
     */
    fun printTransactionReceipt(txn: TransactionDto, announce: Boolean = true) {
        viewModelScope.launch {
            val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startMs = if (txn.startTime > 1577836800000L) txn.startTime else System.currentTimeMillis()
            val endMs = if (txn.endTime > 1577836800000L) txn.endTime else startMs
            val durSec = ((endMs - startMs) / 1000).coerceAtLeast(0)
            val durasi = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                durSec / 3600, (durSec % 3600) / 60, durSec % 60
            )

            val receipt = Receipt(
                type = ReceiptType.SELESAI,
                queueNo = txn.queueNo,
                no = txn.no,
                nama = txn.nama,
                shift = txn.shift,
                tanggal = dateFmt.format(Date(endMs)),
                startTime = timeFmt.format(Date(startMs)),
                endTime = timeFmt.format(Date(endMs)),
                durasi = durasi,
                itemsText = buildItemLines(txn.items),
                otText = txn.ot.takeIf { it.isNotBlank() && it != "-" },
                totalPokok = txn.totalBase.toLong(),
                payAwal = txn.payAwal,
                overtime = txn.totalOT.takeIf { it > 0 }?.toLong(),
                total = txn.totalAll.toLong(),
                cash = txn.cash.takeIf { it > 0 }?.toLong(),
                qris = txn.qris.takeIf { it > 0 }?.toLong(),
                footer = "Terima kasih telah berkunjung!"
            )
            ServiceLocator.printerRepository().printReceipt(receipt)
                .onSuccess {
                    if (announce) {
                        _uiState.update { it.copy(successMessage = "Struk #${txn.no} dikirim ke printer") }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "Gagal cetak struk: ${e.message ?: "printer belum terhubung"}. Hubungkan printer di menu Printer."
                        )
                    }
                }
        }
    }

    /** "ST×2, SB×1" -> "Stroller x2  Rp40.000\nStroller Paket 3J x1  Rp50.000". */
    private fun buildItemLines(itemsStr: String): String {
        if (itemsStr.isBlank() || itemsStr == "-") return ""
        return itemsStr.split(",").mapNotNull { part ->
            val p = part.trim()
            if (p.isBlank()) return@mapNotNull null
            val code = p.substringBefore("×").substringBefore("x").trim()
            val qty = p.substringAfter("×").substringAfter("x").trim().toIntOrNull() ?: 1
            val def = ItemCatalog.findByCode(code)
            val name = def?.name ?: code
            val unit = (def?.priceHour ?: 0.0).toLong()
            "$name x$qty  ${rp(unit * qty)}"
        }.joinToString("\n")
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
            val session = paymentData.session

            val itemStr = paymentData.itemsCalc
                .filter { it.returnQty > 0 }
                .joinToString(", ") { "${it.item.code}×${it.returnQty}" }

            val remainingItems = session.items.map { orig ->
                val calc = paymentData.itemsCalc.find { it.item.code == orig.code }
                val returned = calc?.returnQty ?: 0
                ItemDto(code = orig.code, qty = orig.qty - returned)
            }.filter { it.qty > 0 }

            // Payload mirrors kasir-db App.jsx handleFinalizePayment
            val claimPayload = mapOf(
                "sessionId" to session.id,
                "remainingItems" to remainingItems.map { mapOf("code" to it.code, "qty" to it.qty) },
                "queueNo" to session.queueNo,
                "nama" to session.nama,
                "tanggal" to session.tanggal,
                "startTime" to session.startTime,
                "endTime" to paymentData.endTime,
                "items" to itemStr,
                "ot" to paymentData.otStr,
                "otDur" to paymentData.otDurStr,
                "totalBase" to paymentData.baseSum,
                "totalOT" to paymentData.otSum,
                "totalTol" to 0.0,
                "grandTotal" to paymentData.otSum,
                "totalAll" to paymentData.baseSum + paymentData.otSum,
                "payAwal" to session.payAwal,
                "cash" to cash,
                "qris" to qris,
                "shift" to (_uiState.value.currentShiftUser ?: "-")
            )

            repository().claimSession(claimPayload)
                .onSuccess { res ->
                    val newTxn = res.transaction ?: TransactionDto(
                        id = "t-${session.id.removePrefix("s-")}",
                        no = 0,
                        queueNo = session.queueNo,
                        nama = session.nama,
                        tanggal = session.tanggal,
                        startTime = session.startTime,
                        endTime = paymentData.endTime,
                        items = itemStr,
                        ot = paymentData.otStr,
                        otDur = paymentData.otDurStr,
                        totalBase = paymentData.baseSum,
                        totalOT = paymentData.otSum,
                        grandTotal = paymentData.otSum,
                        totalAll = paymentData.baseSum + paymentData.otSum,
                        payAwal = session.payAwal,
                        cash = cash,
                        qris = qris,
                        shift = _uiState.value.currentShiftUser ?: "-"
                    )
                    _uiState.update {
                        val updatedSessions = if (remainingItems.isNotEmpty()) {
                            it.activeSessions.map { s -> if (s.id == session.id) s.copy(items = remainingItems) else s }
                        } else {
                            it.activeSessions.filter { s -> s.id != session.id }
                        }
                        it.copy(
                            isLoading = false,
                            activeSessions = updatedSessions,
                            transactions = (listOf(newTxn) + it.transactions).sortedBy { t -> t.no },
                            successMessage = "Pembayaran transaksi #${newTxn.no} atas nama ${newTxn.nama} berhasil!"
                        )
                    }
                    if (_uiState.value.printSelesai) {
                        printTransactionReceipt(newTxn, announce = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Gagal memproses pembayaran: ${e.message ?: "Periksa koneksi ke server"}"
                        )
                    }
                }
            activeCheckoutSession.value = null
            activePaymentData.value = null
        }
    }

    fun deleteTransaction(txn: TransactionDto) {
        if (_uiState.value.currentUserRole == "admin") {
            performDeleteTransaction(txn)
        } else {
            pendingAdminAction.value = { performDeleteTransaction(txn) }
        }
    }

    private fun performDeleteTransaction(txn: TransactionDto) {
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

            // Optimistic UI update, then sync with the server
            _uiState.update {
                it.copy(
                    transactions = it.transactions.filter { t -> t.id != txn.id && t.no != txn.no },
                    deletionLogs = listOf(log) + it.deletionLogs
                )
            }

            val deleteRes = repository().deleteTxn(txn.id, txn.no)
            val logRes = repository().addDeletionLog(log)
            when {
                deleteRes.isFailure -> {
                    _uiState.update {
                        it.copy(errorMessage = "Gagal menghapus transaksi di server: ${deleteRes.exceptionOrNull()?.message ?: "koneksi gagal"}")
                    }
                    loadData()
                }
                logRes.isFailure -> {
                    _uiState.update {
                        it.copy(errorMessage = "Transaksi terhapus, tetapi gagal mencatat log penghapusan")
                    }
                }
                else -> {
                    _uiState.update { it.copy(successMessage = "Transaksi #${txn.no} berhasil dihapus") }
                }
            }
        }
    }

    fun clearAllHistory() {
        if (_uiState.value.currentUserRole == "admin") {
            performClearAllHistory()
        } else {
            pendingAdminAction.value = { performClearAllHistory() }
        }
    }

    private fun performClearAllHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository().clearAllTxns()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            transactions = emptyList(),
                            successMessage = "Semua riwayat transaksi berhasil dibersihkan"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Gagal membersihkan riwayat: ${e.message ?: "Periksa koneksi ke server"}"
                        )
                    }
                }
        }
    }

    /** Mirrors kasir-db: editing an active session always requires admin PIN first. */
    fun requestEditSession(session: SessionDto) {
        pendingAdminAction.value = { activeEditSession.value = session }
    }

    fun saveEditedSession(updated: SessionDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository().editSession(updated)
                .onSuccess { res ->
                    val saved = res.session ?: updated
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeSessions = it.activeSessions.map { s -> if (s.id == saved.id) saved else s },
                            successMessage = "Sesi atas nama ${saved.nama} berhasil diperbarui"
                        )
                    }
                    activeEditSession.value = null
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Gagal memperbarui sesi: ${e.message ?: "Periksa koneksi ke server"}"
                        )
                    }
                }
        }
    }

    fun verifyAdminPin(pin: String) {
        viewModelScope.launch {
            val res = repository().verifyAdmin(pin).getOrNull()
            if (res?.valid == true) {
                // Escalate the session with the short-lived admin token returned
                // by verify_admin so the pending destructive action is authorized.
                AuthTokenHolder.token = res.token ?: AuthTokenHolder.token
                val action = pendingAdminAction.value
                pendingAdminAction.value = null
                adminPinError.value = null
                action?.invoke()
            } else {
                adminPinError.value = "Password admin salah!"
            }
        }
    }

    fun cancelAdminPin() {
        pendingAdminAction.value = null
        adminPinError.value = null
    }

    fun loadDeletionLogs() {
        viewModelScope.launch {
            repository().getDeletionLogs()
                .onSuccess { res ->
                    _uiState.update { it.copy(deletionLogs = res.logs) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "Gagal memuat log penghapusan: ${e.message ?: "Periksa koneksi ke server"}")
                    }
                }
        }
    }

    fun changeAdminPassword(oldPass: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository().changeAdminPass(oldPass, newPass)
                .onSuccess { res ->
                    onResult(
                        res.success,
                        if (res.success) "PIN Admin berhasil diperbarui!" else (res.error ?: "Gagal memperbarui PIN admin")
                    )
                }
                .onFailure { e ->
                    onResult(false, "Gagal terhubung ke server: ${e.message ?: "Periksa URL server"}")
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
