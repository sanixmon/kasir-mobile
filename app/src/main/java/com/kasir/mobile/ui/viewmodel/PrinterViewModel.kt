package com.kasir.mobile.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kasir.mobile.data.printer.BluetoothPrinterRepository
import com.kasir.mobile.data.printer.PrinterConnectionState
import com.kasir.mobile.data.printer.PrinterDevice
import com.kasir.mobile.data.printer.PrinterError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrinterViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = BluetoothPrinterRepository(app)

    private val _state = MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Disconnected)
    val state: StateFlow<PrinterConnectionState> = _state.asStateFlow()

    private val _paired = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val paired: StateFlow<List<PrinterDevice>> = _paired.asStateFlow()

    private val _discovered = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val discovered: StateFlow<List<PrinterDevice>> = _discovered.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshPaired()
    }

    fun refreshPaired() {
        viewModelScope.launch {
            _paired.value = repo.getPairedPrinters()
        }
    }

    fun scan() {
        viewModelScope.launch {
            _scanning.value = true
            _discovered.value = repo.scanPrinters()
            _scanning.value = false
        }
    }

    fun connect(device: PrinterDevice) {
        viewModelScope.launch {
            _state.value = PrinterConnectionState.Connecting
            repo.connect(device)
                .onSuccess { _state.value = PrinterConnectionState.Connected(device) }
                .onFailure { _state.value = PrinterConnectionState.Error(describeError(it)) }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repo.disconnect()
            _state.value = PrinterConnectionState.Disconnected
        }
    }

    fun testPrint() {
        viewModelScope.launch {
            repo.testPrint()
                .onSuccess { _message.value = "Test print dikirim ke printer" }
                .onFailure { _message.value = "Gagal mencetak: ${describeError(it)}" }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun describeError(t: Throwable): String =
        if (t is PrinterError) (t.message ?: "Terjadi kesalahan")
        else (t.message ?: "Terjadi kesalahan tak dikenal")
}
