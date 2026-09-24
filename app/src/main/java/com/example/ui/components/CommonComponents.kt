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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
                    modifier = Modifier.size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mart_icon),
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
                        containerColor = LocalAmberSecondary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Portal", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(s.portalBtn, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    .height(120.dp)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = LocalTealPrimary.copy(alpha = 0.5f)
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
