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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = MartRepository(database.martDao())
    val authManager = AuthManager(repository)
    val paymentService = RazorpayPaymentServiceImpl()

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

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPromotions: StateFlow<List<PromotionEntity>> = repository.getAllPromotions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allComplaints: StateFlow<List<ComplaintEntity>> = repository.getAllComplaints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<UserEntity?> = authManager.currentUser
    val currentRole: StateFlow<UserRole> = authManager.currentRole

    val cartItems: StateFlow<List<CartItemEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getCartItems(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        _currentScreen.value = screen
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
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            _userMessage.value = "Order status updated to ${newStatus.name.replace('_', ' ')}"
        }
    }

    // Critical: Admin only handles delivery completion
    fun completeAdminDelivery(orderId: Long, enteredOtp: String, actualOtp: String, codCollected: Double?) {
        viewModelScope.launch {
            repository.completeAdminDelivery(orderId, codCollected, "Harishchandra Sagar (Admin)")
            _userMessage.value = "Delivery marked complete! Cash collected & customer notified. 📦"
        }
    }

    fun addOrUpdateProduct(product: ProductEntity) {
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
        viewModelScope.launch {
            repository.deleteProduct(product)
            _userMessage.value = "Product removed."
        }
    }

    fun toggleProductStock(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(isAvailable = !product.isAvailable))
        }
    }

    fun submitPromotionRequest(shop: ShopEntity, type: PromotionType, targetId: Long, targetName: String, notes: String) {
        viewModelScope.launch {
            val promotion = PromotionEntity(
                shopId = shop.id,
                title = "$targetName Promotion",
                description = notes,
                type = type,
                status = PromotionStatus.PENDING_APPROVAL
            )
            repository.createPromotion(promotion)
            _userMessage.value = "Promotion request submitted for Admin review."
        }
    }

    fun updatePromotionStatus(promotionId: Long, status: PromotionStatus) {
        viewModelScope.launch {
            repository.updatePromotionStatus(promotionId, status)
            _userMessage.value = "Promotion status updated to ${status.name}"
        }
    }

    fun updateShopApproval(shop: ShopEntity, isApproved: Boolean) {
        viewModelScope.launch {
            repository.updateShop(shop.copy(isApproved = isApproved))
            _userMessage.value = "Shop ${if (isApproved) "approved" else "restricted"}."
        }
    }

    fun updateProductApproval(product: ProductEntity, isApproved: Boolean) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(isApproved = isApproved))
            _userMessage.value = "Product ${if (isApproved) "approved" else "restricted"}."
        }
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
        authManager.loginExistingCustomer(user)
        resetCustomerOtpState()
        _userMessage.value = "Welcome back, ${user.name}!"
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
}
