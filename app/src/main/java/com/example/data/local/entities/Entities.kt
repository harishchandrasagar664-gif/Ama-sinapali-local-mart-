package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.*

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val role: UserRole,
    val shopId: Long? = null,
    val isVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val district: String = "Nuapada",
    val block: String = "Sinapali",
    val gp: String = "Sinapali",
    val village: String = "Sinapali",
    val deliveryAddress: String = "Near Panchayat Samiti Office, Word No 4, Sinapali",
    val profilePhotoUri: String? = null
)

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerId: Long,
    val name: String,
    val category: String,
    val address: String,
    val phone: String,
    val upiId: String = "sinapalimart@upi",
    val rating: Double = 4.8,
    val reviewCount: Int = 24,
    val isOpen: Boolean = true,
    val deliveryAvailable: Boolean = true,
    val minOrderAmount: Double = 50.0,
    val estimatedDeliveryMins: Int = 30,
    val isApproved: Boolean = true,
    val imageUrl: String = "hero_local_mart"
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val description: String = ""
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val name: String,
    val category: String,
    val description: String,
    val price: Double,
    val discountPrice: Double? = null,
    val unit: String = "kg",
    val stock: Int = 10,
    val isAvailable: Boolean = true,
    val imageUrl: String = "hero_local_mart",
    val isApproved: Boolean = true
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val productId: Long,
    val shopId: Long,
    val quantity: Int = 1,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val userId: Long,
    val shopId: Long,
    val totalAmount: Double,
    val discountAmount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val finalAmount: Double,
    val status: OrderStatus = OrderStatus.PLACED,
    val deliveryType: DeliveryType = DeliveryType.HOME_DELIVERY,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH_ON_DELIVERY,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val deliveryAddress: String,
    val customerPhone: String,
    val customerName: String,
    val orderNotes: String = "",
    val placedAt: Long = System.currentTimeMillis(),
    val deliveryAgentAssigned: String? = null,
    val estimatedDeliveryTime: Long = System.currentTimeMillis() + 1800000
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int,
    val totalPrice: Double
)

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String = "Home",
    val fullAddress: String,
    val landmark: String = "",
    val phone: String,
    val isDefault: Boolean = false,
    val district: String = "Nuapada",
    val block: String = "Sinapali",
    val gp: String = "Sinapali",
    val village: String = "Sinapali"
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subtitle: String,
    val tag: String,
    val imageUrl: String = "hero_local_mart",
    val actionUrl: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val couponCode: String,
    val description: String,
    val discountPercent: Double,
    val minOrderAmount: Double,
    val maxDiscountAmount: Double,
    val isActive: Boolean = true
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val userId: Long,
    val userName: String,
    val rating: Double,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "promotions")
data class PromotionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val shopName: String = "",
    val title: String,
    val description: String,
    val type: PromotionType,
    val status: PromotionStatus = PromotionStatus.PENDING_APPROVAL,
    val targetProductId: Long? = null,
    val targetProductName: String? = null,
    val location: String = "Sinapali Town",
    val durationDays: Int = 7,
    val startDate: String = "",
    val endDate: String = "",
    val dailyRate: Double = 50.0,
    val totalPrice: Double = 350.0,
    val paymentStatus: String = "Pending Payment",
    val adminNotes: String = "",
    val discountPercent: Double = 0.0,
    val couponCode: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long,
    val userId: Long,
    val shopId: Long,
    val issueType: String,
    val description: String,
    val status: ComplaintStatus = ComplaintStatus.OPEN,
    val resolutionNotes: String = "",
    val filedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_records")
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long,
    val paymentId: String,
    val amount: Double,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val timestamp: Long = System.currentTimeMillis()
)
