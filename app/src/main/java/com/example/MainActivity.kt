package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.UserRole
import com.example.ui.admin.*
import com.example.ui.components.AccessDeniedScreen
import com.example.ui.customer.*
import com.example.ui.shop.*
import com.example.ui.theme.LocalAmberSecondary
import com.example.ui.theme.LocalTealPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val currentRole by viewModel.currentRole.collectAsState()
                val currentUser by viewModel.currentUser.collectAsState()
                val allShops by viewModel.allShops.collectAsState()
                val allProducts by viewModel.allProducts.collectAsState()
                val userMessage by viewModel.userMessage.collectAsState()
                val showPaymentModal by viewModel.showPaymentModal.collectAsState()

                val isShopOwnerHome = currentRole == UserRole.SHOP_OWNER && currentScreen == Screen.ShopOwnerDashboard
                BackHandler(enabled = currentScreen != Screen.CustomerHome && currentScreen != Screen.Splash && !isShopOwnerHome) {
                    viewModel.navigateBack()
                }

                var showPortalSwitcher by remember { mutableStateOf(false) }
                var portalTab by remember { mutableStateOf(0) }
                var shopPhoneInput by remember { mutableStateOf("9437012345") }
                var adminPinInput by remember { mutableStateOf("") }
                var portalError by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    snackbarHost = {
                        userMessage?.let { msg ->
                            Snackbar(
                                modifier = Modifier.padding(16.dp),
                                action = {
                                    TextButton(onClick = { viewModel.clearUserMessage() }) {
                                        Text("Dismiss", color = LocalAmberSecondary)
                                    }
                                }
                            ) {
                                Text(msg)
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (val s = currentScreen) {
                            is Screen.Splash -> SplashScreen {
                                viewModel.navigateTo(Screen.CustomerHome)
                            }
                            is Screen.CustomerAuth -> {
                                if (currentRole == UserRole.SHOP_OWNER) {
                                    AccessDeniedScreen(
                                        title = "Customer Account Restricted",
                                        message = "Shop Owners must never access customer accounts. Please use the Shop Owner portal.",
                                        onGoBack = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }
                                    )
                                } else {
                                    CustomerAuthScreen(
                                        viewModel = viewModel,
                                        onSuccess = { viewModel.navigateTo(Screen.CustomerHome) }
                                    )
                                }
                            }
                            is Screen.CustomerHome -> {
                                if (currentRole == UserRole.SHOP_OWNER) {
                                    ShopOwnerDashboardScreen(
                                        viewModel = viewModel,
                                        onRoleSwitchClick = {
                                            portalError = null
                                            showPortalSwitcher = true
                                        }
                                    )
                                } else {
                                    CustomerHomeScreen(
                                        viewModel = viewModel,
                                        onRoleSwitchClick = { showPortalSwitcher = true }
                                    )
                                }
                            }
                            is Screen.Categories -> {
                                if (currentRole == UserRole.SHOP_OWNER) {
                                    AccessDeniedScreen(
                                        title = "Access Restricted",
                                        message = "Shop Owners can only view and manage their own shop products.",
                                        onGoBack = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }
                                    )
                                } else {
                                    CategoriesScreen(viewModel)
                                }
                            }
                            is Screen.ShopDetail -> {
                                if (currentRole == UserRole.SHOP_OWNER) {
                                    val myShop = allShops.find { it.ownerId == currentUser?.id || (currentUser?.shopId != null && it.id == currentUser?.shopId) }
                                    if (myShop != null && s.shopId != myShop.id) {
                                        AccessDeniedScreen(
                                            title = "Other Shop Restricted",
                                            message = "Shop Owners must never access other shops' data. You can only view your own shop.",
                                            onGoBack = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }
                                        )
                                    } else {
                                        ShopDetailScreen(s.shopId, viewModel)
                                    }
                                } else {
                                    ShopDetailScreen(s.shopId, viewModel)
                                }
                            }
                            is Screen.ProductDetail -> {
                                if (currentRole == UserRole.SHOP_OWNER) {
                                    val prod = allProducts.find { it.id == s.productId }
                                    val myShop = allShops.find { it.ownerId == currentUser?.id || (currentUser?.shopId != null && it.id == currentUser?.shopId) }
                                    if (prod != null && myShop != null && prod.shopId != myShop.id) {
                                        AccessDeniedScreen(
                                            title = "Other Shop Restricted",
                                            message = "Shop Owners must never access other shops' products.",
                                            onGoBack = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }
                                        )
                                    } else {
                                        ProductDetailScreen(s.productId, viewModel)
                                    }
                                } else {
                                    ProductDetailScreen(s.productId, viewModel)
                                }
                            }
                            is Screen.Cart -> {
                                if (currentRole == UserRole.SHOP_OWNER) {
                                    AccessDeniedScreen(
                                        title = "Customer Cart Restricted",
                                        message = "Shop Owners must never access customer carts.",
                                        onGoBack = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }
                                    )
                                } else {
                                    CartScreen(viewModel)
                                }
                            }
                            is Screen.Checkout -> {
                                if (currentRole == UserRole.SHOP_OWNER) {
                                    AccessDeniedScreen(
                                        title = "Checkout Restricted",
                                        message = "Shop Owners must never access customer checkout or carts.",
                                        onGoBack = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }
                                    )
                                } else {
                                    CheckoutScreen(viewModel)
                                }
                            }
                            is Screen.OrderTracking -> {
                                if (currentRole == UserRole.SHOP_OWNER) {
                                    AccessDeniedScreen(
                                        title = "Order Tracking Restricted",
                                        message = "Shop Owners must never access customer order tracking. Shop Owners view their orders from the Shop Owner dashboard.",
                                        onGoBack = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }
                                    )
                                } else {
                                    OrderTrackingScreen(s.orderId, viewModel)
                                }
                            }
                            is Screen.CustomerOrders -> {
                                if (currentRole == UserRole.SHOP_OWNER) {
                                    AccessDeniedScreen(
                                        title = "Customer Order History Restricted",
                                        message = "Shop Owners must never access customer order history. Shop Owners can only view their own shop orders.",
                                        onGoBack = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }
                                    )
                                } else {
                                    CustomerOrdersScreen(viewModel)
                                }
                            }
                            is Screen.CustomerProfile -> {
                                if (currentRole == UserRole.SHOP_OWNER) {
                                    AccessDeniedScreen(
                                        title = "Customer Profile Restricted",
                                        message = "Shop Owners must never access customer accounts or profiles.",
                                        onGoBack = { viewModel.navigateTo(Screen.ShopOwnerDashboard) }
                                    )
                                } else {
                                    CustomerProfileScreen(
                                        viewModel = viewModel,
                                        onRoleSwitchClick = { showPortalSwitcher = true }
                                    )
                                }
                            }
                            is Screen.CustomerHelp -> CustomerHelpScreen(viewModel)
                            is Screen.PrivacyPolicy -> PrivacyPolicyScreen(viewModel)
                            is Screen.ContactUs -> ContactUsScreen(viewModel)
                            is Screen.AboutUs -> AboutUsScreen(viewModel)
                            is Screen.TermsConditions -> TermsConditionsScreen(viewModel)
                            is Screen.ReturnsRefunds -> ReturnsRefundsScreen(viewModel)

                            // Shop Owner Screens: Strictly restricted to UserRole.SHOP_OWNER
                            is Screen.ShopOwnerAuth -> {
                                ShopOwnerAuthScreen(
                                    viewModel = viewModel,
                                    onSuccess = {
                                        viewModel.navigateTo(Screen.ShopOwnerDashboard)
                                    }
                                )
                            }
                            is Screen.ShopOwnerDashboard, is Screen.ShopOrderManagement -> {
                                if (currentRole == UserRole.SHOP_OWNER && currentUser?.role == UserRole.SHOP_OWNER) {
                                    ShopOwnerDashboardScreen(
                                        viewModel = viewModel,
                                        onRoleSwitchClick = {
                                            portalError = null
                                            showPortalSwitcher = true
                                        }
                                    )
                                } else {
                                    AccessDeniedScreen(
                                        title = "Shop Owner Access Denied",
                                        message = "Customers must never access the Shop Owner dashboard. Only authorized Shop Owners can access their shop dashboard with verified mobile & OTP.",
                                        onGoBack = { viewModel.switchToCustomerView() },
                                        secondaryActionText = "Shop Owner Login (OTP)",
                                        onSecondaryAction = { viewModel.navigateTo(Screen.ShopOwnerAuth) }
                                    )
                                }
                            }
                            is Screen.ShopProductManagement -> {
                                if (currentRole == UserRole.SHOP_OWNER && currentUser?.role == UserRole.SHOP_OWNER) {
                                    ShopProductManagementScreen(viewModel)
                                } else {
                                    AccessDeniedScreen(
                                        title = "Shop Owner Access Denied",
                                        message = "Customers cannot manage shop products.",
                                        onGoBack = { viewModel.switchToCustomerView() },
                                        secondaryActionText = "Shop Owner Login (OTP)",
                                        onSecondaryAction = { viewModel.navigateTo(Screen.ShopOwnerAuth) }
                                    )
                                }
                            }
                            is Screen.ShopOffersPromotions -> {
                                if (currentRole == UserRole.SHOP_OWNER && currentUser?.role == UserRole.SHOP_OWNER) {
                                    ShopOffersPromotionsScreen(viewModel)
                                } else {
                                    AccessDeniedScreen(
                                        title = "Shop Owner Access Denied",
                                        message = "Customers cannot request shop promotions.",
                                        onGoBack = { viewModel.switchToCustomerView() },
                                        secondaryActionText = "Shop Owner Login (OTP)",
                                        onSecondaryAction = { viewModel.navigateTo(Screen.ShopOwnerAuth) }
                                    )
                                }
                            }

                            // Admin Screens: Strictly restricted to UserRole.ADMIN
                            is Screen.AdminDashboard -> {
                                if (currentRole == UserRole.ADMIN && currentUser?.role == UserRole.ADMIN) {
                                    AdminDashboardScreen(
                                        viewModel = viewModel,
                                        onRoleSwitchClick = {
                                            portalError = null
                                            showPortalSwitcher = true
                                        }
                                    )
                                } else {
                                    AccessDeniedScreen(
                                        title = "Admin Panel Restricted",
                                        message = "Customers and Shop Owners cannot access the Admin Panel. Only Administrator (Harishchandra Sagar) has access.",
                                        onGoBack = { viewModel.switchToCustomerView() }
                                    )
                                }
                            }
                            is Screen.AdminDelivery -> {
                                if (currentRole == UserRole.ADMIN && currentUser?.role == UserRole.ADMIN) {
                                    AdminDeliveryScreen(viewModel)
                                } else {
                                    AccessDeniedScreen(
                                        title = "Admin Panel Restricted",
                                        message = "Only Admin can access Delivery Operations.",
                                        onGoBack = { viewModel.switchToCustomerView() }
                                    )
                                }
                            }
                            is Screen.AdminOrders -> {
                                if (currentRole == UserRole.ADMIN && currentUser?.role == UserRole.ADMIN) {
                                    AdminOrdersScreen(viewModel)
                                } else {
                                    AccessDeniedScreen(
                                        title = "Admin Panel Restricted",
                                        message = "Only Admin can access All Orders.",
                                        onGoBack = { viewModel.switchToCustomerView() }
                                    )
                                }
                            }
                            is Screen.AdminShops -> {
                                if (currentRole == UserRole.ADMIN && currentUser?.role == UserRole.ADMIN) {
                                    AdminShopsScreen(viewModel)
                                } else {
                                    AccessDeniedScreen(
                                        title = "Admin Panel Restricted",
                                        message = "Only Admin can access Shop Approvals.",
                                        onGoBack = { viewModel.switchToCustomerView() }
                                    )
                                }
                            }
                            is Screen.AdminPromotions -> {
                                if (currentRole == UserRole.ADMIN && currentUser?.role == UserRole.ADMIN) {
                                    AdminPromotionsScreen(viewModel)
                                } else {
                                    AccessDeniedScreen(
                                        title = "Admin Panel Restricted",
                                        message = "Only Admin can access Platform Promotions.",
                                        onGoBack = { viewModel.switchToCustomerView() }
                                    )
                                }
                            }

                            else -> CustomerHomeScreen(
                                viewModel = viewModel,
                                onRoleSwitchClick = {
                                    portalError = null
                                    showPortalSwitcher = true
                                }
                            )
                        }

                        // Role-Based Access Control Dialog
                        if (showPortalSwitcher) {
                            AlertDialog(
                                onDismissRequest = {
                                    showPortalSwitcher = false
                                    portalError = null
                                },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = LocalTealPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Role & Portal Access Control", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Current Role Indicator
                                        Surface(
                                            color = when (currentRole) {
                                                UserRole.ADMIN -> Color(0xFF1E293B)
                                                UserRole.SHOP_OWNER -> Color(0xFFE65100)
                                                UserRole.CUSTOMER -> LocalTealPrimary
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = when (currentRole) {
                                                        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                                        UserRole.SHOP_OWNER -> Icons.Default.Storefront
                                                        UserRole.CUSTOMER -> Icons.Default.Person
                                                    },
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Current: ${currentRole.name.replace('_', ' ')} (${currentUser?.name ?: "Guest"})",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        if (currentRole == UserRole.ADMIN) {
                                            Text(
                                                "You have Full Administrator access to manage shops, orders, and town delivery.",
                                                fontSize = 13.sp
                                            )
                                            Button(
                                                onClick = {
                                                    viewModel.navigateTo(Screen.AdminDashboard)
                                                    showPortalSwitcher = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                            ) {
                                                Icon(Icons.Default.Dashboard, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Open Admin Panel")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.switchToCustomerView()
                                                    showPortalSwitcher = false
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Storefront, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Switch to Customer Store View")
                                            }
                                        } else if (currentRole == UserRole.SHOP_OWNER) {
                                            val currUser = currentUser
                                            val myShop = allShops.find { it.ownerId == currUser?.id || (currUser?.shopId != null && it.id == currUser.shopId) }
                                            Text(
                                                "You have access ONLY to your shop: ${myShop?.name ?: "Assigned Shop"}.",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Button(
                                                onClick = {
                                                    viewModel.navigateTo(Screen.ShopOwnerDashboard)
                                                    showPortalSwitcher = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                            ) {
                                                Icon(Icons.Default.Dashboard, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Open My Shop Dashboard")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.switchToCustomerView()
                                                    showPortalSwitcher = false
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.ShoppingBag, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Switch to Customer Store View")
                                            }
                                        } else {
                                            // CUSTOMER MODE
                                            Text(
                                                "Customers must never access Admin Panel or Shop Owner dashboard. To access, verified merchant or admin credentials are required:",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )

                                            TabRow(
                                                selectedTabIndex = portalTab,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Tab(
                                                    selected = portalTab == 0,
                                                    onClick = {
                                                        portalTab = 0
                                                        portalError = null
                                                    },
                                                    text = { Text("Shop Owner", fontWeight = FontWeight.Bold) }
                                                )
                                                Tab(
                                                    selected = portalTab == 1,
                                                    onClick = {
                                                        portalTab = 1
                                                        portalError = null
                                                    },
                                                    text = { Text("Admin Only", fontWeight = FontWeight.Bold) }
                                                )
                                            }

                                            if (portalTab == 0) {
                                                val isOtpSent by viewModel.isShopOwnerOtpSent.collectAsState()
                                                val otpCode by viewModel.shopOwnerOtpCode.collectAsState()
                                                var inlineOtp by remember { mutableStateOf("") }

                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Text(
                                                        "Enter registered Shop Owner mobile number (e.g. Ramesh Sahu 9437012345, Santosh Meher 9437054321):",
                                                        fontSize = 11.sp
                                                    )
                                                    OutlinedTextField(
                                                        value = shopPhoneInput,
                                                        onValueChange = {
                                                            if (!isOtpSent) {
                                                                shopPhoneInput = it
                                                                portalError = null
                                                            }
                                                        },
                                                        readOnly = isOtpSent,
                                                        label = { Text("Shop Owner Mobile") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true
                                                    )

                                                    if (!isOtpSent) {
                                                        Button(
                                                            onClick = {
                                                                val ok = viewModel.sendShopOwnerOtp(shopPhoneInput)
                                                                if (!ok) {
                                                                    portalError = viewModel.shopOwnerAuthError.value
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                                        ) {
                                                            Icon(Icons.Default.SendToMobile, contentDescription = null)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Send Verification OTP")
                                                        }
                                                    } else {
                                                        otpCode?.let { code ->
                                                            Surface(
                                                                color = Color(0xFFE8F5E9),
                                                                shape = RoundedCornerShape(6.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text(
                                                                    "Demo OTP: $code (or 123456)",
                                                                    color = Color(0xFF2E7D32),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(8.dp)
                                                                )
                                                            }
                                                        }

                                                        OutlinedTextField(
                                                            value = inlineOtp,
                                                            onValueChange = { inlineOtp = it.take(6) },
                                                            label = { Text("Enter 6-digit OTP") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Button(
                                                            onClick = {
                                                                viewModel.verifyShopOwnerOtp(inlineOtp) { success, _, msg ->
                                                                    if (success) {
                                                                        showPortalSwitcher = false
                                                                        inlineOtp = ""
                                                                    } else {
                                                                        portalError = msg
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                                        ) {
                                                            Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Verify OTP & Open Shop")
                                                        }
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            showPortalSwitcher = false
                                                            portalError = null
                                                            viewModel.navigateTo(Screen.ShopOwnerAuth)
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Icon(Icons.Default.Storefront, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Dedicated Shop Login Screen", fontSize = 12.sp)
                                                    }
                                                }
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        "Enter Admin Security PIN to access Harishchandra Sagar's Admin Panel:",
                                                        fontSize = 11.sp
                                                    )
                                                    OutlinedTextField(
                                                        value = adminPinInput,
                                                        onValueChange = {
                                                            adminPinInput = it
                                                            portalError = null
                                                        },
                                                        label = { Text("Admin PIN") },
                                                        visualTransformation = PasswordVisualTransformation(),
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true
                                                    )
                                                    Button(
                                                        onClick = {
                                                            viewModel.loginAdminWithPin(adminPinInput) { success, msg ->
                                                                if (success) {
                                                                    showPortalSwitcher = false
                                                                    adminPinInput = ""
                                                                } else {
                                                                    portalError = msg
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                                    ) {
                                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Unlock Admin Panel")
                                                    }
                                                }
                                            }

                                            portalError?.let { err ->
                                                Text(
                                                    text = err,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = {
                                        showPortalSwitcher = false
                                        portalError = null
                                    }) {
                                        Text("Close")
                                    }
                                }
                            )
                        }

                        // Simulated Razorpay Payment Flow
                        if (showPaymentModal) {
                            AlertDialog(
                                onDismissRequest = { viewModel.onRazorpayPaymentDismiss() },
                                title = { Text("Razorpay Payment Gateway") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Key ID: ${viewModel.paymentService.getRazorpayKeyId()}")
                                        Text("Simulating Razorpay UPI / Card payment verification...")
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val payId = "pay_sim_${System.currentTimeMillis() % 1000000}"
                                            viewModel.onRazorpayPaymentSuccess(payId)
                                        }
                                    ) {
                                        Text("Pay Successful")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { viewModel.onRazorpayPaymentDismiss() }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
