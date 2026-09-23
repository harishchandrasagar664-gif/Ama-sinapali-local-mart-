package com.example.ui.viewmodel

sealed class Screen {
    object Splash : Screen()
    object CustomerAuth : Screen()
    object CustomerHome : Screen()
    object Categories : Screen()
    data class ProductDetail(val productId: Long) : Screen()
    data class ShopDetail(val shopId: Long) : Screen()
    object Cart : Screen()
    object Checkout : Screen()
    data class OrderTracking(val orderId: Long) : Screen()
    object CustomerProfile : Screen()
    object CustomerOrders : Screen()
    object CustomerNotifications : Screen()
    object CustomerAddresses : Screen()
    object CustomerHelp : Screen()
    object ShopOwnerAuth : Screen()
    object ShopOwnerDashboard : Screen()
    object ShopProductManagement : Screen()
    object ShopOrderManagement : Screen()
    object ShopOffersPromotions : Screen()
    object ShopProfile : Screen()
    object AdminAuth : Screen()
    object AdminDashboard : Screen()
    object AdminShops : Screen()
    object AdminProducts : Screen()
    object AdminPromotions : Screen()
    object AdminOrders : Screen()
    object AdminDelivery : Screen()
    object AdminUsers : Screen()
    object AdminComplaints : Screen()
}

data class OrderPlacementParams(
    val deliveryAddress: String,
    val contactPhone: String,
    val customerName: String,
    val deliveryType: com.example.domain.model.DeliveryType,
    val paymentMethod: com.example.domain.model.PaymentMethod,
    val couponCode: String? = null
)
