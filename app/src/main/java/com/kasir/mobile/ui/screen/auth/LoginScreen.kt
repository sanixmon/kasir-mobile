package com.kasir.mobile.ui.screen.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kasir.mobile.ui.navigation.NavRoutes
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
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
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo / Title
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "💰",
                        fontSize = 56.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kasir Mobile",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = KasirGreen
                    )
                    Text(
                        text = "Point of Sale & Rental System",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Portal selector (mirrors kasir-db RoleSelection)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = portal == "cashier",
                    onClick = { portal = "cashier" },
                    label = { Text("Portal Kasir (POS)", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KasirGreen)
                )
                FilterChip(
                    selected = portal == "admin",
                    onClick = { portal = "admin" },
                    label = { Text("Portal Admin", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KasirGreen)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = KasirSurfaceVariant,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = if (portal == "cashier") "Login Shift Kasir" else "Verifikasi Keamanan Admin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KasirGreen,
                                focusedLabelColor = KasirGreen,
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (portal == "cashier") "Password Shift" else "Password Admin") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KasirGreen,
                            focusedLabelColor = KasirGreen,
                        )
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

                    AnimatedVisibility(visible = showServerField) {
                        OutlinedTextField(
                            value = uiState.serverUrl,
                            onValueChange = { authViewModel.setServerUrl(it) },
                            label = { Text("Server URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://utara.evrenhouse.online") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KasirGreen,
                                focusedLabelColor = KasirGreen,
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Error
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                            .height(50.dp),
                        enabled = !uiState.isLoading && password.isNotBlank() && (portal == "admin" || username.isNotBlank()),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = if (portal == "cashier") "Mulai Shift" else "Masuk Admin",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
