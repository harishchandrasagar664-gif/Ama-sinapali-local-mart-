@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.customer

import androidx.compose.foundation.background
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
import com.example.domain.model.DeliveryType
import com.example.domain.model.OrderStatus
import com.example.domain.model.PaymentMethod
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun CartScreen(viewModel: MainViewModel) {
    val items by viewModel.detailedCartItems.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val couponError by viewModel.couponError.collectAsState()
    var couponInput by remember { mutableStateOf("") }

    val subtotal = items.sumOf { it.itemTotal }
    val discount = appliedCoupon?.let { (subtotal * it.discountPercent / 100.0).coerceAtMost(it.maxDiscountAmount) } ?: 0.0
    val total = (subtotal - discount + 25.0).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart 🛒") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                            Text("Total: ₹${total.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LocalTealPrimary)
                            Text("Incl. ₹25 town delivery", fontSize = 11.sp, color = LocalTextSecondary)
                        }
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Checkout) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                        ) {
                            Text("Proceed to Checkout", fontWeight = FontWeight.Bold)
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
                    Text("Your cart is empty", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Add items from Sinapali shops to order!", color = LocalTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
                        Text("Explore Shops")
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
                                Text("From: ${item.shopName}", fontSize = 11.sp, color = LocalTextSecondary)
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
                            Text("Apply Coupon Code", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = couponInput,
                                    onValueChange = { couponInput = it },
                                    placeholder = { Text("e.g. SINAPALI10") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.applyCoupon(couponInput) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Apply")
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
                            Text("Bill Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Item Total", fontSize = 13.sp)
                                Text("₹${subtotal.toInt()}", fontSize = 13.sp)
                            }
                            if (discount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Coupon Discount", fontSize = 13.sp, color = LocalSuccess)
                                    Text("-₹${discount.toInt()}", fontSize = 13.sp, color = LocalSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Delivery Fee", fontSize = 13.sp)
                                Text("₹25", fontSize = 13.sp)
                            }
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("To Pay", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
    var address by remember { mutableStateOf("Near Panchayat Samiti Office, Word No 4, Sinapali") }
    var selectedDeliveryType by remember { mutableStateOf(DeliveryType.HOME_DELIVERY) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH_ON_DELIVERY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Cart) }) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Delivery Address (Sinapali)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Delivery Type", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedDeliveryType == DeliveryType.HOME_DELIVERY,
                            onClick = { selectedDeliveryType = DeliveryType.HOME_DELIVERY }
                        )
                        Text("Doorstep Home Delivery (₹25)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedDeliveryType == DeliveryType.SELF_PICKUP,
                            onClick = { selectedDeliveryType = DeliveryType.SELF_PICKUP }
                        )
                        Text("Self Store Pickup (Free)")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY,
                            onClick = { selectedPaymentMethod = PaymentMethod.CASH_ON_DELIVERY }
                        )
                        Text("Cash on Delivery (Pay to Admin upon delivery)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPaymentMethod == PaymentMethod.RAZORPAY,
                            onClick = { selectedPaymentMethod = PaymentMethod.RAZORPAY }
                        )
                        Text("Online / UPI (Razorpay Instant Pay)")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.initiateCheckout(
                        deliveryAddress = address,
                        deliveryType = selectedDeliveryType,
                        paymentMethod = selectedPaymentMethod
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
            ) {
                Text("Confirm & Place Order", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun OrderTrackingScreen(orderId: Long, viewModel: MainViewModel) {
    val orders by viewModel.allOrders.collectAsState()
    val order = orders.find { it.id == orderId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order #${order?.orderNumber ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerOrders) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                            Text("Current Status:", fontWeight = FontWeight.Medium)
                            StatusBadge(order.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Final Amount: ₹${order.finalAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LocalTealDark)
                        Text("Payment: ${order.paymentMethod.name.replace('_', ' ')} (${order.paymentStatus.name})", fontSize = 12.sp)
                        Text("Delivered By: ${order.deliveryAgentAssigned ?: "Harishchandra Sagar (Admin)"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Address: ${order.deliveryAddress}", fontSize = 12.sp)
                    }
                }

                // Delivery Timeline
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Live Delivery Timeline", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        TimelineItem(title = "Order Placed", active = true, icon = Icons.Default.CheckCircle)
                        TimelineItem(title = "Shop Preparing Order", active = order.status != OrderStatus.PLACED, icon = Icons.Default.Store)
                        TimelineItem(title = "Out for Delivery (Admin)", active = order.status == OrderStatus.OUT_FOR_DELIVERY || order.status == OrderStatus.DELIVERED, icon = Icons.Default.TwoWheeler)
                        TimelineItem(title = "Delivered & Handed Over", active = order.status == OrderStatus.DELIVERED, icon = Icons.Default.DoneAll)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.navigateTo(Screen.CustomerHome) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Shopping")
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
