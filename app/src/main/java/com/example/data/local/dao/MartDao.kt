package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MartDao {
    // Users
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersSync(): List<UserEntity>

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :identifier OR email = :identifier LIMIT 1")
    suspend fun getUserByPhoneOrEmail(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: UserRole): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("DELETE FROM addresses WHERE userId = :userId")
    suspend fun deleteAddressesForUser(userId: Long)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteNotificationsForUser(userId: Long)

    @Delete
    suspend fun deleteShop(shop: ShopEntity)

    @Query("DELETE FROM shops WHERE id = :shopId")
    suspend fun deleteShopById(shopId: Long)

    @Query("DELETE FROM products WHERE shopId = :shopId")
    suspend fun deleteProductsForShop(shopId: Long)

    @Query("DELETE FROM promotions WHERE shopId = :shopId")
    suspend fun deletePromotionsForShop(shopId: Long)

    @Query("DELETE FROM reviews WHERE shopId = :shopId")
    suspend fun deleteReviewsForShop(shopId: Long)

    // Shops
    @Query("SELECT * FROM shops WHERE isApproved = 1")
    fun getActiveShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops")
    fun getAllShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE id = :id")
    fun getShopById(id: Long): Flow<ShopEntity?>

    @Query("SELECT * FROM shops WHERE id = :id")
    suspend fun getShopByIdSync(id: Long): ShopEntity?

    @Query("SELECT * FROM shops WHERE ownerId = :ownerId LIMIT 1")
    suspend fun getShopByOwnerId(ownerId: Long): ShopEntity?

    @Query("SELECT * FROM shops WHERE ownerId = :ownerId LIMIT 1")
    fun getShopFlowByOwnerId(ownerId: Long): Flow<ShopEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllShops(shops: List<ShopEntity>)

    @Update
    suspend fun updateShop(shop: ShopEntity)

    // Products
    @Query("SELECT * FROM products WHERE isApproved = 1 AND isAvailable = 1")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE shopId = :shopId")
    fun getProductsByShop(shopId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category AND isApproved = 1")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductByIdSync(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    // Categories
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    // Cart
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartItems(userId: Long): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId LIMIT 1")
    suspend fun getCartItemByProduct(userId: Long, productId: Long): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItemEntity): Long

    @Update
    suspend fun updateCartItem(cartItem: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateCartQuantity(id: Long, quantity: Int)

    @Delete
    suspend fun deleteCartItem(cartItem: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: Long)

    // Orders
    @Query("SELECT * FROM orders ORDER BY placedAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY placedAt DESC")
    fun getOrdersByCustomer(userId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE shopId = :shopId ORDER BY placedAt DESC")
    fun getOrdersByShop(shopId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getOrderById(id: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderByIdSync(id: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE orderNumber = :orderNumber LIMIT 1")
    suspend fun getOrderByOrderNumber(orderNumber: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOrders(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus)

    @Query("UPDATE orders SET status = :status, paymentStatus = :paymentStatus, deliveryAgentAssigned = :deliveredBy WHERE id = :orderId")
    suspend fun completeDeliveryOrder(
        orderId: Long,
        status: OrderStatus,
        paymentStatus: PaymentStatus,
        deliveredBy: String?
    )

    // Order Items
    @Query("SELECT * FROM order_items")
    fun getAllOrderItems(): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: Long): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    // Addresses
    @Query("SELECT * FROM addresses WHERE userId = :userId")
    fun getAddressesByCustomer(userId: Long): Flow<List<AddressEntity>>

    @Query("SELECT * FROM addresses WHERE userId = :userId AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultAddressForUser(userId: Long): AddressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: AddressEntity): Long

    @Update
    suspend fun updateAddress(address: AddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddresses(addresses: List<AddressEntity>)

    // Banners
    @Query("SELECT * FROM banners WHERE isActive = 1")
    fun getActiveBanners(): Flow<List<BannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanners(banners: List<BannerEntity>)

    // Offers
    @Query("SELECT * FROM offers WHERE isActive = 1")
    fun getActiveOffers(): Flow<List<OfferEntity>>

    @Query("SELECT * FROM offers WHERE couponCode = :code AND isActive = 1 LIMIT 1")
    suspend fun getOfferByCode(code: String): OfferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: OfferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOffers(offers: List<OfferEntity>)

    // Reviews
    @Query("SELECT * FROM reviews WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun getReviewsForShop(shopId: Long): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)

    // Notifications
    @Query("SELECT * FROM notifications WHERE userId = :userId OR userId = 0 ORDER BY timestamp DESC")
    fun getNotifications(userId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: Long)

    // Promotions
    @Query("SELECT * FROM promotions ORDER BY submittedAt DESC")
    fun getAllPromotions(): Flow<List<PromotionEntity>>

    @Query("SELECT * FROM promotions WHERE shopId = :shopId ORDER BY submittedAt DESC")
    fun getPromotionsByShop(shopId: Long): Flow<List<PromotionEntity>>

    @Query("SELECT * FROM promotions WHERE status = 'APPROVED' OR status = 'ACTIVE' ORDER BY submittedAt DESC")
    fun getApprovedPromotions(): Flow<List<PromotionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotion(promotion: PromotionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPromotions(promotions: List<PromotionEntity>)

    @Update
    suspend fun updatePromotion(promotion: PromotionEntity)

    @Query("UPDATE promotions SET status = :status WHERE id = :id")
    suspend fun updatePromotionStatus(id: Long, status: PromotionStatus)

    @Query("UPDATE promotions SET status = :status, totalPrice = :price, durationDays = :duration, startDate = :startDate, endDate = :endDate, location = :location, adminNotes = :notes, approvedAt = :approvedAt WHERE id = :id")
    suspend fun updatePromotionDetailsByAdmin(
        id: Long,
        status: PromotionStatus,
        price: Double,
        duration: Int,
        startDate: String,
        endDate: String,
        location: String,
        notes: String,
        approvedAt: Long?
    )

    @Query("UPDATE promotions SET paymentStatus = :paymentStatus WHERE id = :id")
    suspend fun updatePromotionPaymentStatus(id: Long, paymentStatus: String)

    // Complaints
    @Query("SELECT * FROM complaints ORDER BY filedAt DESC")
    fun getAllComplaints(): Flow<List<ComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity): Long

    @Update
    suspend fun updateComplaint(complaint: ComplaintEntity)

    // Payments
    @Query("SELECT * FROM payment_records ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecordEntity): Long
}
