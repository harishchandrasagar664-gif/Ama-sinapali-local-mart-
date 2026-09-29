@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppLanguage
import com.example.domain.model.DeliveryType
import com.example.domain.model.OrderStatus
import com.example.domain.model.PaymentMethod
import com.example.ui.components.StatusBadge
import com.example.ui.locale.AppStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun CartScreen(viewModel: MainViewModel) {
    val items by viewModel.detailedCartItems.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val couponError by viewModel.couponError.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }
    var couponInput by remember { mutableStateOf("") }

    val subtotal = items.sumOf { it.itemTotal }
    val discount = appliedCoupon?.let { (subtotal * it.discountPercent / 100.0).coerceAtMost(it.maxDiscountAmount) } ?: 0.0
    val total = (subtotal - discount + 25.0).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.yourCartTitle) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        },
        bottomBar = {
            if (items.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${s.toPay}: ₹${total.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LocalTealPrimary)
                            Text("Incl. ₹25 town delivery", fontSize = 11.sp, color = LocalTextSecondary)
                        }
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Checkout) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                        ) {
                            Text(s.proceedToCheckout, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingBasket, contentDescription = null, modifier = Modifier.size(64.dp), tint = LocalTextMuted)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(s.cartEmpty, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(s.cartEmptyDesc, color = LocalTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
                        Text(s.exploreShopsBtn)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${s.fromShop}: ${item.shopName}", fontSize = 11.sp, color = LocalTextSecondary)
                                Text("₹${item.effectivePrice.toInt()} each", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LocalTealPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(item, -1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text("${item.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(item, 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                }

                // Coupon section
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(s.applyCoupon, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = couponInput,
                                    onValueChange = { couponInput = it },
                                    placeholder = { Text(s.couponCodePlaceholder) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.applyCoupon(couponInput) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(s.applyBtn)
                                }
                            }
                            if (appliedCoupon != null) {
                                Text("✓ Coupon Applied: ${appliedCoupon?.couponCode}", color = LocalSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (couponError != null) {
                                Text(couponError ?: "", color = LocalError, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Bill summary
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(s.billSummary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s.itemTotal, fontSize = 13.sp)
                                Text("₹${subtotal.toInt()}", fontSize = 13.sp)
                            }
                            if (discount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(s.couponDiscount, fontSize = 13.sp, color = LocalSuccess)
                                    Text("-₹${discount.toInt()}", fontSize = 13.sp, color = LocalSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s.deliveryFeeText, fontSize = 13.sp)
                                Text("₹25", fontSize = 13.sp)
                            }
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s.toPay, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("₹${total.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LocalTealPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutScreen(viewModel: MainViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val items by viewModel.detailedCartItems.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    val savedFullAddress = remember(user) {
        val baseAddr = user?.deliveryAddress?.takeIf { it.isNotBlank() } ?: "Near Panchayat Samiti Office, Word No 4, Sinapali"
        val village = user?.village?.takeIf { it.isNotBlank() } ?: "Sinapali"
        val gp = user?.gp?.takeIf { it.isNotBlank() } ?: "Sinapali"
        val block = user?.block?.takeIf { it.isNotBlank() } ?: "Sinapali"
        val dist = user?.district?.takeIf { it.isNotBlank() } ?: "Nuapada"
        "$baseAddr, Village: $village, GP: $gp, Block: $block, Dist: $dist"
    }
    var address by remember(savedFullAddress) { mutableStateOf(savedFullAddress) }
    var selectedDeliveryType by remember { mutableStateOf(DeliveryType.HOME_DELIVERY) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH_ON_DELIVERY) }

    val subtotal = items.sumOf { it.itemTotal }
    val discount = appliedCoupon?.let { (subtotal * it.discountPercent / 100.0).coerceAtMost(it.maxDiscountAmount) } ?: 0.0
    val deliveryFee = if (selectedDeliveryType == DeliveryType.HOME_DELIVERY) 25.0 else 0.0
    val total = (subtotal - discount + deliveryFee).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.checkoutTitle) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingBasket,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = LocalTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(s.cartEmpty, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(s.cartEmptyDesc, color = LocalTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
                        Text(s.exploreShopsBtn)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Delivery Address Card
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.deliveryAddressTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Surface(
                                    color = LocalTealPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        s.savedAddressBadge,
                                        color = LocalTealPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (user != null) {
                                Text(
                                    "${s.deliverTo} ${user?.name} (${user?.phone})",
                                    fontSize = 12.sp,
                                    color = LocalTextSecondary
                                )
                                Text(
                                    "📍 ${s.villageLabel}: ${user?.village}, ${s.gpLabel}: ${user?.gp}, ${s.blockLabel}: ${user?.block}, ${s.districtLabel}: ${user?.district}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LocalTealDark
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                label = { Text(s.fullAddressLabel) }
                            )
                        }
                    }

                    // Delivery Type Card
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(s.deliveryTypeTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = selectedDeliveryType == DeliveryType.HOME_DELIVERY,
                                    onClick = { selectedDeliveryType = DeliveryType.HOME_DELIVERY }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(s.homeDelivery, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Doorstep delivery across Sinapali town (₹25)", fontSize = 11.sp, color = LocalTextSecondary)
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = selectedDeliveryType == DeliveryType.SELF_PICKUP,
                                    onClick = { selectedDeliveryType = DeliveryType.SELF_PICKUP }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(s.storePickup, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Collect directly from the merchant shop (₹0 fee)", fontSize = 11.sp, color = LocalTextSecondary)
                                }
                            }
                        }
                    }

                    // Payment Method Card
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.paymentMethodTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Surface(
                                    color = LocalSuccess.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "✓ COD Available",
                                        color = LocalSuccess,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY,
                                    onClick = { selectedPaymentMethod = PaymentMethod.CASH_ON_DELIVERY }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(s.codMethod, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Pay cash to delivery person upon arrival in Sinapali", fontSize = 12.sp, color = LocalTextSecondary)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == PaymentMethod.ONLINE_UPI,
                                    onClick = { selectedPaymentMethod = PaymentMethod.ONLINE_UPI }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(s.onlineMethod, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Pay securely via UPI (PhonePe, GPay, Paytm) or Cards", fontSize = 12.sp, color = LocalTextSecondary)
                                }
                            }
                        }
                    }

                    // Order Summary Card
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(s.orderSummaryTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${s.itemTotal} (${items.sumOf { it.quantity }} items)", fontSize = 13.sp)
                                Text("₹${subtotal.toInt()}", fontSize = 13.sp)
                            }
                            if (discount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(s.couponDiscount, fontSize = 13.sp, color = LocalSuccess)
                                    Text("-₹${discount.toInt()}", fontSize = 13.sp, color = LocalSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s.deliveryFeeText, fontSize = 13.sp)
                                Text(
                                    if (selectedDeliveryType == DeliveryType.HOME_DELIVERY) "₹25" else "FREE",
                                    fontSize = 13.sp,
                                    color = if (selectedDeliveryType == DeliveryType.SELF_PICKUP) LocalSuccess else Color.Unspecified
                                )
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.toPay, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("₹${total.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LocalTealPrimary)
                            }
                        }
                    }

                    // Always show the "Place Order (Cash on Delivery)" button for customers when COD is available below Delivery Type/Payment Method
                    Button(
                        onClick = {
                            viewModel.initiateCheckout(
                                deliveryAddress = address.ifBlank { savedFullAddress },
                                deliveryType = selectedDeliveryType,
                                paymentMethod = PaymentMethod.CASH_ON_DELIVERY
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("place_order_cod_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalTealPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${s.placeOrderCod} • ₹${total.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Optional Online Payment Action if user selected Online UPI
                    if (selectedPaymentMethod == PaymentMethod.ONLINE_UPI) {
                        OutlinedButton(
                            onClick = {
                                viewModel.initiateCheckout(
                                    deliveryAddress = address.ifBlank { savedFullAddress },
                                    deliveryType = selectedDeliveryType,
                                    paymentMethod = PaymentMethod.ONLINE_UPI
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("pay_online_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${s.payAndPlaceOrder} • ₹${total.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }

                    // Ample bottom space so button is completely above system navigation bars and gesture pill
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun OrderTrackingScreen(orderId: Long, viewModel: MainViewModel) {
    val orders by viewModel.allOrders.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    val order = orders.find { it.id == orderId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${s.orderNumber}${order?.orderNumber ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { padding ->
        if (order != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status:", fontWeight = FontWeight.Medium)
                            StatusBadge(order.status, language = appLanguage)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${s.toPay}: ₹${order.finalAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LocalTealDark)
                        Text("Payment: ${order.paymentMethod.name.replace('_', ' ')} (${order.paymentStatus.name})", fontSize = 12.sp)
                        Text("${s.deliveredBy}: ${order.deliveryAgentAssigned ?: "Harishchandra Sagar (Admin)"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${s.deliveryAddressTitle}: ${order.deliveryAddress}", fontSize = 12.sp)
                    }
                }

                // Delivery Timeline
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Live Delivery Timeline", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        TimelineItem(title = s.orderStatusPlaced, active = true, icon = Icons.Default.CheckCircle)
                        TimelineItem(title = s.orderStatusPreparing, active = order.status != OrderStatus.PLACED, icon = Icons.Default.Store)
                        TimelineItem(title = s.orderStatusOutForDelivery, active = order.status == OrderStatus.OUT_FOR_DELIVERY || order.status == OrderStatus.DELIVERED, icon = Icons.Default.TwoWheeler)
                        TimelineItem(title = s.orderStatusDelivered, active = order.status == OrderStatus.DELIVERED, icon = Icons.Default.DoneAll)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.navigateTo(Screen.CustomerHome) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(s.exploreShopsBtn)
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(title: String, active: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) LocalTealPrimary else LocalTextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) LocalTextPrimary else LocalTextMuted,
            fontSize = 14.sp
        )
    }
}
