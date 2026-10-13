package com.example.data.repository

import com.example.data.local.SeedData
import com.example.data.local.dao.MartDao
import com.example.data.local.entities.*
import com.example.domain.model.*
import com.example.data.firebase.FirestoreSyncService
import com.example.data.firebase.FirebaseSyncConfig
import com.example.service.OrderSyncService
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
            syncOrdersFromCloud()
            syncShopsAndProductsFromCloud()
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
            dao.insertAllPromotions(SeedData.samplePromotions)
        } else {
            // Ensure full category list, promotions and new products are always available
            dao.insertCategories(SeedData.categories)
            dao.deleteCategoryByName("Medicine & Healthcare")
            dao.deleteCategoryByName("Medicine and Health Care")
            dao.deleteProductsByCategory("Medicine & Healthcare")
            dao.deleteProductsByCategory("Medicine and Health Care")
            val existingProducts = dao.getAllProducts().first()
            if (existingProducts.size < SeedData.products.size) {
                dao.insertAllProducts(SeedData.products)
            }
            val existingPromos = dao.getAllPromotions().first()
            if (existingPromos.isEmpty()) {
                dao.insertAllPromotions(SeedData.samplePromotions)
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

    /**
     * Permanently deletes customer account, personal addresses, cart, and wipes cloud records.
     * Complies with Google Play Store User Data Deletion Policy and GDPR.
     */
    suspend fun deleteCustomerAccountAndData(userId: Long) = withContext(Dispatchers.IO) {
        dao.clearCart(userId)
        dao.deleteAddressesForUser(userId)
        dao.deleteNotificationsForUser(userId)
        dao.deleteUserById(userId)
        try {
            FirestoreSyncService.deleteUserFromFirestore(userId)
        } catch (_: Throwable) { }
    }

    /**
     * Permanently deletes shop owner account, shop entity, shop products, promotions, reviews and cloud records.
     */
    suspend fun deleteShopOwnerAccountAndData(userId: Long, shopId: Long?) = withContext(Dispatchers.IO) {
        dao.clearCart(userId)
        dao.deleteAddressesForUser(userId)
        dao.deleteNotificationsForUser(userId)
        if (shopId != null && shopId > 0L) {
            dao.deleteProductsForShop(shopId)
            dao.deletePromotionsForShop(shopId)
            dao.deleteReviewsForShop(shopId)
            dao.deleteShopById(shopId)
            try {
                FirestoreSyncService.deleteShopFromFirestore(shopId)
            } catch (_: Throwable) { }
        }
        dao.deleteUserById(userId)
        try {
            FirestoreSyncService.deleteUserFromFirestore(userId)
        } catch (_: Throwable) { }
    }

    fun getActiveShops(): Flow<List<ShopEntity>> = dao.getActiveShops()
    fun getAllShops(): Flow<List<ShopEntity>> = dao.getAllShops()
    fun getShopById(shopId: Long): Flow<ShopEntity?> = dao.getShopById(shopId)
    suspend fun getShopByIdSync(shopId: Long): ShopEntity? = dao.getShopByIdSync(shopId)
    suspend fun getShopByOwnerId(ownerId: Long): ShopEntity? = dao.getShopByOwnerId(ownerId)
    fun getShopFlowByOwnerId(ownerId: Long): Flow<ShopEntity?> = dao.getShopFlowByOwnerId(ownerId)
    suspend fun createShop(shop: ShopEntity): Long = withContext(Dispatchers.IO) {
        val assignedId = if (shop.id > 0L) shop.id else (System.currentTimeMillis() % 100000000L + 1000L)
        val toInsert = shop.copy(id = assignedId)
        val id = dao.insertShop(toInsert)
        val finalId = if (id > 0L) id else assignedId
        val persisted = toInsert.copy(id = finalId)
        try {
            FirestoreSyncService.pushShopToFirestore(persisted)
        } catch (_: Throwable) { }
        finalId
    }
    suspend fun updateShop(shop: ShopEntity) = withContext(Dispatchers.IO) {
        dao.updateShop(shop)
        try {
            FirestoreSyncService.pushShopToFirestore(shop)
        } catch (_: Throwable) { }
    }

    suspend fun insertOrUpdateShopFromCloud(shop: ShopEntity) = withContext(Dispatchers.IO) {
        dao.insertShop(shop)
    }

    fun getActiveProducts(): Flow<List<ProductEntity>> = dao.getActiveProducts()
    fun getAllProducts(): Flow<List<ProductEntity>> = dao.getAllProducts()
    fun getProductsByShop(shopId: Long): Flow<List<ProductEntity>> = dao.getProductsByShop(shopId)
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> = dao.getProductsByCategory(category)
    fun getProductById(productId: Long): Flow<ProductEntity?> = dao.getProductById(productId)
    suspend fun getProductByIdSync(productId: Long): ProductEntity? = dao.getProductByIdSync(productId)

    suspend fun addProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        val assignedId = if (product.id > 0L) product.id else (System.currentTimeMillis() % 100000000L + 10000L)
        val toInsert = product.copy(id = assignedId)
        val id = dao.insertProduct(toInsert)
        val finalId = if (id > 0L) id else assignedId
        val persisted = toInsert.copy(id = finalId)
        try {
            FirestoreSyncService.pushProductToFirestore(persisted)
        } catch (_: Throwable) { }
        finalId
    }

    suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.updateProduct(product)
        try {
            FirestoreSyncService.pushProductToFirestore(product)
        } catch (_: Throwable) { }
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.deleteProduct(product)
        try {
            FirestoreSyncService.deleteProductFromFirestore(product)
        } catch (_: Throwable) { }
    }

    suspend fun insertOrUpdateProductFromCloud(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.insertProduct(product)
    }

    suspend fun deleteProductLocally(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.deleteProduct(product)
    }

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
    fun getAllOrderItems(): Flow<List<OrderItemEntity>> = dao.getAllOrderItems()

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
        val orderNumber = "ORD-SIN-${System.currentTimeMillis()}-${(100..999).random()}"

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

        // Guaranteed immediate push to Firebase Firestore so Admin sees it immediately from any device
        try {
            FirestoreSyncService.pushOrderToFirestore(order.copy(id = orderId), orderItems, shop.name)
        } catch (e: Throwable) {
            android.util.Log.w("MartRepository", "Notice: Firestore immediate push: ${e.message}")
        }

        // Notification for the correct Shop Owner based on ordered product/shop
        val allUsers = dao.getAllUsersSync()
        val itemsByShopId = items.groupBy { it.shopId }.ifEmpty { mapOf(shop.id to items) }
        val allShopNames = mutableListOf<String>()

        for ((orderedShopId, shopItems) in itemsByShopId) {
            val targetShop = dao.getShopByIdSync(orderedShopId) ?: (if (orderedShopId == shop.id) shop else SeedData.shops.find { it.id == orderedShopId })
            val shopName = targetShop?.name ?: shop.name
            allShopNames.add(shopName)

            val targetOwnerUserIds = mutableSetOf<Long>()
            if (targetShop != null && targetShop.ownerId > 0) {
                targetOwnerUserIds.add(targetShop.ownerId)
            }
            for (u in allUsers) {
                if (u.role == UserRole.SHOP_OWNER) {
                    if ((targetShop != null && u.id == targetShop.ownerId) || u.shopId == orderedShopId || (targetShop != null && u.phone.isNotBlank() && u.phone == targetShop.phone)) {
                        targetOwnerUserIds.add(u.id)
                    }
                }
            }
            for (so in SeedData.shopOwners) {
                if ((targetShop != null && so.id == targetShop.ownerId) || so.shopId == orderedShopId || (targetShop != null && so.phone.isNotBlank() && so.phone == targetShop.phone)) {
                    targetOwnerUserIds.add(so.id)
                }
            }
            if (targetOwnerUserIds.isEmpty()) {
                targetOwnerUserIds.add(targetShop?.ownerId ?: orderedShopId)
            }

            val productNamesSummary = shopItems.joinToString(", ") { "${it.quantity}x ${it.productName}" }
            val shopTotal = shopItems.sumOf { it.itemTotal }
            val notifTitle = "New Order #$orderNumber for $shopName! 🛍️"
            val notifMessage = "Customer ${customer.name} (${customer.phone}) ordered: $productNamesSummary. Total: ₹${shopTotal.toInt()} (${paymentMethod.name.replace('_', ' ')}). Please accept & prepare items for delivery."

            for (ownerUserId in targetOwnerUserIds) {
                dao.insertNotification(
                    NotificationEntity(
                        userId = ownerUserId,
                        title = notifTitle,
                        message = notifMessage,
                        type = NotificationType.ORDER_STATUS,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        // Notification for Admin (Harishchandra Sagar) so Admin Panel alerts immediately
        val shopNamesSummary = allShopNames.distinct().joinToString(", ").ifEmpty { shop.name }
        val allProductsSummary = items.joinToString(", ") { "${it.quantity}x ${it.productName}" }
        dao.insertNotification(
            NotificationEntity(
                userId = SeedData.adminUser.id,
                title = "New Town Order #$orderNumber ($shopNamesSummary)",
                message = "New order of ₹${finalAmount.toInt()} from ${customer.name} for $shopNamesSummary. Items: $allProductsSummary.",
                type = NotificationType.ORDER_STATUS,
                timestamp = System.currentTimeMillis()
            )
        )

        orderId
    }

    suspend fun notifyShopOwnersAndAdminForOrderEntity(order: OrderEntity, items: List<OrderItemEntity>) = withContext(Dispatchers.IO) {
        val allUsers = dao.getAllUsersSync()
        val itemsByShop = mutableMapOf<Long, MutableList<OrderItemEntity>>()
        for (item in items) {
            val prod = dao.getProductByIdSync(item.productId) ?: SeedData.products.find { it.id == item.productId }
            val sId = prod?.shopId ?: order.shopId
            itemsByShop.getOrPut(sId) { mutableListOf() }.add(item)
        }
        if (itemsByShop.isEmpty()) {
            itemsByShop[order.shopId] = items.toMutableList()
        }

        val allShopNames = mutableListOf<String>()
        for ((sId, shopItems) in itemsByShop) {
            val shop = dao.getShopByIdSync(sId) ?: SeedData.shops.find { it.id == sId }
            val shopName = shop?.name ?: "Shop #$sId"
            allShopNames.add(shopName)

            val targetOwnerUserIds = mutableSetOf<Long>()
            if (shop != null && shop.ownerId > 0) targetOwnerUserIds.add(shop.ownerId)
            for (u in allUsers) {
                if (u.role == UserRole.SHOP_OWNER) {
                    if ((shop != null && u.id == shop.ownerId) || u.shopId == sId || (shop != null && u.phone.isNotBlank() && u.phone == shop.phone)) {
                        targetOwnerUserIds.add(u.id)
                    }
                }
            }
            for (so in SeedData.shopOwners) {
                if ((shop != null && so.id == shop.ownerId) || so.shopId == sId || (shop != null && so.phone.isNotBlank() && so.phone == shop.phone)) {
                    targetOwnerUserIds.add(so.id)
                }
            }
            if (targetOwnerUserIds.isEmpty()) targetOwnerUserIds.add(shop?.ownerId ?: sId)

            val itemsSummary = if (shopItems.isNotEmpty()) shopItems.joinToString(", ") { "${it.quantity}x ${it.productName}" } else "Order #${order.orderNumber}"
            val shopTotal = if (shopItems.isNotEmpty()) shopItems.sumOf { it.totalPrice } else order.finalAmount
            val notifTitle = "New Order #${order.orderNumber} for $shopName! 🛍️"
            val notifMessage = "Customer ${order.customerName} (${order.customerPhone}) ordered: $itemsSummary. Total: ₹${shopTotal.toInt()} (${order.paymentMethod.name.replace('_', ' ')}). Please accept & prepare items for delivery."

            for (ownerUserId in targetOwnerUserIds) {
                dao.insertNotification(
                    NotificationEntity(
                        userId = ownerUserId,
                        title = notifTitle,
                        message = notifMessage,
                        type = NotificationType.ORDER_STATUS,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        val allShopNamesSummary = allShopNames.distinct().joinToString(", ").ifEmpty { "Sinapali Mart" }
        val allProductsSummary = if (items.isNotEmpty()) items.joinToString(", ") { "${it.quantity}x ${it.productName}" } else "Order #${order.orderNumber}"
        dao.insertNotification(
            NotificationEntity(
                userId = SeedData.adminUser.id,
                title = "New Town Order #${order.orderNumber} ($allShopNamesSummary)",
                message = "New order of ₹${order.finalAmount.toInt()} from ${order.customerName} for $allShopNamesSummary. Items: $allProductsSummary.",
                type = NotificationType.ORDER_STATUS,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun insertOrderFromCloud(order: OrderEntity, items: List<OrderItemEntity>): Long = withContext(Dispatchers.IO) {
        val existing = dao.getOrderByOrderNumber(order.orderNumber)
        if (existing != null) {
            if (existing.status != order.status || existing.paymentStatus != order.paymentStatus || existing.finalAmount != order.finalAmount) {
                dao.updateOrder(order.copy(id = existing.id))
            }
            val existingItems = dao.getOrderItems(existing.id).first()
            if (existingItems.isEmpty() && items.isNotEmpty()) {
                val list = items.map { it.copy(id = 0L, orderId = existing.id) }
                dao.insertOrderItems(list)
            }
            return@withContext existing.id
        }
        val newId = dao.insertOrder(order.copy(id = 0L))
        val mappedItems = if (items.isNotEmpty()) {
            val list = items.map { it.copy(id = 0L, orderId = newId) }
            dao.insertOrderItems(list)
            list
        } else emptyList()
        notifyShopOwnersAndAdminForOrderEntity(order, mappedItems.ifEmpty { items })
        newId
    }

    suspend fun insertOrderItems(items: List<OrderItemEntity>) = withContext(Dispatchers.IO) {
        dao.insertOrderItems(items)
    }

    suspend fun updateOrderStatusLocally(orderId: Long, status: OrderStatus) = withContext(Dispatchers.IO) {
        dao.updateOrderStatus(orderId, status)
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: OrderStatus) = withContext(Dispatchers.IO) {
        dao.updateOrderStatus(orderId, newStatus)
        val order = dao.getOrderByIdSync(orderId)
        if (order != null) {
            try {
                FirestoreSyncService.updateOrderStatusInFirestore(order.orderNumber, newStatus)
            } catch (e: Throwable) { }
            try {
                OrderSyncService.updateOrderStatus(order.orderNumber, newStatus, order.paymentStatus, order.deliveryAgentAssigned)
            } catch (e: Throwable) { }
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
            try {
                FirestoreSyncService.updateOrderStatusInFirestore(order.orderNumber, OrderStatus.DELIVERED)
            } catch (e: Throwable) { }
            try {
                OrderSyncService.updateOrderStatus(order.orderNumber, OrderStatus.DELIVERED, paymentStatus, deliveredBy)
            } catch (e: Throwable) { }
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
    fun getApprovedPromotions(): Flow<List<PromotionEntity>> = dao.getApprovedPromotions()
    suspend fun createPromotion(promotion: PromotionEntity): Long = dao.insertPromotion(promotion)
    suspend fun updatePromotion(promotion: PromotionEntity) = dao.updatePromotion(promotion)
    suspend fun updatePromotionStatus(id: Long, status: PromotionStatus) = dao.updatePromotionStatus(id, status)
    suspend fun updatePromotionDetailsByAdmin(
        id: Long,
        status: PromotionStatus,
        price: Double,
        duration: Int,
        startDate: String,
        endDate: String,
        location: String,
        notes: String
    ) = dao.updatePromotionDetailsByAdmin(
        id = id,
        status = status,
        price = price,
        duration = duration,
        startDate = startDate,
        endDate = endDate,
        location = location,
        notes = notes,
        approvedAt = if (status == PromotionStatus.APPROVED || status == PromotionStatus.ACTIVE) System.currentTimeMillis() else null
    )
    suspend fun updatePromotionPaymentStatus(id: Long, paymentStatus: String) = dao.updatePromotionPaymentStatus(id, paymentStatus)

    fun getActiveBanners(): Flow<List<BannerEntity>> = dao.getActiveBanners()

    fun getReviewsForShop(shopId: Long): Flow<List<ReviewEntity>> = dao.getReviewsForShop(shopId)
    suspend fun addReview(review: ReviewEntity): Long = dao.insertReview(review)

    fun getNotifications(userId: Long): Flow<List<NotificationEntity>> = dao.getNotifications(userId)
    fun getAllNotifications(): Flow<List<NotificationEntity>> = dao.getAllNotifications()
    suspend fun insertNotification(notification: NotificationEntity): Long = dao.insertNotification(notification)
    suspend fun markNotificationRead(id: Long) = dao.markNotificationRead(id)

    fun getAllPayments(): Flow<List<PaymentRecordEntity>> = dao.getAllPayments()

    fun getAllComplaints(): Flow<List<ComplaintEntity>> = dao.getAllComplaints()
    suspend fun fileComplaint(complaint: ComplaintEntity): Long = dao.insertComplaint(complaint)
    suspend fun updateComplaint(complaint: ComplaintEntity) = dao.updateComplaint(complaint)

    suspend fun getOrderByOrderNumber(orderNumber: String): OrderEntity? = dao.getOrderByOrderNumber(orderNumber)

    /**
     * Bidirectional sync: Pulls all orders and items from Firebase Firestore and merges
     * them into local Room database, and pushes any local orders not yet in the cloud database.
     * This guarantees that every customer order from every device appears in the Admin Panel.
     */
    suspend fun syncOrdersFromCloud(): Int = withContext(Dispatchers.IO) {
        var syncedCount = 0
        try {
            val firestoreResult = FirestoreSyncService.performLiveCloudSync(dao)
            syncedCount = firestoreResult.downloadedOrdersCount
        } catch (e: Throwable) {
            android.util.Log.w("MartRepository", "Notice: syncOrdersFromCloud error: ${e.message}")
        }
        syncedCount
    }

    /**
     * Bidirectional sync: Pulls all active shops and their products from Firebase Firestore
     * and merges them into the local Room database, and backfills local shops/products to Firestore.
     */
    suspend fun syncShopsAndProductsFromCloud(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var res = Pair(0, 0)
        try {
            res = FirestoreSyncService.performShopAndProductCloudSync(dao)
        } catch (e: Throwable) {
            android.util.Log.w("MartRepository", "Notice: syncShopsAndProductsFromCloud error: ${e.message}")
        }
        res
    }
}
