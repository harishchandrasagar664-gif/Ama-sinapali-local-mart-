@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.customer

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.ShopEntity
import com.example.domain.model.AppLanguage
import com.example.ui.components.OdiaGreetingHeader
import com.example.ui.components.ProductCard
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("ଆମ ସିନାପାଲି ଲୋକାଲ ମାର୍ଟ", color = LocalAmberLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Ama Sinapali Local Mart", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Town Fast Delivery • Vocal for Local", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    val matchingShops = remember(shops, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            shops.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val filteredProducts = remember(products, searchQuery, selectedCat, matchingShops) {
        val matchingShopIds = matchingShops.map { it.id }.toSet()
        products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.category.contains(searchQuery, ignoreCase = true) ||
                matchingShopIds.contains(p.shopId)
            val matchesCategory = selectedCat == "All" || p.category == selectedCat
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
                    icon = { Icon(Icons.Default.Person, contentDescription = s.navProfile) },
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
                    TextButton(
                        onClick = { viewModel.navigateTo(Screen.CustomerAuth) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (user?.isVerified == true) s.verifiedBadge else s.loginOtp,
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

            // Banners
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(banners) { b ->
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .height(110.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = LocalTealDark)
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

            // Category Chips
            item {
                Text(
                    s.shopByCategory,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCat == "All",
                            onClick = { viewModel.setSelectedCategory("All") },
                            label = { Text(s.allCategory) }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCat == cat.name,
                            onClick = { viewModel.setSelectedCategory(cat.name) },
                            label = { Text(cat.name) }
                        )
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
                Text(
                    s.trendingItems,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

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

@Composable
fun CategoriesScreen(viewModel: MainViewModel) {
    val categories by viewModel.categories.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val s = remember(appLanguage) { AppStrings.get(appLanguage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.allCategoriesTitle) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
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
            items(categories) { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setSelectedCategory(cat.name)
                            viewModel.navigateTo(Screen.CustomerHome)
                        },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = LocalTealPrimary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(cat.description, color = LocalTextSecondary, fontSize = 12.sp)
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
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
                    IconButton(onClick = { viewModel.navigateTo(Screen.CustomerHome) }) {
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(64.dp), tint = LocalTealPrimary)
                }
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
