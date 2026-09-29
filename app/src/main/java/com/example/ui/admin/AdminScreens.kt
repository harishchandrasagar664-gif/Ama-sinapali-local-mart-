@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.admin

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
import com.example.data.local.entities.OrderEntity
import com.example.domain.model.OrderStatus
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PromotionStatus
import com.example.ui.components.OdiaGreetingHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onRoleSwitchClick: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val promotions by viewModel.allPromotions.collectAsState()
    val complaints by viewModel.allComplaints.collectAsState()

    val pendingDelivery = orders.count { it.status == OrderStatus.READY_FOR_PICKUP || it.status == OrderStatus.OUT_FOR_DELIVERY }
    val totalRevenue = orders.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.finalAmount }

    Scaffold(
        topBar = {
            OdiaGreetingHeader(
                currentRole = currentRole,
                onRoleSwitchClick = onRoleSwitchClick,
                userName = user?.name ?: "Harishchandra Sagar (Admin)"
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = true,
                    onClick = { viewModel.navigateTo(Screen.AdminDashboard) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.AdminDelivery) },
                    icon = {
                        BadgedBox(badge = {
                            if (pendingDelivery > 0) Badge { Text("$pendingDelivery") }
                        }) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = "Delivery")
                        }
                    },
                    label = { Text("Deliveries") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.AdminOrders) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Orders") },
                    label = { Text("Orders") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.AdminShops) },
                    icon = { Icon(Icons.Default.Store, contentDescription = "Shops") },
                    label = { Text("Shops") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalTealPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Admin Control & Delivery Center", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Harishchandra Sagar personally manages delivery fulfillment & town orders.", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total GMV: ₹${totalRevenue.toInt()}", color = LocalAmberLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Active Shops: ${shops.size}", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Quick Nav Cards
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(Screen.AdminDelivery) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Delivery Hub", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("$pendingDelivery to deliver", fontSize = 12.sp, color = Color(0xFFE65100), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(Screen.AdminPromotions) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Promotions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${promotions.size} requests", fontSize = 12.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }

            item {
                Text("Recent Town Orders", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            items(orders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Order #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            StatusBadge(order.status)
                        }
                        Text("Customer: ${order.customerName} (${order.customerPhone})", fontSize = 12.sp)
                        Text("Amount: ₹${order.finalAmount.toInt()} (${order.paymentMethod.name.replace('_', ' ')})", fontSize = 12.sp)
                        Text("Address: ${order.deliveryAddress}", fontSize = 11.sp, color = LocalTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDeliveryScreen(viewModel: MainViewModel) {
    val orders by viewModel.allOrders.collectAsState()
    val deliveryOrders = orders.filter { it.status == OrderStatus.READY_FOR_PICKUP || it.status == OrderStatus.OUT_FOR_DELIVERY }

    var selectedOrderForDelivery by remember { mutableStateOf<OrderEntity?>(null) }
    var codCollectedText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Delivery Fulfillment 🛵") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.AdminDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("⚡ Exclusive Admin Delivery Policy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Only Admin (Harishchandra Sagar) can mark an order DELIVERED. For COD orders, confirm and record cash collected to mark paid.",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (deliveryOrders.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No pending deliveries right now. All orders delivered! 🎉", color = LocalTextSecondary)
                    }
                }
            } else {
                items(deliveryOrders) { order ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Order #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                StatusBadge(order.status)
                            }
                            Text("👤 Customer: ${order.customerName} • 📞 ${order.customerPhone}", fontSize = 13.sp)
                            Text("📍 Address: ${order.deliveryAddress}", fontSize = 13.sp)
                            Text(
                                "💰 To Collect: ₹${order.finalAmount.toInt()} (${order.paymentMethod.name.replace('_', ' ')})",
                                fontWeight = FontWeight.Bold,
                                color = LocalTealPrimary,
                                fontSize = 14.sp
                            )

                            if (order.status == OrderStatus.READY_FOR_PICKUP) {
                                Button(
                                    onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.OUT_FOR_DELIVERY) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = LocalAmberSecondary, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Up & Start Delivery (Out for Delivery)")
                                }
                            } else if (order.status == OrderStatus.OUT_FOR_DELIVERY) {
                                Button(
                                    onClick = {
                                        selectedOrderForDelivery = order
                                        codCollectedText = "${order.finalAmount.toInt()}"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = LocalSuccess)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Complete Delivery & Collect Cash", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedOrderForDelivery != null) {
            val order = selectedOrderForDelivery!!
            AlertDialog(
                onDismissRequest = { selectedOrderForDelivery = null },
                title = { Text("Complete Order Delivery") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Order #${order.orderNumber} for ${order.customerName}")
                        Text("Payment Type: ${order.paymentMethod.name.replace('_', ' ')}")
                        if (order.paymentMethod == PaymentMethod.CASH_ON_DELIVERY) {
                            OutlinedTextField(
                                value = codCollectedText,
                                onValueChange = { codCollectedText = it },
                                label = { Text("COD Cash Amount Collected (₹)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("Prepaid via Razorpay. No cash collection required.", color = LocalSuccess, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = codCollectedText.toDoubleOrNull() ?: order.finalAmount
                            viewModel.completeAdminDelivery(order.id, "", "", amount)
                            selectedOrderForDelivery = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalSuccess)
                    ) {
                        Text("Confirm Delivered")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedOrderForDelivery = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun AdminOrdersScreen(viewModel: MainViewModel) {
    val orders by viewModel.allOrders.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Town Orders") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.AdminDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(orders) { order ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Order #${order.orderNumber}", fontWeight = FontWeight.Bold)
                            StatusBadge(order.status)
                        }
                        Text("Customer: ${order.customerName} • ₹${order.finalAmount.toInt()}", fontSize = 13.sp)
                        Text("Delivered by: ${order.deliveryAgentAssigned ?: "Harishchandra Sagar (Admin)"}", fontSize = 11.sp, color = LocalTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminShopsScreen(viewModel: MainViewModel) {
    val shops by viewModel.allShops.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Town Shops") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.AdminDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shops) { shop ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(shop.category, fontSize = 12.sp, color = LocalTextSecondary)
                            Text("Phone: ${shop.phone} • UPI: ${shop.upiId}", fontSize = 11.sp)
                        }
                        Switch(
                            checked = shop.isApproved,
                            onCheckedChange = { viewModel.updateShopApproval(shop, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPromotionsScreen(viewModel: MainViewModel) {
    val promotions by viewModel.allPromotions.collectAsState()
    val shops by viewModel.allShops.collectAsState()

    var filterStatus by remember { mutableStateOf<PromotionStatus?>(null) }
    var editingPromo by remember { mutableStateOf<com.example.data.local.entities.PromotionEntity?>(null) }

    // Edit Dialog States
    var editPrice by remember { mutableStateOf("") }
    var editDuration by remember { mutableStateOf("") }
    var editStartDate by remember { mutableStateOf("") }
    var editEndDate by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }

    val filteredList = remember(promotions, filterStatus) {
        if (filterStatus == null) promotions else promotions.filter { it.status == filterStatus }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promotion Reviews & Approvals") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.AdminDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overview Summary
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Pending", fontSize = 11.sp, color = LocalTextSecondary)
                            Text(
                                "${promotions.count { it.status == PromotionStatus.PENDING_APPROVAL }}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAmberDark
                            )
                        }
                    }
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Approved", fontSize = 11.sp, color = LocalTextSecondary)
                            Text(
                                "${promotions.count { it.status == PromotionStatus.APPROVED || it.status == PromotionStatus.ACTIVE }}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalSuccess
                            )
                        }
                    }
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Value", fontSize = 11.sp, color = LocalTextSecondary)
                            val totalRev = promotions.filter { it.status == PromotionStatus.APPROVED || it.status == PromotionStatus.ACTIVE }.sumOf { it.totalPrice }
                            Text("₹${totalRev.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LocalTealPrimary)
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterStatus == null,
                        onClick = { filterStatus = null },
                        label = { Text("All (${promotions.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == PromotionStatus.PENDING_APPROVAL,
                        onClick = { filterStatus = PromotionStatus.PENDING_APPROVAL },
                        label = { Text("Pending (${promotions.count { it.status == PromotionStatus.PENDING_APPROVAL }})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == PromotionStatus.APPROVED,
                        onClick = { filterStatus = PromotionStatus.APPROVED },
                        label = { Text("Approved (${promotions.count { it.status == PromotionStatus.APPROVED || it.status == PromotionStatus.ACTIVE }})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterStatus == PromotionStatus.REJECTED,
                        onClick = { filterStatus = PromotionStatus.REJECTED },
                        label = { Text("Rejected (${promotions.count { it.status == PromotionStatus.REJECTED }})", fontSize = 11.sp) }
                    )
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No promotion requests in this section.", color = LocalTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }

            items(filteredList) { promo ->
                val associatedShop = shops.find { it.id == promo.shopId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    promo.shopName.ifBlank { associatedShop?.name ?: "Shop #${promo.shopId}" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LocalTealDark
                                )
                                Surface(
                                    color = LocalTealPrimary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        promo.type.name.replace('_', ' '),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalTealDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            // Status Badge
                            Surface(
                                color = when (promo.status) {
                                    PromotionStatus.APPROVED, PromotionStatus.ACTIVE -> LocalSuccess.copy(alpha = 0.15f)
                                    PromotionStatus.PENDING_APPROVAL -> LocalAmberSecondary.copy(alpha = 0.25f)
                                    PromotionStatus.REJECTED -> LocalError.copy(alpha = 0.15f)
                                    else -> Color.LightGray.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when (promo.status) {
                                        PromotionStatus.APPROVED, PromotionStatus.ACTIVE -> "✓ Approved"
                                        PromotionStatus.PENDING_APPROVAL -> "⏳ Pending Approval"
                                        PromotionStatus.REJECTED -> "✕ Rejected"
                                        else -> promo.status.name
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (promo.status) {
                                        PromotionStatus.APPROVED, PromotionStatus.ACTIVE -> LocalSuccess
                                        PromotionStatus.PENDING_APPROVAL -> LocalAmberDark
                                        PromotionStatus.REJECTED -> LocalError
                                        else -> LocalTextSecondary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(promo.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (promo.description.isNotBlank()) {
                            Text(promo.description, fontSize = 12.sp, color = LocalTextSecondary)
                        }

                        if (!promo.targetProductName.isNullOrBlank()) {
                            Text("Target Product: ${promo.targetProductName}", fontSize = 12.sp, color = LocalTealPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Duration & Dates:", fontSize = 11.sp, color = LocalTextSecondary)
                                    Text("${promo.durationDays} Days (${promo.startDate.ifBlank { "Immediate" }} to ${promo.endDate.ifBlank { "Ongoing" }})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Target Location:", fontSize = 11.sp, color = LocalTextSecondary)
                                    Text("📍 ${promo.location}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Set Price:", fontSize = 11.sp, color = LocalTextSecondary)
                                    Text("₹${promo.totalPrice.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LocalTealPrimary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Payment:", fontSize = 11.sp, color = LocalTextSecondary)
                                    Text(promo.paymentStatus, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (promo.paymentStatus.contains("PAID", ignoreCase = true)) LocalSuccess else LocalAmberDark)
                                }
                            }
                        }

                        if (promo.adminNotes.isNotBlank()) {
                            Text("Admin Notes: ${promo.adminNotes}", fontSize = 11.sp, color = LocalTextSecondary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editingPromo = promo
                                    editPrice = promo.totalPrice.toInt().toString()
                                    editDuration = promo.durationDays.toString()
                                    editStartDate = promo.startDate.ifBlank { "2026-09-29" }
                                    editEndDate = promo.endDate.ifBlank { "2026-10-06" }
                                    editLocation = promo.location
                                    editNotes = promo.adminNotes
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Review & Edit Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            if (promo.status == PromotionStatus.PENDING_APPROVAL) {
                                Button(
                                    onClick = {
                                        viewModel.updatePromotionDetailsByAdmin(
                                            id = promo.id,
                                            status = PromotionStatus.APPROVED,
                                            price = promo.totalPrice,
                                            duration = promo.durationDays,
                                            startDate = promo.startDate,
                                            endDate = promo.endDate,
                                            location = promo.location,
                                            notes = "Approved by Admin Harishchandra Sagar",
                                            shopOwnerUserId = associatedShop?.ownerId,
                                            promoTitle = promo.title
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LocalSuccess),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updatePromotionDetailsByAdmin(
                                            id = promo.id,
                                            status = PromotionStatus.REJECTED,
                                            price = promo.totalPrice,
                                            duration = promo.durationDays,
                                            startDate = promo.startDate,
                                            endDate = promo.endDate,
                                            location = promo.location,
                                            notes = "Rejected by Admin",
                                            shopOwnerUserId = associatedShop?.ownerId,
                                            promoTitle = promo.title
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reject", fontSize = 12.sp, color = LocalError)
                                }
                            } else {
                                TextButton(
                                    onClick = {
                                        val newPay = if (promo.paymentStatus.contains("PAID", ignoreCase = true)) "Pending Payment" else "PAID"
                                        viewModel.updatePromotionPaymentStatus(promo.id, newPay)
                                    }
                                ) {
                                    Text(
                                        if (promo.paymentStatus.contains("PAID", ignoreCase = true)) "Mark Unpaid" else "Mark Paid ✓",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ADMIN REVIEW & EDIT DIALOG: Allows Admin to review, approve/reject, set/edit price, date, duration and location
        editingPromo?.let { promo ->
            val associatedShop = shops.find { it.id == promo.shopId }
            AlertDialog(
                onDismissRequest = { editingPromo = null },
                title = { Text("Review & Edit Promotion (${promo.shopName})") },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text("Format: ${promo.type.name.replace('_', ' ')}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LocalTealDark)
                            Text("Title: ${promo.title}", fontSize = 12.sp)
                            HorizontalDivider()
                        }
                        item {
                            OutlinedTextField(
                                value = editPrice,
                                onValueChange = { editPrice = it },
                                label = { Text("Set / Edit Total Price (₹)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editDuration,
                                onValueChange = { editDuration = it },
                                label = { Text("Duration (Days)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editStartDate,
                                    onValueChange = { editStartDate = it },
                                    label = { Text("Start Date") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = editEndDate,
                                    onValueChange = { editEndDate = it },
                                    label = { Text("End Date") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = editLocation,
                                onValueChange = { editLocation = it },
                                label = { Text("Set / Edit Location") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editNotes,
                                onValueChange = { editNotes = it },
                                label = { Text("Admin Review Notes") },
                                placeholder = { Text("e.g. Approved with revised festival pricing.") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                val price = editPrice.toDoubleOrNull() ?: promo.totalPrice
                                val dur = editDuration.toIntOrNull() ?: promo.durationDays
                                viewModel.updatePromotionDetailsByAdmin(
                                    id = promo.id,
                                    status = PromotionStatus.APPROVED,
                                    price = price,
                                    duration = dur,
                                    startDate = editStartDate,
                                    endDate = editEndDate,
                                    location = editLocation,
                                    notes = editNotes.ifBlank { "Approved by Admin Harishchandra Sagar" },
                                    shopOwnerUserId = associatedShop?.ownerId,
                                    promoTitle = promo.title
                                )
                                editingPromo = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LocalSuccess)
                        ) {
                            Text("Approve")
                        }
                        Button(
                            onClick = {
                                val price = editPrice.toDoubleOrNull() ?: promo.totalPrice
                                val dur = editDuration.toIntOrNull() ?: promo.durationDays
                                viewModel.updatePromotionDetailsByAdmin(
                                    id = promo.id,
                                    status = PromotionStatus.REJECTED,
                                    price = price,
                                    duration = dur,
                                    startDate = editStartDate,
                                    endDate = editEndDate,
                                    location = editLocation,
                                    notes = editNotes.ifBlank { "Rejected by Admin" },
                                    shopOwnerUserId = associatedShop?.ownerId,
                                    promoTitle = promo.title
                                )
                                editingPromo = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LocalError)
                        ) {
                            Text("Reject")
                        }
                        FilledTonalButton(
                            onClick = {
                                val price = editPrice.toDoubleOrNull() ?: promo.totalPrice
                                val dur = editDuration.toIntOrNull() ?: promo.durationDays
                                viewModel.updatePromotionDetailsByAdmin(
                                    id = promo.id,
                                    status = promo.status,
                                    price = price,
                                    duration = dur,
                                    startDate = editStartDate,
                                    endDate = editEndDate,
                                    location = editLocation,
                                    notes = editNotes,
                                    shopOwnerUserId = associatedShop?.ownerId,
                                    promoTitle = promo.title
                                )
                                editingPromo = null
                            }
                        ) {
                            Text("Save")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingPromo = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
