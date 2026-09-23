package com.example.ui.shopowner

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.OfferEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.ShopEntity
import com.example.domain.model.DeliveryType
import com.example.domain.model.OrderStatus
import com.example.domain.model.PromotionType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.StatCard
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopOwnerDashboardScreen(
    viewModel: MainViewModel,
    onRoleSwitchClick: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    val offers by viewModel.offers.collectAsState()

    // Find current owner's shop (default to first shop if matching owner)
    val ownerShop = remember(shops, currentUser) {
        shops.find { it.ownerId == (currentUser?.id ?: 0L) } ?: shops.firstOrNull()
    }

    if (ownerShop == null) {
        EmptyStateView(
            icon = Icons.Default.Store,
            title = "No Shop Found",
            message = "You don't have a registered shop yet.",
            actionText = "Switch Mode",
            onAction = onRoleSwitchClick
        )
        return
    }

    val shopProducts = remember(products, ownerShop) {
        products.filter { it.shopId == ownerShop.id }
    }
    val shopOrders = remember(orders, ownerShop) {
        orders.filter { it.shopId == ownerShop.id }
    }
    val shopOffers = remember(offers, ownerShop) {
        offers.filter { it.shopId == ownerShop.id }
    }

    val pendingOrders = shopOrders.filter { it.orderStatus == OrderStatus.ORDER_PLACED || it.orderStatus == OrderStatus.ORDER_CONFIRMED || it.orderStatus == OrderStatus.PREPARING }
    val completedOrders = shopOrders.filter { it.orderStatus == OrderStatus.DELIVERED }
    val totalRevenue = completedOrders.sumOf { it.totalAmount }
    val lowStockCount = shopProducts.count { it.stock < 10 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ownerShop.shopName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Shop Owner Portal • Odisha", fontSize = 11.sp, color = LocalTealPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = onRoleSwitchClick) {
                        Text("Switch Role", color = LocalTealPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LocalBackgroundLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Shop Status Banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(LocalTealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(ownerShop.shopName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Delivery Radius: ${ownerShop.deliveryRadiusKm} km", fontSize = 11.sp, color = LocalTextSecondary)
                            }
                        }

                        // Open / Closed Toggle
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (ownerShop.isOpen) LocalSuccess.copy(alpha = 0.15f) else LocalError.copy(alpha = 0.15f),
                            modifier = Modifier.clickable {
                                viewModel.launch {
                                    viewModel.repository.updateShop(ownerShop.copy(isOpen = !ownerShop.isOpen))
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (ownerShop.isOpen) LocalSuccess else LocalError)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (ownerShop.isOpen) "Open" else "Closed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (ownerShop.isOpen) LocalSuccess else LocalError
                                )
                            }
                        }
                    }
                }
            }

            // Quick Stats Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Pending Orders",
                        value = "${pendingOrders.size}",
                        icon = Icons.Default.PendingActions,
                        iconTint = LocalAmberSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Sales",
                        value = "₹${totalRevenue.toInt()}",
                        icon = Icons.Default.CurrencyRupee,
                        iconTint = LocalSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Products",
                        value = "${shopProducts.size}",
                        icon = Icons.Default.Inventory2,
                        iconTint = LocalTealPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Low Stock (<10)",
                        value = "$lowStockCount",
                        icon = Icons.Default.WarningAmber,
                        iconTint = if (lowStockCount > 0) LocalError else LocalSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Nav Action Buttons
            item {
                Text(
                    text = "Management Portals",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo(Screen.ShopProductManagement) },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Products", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { viewModel.navigateTo(Screen.ShopOrderManagement) },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAmberSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Orders (${pendingOrders.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.ShopOffersPromotions) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Offers & Ads", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.ShopProfile) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shop Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Recent Orders Header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Incoming Orders (${shopOrders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.navigateTo(Screen.ShopOrderManagement) }) {
                        Text("View All", fontSize = 12.sp, color = LocalTealPrimary)
                    }
                }
            }

            if (shopOrders.isEmpty()) {
                item {
                    Text("No orders placed for this shop yet.", fontSize = 12.sp, color = LocalTextSecondary)
                }
            } else {
                items(shopOrders.take(3)) { order ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LocalDivider),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.ShopOrderManagement) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(order.orderNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Customer: ${order.customerName} (${order.customerPhone})", fontSize = 11.sp, color = LocalTextSecondary)
                                Text("₹${order.totalAmount.toInt()} • ${order.paymentMethod.label}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OrderStatusBadge(status = order.orderStatus)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopProductManagementScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val products by viewModel.allProducts.collectAsState()

    val ownerShop = remember(shops, currentUser) {
        shops.find { it.ownerId == (currentUser?.id ?: 0L) } ?: shops.firstOrNull()
    }
    val shopProducts = remember(products, ownerShop) {
        products.filter { it.shopId == (ownerShop?.id ?: 0L) }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Management (${shopProducts.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddDialog = true
                },
                containerColor = LocalTealPrimary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LocalBackgroundLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shopProducts) { product ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LocalTealPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(product.category),
                                    contentDescription = null,
                                    tint = LocalTealPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                Text("Category: ${product.category}", fontSize = 11.sp, color = LocalTextSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${(product.discountPrice ?: product.price).toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = LocalAmberSecondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Stock: ${product.stock}",
                                        fontSize = 11.sp,
                                        color = if (product.stock < 10) LocalError else LocalSuccess,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Action buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stock Toggle
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (product.isAvailable) LocalSuccess.copy(alpha = 0.12f) else LocalError.copy(alpha = 0.12f),
                                modifier = Modifier.clickable { viewModel.toggleProductStock(product) }
                            ) {
                                Text(
                                    text = if (product.isAvailable) "In Stock" else "Out of Stock",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.isAvailable) LocalSuccess else LocalError,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingProduct = product
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = LocalTealPrimary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteProduct(product) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = LocalError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog && ownerShop != null) {
        ProductFormDialog(
            initialProduct = editingProduct,
            shop = ownerShop,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.addOrUpdateProduct(it)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopOrderManagementScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val orders by viewModel.allOrders.collectAsState()

    val ownerShop = remember(shops, currentUser) {
        shops.find { it.ownerId == (currentUser?.id ?: 0L) } ?: shops.firstOrNull()
    }
    val shopOrders = remember(orders, ownerShop) {
        orders.filter { it.shopId == (ownerShop?.id ?: 0L) }
    }

    var selectedFilter by remember { mutableStateOf<OrderStatus?>(null) }
    val filteredOrders = remember(shopOrders, selectedFilter) {
        if (selectedFilter == null) shopOrders else shopOrders.filter { it.orderStatus == selectedFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop Orders (${shopOrders.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LocalBackgroundLight)
        ) {
            // Status filters
            ScrollableTabRow(
                selectedTabIndex = if (selectedFilter == null) 0 else OrderStatus.values().indexOf(selectedFilter) + 1,
                edgePadding = 16.dp,
                containerColor = Color.White
            ) {
                Tab(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    text = { Text("All (${shopOrders.size})") }
                )
                OrderStatus.values().forEach { status ->
                    val count = shopOrders.count { it.orderStatus == status }
                    Tab(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = status },
                        text = { Text("${status.label} ($count)") }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders) { order ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LocalDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(order.orderNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                OrderStatusBadge(status = order.orderStatus)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Customer: ${order.customerName} • 📞 +91 ${order.customerPhone}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Address: ${order.deliveryAddress}", fontSize = 11.sp, color = LocalTextSecondary)
                            Text("Fulfillment: ${order.deliveryType.label}", fontSize = 11.sp, color = LocalTealPrimary, fontWeight = FontWeight.SemiBold)

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount: ₹${order.totalAmount.toInt()} (${order.paymentMethod.label})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(order.paymentStatus.label, fontSize = 12.sp, color = if (order.paymentStatus == com.example.domain.model.PaymentStatus.PAID) LocalSuccess else LocalAmberSecondary, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Workflow button based on status
                            when (order.orderStatus) {
                                OrderStatus.ORDER_PLACED -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.ORDER_CONFIRMED) },
                                            colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Confirm Order", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.CANCELLED) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject", color = LocalError, fontSize = 12.sp)
                                        }
                                    }
                                }
                                OrderStatus.ORDER_CONFIRMED -> {
                                    Button(
                                        onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.PREPARING) },
                                        colors = ButtonDefaults.buttonColors(containerColor = LocalAmberSecondary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Start Preparing Items", fontSize = 12.sp)
                                    }
                                }
                                OrderStatus.PREPARING -> {
                                    Button(
                                        onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.READY_FOR_DELIVERY) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Mark Ready for Delivery (Admin Handles Pickup)", fontSize = 12.sp)
                                    }
                                }
                                OrderStatus.READY_FOR_DELIVERY -> {
                                    Surface(
                                        color = Color(0xFFE0F2F1),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeliveryDining,
                                                contentDescription = null,
                                                tint = Color(0xFF00796B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Ready for Admin Pickup. Admin will personally deliver this order.",
                                                fontSize = 12.sp,
                                                color = Color(0xFF004D40),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                OrderStatus.DELIVERY_ACCEPTED, OrderStatus.PICKED_UP, OrderStatus.OUT_FOR_DELIVERY -> {
                                    Surface(
                                        color = Color(0xFFFFF8E1),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalShipping,
                                                contentDescription = null,
                                                tint = Color(0xFFF57F17),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Delivery in progress by Admin (${order.orderStatus.label})",
                                                fontSize = 12.sp,
                                                color = Color(0xFFE65100),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                OrderStatus.DELIVERED -> {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Delivered to customer by Admin ✅",
                                            fontSize = 12.sp,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopOffersPromotionsScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val offers by viewModel.offers.collectAsState()
    val promotions by viewModel.allPromotions.collectAsState()

    val ownerShop = remember(shops, currentUser) {
        shops.find { it.ownerId == (currentUser?.id ?: 0L) } ?: shops.firstOrNull()
    }
    val shopOffers = remember(offers, ownerShop) {
        offers.filter { it.shopId == (ownerShop?.id ?: 0L) }
    }
    val shopPromotions = remember(promotions, ownerShop) {
        promotions.filter { it.shopId == (ownerShop?.id ?: 0L) }
    }

    var showCouponDialog by remember { mutableStateOf(false) }
    var showPromotionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offers & Promotions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LocalBackgroundLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Action Buttons
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showCouponDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAmberSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Coupon", fontSize = 12.sp, color = Color.White)
                    }
                    Button(
                        onClick = { showPromotionDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Request Ad Banner", fontSize = 12.sp)
                    }
                }
            }

            // Active Coupons
            item {
                Text("Active Coupons for Your Shop (${shopOffers.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            items(shopOffers) { offer ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(offer.couponCode, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LocalAmberSecondary)
                            Text("${offer.discountPercentage}% OFF • Min order ₹${offer.minOrderAmount.toInt()}", fontSize = 11.sp, color = LocalTextSecondary)
                        }
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                            Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LocalSuccess, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }

            // Submitted Promotion Requests
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Promotion / Sponsorship Requests to Admin (${shopPromotions.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            items(shopPromotions) { promo ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(promo.type.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(
                                color = when (promo.status) {
                                    com.example.domain.model.PromotionStatus.PENDING -> Color(0xFFFFF3E0)
                                    com.example.domain.model.PromotionStatus.APPROVED,
                                    com.example.domain.model.PromotionStatus.PAID -> Color(0xFFE3F2FD)
                                    com.example.domain.model.PromotionStatus.ACTIVE -> Color(0xFFE8F5E9)
                                    com.example.domain.model.PromotionStatus.REJECTED -> Color(0xFFFFEBEE)
                                    com.example.domain.model.PromotionStatus.EXPIRED -> Color(0xFFECEFF1)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = promo.status.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (promo.status) {
                                        com.example.domain.model.PromotionStatus.PENDING -> LocalAmberSecondary
                                        com.example.domain.model.PromotionStatus.APPROVED,
                                        com.example.domain.model.PromotionStatus.PAID -> Color(0xFF1565C0)
                                        com.example.domain.model.PromotionStatus.ACTIVE -> LocalSuccess
                                        com.example.domain.model.PromotionStatus.REJECTED -> LocalError
                                        com.example.domain.model.PromotionStatus.EXPIRED -> LocalTextMuted
                                    },
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Target: ${promo.targetName} • Fee: ₹${promo.amount.toInt()}", fontSize = 12.sp, color = LocalTextSecondary)
                        Text(promo.notes, fontSize = 11.sp, color = LocalTextMuted)
                    }
                }
            }
        }
    }

    if (showCouponDialog && ownerShop != null) {
        CreateCouponDialog(
            shop = ownerShop,
            onDismiss = { showCouponDialog = false },
            onSave = {
                viewModel.launch {
                    viewModel.repository.addOffer(it)
                }
                showCouponDialog = false
            }
        )
    }

    if (showPromotionDialog && ownerShop != null) {
        RequestPromotionDialog(
            shop = ownerShop,
            onDismiss = { showPromotionDialog = false },
            onSubmit = { type, notes ->
                viewModel.submitPromotionRequest(
                    shop = ownerShop,
                    type = type,
                    targetId = ownerShop.id,
                    targetName = ownerShop.shopName,
                    notes = notes
                )
                showPromotionDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopProfileScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val shops by viewModel.allShops.collectAsState()

    val ownerShop = remember(shops, currentUser) {
        shops.find { it.ownerId == (currentUser?.id ?: 0L) } ?: shops.firstOrNull()
    }

    if (ownerShop == null) return

    var shopName by remember { mutableStateOf(ownerShop.shopName) }
    var address by remember { mutableStateOf(ownerShop.address) }
    var openingHours by remember { mutableStateOf(ownerShop.openingHours) }
    var deliveryRadius by remember { mutableStateOf(ownerShop.deliveryRadiusKm.toString()) }
    var deliveryFee by remember { mutableStateOf(ownerShop.deliveryFee.toInt().toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop Profile Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text("Shop Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Shop Address") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = openingHours,
                onValueChange = { openingHours = it },
                label = { Text("Opening Hours") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = deliveryRadius,
                onValueChange = { deliveryRadius = it },
                label = { Text("Delivery Radius (km)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = deliveryFee,
                onValueChange = { deliveryFee = it },
                label = { Text("Delivery Fee (₹)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    viewModel.launch {
                        viewModel.repository.updateShop(
                            ownerShop.copy(
                                shopName = shopName,
                                address = address,
                                openingHours = openingHours,
                                deliveryRadiusKm = deliveryRadius.toDoubleOrNull() ?: 5.0,
                                deliveryFee = deliveryFee.toDoubleOrNull() ?: 25.0
                            )
                        )
                    }
                    viewModel.navigateTo(Screen.ShopOwnerDashboard)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Dialogs
@Composable
fun ProductFormDialog(
    initialProduct: ProductEntity?,
    shop: ShopEntity,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Electronics") }
    var price by remember { mutableStateOf(initialProduct?.price?.toInt()?.toString() ?: "") }
    var discountPrice by remember { mutableStateOf(initialProduct?.discountPrice?.toInt()?.toString() ?: "") }
    var stock by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "10") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }

    val categories = listOf(
        "Electronics", "Mobile Accessories", "Clothing", "Footwear",
        "Beauty & Personal Care", "Home & Kitchen", "Stationery", "Household Items", "Other Local Products"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (initialProduct == null) "Add New Product" else "Edit Product",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown selector
                Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(category)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                expanded = false
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = discountPrice,
                        onValueChange = { discountPrice = it },
                        label = { Text("Offer Price (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stock Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && price.isNotBlank()) {
                                onSave(
                                    ProductEntity(
                                        id = initialProduct?.id ?: 0L,
                                        shopId = shop.id,
                                        shopName = shop.shopName,
                                        name = name,
                                        category = category,
                                        price = price.toDoubleOrNull() ?: 100.0,
                                        discountPrice = discountPrice.toDoubleOrNull(),
                                        stock = stock.toIntOrNull() ?: 10,
                                        description = description.ifBlank { "High quality item from ${shop.shopName}" },
                                        isAvailable = true,
                                        isApproved = true
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateCouponDialog(
    shop: ShopEntity,
    onDismiss: () -> Unit,
    onSave: (OfferEntity) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var percentage by remember { mutableStateOf("10") }
    var minOrder by remember { mutableStateOf("200") }
    var maxDiscount by remember { mutableStateOf("100") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create Discount Coupon", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Coupon Code (e.g. SHOP20)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = percentage,
                    onValueChange = { percentage = it },
                    label = { Text("Discount Percentage (%)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = minOrder,
                    onValueChange = { minOrder = it },
                    label = { Text("Minimum Order Amount (₹)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maxDiscount,
                    onValueChange = { maxDiscount = it },
                    label = { Text("Max Discount Limit (₹)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (code.isNotBlank()) {
                                onSave(
                                    OfferEntity(
                                        shopId = shop.id,
                                        shopName = shop.shopName,
                                        title = "Special Discount - $code",
                                        couponCode = code,
                                        discountPercentage = percentage.toIntOrNull() ?: 10,
                                        minOrderAmount = minOrder.toDoubleOrNull() ?: 100.0,
                                        maxDiscountAmount = maxDiscount.toDoubleOrNull() ?: 50.0,
                                        validUntil = "Next 30 Days"
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAmberSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RequestPromotionDialog(
    shop: ShopEntity,
    onDismiss: () -> Unit,
    onSubmit: (PromotionType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(PromotionType.SPONSORED_SHOP) }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Request Platform Promotion", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Get more local Sinapali & Nuapada customers to see your store!", fontSize = 11.sp, color = LocalTextSecondary)

                PromotionType.values().forEach { type ->
                    Surface(
                        onClick = { selectedType = type },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            if (selectedType == type) 2.dp else 1.dp,
                            if (selectedType == type) LocalTealPrimary else LocalDivider
                        ),
                        color = if (selectedType == type) LocalTealPrimary.copy(alpha = 0.08f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(type.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("₹${type.defaultPrice.toInt()}/mo", fontSize = 12.sp, color = LocalAmberSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Special Request") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSubmit(selectedType, notes.ifBlank { "Promote ${shop.shopName} to local customers" })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Submit Request")
                    }
                }
            }
        }
    }
}
