@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.ui.locale.AppStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch

@Composable
fun CustomerAuthScreen(
    viewModel: MainViewModel,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isOtpSent by viewModel.isOtpSent.collectAsState()
    val isOtpVerified by viewModel.isOtpVerified.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val authError by viewModel.authErrorMessage.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var useEmail by remember { mutableStateOf(false) }

    var mobileInput by remember { mutableStateOf("8895011223") }
    var emailInput by remember { mutableStateOf("priyanka@sinapali.com") }
    var otpInput by remember { mutableStateOf("") }

    // First-time Registration Address fields
    var fullName by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("Nuapada") }
    var block by remember { mutableStateOf("Sinapali") }
    var gp by remember { mutableStateOf("Sinapali") }
    var village by remember { mutableStateOf("Sinapali") }
    var fullAddress by remember { mutableStateOf("Near Panchayat Samiti Office, Word No 4, Sinapali") }
    var landmark by remember { mutableStateOf("Opposite High School Playground") }

    val odishaDistricts = listOf(
        "Nuapada", "Kalahandi", "Balangir", "Bargarh", "Sambalpur",
        "Cuttack", "Khordha", "Puri", "Ganjam", "Rayagada", "Koraput"
    )
    val nuapadaBlocks = listOf("Sinapali", "Boden", "Khariar", "Komna", "Nuapada")
    val sinapaliGPs = listOf(
        "Sinapali", "Hatibandh", "Timanpur", "Karlakot", "Ghilaguda",
        "Kapsi", "Litiguda", "Gorla", "Chikhili"
    )
    val localVillages = listOf("Sinapali Town", "Chikhili", "Brahmanpada", "Litiguda", "Kapsi")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.loginRegisterTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.resetCustomerOtpState()
                            viewModel.navigateTo(Screen.CustomerHome)
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            item {
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
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 3.dp
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_mart_icon),
                                contentDescription = "Ama Sinapali Local Mart",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Ama Sinapali Local Mart",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = LocalTealDark
                            )
                            Text(
                                s.loginRegisterWithOtp,
                                fontSize = 13.sp,
                                color = LocalTextSecondary
                            )
                        }
                    }
                }
            }

            // Mode Selector: Login vs Register
            item {
                TabRow(
                    selectedTabIndex = if (authMode == AuthMode.LOGIN) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = authMode == AuthMode.LOGIN,
                        onClick = {
                            authMode = AuthMode.LOGIN
                            viewModel.resetCustomerOtpState()
                            otpInput = ""
                        },
                        text = {
                            Text(
                                s.loginTab,
                                fontWeight = if (authMode == AuthMode.LOGIN) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_login")
                    )
                    Tab(
                        selected = authMode == AuthMode.REGISTER,
                        onClick = {
                            authMode = AuthMode.REGISTER
                            viewModel.resetCustomerOtpState()
                            otpInput = ""
                        },
                        text = {
                            Text(
                                s.registerTab,
                                fontWeight = if (authMode == AuthMode.REGISTER) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_register")
                    )
                }
            }

            // Channel Selector: Mobile vs Email
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Choose Verification Method:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = LocalTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = !useEmail,
                                onClick = {
                                    useEmail = false
                                    viewModel.resetCustomerOtpState()
                                    otpInput = ""
                                },
                                label = { Text(s.mobileOtpMethod) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_mobile_otp")
                            )
                            FilterChip(
                                selected = useEmail,
                                onClick = {
                                    useEmail = true
                                    viewModel.resetCustomerOtpState()
                                    otpInput = ""
                                },
                                label = { Text(s.emailOtpMethod) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_email_otp")
                            )
                        }
                    }
                }
            }

            // Input Target (Phone or Email)
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!useEmail) {
                            OutlinedTextField(
                                value = mobileInput,
                                onValueChange = { mobileInput = it.filter { ch -> ch.isDigit() }.take(10) },
                                label = { Text(s.mobileNumberField) },
                                leadingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                                    ) {
                                        Text("+91", fontWeight = FontWeight.Bold, color = LocalTealPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Divider(
                                            modifier = Modifier
                                                .height(20.dp)
                                                .width(1.dp)
                                        )
                                    }
                                },
                                placeholder = { Text("e.g. 9437012345") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_mobile_number"),
                                enabled = !isOtpVerified
                            )
                        } else {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it.trim() },
                                label = { Text(s.emailAddressField) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = LocalTealPrimary)
                                },
                                placeholder = { Text("e.g. customer@sinapali.com") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_email_address"),
                                enabled = !isOtpVerified
                            )
                        }

                        Button(
                            onClick = {
                                val target = if (!useEmail) mobileInput else emailInput
                                if (target.isNotBlank()) {
                                    val code = viewModel.sendCustomerOtp(target, useEmail)
                                    otpInput = code // Prefill for smooth testing/convenience
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_send_otp"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                            enabled = !isOtpVerified && ((!useEmail && mobileInput.length >= 10) || (useEmail && emailInput.contains("@")))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isOtpSent) s.resendOtpBtn else s.sendOtpBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // OTP Verification Box
            if (isOtpSent && !isOtpVerified) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                s.enterOtpTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Surface(
                                color = LocalAmberSecondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Verification Code Sent:",
                                            fontSize = 11.sp,
                                            color = LocalTextSecondary
                                        )
                                        Text(
                                            "${otpCode ?: "123456"}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LocalTealDark,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { otpInput = otpCode ?: "123456" },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Auto-fill", fontSize = 12.sp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = { if (it.length <= 6) otpInput = it },
                                label = { Text(s.enterOtpTitle) },
                                placeholder = { Text("123456") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_otp_code")
                            )

                            if (authError != null) {
                                Text(
                                    text = authError ?: "",
                                    color = LocalError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    val valid = viewModel.verifyCustomerOtp(otpInput)
                                    if (valid) {
                                        val target = if (!useEmail) mobileInput else emailInput
                                        coroutineScope.launch {
                                            val existing = viewModel.checkCustomerExists(target)
                                            if (authMode == AuthMode.LOGIN && existing != null) {
                                                // Existing customer logs in directly!
                                                viewModel.loginCustomerWithVerifiedOtp(existing) {
                                                    onSuccess()
                                                }
                                            } else {
                                                // Switch to First-time Registration to collect Odisha address!
                                                authMode = AuthMode.REGISTER
                                                if (existing != null) {
                                                    fullName = existing.name
                                                    district = existing.district
                                                    block = existing.block
                                                    gp = existing.gp
                                                    village = existing.village
                                                    fullAddress = existing.deliveryAddress
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_verify_otp"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LocalSuccess)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(s.verifyOtpBtn, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // First-Time Registration Form: Collects Odisha District, Block, GP, Village & Full Delivery Address
            if (isOtpVerified && authMode == AuthMode.REGISTER) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = LocalSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LocalSuccess)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "OTP Verified! Complete your First-Time Registration.",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = LocalSuccess
                                    )
                                }
                            }

                            Text(
                                s.savedDeliveryAddress,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                s.firstTimeRegNote,
                                fontSize = 12.sp,
                                color = LocalTextSecondary
                            )

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text(s.customerNameField) },
                                placeholder = { Text("e.g. Priyanka Majhi") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_customer_name")
                            )

                            // Odisha District Selector
                            Column {
                                Text(
                                    "${s.districtLabel} *",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = LocalTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(odishaDistricts) { d ->
                                        FilterChip(
                                            selected = district == d,
                                            onClick = { district = d },
                                            label = { Text(d) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = district,
                                    onValueChange = { district = it },
                                    label = { Text(s.districtLabel) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_district")
                                )
                            }

                            // Block Selector
                            Column {
                                Text(
                                    "${s.blockLabel} *",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = LocalTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(nuapadaBlocks) { b ->
                                        FilterChip(
                                            selected = block == b,
                                            onClick = { block = b },
                                            label = { Text(b) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = block,
                                    onValueChange = { block = it },
                                    label = { Text(s.blockLabel) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_block")
                                )
                            }

                            // Gram Panchayat (GP)
                            Column {
                                Text(
                                    "${s.gpLabel} *",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = LocalTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(sinapaliGPs) { g ->
                                        FilterChip(
                                            selected = gp == g,
                                            onClick = { gp = g },
                                            label = { Text(g) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = gp,
                                    onValueChange = { gp = it },
                                    label = { Text(s.gpLabel) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_gp")
                                )
                            }

                            // Village / Town
                            Column {
                                Text(
                                    "${s.villageLabel} *",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = LocalTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(localVillages) { v ->
                                        FilterChip(
                                            selected = village == v,
                                            onClick = { village = v },
                                            label = { Text(v) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = village,
                                    onValueChange = { village = it },
                                    label = { Text(s.villageLabel) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_village")
                                )
                            }

                            // Full Delivery Address
                            OutlinedTextField(
                                value = fullAddress,
                                onValueChange = { fullAddress = it },
                                label = { Text("${s.fullAddressLabel} *") },
                                placeholder = { Text("e.g. Ward No 4, Near Panchayat Samiti Office, Sinapali") },
                                minLines = 2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_full_address")
                            )

                            OutlinedTextField(
                                value = landmark,
                                onValueChange = { landmark = it },
                                label = { Text(s.landmarkLabel) },
                                placeholder = { Text("e.g. Opposite High School Playground") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_landmark")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val finalAddress = buildString {
                                        append(fullAddress.trim())
                                        if (landmark.isNotBlank()) append(" (Landmark: ${landmark.trim()})")
                                    }
                                    viewModel.registerCustomerWithOdishaAddress(
                                        name = fullName.ifBlank { "Sinapali Customer" },
                                        phone = mobileInput.trim(),
                                        email = emailInput.trim(),
                                        district = district.trim(),
                                        block = block.trim(),
                                        gp = gp.trim(),
                                        village = village.trim(),
                                        fullAddress = finalAddress
                                    ) {
                                        onSuccess()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_save_registration"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                                enabled = fullName.isNotBlank() && fullAddress.isNotBlank() && village.isNotBlank()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    s.saveAddressRegisterBtn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AuthMode {
    LOGIN,
    REGISTER
}
