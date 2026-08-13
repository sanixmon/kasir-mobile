package com.kasir.mobile.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kasir.mobile.data.ServiceLocator
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.viewmodel.KasirViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: KasirViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf(false) }

    val isAdmin = uiState.currentUserRole == "admin"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Pengaturan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
            )
        },
        containerColor = KasirSurface
    ) { padding ->
        if (!isAdmin) {
            // Mirrors kasir-db: Pengaturan is admin-only
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Akses Ditolak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Hanya Admin yang dapat mengakses Pengaturan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server info
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = KasirSurfaceCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = KasirGreen)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Server Aktif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ServiceLocator.activeServerUrl, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Section 1: Cetak Struk Options
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = KasirSurfaceCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Print, contentDescription = null, tint = KasirGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Pengaturan Cetak Struk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cetak Struk Otomatis Saat Mulai Sewa")
                        Switch(
                            checked = uiState.printMulai,
                            onCheckedChange = { viewModel.togglePrintMulai(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = KasirGreen)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cetak Struk Otomatis Saat Selesai Sewa")
                        Switch(
                            checked = uiState.printSelesai,
                            onCheckedChange = { viewModel.togglePrintSelesai(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = KasirGreen)
                        )
                    }
                }
            }

            // Section 2: Ganti PIN Admin
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = KasirSurfaceCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = KasirGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Ubah PIN Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { oldPin = it },
                        label = { Text("PIN Lama") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirGreen)
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("PIN Baru") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KasirGreen)
                    )

                    pinMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            color = if (pinError) MaterialTheme.colorScheme.error else KasirGreen,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (oldPin.isNotBlank() && newPin.isNotBlank()) {
                                viewModel.changeAdminPassword(oldPin, newPin) { ok, msg ->
                                    pinError = !ok
                                    pinMessage = msg
                                    if (ok) {
                                        oldPin = ""
                                        newPin = ""
                                    }
                                }
                            }
                        },
                        enabled = oldPin.isNotBlank() && newPin.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
                    ) {
                        Text("Simpan PIN Baru")
                    }
                }
            }
        }
    }
}
