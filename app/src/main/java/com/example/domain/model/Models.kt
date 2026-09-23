package com.example.domain.model

enum class UserRole {
    CUSTOMER,
    SHOP_OWNER,
    ADMIN
}

enum class OrderStatus(val label: String, val odiaLabel: String) {
    ORDER_PLACED("Order Placed", "ଅର୍ଡର ଦିଆଗଲା"),
    ORDER_CONFIRMED("Order Confirmed", "ଅର୍ଡର ସ୍ୱୀକୃତ"),
    PREPARING("Preparing", "ପ୍ୟାକ୍ ହେଉଛି"),
    READY_FOR_DELIVERY("Ready for Delivery", "ଡେଲିଭରୀ ପାଇଁ ପ୍ରସ୍ତୁତ"),
    DELIVERY_ACCEPTED("Delivery Accepted", "ଡେଲିଭରୀ ଗ୍ରହଣ କରାଗଲା"),
    PICKED_UP("Picked Up from Shop", "ଦୋକାନରୁ ସଂଗ୍ରହ ହୋଇଛି"),
    OUT_FOR_DELIVERY("Out for Delivery", "ଡେଲିଭରୀ ବାହାରିଛି"),
    DELIVERED("Delivered", "ଡେଲିଭରୀ ସମ୍ପନ୍ନ"),
    CANCELLED("Cancelled", "ବାତିଲ ହୋଇଛି")
}

enum class PaymentMethod(val label: String) {
    CASH_ON_DELIVERY("Cash on Delivery (Pay upon arrival)"),
    RAZORPAY_ONLINE("Razorpay Online (UPI / Card / NetBanking)")
}

enum class PaymentStatus(val label: String) {
    PENDING("Pending"),
    PAID("Paid"),
    FAILED("Failed")
}

enum class DeliveryType(val label: String, val odiaLabel: String) {
    HOME_DELIVERY("Home Delivery (ଘରକୁ ଡେଲିଭରୀ)", "ଘରକୁ ଡେଲିଭରୀ"),
    STORE_PICKUP("Self Store Pickup (ଦୋକାନରୁ ନିଜେ ନିଅନ୍ତୁ)", "ଦୋକାନରୁ ନିଜେ ନିଅନ୍ତୁ")
}

enum class PromotionType(val label: String, val defaultPrice: Double) {
    SPONSORED_SHOP("Sponsored Shop (Top Placement)", 499.0),
    SPONSORED_PRODUCT("Sponsored Product (Highlighted)", 199.0),
    HOMEPAGE_BANNER("Homepage Hero Banner", 799.0),
    FEATURED_SHOP("Featured Local Shop Tag", 299.0),
    FEATURED_PRODUCT("Featured Deal Product", 149.0),
    PROMOTIONAL_CAMPAIGN("Festive Local Campaign", 999.0)
}

enum class PromotionStatus(val label: String) {
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    PAID("Payment Received"),
    ACTIVE("Active Now"),
    REJECTED("Rejected"),
    EXPIRED("Expired")
}

enum class ComplaintStatus(val label: String) {
    OPEN("Open"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed")
}

enum class NotificationType {
    ORDER_STATUS,
    NEW_ORDER,
    NEW_OFFER,
    PROMOTION_UPDATE,
    PAYMENT_UPDATE
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
    val effectivePrice: Double get() = discountPrice ?: unitPrice
    val itemTotal: Double get() = effectivePrice * quantity
}
