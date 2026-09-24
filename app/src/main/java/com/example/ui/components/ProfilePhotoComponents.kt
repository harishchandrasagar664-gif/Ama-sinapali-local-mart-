package com.example.ui.components

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.local.entities.ShopEntity
import com.example.domain.model.AppLanguage
import com.example.ui.locale.AppStrings
import com.example.ui.theme.*
import com.example.util.ImageStorageHelper
import java.io.File

@Composable
fun UserAvatar(
    photoUri: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showEditBadge: Boolean = false,
    onEditClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onEditClick != null) Modifier.clickable { onEditClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = LocalTealPrimary.copy(alpha = 0.15f),
            shadowElevation = 2.dp
        ) {
            when {
                photoUri.isNullOrBlank() -> {
                    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(LocalTealPrimary, LocalTealDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = (size.value * 0.42f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                photoUri == "ic_mart_icon" -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mart_icon),
                        contentDescription = name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                photoUri == "hero_local_mart" -> {
                    Image(
                        painter = painterResource(id = R.drawable.hero_local_mart),
                        contentDescription = name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    val model = if (photoUri.startsWith("/")) File(photoUri) else photoUri
                    SubcomposeAsyncImage(
                        model = model,
                        contentDescription = name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(size * 0.4f),
                                    color = LocalTealPrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(LocalTealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(size * 0.5f)
                                )
                            }
                        }
                    )
                }
            }
        }

        if (showEditBadge) {
            Surface(
                modifier = Modifier
                    .size((size.value * 0.36f).coerceAtLeast(24f).dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp),
                shape = CircleShape,
                color = LocalAmberSecondary,
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Edit photo",
                        tint = Color.Black,
                        modifier = Modifier.size((size.value * 0.20f).coerceAtLeast(14f).dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfilePhotoUploadDialog(
    language: AppLanguage,
    currentPhotoUri: String?,
    associatedShop: ShopEntity? = null,
    onDismissRequest: () -> Unit,
    onPhotoSelected: (String?) -> Unit,
    onShopPhotoSelected: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val s = AppStrings.get(language)
    var alsoSetAsShopPhoto by remember { mutableStateOf(false) }

    fun handlePhotoResult(savedPath: String) {
        onPhotoSelected(savedPath)
        if (alsoSetAsShopPhoto && onShopPhotoSelected != null) {
            onShopPhotoSelected(savedPath)
        }
        Toast.makeText(context, s.photoUpdatedSuccess, Toast.LENGTH_SHORT).show()
        onDismissRequest()
    }

    // Gallery launcher using modern zero-permission Android Photo Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.copyUri(context, uri, prefix = "profile_dp")
            if (savedPath != null) {
                handlePhotoResult(savedPath)
            } else {
                Toast.makeText(context, "Failed to load photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedPath = ImageStorageHelper.saveBitmap(context, bitmap, prefix = "camera_dp")
            handlePhotoResult(savedPath)
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, s.cameraPermissionRequired, Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = LocalTealPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = s.changeProfilePhoto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option 1: Camera
                OutlinedCard(
                    onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LocalTealPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = LocalTealPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = s.takePhotoCamera,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (language == AppLanguage.ODIA) "ନୂଆ ଫଟୋ ଉଠାଇ DP ସେଟ୍ କରନ୍ତୁ" else "Capture a fresh photo using camera",
                                fontSize = 11.sp,
                                color = LocalTextSecondary
                            )
                        }
                    }
                }

                // Option 2: Gallery
                OutlinedCard(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LocalAmberSecondary.copy(alpha = 0.25f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = LocalAmberDark)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = s.chooseGallery,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (language == AppLanguage.ODIA) "ମୋବାଇଲ୍ ଗ୍ୟାଲେରୀରୁ ଫଟୋ ବାଛନ୍ତୁ" else "Select existing image from device",
                                fontSize = 11.sp,
                                color = LocalTextSecondary
                            )
                        }
                    }
                }

                // Shop Owner Toggle: Also update shop's banner/logo
                if (associatedShop != null && onShopPhotoSelected != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { alsoSetAsShopPhoto = !alsoSetAsShopPhoto }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = alsoSetAsShopPhoto,
                                onCheckedChange = { alsoSetAsShopPhoto = it },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.alsoUpdateShopPhoto,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Option 3: Use Shop Photo / Logo as DP (For Shop Owners)
                if (associatedShop != null) {
                    OutlinedCard(
                        onClick = {
                            val shopPhoto = associatedShop.imageUrl.ifBlank { "ic_mart_icon" }
                            onPhotoSelected(shopPhoto)
                            Toast.makeText(context, "Shop photo set as your DP! 🏪✨", Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = LocalTealDark)
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = s.useShopLogoAsDp,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LocalTealDark
                                )
                                Text(
                                    text = "${associatedShop.name} (${if (language == AppLanguage.ODIA) "ଦୋକାନ ଫଟୋ" else "Shop Photo"})",
                                    fontSize = 11.sp,
                                    color = LocalTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Option 4: Use Official Ama Sinapali Mart Logo as DP
                    OutlinedCard(
                        onClick = {
                            onPhotoSelected("ic_mart_icon")
                            Toast.makeText(context, "Sinapali Mart Logo set as your DP! 🛍️✨", Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 1.dp,
                                modifier = Modifier.size(40.dp)
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
                                    text = s.useMartLogoAsDp,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (language == AppLanguage.ODIA) "ଅଫିସିଆଲ୍ ମାର୍ଟ ଲୋଗୋ DP ଭାବେ ରଖନ୍ତୁ" else "Official Ama Sinapali Mart circular logo",
                                    fontSize = 11.sp,
                                    color = LocalTextSecondary
                                )
                            }
                        }
                    }
                }

                // Option: Remove Photo
                if (!currentPhotoUri.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            onPhotoSelected(null)
                            Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = LocalError)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.removePhoto, color = LocalError, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(s.close, fontWeight = FontWeight.Bold)
            }
        }
    )
}
