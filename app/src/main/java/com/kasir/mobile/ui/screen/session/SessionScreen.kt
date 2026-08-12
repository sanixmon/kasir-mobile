package com.kasir.mobile.ui.screen.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(navController: NavController) {
    var isCheckedIn by remember { mutableStateOf(false) }
    var checkInTime by remember { mutableStateOf<Long?>(null) }
    var sessionId by remember { mutableStateOf<Long?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    // Update clock every second
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val elapsedMillis = if (isCheckedIn && checkInTime != null) now - checkInTime!! else 0L
    val elapsedHours = elapsedMillis / 3600000
    val elapsedMinutes = (elapsedMillis % 3600000) / 60000
    val elapsedSeconds = (elapsedMillis % 60000) / 1000

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sesi Kerja", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
            )
        },
        containerColor = KasirSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Badge,
                contentDescription = null,
                tint = if (isCheckedIn) KasirGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isCheckedIn) "SHIFT AKTIF" else "BELUM CHECK-IN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCheckedIn) KasirGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = dateFormat.format(Date(now)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            if (isCheckedIn) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = KasirGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Durasi Kerja", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = KasirGreen
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Check-in: ${timeFormat.format(Date(checkInTime!!))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (elapsedHours >= 8) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Overtime ${elapsedHours - 8}h ${elapsedMinutes}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = KasirAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    isLoading = true
                    if (isCheckedIn) {
                        // TODO: repository.checkOut(sessionId) via ViewModel
                        isCheckedIn = false
                        checkInTime = null
                        sessionId = null
                    } else {
                        // TODO: repository.checkIn(userId, shiftDate) via ViewModel
                        isCheckedIn = true
                        checkInTime = System.currentTimeMillis()
                        sessionId = System.currentTimeMillis() // placeholder
                    }
                    isLoading = false
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCheckedIn) MaterialTheme.colorScheme.error else KasirGreen
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (isCheckedIn) "Check-Out" else "Check-In",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
