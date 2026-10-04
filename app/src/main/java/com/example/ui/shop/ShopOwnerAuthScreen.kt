@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.shop

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.SendToMobile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SeedData
import com.example.ui.components.ProductImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.ImageStorageHelper

@Composable
fun ShopOwnerAuthScreen(
    viewModel: MainViewModel,
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val isOtpSent by viewModel.isShopOwnerOtpSent.collectAsState()
    val otpCode by viewModel.shopOwnerOtpCode.collectAsState()
    val authError by viewModel.shopOwnerAuthError.collectAsState()
    val isNewRegistration by viewModel.isNewShopOwnerRegistration.collectAsState()
    val isRegistering by viewModel.isRegisteringShop.collectAsState()

    var mobileInput by remember { mutableStateOf("9437012345") }
    var enteredOtp by remember { mutableStateOf("") }
    var otpVerifiedForNewReg by remember { mutableStateOf(false) }

    // Shop Registration Form Fields
    var regOwnerName by remember { mutableStateOf("") }
    var regShopName by remember { mutableStateOf("") }
    var regCategory by remember { mutableStateOf("Daily Essentials") }
    var regAddress by remember { mutableStateOf("Sinapali Main Market, Nuapada") }
    var regUpiId by remember { mutableStateOf("") }
    var regImageUrl by remember { mutableStateOf("hero_local_mart") }
    var regDeliveryAvailable by remember { mutableStateOf(true) }
    var regDescription by remember { mutableStateOf("") }

    // Modern Zero-Permission Photo Picker for Shop Photo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.copyProductUri(context, uri)
                ?: ImageStorageHelper.copyUri(context, uri, prefix = "shop_photo", subDir = "shops")
            if (savedPath != null) {
                regImageUrl = savedPath
                Toast.makeText(context, "Shop photo selected successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera launcher for physical shop capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedPath = ImageStorageHelper.saveProductBitmap(context, bitmap)
            regImageUrl = savedPath
            Toast.makeText(context, "Shop photo captured!", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission needed to photograph shop", Toast.LENGTH_SHORT).show()
        }
    }

    val shopCategories = remember {
        listOf(
            "Daily Essentials",
            "Grocery & Kirana",
            "Sweets & Bakery",
            "Electronics & Mobile",
            "Clothing & Fashion",
            "Fresh Vegetables & Fruits",
            "Pharmacy & Health",
            "Hardware & Electricals"
        )
    }

    val photoPresets = remember {
        listOf(
            Pair("General Mart", "hero_local_mart"),
            Pair("Grocery", "grocery_store"),
            Pair("Sweets", "sweets_bakery"),
            Pair("Clothing", "clothing_store"),
            Pair("Electronics", "electronics_store"),
            Pair("Produce", "fresh_produce")
        )
    }

    BackHandler {
        if (otpVerifiedForNewReg) {
            otpVerifiedForNewReg = false
        } else if (isOtpSent) {
            viewModel.resetShopOwnerOtpState()
            enteredOtp = ""
        } else {
            viewModel.resetShopOwnerOtpState()
            viewModel.navigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (otpVerifiedForNewReg) "Shop Registration" else "Shop Owner Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            if (otpVerifiedForNewReg) "ନୂଆ ଦୋକାନ ପଞ୍ଜୀକରଣ" else "ଦୋକାନୀ ଲଗଇନ୍ • OTP ଯାଞ୍ଚ",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (otpVerifiedForNewReg) {
                                otpVerifiedForNewReg = false
                            } else {
                                viewModel.resetShopOwnerOtpState()
                                viewModel.navigateBack()
                            }
                        },
                        modifier = Modifier.testTag("shop_owner_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE65100),
                    titleContentColor = Color.White
                )
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
            // Header card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE65100),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Sinapali Merchant Portal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFFBF360C)
                                )
                                Text(
                                    "Exclusive portal for verified local shop owners",
                                    fontSize = 12.sp,
                                    color = LocalTextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "✓ Isolated Merchant Access",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    "• Only Your Own Shop & Orders",
                                    fontSize = 11.sp,
                                    color = LocalTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // CASE 1: NEW SHOP REGISTRATION FORM (Visible when new mobile OTP is verified)
            if (otpVerifiedForNewReg) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Register Your Shop (ନୂଆ ଦୋକାନ)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFBF360C)
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("OTP Verified", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                }
                            }

                            // Verified Mobile (Read-Only)
                            OutlinedTextField(
                                value = mobileInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Verified Mobile Number") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2E7D32))
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2E7D32))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Owner Name
                            OutlinedTextField(
                                value = regOwnerName,
                                onValueChange = { regOwnerName = it },
                                label = { Text("Owner Full Name * (ଦୋକାନୀଙ୍କ ନାମ)") },
                                placeholder = { Text("e.g. Ramesh Chandra Meher") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFE65100))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_owner_name_input"),
                                singleLine = true
                            )

                            // Shop Name
                            OutlinedTextField(
                                value = regShopName,
                                onValueChange = { regShopName = it },
                                label = { Text("Shop Name * (ଦୋକାନ ନାମ)") },
                                placeholder = { Text("e.g. Maa Tarini General Store") },
                                leadingIcon = {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFFE65100))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_shop_name_input"),
                                singleLine = true
                            )

                            // Category Selector
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Shop Category * (ଦୋକାନ ବର୍ଗ):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    items(shopCategories) { cat ->
                                        val isSel = regCategory == cat
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { regCategory = cat },
                                            label = { Text(cat, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFE65100),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            // Shop Address
                            OutlinedTextField(
                                value = regAddress,
                                onValueChange = { regAddress = it },
                                label = { Text("Shop Address * (ଦୋକାନ ଠିକଣା)") },
                                placeholder = { Text("e.g. Near Bus Stand, Main Road, Sinapali") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE65100))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_shop_address_input"),
                                singleLine = false,
                                maxLines = 2
                            )

                            // UPI ID for customer payments
                            OutlinedTextField(
                                value = regUpiId,
                                onValueChange = { regUpiId = it },
                                label = { Text("Shop UPI ID for Payments") },
                                placeholder = { Text("${mobileInput}@upi") },
                                leadingIcon = {
                                    Icon(Icons.Default.Payment, contentDescription = null, tint = Color(0xFFE65100))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Shop Photo Section (Camera, Gallery, Presets, and Live Preview)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Shop Photo * (ଦୋକାନ ଫଟୋ):",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = LocalTextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Text(
                                            "Cloud Stored",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, Color(0xFFFFCC80)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ProductImage(
                                            imageUrl = regImageUrl,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            fallbackIcon = Icons.Default.Storefront
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                "Upload Shop Photo",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFFBF360C)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFE65100))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Camera", fontSize = 11.sp, color = Color(0xFFE65100))
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        galleryLauncher.launch(
                                                            PickVisualMediaRequest(
                                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                                            )
                                                        )
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFE65100))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Gallery", fontSize = 11.sp, color = Color(0xFFE65100))
                                                }
                                            }
                                        }
                                    }
                                }

                                Text("Or select a thematic preset photo:", fontSize = 11.sp, color = LocalTextSecondary)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    items(photoPresets) { (label, key) ->
                                        val isSel = regImageUrl == key
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSel) Color(0xFFE65100) else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { regImageUrl = key }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.PhotoCamera,
                                                    contentDescription = null,
                                                    tint = if (isSel) Color.White else Color(0xFFE65100),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) Color.White else LocalTextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Delivery Available Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Home Delivery Available", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Allow town delivery orders for your shop", fontSize = 11.sp, color = LocalTextSecondary)
                                }
                                Switch(
                                    checked = regDeliveryAvailable,
                                    onCheckedChange = { regDeliveryAvailable = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE65100), checkedTrackColor = Color(0xFFFFCC80))
                                )
                            }

                            // Submit Button
                            Button(
                                onClick = {
                                    viewModel.registerNewShopOwnerAndShop(
                                        ownerName = regOwnerName,
                                        shopName = regShopName,
                                        category = regCategory,
                                        address = regAddress,
                                        phone = mobileInput,
                                        imageUrl = regImageUrl,
                                        upiId = regUpiId.ifBlank { "${mobileInput}@upi" },
                                        description = regDescription,
                                        deliveryAvailable = regDeliveryAvailable
                                    ) { success, _ ->
                                        if (success) {
                                            onSuccess()
                                        }
                                    }
                                },
                                enabled = !isRegistering && regOwnerName.isNotBlank() && regShopName.isNotBlank() && regAddress.isNotBlank() && regImageUrl.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("reg_submit_shop_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                            ) {
                                if (isRegistering) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Saving to Firebase & Creating Account...", fontSize = 14.sp)
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Complete Registration & Open Dashboard", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // CASE 2: STEP 1 & STEP 2 (MOBILE + OTP VERIFICATION)
                // Quick merchant chips for effortless testing
                if (!isOtpSent) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Quick Access (Select Registered Merchant):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LocalTextSecondary
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(SeedData.shopOwners) { owner ->
                                    val isSelected = mobileInput == owner.phone
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) Color(0xFFE65100) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable {
                                            mobileInput = owner.phone
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Store,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSelected) Color.White else Color(0xFFE65100)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "${owner.name} (${owner.phone})",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else LocalTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 1: Mobile Number Input
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                if (isOtpSent) "Mobile Number for Verification" else "Step 1: Enter 10-digit Mobile Number",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFBF360C)
                            )

                            OutlinedTextField(
                                value = mobileInput,
                                onValueChange = {
                                    if (!isOtpSent) {
                                        mobileInput = it.take(15)
                                    }
                                },
                                readOnly = isOtpSent,
                                label = { Text("10-digit Mobile Number") },
                                placeholder = { Text("e.g. 9437012345 or any new number") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFE65100))
                                },
                                trailingIcon = {
                                    if (isOtpSent) {
                                        TextButton(
                                            onClick = {
                                                viewModel.resetShopOwnerOtpState()
                                                enteredOtp = ""
                                                otpVerifiedForNewReg = false
                                            },
                                            modifier = Modifier.testTag("change_shop_mobile_btn")
                                        ) {
                                            Text("Change", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shop_owner_mobile_input"),
                                singleLine = true
                            )

                            if (!isOtpSent) {
                                Button(
                                    onClick = {
                                        viewModel.sendShopOwnerOtp(mobileInput)
                                    },
                                    enabled = mobileInput.trim().length >= 10,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("shop_owner_send_otp_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.SendToMobile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send Verification OTP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }

                                Text(
                                    "✨ New merchant? Enter any 10-digit mobile number to verify and register your new shop in seconds!",
                                    fontSize = 11.sp,
                                    color = LocalTextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Step 2: OTP Verification Card (Visible when OTP is sent)
                if (isOtpSent) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    "Step 2: Enter 6-digit Verification OTP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFFBF360C)
                                )

                                // Demo OTP banner
                                otpCode?.let { code ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    "OTP Code: $code",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF1B5E20)
                                                )
                                                Text(
                                                    "Tap below to auto-fill or enter $code (or test code 123456)",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = enteredOtp,
                                    onValueChange = { enteredOtp = it.take(6) },
                                    label = { Text("6-digit Verification OTP") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFE65100))
                                    },
                                    trailingIcon = {
                                        if (enteredOtp.isEmpty() && otpCode != null) {
                                            TextButton(onClick = { enteredOtp = otpCode ?: "123456" }) {
                                                Text("Fill OTP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("shop_owner_otp_input"),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        viewModel.verifyShopOwnerOtp(enteredOtp) { success, needsReg, _ ->
                                            if (success) {
                                                if (needsReg) {
                                                    otpVerifiedForNewReg = true
                                                    regUpiId = "${mobileInput.trim()}@upi"
                                                } else {
                                                    onSuccess()
                                                }
                                            }
                                        }
                                    },
                                    enabled = enteredOtp.length >= 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("shop_owner_verify_otp_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isNewRegistration) "Verify OTP & Continue to Shop Details" else "Verify & Access Shop Dashboard",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.sendShopOwnerOtp(mobileInput)
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Resend OTP", fontSize = 12.sp, color = Color(0xFFE65100))
                                    }

                                    TextButton(
                                        onClick = {
                                            viewModel.resetShopOwnerOtpState()
                                            enteredOtp = ""
                                            otpVerifiedForNewReg = false
                                        }
                                    ) {
                                        Text("Change Number", fontSize = 12.sp, color = LocalTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Error Message Card
            authError?.let { err ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Security Policy & Access Segregation Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Merchant Security & Privacy Rules", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            "• Only verified merchants registered in Sinapali are permitted access.\n" +
                            "• Once verified, you will exclusively manage your own shop's products, customer orders, and notifications.\n" +
                            "• All shop and owner details are securely backed up to Firebase Firestore cloud.\n" +
                            "• Customer shopping carts, customer profiles, and Town Admin panel functions are completely restricted.",
                            fontSize = 11.sp,
                            color = LocalTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Switch to Customer Store button
            item {
                OutlinedButton(
                    onClick = {
                        viewModel.resetShopOwnerOtpState()
                        viewModel.switchToCustomerView()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("return_to_customer_store_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Return to Customer Store View")
                }
            }
        }
    }
}
