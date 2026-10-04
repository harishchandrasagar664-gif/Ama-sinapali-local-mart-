@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.OrderEntity
import com.example.data.firebase.FirestoreSyncService
import com.example.data.firebase.FirebaseSyncConfig
import com.example.domain.model.DeliveryType
import com.example.domain.model.OrderStatus
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PaymentStatus
import com.example.domain.model.PromotionStatus
import com.example.ui.components.OdiaGreetingHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val placedOrdersCount = orders.count { it.status == OrderStatus.PLACED }
    val pendingDelivery = orders.count { it.status == OrderStatus.READY_FOR_PICKUP || it.status == OrderStatus.OUT_FOR_DELIVERY }
    val totalRevenue = orders.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.finalAmount }

    LaunchedEffect(Unit) {
        viewModel.verifyFirebaseConnection()
        viewModel.syncOrdersFromCloud()
        while (true) {
            kotlinx.coroutines.delay(3000)
            viewModel.syncOrdersFromCloud()
        }
    }

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
                    icon = {
                        BadgedBox(badge = {
                            if (orders.isNotEmpty()) Badge { Text("${orders.size}") }
                        }) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = "Orders")
                        }
                    },
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

            // Quick Nav Cards: Orders, Delivery Hub, Promotions
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(Screen.AdminOrders) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = LocalTealPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Town Orders", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${orders.size} total ($placedOrdersCount new)", fontSize = 11.sp, color = LocalTealDark, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(Screen.AdminDelivery) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Delivery Hub", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("$pendingDelivery to deliver", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(Screen.AdminPromotions) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Promotions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${promotions.size} requests", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Town Orders (${orders.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (orders.isNotEmpty()) {
                        TextButton(onClick = { viewModel.navigateTo(Screen.AdminOrders) }) {
                            Text("View All", fontSize = 12.sp, color = LocalTealPrimary)
                        }
                    }
                }
            }

            if (orders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(40.dp), tint = LocalTextSecondary)
                            Text("No customer orders yet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Orders placed by customers will appear here immediately.", fontSize = 12.sp, color = LocalTextSecondary)
                        }
                    }
                }
            } else {
                items(orders, key = { it.orderNumber.ifBlank { it.id.toString() } }) { order ->
                    val shop = shops.find { it.id == order.shopId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.AdminOrders) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Order #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                StatusBadge(order.status)
                            }
                            Text("Shop: ${shop?.name ?: "Sinapali Mart"}", fontSize = 12.sp, color = LocalTealPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Customer: ${order.customerName} (${order.customerPhone})", fontSize = 12.sp)
                            Text("Amount: ₹${order.finalAmount.toInt()} (${order.paymentMethod.name.replace('_', ' ')})", fontSize = 12.sp)
                            Text("Address: ${order.deliveryAddress}", fontSize = 11.sp, color = LocalTextSecondary)
                        }
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
    val context = LocalContext.current
    val orders by viewModel.allOrders.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val allOrderItems by viewModel.allOrderItems.collectAsState()
    val isSyncing by viewModel.isOrderSyncing.collectAsState()
    val lastSyncTime by viewModel.lastOrderSyncTime.collectAsState()
    val syncStatusMsg by viewModel.orderSyncStatusMessage.collectAsState()
    val firebaseReport by viewModel.firebaseVerificationReport.collectAsState()
    val isVerifyingFirebase by viewModel.isVerifyingFirebase.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }
    var inputProjectId by remember { mutableStateOf(com.example.data.firebase.FirebaseSyncConfig.FIREBASE_PROJECT_ID) }

    var selectedShopId by remember { mutableStateOf<Long?>(null) }
    var selectedStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusMenuOrderId by remember { mutableStateOf<Long?>(null) }

    val shopMap = remember(shops) { shops.associateBy { it.id } }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    // Automatic live sync and continuous polling every 3 seconds while viewing Admin Orders
    LaunchedEffect(Unit) {
        viewModel.verifyFirebaseConnection()
        viewModel.syncOrdersFromCloud()
        while (true) {
            kotlinx.coroutines.delay(3000)
            viewModel.syncOrdersFromCloud()
        }
    }

    // Filter by shop
    val shopFiltered = remember(orders, selectedShopId) {
        if (selectedShopId == null) orders else orders.filter { it.shopId == selectedShopId }
    }

    // Filter by status
    val statusFiltered = remember(shopFiltered, selectedStatus) {
        if (selectedStatus == null) shopFiltered else shopFiltered.filter { it.status == selectedStatus }
    }

    // Filter by search query
    val finalOrders = remember(statusFiltered, searchQuery, shopMap) {
        if (searchQuery.isBlank()) {
            statusFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            statusFiltered.filter { order ->
                order.orderNumber.lowercase().contains(q) ||
                order.customerName.lowercase().contains(q) ||
                order.customerPhone.lowercase().contains(q) ||
                order.deliveryAddress.lowercase().contains(q) ||
                (shopMap[order.shopId]?.name?.lowercase()?.contains(q) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("All Town Orders", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${finalOrders.size} of ${orders.size} orders across Sinapali",
                            fontSize = 12.sp,
                            color = LocalTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.AdminDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncOrdersFromCloud() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = LocalTealPrimary
                            )
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync orders from cloud")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Multi-Device Cloud Sync Info Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (firebaseReport?.isConnected == true)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = if (firebaseReport?.isConnected == true) LocalTealPrimary else MaterialTheme.colorScheme.error
                                )
                                Column {
                                    Text("Firebase Firestore Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val isConn = firebaseReport?.isConnected == true
                                    val isReal = firebaseReport?.isRealGoogleCloudProject == true
                                    val badgeText = when {
                                        isConn && firebaseReport?.state == FirestoreSyncService.FirebaseConnectionState.CONNECTED ->
                                            "🟢 Real Firebase Project Connected"
                                        isReal || firebaseReport?.state == FirestoreSyncService.FirebaseConnectionState.VERIFIED_AUTH_REQUIRED ->
                                            "🟢 Real Project Verified (${firebaseReport?.projectId})"
                                        firebaseReport?.state == FirestoreSyncService.FirebaseConnectionState.NOT_CONFIGURED ->
                                            "🔴 Not Configured • Enter Project ID"
                                        else ->
                                            "🟡 Checking Endpoint (${firebaseReport?.projectId})"
                                    }
                                    Text(
                                        badgeText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isConn || isReal) LocalSuccess else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (isSyncing || isVerifyingFirebase) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = LocalTealPrimary
                                )
                            }
                        }

                        // Project & Status details
                        Text(
                            firebaseReport?.statusMessage
                                ?: "Real Google Cloud Firestore connection verification active. Zero mock data.",
                            fontSize = 12.sp,
                            color = LocalTextSecondary,
                            lineHeight = 16.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Project: ${firebaseReport?.projectId ?: com.example.data.firebase.FirebaseSyncConfig.FIREBASE_PROJECT_ID}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = LocalTealPrimary
                            )
                            Text(
                                "Endpoint: firestore.googleapis.com",
                                fontSize = 10.sp,
                                color = LocalTextSecondary
                            )
                        }

                        Text(
                            "Local Data Guarantee: 100% of customer orders, shops, and products are safely preserved in Room SQLite without deletion or reset.",
                            fontSize = 11.sp,
                            color = LocalTextSecondary
                        )

                        // Action buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.verifyFirebaseConnection() },
                                enabled = !isVerifyingFirebase,
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verify Real", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { showConfigDialog = true },
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Project ID", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.syncOrdersFromCloud() },
                                enabled = !isSyncing,
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Project ID Configuration Dialog
            if (showConfigDialog) {
                item {
                    AlertDialog(
                        onDismissRequest = { showConfigDialog = false },
                        title = { Text("Configure Firebase Project") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Enter your real Firebase / Google Cloud Project ID from console.firebase.google.com:",
                                    fontSize = 13.sp
                                )
                                OutlinedTextField(
                                    value = inputProjectId,
                                    onValueChange = { inputProjectId = it },
                                    label = { Text("Firebase Project ID") },
                                    placeholder = { Text("e.g. my-firebase-app-123") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "Note: All local data remains preserved. Firestore will be probed directly via REST API.",
                                    fontSize = 11.sp,
                                    color = LocalTextSecondary
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.setFirebaseProjectId(inputProjectId)
                                showConfigDialog = false
                            }) {
                                Text("Save & Verify")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfigDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by Order #, Customer, Phone, Shop...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Shop Filter (All Shops or Specific Shop)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Filter by Shop:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedShopId == null,
                                onClick = { selectedShopId = null },
                                label = { Text("All Shops (${orders.size})") }
                            )
                        }
                        items(shops) { shop ->
                            val shopOrderCount = orders.count { it.shopId == shop.id }
                            FilterChip(
                                selected = selectedShopId == shop.id,
                                onClick = { selectedShopId = if (selectedShopId == shop.id) null else shop.id },
                                label = { Text("${shop.name} ($shopOrderCount)") }
                            )
                        }
                    }
                }
            }

            // Order Status Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Filter by Status:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedStatus == null,
                                onClick = { selectedStatus = null },
                                label = { Text("All (${shopFiltered.size})") }
                            )
                        }
                        items(OrderStatus.values()) { status ->
                            val count = shopFiltered.count { it.status == status }
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = if (selectedStatus == status) null else status },
                                label = { Text("${status.name.replace('_', ' ')} ($count)") }
                            )
                        }
                    }
                }
            }

            // Orders List
            if (finalOrders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp), tint = LocalTextSecondary)
                            Text("No orders match the selected filters", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Try clearing filters or search to view other orders.", fontSize = 12.sp, color = LocalTextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (selectedShopId != null || selectedStatus != null || searchQuery.isNotBlank()) {
                                    OutlinedButton(onClick = {
                                        selectedShopId = null
                                        selectedStatus = null
                                        searchQuery = ""
                                    }) {
                                        Text("Clear Filters")
                                    }
                                }
                                Button(onClick = { viewModel.syncOrdersFromCloud() }) {
                                    Text("Refresh from Cloud")
                                }
                            }
                        }
                    }
                }
            } else {
                items(finalOrders, key = { it.orderNumber.ifBlank { it.id.toString() } }) { order ->
                    val shop = shopMap[order.shopId]
                    val items = allOrderItems.filter { it.orderId == order.id }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header: Order #, Date, Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Order #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    val formattedDate = try {
                                        dateFormat.format(Date(order.placedAt))
                                    } catch (e: Exception) {
                                        "Recently placed"
                                    }
                                    Text("Placed: $formattedDate", fontSize = 11.sp, color = LocalTextSecondary)
                                }
                                StatusBadge(order.status)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Shop Information
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LocalTealPrimary.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp), tint = LocalTealPrimary)
                                    Text(
                                        "Shop: ${shop?.name ?: "Shop #${order.shopId}"} (${shop?.category ?: "Local Vendor"})",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = LocalTealPrimary
                                    )
                                }
                            }

                            // Customer Details with Direct Call Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("👤 Customer: ${order.customerName}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("📞 Phone: ${order.customerPhone}", fontSize = 12.sp, color = LocalTextSecondary)
                                    Text("📍 Address: ${order.deliveryAddress}", fontSize = 12.sp, color = LocalTextSecondary)
                                }

                                if (order.customerPhone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customerPhone}"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                // Handle no dialer
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call Customer", tint = Color(0xFF2E7D32))
                                    }
                                }
                            }

                            // Ordered Items List
                            if (items.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Ordered Products (${items.sumOf { it.quantity }} items):",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    for (item in items) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• ${item.quantity}x ${item.productName}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Text("₹${item.totalPrice.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(14.dp), tint = LocalTextSecondary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (order.orderNotes.isNotBlank()) "Order: ${order.orderNotes}" else "Town Order Items • Total ₹${order.finalAmount.toInt()}",
                                            fontSize = 12.sp,
                                            color = LocalTextSecondary
                                        )
                                    }
                                }
                            }

                            // Financial & Delivery Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "🛵 ${if (order.deliveryType == DeliveryType.HOME_DELIVERY) "Home Delivery" else "Store Pickup"}",
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "💳 ${order.paymentMethod.name.replace('_', ' ')} • ${order.paymentStatus.name}",
                                        fontSize = 11.sp,
                                        color = if (order.paymentStatus == PaymentStatus.PAID) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Total: ₹${order.finalAmount.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = LocalTealPrimary
                                    )
                                    if (order.discountAmount > 0) {
                                        Text(
                                            "Saved ₹${order.discountAmount.toInt()}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }

                            if (order.orderNotes.isNotBlank()) {
                                Text("📝 Note: ${order.orderNotes}", fontSize = 11.sp, color = LocalTextSecondary)
                            }

                            Text(
                                "🛵 Delivery Agent: ${order.deliveryAgentAssigned ?: "Harishchandra Sagar (Admin)"}",
                                fontSize = 11.sp,
                                color = LocalTextSecondary
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Admin Action Controls (Change Status)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Admin Status Actions:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when (order.status) {
                                        OrderStatus.PLACED -> {
                                            Button(
                                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.ACCEPTED) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Accept Order", fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.CANCELLED) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Text("Cancel", fontSize = 12.sp)
                                            }
                                        }
                                        OrderStatus.ACCEPTED -> {
                                            Button(
                                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.PREPARING) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Mark Preparing 🍳", fontSize = 12.sp)
                                            }
                                        }
                                        OrderStatus.PREPARING -> {
                                            Button(
                                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.READY_FOR_PICKUP) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Ready for Pickup 🛍️", fontSize = 12.sp)
                                            }
                                        }
                                        OrderStatus.READY_FOR_PICKUP -> {
                                            Button(
                                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.OUT_FOR_DELIVERY) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                            ) {
                                                Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Out for Delivery 🛵", fontSize = 12.sp)
                                            }
                                        }
                                        OrderStatus.OUT_FOR_DELIVERY -> {
                                            Button(
                                                onClick = { viewModel.completeAdminDelivery(order.id, "", "", order.finalAmount) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Mark Delivered & Paid 📦", fontSize = 12.sp)
                                            }
                                        }
                                        OrderStatus.DELIVERED -> {
                                            SuggestionChip(
                                                onClick = { },
                                                label = { Text("✅ Order Delivered & Fulfilled") },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        OrderStatus.CANCELLED -> {
                                            SuggestionChip(
                                                onClick = { },
                                                label = { Text("❌ Order Cancelled") },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // Dropdown menu button to change status to any value
                                    Box {
                                        IconButton(onClick = { statusMenuOrderId = order.id }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Change Status")
                                        }

                                        DropdownMenu(
                                            expanded = statusMenuOrderId == order.id,
                                            onDismissRequest = { statusMenuOrderId = null }
                                        ) {
                                            Text("Set Status:", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = LocalTextSecondary)
                                            for (s in OrderStatus.values()) {
                                                DropdownMenuItem(
                                                    text = { Text(s.name.replace('_', ' ')) },
                                                    onClick = {
                                                        statusMenuOrderId = null
                                                        viewModel.updateOrderStatus(order.id, s)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
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
