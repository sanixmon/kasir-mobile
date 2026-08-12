package com.kasir.mobile.ui.screen.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kasir.mobile.ui.theme.KasirGreen
import com.kasir.mobile.ui.theme.KasirSurface
import com.kasir.mobile.ui.theme.KasirSurfaceVariant
import java.text.NumberFormat
import java.util.Locale

data class CartItem(
    val id: Long,
    val name: String,
    val price: Long,
    var qty: Int,
    val stock: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(navController: NavController) {
    var cart by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val total = cart.sumOf { it.price * it.qty }
    val idrFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Point of Sale", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (cart.isNotEmpty()) Badge { Text(cart.sumOf { it.qty }.toString()) }
                        }
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart")
                    }
                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KasirSurfaceVariant)
            )
        },
        bottomBar = {
            if (cart.isNotEmpty()) {
                Surface(
                    color = KasirSurfaceVariant,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total", style = MaterialTheme.typography.labelMedium)
                            Text(
                                idrFormat.format(total),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = KasirGreen
                            )
                        }
                        Button(
                            onClick = { showCheckoutDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
                        ) {
                            Text("Bayar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        containerColor = KasirSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (cart.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Keranjang kosong",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Pilih barang dari inventori",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                Text(
                    "Keranjang (${cart.sumOf { it.qty }} item)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cart, key = { it.id }) { item ->
                        CartItemCard(
                            item = item,
                            onIncrement = {
                                cart = cart.map {
                                    if (it.id == item.id && it.qty < it.stock) it.copy(qty = it.qty + 1) else it
                                }
                            },
                            onDecrement = {
                                cart = cart.mapNotNull {
                                    if (it.id == item.id) {
                                        if (it.qty > 1) it.copy(qty = it.qty - 1) else null
                                    } else it
                                }
                            },
                            idrFormat = idrFormat
                        )
                    }
                }
            }
        }
    }

    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = { Text("Konfirmasi Pembayaran") },
            text = {
                Column {
                    Text("Total: ${idrFormat.format(total)}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    cart.forEach { item ->
                        Text("• ${item.name} x${item.qty} = ${idrFormat.format(item.price * item.qty)}")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        // TODO: call repository.sellItems(cart) via ViewModel
                        showCheckoutDialog = false
                        cart = emptyList()
                        isSubmitting = false
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = KasirGreen)
                ) { Text("Bayar") }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    idrFormat: NumberFormat
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text(
                    idrFormat.format(item.price),
                    style = MaterialTheme.typography.bodySmall,
                    color = KasirGreen
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = "Kurang", modifier = Modifier.size(18.dp))
                }
                Text(
                    item.qty.toString(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(32.dp),
                    enabled = item.qty < item.stock
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
