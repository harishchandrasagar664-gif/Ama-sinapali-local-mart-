package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import java.io.File
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Shape
import com.example.R
import com.example.data.local.entities.ProductEntity
import com.example.domain.model.AppLanguage
import com.example.domain.model.OrderStatus
import com.example.domain.model.UserRole
import com.example.ui.locale.AppStrings
import com.example.ui.theme.*

@Composable
fun OdiaGreetingHeader(
    currentRole: UserRole,
    onRoleSwitchClick: () -> Unit,
    userName: String = "User",
    language: AppLanguage = AppLanguage.ENGLISH,
    onLanguageToggle: (() -> Unit)? = null
) {
    val s = AppStrings.get(language)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier.size(50.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sinapali_mart_logo),
                        contentDescription = s.appName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = s.odiaGreeting,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = s.appName,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${s.loggedAs}: $userName (${currentRole.name.replace('_', ' ')})",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onLanguageToggle != null) {
                    LanguageToggleChip(
                        currentLanguage = language,
                        onToggle = onLanguageToggle
                    )
                }
                FilledTonalButton(
                    onClick = onRoleSwitchClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (currentRole == UserRole.CUSTOMER) Color.White.copy(alpha = 0.25f) else LocalAmberSecondary,
                        contentColor = if (currentRole == UserRole.CUSTOMER) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    val icon = when (currentRole) {
                        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                        UserRole.SHOP_OWNER -> Icons.Default.Storefront
                        UserRole.CUSTOMER -> Icons.Default.Lock
                    }
                    val label = when (currentRole) {
                        UserRole.ADMIN -> "Admin Controls"
                        UserRole.SHOP_OWNER -> "Shop Portal"
                        UserRole.CUSTOMER -> "Staff Login"
                    }
                    Icon(icon, contentDescription = "Role Access", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun LanguageToggleChip(
    currentLanguage: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.2f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Translate,
                contentDescription = "Language Switcher",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (currentLanguage == AppLanguage.ENGLISH) LocalAmberSecondary else Color.Transparent
            ) {
                Text(
                    "EN",
                    color = if (currentLanguage == AppLanguage.ENGLISH) Color.Black else Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (currentLanguage == AppLanguage.ODIA) LocalAmberSecondary else Color.Transparent
            ) {
                Text(
                    "ଓଡ଼ିଆ",
                    color = if (currentLanguage == AppLanguage.ODIA) Color.Black else Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: OrderStatus,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    val s = AppStrings.get(language)
    val (bg, fg) = when (status) {
        OrderStatus.PLACED -> Pair(Color(0xFFE3F2FD), Color(0xFF1976D2))
        OrderStatus.ACCEPTED, OrderStatus.PREPARING -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        OrderStatus.READY_FOR_PICKUP -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        OrderStatus.OUT_FOR_DELIVERY -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        OrderStatus.DELIVERED -> Pair(Color(0xFFE0F2F1), Color(0xFF00796B))
        OrderStatus.CANCELLED -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = s.getStatusText(status),
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ProductImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    shape: Shape = RoundedCornerShape(8.dp),
    fallbackIcon: ImageVector = Icons.Default.ShoppingBag
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color(0xFFF8FAFC)
    ) {
        when {
            imageUrl.isNullOrBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = contentDescription,
                        tint = LocalTealPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            imageUrl == "ic_mart_icon" -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_mart_icon),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            imageUrl == "hero_local_mart" -> {
                Image(
                    painter = painterResource(id = R.drawable.hero_local_mart),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            imageUrl.startsWith("data:") -> {
                val bitmap = remember(imageUrl) {
                    try {
                        val base64Data = imageUrl.substringAfter(",")
                        val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.hero_local_mart),
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                }
            }
            else -> {
                val cleanPath = imageUrl.removePrefix("file:")
                val model: Any = if (cleanPath.startsWith("/")) {
                    val file = File(cleanPath)
                    if (file.exists()) file else imageUrl
                } else {
                    imageUrl
                }
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = LocalTealPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Image(
                            painter = painterResource(id = R.drawable.hero_local_mart),
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = contentScale
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onProductClick: () -> Unit,
    isWishlisted: Boolean,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
    language: AppLanguage = AppLanguage.ENGLISH
) {
    val s = AppStrings.get(language)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProductClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                ProductImage(
                    imageUrl = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    contentScale = ContentScale.Fit
                )
                if (product.stock <= 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = s.outOfStock,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.category,
                    color = LocalTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val displayPrice = product.discountPrice ?: product.price
                        Text(
                            text = "₹${displayPrice.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = LocalTealPrimary
                        )
                        if (product.discountPrice != null) {
                            Text(
                                text = "₹${product.price.toInt()}",
                                fontSize = 11.sp,
                                color = LocalTextMuted
                            )
                        }
                    }
                    IconButton(
                        onClick = onAddToCart,
                        enabled = product.stock > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (product.stock > 0) LocalTealPrimary else Color.LightGray,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = s.addToBasket,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccessDeniedScreen(
    title: String,
    message: String,
    onGoBack: () -> Unit,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Access Denied",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onGoBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Return to Customer Store", color = Color.White)
                }

                if (secondaryActionText != null && onSecondaryAction != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(secondaryActionText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
