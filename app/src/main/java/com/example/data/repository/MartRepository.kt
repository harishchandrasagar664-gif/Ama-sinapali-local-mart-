package com.example.data.repository

import com.example.data.local.SeedData
import com.example.data.local.dao.MartDao
import com.example.data.local.entities.*
import com.example.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MartRepository(private val dao: MartDao) {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedDatabaseIfEmpty()
        }
    }

    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val existingUsers = dao.getAllUsers().first()
        if (existingUsers.isEmpty()) {
            dao.insertUser(SeedData.adminUser)
            dao.insertAllUsers(SeedData.shopOwners)
            dao.insertAllUsers(SeedData.customers)
            dao.insertAllShops(SeedData.shops)
            dao.insertCategories(SeedData.categories)
            dao.insertAllProducts(SeedData.products)
            dao.insertAddresses(SeedData.sampleAddresses)
            dao.insertAllOffers(SeedData.sampleOffers)
            dao.insertBanners(SeedData.sampleBanners)
            dao.insertAllOrders(SeedData.sampleOrders)
            dao.insertOrderItems(SeedData.sampleOrderItems)
            dao.insertReviews(SeedData.sampleReviews)
            dao.insertNotifications(SeedData.sampleNotifications)
        } else {
            // Ensure full category list and new products are always available
            dao.insertCategories(SeedData.categories)
            val existingProducts = dao.getAllProducts().first()
            if (existingProducts.size < SeedData.products.size) {
                dao.insertAllProducts(SeedData.products)
            }
        }
    }

    fun getAllUsers(): Flow<List<UserEntity>> = dao.getAllUsers()
    suspend fun getUserByPhone(phone: String): UserEntity? = dao.getUserByPhone(phone)
    suspend fun getUserByEmail(email: String): UserEntity? = dao.getUserByEmail(email)
    suspend fun getUserByPhoneOrEmail(identifier: String): UserEntity? = dao.getUserByPhoneOrEmail(identifier)
    suspend fun getUserById(id: Long): UserEntity? = dao.getUserById(id)
    suspend fun registerUser(user: UserEntity): Long = dao.insertUser(user)
    suspend fun insertUser(user: UserEntity): Long = dao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = dao.updateUser(user)

    fun getActiveShops(): Flow<List<ShopEntity>> = dao.getActiveShops()
    fun getAllShops(): Flow<List<ShopEntity>> = dao.getAllShops()
    fun getShopById(shopId: Long): Flow<ShopEntity?> = dao.getShopById(shopId)
    suspend fun getShopByIdSync(shopId: Long): ShopEntity? = dao.getShopByIdSync(shopId)
    suspend fun getShopByOwnerId(ownerId: Long): ShopEntity? = dao.getShopByOwnerId(ownerId)
    fun getShopFlowByOwnerId(ownerId: Long): Flow<ShopEntity?> = dao.getShopFlowByOwnerId(ownerId)
    suspend fun createShop(shop: ShopEntity): Long = dao.insertShop(shop)
    suspend fun updateShop(shop: ShopEntity) = dao.updateShop(shop)

    fun getActiveProducts(): Flow<List<ProductEntity>> = dao.getActiveProducts()
    fun getAllProducts(): Flow<List<ProductEntity>> = dao.getAllProducts()
    fun getProductsByShop(shopId: Long): Flow<List<ProductEntity>> = dao.getProductsByShop(shopId)
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> = dao.getProductsByCategory(category)
    fun getProductById(productId: Long): Flow<ProductEntity?> = dao.getProductById(productId)
    suspend fun getProductByIdSync(productId: Long): ProductEntity? = dao.getProductByIdSync(productId)
    suspend fun addProduct(product: ProductEntity): Long = dao.insertProduct(product)
    suspend fun updateProduct(product: ProductEntity) = dao.updateProduct(product)
    suspend fun deleteProduct(product: ProductEntity) = dao.deleteProduct(product)

    fun getAllCategories(): Flow<List<CategoryEntity>> = dao.getAllCategories()

    fun getCartItems(customerId: Long): Flow<List<CartItemEntity>> = dao.getCartItems(customerId)
    suspend fun addToCart(userId: Long, productId: Long, shopId: Long, quantity: Int = 1) = withContext(Dispatchers.IO) {
        val existing = dao.getCartItemByProduct(userId, productId)
        if (existing != null) {
            dao.updateCartQuantity(existing.id, existing.quantity + quantity)
        } else {
            dao.insertCartItem(
                CartItemEntity(
                    userId = userId,
                    productId = productId,
                    shopId = shopId,
                    quantity = quantity
                )
            )
        }
    }
    suspend fun updateCartQuantity(cartItemId: Long, newQuantity: Int) = withContext(Dispatchers.IO) {
        if (newQuantity <= 0) {
            // Delete if zero
        } else {
            dao.updateCartQuantity(cartItemId, newQuantity)
        }
    }
    suspend fun removeCartItem(cartItem: CartItemEntity) = dao.deleteCartItem(cartItem)
    suspend fun clearCart(customerId: Long) = dao.clearCart(customerId)

    fun getAllOrders(): Flow<List<OrderEntity>> = dao.getAllOrders()
    fun getOrdersByCustomer(customerId: Long): Flow<List<OrderEntity>> = dao.getOrdersByCustomer(customerId)
    fun getOrdersByShop(shopId: Long): Flow<List<OrderEntity>> = dao.getOrdersByShop(shopId)
    fun getOrderById(orderId: Long): Flow<OrderEntity?> = dao.getOrderById(orderId)
    suspend fun getOrderByIdSync(orderId: Long): OrderEntity? = dao.getOrderByIdSync(orderId)
    fun getOrderItems(orderId: Long): Flow<List<OrderItemEntity>> = dao.getOrderItems(orderId)

    suspend fun placeOrder(
        customer: UserEntity,
        shop: ShopEntity,
        items: List<CartItemWithProduct>,
        deliveryAddress: String,
        deliveryType: DeliveryType,
        paymentMethod: PaymentMethod,
        couponCode: String?,
        discountAmount: Double,
        deliveryFee: Double,
        razorpayPaymentId: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val subtotal = items.sumOf { it.itemTotal }
        val finalAmount = (subtotal - discountAmount + deliveryFee).coerceAtLeast(0.0)
        val orderNumber = "ORD-SIN-${System.currentTimeMillis() % 100000}"

        val order = OrderEntity(
            orderNumber = orderNumber,
            userId = customer.id,
            shopId = shop.id,
            totalAmount = subtotal,
            discountAmount = discountAmount,
            deliveryFee = deliveryFee,
            finalAmount = finalAmount,
            status = OrderStatus.PLACED,
            deliveryType = deliveryType,
            paymentMethod = paymentMethod,
            paymentStatus = if (paymentMethod == PaymentMethod.CASH_ON_DELIVERY) PaymentStatus.PENDING else PaymentStatus.PAID,
            deliveryAddress = deliveryAddress,
            customerPhone = customer.phone,
            customerName = customer.name,
            placedAt = System.currentTimeMillis()
        )
        val orderId = dao.insertOrder(order)

        val orderItems = items.map {
            OrderItemEntity(
                orderId = orderId,
                productId = it.productId,
                productName = it.productName,
                unitPrice = it.effectivePrice,
                quantity = it.quantity,
                totalPrice = it.itemTotal
            )
        }
        dao.insertOrderItems(orderItems)
        dao.clearCart(customer.id)

        // Notification for Shop Owner
        dao.insertNotification(
            NotificationEntity(
                userId = shop.ownerId,
                title = "New Order #$orderNumber Received! 🛍️",
                message = "New order of ₹$finalAmount from ${customer.name}. Check & prepare items.",
                type = NotificationType.ORDER_STATUS
            )
        )

        orderId
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: OrderStatus) = withContext(Dispatchers.IO) {
        dao.updateOrderStatus(orderId, newStatus)
        val order = dao.getOrderByIdSync(orderId)
        if (order != null) {
            dao.insertNotification(
                NotificationEntity(
                    userId = order.userId,
                    title = "Order Status Updated 🚚",
                    message = "Your order #${order.orderNumber} is now ${newStatus.name.replace('_', ' ')}.",
                    type = NotificationType.ORDER_STATUS
                )
            )
        }
    }

    // Critical Rule: Only Admin can complete delivery
    suspend fun completeAdminDelivery(
        orderId: Long,
        codCollectedAmount: Double?,
        deliveredBy: String
    ) = withContext(Dispatchers.IO) {
        val order = dao.getOrderByIdSync(orderId)
        val paymentStatus = if (order?.paymentMethod == PaymentMethod.CASH_ON_DELIVERY) {
            PaymentStatus.PAID
        } else {
            order?.paymentStatus ?: PaymentStatus.PAID
        }

        dao.completeDeliveryOrder(
            orderId = orderId,
            status = OrderStatus.DELIVERED,
            paymentStatus = paymentStatus,
            deliveredBy = deliveredBy
        )

        if (order != null) {
            // Customer notification
            dao.insertNotification(
                NotificationEntity(
                    userId = order.userId,
                    title = "Order Delivered! 📦🎉",
                    message = "Your order #${order.orderNumber} has been delivered successfully by Admin ($deliveredBy).",
                    type = NotificationType.ORDER_STATUS
                )
            )
            // Shop owner notification
            dao.insertNotification(
                NotificationEntity(
                    userId = order.shopId,
                    title = "Order #${order.orderNumber} Delivered",
                    message = "Admin has completed delivery for order #${order.orderNumber}. Amount: ₹${order.finalAmount}",
                    type = NotificationType.ORDER_STATUS
                )
            )
        }
    }

    fun getAddressesByCustomer(customerId: Long): Flow<List<AddressEntity>> = dao.getAddressesByCustomer(customerId)
    suspend fun getDefaultAddressForUser(userId: Long): AddressEntity? = dao.getDefaultAddressForUser(userId)
    suspend fun addAddress(address: AddressEntity): Long = dao.insertAddress(address)
    suspend fun updateAddress(address: AddressEntity) = dao.updateAddress(address)

    fun getActiveOffers(): Flow<List<OfferEntity>> = dao.getActiveOffers()
    suspend fun getOfferByCode(code: String): OfferEntity? = dao.getOfferByCode(code)
    suspend fun addOffer(offer: OfferEntity): Long = dao.insertOffer(offer)

    fun getAllPromotions(): Flow<List<PromotionEntity>> = dao.getAllPromotions()
    fun getPromotionsByShop(shopId: Long): Flow<List<PromotionEntity>> = dao.getPromotionsByShop(shopId)
    suspend fun createPromotion(promotion: PromotionEntity): Long = dao.insertPromotion(promotion)
    suspend fun updatePromotionStatus(id: Long, status: PromotionStatus) = dao.updatePromotionStatus(id, status)

    fun getActiveBanners(): Flow<List<BannerEntity>> = dao.getActiveBanners()

    fun getReviewsForShop(shopId: Long): Flow<List<ReviewEntity>> = dao.getReviewsForShop(shopId)
    suspend fun addReview(review: ReviewEntity): Long = dao.insertReview(review)

    fun getNotifications(userId: Long): Flow<List<NotificationEntity>> = dao.getNotifications(userId)
    fun getAllNotifications(): Flow<List<NotificationEntity>> = dao.getAllNotifications()
    suspend fun markNotificationRead(id: Long) = dao.markNotificationRead(id)

    fun getAllPayments(): Flow<List<PaymentRecordEntity>> = dao.getAllPayments()

    fun getAllComplaints(): Flow<List<ComplaintEntity>> = dao.getAllComplaints()
    suspend fun fileComplaint(complaint: ComplaintEntity): Long = dao.insertComplaint(complaint)
    suspend fun updateComplaint(complaint: ComplaintEntity) = dao.updateComplaint(complaint)
}
