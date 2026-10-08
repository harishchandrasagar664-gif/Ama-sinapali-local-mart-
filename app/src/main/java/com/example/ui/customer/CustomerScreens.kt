@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.PromotionEntity
import com.example.data.local.entities.ShopEntity
import com.example.domain.model.AppLanguage
import com.example.domain.model.PromotionStatus
import com.example.domain.model.PromotionType
import com.example.ui.components.OdiaGreetingHeader
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductImage
import com.example.ui.components.UserAvatar
import com.example.ui.locale.AppStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        onTimeout()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalTealPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.size(140.dp)
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
            Spacer(modifier = Modifier.height(20.dp))
            Text("ଆମ ସିନାପାଲି ଲୋକାଲ ମାର୍ଟ", color = LocalAmberLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Ama Sinapali Local Mart", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Town Fast Delivery • Vocal for Local", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
    }
}

@Composable
fun CustomerHomeScreen(
    viewModel: MainViewModel,
    onRoleSwitchClick: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    val shops by viewModel.activeShops.collectAsState()
    val products by viewModel.activeProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val banners by viewModel.banners.collectAsState()
    val approvedPromotions by viewModel.approvedPromotions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    val approvedBanners = remember(approvedPromotions) {
        approvedPromotions.filter { it.type == PromotionType.PROMOTIONAL_BANNER || it.type == PromotionType.BANNER }
    }
    val approvedFeaturedShops = remember(approvedPromotions, shops) {
        val featuredPromoShopIds = approvedPromotions.filter { it.type == PromotionType.FEATURED_SHOP }.map { it.shopId }.toSet()
        shops.filter { featuredPromoShopIds.contains(it.id) }
    }
    val approvedProductAds = remember(approvedPromotions) {
        approvedPromotions.filter { it.type == PromotionType.PRODUCT_AD || it.type == PromotionType.FEATURED_PRODUCT }
    }
    val approvedAreaOffers = remember(approvedPromotions) {
        approvedPromotions.filter { it.type == PromotionType.LOCATION_BASED || it.type == PromotionType.DATE_BASED }
    }

    val matchingShops = remember(shops, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            shops.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val allCategoryList = remember(categories, products) {
        val shoppingCats = com.example.data.local.SeedData.shoppingCategories
        val catNamesFromDb = categories.map { it.name }
        val catNamesFromProducts = products.map { it.category }
        val defaultList = com.example.data.local.SeedData.defaultCategoryNames
        val combined = (shoppingCats + catNamesFromDb + catNamesFromProducts + defaultList).distinct().filter { it.isNotBlank() }
        listOf("All") + combined
    }

    val filteredProducts = remember(products, searchQuery, selectedCat, matchingShops) {
        val matchingShopIds = matchingShops.map { it.id }.toSet()
        products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.category.contains(searchQuery, ignoreCase = true) ||
                matchingShopIds.contains(p.shopId)
            val matchesCategory = productMatchesCategory(p.category, selectedCat)
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            OdiaGreetingHeader(
                currentRole = currentRole,
                onRoleSwitchClick = onRoleSwitchClick,
                userName = user?.name ?: "Customer",
                language = appLanguage,
                onLanguageToggle = { viewModel.toggleLanguage() }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = true,
                    onClick = { viewModel.navigateTo(Screen.CustomerHome) },
                    icon = { Icon(Icons.Default.Home, contentDescription = s.navHome) },
                    label = { Text(s.navHome) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.Categories) },
                    icon = { Icon(Icons.Default.Category, contentDescription = s.navCategories) },
                    label = { Text(s.navCategories) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.Cart) },
                    icon = {
                        BadgedBox(badge = {
                            if (cartItems.isNotEmpty()) Badge { Text("${cartItems.sumOf { it.quantity }}") }
                        }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = s.navCart)
                        }
                    },
                    label = { Text(s.navCart) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.CustomerOrders) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = s.navOrders) },
                    label = { Text(s.navOrders) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.CustomerProfile) },
                    icon = {
                        if (!user?.profilePhotoUri.isNullOrBlank()) {
                            UserAvatar(
                                photoUri = user?.profilePhotoUri,
                                name = user?.name ?: "Customer",
                                size = 24.dp
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = s.navProfile)
                        }
                    },
                    label = { Text(s.navProfile) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(Screen.CustomerProfile) }
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = LocalTealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${s.deliverTo} ${user?.village ?: "Sinapali"}, ${user?.district ?: "Nuapada"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            color = LocalTextSecondary
                        )
                    }
                    val isCustomerActive = viewModel.authManager.isCustomerSessionActive() || (user?.isVerified == true && user?.id != null)
                    TextButton(
                        onClick = {
                            viewModel.navigateTo(Screen.CustomerProfile)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        if (!user?.profilePhotoUri.isNullOrBlank()) {
                            UserAvatar(
                                photoUri = user?.profilePhotoUri,
                                name = user?.name ?: "Customer",
                                size = 24.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isCustomerActive) s.verifiedBadge else s.loginOtp,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text(s.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Quick Merchant / Shop Owner Access Banner
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.navigateTo(Screen.ShopOwnerAuth) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE65100),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Shop Owner? Login / Register Store (OTP)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFBF360C)
                                )
                                Text(
                                    "Register with Owner Name, Shop Photo & Location",
                                    fontSize = 10.sp,
                                    color = LocalTextSecondary
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (matchingShops.isNotEmpty()) {
                item {
                    Text(
                        s.matchingShops,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = LocalTealPrimary,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 6.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(matchingShops) { shop ->
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable { viewModel.navigateTo(Screen.ShopDetail(shop.id)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Store, contentDescription = null, tint = LocalTealDark)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                    }
                                    Text(shop.category, color = LocalTextSecondary, fontSize = 11.sp, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("📍 ${shop.address}", fontSize = 11.sp, color = LocalTextSecondary, maxLines = 1)
                                    Text(s.viewShopProducts, color = LocalTealPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Banners (System Banners + Admin-Approved Shop Promotional Banners)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Admin-Approved Shop Promotional Banners
                    items(approvedBanners) { promo ->
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .height(115.dp)
                                .clickable {
                                    if (promo.shopId > 0) {
                                        viewModel.navigateTo(Screen.ShopDetail(promo.shopId))
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = LocalTealDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = LocalAmberSecondary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "📢 ${promo.shopName.ifBlank { "Promoted" }}",
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        "📍 ${promo.location}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    promo.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    promo.description,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    // 2. Default System Banners
                    items(banners) { b ->
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .height(115.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = LocalTealPrimary)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Surface(
                                    color = LocalAmberSecondary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        b.tag,
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(b.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(b.subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Admin-Approved Featured Shops
            if (approvedFeaturedShops.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⭐", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Featured Sinapali Shops",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = LocalTealDark
                                )
                            }
                            Surface(
                                color = LocalAmberSecondary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Admin Verified",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAmberDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(approvedFeaturedShops) { shop ->
                                Card(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clickable { viewModel.navigateTo(Screen.ShopDetail(shop.id)) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, LocalAmberDark),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = LocalAmberSecondary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "⭐ FEATURED",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text("📍 ${shop.address}", fontSize = 10.sp, color = LocalTextSecondary, maxLines = 1)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                        Text(shop.category, fontSize = 11.sp, color = LocalTextSecondary, maxLines = 1)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Visit Featured Shop →",
                                            color = LocalTealPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Admin-Approved Product Ads (Spotlight Sponsored Deals)
            if (approvedProductAds.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = LocalTealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Spotlight Deals & Product Ads",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = LocalTealDark
                                )
                            }
                            Surface(
                                color = LocalTealPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Sponsored",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalTealPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(approvedProductAds) { ad ->
                                Card(
                                    modifier = Modifier
                                        .width(240.dp)
                                        .clickable {
                                            if (ad.targetProductId != null && ad.targetProductId > 0) {
                                                viewModel.navigateTo(Screen.ProductDetail(ad.targetProductId))
                                            } else if (ad.shopId > 0) {
                                                viewModel.navigateTo(Screen.ShopDetail(ad.shopId))
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LocalTealPrimary.copy(alpha = 0.5f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = LocalTealPrimary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "SPONSORED AD",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text("📍 ${ad.location}", fontSize = 10.sp, color = LocalTextSecondary)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            ad.targetProductName ?: ad.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            "By ${ad.shopName}",
                                            fontSize = 11.sp,
                                            color = LocalTextSecondary,
                                            maxLines = 1
                                        )
                                        if (ad.description.isNotBlank()) {
                                            Text(
                                                ad.description,
                                                fontSize = 11.sp,
                                                color = LocalTealDark,
                                                maxLines = 1
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "View Deal Details →",
                                            color = LocalTealPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Admin-Approved Location & Date-Based Promotions
            if (approvedAreaOffers.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📍", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Sinapali Area & Festival Offers",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = LocalTealDark
                                )
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(approvedAreaOffers) { offer ->
                                Card(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .clickable {
                                            if (offer.shopId > 0) {
                                                viewModel.navigateTo(Screen.ShopDetail(offer.shopId))
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = if (offer.type == PromotionType.LOCATION_BASED) LocalTealPrimary else LocalAmberDark,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    if (offer.type == PromotionType.LOCATION_BASED) "📍 ${offer.location}" else "📅 FESTIVAL SPECIAL",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (offer.endDate.isNotBlank()) {
                                                Text("Till ${offer.endDate}", fontSize = 10.sp, color = LocalTextSecondary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(offer.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text(offer.description, fontSize = 11.sp, color = LocalTextSecondary, maxLines = 2)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Offered by: ${offer.shopName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LocalTealPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Chips (Horizontally Scrollable)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            tint = LocalTealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            s.shopByCategory,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    if (!selectedCat.equals("All", ignoreCase = true)) {
                        TextButton(
                            onClick = { viewModel.setSelectedCategory("All") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "✕ ${s.allCategory}",
                                fontSize = 12.sp,
                                color = LocalTealPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "${allCategoryList.size - 1} ${if (s.isOdia) "ବର୍ଗ" else "Categories"}",
                            fontSize = 12.sp,
                            color = LocalTextSecondary
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allCategoryList) { catName ->
                        val isSelected = selectedCat.equals(catName, ignoreCase = true)
                        val displayName = if (catName.equals("All", ignoreCase = true)) s.allCategory else s.getCategoryDisplayName(catName)
                        val emoji = s.getCategoryEmoji(catName)
                        val count = if (catName.equals("All", ignoreCase = true)) products.size else products.count { productMatchesCategory(it.category, catName) }

                        Surface(
                            onClick = { viewModel.setSelectedCategory(catName) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) LocalTealPrimary else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) LocalTealDark else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shadowElevation = if (isSelected) 3.dp else 1.dp,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Column {
                                    Text(
                                        text = displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (count > 0) {
                                        Text(
                                            text = "$count ${if (s.isOdia) "ଆଇଟମ୍" else "items"}",
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else LocalTextSecondary
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Local Shops
            item {
                Text(
                    s.popularShops,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shops) { shop ->
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { viewModel.navigateTo(Screen.ShopDetail(shop.id)) },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Store, contentDescription = null, tint = LocalTealPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                }
                                Text(shop.category, color = LocalTextSecondary, fontSize = 11.sp, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("★ ${shop.rating} • ${shop.estimatedDeliveryMins} ${s.mins}", color = LocalAmberDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Products Grid Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selectedCat.equals("All", ignoreCase = true)) s.trendingItems else "${s.getCategoryDisplayName(selectedCat)} (${filteredProducts.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (!selectedCat.equals("All", ignoreCase = true)) {
                        Text(
                            text = "${filteredProducts.size} ${if (s.isOdia) "ଉପଲବ୍ଧ" else "items"}",
                            fontSize = 12.sp,
                            color = LocalTextSecondary
                        )
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🛒",
                                fontSize = 36.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (s.isOdia) "ଏହି ବର୍ଗରେ ଏପର୍ଯ୍ୟନ୍ତ କୌଣସି ଉତ୍ପାଦ ମିଳିଲା ନାହିଁ।" else "No products found in this category right now.",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.setSelectedCategory("All") },
                                colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(s.allCategory)
                            }
                        }
                    }
                }
            } else {
                // Products
                items(filteredProducts.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (prod in pair) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProductCard(
                                    product = prod,
                                    onProductClick = { viewModel.navigateTo(Screen.ProductDetail(prod.id)) },
                                    isWishlisted = false,
                                    onAddToCart = { viewModel.addToCart(prod.id, prod.shopId, 1) },
                                    language = appLanguage
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesScreen(viewModel: MainViewModel) {
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.activeProducts.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    val allCategoryNames = remember(categories, products) {
        val shoppingCats = com.example.data.local.SeedData.shoppingCategories
        val fromDb = categories.map { it.name }
        val fromProducts = products.map { it.category }
        val defaultList = com.example.data.local.SeedData.defaultCategoryNames
        val combined = (shoppingCats + fromDb + fromProducts + defaultList).distinct().filter { it.isNotBlank() }
        listOf("All") + combined
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.allCategoriesTitle) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allCategoryNames) { catName ->
                val displayName = if (catName.equals("All", ignoreCase = true)) s.allCategory else s.getCategoryDisplayName(catName)
                val emoji = s.getCategoryEmoji(catName)
                val count = if (catName.equals("All", ignoreCase = true)) products.size else products.count { productMatchesCategory(it.category, catName) }
                val catEntity = categories.find { it.name.equals(catName, ignoreCase = true) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setSelectedCategory(catName)
                            viewModel.navigateTo(Screen.CustomerHome)
                        },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            val desc = catEntity?.description ?: if (catName == "All") (if (s.isOdia) "ସମସ୍ତ ସାମଗ୍ରୀ ଓ ଉତ୍ପାଦ ବ୍ରାଉଜ କରନ୍ତୁ" else "Browse all available local products") else (if (s.isOdia) "ସ୍ଥାନୀୟ ସିନାପାଲି ଉତ୍ପାଦ" else "Local Sinapali products")
                            Text(desc, color = LocalTextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LocalTealPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$count",
                                color = LocalTealPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopDetailScreen(shopId: Long, viewModel: MainViewModel) {
    val shops by viewModel.allShops.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    val shop = shops.find { it.id == shopId }
    val shopProducts = products.filter { it.shopId == shopId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(shop?.name ?: s.shopDetails) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { padding ->
        if (shop != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(shop.category, color = LocalTealDark, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("📍 ${shop.address}", fontSize = 12.sp)
                            Text("📞 ${shop.phone} • UPI: ${shop.upiId}", fontSize = 12.sp)
                            Text("⭐ ${shop.rating} (${shop.reviewCount} reviews) • Delivery in ~${shop.estimatedDeliveryMins}${s.mins}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(s.productsFromShop, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                items(shopProducts) { prod ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { viewModel.navigateTo(Screen.ProductDetail(prod.id)) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductImage(
                                imageUrl = prod.imageUrl,
                                contentDescription = prod.name,
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("₹${prod.price.toInt()} per ${prod.unit}", color = LocalTealPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { viewModel.addToCart(prod.id, shop.id, 1) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                            ) {
                                Text("+ ${s.addToBasket}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailScreen(productId: Long, viewModel: MainViewModel) {
    val products by viewModel.allProducts.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    val product = products.find { it.id == productId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: s.productDetails) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { padding ->
        if (product != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                ProductImage(
                    imageUrl = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(product.category, color = LocalTextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${(product.discountPrice ?: product.price).toInt()}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = LocalTealPrimary)
                    if (product.discountPrice != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("₹${product.price.toInt()}", fontSize = 14.sp, color = LocalTextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(product.description, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        viewModel.addToCart(product.id, product.shopId, 1)
                        viewModel.navigateTo(Screen.Cart)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalTealPrimary)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${s.addToCartBtn} & ${s.navCart}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun productMatchesCategory(productCategory: String, selectedCategory: String): Boolean {
    if (selectedCategory.equals("All", ignoreCase = true)) return true
    if (productCategory.equals(selectedCategory, ignoreCase = true)) return true

    val normSelected = selectedCategory.trim().lowercase().replace("’", "'")
    val normProd = productCategory.trim().lowercase().replace("’", "'")

    if (normProd.contains(normSelected)) return true

    return when (normSelected) {
        "clothing" -> normProd.contains("clothes") || normProd.contains("handloom") || normProd.contains("wear") || normProd.contains("saree")
        "saree" -> normProd.contains("saree") || normProd.contains("handloom")
        "chappal" -> normProd.contains("chappal") || normProd.contains("slipper") || normProd.contains("sandals")
        "shoes" -> normProd.contains("shoe") || normProd.contains("footwear")
        "cosmetics" -> normProd.contains("cosmetic") || normProd.contains("personal care") || normProd.contains("hygiene") || normProd.contains("beauty")
        "fancy" -> normProd.contains("fancy") || normProd.contains("gift") || normProd.contains("puja") || normProd.contains("bangle")
        "bags" -> normProd.contains("bag") || normProd.contains("backpack") || normProd.contains("luggage")
        "jewellery", "jewelry" -> normProd.contains("jewel") || normProd.contains("ornament") || normProd.contains("necklace") || normProd.contains("bangle")
        "kids wear" -> normProd.contains("kid") || normProd.contains("baby") || normProd.contains("child")
        "men's wear", "mens wear" -> normProd.contains("men") || normProd.contains("shirt")
        "women's wear", "womens wear" -> normProd.contains("women") || normProd.contains("kurti") || normProd.contains("saree")
        "household items" -> normProd.contains("household") || normProd.contains("home") || normProd.contains("kitchen")
        "grocery" -> normProd.contains("grocer") || normProd.contains("spice") || normProd.contains("dal") || normProd.contains("rice")
        "vegetables" -> normProd.contains("vegetable") || normProd.contains("organic")
        "fruits" -> normProd.contains("fruit")
        "electronics" -> normProd.contains("electronic") || normProd.contains("mobile") || normProd.contains("device")
        "other" -> normProd.contains("other") || normProd.contains("paan") || normProd.contains("hardware") || normProd.contains("stationery") || normProd.contains("medicine")
        else -> false
    }
}

