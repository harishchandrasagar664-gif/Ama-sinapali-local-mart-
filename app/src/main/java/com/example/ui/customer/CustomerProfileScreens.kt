@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.domain.model.UserRole
import com.example.ui.components.ProfilePhotoUploadDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.UserAvatar
import com.example.ui.locale.AppStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun CustomerOrdersScreen(viewModel: MainViewModel) {
    val orders by viewModel.allOrders.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    val customerOrders = remember(orders, user) {
        if (user != null) orders.filter { it.userId == user?.id } else orders
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.myOrdersTitle) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { padding ->
        if (customerOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = LocalTealPrimary
                        )
                        Text(s.myOrdersTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "No orders placed yet. Orders you place will appear here.",
                            fontSize = 13.sp,
                            color = LocalTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
                            Text("Start Shopping")
                        }
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
                items(customerOrders) { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.OrderTracking(order.id)) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${s.orderNumber}${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                StatusBadge(order.status, language = appLanguage)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${s.toPay}: ₹${order.finalAmount.toInt()} • ${order.paymentMethod.name.replace('_', ' ')}", fontSize = 13.sp)
                            Text("${s.deliveredBy}: ${order.deliveryAgentAssigned ?: "Harishchandra Sagar (Admin)"}", fontSize = 12.sp, color = LocalTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerProfileScreen(
    viewModel: MainViewModel,
    onRoleSwitchClick: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }
    var showEditAddressDialog by remember { mutableStateOf(false) }
    var showDpDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val isSessionActive = viewModel.authManager.isCustomerSessionActive() || (user?.isVerified == true && user?.id != null)

    var editDistrict by remember(user) { mutableStateOf(user?.district ?: "Nuapada") }
    var editBlock by remember(user) { mutableStateOf(user?.block ?: "Sinapali") }
    var editGp by remember(user) { mutableStateOf(user?.gp ?: "Sinapali") }
    var editVillage by remember(user) { mutableStateOf(user?.village ?: "Sinapali") }
    var editAddress by remember(user) { mutableStateOf(user?.deliveryAddress ?: "Near Panchayat Samiti Office, Word No 4, Sinapali") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.profileTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User info card
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(
                                photoUri = user?.profilePhotoUri,
                                name = user?.name ?: "Customer",
                                size = 64.dp,
                                showEditBadge = true,
                                onEditClick = { showDpDialog = true }
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user?.name ?: "Customer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = LocalSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isSessionActive) (if (appLanguage == AppLanguage.ODIA) "ସକ୍ରିୟ ଗ୍ରାହକ ସେସନ୍ (OTP ଯାଞ୍ଚ)" else "Active Customer (OTP Verified)") else (if (appLanguage == AppLanguage.ODIA) "ପଞ୍ଜୀକୃତ ଉପଭୋକ୍ତା" else "Registered User"),
                                        fontSize = 12.sp,
                                        color = LocalSuccess,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = { showDpDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = LocalTealPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (user?.profilePhotoUri != null) (if (appLanguage == AppLanguage.ODIA) "DP ବଦଳାନ୍ତୁ" else "Change DP") else (if (appLanguage == AppLanguage.ODIA) "DP ଯୋଡ଼ନ୍ତୁ" else "Add Profile Photo"),
                                        fontSize = 12.sp,
                                        color = LocalTealPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${s.mobileLabel}: ${user?.phone ?: "Not registered"}", fontSize = 13.sp, color = LocalTextSecondary)
                        if (!user?.email.isNullOrBlank()) {
                            Text("Email: ${user?.email}", fontSize = 13.sp, color = LocalTextSecondary)
                        }
                        Text("Account Role: ${user?.role?.name?.replace('_', ' ') ?: "CUSTOMER"}", fontSize = 12.sp, color = LocalTextMuted)
                    }
                }
            }

            // Language Selector Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LocalTealPrimary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = LocalTealPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(s.languageSectionTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(s.chooseLanguageDesc, fontSize = 12.sp, color = LocalTextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // English option
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setLanguage(AppLanguage.ENGLISH) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (appLanguage == AppLanguage.ENGLISH) LocalTealPrimary else MaterialTheme.colorScheme.surface,
                                shadowElevation = if (appLanguage == AppLanguage.ENGLISH) 2.dp else 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    RadioButton(
                                        selected = appLanguage == AppLanguage.ENGLISH,
                                        onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color.White,
                                            unselectedColor = LocalTextSecondary
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        s.englishName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (appLanguage == AppLanguage.ENGLISH) Color.White else LocalTextPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Odia option
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setLanguage(AppLanguage.ODIA) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (appLanguage == AppLanguage.ODIA) LocalTealPrimary else MaterialTheme.colorScheme.surface,
                                shadowElevation = if (appLanguage == AppLanguage.ODIA) 2.dp else 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    RadioButton(
                                        selected = appLanguage == AppLanguage.ODIA,
                                        onClick = { viewModel.setLanguage(AppLanguage.ODIA) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color.White,
                                            unselectedColor = LocalTextSecondary
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        s.odiaName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (appLanguage == AppLanguage.ODIA) Color.White else LocalTextPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Saved Odisha Delivery Address Card (Saved for future orders)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = LocalTealPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(s.savedDeliveryAddress, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            TextButton(onClick = { showEditAddressDialog = true }) {
                                Text(s.editBtn, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("${s.villageLabel}: ${user?.village ?: "Sinapali"}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("${s.gpLabel}: ${user?.gp ?: "Sinapali"}", fontSize = 13.sp)
                        Text("${s.blockLabel}: ${user?.block ?: "Sinapali"} • ${s.districtLabel}: ${user?.district ?: "Nuapada"}", fontSize = 13.sp)
                        Text("${s.fullAddressLabel}: ${user?.deliveryAddress ?: "Near Panchayat Samiti Office, Word No 4, Sinapali"}", fontSize = 13.sp, color = LocalTextSecondary)
                        Surface(
                            color = LocalTealPrimary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                s.savedAddressBadge,
                                fontSize = 11.sp,
                                color = LocalTealPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Account Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isSessionActive) {
                        // Explicit Logout Button when logged in
                        Button(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn_logout_customer"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.logoutBtn, fontWeight = FontWeight.Bold)
                        }

                        // Option to switch account
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screen.CustomerAuth) },
                            modifier = Modifier.fillMaxWidth().testTag("btn_switch_account"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (appLanguage == AppLanguage.ODIA) "ଅନ୍ୟ ନମ୍ବର ସହିତ ଲଗଇନ୍ କରନ୍ତୁ" else "Switch / Re-login with different mobile", fontSize = 13.sp)
                        }
                    } else {
                        // Customer Login / Register with OTP
                        Button(
                            onClick = { viewModel.navigateTo(Screen.CustomerAuth) },
                            modifier = Modifier.fillMaxWidth().testTag("btn_login_customer"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s.loginRegisterWithOtp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onRoleSwitchClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAmberSecondary, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (currentRole == UserRole.CUSTOMER) "Staff & Merchant Access (Restricted)" else s.switchPortalMenu,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(Screen.CustomerHelp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(s.helpComplaintMenu)
                    }
                }
            }

            // Settings, Support & Legal Policies
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            s.settingsAndInfo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = LocalTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        ProfileNavRow(
                            icon = Icons.Default.Info,
                            title = s.aboutUs,
                            subtitle = "Ama Sinapali Local Mart mission & story",
                            onClick = { viewModel.navigateTo(Screen.AboutUs) }
                        )
                        HorizontalDivider()

                        ProfileNavRow(
                            icon = Icons.Default.ContactSupport,
                            title = s.contactUs,
                            subtitle = "Admin support, email, office & hours",
                            onClick = { viewModel.navigateTo(Screen.ContactUs) }
                        )
                        HorizontalDivider()

                        ProfileNavRow(
                            icon = Icons.Default.AssignmentReturn,
                            title = s.returnsRefunds,
                            subtitle = "COD refunds, freshness guarantee & exchange",
                            onClick = { viewModel.navigateTo(Screen.ReturnsRefunds) }
                        )
                        HorizontalDivider()

                        ProfileNavRow(
                            icon = Icons.Default.Security,
                            title = s.privacyPolicy,
                            subtitle = "How we protect your data & address",
                            onClick = { viewModel.navigateTo(Screen.PrivacyPolicy) }
                        )
                        HorizontalDivider()

                        ProfileNavRow(
                            icon = Icons.Default.Description,
                            title = s.termsConditions,
                            subtitle = "Store rules, user agreement & COD guidelines",
                            onClick = { viewModel.navigateTo(Screen.TermsConditions) }
                        )
                    }
                }
            }
        }

        // Edit Saved Address Dialog
        if (showEditAddressDialog) {
            AlertDialog(
                onDismissRequest = { showEditAddressDialog = false },
                title = { Text(s.updateAddressDialogTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editDistrict,
                            onValueChange = { editDistrict = it },
                            label = { Text(s.districtLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editBlock,
                            onValueChange = { editBlock = it },
                            label = { Text(s.blockLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editGp,
                            onValueChange = { editGp = it },
                            label = { Text(s.gpLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editVillage,
                            onValueChange = { editVillage = it },
                            label = { Text(s.villageLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editAddress,
                            onValueChange = { editAddress = it },
                            label = { Text(s.fullAddressLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateCustomerAddress(
                                district = editDistrict.trim(),
                                block = editBlock.trim(),
                                gp = editGp.trim(),
                                village = editVillage.trim(),
                                fullAddress = editAddress.trim()
                            )
                            showEditAddressDialog = false
                        }
                    ) {
                        Text(s.saveAddressBtn)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditAddressDialog = false }) {
                        Text(s.cancelBtn)
                    }
                }
            )
        }

        if (showDpDialog) {
            val shops by viewModel.allShops.collectAsState()
            val myShop = shops.find { it.ownerId == user?.id }
            ProfilePhotoUploadDialog(
                language = appLanguage,
                currentPhotoUri = user?.profilePhotoUri,
                associatedShop = myShop,
                onDismissRequest = { showDpDialog = false },
                onPhotoSelected = { newUri ->
                    viewModel.updateProfilePhoto(newUri)
                }
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(s.logoutConfirmTitle, fontWeight = FontWeight.Bold) },
                text = { Text(s.logoutConfirmMessage) },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logoutCustomer()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(s.logoutBtn)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(s.cancelBtn)
                    }
                }
            )
        }
    }
}

@Composable
fun CustomerHelpScreen(viewModel: MainViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.helpComplaintMenu) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(s.helpComplaintMenu, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Admin Helpline & Direct Support", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LocalTealDark)
                    Text("• Administrator: Harishchandra Sagar", fontSize = 12.sp, color = LocalTextPrimary)
                    Text("• Mobile: 7894952827 (Call & WhatsApp)", fontSize = 12.sp, color = LocalTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("• Physical Office: Kotamal, Sinapali Main Road", fontSize = 12.sp, color = LocalTextPrimary)
                    Text("• Email: harishchandrasagar664@gmail.com", fontSize = 12.sp, color = LocalTextPrimary)
                }
            }

            Text("Or submit your query below to record an official ticket:", fontSize = 12.sp, color = LocalTextSecondary)

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject (e.g. Delivery Delay, Quality)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Button(
                onClick = {
                    if (subject.isNotBlank() && description.isNotBlank()) {
                        viewModel.fileComplaint(1, "Ama Sinapali Mart", subject, description)
                        viewModel.navigateTo(Screen.CustomerProfile)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
            ) {
                Text(s.submit, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = LocalTealPrimary.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = LocalTealPrimary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = LocalTextSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LocalTextMuted, modifier = Modifier.size(20.dp))
    }
}
