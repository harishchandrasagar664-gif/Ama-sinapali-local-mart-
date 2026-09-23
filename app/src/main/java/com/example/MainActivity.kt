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
import com.example.ui.customer.*
import com.example.ui.shop.*
import com.example.ui.theme.LocalAmberSecondary
import com.example.ui.theme.LocalTealPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val currentRole by viewModel.currentRole.collectAsState()
                val userMessage by viewModel.userMessage.collectAsState()
                val showPaymentModal by viewModel.showPaymentModal.collectAsState()

                var showPortalSwitcher by remember { mutableStateOf(false) }

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
                            is Screen.CustomerHome -> CustomerHomeScreen(
                                viewModel = viewModel,
                                onRoleSwitchClick = { showPortalSwitcher = true }
                            )
                            is Screen.Categories -> CategoriesScreen(viewModel)
                            is Screen.ShopDetail -> ShopDetailScreen(s.shopId, viewModel)
                            is Screen.ProductDetail -> ProductDetailScreen(s.productId, viewModel)
                            is Screen.Cart -> CartScreen(viewModel)
                            is Screen.Checkout -> CheckoutScreen(viewModel)
                            is Screen.OrderTracking -> OrderTrackingScreen(s.orderId, viewModel)
                            is Screen.CustomerOrders -> CustomerOrdersScreen(viewModel)
                            is Screen.CustomerProfile -> CustomerProfileScreen(
                                viewModel = viewModel,
                                onRoleSwitchClick = { showPortalSwitcher = true }
                            )
                            is Screen.CustomerHelp -> CustomerHelpScreen(viewModel)

                            // Shop Owner Screens
                            is Screen.ShopOwnerDashboard, is Screen.ShopOrderManagement -> ShopOwnerDashboardScreen(
                                viewModel = viewModel,
                                onRoleSwitchClick = { showPortalSwitcher = true }
                            )
                            is Screen.ShopProductManagement -> ShopProductManagementScreen(viewModel)
                            is Screen.ShopOffersPromotions -> ShopOffersPromotionsScreen(viewModel)

                            // Admin Screens
                            is Screen.AdminDashboard -> AdminDashboardScreen(
                                viewModel = viewModel,
                                onRoleSwitchClick = { showPortalSwitcher = true }
                            )
                            is Screen.AdminDelivery -> AdminDeliveryScreen(viewModel)
                            is Screen.AdminOrders -> AdminOrdersScreen(viewModel)
                            is Screen.AdminShops -> AdminShopsScreen(viewModel)
                            is Screen.AdminPromotions -> AdminPromotionsScreen(viewModel)

                            else -> CustomerHomeScreen(
                                viewModel = viewModel,
                                onRoleSwitchClick = { showPortalSwitcher = true }
                            )
                        }

                        // Portal Switcher Dialog
                        if (showPortalSwitcher) {
                            AlertDialog(
                                onDismissRequest = { showPortalSwitcher = false },
                                title = { Text("Switch Ama Sinapali Portal") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Select role to test multi-shop, customer & delivery features:")

                                        Button(
                                            onClick = {
                                                viewModel.authManager.switchRole(UserRole.CUSTOMER)
                                                viewModel.navigateTo(Screen.CustomerHome)
                                                showPortalSwitcher = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                                        ) {
                                            Text("Customer Portal (Browse & Order)")
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.authManager.switchRole(UserRole.SHOP_OWNER)
                                                viewModel.navigateTo(Screen.ShopOwnerDashboard)
                                                showPortalSwitcher = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                        ) {
                                            Text("Shop Owner Portal (Manage Orders/Products)")
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.authManager.switchRole(UserRole.ADMIN)
                                                viewModel.navigateTo(Screen.AdminDashboard)
                                                showPortalSwitcher = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Text("Admin & Delivery Portal (Harishchandra)")
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showPortalSwitcher = false }) {
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
