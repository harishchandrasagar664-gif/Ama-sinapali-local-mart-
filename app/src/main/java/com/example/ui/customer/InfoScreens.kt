@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.locale.AppStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun PrivacyPolicyScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.privacyPolicyTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
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
                    subtitle = "Google Play Compliant Privacy Policy • Updated: October 2026 • Sinapali, Nuapada, Odisha",
                    icon = Icons.Default.Security
                )
            }

            item {
                InfoSectionCard(
                    title = "1. Customer Personal Data We Collect & Store",
                    icon = Icons.Default.Person,
                    content = "When you register or order on Ama Sinapali Local Mart, we collect:\n\n• Mobile Number or Email (verified via OTP) for account creation, order verification, and dispatch updates.\n• Customer Full Name for billing and package handover.\n• Delivery Address: Odisha District (Nuapada), Block (Sinapali), Gram Panchayat (GP), Village/Town, and Ward/Street details.\n• Optional Customer Profile Picture (DP) uploaded from camera or photo library."
                )
            }

            item {
                InfoSectionCard(
                    title = "2. Shop Owner / Merchant Data We Collect",
                    icon = Icons.Default.Storefront,
                    content = "For local shop merchants operating on Ama Sinapali:\n\n• Shop Owner Full Name, Mobile Number, and Email.\n• Shop Name, Physical Shop Address, Category, and Working Hours.\n• Product Catalog Data: Item names, Odia titles, prices, descriptions, unit measurements, and stock availability.\n• Shop Photos & Product Images: Uploaded storefront banners and product photos."
                )
            }

            item {
                InfoSectionCard(
                    title = "3. Order Data & Transaction History",
                    icon = Icons.Default.ReceiptLong,
                    content = "To fulfill hyper-local purchases:\n\n• Cart Items & Order History: Quantities, ordered prices, selected shop partner, and timestamps.\n• Payment Method: Cash on Delivery (COD) collection status or digital payment references.\n• Delivery Status: Tracking milestones (Placed, Preparing, Out for Delivery, Delivered) and delivery agent assignment."
                )
            }

            item {
                InfoSectionCard(
                    title = "4. Device Storage & Firebase Backend Cloud Storage",
                    icon = Icons.Default.CloudSync,
                    content = "Where and how your data is processed:\n\n• Local Device Storage: Encrypted SQLite Room database stores active sessions, local cart, and offline viewing cache.\n• Firebase Cloud Backend (Firestore): Multi-tier cloud synchronization stores customer profiles, shop catalogs, active orders, and notifications to allow real-time order tracking and merchant fulfillment.\n• Cloud-hosted data is transmitted over TLS/HTTPS encrypted connections."
                )
            }

            item {
                InfoSectionCard(
                    title = "5. Admin Access & Safeguards",
                    icon = Icons.Default.AdminPanelSettings,
                    content = "Strict operational access limits:\n\n• Platform Administrator (Harishchandra Sagar) oversees delivery coordination, merchant approvals, and grievance redressal.\n• Assigned Shop Owners only access order details (customer name, items, and delivery address) strictly required to prepare and pack that specific order.\n• We DO NOT sell, rent, monetize, or broker personal user information to third-party ad networks or brokers."
                )
            }

            item {
                InfoSectionCard(
                    title = "6. Profile Photos, Camera & Media",
                    icon = Icons.Default.CameraAlt,
                    content = "How images are handled:\n\n• Camera access is used solely when you choose to take a photo for your profile avatar or shop storefront.\n• Media selection uses the zero-permission Android system photo picker.\n• Images are stored locally and synced to your merchant/customer profile for visual recognition."
                )
            }

            item {
                InfoSectionCard(
                    title = "7. Data Retention & Preservation Periods",
                    icon = Icons.Default.Timelapse,
                    content = "Data is retained only as long as necessary:\n\n• Account & Profile Information: Retained while your account remains active.\n• Carts & Drafts: Stored until checkout or cleared by you.\n• Completed Order Receipts: Anonymized order records and tax tallies may be retained as required by Indian commercial, GST, and accounting regulations.\n• When you request account deletion, all active personal identifying data is erased immediately."
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "8. Account Deletion & Data Wipe Rights",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Text(
                            "In full compliance with Google Play Store User Data Deletion Policy and global privacy standards, you have the right to request permanent deletion of your account and all associated personal data anytime.",
                            fontSize = 13.sp,
                            color = LocalTextPrimary
                        )

                        Text(
                            "• In-App Deletion for Customers: Go to 'My Profile' > tap 'Delete Account & Wipe Data' to permanently erase your profile, delivery addresses, cart, profile picture, and Firebase cloud records.\n• In-App Deletion for Shop Owners: Go to 'Shop Overview' > tap 'Delete Shop Account & Wipe Cloud Data' to erase shop details, products, and merchant cloud entries.\n• External Web Deletion Method: If you do not have the app installed, submit an online deletion request using our dedicated web page below.",
                            fontSize = 13.sp,
                            color = LocalTextSecondary
                        )

                        Button(
                            onClick = {
                                viewModel.navigateTo(Screen.AccountDeletion)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Account Deletion Screen", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                InfoSectionCard(
                    title = "9. Grievance Officer & Contact",
                    icon = Icons.Default.ContactSupport,
                    content = "For questions, rights requests, or deletion queries, contact our designated Grievance Officer:\n\nHarishchandra Sagar (Administrator & Grievance Officer)\nAma Sinapali Local Mart\nPhysical Address: Kotamal, Sinapali Main Road, Dist: Nuapada, Odisha - 766108, India\nMobile Number: +91 7894952827\nEmail: harishchandrasagar664@gmail.com\nLocation: Sinapali, Nuapada, Odisha"
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
                    IconButton(onClick = { viewModel.navigateBack() }) {
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
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_sinapali_mart_logo),
                                contentDescription = "Ama Sinapali Local Mart Official Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
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
                    IconButton(onClick = { viewModel.navigateBack() }) {
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
                    IconButton(onClick = { viewModel.navigateBack() }) {
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

@Composable
fun AccountDeletionScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isCustomerActive = viewModel.authManager.isCustomerSessionActive() || (currentUser?.isVerified == true && currentUser?.id != null)
    val isShopOwnerActive = viewModel.authManager.isShopOwnerSessionActive() || (currentRole == com.example.domain.model.UserRole.SHOP_OWNER)

    var showConfirmDialog by remember { mutableStateOf(false) }
    var deletionReason by remember { mutableStateOf("") }
    var confirmedRisk by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Deletion", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.error,
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Permanent Data Wipe Request",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            "Under Google Play Store policies and privacy laws, you can permanently erase your account, contact details, delivery addresses, shopping carts, and Firebase cloud records anytime.",
                            fontSize = 13.sp,
                            color = LocalTextPrimary
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Active Account Status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (currentUser != null) {
                            Text("• Registered Name: ${currentUser?.name}", fontSize = 13.sp)
                            Text("• Mobile Number: ${currentUser?.phone}", fontSize = 13.sp)
                            Text("• Account Role: ${if (isShopOwnerActive) "Shop Owner / Merchant" else "Customer"}", fontSize = 13.sp)
                            Text("• Account ID: #${currentUser?.id}", fontSize = 13.sp, color = LocalTextSecondary)
                        } else {
                            Text(
                                "No local user session currently active. If you uninstalled the app or lost your device, use the verified web deletion form below.",
                                fontSize = 13.sp,
                                color = LocalTextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("What will be deleted immediately:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("1. Account Profile: Mobile number, name, and profile picture (DP).", fontSize = 12.sp, color = LocalTextSecondary)
                        Text("2. Location & Addresses: Saved Sinapali delivery address, GP, and village.", fontSize = 12.sp, color = LocalTextSecondary)
                        Text("3. Shopping Records: Active cart items and notification preferences.", fontSize = 12.sp, color = LocalTextSecondary)
                        Text("4. Cloud Records: Corresponding documents in Firebase Firestore collections (users, shops, products).", fontSize = 12.sp, color = LocalTextSecondary)
                        Text("5. Statutory Exception: Historical completed transactions are anonymized for tax & GST accounting as required by Indian law.", fontSize = 12.sp, color = LocalTextSecondary)
                    }
                }
            }

            if (currentUser != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Confirm & Delete Account Now", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.error)

                            OutlinedTextField(
                                value = deletionReason,
                                onValueChange = { deletionReason = it },
                                label = { Text("Reason for Deletion (Optional)") },
                                placeholder = { Text("Tell us why you are leaving (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { confirmedRisk = !confirmedRisk }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = confirmedRisk,
                                    onCheckedChange = { confirmedRisk = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "I understand that this action is irreversible and my account and cloud records will be permanently erased.",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Button(
                                onClick = { showConfirmDialog = true },
                                enabled = confirmedRisk,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Permanently Delete My Account", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Privacy Policy & Support Information", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Review our complete Privacy Policy and data retention terms, or contact our Grievance Officer directly if you have queries regarding your account deletion:",
                            fontSize = 12.sp,
                            color = LocalTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.navigateTo(Screen.PrivacyPolicy) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Privacy Policy", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.navigateTo(Screen.ContactUs) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContactSupport, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Contact Admin", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                InfoSectionCard(
                    title = "Grievance Officer Direct Contact",
                    icon = Icons.Default.ContactSupport,
                    content = "Harishchandra Sagar (Administrator & Grievance Officer)\nAma Sinapali Local Mart\nLocation: Kotamal, Sinapali Main Road, Dist: Nuapada, Odisha - 766108\nMobile Helpline: +91 7894952827\nEmail: harishchandrasagar664@gmail.com"
                )
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Permanent Deletion?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    "Are you absolutely sure? All data associated with ${currentUser?.name ?: "this account"} (${currentUser?.phone ?: ""}) will be permanently wiped from our database and Firebase backend immediately.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        if (isShopOwnerActive) {
                            viewModel.deleteShopOwnerAccount()
                        } else {
                            viewModel.deleteCustomerAccount()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
