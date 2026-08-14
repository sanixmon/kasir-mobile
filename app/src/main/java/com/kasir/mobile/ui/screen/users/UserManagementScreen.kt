package com.kasir.mobile.ui.screen.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kasir.mobile.data.model.UserDto
import com.kasir.mobile.ui.screen.dashboard.AdminPinDialog
import com.kasir.mobile.ui.theme.KasirAccent
import com.kasir.mobile.ui.theme.KasirError
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirLine
import com.kasir.mobile.ui.theme.KasirMono
import com.kasir.mobile.ui.theme.KasirOnSurface
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceCard
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import com.kasir.mobile.ui.theme.KasirTextLow
import com.kasir.mobile.ui.viewmodel.KasirViewModel

/**
 * Admin-only screen to manage cashier accounts: lists all users from
 * fetch_data, adds new cashiers (save_user) and removes them (delete_user).
 * Both mutations require the admin role server-side; when the current session
 * isn't already admin, they're gated behind the shared admin PIN dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    navController: NavController,
    viewModel: KasirViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val msg = uiState.errorMessage ?: uiState.successMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<UserDto?>(null) }

    // Admins first, then cashiers alphabetically.
    val sortedUsers = remember(uiState.users) {
        uiState.users.sortedWith(compareBy({ it.role != "admin" }, { it.username.lowercase() }))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Kasir", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
            )
        },
        containerColor = KasirSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    addError = null
                    showAddDialog = true
                },
                containerColor = KasirGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "Tambah Kasir")
            }
        }
    ) { padding ->
        if (sortedUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada kasir terdaftar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedUsers, key = { it.username }) { user ->
                    UserRow(
                        user = user,
                        isSelf = user.username == uiState.currentShiftUser,
                        canDelete = user.role != "admin" && user.username != uiState.currentShiftUser,
                        onDelete = { pendingDelete = user }
                    )
                }
            }
        }
    }

    // Add cashier dialog
    if (showAddDialog) {
        AddCashierDialog(
            errorMessage = addError,
            onDismiss = {
                showAddDialog = false
                addError = null
            },
            onSave = { username, password ->
                viewModel.saveCashier(username, password) { ok, msg ->
                    if (ok) {
                        showAddDialog = false
                        addError = null
                    } else {
                        addError = msg
                    }
                }
            }
        )
    }

    // Delete confirmation
    pendingDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus Kasir?") },
            text = { Text("Kasir \"${user.username}\" tidak akan bisa login shift lagi. Lanjutkan?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        viewModel.deleteCashier(user.username)
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Admin PIN verification gate (delete/add when session isn't already admin)
    AdminPinDialog(viewModel = viewModel)
}

@Composable
private fun UserRow(
    user: UserDto,
    isSelf: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = KasirSurfaceCard,
        border = BorderStroke(1.dp, KasirLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initial avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(KasirGreen.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = KasirGreen
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.username, fontWeight = FontWeight.Bold, color = KasirOnSurface)
                    if (isSelf) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = KasirSurfaceVariant, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                "Anda",
                                fontFamily = KasirMono,
                                fontSize = 10.sp,
                                color = KasirTextLow,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                val roleColor = if (user.role == "admin") KasirAccent else KasirGreen
                Surface(
                    color = roleColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, roleColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        user.role.uppercase(),
                        fontFamily = KasirMono,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = roleColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Hapus ${user.username}",
                        tint = KasirError
                    )
                }
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = KasirTextLow,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCashierDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KasirSurfaceCard,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = KasirGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Kasir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Kasir login shift memakai username & password ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KasirTextLow
                )

                Spacer(Modifier.height(16.dp))

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
                        unfocusedBorderColor = KasirLine,
                        unfocusedLabelColor = KasirTextLow
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                        unfocusedBorderColor = KasirLine,
                        unfocusedLabelColor = KasirTextLow
                    )
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = { onSave(username.trim(), password) },
                        enabled = username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
