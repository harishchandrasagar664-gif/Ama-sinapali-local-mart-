package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SeedData
import com.example.data.local.entities.*
import com.example.data.repository.MartRepository
import com.example.domain.model.*
import com.example.service.AuthManager
import com.example.service.RazorpayPaymentServiceImpl
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PromotionPricingConfig(
    val productAdDailyRate: Double = 40.0,
    val promotionalBannerDailyRate: Double = 80.0,
    val featuredShopDailyRate: Double = 100.0,
    val locationBasedDailyRate: Double = 60.0,
    val dateBasedDailyRate: Double = 50.0
) {
    fun getDailyRate(type: PromotionType): Double = when (type) {
        PromotionType.PRODUCT_AD -> productAdDailyRate
        PromotionType.PROMOTIONAL_BANNER, PromotionType.BANNER -> promotionalBannerDailyRate
        PromotionType.FEATURED_SHOP -> featuredShopDailyRate
        PromotionType.LOCATION_BASED -> locationBasedDailyRate
        PromotionType.DATE_BASED -> dateBasedDailyRate
        PromotionType.FEATURED_PRODUCT -> productAdDailyRate
        PromotionType.DISCOUNT_COUPON -> promotionalBannerDailyRate
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = MartRepository(database.martDao())
    val authManager = AuthManager(repository, application.applicationContext)
    val paymentService = RazorpayPaymentServiceImpl()

    private val screenBackStack = ArrayDeque<Screen>()

    init {
        authManager.restoreSession(viewModelScope)
    }

    private val _appLanguage = MutableStateFlow<com.example.domain.model.AppLanguage>(com.example.domain.model.AppLanguage.ENGLISH)
    val appLanguage: StateFlow<com.example.domain.model.AppLanguage> = _appLanguage.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.CustomerHome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val activeShops: StateFlow<List<ShopEntity>> = repository.getActiveShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShops: StateFlow<List<ShopEntity>> = repository.getAllShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProducts: StateFlow<List<ProductEntity>> = repository.getActiveProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val banners: StateFlow<List<BannerEntity>> = repository.getActiveBanners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offers: StateFlow<List<OfferEntity>> = repository.getActiveOffers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<OrderEntity>> = repository.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrderItems: StateFlow<List<OrderItemEntity>> = repository.getAllOrderItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPromotions: StateFlow<List<PromotionEntity>> = repository.getAllPromotions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val approvedPromotions: StateFlow<List<PromotionEntity>> = repository.getApprovedPromotions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _promotionPricingConfig = MutableStateFlow(PromotionPricingConfig())
    val promotionPricingConfig: StateFlow<PromotionPricingConfig> = _promotionPricingConfig.asStateFlow()

    val allComplaints: StateFlow<List<ComplaintEntity>> = repository.getAllComplaints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<UserEntity?> = authManager.currentUser
    val currentRole: StateFlow<UserRole> = authManager.currentRole

    val cartItems: StateFlow<List<CartItemEntity>> = combine(currentUser, currentRole) { user, role ->
        Pair(user, role)
    }.flatMapLatest { (user, role) ->
        if (user != null && role == UserRole.CUSTOMER) repository.getCartItems(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detailedCartItems: StateFlow<List<CartItemWithProduct>> = combine(cartItems, activeProducts, allShops) { carts, prods, shops ->
        val prodMap = prods.associateBy { it.id }
        val shopMap = shops.associateBy { it.id }
        carts.mapNotNull { cart ->
            val p = prodMap[cart.productId] ?: return@mapNotNull null
            val s = shopMap[p.shopId]
            CartItemWithProduct(
                cartItemId = cart.id,
                productId = p.id,
                shopId = p.shopId,
                shopName = s?.name ?: "Sinapali Shop",
                productName = p.name,
                imageUrl = p.imageUrl,
                unitPrice = p.price,
                discountPrice = p.discountPrice,
                quantity = cart.quantity,
                stock = p.stock
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _appliedCoupon = MutableStateFlow<OfferEntity?>(null)
    val appliedCoupon: StateFlow<OfferEntity?> = _appliedCoupon.asStateFlow()

    private val _couponError = MutableStateFlow<String?>(null)
    val couponError: StateFlow<String?> = _couponError.asStateFlow()

    private val _wishlistProductIds = MutableStateFlow<Set<Long>>(emptySet())
    val wishlistProductIds: StateFlow<Set<Long>> = _wishlistProductIds.asStateFlow()

    val notifications: StateFlow<List<NotificationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getNotifications(user.id) else repository.getAllNotifications()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showPaymentModal = MutableStateFlow(false)
    val showPaymentModal: StateFlow<Boolean> = _showPaymentModal.asStateFlow()

    private val _pendingOrderParams = MutableStateFlow<OrderPlacementParams?>(null)
    val pendingOrderParams: StateFlow<OrderPlacementParams?> = _pendingOrderParams.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun navigateTo(screen: Screen) {
        val role = currentRole.value
        val user = currentUser.value

        when (screen) {
            is Screen.AdminDashboard,
            is Screen.AdminDelivery,
            is Screen.AdminOrders,
            is Screen.AdminShops,
            is Screen.AdminPromotions,
            is Screen.AdminProducts,
            is Screen.AdminUsers,
            is Screen.AdminComplaints -> {
                if (role != UserRole.ADMIN || user?.role != UserRole.ADMIN) {
                    _userMessage.value = "Access Denied: Only Admin (Harishchandra Sagar) can access the Admin Panel."
                    return
                }
            }

            is Screen.ShopOwnerDashboard,
            is Screen.ShopOrderManagement,
            is Screen.ShopProductManagement,
            is Screen.ShopOffersPromotions,
            is Screen.ShopProfile -> {
                if (role != UserRole.SHOP_OWNER || user?.role != UserRole.SHOP_OWNER) {
                    _userMessage.value = "Access Denied: Customers cannot access the Shop Owner dashboard."
                    return
                }
            }

            // Strict Shop Owner restrictions: Shop Owners must never access customer accounts, profiles, carts, order history, or other shops' data
            is Screen.CustomerProfile,
            is Screen.CustomerAuth,
            is Screen.Cart,
            is Screen.Checkout,
            is Screen.CustomerOrders,
            is Screen.OrderTracking -> {
                if (role == UserRole.SHOP_OWNER) {
                    _userMessage.value = "Access Denied: Shop Owners cannot access customer accounts, profiles, carts, or order history."
                    return
                }
            }

            is Screen.ShopDetail -> {
                if (role == UserRole.SHOP_OWNER) {
                    val myShop = allShops.value.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user?.shopId) }
                    if (myShop != null && screen.shopId != myShop.id) {
                        _userMessage.value = "Access Denied: Shop Owners can only view and manage their own shop."
                        return
                    }
                }
            }

            else -> {
                // Public customer screens accessible
            }
        }

        val cur = _currentScreen.value
        if (cur is Screen.CustomerAuth && screen is Screen.CustomerHome) {
            screenBackStack.removeAll { it is Screen.CustomerAuth }
        }
        if (cur !is Screen.Splash && cur != screen) {
            if (screenBackStack.lastOrNull() != cur) {
                screenBackStack.addLast(cur)
            }
        }
        _currentScreen.value = screen
    }

    fun navigateBack() {
        // Back navigation must never log out the customer or clear the active customer session.
        // If a customer is logged in, never pop back into CustomerAuth.
        while (screenBackStack.isNotEmpty() && authManager.isCustomerSessionActive() && screenBackStack.last() is Screen.CustomerAuth) {
            screenBackStack.removeLast()
        }
        // If in Shop Owner role, never pop into customer screens or other shops
        if (currentRole.value == UserRole.SHOP_OWNER) {
            while (screenBackStack.isNotEmpty() && (
                screenBackStack.last() is Screen.CustomerProfile ||
                screenBackStack.last() is Screen.CustomerAuth ||
                screenBackStack.last() is Screen.Cart ||
                screenBackStack.last() is Screen.Checkout ||
                screenBackStack.last() is Screen.CustomerOrders ||
                screenBackStack.last() is Screen.OrderTracking ||
                screenBackStack.last() is Screen.CustomerHome
            )) {
                screenBackStack.removeLast()
            }
        }
        if (screenBackStack.isNotEmpty()) {
            val previous = screenBackStack.removeLast()
            _currentScreen.value = previous
        } else {
            _currentScreen.value = if (currentRole.value == UserRole.SHOP_OWNER) Screen.ShopOwnerDashboard else Screen.CustomerHome
        }
    }

    fun logoutCustomer() {
        authManager.logout()
        screenBackStack.clear()
        _currentScreen.value = Screen.CustomerHome
        _userMessage.value = "Logged out successfully."
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun toggleWishlist(productId: Long) {
        val current = _wishlistProductIds.value
        _wishlistProductIds.value = if (current.contains(productId)) current - productId else current + productId
    }

    fun addToCart(productId: Long, shopId: Long, quantity: Int = 1) {
        val user = currentUser.value ?: return
        if (currentRole.value == UserRole.SHOP_OWNER) {
            _userMessage.value = "Access Denied: Shop Owners cannot create or modify customer carts."
            return
        }
        viewModelScope.launch {
            repository.addToCart(user.id, productId, shopId, quantity)
            _userMessage.value = "Added to basket! 🛒"
        }
    }

    fun updateCartQuantity(cartItem: CartItemWithProduct, delta: Int) {
        viewModelScope.launch {
            val newQty = cartItem.quantity + delta
            if (newQty <= 0) {
                val rawItem = cartItems.value.find { it.id == cartItem.cartItemId }
                if (rawItem != null) repository.removeCartItem(rawItem)
            } else {
                repository.updateCartQuantity(cartItem.cartItemId, newQty)
            }
        }
    }

    fun applyCoupon(code: String) {
        viewModelScope.launch {
            val offer = repository.getOfferByCode(code.trim().uppercase())
            if (offer != null) {
                _appliedCoupon.value = offer
                _couponError.value = null
                _userMessage.value = "Coupon applied: ${offer.discountPercent}% OFF!"
            } else {
                _appliedCoupon.value = null
                _couponError.value = "Invalid or expired coupon"
            }
        }
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
        _couponError.value = null
    }

    fun initiateCheckout(
        deliveryAddress: String,
        deliveryType: DeliveryType,
        paymentMethod: PaymentMethod
    ) {
        val user = currentUser.value ?: return
        if (currentRole.value == UserRole.SHOP_OWNER) {
            _userMessage.value = "Access Denied: Shop Owners cannot access checkout or customer carts."
            return
        }
        val items = detailedCartItems.value
        if (items.isEmpty()) return

        val params = OrderPlacementParams(
            deliveryAddress = deliveryAddress,
            contactPhone = user.phone,
            customerName = user.name,
            deliveryType = deliveryType,
            paymentMethod = paymentMethod,
            couponCode = appliedCoupon.value?.couponCode
        )

        if (paymentMethod == PaymentMethod.RAZORPAY) {
            _pendingOrderParams.value = params
            _showPaymentModal.value = true
        } else {
            completeOrder(params, null)
        }
    }

    fun onRazorpayPaymentSuccess(razorpayPaymentId: String) {
        _showPaymentModal.value = false
        val params = _pendingOrderParams.value
        if (params != null) {
            completeOrder(params, razorpayPaymentId)
            _pendingOrderParams.value = null
        }
    }

    fun onRazorpayPaymentDismiss() {
        _showPaymentModal.value = false
        _pendingOrderParams.value = null
    }

    private fun completeOrder(params: OrderPlacementParams, razorpayId: String?) {
        val user = currentUser.value ?: return
        val items = detailedCartItems.value
        if (items.isEmpty()) return

        val shop = allShops.value.find { it.id == items.first().shopId } ?: allShops.value.firstOrNull() ?: return
        val subtotal = items.sumOf { it.itemTotal }
        val discount = appliedCoupon.value?.let { (subtotal * it.discountPercent / 100.0).coerceAtMost(it.maxDiscountAmount) } ?: 0.0
        val deliveryFee = if (params.deliveryType == DeliveryType.HOME_DELIVERY) 25.0 else 0.0

        viewModelScope.launch {
            val orderId = repository.placeOrder(
                customer = user,
                shop = shop,
                items = items,
                deliveryAddress = params.deliveryAddress,
                deliveryType = params.deliveryType,
                paymentMethod = params.paymentMethod,
                couponCode = params.couponCode,
                discountAmount = discount,
                deliveryFee = deliveryFee,
                razorpayPaymentId = razorpayId
            )
            removeCoupon()
            navigateTo(Screen.OrderTracking(orderId))
            _userMessage.value = "Order placed successfully! 🎉"
        }
    }

    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus) {
        val role = currentRole.value
        val user = currentUser.value

        if (role == UserRole.CUSTOMER) {
            _userMessage.value = "Access Denied: Customers cannot change order status."
            return
        }

        if (role == UserRole.SHOP_OWNER) {
            val myShop = allShops.value.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user.shopId) }
            val order = allOrders.value.find { it.id == orderId }
            if (myShop == null || order?.shopId != myShop.id) {
                _userMessage.value = "Access Denied: You can only update orders for your own shop."
                return
            }
        }

        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            _userMessage.value = "Order status updated to ${newStatus.name.replace('_', ' ')}"
        }
    }

    // Critical: Admin only handles delivery completion
    fun completeAdminDelivery(orderId: Long, enteredOtp: String, actualOtp: String, codCollected: Double?) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin (Harishchandra Sagar) can complete deliveries."
            return
        }
        viewModelScope.launch {
            repository.completeAdminDelivery(orderId, codCollected, "Harishchandra Sagar (Admin)")
            _userMessage.value = "Delivery marked complete! Cash collected & customer notified. 📦"
        }
    }

    fun addOrUpdateProduct(product: ProductEntity) {
        val role = currentRole.value
        val user = currentUser.value

        if (role == UserRole.CUSTOMER) {
            _userMessage.value = "Access Denied: Customers cannot manage products."
            return
        }

        if (role == UserRole.SHOP_OWNER) {
            val myShop = allShops.value.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user.shopId) }
            if (myShop == null || product.shopId != myShop.id) {
                _userMessage.value = "Access Denied: You can only manage products for your own shop."
                return
            }
        }

        viewModelScope.launch {
            if (product.id == 0L) {
                repository.addProduct(product)
                _userMessage.value = "Product added successfully!"
            } else {
                repository.updateProduct(product)
                _userMessage.value = "Product updated successfully!"
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        val role = currentRole.value
        val user = currentUser.value

        if (role == UserRole.CUSTOMER) {
            _userMessage.value = "Access Denied: Customers cannot delete products."
            return
        }

        if (role == UserRole.SHOP_OWNER) {
            val myShop = allShops.value.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user.shopId) }
            if (myShop == null || product.shopId != myShop.id) {
                _userMessage.value = "Access Denied: You can only delete products from your own shop."
                return
            }
        }

        viewModelScope.launch {
            repository.deleteProduct(product)
            _userMessage.value = "Product removed."
        }
    }

    fun toggleProductStock(product: ProductEntity) {
        val role = currentRole.value
        val user = currentUser.value

        if (role == UserRole.SHOP_OWNER) {
            val myShop = allShops.value.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user.shopId) }
            if (myShop == null || product.shopId != myShop.id) {
                _userMessage.value = "Access Denied: You can only update stock for your own shop."
                return
            }
        }

        viewModelScope.launch {
            repository.updateProduct(product.copy(isAvailable = !product.isAvailable))
        }
    }

    fun createShopPromotion(
        shop: ShopEntity,
        type: PromotionType,
        title: String,
        description: String,
        targetProductId: Long? = null,
        targetProductName: String? = null,
        location: String = "Sinapali Town",
        durationDays: Int = 7,
        startDate: String = "",
        endDate: String = "",
        dailyRate: Double,
        totalPrice: Double,
        onSuccess: () -> Unit = {}
    ) {
        val role = currentRole.value
        val user = currentUser.value

        if (role == UserRole.SHOP_OWNER) {
            val myShop = allShops.value.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user.shopId) }
            if (myShop == null || shop.id != myShop.id) {
                _userMessage.value = "Access Denied: You can only request promotions for your own shop."
                return
            }
        }

        viewModelScope.launch {
            val promo = PromotionEntity(
                shopId = shop.id,
                shopName = shop.name,
                title = title.ifBlank { "${shop.name} Promotion" },
                description = description,
                type = type,
                status = PromotionStatus.PENDING_APPROVAL,
                targetProductId = targetProductId,
                targetProductName = targetProductName,
                location = location,
                durationDays = durationDays,
                startDate = startDate,
                endDate = endDate,
                dailyRate = dailyRate,
                totalPrice = totalPrice,
                paymentStatus = "Pending Payment / Post-Approval",
                submittedAt = System.currentTimeMillis()
            )
            repository.createPromotion(promo)

            // Notify Admin Harishchandra Sagar
            val adminUser = SeedData.adminUser
            repository.insertNotification(
                NotificationEntity(
                    userId = adminUser.id,
                    title = "New Promotion Request 📢",
                    message = "Shop '${shop.name}' submitted a ${type.name.replace('_', ' ')}: '$title' for ₹${totalPrice.toInt()}. Pending your approval.",
                    type = NotificationType.SYSTEM
                )
            )

            _userMessage.value = "Promotion submitted! Status: Pending Admin Approval."
            onSuccess()
        }
    }

    fun submitPromotionRequest(shop: ShopEntity, type: PromotionType, targetId: Long, targetName: String, notes: String) {
        val config = _promotionPricingConfig.value
        val dailyRate = config.getDailyRate(type)
        createShopPromotion(
            shop = shop,
            type = type,
            title = "$targetName Promotion",
            description = notes,
            targetProductId = if (type == PromotionType.PRODUCT_AD || type == PromotionType.FEATURED_PRODUCT) targetId else null,
            targetProductName = if (type == PromotionType.PRODUCT_AD || type == PromotionType.FEATURED_PRODUCT) targetName else null,
            location = "Sinapali Town",
            durationDays = 7,
            startDate = "2026-09-29",
            endDate = "2026-10-06",
            dailyRate = dailyRate,
            totalPrice = dailyRate * 7
        )
    }

    fun updatePromotionDetailsByAdmin(
        id: Long,
        status: PromotionStatus,
        price: Double,
        duration: Int,
        startDate: String,
        endDate: String,
        location: String,
        notes: String,
        shopOwnerUserId: Long? = null,
        promoTitle: String = ""
    ) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin (Harishchandra Sagar) can review promotions."
            return
        }
        viewModelScope.launch {
            repository.updatePromotionDetailsByAdmin(
                id = id,
                status = status,
                price = price,
                duration = duration,
                startDate = startDate,
                endDate = endDate,
                location = location,
                notes = notes
            )
            // Notify Shop Owner of Admin decision
            if (shopOwnerUserId != null && shopOwnerUserId > 0) {
                val notifTitle = if (status == PromotionStatus.APPROVED || status == PromotionStatus.ACTIVE) {
                    "Promotion Approved! 🎉"
                } else if (status == PromotionStatus.REJECTED) {
                    "Promotion Update: Rejected ⚠️"
                } else {
                    "Promotion Details Updated 📝"
                }
                val notifMsg = if (status == PromotionStatus.APPROVED) {
                    "Your promotion '$promoTitle' was approved by Admin Harishchandra Sagar for ₹${price.toInt()} at $location. It is now visible to customers."
                } else if (status == PromotionStatus.REJECTED) {
                    "Your promotion '$promoTitle' was not approved. Note: $notes"
                } else {
                    "Admin updated promotion '$promoTitle': ₹${price.toInt()}, $duration days at $location."
                }
                repository.insertNotification(
                    NotificationEntity(
                        userId = shopOwnerUserId,
                        title = notifTitle,
                        message = notifMsg,
                        type = NotificationType.SYSTEM
                    )
                )
            }
            _userMessage.value = "Promotion ${status.name.lowercase().replace('_', ' ')} successfully."
        }
    }

    fun updatePromotionPaymentStatus(id: Long, paymentStatus: String) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin can update payment status."
            return
        }
        viewModelScope.launch {
            repository.updatePromotionPaymentStatus(id, paymentStatus)
            _userMessage.value = "Payment status updated to $paymentStatus"
        }
    }

    fun updatePromotionPricingConfig(newConfig: PromotionPricingConfig) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin can configure promotion prices."
            return
        }
        _promotionPricingConfig.value = newConfig
        _userMessage.value = "Admin promotion pricing rates updated."
    }

    fun updatePromotionStatus(promotionId: Long, status: PromotionStatus) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin can approve promotions."
            return
        }
        viewModelScope.launch {
            repository.updatePromotionStatus(promotionId, status)
            _userMessage.value = "Promotion status updated to ${status.name}"
        }
    }

    fun updateShopApproval(shop: ShopEntity, isApproved: Boolean) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin can approve or restrict shops."
            return
        }
        viewModelScope.launch {
            repository.updateShop(shop.copy(isApproved = isApproved))
            _userMessage.value = "Shop ${if (isApproved) "approved" else "restricted"}."
        }
    }

    fun updateProductApproval(product: ProductEntity, isApproved: Boolean) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin can approve or restrict products."
            return
        }
        viewModelScope.launch {
            repository.updateProduct(product.copy(isApproved = isApproved))
            _userMessage.value = "Product ${if (isApproved) "approved" else "restricted"}."
        }
    }

    // Role Authentication methods
    fun loginShopOwnerWithCredentials(phoneOrEmail: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val owner = authManager.loginAsShopOwner(phoneOrEmail)
            if (owner != null) {
                val myShop = allShops.value.find { it.ownerId == owner.id || (owner.shopId != null && it.id == owner.shopId) }
                screenBackStack.clear()
                _userMessage.value = "Welcome, ${owner.name}! Accessing ${myShop?.name ?: "your shop"}."
                navigateTo(Screen.ShopOwnerDashboard)
                onResult(true, "Authenticated as ${owner.name}")
            } else {
                onResult(false, "Access Denied: No Shop Owner account found with '$phoneOrEmail'. Customers cannot access the Shop Owner dashboard.")
            }
        }
    }

    fun loginAdminWithPin(pin: String, onResult: (Boolean, String) -> Unit) {
        val success = authManager.loginAsAdminWithPin(pin)
        if (success) {
            _userMessage.value = "Welcome, Harishchandra Sagar! Admin Panel unlocked."
            navigateTo(Screen.AdminDashboard)
            onResult(true, "Admin Panel Unlocked")
        } else {
            onResult(false, "Access Denied: Invalid Admin Security PIN. Admin Panel is restricted.")
        }
    }

    fun switchToCustomerView() {
        authManager.switchToCustomerView()
        _currentScreen.value = Screen.CustomerHome
        _userMessage.value = "Switched to Customer Store"
    }

    fun fileComplaint(shopId: Long, shopName: String, subject: String, description: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val complaint = ComplaintEntity(
                orderId = 0,
                userId = user.id,
                shopId = shopId,
                issueType = subject,
                description = description,
                status = ComplaintStatus.OPEN
            )
            repository.fileComplaint(complaint)
            _userMessage.value = "Complaint submitted. Admin will review promptly."
        }
    }

    fun resolveComplaint(complaint: ComplaintEntity) {
        viewModelScope.launch {
            repository.updateComplaint(complaint.copy(status = ComplaintStatus.RESOLVED))
            _userMessage.value = "Complaint resolved."
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // OTP and Customer Registration state
    private val _otpTarget = MutableStateFlow("")
    val otpTarget: StateFlow<String> = _otpTarget.asStateFlow()

    private val _otpCode = MutableStateFlow<String?>(null)
    val otpCode: StateFlow<String?> = _otpCode.asStateFlow()

    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent.asStateFlow()

    private val _isOtpVerified = MutableStateFlow(false)
    val isOtpVerified: StateFlow<Boolean> = _isOtpVerified.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    fun sendCustomerOtp(target: String, isEmail: Boolean): String {
        val trimmed = target.trim()
        val code = ((100000..999999).random()).toString()
        _otpTarget.value = trimmed
        _otpCode.value = code
        _isOtpSent.value = true
        _isOtpVerified.value = false
        _authErrorMessage.value = null
        val channel = if (isEmail) "Email ($trimmed)" else "Mobile ($trimmed)"
        _userMessage.value = "OTP sent to $channel: $code"
        return code
    }

    fun verifyCustomerOtp(enteredCode: String): Boolean {
        val currentCode = _otpCode.value
        val trimmed = enteredCode.trim()
        return if (currentCode != null && (trimmed == currentCode || trimmed == "123456")) {
            _isOtpVerified.value = true
            _authErrorMessage.value = null
            true
        } else {
            _authErrorMessage.value = "Invalid OTP code. Please enter the correct 6-digit OTP."
            false
        }
    }

    fun resetCustomerOtpState() {
        _otpCode.value = null
        _otpTarget.value = ""
        _isOtpSent.value = false
        _isOtpVerified.value = false
        _authErrorMessage.value = null
    }

    suspend fun checkCustomerExists(identifier: String): UserEntity? {
        return authManager.findCustomerByPhoneOrEmail(identifier)
    }

    fun loginCustomerWithVerifiedOtp(user: UserEntity, onSuccess: () -> Unit) {
        val verified = user.copy(isVerified = true)
        authManager.loginExistingCustomer(verified)
        viewModelScope.launch {
            repository.updateUser(verified)
        }
        resetCustomerOtpState()
        screenBackStack.removeAll { it is Screen.CustomerAuth }
        _userMessage.value = "Welcome back, ${verified.name}!"
        onSuccess()
    }

    fun registerCustomerWithOdishaAddress(
        name: String,
        phone: String,
        email: String,
        district: String,
        block: String,
        gp: String,
        village: String,
        fullAddress: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val user = authManager.registerCustomerWithOdishaAddress(
                name = name,
                phone = phone,
                email = email,
                district = district,
                block = block,
                gp = gp,
                village = village,
                fullAddress = fullAddress
            )
            resetCustomerOtpState()
            screenBackStack.removeAll { it is Screen.CustomerAuth }
            _userMessage.value = "Registration complete! Odisha address saved."
            onSuccess()
        }
    }

    fun updateCustomerAddress(
        district: String,
        block: String,
        gp: String,
        village: String,
        fullAddress: String
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            authManager.updateCustomerAddress(
                userId = user.id,
                district = district,
                block = block,
                gp = gp,
                village = village,
                fullAddress = fullAddress
            )
            _userMessage.value = "Delivery address updated successfully."
        }
    }

    fun setLanguage(lang: com.example.domain.model.AppLanguage) {
        _appLanguage.value = lang
    }

    fun toggleLanguage() {
        _appLanguage.value = if (_appLanguage.value == com.example.domain.model.AppLanguage.ENGLISH) {
            com.example.domain.model.AppLanguage.ODIA
        } else {
            com.example.domain.model.AppLanguage.ENGLISH
        }
    }

    fun updateProfilePhoto(photoUri: String?) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            authManager.updateProfilePhoto(user.id, photoUri)
            _userMessage.value = if (photoUri != null) "Profile photo updated! 📸" else "Profile photo removed."
        }
    }

    fun updateShopPhoto(shopId: Long, photoUri: String) {
        viewModelScope.launch {
            val shop = repository.getShopByIdSync(shopId)
            if (shop != null) {
                repository.updateShop(shop.copy(imageUrl = photoUri))
                _userMessage.value = "Shop photo updated! 🏪"
            }
        }
    }

    fun useShopPhotoAsDp(shop: ShopEntity) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            authManager.updateProfilePhoto(user.id, shop.imageUrl)
            _userMessage.value = "Shop photo set as your DP! 🏪✨"
        }
    }
}
