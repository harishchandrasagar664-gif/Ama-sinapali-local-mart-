@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.locale.AppStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun PrivacyPolicyScreen(viewModel: MainViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.privacyPolicyTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerProfile) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalTealPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
                InfoBannerCard(
                    title = "Ama Sinapali Privacy Commitment",
                    subtitle = "Last Updated: September 2026 • Sinapali, Nuapada, Odisha",
                    icon = Icons.Default.Security
                )
            }

            item {
                InfoSectionCard(
                    title = "1. Information We Collect",
                    icon = Icons.Default.ContactPhone,
                    content = "When you register or place an order on Ama Sinapali Local Mart, we collect:\n\n• Mobile Number or Email (verified via OTP) for account identification and order updates.\n• Customer Name for billing and package handover.\n• Delivery Address including Odisha District (Nuapada), Block (Sinapali), Gram Panchayat (GP), Village/Town, and full street/ward details.\n• Order and transaction history (Cash on Delivery receipts and cart items)."
                )
            }

            item {
                InfoSectionCard(
                    title = "2. How We Use Your Information",
                    icon = Icons.Default.LocalShipping,
                    content = "Your information is used strictly to provide hyper-local commerce services in Sinapali:\n\n• Dispatching orders to local shop merchants for preparation.\n• Accurate doorstep delivery by our local delivery personnel.\n• Sending order status notifications and verification OTPs.\n• Resolving customer queries, delivery tracking, and complaints."
                )
            }

            item {
                InfoSectionCard(
                    title = "3. Data Sharing & Security",
                    icon = Icons.Default.Lock,
                    content = "We do NOT sell, rent, or trade your personal information to third parties.\n\n• Only the assigned local shop owner sees the items ordered and delivery destination required for fulfillment.\n• All data is stored securely using local on-device encrypted Room storage and protected backend services.\n• Payment information for Cash on Delivery is handled in person upon verified item receipt."
                )
            }

            item {
                InfoSectionCard(
                    title = "4. Your Rights & Data Control",
                    icon = Icons.Default.ManageAccounts,
                    content = "You have full control over your saved information:\n\n• You can view and edit your saved Odisha delivery address anytime under My Profile.\n• You can change your registered phone or email by verifying via OTP.\n• To request account closure or data deletion, contact the Admin directly."
                )
            }

            item {
                InfoSectionCard(
                    title = "5. Grievance Officer & Contact",
                    icon = Icons.Default.AdminPanelSettings,
                    content = "For any privacy-related queries, reach out to:\n\nHarishchandra Sagar (Administrator)\nAma Sinapali Local Mart\nPhysical Office Location: Kotamal, Sinapali Main Road, Dist: Nuapada, Odisha - 766108\nMobile Number: 7894952827\nEmail: harishchandrasagar664@gmail.com"
                )
            }
        }
    }
}

@Composable
fun ContactUsScreen(viewModel: MainViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.contactUsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerProfile) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalTealPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
                InfoBannerCard(
                    title = "We are Here to Help! 🙏",
                    subtitle = "Local Support for Sinapali & surrounding Gram Panchayats",
                    icon = Icons.Default.SupportAgent
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Get in Touch with Administration", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        ContactRow(
                            icon = Icons.Default.Person,
                            label = "Administrator",
                            value = "Harishchandra Sagar"
                        )
                        HorizontalDivider()

                        ContactRow(
                            icon = Icons.Default.Phone,
                            label = "Mobile Number (Call & WhatsApp)",
                            value = "+91 7894952827"
                        )
                        HorizontalDivider()

                        ContactRow(
                            icon = Icons.Default.Email,
                            label = "Email Support",
                            value = "harishchandrasagar664@gmail.com"
                        )
                        HorizontalDivider()

                        ContactRow(
                            icon = Icons.Default.LocationOn,
                            label = "Physical Office Location",
                            value = "Kotamal, Sinapali Main Road, Dist: Nuapada, Odisha - 766108"
                        )
                        HorizontalDivider()

                        ContactRow(
                            icon = Icons.Default.Schedule,
                            label = "Operating Hours",
                            value = "Monday - Sunday: 7:00 AM - 9:00 PM"
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Have an issue with an existing order?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = LocalTealDark
                        )
                        Text(
                            "You can file a direct support ticket or complaint to the Admin with order details and subject.",
                            fontSize = 13.sp,
                            color = LocalTextSecondary
                        )
                        Button(
                            onClick = { viewModel.navigateTo(Screen.CustomerHelp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("File Complaint / Query to Admin")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutUsScreen(viewModel: MainViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.aboutUsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerProfile) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalTealPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalTealPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = LocalAmberLight, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "ଆମ ସିନାପାଲି ଲୋକାଲ ମାର୍ଟ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LocalAmberLight
                        )
                        Text(
                            "Ama Sinapali Local Mart",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Empowering Sinapali Town & Rural Merchants",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            item {
                InfoSectionCard(
                    title = "Our Mission",
                    icon = Icons.Default.Flag,
                    content = "Ama Sinapali Local Mart was built with a singular vision: to empower local shopkeepers, vegetable vendors, grocery stores, and sweets makers in Sinapali (Nuapada District) by connecting them directly to town and village residents via modern hyper-local technology."
                )
            }

            item {
                InfoSectionCard(
                    title = "What Makes Us Unique",
                    icon = Icons.Default.Stars,
                    content = "• Multi-Shop Convenience: Order from multiple local shops (Kirana, Daily Vegetable Market, Sweets, Stationery) in one seamless experience.\n\n• Vocal for Local: 100% of revenue directly supports our neighborhood Sinapali businesses.\n\n• Transparent Cash on Delivery: Inspect your items in person before paying.\n\n• Gram Panchayat Reach: Service designed for both Sinapali town wards and surrounding Gram Panchayats like Hatibandh, Timanpur, Karlakot, Ghilaguda, and Litiguda."
                )
            }

            item {
                InfoSectionCard(
                    title = "Leadership & Local Commitment",
                    icon = Icons.Default.Handshake,
                    content = "Led by Harishchandra Sagar, Ama Sinapali Local Mart bridges the digital divide for local commerce in Western Odisha. We take personal pride in every order delivered quickly and safely to your home.\n\n• Physical Office Location: Kotamal, Sinapali Main Road\n• Mobile Helpline: 7894952827\n• Email: harishchandrasagar664@gmail.com"
                )
            }
        }
    }
}

@Composable
fun TermsConditionsScreen(viewModel: MainViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.termsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerProfile) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalTealPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
                InfoBannerCard(
                    title = "User Terms & Store Agreement",
                    subtitle = "Effective for all customers, shops, and delivery orders in Sinapali",
                    icon = Icons.Default.Gavel
                )
            }

            item {
                InfoSectionCard(
                    title = "1. Account & Verification",
                    icon = Icons.Default.VerifiedUser,
                    content = "• Users must register with a genuine mobile number or email address verified through one-time password (OTP).\n\n• Customers must provide an accurate Odisha delivery address (District, Block, GP, Village, Landmark) during initial registration to ensure reliable delivery."
                )
            }

            item {
                InfoSectionCard(
                    title = "2. Ordering & Shop Fulfillment",
                    icon = Icons.Default.Store,
                    content = "• Orders placed through the app are forwarded directly to the selected local store merchant in Sinapali.\n\n• Store owners maintain stock accuracy and prepare items fresh.\n\n• Prices and discounts are set transparently by local shop owners."
                )
            }

            item {
                InfoSectionCard(
                    title = "3. Payment & Cash on Delivery (COD)",
                    icon = Icons.Default.Payments,
                    content = "• Cash on Delivery (COD) is the standard and direct payment method.\n\n• Customers must pay the exact bill amount shown at checkout to the authorized delivery personnel upon physical delivery of items.\n\n• No hidden convenience fees or unauthorized charges are imposed."
                )
            }

            item {
                InfoSectionCard(
                    title = "4. Delivery Timeline & Service Area",
                    icon = Icons.Default.TwoWheeler,
                    content = "• Deliveries operate within Sinapali town and mapped Gram Panchayats in Nuapada district.\n\n• Standard delivery time ranges between 20 to 45 minutes depending on distance and shop preparation time.\n\n• A standard nominal delivery fee (₹20–25) is clearly itemized on the cart summary."
                )
            }

            item {
                InfoSectionCard(
                    title = "5. Governing Law & Jurisdiction",
                    icon = Icons.Default.Balance,
                    content = "These Terms & Conditions are governed by the laws of India and subject to the jurisdiction of the competent courts of Nuapada District, Odisha."
                )
            }
        }
    }
}

@Composable
fun ReturnsRefundsScreen(viewModel: MainViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.returnsRefundsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerProfile) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalTealPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
                InfoBannerCard(
                    title = "Customer Satisfaction & Returns",
                    subtitle = "Fair, transparent return & exchange policy for Sinapali shoppers",
                    icon = Icons.Default.AssignmentReturn
                )
            }

            item {
                InfoSectionCard(
                    title = "1. Fresh & Perishable Items",
                    icon = Icons.Default.Eco,
                    content = "• Fresh Vegetables, Milk, Paneer, and Traditional Sweets must be inspected upon delivery.\n\n• If any item is found damaged, spoiled, or unsatisfactory at the time of delivery, you may refuse that item immediately.\n\n• The delivery agent will adjust the bill on the spot or arrange an immediate replacement from the store."
                )
            }

            item {
                InfoSectionCard(
                    title = "2. Packaged Groceries & Essentials",
                    icon = Icons.Default.Inventory,
                    content = "• Packaged goods (dal, rice, spices, oil, snacks, toiletries) have a 24-hour return/replacement window.\n\n• Eligible reasons: manufacturing defect, damaged packaging, expired date, or wrong item delivered.\n\n• Items must be retained in their original packaging."
                )
            }

            item {
                InfoSectionCard(
                    title = "3. Refund Process for Cash on Delivery",
                    icon = Icons.Default.CurrencyRupee,
                    content = "• For Cash on Delivery orders, refunds are issued in cash directly upon physical pickup of the returned item by our delivery personnel.\n\n• Alternatively, customers may opt for store credit/replacement with identical or equal-value goods from the same shop."
                )
            }

            item {
                InfoSectionCard(
                    title = "4. How to Request a Return / Refund",
                    icon = Icons.Default.ContactSupport,
                    content = "1. Open My Profile -> Help & File Complaint.\n2. Select your Order and describe the issue.\n3. Or directly contact Harishchandra Sagar (Admin) via Mobile at 7894952827 or email at harishchandrasagar664@gmail.com.\n4. Physical Office Location: Kotamal, Sinapali Main Road.\n5. Our team will verify and resolve your request within 2–4 hours."
                )
            }
        }
    }
}

@Composable
private fun InfoBannerCard(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = LocalTealPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LocalTealDark)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = LocalTextSecondary)
            }
        }
    }
}

@Composable
private fun InfoSectionCard(
    title: String,
    icon: ImageVector,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = LocalTealPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LocalTextPrimary)
            }
            Text(
                text = content,
                fontSize = 13.sp,
                color = LocalTextSecondary,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = LocalTealPrimary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = LocalTextMuted, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 13.sp, color = LocalTextPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}
