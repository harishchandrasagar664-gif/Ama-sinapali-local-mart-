package com.example.data.local

import com.example.data.local.entities.*
import com.example.domain.model.*

object SeedData {
    val adminUser = UserEntity(
        id = 1,
        name = "Harishchandra Sagar (Admin)",
        phone = "9876543210",
        email = "harishchandrasagar664@gmail.com",
        role = UserRole.ADMIN
    )

    val shopOwners = listOf(
        UserEntity(
            id = 2,
            name = "Ramesh Sahu (Grocery)",
            phone = "9437012345",
            email = "ramesh@sinapali.com",
            role = UserRole.SHOP_OWNER,
            shopId = 1
        ),
        UserEntity(
            id = 3,
            name = "Santosh Meher (Sweets)",
            phone = "9437054321",
            email = "santosh@sinapali.com",
            role = UserRole.SHOP_OWNER,
            shopId = 2
        ),
        UserEntity(
            id = 4,
            name = "Bikram Patel (Electronics)",
            phone = "9437098765",
            email = "bikram@sinapali.com",
            role = UserRole.SHOP_OWNER,
            shopId = 3
        )
    )

    val customers = listOf(
        UserEntity(
            id = 5,
            name = "Priyanka Majhi",
            phone = "8895011223",
            email = "priyanka@sinapali.com",
            role = UserRole.CUSTOMER
        ),
        UserEntity(
            id = 6,
            name = "Deepak Bag",
            phone = "8895099887",
            email = "deepak@sinapali.com",
            role = UserRole.CUSTOMER
        )
    )

    val shops = listOf(
        ShopEntity(
            id = 1,
            ownerId = 2,
            name = "Maa Samaleswari Kirana Store",
            category = "Daily Groceries & Essentials",
            address = "Main Market Road, Near Daily Market, Sinapali",
            phone = "9437012345",
            rating = 4.8,
            reviewCount = 38,
            isOpen = true,
            deliveryAvailable = true,
            minOrderAmount = 100.0,
            estimatedDeliveryMins = 25,
            imageUrl = "hero_local_mart"
        ),
        ShopEntity(
            id = 2,
            ownerId = 3,
            name = "Sinapali Sweets & Snacks Corner",
            category = "Sweets, Chhena Poda & Snacks",
            address = "Bus Stand Square, Sinapali",
            phone = "9437054321",
            rating = 4.9,
            reviewCount = 52,
            isOpen = true,
            deliveryAvailable = true,
            minOrderAmount = 80.0,
            estimatedDeliveryMins = 20,
            imageUrl = "hero_local_mart"
        ),
        ShopEntity(
            id = 3,
            ownerId = 4,
            name = "Patel Electronics & Mobile Care",
            category = "Electronics, Accessories & Repairs",
            address = "College Road, Sinapali",
            phone = "9437098765",
            rating = 4.6,
            reviewCount = 19,
            isOpen = true,
            deliveryAvailable = true,
            minOrderAmount = 150.0,
            estimatedDeliveryMins = 40,
            imageUrl = "hero_local_mart"
        )
    )

    val categories = listOf(
        CategoryEntity(id = 1, name = "Groceries & Spices", iconName = "ShoppingBag", description = "Fresh rice, dal, spices, mustard oil, local items"),
        CategoryEntity(id = 2, name = "Fresh Vegetables & Fruits", iconName = "Eco", description = "Locally grown Sinapali organic vegetables & fruits"),
        CategoryEntity(id = 3, name = "Sinapali Sweets & Dairy", iconName = "Cake", description = "Authentic Chhena Poda, Rasagola & Fresh Milk"),
        CategoryEntity(id = 4, name = "Electronics & Mobile", iconName = "Devices", description = "Mobile accessories, chargers, ear buds & repairs"),
        CategoryEntity(id = 5, name = "Handloom & Sambalpuri Clothes", iconName = "Checkroom", description = "Original Sambalpuri handloom sarees & kurtas")
    )

    val products = listOf(
        ProductEntity(
            id = 1,
            shopId = 1,
            name = "Sinapali Basmati Aromatic Rice (5kg)",
            category = "Groceries & Spices",
            description = "Premium fine grain local basmati rice with sweet natural aroma.",
            price = 350.0,
            discountPrice = 320.0,
            unit = "5kg bag",
            stock = 25,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 2,
            shopId = 1,
            name = "Pure Mustard Oil (Kachhi Ghani 1L)",
            category = "Groceries & Spices",
            description = "100% pure cold pressed mustard oil for traditional Odisha cooking.",
            price = 160.0,
            discountPrice = 145.0,
            unit = "1 L pouch",
            stock = 40,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 3,
            shopId = 2,
            name = "Special Baked Chhena Poda (500g)",
            category = "Sinapali Sweets & Dairy",
            description = "Slow baked authentic cottage cheese dessert with caramelized crust and cardamom.",
            price = 220.0,
            discountPrice = 199.0,
            unit = "500 gm",
            stock = 15,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 4,
            shopId = 2,
            name = "Garam Gulab Jamun & Rasagola Box (10 pcs)",
            category = "Sinapali Sweets & Dairy",
            description = "Soft juicy sweets made from fresh pure cow milk chhena.",
            price = 180.0,
            discountPrice = 160.0,
            unit = "10 pcs",
            stock = 30,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 5,
            shopId = 3,
            name = "Fast Charging Type-C Braided Cable (65W)",
            category = "Electronics & Mobile",
            description = "Ultra durable braided fast charging data cable compatible with all smartphones.",
            price = 299.0,
            discountPrice = 199.0,
            unit = "1 unit",
            stock = 20,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 6,
            shopId = 3,
            name = "Wireless Bluetooth Earbuds with ANC",
            category = "Electronics & Mobile",
            description = "Crystal clear bass, active noise cancellation, 30 hours total battery life.",
            price = 1499.0,
            discountPrice = 899.0,
            unit = "1 box",
            stock = 8,
            imageUrl = "hero_local_mart"
        )
    )

    val sampleAddresses = listOf(
        AddressEntity(
            id = 1,
            userId = 5,
            title = "Home",
            fullAddress = "Near Panchayat Samiti Office, Word No 4, Sinapali",
            landmark = "Opposite High School Playground",
            phone = "8895011223",
            isDefault = true
        )
    )

    val sampleOffers = listOf(
        OfferEntity(
            id = 1,
            couponCode = "SINAPALI10",
            description = "Flat 10% discount on orders above ₹200 across all shops",
            discountPercent = 10.0,
            minOrderAmount = 200.0,
            maxDiscountAmount = 100.0,
            isActive = true
        ),
        OfferEntity(
            id = 2,
            couponCode = "WELCOME50",
            description = "Get ₹50 OFF on your first local order above ₹300",
            discountPercent = 15.0,
            minOrderAmount = 300.0,
            maxDiscountAmount = 50.0,
            isActive = true
        )
    )

    val sampleBanners = listOf(
        BannerEntity(
            id = 1,
            title = "Ama Sinapali Local Mart",
            subtitle = "Directly from your favorite town shops to your doorstep in 30 minutes!",
            tag = "FREE DELIVERY TODAY",
            imageUrl = "hero_local_mart",
            actionUrl = "shops"
        ),
        BannerEntity(
            id = 2,
            title = "Delicious Chhena Poda & Sweets",
            subtitle = "Freshly prepared daily at Bus Stand Square Sinapali",
            tag = "SPECIAL TASTE OF ODISHA",
            imageUrl = "hero_local_mart",
            actionUrl = "sweets"
        )
    )

    val sampleOrders = listOf(
        OrderEntity(
            id = 1,
            orderNumber = "ORD-SIN-2026-001",
            userId = 5,
            shopId = 2,
            totalAmount = 359.0,
            discountAmount = 35.9,
            deliveryFee = 20.0,
            finalAmount = 343.1,
            status = OrderStatus.DELIVERED,
            deliveryType = DeliveryType.HOME_DELIVERY,
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            paymentStatus = PaymentStatus.PAID,
            deliveryAddress = "Near Panchayat Samiti Office, Word No 4, Sinapali",
            customerPhone = "8895011223",
            customerName = "Priyanka Majhi",
            orderNotes = "Please deliver hot before evening",
            placedAt = System.currentTimeMillis() - 86400000,
            deliveryAgentAssigned = "Harishchandra Sagar (Admin)"
        ),
        OrderEntity(
            id = 2,
            orderNumber = "ORD-SIN-2026-002",
            userId = 5,
            shopId = 1,
            totalAmount = 465.0,
            discountAmount = 0.0,
            deliveryFee = 25.0,
            finalAmount = 490.0,
            status = OrderStatus.OUT_FOR_DELIVERY,
            deliveryType = DeliveryType.HOME_DELIVERY,
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            paymentStatus = PaymentStatus.PENDING,
            deliveryAddress = "Near Panchayat Samiti Office, Word No 4, Sinapali",
            customerPhone = "8895011223",
            customerName = "Priyanka Majhi",
            orderNotes = "Call upon arrival",
            placedAt = System.currentTimeMillis() - 3600000,
            deliveryAgentAssigned = "Harishchandra Sagar (Admin)"
        )
    )

    val sampleOrderItems = listOf(
        OrderItemEntity(
            id = 1,
            orderId = 1,
            productId = 3,
            productName = "Special Baked Chhena Poda (500g)",
            unitPrice = 199.0,
            quantity = 1,
            totalPrice = 199.0
        ),
        OrderItemEntity(
            id = 2,
            orderId = 1,
            productId = 4,
            productName = "Garam Gulab Jamun & Rasagola Box (10 pcs)",
            unitPrice = 160.0,
            quantity = 1,
            totalPrice = 160.0
        ),
        OrderItemEntity(
            id = 3,
            orderId = 2,
            productId = 1,
            productName = "Sinapali Basmati Aromatic Rice (5kg)",
            unitPrice = 320.0,
            quantity = 1,
            totalPrice = 320.0
        ),
        OrderItemEntity(
            id = 4,
            orderId = 2,
            productId = 2,
            productName = "Pure Mustard Oil (Kachhi Ghani 1L)",
            unitPrice = 145.0,
            quantity = 1,
            totalPrice = 145.0
        )
    )

    val sampleReviews = listOf(
        ReviewEntity(
            id = 1,
            shopId = 2,
            userId = 5,
            userName = "Priyanka Majhi",
            rating = 5.0,
            comment = "Best Chhena Poda in Nuapada district! Fresh, warm and delivered on time."
        )
    )

    val sampleNotifications = listOf(
        NotificationEntity(
            id = 1,
            userId = 5,
            title = "Order Delivered! 📦🎉",
            message = "Your order #ORD-SIN-2026-001 has been delivered successfully by Admin (Harishchandra Sagar).",
            type = NotificationType.ORDER_STATUS
        ),
        NotificationEntity(
            id = 2,
            userId = 0,
            title = "Namaskar Sinapali! 🙏",
            message = "Welcome to Ama Sinapali Local Mart. Support local shops & get swift home delivery!",
            type = NotificationType.SYSTEM
        )
    )
}
