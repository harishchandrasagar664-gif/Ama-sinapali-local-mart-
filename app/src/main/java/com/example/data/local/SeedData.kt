package com.example.data.local

import com.example.data.local.entities.*
import com.example.domain.model.*

object SeedData {
    val adminUser = UserEntity(
        id = 1,
        name = "Harishchandra Sagar (Admin)",
        phone = "7894952827",
        email = "harishchandrasagar664@gmail.com",
        role = UserRole.ADMIN,
        deliveryAddress = "Kotamal, Sinapali Main Road, Dist: Nuapada, Odisha - 766108"
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

    val defaultCategoryNames = listOf(
        "Groceries & Spices",
        "Fresh Vegetables",
        "Fresh Fruits",
        "Sinapali Sweets & Dairy",
        "Bakery & Snacks",
        "Beverages & Cold Drinks",
        "Electronics & Mobile",
        "Handloom & Sambalpuri Clothes",
        "Personal Care & Hygiene",
        "Home & Kitchen Essentials",
        "Puja & Festive Items",
        "Stationery & Books",
        "Medicine & Healthcare",
        "Baby Care & Toys",
        "Hardware & Electricals",
        "Paan & Refreshments"
    )

    val categories = listOf(
        CategoryEntity(id = 1, name = "Groceries & Spices", iconName = "ShoppingBag", description = "Fresh rice, dal, spices, mustard oil, local items"),
        CategoryEntity(id = 2, name = "Fresh Vegetables", iconName = "Eco", description = "Locally grown Sinapali organic vegetables"),
        CategoryEntity(id = 3, name = "Fresh Fruits", iconName = "Apple", description = "Fresh seasonal fruits, bananas, apples, oranges"),
        CategoryEntity(id = 4, name = "Sinapali Sweets & Dairy", iconName = "Cake", description = "Authentic Chhena Poda, Rasagola, Milk & Paneer"),
        CategoryEntity(id = 5, name = "Bakery & Snacks", iconName = "Fastfood", description = "Fresh samosas, biscuits, toast, mixture & snacks"),
        CategoryEntity(id = 6, name = "Beverages & Cold Drinks", iconName = "LocalDrink", description = "Juices, soft drinks, lassi & drinking water"),
        CategoryEntity(id = 7, name = "Electronics & Mobile", iconName = "Devices", description = "Mobile accessories, chargers, ear buds & repairs"),
        CategoryEntity(id = 8, name = "Handloom & Sambalpuri Clothes", iconName = "Checkroom", description = "Original Sambalpuri handloom sarees, kurtas & fabrics"),
        CategoryEntity(id = 9, name = "Personal Care & Hygiene", iconName = "Spa", description = "Soaps, shampoos, hair oil, toothpaste & skincare"),
        CategoryEntity(id = 10, name = "Home & Kitchen Essentials", iconName = "Kitchen", description = "Cookware, utensils, steel bottles, cleaning supplies"),
        CategoryEntity(id = 11, name = "Puja & Festive Items", iconName = "SelfImprovement", description = "Agarbatti, camphor, brass diyas, puja samagri"),
        CategoryEntity(id = 12, name = "Stationery & Books", iconName = "Book", description = "Notebooks, pens, school supplies & office stationery"),
        CategoryEntity(id = 13, name = "Medicine & Healthcare", iconName = "MedicalServices", description = "First aid essentials, OTC medicine, ORS & wellness"),
        CategoryEntity(id = 14, name = "Baby Care & Toys", iconName = "ChildCare", description = "Baby food, diapers, baby wipes & local toys"),
        CategoryEntity(id = 15, name = "Hardware & Electricals", iconName = "Build", description = "LED bulbs, extension cords, plugs, switches & tools"),
        CategoryEntity(id = 16, name = "Paan & Refreshments", iconName = "EmojiFoodBeverage", description = "Sinapali sweet meetha paan & mukhwas")
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
            shopId = 1,
            name = "Sinapali Organic Arhar / Kandul Dal (1kg)",
            category = "Groceries & Spices",
            description = "High protein locally grown polished organic unpolished yellow dal.",
            price = 140.0,
            discountPrice = 125.0,
            unit = "1 kg packet",
            stock = 30,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 4,
            shopId = 1,
            name = "Fresh Sinapali Country Brinjal / Baigan (1kg)",
            category = "Fresh Vegetables",
            description = "Freshly harvested organic green country brinjals from Sinapali farmlands.",
            price = 40.0,
            discountPrice = 35.0,
            unit = "1 kg",
            stock = 20,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 5,
            shopId = 1,
            name = "Fresh Desi Farm Potatoes (2kg)",
            category = "Fresh Vegetables",
            description = "Clean, firm, farm-direct potatoes perfect for everyday cooking.",
            price = 55.0,
            discountPrice = 48.0,
            unit = "2 kg bag",
            stock = 35,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 6,
            shopId = 1,
            name = "Fresh Ripe Red Tomatoes (1kg)",
            category = "Fresh Vegetables",
            description = "Juicy sun-ripened local country tomatoes for curries and salads.",
            price = 35.0,
            discountPrice = 30.0,
            unit = "1 kg",
            stock = 25,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 7,
            shopId = 1,
            name = "Sweet Sinapali Farm Papaya (1 pc)",
            category = "Fresh Fruits",
            description = "Naturally ripened sweet and healthy local Sinapali papaya.",
            price = 50.0,
            discountPrice = 45.0,
            unit = "1 pc (~1.2kg)",
            stock = 15,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 8,
            shopId = 1,
            name = "Fresh Champa Bananas (1 Dozen)",
            category = "Fresh Fruits",
            description = "Nutritious, sweet, energy-rich fresh local bananas.",
            price = 60.0,
            discountPrice = 50.0,
            unit = "12 pcs",
            stock = 18,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 9,
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
            id = 10,
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
            id = 11,
            shopId = 2,
            name = "Fresh Pure Cow Milk (1L pouch)",
            category = "Sinapali Sweets & Dairy",
            description = "Daily morning fresh raw pure cow milk delivered chilled.",
            price = 56.0,
            discountPrice = 52.0,
            unit = "1 L pouch",
            stock = 40,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 12,
            shopId = 2,
            name = "Sinapali Special Spicy Chanachur / Mixture (500g)",
            category = "Bakery & Snacks",
            description = "Crispy, crunchy town-favorite spicy mixture with roasted peanuts and curry leaves.",
            price = 90.0,
            discountPrice = 80.0,
            unit = "500 gm packet",
            stock = 35,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 13,
            shopId = 2,
            name = "Fresh Bakery Milk Toast & Rusk (400g)",
            category = "Bakery & Snacks",
            description = "Crispy oven-baked cardamom flavored tea rusks.",
            price = 60.0,
            discountPrice = 50.0,
            unit = "400 gm pack",
            stock = 25,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 14,
            shopId = 2,
            name = "Hot Fresh Samosa & Singada Box (4 pcs)",
            category = "Bakery & Snacks",
            description = "Piping hot spicy potato-filled samosas served with sweet & sour chutney.",
            price = 40.0,
            discountPrice = 35.0,
            unit = "4 pcs box",
            stock = 20,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 15,
            shopId = 2,
            name = "Chilled Frooti Mango Drink (1.2L)",
            category = "Beverages & Cold Drinks",
            description = "Refreshing sweet mango juice drink, served chilled for the family.",
            price = 70.0,
            discountPrice = 65.0,
            unit = "1.2 L bottle",
            stock = 25,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 16,
            shopId = 2,
            name = "Traditional Sinapali Sweet Lassi (300ml)",
            category = "Beverages & Cold Drinks",
            description = "Rich curd lassi blended with rabdi, dry fruits, and rose syrup.",
            price = 45.0,
            discountPrice = 40.0,
            unit = "300 ml glass bottle",
            stock = 20,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 17,
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
            id = 18,
            shopId = 3,
            name = "Wireless Bluetooth Earbuds with ANC",
            category = "Electronics & Mobile",
            description = "Crystal clear bass, active noise cancellation, 30 hours total battery life.",
            price = 1499.0,
            discountPrice = 899.0,
            unit = "1 box",
            stock = 8,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 19,
            shopId = 3,
            name = "10000mAh Dual USB Fast Power Bank",
            category = "Electronics & Mobile",
            description = "Slim fast-charging power bank with LED battery indicators.",
            price = 1199.0,
            discountPrice = 799.0,
            unit = "1 piece",
            stock = 12,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 20,
            shopId = 1,
            name = "Traditional Sambalpuri Handloom Cotton Saree",
            category = "Handloom & Sambalpuri Clothes",
            description = "Pure cotton hand-woven original Sambalpuri Bandha saree with authentic border design.",
            price = 1850.0,
            discountPrice = 1599.0,
            unit = "1 saree with blouse piece",
            stock = 6,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 21,
            shopId = 1,
            name = "Authentic Sambalpuri Men's Kurta (Size L/XL)",
            category = "Handloom & Sambalpuri Clothes",
            description = "100% pure handloom cotton kurta with traditional Sambalpuri motifs.",
            price = 950.0,
            discountPrice = 799.0,
            unit = "1 kurta",
            stock = 8,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 22,
            shopId = 1,
            name = "Herbal Neem & Aloe Vera Bath Soap (Pack of 3)",
            category = "Personal Care & Hygiene",
            description = "Antibacterial Ayurvedic soap bar for clear, healthy skin.",
            price = 120.0,
            discountPrice = 105.0,
            unit = "3 x 100g",
            stock = 30,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 23,
            shopId = 1,
            name = "Pure Cooling Ayurvedic Hair Oil (200ml)",
            category = "Personal Care & Hygiene",
            description = "Herbal oil enriched with bhringraj and amla for stress relief and strong hair.",
            price = 110.0,
            discountPrice = 95.0,
            unit = "200 ml bottle",
            stock = 25,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 24,
            shopId = 1,
            name = "Stainless Steel Insulated Water Bottle (1L)",
            category = "Home & Kitchen Essentials",
            description = "Double wall vacuum insulated bottle keeps water cold/hot for 12 hours.",
            price = 399.0,
            discountPrice = 299.0,
            unit = "1 L bottle",
            stock = 15,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 25,
            shopId = 1,
            name = "Heavy Duty Iron Tawa / Kadai",
            category = "Home & Kitchen Essentials",
            description = "Traditional pure iron cookware for crispy roti and healthy cooking.",
            price = 280.0,
            discountPrice = 240.0,
            unit = "1 piece",
            stock = 10,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 26,
            shopId = 1,
            name = "Pure Brass Diya & Camphor Puja Kit",
            category = "Puja & Festive Items",
            description = "Traditional carved brass diya with pure bhimseni camphor and wicks.",
            price = 150.0,
            discountPrice = 130.0,
            unit = "1 set",
            stock = 20,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 27,
            shopId = 1,
            name = "Natural Sandalwood Agarbatti (Pack of 3)",
            category = "Puja & Festive Items",
            description = "Long lasting soothing chandan fragrance for daily morning and evening prayers.",
            price = 90.0,
            discountPrice = 75.0,
            unit = "3 packs",
            stock = 30,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 28,
            shopId = 1,
            name = "Classmate Long Notebooks (Pack of 4)",
            category = "Stationery & Books",
            description = "Premium smooth paper long ruled notebooks for students and office.",
            price = 180.0,
            discountPrice = 160.0,
            unit = "4 books",
            stock = 25,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 29,
            shopId = 1,
            name = "Smooth Blue Ball Pen Box (Pack of 10)",
            category = "Stationery & Books",
            description = "Comfort grip smooth glide 0.7mm ball pens.",
            price = 100.0,
            discountPrice = 80.0,
            unit = "10 pens",
            stock = 40,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 30,
            shopId = 1,
            name = "First Aid Emergency Kit Box",
            category = "Medicine & Healthcare",
            description = "Contains antiseptic lotion, bandages, medical tape, cotton, and ointment.",
            price = 250.0,
            discountPrice = 210.0,
            unit = "1 kit box",
            stock = 15,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 31,
            shopId = 1,
            name = "ORS Hydration Electrolyte Packets (Pack of 5)",
            category = "Medicine & Healthcare",
            description = "WHO recommended formula for instant energy and rehydration.",
            price = 75.0,
            discountPrice = 65.0,
            unit = "5 sachets",
            stock = 30,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 32,
            shopId = 1,
            name = "Soft Baby Cleansing Wipes (72 Wipes)",
            category = "Baby Care & Toys",
            description = "Alcohol-free, paraben-free gentle aloe vera baby wipes.",
            price = 140.0,
            discountPrice = 119.0,
            unit = "72 wipes pack",
            stock = 20,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 33,
            shopId = 3,
            name = "Emergency Rechargeable LED Bulb (9W Inverter)",
            category = "Hardware & Electricals",
            description = "Provides up to 4 hours of bright light during power cuts automatically.",
            price = 320.0,
            discountPrice = 260.0,
            unit = "1 bulb",
            stock = 18,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 34,
            shopId = 3,
            name = "4-Socket Extension Board with Surge Spike Guard",
            category = "Hardware & Electricals",
            description = "3 meter heavy duty copper cord with individual switches and safety fuse.",
            price = 350.0,
            discountPrice = 280.0,
            unit = "1 unit",
            stock = 14,
            imageUrl = "hero_local_mart"
        ),
        ProductEntity(
            id = 35,
            shopId = 1,
            name = "Sinapali Meetha Sweet Paan Box (4 pcs)",
            category = "Paan & Refreshments",
            description = "Fresh betel leaf with sweet gulkand, cherries, dry fruits and aromatic spices.",
            price = 60.0,
            discountPrice = 50.0,
            unit = "4 pcs",
            stock = 25,
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
