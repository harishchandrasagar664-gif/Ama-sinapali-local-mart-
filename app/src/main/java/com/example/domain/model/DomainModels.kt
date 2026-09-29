package com.example.domain.model

enum class UserRole {
    CUSTOMER,
    SHOP_OWNER,
    ADMIN
}

enum class OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}

enum class DeliveryType {
    SELF_PICKUP,
    HOME_DELIVERY
}

enum class PaymentMethod {
    CASH_ON_DELIVERY,
    ONLINE_UPI,
    RAZORPAY
}

enum class PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}

enum class ComplaintStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED,
    REJECTED
}

enum class PromotionType {
    PRODUCT_AD,
    PROMOTIONAL_BANNER,
    FEATURED_SHOP,
    LOCATION_BASED,
    DATE_BASED,
    BANNER,
    FEATURED_PRODUCT,
    DISCOUNT_COUPON
}

enum class PromotionStatus {
    PENDING_APPROVAL,
    APPROVED,
    ACTIVE,
    EXPIRED,
    REJECTED
}

enum class NotificationType {
    ORDER_STATUS,
    OFFER,
    SYSTEM,
    DELIVERY
}

data class CartItemWithProduct(
    val cartItemId: Long,
    val productId: Long,
    val shopId: Long,
    val shopName: String,
    val productName: String,
    val imageUrl: String,
    val unitPrice: Double,
    val discountPrice: Double?,
    val quantity: Int,
    val stock: Int
) {
    val effectivePrice: Double
        get() = discountPrice ?: unitPrice

    val itemTotal: Double
        get() = effectivePrice * quantity
}
