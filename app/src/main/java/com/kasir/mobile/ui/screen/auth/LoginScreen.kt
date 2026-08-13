package com.kasir.mobile.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kasir.mobile.ui.navigation.NavRoutes
import com.kasir.mobile.ui.theme.KasirDisplay
import com.kasir.mobile.ui.theme.KasirError
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirGreenDark
import com.kasir.mobile.ui.theme.KasirLine
import com.kasir.mobile.ui.theme.KasirOnSurface
import com.kasir.mobile.ui.theme.KasirOnSurfaceVariant
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.theme.KasirTextLow
import com.kasir.mobile.ui.viewmodel.AuthViewModel
import com.kasir.mobile.ui.viewmodel.KasirViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    kasirViewModel: KasirViewModel,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    var portal by remember { mutableStateOf("cashier") } // "cashier" | "admin"
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showServerField by remember { mutableStateOf(false) }

    // Navigate to dashboard on success and register the shift user
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            kasirViewModel.setShiftUser(
                name = uiState.username,
                role = if (uiState.isAdmin) "admin" else "cashier"
            )
            navController.navigate(NavRoutes.DASHBOARD) {
                popUpTo(NavRoutes.LOGIN) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KasirSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Brand block ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(KasirGreenDark.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .border(1.dp, KasirGreen.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ElectricScooter,
                    contentDescription = null,
                    tint = KasirGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "EVREN HOUSE",
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = KasirDisplay,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = KasirOnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scooter & Stroller",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
                color = KasirGreen
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Portal selector (Kasir / Admin) ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KasirSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PortalButton(
                    selected = portal == "cashier",
                    label = "Kasir",
                    icon = Icons.Filled.Person,
                    onClick = { portal = "cashier" },
                    modifier = Modifier.weight(1f)
                )
                PortalButton(
                    selected = portal == "admin",
                    label = "Admin",
                    icon = Icons.Filled.Lock,
                    onClick = { portal = "admin" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Login card ─────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = KasirSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, KasirLine)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = if (portal == "cashier") "Login Shift Kasir" else "Verifikasi Keamanan Admin",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (portal == "cashier")
                            "Masuk dengan akun kasir untuk membuka shift."
                        else
                            "Aksi admin memerlukan password admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KasirTextLow
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (portal == "cashier") {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username Kasir") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp),
                            colors = kasirFieldColors()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (portal == "cashier") "Password Shift" else "Password Admin") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        colors = kasirFieldColors()
                    )

                    // Server URL toggle
                    TextButton(
                        onClick = { showServerField = !showServerField },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (showServerField) "Sembunyikan Server" else "Ubah Server",
                            color = KasirGreen,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    if (showServerField) {
                        OutlinedTextField(
                            value = uiState.serverUrl,
                            onValueChange = { authViewModel.setServerUrl(it) },
                            label = { Text("Server URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("https://utara.evrenhouse.online") },
                            colors = kasirFieldColors()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = KasirError.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (portal == "cashier") {
                                authViewModel.loginAsCashier(username, password, uiState.serverUrl)
                            } else {
                                authViewModel.loginAsAdmin(password, uiState.serverUrl)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isLoading && password.isNotBlank() && (portal == "admin" || username.isNotBlank()),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KasirGreen,
                            contentColor = KasirSurface,
                            disabledContainerColor = KasirGreen.copy(alpha = 0.25f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = KasirSurface
                            )
                        } else {
                            Text(
                                text = if (portal == "cashier") "Mulai Shift" else "Masuk Admin",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Konter sewa Evren House · akses terbatas kasir",
                style = MaterialTheme.typography.labelSmall,
                color = KasirTextLow
            )
        }
    }
}

@Composable
private fun PortalButton(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) KasirGreen else Color.Transparent,
        contentColor = if (selected) KasirSurface else KasirOnSurfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun kasirFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = KasirGreen,
    unfocusedBorderColor = KasirLine,
    focusedLabelColor = KasirGreen,
    unfocusedLabelColor = KasirTextLow,
    focusedContainerColor = KasirSurfaceVariant,
    unfocusedContainerColor = KasirSurfaceVariant,
    cursorColor = KasirGreen
)
