@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.shop

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.CheckCircle

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
import com.example.data.local.entities.OrderItemEntity
import com.example.data.local.entities.NotificationEntity
import com.example.domain.model.OrderStatus
import com.example.domain.model.NotificationType
import com.example.domain.model.PromotionType
import com.example.domain.model.PromotionStatus
import com.example.data.local.entities.PromotionEntity
import com.example.ui.components.OdiaGreetingHeader
import com.example.ui.components.ProductImage
import com.example.ui.components.ProfilePhotoUploadDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.UserAvatar
import com.example.util.ImageStorageHelper
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
    val appLanguage by viewModel.appLanguage.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val allOrderItems by viewModel.allOrderItems.collectAsState()
    val allNotifications by viewModel.notifications.collectAsState()

    var showDpDialog by remember { mutableStateOf(false) }

    val myShop = shops.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user?.shopId) }
    val myOrders = if (myShop != null) orders.filter { it.shopId == myShop.id } else emptyList()
    val myProducts = if (myShop != null) products.filter { it.shopId == myShop.id } else emptyList()
    val myNotifications = remember(allNotifications, myShop, user) {
        if (myShop != null) {
            allNotifications.filter { notif ->
                val isOrderNotification = notif.type == NotificationType.ORDER_STATUS ||
                    notif.title.contains("Order", ignoreCase = true) ||
                    notif.message.contains("Order", ignoreCase = true)
                val isForMyShop = notif.userId == user?.id ||
                    notif.userId == myShop.ownerId ||
                    notif.title.contains(myShop.name, ignoreCase = true) ||
                    notif.message.contains(myShop.name, ignoreCase = true)
                isOrderNotification && isForMyShop
            }
        } else emptyList()
    }

    Scaffold(
        topBar = {
            OdiaGreetingHeader(
                currentRole = currentRole,
                onRoleSwitchClick = onRoleSwitchClick,
                userName = "${user?.name ?: "Shop Owner"} (${myShop?.name ?: "No Shop Assigned"})"
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
            // Shop Owner Profile Card with DP
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                photoUri = user?.profilePhotoUri ?: myShop?.imageUrl,
                                name = user?.name ?: "Shop Owner",
                                size = 64.dp,
                                showEditBadge = true,
                                onEditClick = { showDpDialog = true }
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    myShop?.name ?: "Sinapali Mart Partner Shop",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = LocalTealDark
                                )
                                Text(
                                    "Owner: ${user?.name ?: "Shop Owner"}",
                                    fontSize = 13.sp,
                                    color = LocalTextSecondary
                                )
                                Text(
                                    "📍 ${myShop?.address ?: "Sinapali"}",
                                    fontSize = 11.sp,
                                    color = LocalTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDpDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Change DP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            if (myShop != null) {
                                FilledTonalButton(
                                    onClick = { viewModel.useShopPhotoAsDp(myShop) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = LocalAmberSecondary, contentColor = Color.Black),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Use Shop Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

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

            if (myNotifications.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = LocalTealPrimary.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = LocalTealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shop Order Notifications (${myNotifications.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            myNotifications.take(4).forEach { notif ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(notif.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = LocalTealDark)
                                        Text(notif.message, fontSize = 11.sp, color = LocalTextSecondary)
                                    }
                                }
                            }
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
                    Text("My Shop Orders (${myOrders.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (myOrders.isNotEmpty()) {
                        TextButton(onClick = { viewModel.navigateTo(Screen.ShopOrderManagement) }) {
                            Text("Manage All Orders →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (myOrders.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No customer orders received yet for this shop.", color = LocalTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }

            items(myOrders) { order ->
                val itemsForOrder = allOrderItems.filter { it.orderId == order.id }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Order #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            StatusBadge(order.status)
                        }

                        // Customer Name & Mobile Number strictly
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = LocalTealPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Customer Name: ${order.customerName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = LocalTealPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mobile Number: ${order.customerPhone}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }

                        // Ordered Products with Quantity and MRP strictly
                        Text("Ordered Products (${itemsForOrder.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LocalTextSecondary)
                        if (itemsForOrder.isEmpty()) {
                            Text("Items logged in order. Total: ₹${order.totalAmount.toInt()}", fontSize = 12.sp, color = LocalTextSecondary)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                itemsForOrder.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("Qty: ${item.quantity}  •  MRP: ₹${item.unitPrice.toInt()}", fontSize = 12.sp, color = LocalTextSecondary)
                                        }
                                        Text("₹${item.totalPrice.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LocalTealPrimary)
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Final Order Total:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("₹${order.finalAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LocalTealDark)
                        }

                        if (order.status == OrderStatus.PLACED) {
                            Button(
                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.PREPARING) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                            ) {
                                Icon(Icons.Default.SoupKitchen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accept & Prepare", fontWeight = FontWeight.Bold)
                            }
                        } else if (order.status == OrderStatus.PREPARING) {
                            Button(
                                onClick = { viewModel.updateOrderStatus(order.id, OrderStatus.READY_FOR_PICKUP) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LocalAmberSecondary, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark Ready for Admin Pickup", fontWeight = FontWeight.Bold)
                            }
                        } else if (order.status == OrderStatus.READY_FOR_PICKUP) {
                            Surface(
                                color = LocalAmberSecondary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "✓ Ready for Admin (Harishchandra Sagar) pickup & delivery",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LocalAmberDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDpDialog) {
            ProfilePhotoUploadDialog(
                language = appLanguage,
                currentPhotoUri = user?.profilePhotoUri ?: myShop?.imageUrl,
                associatedShop = myShop,
                onDismissRequest = { showDpDialog = false },
                onPhotoSelected = { newUri ->
                    viewModel.updateProfilePhoto(newUri)
                },
                onShopPhotoSelected = { shopPhotoUri ->
                    if (myShop != null) {
                        viewModel.updateShopPhoto(myShop.id, shopPhotoUri)
                    }
                }
            )
        }
    }
}

@Composable
fun ShopProductManagementScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val myShop = shops.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user?.shopId) }
    val myProducts = if (myShop != null) products.filter { it.shopId == myShop.id } else emptyList()

    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var prodName by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodCat by remember { mutableStateOf("Groceries & Spices") }
    var savedImagePath by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageSourceDescription by remember { mutableStateOf<String?>(null) }

    // Photo Picker (Gallery)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentPath = ImageStorageHelper.copyProductUri(context, uri)
            savedImagePath = permanentPath ?: uri.toString()
            selectedImageUri = uri
            capturedBitmap = null
            imageSourceDescription = "Photo saved to app storage"
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val permanentPath = ImageStorageHelper.saveProductBitmap(context, bitmap)
            savedImagePath = permanentPath
            capturedBitmap = bitmap
            selectedImageUri = null
            imageSourceDescription = "Captured & saved to app storage"
        }
    }

    // Permission Launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required to take product photo", Toast.LENGTH_SHORT).show()
        }
    }

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
                onClick = {
                    editingProduct = null
                    prodName = ""
                    prodPrice = ""
                    prodCat = "Groceries & Spices"
                    savedImagePath = null
                    selectedImageUri = null
                    capturedBitmap = null
                    imageSourceDescription = null
                    showProductDialog = true
                },
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingProduct = prod
                            prodName = prod.name
                            prodPrice = prod.price.toInt().toString()
                            prodCat = prod.category
                            savedImagePath = prod.imageUrl
                            selectedImageUri = null
                            capturedBitmap = null
                            imageSourceDescription = null
                            showProductDialog = true
                        },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductImage(
                            imageUrl = prod.imageUrl,
                            contentDescription = prod.name,
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("₹${prod.price.toInt()} • Stock: ${prod.stock}", fontSize = 12.sp, color = LocalTextSecondary)
                            Text(
                                if (prod.isAvailable) "In Stock" else "Out of Stock",
                                color = if (prod.isAvailable) LocalSuccess else LocalError,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = {
                            editingProduct = prod
                            prodName = prod.name
                            prodPrice = prod.price.toInt().toString()
                            prodCat = prod.category
                            savedImagePath = prod.imageUrl
                            selectedImageUri = null
                            capturedBitmap = null
                            imageSourceDescription = null
                            showProductDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = LocalTealPrimary)
                        }
                        Switch(
                            checked = prod.isAvailable,
                            onCheckedChange = { viewModel.toggleProductStock(prod) }
                        )
                    }
                }
            }
        }

        if (showProductDialog && myShop != null) {
            AlertDialog(
                onDismissRequest = {
                    showProductDialog = false
                    editingProduct = null
                    selectedImageUri = null
                    capturedBitmap = null
                    savedImagePath = null
                    imageSourceDescription = null
                },
                title = { Text(if (editingProduct != null) "Edit Product" else "Add New Product") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = prodName,
                            onValueChange = { prodName = it },
                            label = { Text("Product Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = prodPrice,
                            onValueChange = { prodPrice = it },
                            label = { Text("Price (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = prodCat,
                            onValueChange = { prodCat = it },
                            label = { Text("Category") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Product Image (Permanent App Storage)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gallery", fontSize = 12.sp)
                            }
                        }

                        // Preview of selected or existing image
                        if (!savedImagePath.isNullOrBlank()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                ProductImage(
                                    imageUrl = savedImagePath,
                                    contentDescription = "Product Image Preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LocalSuccess, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(imageSourceDescription ?: "Image ready (saved in app storage)", fontSize = 11.sp, color = LocalSuccess)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val price = prodPrice.toDoubleOrNull() ?: 100.0
                        val finalImageUrl = savedImagePath
                            ?: selectedImageUri?.let { ImageStorageHelper.copyProductUri(context, it) }
                            ?: capturedBitmap?.let { ImageStorageHelper.saveProductBitmap(context, it) }
                            ?: editingProduct?.imageUrl
                            ?: "hero_local_mart"

                        if (editingProduct != null) {
                            val updated = editingProduct!!.copy(
                                name = prodName.ifBlank { editingProduct!!.name },
                                price = price,
                                category = prodCat.ifBlank { editingProduct!!.category },
                                imageUrl = finalImageUrl
                            )
                            viewModel.addOrUpdateProduct(updated)
                        } else {
                            val newProd = ProductEntity(
                                shopId = myShop.id,
                                name = prodName.ifBlank { "Local Product" },
                                category = prodCat.ifBlank { "Groceries & Spices" },
                                imageUrl = finalImageUrl,
                                description = "Freshly supplied by ${myShop.name}",
                                price = price,
                                unit = "1 unit",
                                stock = 20
                            )
                            viewModel.addOrUpdateProduct(newProd)
                        }
                        showProductDialog = false
                        editingProduct = null
                        selectedImageUri = null
                        capturedBitmap = null
                        savedImagePath = null
                        imageSourceDescription = null
                    }) {
                        Text(if (editingProduct != null) "Save" else "Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showProductDialog = false
                        editingProduct = null
                        selectedImageUri = null
                        capturedBitmap = null
                        savedImagePath = null
                        imageSourceDescription = null
                    }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ShopOffersPromotionsScreen(viewModel: MainViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val shops by viewModel.allShops.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val allPromotions by viewModel.allPromotions.collectAsState()
    val pricingConfig by viewModel.promotionPricingConfig.collectAsState()

    val myShop = shops.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user?.shopId) }
    val myProducts = if (myShop != null) products.filter { it.shopId == myShop.id } else emptyList()
    val myPromotions = if (myShop != null) allPromotions.filter { it.shopId == myShop.id } else emptyList()

    var selectedTab by remember { mutableStateOf(0) }

    // Promotion Creation Form State
    var selectedType by remember { mutableStateOf(PromotionType.PROMOTIONAL_BANNER) }
    var promoTitle by remember { mutableStateOf("") }
    var promoDescription by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedLocation by remember { mutableStateOf("Sinapali Market") }
    var durationDays by remember { mutableStateOf(7) }
    var startDate by remember { mutableStateOf("2026-09-29") }
    var endDate by remember { mutableStateOf("2026-10-06") }

    val dailyRate = remember(selectedType, pricingConfig) {
        pricingConfig.getDailyRate(selectedType)
    }
    val totalPrice = remember(dailyRate, durationDays) {
        dailyRate * durationDays
    }

    val locationOptions = listOf(
        "Sinapali Market",
        "Sinapali Town (Main Road)",
        "Hatibandh GP",
        "Timanpur GP",
        "Karanbahali GP",
        "Ghantiguda GP",
        "All Sinapali Block"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop Promotions & Ads") },
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Create Promotion", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "My Promotions (${myPromotions.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                // CREATE PROMOTION TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "📢 Boost Your Shop Visibility in Sinapali",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = LocalTealDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Select from 5 promotion formats. Review the Admin-configured pricing before submitting for approval.",
                                    fontSize = 12.sp,
                                    color = LocalTextSecondary
                                )
                            }
                        }
                    }

                    // 1. Choose Promotion Type (5 Options)
                    item {
                        Text("1. Select Promotion Format", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PromotionTypeSelectionCard(
                                title = "1. Product Ad",
                                subtitle = "Spotlight a product with 'Sponsored Ad' badge across customer searches",
                                dailyRate = pricingConfig.productAdDailyRate,
                                isSelected = selectedType == PromotionType.PRODUCT_AD,
                                onSelect = {
                                    selectedType = PromotionType.PRODUCT_AD
                                    if (promoTitle.isBlank() && myProducts.isNotEmpty()) {
                                        promoTitle = "${myProducts[0].name} Spotlight Ad"
                                    }
                                }
                            )
                            PromotionTypeSelectionCard(
                                title = "2. Promotional Banner",
                                subtitle = "Top hero carousel banner on Sinapali Mart customer home screen",
                                dailyRate = pricingConfig.promotionalBannerDailyRate,
                                isSelected = selectedType == PromotionType.PROMOTIONAL_BANNER || selectedType == PromotionType.BANNER,
                                onSelect = { selectedType = PromotionType.PROMOTIONAL_BANNER }
                            )
                            PromotionTypeSelectionCard(
                                title = "3. Featured Shop",
                                subtitle = "Featured partner placement with ⭐ badge on the Sinapali shop list",
                                dailyRate = pricingConfig.featuredShopDailyRate,
                                isSelected = selectedType == PromotionType.FEATURED_SHOP,
                                onSelect = { selectedType = PromotionType.FEATURED_SHOP }
                            )
                            PromotionTypeSelectionCard(
                                title = "4. Location-based Promotion",
                                subtitle = "Target customers in specific Sinapali GPs (Hatibandh, Timanpur, etc.)",
                                dailyRate = pricingConfig.locationBasedDailyRate,
                                isSelected = selectedType == PromotionType.LOCATION_BASED,
                                onSelect = { selectedType = PromotionType.LOCATION_BASED }
                            )
                            PromotionTypeSelectionCard(
                                title = "5. Date-based Promotion",
                                subtitle = "Scheduled promotion for festivals & weekly haat market days",
                                dailyRate = pricingConfig.dateBasedDailyRate,
                                isSelected = selectedType == PromotionType.DATE_BASED,
                                onSelect = { selectedType = PromotionType.DATE_BASED }
                            )
                        }
                    }

                    // If Product Ad: Select Product
                    if (selectedType == PromotionType.PRODUCT_AD && myProducts.isNotEmpty()) {
                        item {
                            Text("Select Product to Advertise", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                myProducts.forEach { prod ->
                                    val isPicked = (selectedProduct?.id == prod.id) || (selectedProduct == null && prod == myProducts.first())
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedProduct = prod
                                                promoTitle = "${prod.name} - Special Town Deal"
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPicked) LocalTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                        border = if (isPicked) androidx.compose.foundation.BorderStroke(1.5.dp, LocalTealPrimary) else androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = isPicked, onClick = {
                                                selectedProduct = prod
                                                promoTitle = "${prod.name} - Special Town Deal"
                                            })
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(prod.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text("₹${prod.price.toInt()} • ${prod.category}", fontSize = 11.sp, color = LocalTextSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Promotion Details
                    item {
                        Text("2. Promotion Title & Description", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = promoTitle,
                            onValueChange = { promoTitle = it },
                            label = { Text("Promotion Title") },
                            placeholder = { Text("e.g. Nuakhai Special Offer - 15% Off Sweets") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = promoDescription,
                            onValueChange = { promoDescription = it },
                            label = { Text("Promotion Details / Offer Terms") },
                            placeholder = { Text("e.g. Flat 15% off on all items, fresh stock from Sinapali.") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }

                    // 3. Location Selection
                    item {
                        Text("3. Target Location", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Choose where your promotion will be targeted across Sinapali:", fontSize = 12.sp, color = LocalTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            locationOptions.forEach { loc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedLocation = loc }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedLocation == loc,
                                        onClick = { selectedLocation = loc }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(loc, fontSize = 13.sp, fontWeight = if (selectedLocation == loc) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // 4. Duration & Dates
                    item {
                        Text("4. Duration & Scheduled Dates", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 7, 14, 30).forEach { days ->
                                FilterChip(
                                    selected = durationDays == days,
                                    onClick = {
                                        durationDays = days
                                        val endDay = 29 + days
                                        endDate = if (endDay <= 30) "2026-09-${if (endDay < 10) "0$endDay" else "$endDay"}" else "2026-10-${if (endDay - 30 < 10) "0${endDay - 30}" else "${endDay - 30}"}"
                                    },
                                    label = { Text("$days Days", fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Start Date") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("End Date") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    // 5. Admin-Configured Pricing Breakdown Card (Before Submission)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, LocalTealPrimary)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Admin-Configured Pricing Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LocalTealDark)
                                    Surface(color = LocalTealPrimary, shape = RoundedCornerShape(4.dp)) {
                                        Text("Pre-Submission Quote", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                HorizontalDivider()
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Promotion Format:", fontSize = 12.sp, color = LocalTextSecondary)
                                    Text(selectedType.name.replace('_', ' '), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Admin Daily Rate:", fontSize = 12.sp, color = LocalTextSecondary)
                                    Text("₹${dailyRate.toInt()} / day", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Duration & Dates:", fontSize = 12.sp, color = LocalTextSecondary)
                                    Text("$durationDays Days ($startDate to $endDate)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Target Location:", fontSize = 12.sp, color = LocalTextSecondary)
                                    Text(selectedLocation, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total Price Due:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("₹${totalPrice.toInt()}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = LocalTealPrimary)
                                }
                                Text(
                                    "ℹ️ Payment Status: Pending Admin Approval. Admin (Harishchandra Sagar) can review, adjust terms, and confirm payment post-approval.",
                                    fontSize = 11.sp,
                                    color = LocalTextSecondary
                                )
                            }
                        }
                    }

                    // 6. Submit Button
                    item {
                        Button(
                            onClick = {
                                if (myShop != null) {
                                    val finalProd = selectedProduct ?: myProducts.firstOrNull()
                                    val finalTitle = promoTitle.ifBlank { "${myShop.name} ${selectedType.name.replace('_', ' ')}" }
                                    viewModel.createShopPromotion(
                                        shop = myShop,
                                        type = selectedType,
                                        title = finalTitle,
                                        description = promoDescription.ifBlank { "Special promotion by ${myShop.name}" },
                                        targetProductId = if (selectedType == PromotionType.PRODUCT_AD) finalProd?.id else null,
                                        targetProductName = if (selectedType == PromotionType.PRODUCT_AD) finalProd?.name else null,
                                        location = selectedLocation,
                                        durationDays = durationDays,
                                        startDate = startDate,
                                        endDate = endDate,
                                        dailyRate = dailyRate,
                                        totalPrice = totalPrice,
                                        onSuccess = {
                                            promoTitle = ""
                                            promoDescription = ""
                                            selectedTab = 1
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit Promotion → Status: Pending Approval", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                // MY PROMOTIONS & HISTORY TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("My Shop Promotions & History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            TextButton(onClick = { selectedTab = 0 }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Promotion", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (myPromotions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = LocalTextSecondary, modifier = Modifier.size(40.dp))
                                        Text("No promotions submitted yet.", color = LocalTextSecondary, fontWeight = FontWeight.SemiBold)
                                        Text("Create a Product Ad, Promotional Banner, or Featured Shop to grow your orders.", fontSize = 12.sp, color = LocalTextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    items(myPromotions) { promo ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = LocalTealPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            promo.type.name.replace('_', ' '),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = LocalTealDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    // Status Badge
                                    when (promo.status) {
                                        PromotionStatus.PENDING_APPROVAL -> {
                                            Surface(color = LocalAmberSecondary.copy(alpha = 0.25f), shape = RoundedCornerShape(6.dp)) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = LocalAmberDark, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Pending Admin Approval", color = LocalAmberDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        PromotionStatus.APPROVED, PromotionStatus.ACTIVE -> {
                                            Surface(color = LocalSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LocalSuccess, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Approved & Live", color = LocalSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        PromotionStatus.REJECTED -> {
                                            Surface(color = LocalError.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = LocalError, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Rejected by Admin", color = LocalError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        else -> {
                                            Text(promo.status.name, fontSize = 11.sp, color = LocalTextSecondary)
                                        }
                                    }
                                }

                                Text(promo.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (promo.description.isNotBlank()) {
                                    Text(promo.description, fontSize = 12.sp, color = LocalTextSecondary)
                                }

                                if (!promo.targetProductName.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(14.dp), tint = LocalTealPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Product: ${promo.targetProductName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                HorizontalDivider()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Duration & Dates:", fontSize = 11.sp, color = LocalTextSecondary)
                                        Text("${promo.durationDays} Days (${promo.startDate.ifBlank { "Immediate" }} - ${promo.endDate.ifBlank { "Active" }})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Target Location:", fontSize = 11.sp, color = LocalTextSecondary)
                                        Text("📍 ${promo.location}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Agreed Total Price:", fontSize = 11.sp, color = LocalTextSecondary)
                                        Text("₹${promo.totalPrice.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LocalTealPrimary)
                                    }
                                    Surface(
                                        color = if (promo.paymentStatus.contains("PAID", ignoreCase = true)) LocalSuccess.copy(alpha = 0.12f) else LocalAmberSecondary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "Payment: ${promo.paymentStatus}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (promo.paymentStatus.contains("PAID", ignoreCase = true)) LocalSuccess else LocalAmberDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (promo.adminNotes.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Admin Note: ${promo.adminNotes}",
                                            fontSize = 11.sp,
                                            color = LocalTextSecondary,
                                            modifier = Modifier.padding(8.dp)
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
}

@Composable
private fun PromotionTypeSelectionCard(
    title: String,
    subtitle: String,
    dailyRate: Double,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) LocalTealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, LocalTealPrimary) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = onSelect)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, fontSize = 11.sp, color = LocalTextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = if (isSelected) LocalTealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "₹${dailyRate.toInt()}/day",
                    color = if (isSelected) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
