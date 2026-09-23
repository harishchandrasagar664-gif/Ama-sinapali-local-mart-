@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProductEntity
import com.example.domain.model.OrderStatus
import com.example.domain.model.PromotionType
import com.example.ui.components.OdiaGreetingHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun ShopOwnerDashboardScreen(
    viewModel: MainViewModel,
    onRoleSwitchClick: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    val products by viewModel.allProducts.collectAsState()

    val myShop = shops.find { it.ownerId == (user?.id ?: 2) } ?: shops.firstOrNull()
    val myOrders = orders.filter { it.shopId == (myShop?.id ?: 1) }
    val myProducts = products.filter { it.shopId == (myShop?.id ?: 1) }

    Scaffold(
        topBar = {
            OdiaGreetingHeader(
                currentRole = currentRole,
                onRoleSwitchClick = onRoleSwitchClick,
                userName = "${user?.name ?: "Shop Owner"} (${myShop?.name ?: ""})"
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = true,
                    onClick = { viewModel.navigateTo(Screen.ShopOwnerDashboard) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.ShopOrderManagement) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Orders") },
                    label = { Text("Orders") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.ShopProductManagement) },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Products") },
                    label = { Text("Products") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.ShopOffersPromotions) },
                    icon = { Icon(Icons.Default.LocalOffer, contentDescription = "Promotions") },
                    label = { Text("Promotions") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Pending Orders", fontSize = 12.sp, color = LocalTextSecondary)
                            Text("${myOrders.count { it.status == OrderStatus.PLACED || it.status == OrderStatus.PREPARING }}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LocalTealPrimary)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Total Products", fontSize = 12.sp, color = LocalTextSecondary)
                            Text("${myProducts.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LocalAmberDark)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("💡 Town Delivery Policy Notice", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "As a shop owner, you accept & prepare items. Admin (Harishchandra Sagar) personally delivers to customers and marks order DELIVERED.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Text("Recent Orders from Customers", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            items(myOrders) { order ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Order #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            StatusBadge(order.status)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Customer: ${order.customerName} (${order.customerPhone})", fontSize = 12.sp)
                        Text("Total: ₹${order.finalAmount.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                        if (order.status == OrderStatus.PLACED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.PREPARING) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                                ) {
                                    Text("Accept & Prepare")
                                }
                            }
                        } else if (order.status == OrderStatus.PREPARING) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.READY_FOR_PICKUP) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LocalAmberSecondary, contentColor = Color.Black)
                            ) {
                                Text("Mark Ready for Admin Pickup")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopProductManagementScreen(viewModel: MainViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val myShop = shops.find { it.ownerId == (user?.id ?: 2) } ?: shops.firstOrNull()
    val myProducts = products.filter { it.shopId == (myShop?.id ?: 1) }

    var showAddDialog by remember { mutableStateOf(false) }
    var prodName by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodCat by remember { mutableStateOf("Groceries & Spices") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Shop Products") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = LocalTealPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(myProducts) { prod ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("₹${prod.price.toInt()} • Stock: ${prod.stock}", fontSize = 12.sp, color = LocalTextSecondary)
                            Text(if (prod.isAvailable) "In Stock" else "Out of Stock", color = if (prod.isAvailable) LocalSuccess else LocalError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = prod.isAvailable,
                            onCheckedChange = { viewModel.toggleProductStock(prod) }
                        )
                    }
                }
            }
        }

        if (showAddDialog && myShop != null) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add New Product") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = prodName, onValueChange = { prodName = it }, label = { Text("Product Name") })
                        OutlinedTextField(value = prodPrice, onValueChange = { prodPrice = it }, label = { Text("Price (₹)") })
                        OutlinedTextField(value = prodCat, onValueChange = { prodCat = it }, label = { Text("Category") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val price = prodPrice.toDoubleOrNull() ?: 100.0
                        val newProd = ProductEntity(
                            shopId = myShop.id,
                            name = prodName,
                            category = prodCat,
                            description = "Freshly supplied by ${myShop.name}",
                            price = price,
                            unit = "1 unit",
                            stock = 20
                        )
                        viewModel.addOrUpdateProduct(newProd)
                        showAddDialog = false
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ShopOffersPromotionsScreen(viewModel: MainViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val myShop = shops.find { it.ownerId == (user?.id ?: 2) } ?: shops.firstOrNull()

    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Promotion / Banner") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Request Hero Banner Promotion", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Admin (Harishchandra Sagar) reviews and approves shop banners and top placement on Sinapali Mart.", fontSize = 12.sp, color = LocalTextSecondary)

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Promotion Offer Details") },
                placeholder = { Text("e.g. 15% discount on all sweets for Nuakhai / Puja festival") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Button(
                onClick = {
                    if (notes.isNotBlank() && myShop != null) {
                        viewModel.submitPromotionRequest(myShop, PromotionType.BANNER, myShop.id, myShop.name, notes)
                        notes = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
            ) {
                Text("Submit Request to Admin", fontWeight = FontWeight.Bold)
            }
        }
    }
}
