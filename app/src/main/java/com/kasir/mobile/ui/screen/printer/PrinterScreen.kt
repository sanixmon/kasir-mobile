package com.kasir.mobile.ui.screen.printer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kasir.mobile.data.printer.PrinterConnectionState
import com.kasir.mobile.data.printer.PrinterDevice
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirError
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirLine
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.theme.KasirTextLow
import com.kasir.mobile.ui.viewmodel.PrinterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreen(
    navController: NavController,
    viewModel: PrinterViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val paired by viewModel.paired.collectAsState()
    val discovered by viewModel.discovered.collectAsState()
    val scanning by viewModel.scanning.collectAsState()
    val message by viewModel.message.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    val requiredPermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { granted -> granted }) {
            viewModel.refreshPaired()
            viewModel.scan()
        }
    }

    val requestScan = {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            viewModel.scan()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    val devices = remember(paired, discovered) {
        (paired + discovered)
            .distinctBy { it.address }
            .sortedWith(Comparator { a, b ->
                val aIsRp = a.name.equals("RP02N", ignoreCase = true)
                val bIsRp = b.name.equals("RP02N", ignoreCase = true)
                when {
                    aIsRp && !bIsRp -> -1
                    !aIsRp && bIsRp -> 1
                    else -> a.name.lowercase().compareTo(b.name.lowercase())
                }
            })
    }

    val connectedAddress = (state as? PrinterConnectionState.Connected)?.device?.address

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Printer Bluetooth", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
            )
        },
        containerColor = KasirSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(state)

            // Scan
            Button(
                onClick = requestScan,
                enabled = !scanning,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
            ) {
                if (scanning) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Memindai…")
                } else {
                    Icon(Icons.Filled.Bluetooth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Printer Bluetooth")
                }
            }

            // Available printers
            Text("Printer Tersedia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (devices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KasirSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, KasirLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = KasirTextLow)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (scanning) "Memindai perangkat…" else "Belum ada printer. Tekan Scan, atau pastikan printer sudah dipasangkan (pair) di pengaturan Bluetooth.",
                            color = KasirTextLow,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    devices.forEach { device ->
                        PrinterDeviceRow(
                            device = device,
                            isConnected = device.address == connectedAddress,
                            onConnect = { viewModel.connect(device) }
                        )
                    }
                }
            }

            // Test print
            Button(
                onClick = { viewModel.testPrint() },
                enabled = state is PrinterConnectionState.Connected,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state is PrinterConnectionState.Connected) KasirGreen else KasirSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.Print, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Test Print", fontWeight = FontWeight.Bold)
            }

            if (state is PrinterConnectionState.Connected) {
                OutlinedButton(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Putuskan Koneksi")
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: PrinterConnectionState) {
    val (dotColor, title, subtitle) = when (state) {
        PrinterConnectionState.Disconnected -> Triple(KasirTextLow, "Printer belum terhubung", "Pilih printer lalu tekan Connect")
        PrinterConnectionState.Connecting -> Triple(KasirAccent, "Menghubungkan…", "Membuka koneksi Bluetooth")
        is PrinterConnectionState.Connected -> Triple(KasirGreen, "Terhubung · ${state.device.name}", state.device.address)
        is PrinterConnectionState.Error -> Triple(KasirError, "Terjadi kesalahan", state.message)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KasirSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, KasirLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = KasirTextLow)
            }
        }
    }
}

@Composable
private fun PrinterDeviceRow(
    device: PrinterDevice,
    isConnected: Boolean,
    onConnect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KasirSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConnected) KasirGreen.copy(alpha = 0.5f) else KasirLine
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Print, contentDescription = null, tint = if (isConnected) KasirGreen else KasirTextLow)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.SemiBold)
                Text(device.address, style = MaterialTheme.typography.bodySmall, color = KasirTextLow)
            }
            if (isConnected) {
                Text("Terhubung", color = KasirGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KasirGreen),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Connect")
                }
            }
        }
    }
}
