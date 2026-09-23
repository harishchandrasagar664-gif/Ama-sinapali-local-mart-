package com.example.ui.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.OrderEntity
import com.example.domain.model.DeliveryType
import com.example.domain.model.OrderStatus
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PaymentStatus
import com.example.domain.model.UserRole
import com.example.ui.components.EmptyStateView
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(viewModel: MainViewModel) {
    val items by viewModel.detailedCartItems.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val couponError by viewModel.couponError.collectAsState()
    var couponInput by remember { mutableStateOf("") }
    var deliveryType by remember { mutableStateOf(DeliveryType.HOME_DELIVERY) }

    val subtotal = remember(items) { items.sumOf { it.itemTotal } }
    val discount = remember(items, appliedCoupon) {
        if (appliedCoupon != null) {
            (subtotal * (appliedCoupon?.discountPercentage ?: 0) / 100.0)
                .coerceAtMost(appliedCoupon?.maxDiscountAmount ?: 0.0)
        } else 0.0
    }
    val deliveryFee = if (deliveryType == DeliveryType.HOME_DELIVERY && items.isNotEmpty()) 25.0 else 0.0
    val total = (subtotal - discount + deliveryFee).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cart (${items.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (items.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Amount", fontSize = 11.sp, color = LocalTextSecondary)
                            Text(
                                text = "₹${total.toInt()}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAmberSecondary
                            )
                        }
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Checkout) },
                            colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("cart_checkout_btn")
                        ) {
                            Text("Proceed to Checkout", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.ShoppingCart,
                title = "Your cart is empty",
                message = "Explore products from nearby Odisha shops and add them to your cart.",
                actionText = "Start Shopping",
                onAction = { viewModel.navigateTo(Screen.CustomerHome) },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(LocalBackgroundLight),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cart Items List
                items(items) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LocalDivider)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LocalTealPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = LocalTealPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.shopName,
                                    fontSize = 11.sp,
                                    color = LocalTealPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${item.effectivePrice.toInt()} each",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAmberSecondary
                                )
                            }

                            // Quantity controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(item, -1) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = if (item.quantity == 1) LocalError else LocalTextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${item.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(item, 1) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Delivery / Pickup Option
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LocalDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Choose Delivery or Store Pickup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = deliveryType == DeliveryType.HOME_DELIVERY,
                                    onClick = { deliveryType = DeliveryType.HOME_DELIVERY },
                                    label = { Text("Home Delivery (₹25)") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                                FilterChip(
                                    selected = deliveryType == DeliveryType.STORE_PICKUP,
                                    onClick = { deliveryType = DeliveryType.STORE_PICKUP },
                                    label = { Text("Store Pickup (Free)") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }
                }

                // Coupon Code Section
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LocalDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Apply Coupon / Promo Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (appliedCoupon != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = LocalSuccess)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${appliedCoupon?.couponCode} Applied!",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = LocalSuccess
                                            )
                                            Text(
                                                text = "${appliedCoupon?.discountPercentage}% off up to ₹${appliedCoupon?.maxDiscountAmount?.toInt()}",
                                                fontSize = 10.sp,
                                                color = LocalTextSecondary
                                            )
                                        }
                                    }
                                    TextButton(onClick = { viewModel.removeCoupon() }) {
                                        Text("Remove", color = LocalError, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = couponInput,
                                        onValueChange = { couponInput = it.uppercase() },
                                        placeholder = { Text("e.g. AMA10, UTKAL15") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("coupon_input")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (couponInput.isNotBlank()) {
                                                viewModel.applyCoupon(couponInput)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LocalAmberSecondary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("apply_coupon_btn")
                                    ) {
                                        Text("Apply", color = Color.White)
                                    }
                                }

                                if (couponError != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = couponError ?: "",
                                        fontSize = 11.sp,
                                        color = LocalError
                                    )
                                }
                            }
                        }
                    }
                }

                // Bill Summary
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LocalDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Bill Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Items Total", fontSize = 12.sp, color = LocalTextSecondary)
                                Text("₹${subtotal.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            if (discount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Coupon Discount", fontSize = 12.sp, color = LocalSuccess)
                                    Text("-₹${discount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalSuccess)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Delivery Fee", fontSize = 12.sp, color = LocalTextSecondary)
                                Text(if (deliveryFee > 0) "₹${deliveryFee.toInt()}" else "FREE", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("To Pay", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("₹${total.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LocalAmberSecondary)
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
fun CheckoutScreen(viewModel: MainViewModel) {
    val items by viewModel.detailedCartItems.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()

    var deliveryAddress by remember {
        mutableStateOf(currentUser?.location ?: "Near High School, Main Road, Sinapali, Nuapada, Odisha - 766108")
    }
    var contactPhone by remember {
        mutableStateOf(currentUser?.phone ?: "9861011111")
    }
    var selectedDeliveryType by remember { mutableStateOf(DeliveryType.HOME_DELIVERY) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH_ON_DELIVERY) }

    val subtotal = remember(items) { items.sumOf { it.itemTotal } }
    val discount = remember(items, appliedCoupon) {
        if (appliedCoupon != null) {
            (subtotal * (appliedCoupon?.discountPercentage ?: 0) / 100.0)
                .coerceAtMost(appliedCoupon?.maxDiscountAmount ?: 0.0)
        } else 0.0
    }
    val deliveryFee = if (selectedDeliveryType == DeliveryType.HOME_DELIVERY) 25.0 else 0.0
    val total = (subtotal - discount + deliveryFee).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Cart) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Payable", fontSize = 11.sp, color = LocalTextSecondary)
                        Text(
                            text = "₹${total.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LocalAmberSecondary
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.initiateCheckout(
                                deliveryAddress = deliveryAddress,
                                deliveryType = selectedDeliveryType,
                                paymentMethod = selectedPaymentMethod
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("place_order_btn")
                    ) {
                        Text(
                            text = if (selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY) "Place Order (COD)" else "Pay with Razorpay",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LocalBackgroundLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Delivery Address Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = LocalTealPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delivery Address (ଡେଲିଭରୀ ଠିକଣା)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = deliveryAddress,
                            onValueChange = { deliveryAddress = it },
                            label = { Text("Full Address & Landmark") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Contact Phone") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Delivery vs Pickup
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Fulfillment Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedDeliveryType == DeliveryType.HOME_DELIVERY,
                                onClick = { selectedDeliveryType = DeliveryType.HOME_DELIVERY },
                                label = { Text("Home Delivery (₹25)") }
                            )
                            FilterChip(
                                selected = selectedDeliveryType == DeliveryType.STORE_PICKUP,
                                onClick = { selectedDeliveryType = DeliveryType.STORE_PICKUP },
                                label = { Text("Store Pickup (Free)") }
                            )
                        }
                    }
                }
            }

            // Payment Options
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Payment Method (ମୂଲ୍ୟ ଦେୟ ବିକଳ୍ପ)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Cash on Delivery
                        Surface(
                            onClick = { selectedPaymentMethod = PaymentMethod.CASH_ON_DELIVERY },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                if (selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY) 2.dp else 1.dp,
                                if (selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY) LocalAmberSecondary else LocalDivider
                            ),
                            color = if (selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY) LocalAmberSecondary.copy(alpha = 0.08f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY,
                                    onClick = { selectedPaymentMethod = PaymentMethod.CASH_ON_DELIVERY }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Cash on Delivery (COD)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Pay cash or UPI to delivery agent at your door", fontSize = 11.sp, color = LocalTextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Razorpay Online Payment
                        Surface(
                            onClick = { selectedPaymentMethod = PaymentMethod.RAZORPAY_ONLINE },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                if (selectedPaymentMethod == PaymentMethod.RAZORPAY_ONLINE) 2.dp else 1.dp,
                                if (selectedPaymentMethod == PaymentMethod.RAZORPAY_ONLINE) Color(0xFF3395FF) else LocalDivider
                            ),
                            color = if (selectedPaymentMethod == PaymentMethod.RAZORPAY_ONLINE) Color(0xFF3395FF).copy(alpha = 0.08f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == PaymentMethod.RAZORPAY_ONLINE,
                                    onClick = { selectedPaymentMethod = PaymentMethod.RAZORPAY_ONLINE }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Razorpay Online Payment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(4.dp)) {
                                            Text("UPI / Cards", fontSize = 9.sp, color = Color(0xFF1565C0), modifier = Modifier.padding(3.dp))
                                        }
                                    }
                                    Text("Google Pay, PhonePe, Paytm, Debit/Credit Card", fontSize = 11.sp, color = LocalTextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Items Summary
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Order Items (${items.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.productName}",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "₹${item.itemTotal.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
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
fun OrderTrackingScreen(
    orderId: Long,
    viewModel: MainViewModel
) {
    val orders by viewModel.allOrders.collectAsState()
    val order = orders.find { it.id == orderId }

    if (order == null) {
        EmptyStateView(
            icon = Icons.Default.ReceiptLong,
            title = "Order not found",
            message = "Order details are unavailable.",
            actionText = "Back to Home",
            onAction = { viewModel.navigateTo(Screen.CustomerHome) }
        )
        return
    }

    val steps = listOf(
        OrderStatus.ORDER_PLACED to "Order Placed (ଅର୍ଡର ସ୍ୱୀକୃତ)",
        OrderStatus.ORDER_CONFIRMED to "Order Confirmed by Shop",
        OrderStatus.PREPARING to "Packing & Preparing Item",
        OrderStatus.READY_FOR_DELIVERY to "Ready for Delivery",
        OrderStatus.DELIVERY_ACCEPTED to "Admin Accepted Delivery (ହାରିଶବାବୁ)",
        OrderStatus.PICKED_UP to "Admin Picked Up from Shop",
        OrderStatus.OUT_FOR_DELIVERY to "Out for Delivery to Your Doorstep",
        OrderStatus.DELIVERED to "Delivered Successfully (ଡେଲିଭରୀ ସମ୍ପନ୍ନ)"
    )

    val currentStepIndex = when (order.orderStatus) {
        OrderStatus.ORDER_PLACED -> 0
        OrderStatus.ORDER_CONFIRMED -> 1
        OrderStatus.PREPARING -> 2
        OrderStatus.READY_FOR_DELIVERY -> 3
        OrderStatus.DELIVERY_ACCEPTED -> 4
        OrderStatus.PICKED_UP -> 5
        OrderStatus.OUT_FOR_DELIVERY -> 6
        OrderStatus.DELIVERED -> 7
        OrderStatus.CANCELLED -> -1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Order #${order.orderNumber}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
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
            // Status Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalTealPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.orderStatus.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(color = LocalAmberSecondary, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = order.orderStatus.odiaLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Estimated delivery within 90-120 minutes by ${order.shopName}",
                            fontSize = 12.sp,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }
            }

            // Timeline Steps
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Live Order Progress",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        steps.forEachIndexed { index, pair ->
                            val isCompleted = index <= currentStepIndex
                            val isCurrent = index == currentStepIndex

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCompleted) LocalSuccess else Color(0xFFE0E0E0)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                    if (index < steps.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(28.dp)
                                                .background(
                                                    if (index < currentStepIndex) LocalSuccess else Color(0xFFE0E0E0)
                                                )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = pair.second,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isCompleted) LocalTextPrimary else LocalTextMuted
                                    )
                                    if (isCurrent) {
                                        Text(
                                            text = "Active Status Now",
                                            fontSize = 11.sp,
                                            color = LocalSuccess,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Customer Delivery OTP Card (shown for personal admin delivery verification)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                    border = BorderStroke(1.dp, Color(0xFFCE93D8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Delivery Verification OTP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF4A148C)
                                )
                                Text(
                                    text = "Share this OTP with Admin when delivery arrives at your door",
                                    fontSize = 11.sp,
                                    color = LocalTextSecondary
                                )
                            }
                            Surface(
                                color = Color(0xFF7B1FA2),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = order.deliveryOtp,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    letterSpacing = 3.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                        if (order.deliveredBy != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Delivered by: ${order.deliveredBy}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LocalSuccess
                            )
                        }
                    }
                }
            }

            // Order Details Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Order Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Shop Name", fontSize = 12.sp, color = LocalTextSecondary)
                            Text(order.shopName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalTealPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Payment", fontSize = 12.sp, color = LocalTextSecondary)
                            Text("${order.paymentMethod.label} (${order.paymentStatus.label})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        if (order.razorpayPaymentId != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Razorpay Ref", fontSize = 12.sp, color = LocalTextSecondary)
                                Text(order.razorpayPaymentId, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1565C0))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Deliver To", fontSize = 12.sp, color = LocalTextSecondary)
                            Text(order.deliveryAddress, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Paid/Due", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("₹${order.totalAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LocalAmberSecondary)
                        }
                    }
                }
            }

            // Quick Support Action
            item {
                OutlinedButton(
                    onClick = {
                        viewModel.fileComplaint(
                            shopId = order.shopId,
                            shopName = order.shopName,
                            subject = "Need help with order #${order.orderNumber}",
                            description = "Customer inquiry regarding order delivery status."
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.SupportAgent, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Need Help? Contact Local Shop / Support")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    viewModel: MainViewModel,
    onRoleSwitchClick: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    val wishlistIds by viewModel.wishlistProductIds.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    val customerOrders = remember(orders, currentUser) {
        orders.filter { it.customerId == (currentUser?.id ?: 0L) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile & Account", fontWeight = FontWeight.Bold) },
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
            // Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, LocalDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(LocalAmberSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = currentUser?.name ?: "Priya Mishra",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Phone: +91 ${currentUser?.phone ?: "9861011111"}",
                                fontSize = 12.sp,
                                color = LocalTextSecondary
                            )
                            Text(
                                text = currentUser?.location ?: "Main Road, Sinapali, Nuapada",
                                fontSize = 12.sp,
                                color = LocalTextMuted
                            )
                        }
                    }
                }
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LocalDivider),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${customerOrders.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalTealPrimary
                            )
                            Text("Total Orders", fontSize = 11.sp, color = LocalTextSecondary)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LocalDivider),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${wishlistIds.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                            Text("Wishlist Items", fontSize = 11.sp, color = LocalTextSecondary)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LocalDivider),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${notifications.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAmberSecondary
                            )
                            Text("Alerts", fontSize = 11.sp, color = LocalTextSecondary)
                        }
                    }
                }
            }

            // Orders list header
            item {
                Text(
                    text = "Recent Orders (ମୋର ଅର୍ଡର)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (customerOrders.isEmpty()) {
                item {
                    Text(
                        text = "No orders placed yet.",
                        fontSize = 12.sp,
                        color = LocalTextSecondary
                    )
                }
            } else {
                items(customerOrders) { order ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LocalDivider),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.OrderTracking(order.id)) }
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
                                Text(order.shopName, fontSize = 11.sp, color = LocalTealPrimary)
                                Text("₹${order.totalAmount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            OrderStatusBadge(status = order.orderStatus)
                        }
                    }
                }
            }

            // Role Switcher Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRoleSwitchClick,
                    colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch Role (Shop Owner / Admin Mode)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrdersScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val orders by viewModel.allOrders.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val customerOrders = remember(orders, currentUser) {
        orders.filter { it.customerId == (currentUser?.id ?: 0L) }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Orders", "In Progress", "Delivered")

    val filteredOrders = remember(customerOrders, selectedTab) {
        when (selectedTab) {
            1 -> customerOrders.filter { it.orderStatus != OrderStatus.DELIVERED && it.orderStatus != OrderStatus.CANCELLED }
            2 -> customerOrders.filter { it.orderStatus == OrderStatus.DELIVERED }
            else -> customerOrders
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Orders", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "ମୋର ଅର୍ଡର • ${customerOrders.size} total",
                            fontSize = 11.sp,
                            color = LocalTextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Cart) }) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Cart")
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = LocalTealPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) LocalTealPrimary else LocalTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.ReceiptLong,
                        title = if (selectedTab == 0) "No Orders Yet" else "No orders in this tab",
                        message = "Explore items from local Sinapali stores and place an order.",
                        actionText = "Explore Local Mart",
                        onAction = { viewModel.navigateTo(Screen.CustomerHome) }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders) { order ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, LocalDivider),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screen.OrderTracking(order.id)) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = order.orderNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Store: ${order.shopName}",
                                            fontSize = 12.sp,
                                            color = LocalTealPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    OrderStatusBadge(status = order.orderStatus)
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = LocalDivider.copy(alpha = 0.6f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Delivery to: ${order.deliveryAddress}",
                                            fontSize = 11.sp,
                                            color = LocalTextSecondary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Payment: ${order.paymentMethod.name} • ${order.paymentStatus.name}",
                                            fontSize = 11.sp,
                                            color = LocalTextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "₹${order.totalAmount.toInt()}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAmberSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.navigateTo(Screen.OrderTracking(order.id)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalTealPrimary)
                                    ) {
                                        Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Track Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.navigateTo(Screen.ShopDetail(order.shopId)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                                    ) {
                                        Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Shop More", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
