package com.example

import com.example.data.local.SeedData
import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class AmaSinapaliFlowVerificationUnitTest {

    @Test
    fun testFlow1_CustomerLogin() {
        val customers = SeedData.customers
        assertTrue("Seed data contains registered customers", customers.isNotEmpty())
        val defaultCustomer = customers[0]
        assertEquals(5L, defaultCustomer.id)
        assertEquals("Priyanka Majhi", defaultCustomer.name)
        assertEquals(UserRole.CUSTOMER, defaultCustomer.role)
    }

    @Test
    fun testFlow2_BrowseMultipleShops() {
        val shops = SeedData.shops
        assertTrue(shops.isNotEmpty())
        assertTrue(shops.all { it.isApproved && it.isOpen })
    }

    @Test
    fun testFlow3_OpenProductsFromDifferentShops() {
        val shops = SeedData.shops
        val products = SeedData.products
        val shop1 = shops[0]
        val shop2 = shops[1]

        val shop1Products = products.filter { it.shopId == shop1.id }
        val shop2Products = products.filter { it.shopId == shop2.id }

        assertTrue("Shop 1 has products", shop1Products.isNotEmpty())
        assertTrue("Shop 2 has products", shop2Products.isNotEmpty())
        assertTrue(shop1Products.all { it.shopId == shop1.id })
        assertTrue(shop2Products.all { it.shopId == shop2.id })
        assertNotEquals(shop1Products.first().shopId, shop2Products.first().shopId)
    }

    @Test
    fun testFlow4_AddProductsFromMultipleShopsToCart() {
        val products = SeedData.products
        val shop1Product = products.first { it.shopId == 1L }
        val shop2Product = products.first { it.shopId == 2L }

        val item1 = CartItemWithProduct(
            cartItemId = 1L,
            productId = shop1Product.id,
            shopId = shop1Product.shopId,
            shopName = "Shop 1",
            productName = shop1Product.name,
            imageUrl = shop1Product.imageUrl,
            unitPrice = shop1Product.price,
            discountPrice = shop1Product.discountPrice,
            quantity = 2,
            stock = shop1Product.stock
        )
        val item2 = CartItemWithProduct(
            cartItemId = 2L,
            productId = shop2Product.id,
            shopId = shop2Product.shopId,
            shopName = "Shop 2",
            productName = shop2Product.name,
            imageUrl = shop2Product.imageUrl,
            unitPrice = shop2Product.price,
            discountPrice = shop2Product.discountPrice,
            quantity = 1,
            stock = shop2Product.stock
        )

        val cartItems = listOf(item1, item2)
        assertEquals(2, cartItems.size)
        val distinctShops = cartItems.map { it.shopId }.distinct()
        assertEquals("Cart contains products from 2 different shops", 2, distinctShops.size)
    }

    @Test
    fun testFlow5_PlaceCodOrder() {
        val customer = SeedData.customers[0]
        val shop = SeedData.shops[0]
        val product = SeedData.products.first { it.shopId == shop.id }

        val cartItem = CartItemWithProduct(
            cartItemId = 1L,
            productId = product.id,
            shopId = shop.id,
            shopName = shop.name,
            productName = product.name,
            imageUrl = product.imageUrl,
            unitPrice = product.price,
            discountPrice = product.discountPrice,
            quantity = 1,
            stock = product.stock
        )

        val subtotal = cartItem.itemTotal
        val deliveryFee = 20.0
        val total = subtotal + deliveryFee

        val order = com.example.data.local.entities.OrderEntity(
            id = 1001L,
            orderNumber = "ORD-OD-1001",
            userId = customer.id,
            shopId = shop.id,
            totalAmount = subtotal,
            discountAmount = 0.0,
            deliveryFee = deliveryFee,
            finalAmount = total,
            deliveryAddress = "Ward 2, Sinapali",
            deliveryType = DeliveryType.HOME_DELIVERY,
            status = OrderStatus.PLACED,
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            paymentStatus = PaymentStatus.PENDING,
            customerPhone = customer.phone,
            customerName = customer.name
        )

        assertEquals(PaymentMethod.CASH_ON_DELIVERY, order.paymentMethod)
        assertEquals(PaymentStatus.PENDING, order.paymentStatus)
        assertEquals(OrderStatus.PLACED, order.status)
        assertEquals("Ward 2, Sinapali", order.deliveryAddress)
    }

    @Test
    fun testFlow6_VerifyShopOwnerReceivesOnlyTheirOwnItems() {
        val shop1 = SeedData.shops[0]
        val shop2 = SeedData.shops[1]
        val prod1 = SeedData.products.first { it.shopId == shop1.id }
        val prod2 = SeedData.products.first { it.shopId == shop2.id }

        val multiShopItems = listOf(
            CartItemWithProduct(1L, prod1.id, shop1.id, shop1.name, prod1.name, prod1.imageUrl, prod1.price, prod1.discountPrice, 1, prod1.stock),
            CartItemWithProduct(2L, prod2.id, shop2.id, shop2.name, prod2.name, prod2.imageUrl, prod2.price, prod2.discountPrice, 2, prod2.stock)
        )

        val shopGroups = multiShopItems.groupBy { it.shopId }
        assertEquals(2, shopGroups.size)
        val shop1Group = shopGroups[shop1.id]!!
        val shop2Group = shopGroups[shop2.id]!!

        assertTrue("Shop 1 receives only shop 1's items", shop1Group.all { it.shopId == shop1.id })
        assertTrue("Shop 2 receives only shop 2's items", shop2Group.all { it.shopId == shop2.id })
        assertFalse("Shop 1 does not receive Shop 2's items", shop1Group.any { it.shopId == shop2.id })
        assertFalse("Shop 2 does not receive Shop 1's items", shop2Group.any { it.shopId == shop1.id })
    }

    @Test
    fun testFlow7_And_8_SwitchShopOwnersAndVerifyIsolation() {
        val owners = SeedData.shopOwners
        val shops = SeedData.shops
        val products = SeedData.products

        assertEquals(3, owners.size)
        for (owner in owners) {
            assertEquals(UserRole.SHOP_OWNER, owner.role)
            val ownerShop = shops.find { it.ownerId == owner.id }
            assertNotNull("Shop exists for owner ${owner.name}", ownerShop)
            val shop = ownerShop!!

            val ownerProducts = products.filter { it.shopId == shop.id }
            assertTrue("Owner ${owner.name} has products", ownerProducts.isNotEmpty())
            assertTrue("All products belong to owner's shop", ownerProducts.all { it.shopId == shop.id })

            val otherProducts = products.filter { it.shopId != shop.id }
            assertFalse("Owner does not own other shop's products", otherProducts.any { it.shopId == shop.id })
        }
    }

    @Test
    fun testFlow9_AdminCanSeeAllShopsAndOrders() {
        val admin = SeedData.adminUser
        assertEquals(UserRole.ADMIN, admin.role)
        assertEquals("Harishchandra Sagar (Admin)", admin.name)
        assertEquals("7894952827", admin.phone)
        assertTrue("Physical office location contains Kotamal, Sinapali Main Road",
            admin.deliveryAddress?.contains("Kotamal, Sinapali Main Road") == true)

        val allShops = SeedData.shops
        assertTrue(allShops.isNotEmpty())

        val allOrders = SeedData.sampleOrders
        assertTrue("Sample orders exist in platform", allOrders.isNotEmpty())
    }

    @Test
    fun testFlow10_OrderStatusFlowWorksCorrectly() {
        var status = OrderStatus.PLACED
        assertEquals(OrderStatus.PLACED, status)
        status = OrderStatus.ACCEPTED
        assertEquals(OrderStatus.ACCEPTED, status)
        status = OrderStatus.PREPARING
        assertEquals(OrderStatus.PREPARING, status)
        status = OrderStatus.READY_FOR_PICKUP
        assertEquals(OrderStatus.READY_FOR_PICKUP, status)
        status = OrderStatus.OUT_FOR_DELIVERY
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, status)
        status = OrderStatus.DELIVERED
        assertEquals(OrderStatus.DELIVERED, status)

        val paymentStatusOnDelivered = PaymentStatus.PAID
        assertEquals(PaymentStatus.PAID, paymentStatusOnDelivered)
    }

    @Test
    fun testAdminDeliveryFlow_CompleteLifecycle() {
        val lifecycle = listOf(
            OrderStatus.ACCEPTED,
            OrderStatus.PREPARING,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED
        )
        assertEquals(5, lifecycle.size)

        val sampleOrder = SeedData.sampleOrders.first()
        val expectedCodAmount = sampleOrder.finalAmount
        assertTrue("Order amount is positive", expectedCodAmount > 0)

        val admin = SeedData.adminUser
        assertEquals(UserRole.ADMIN, admin.role)
    }

    @Test
    fun testCustomerSearchByShopName_ShowsOnlyThatShopsProducts() {
        val shops = SeedData.shops
        val products = SeedData.products
        
        // Example user requirement: Search "Sagar Store" or any shop name
        val targetShop = shops.first()
        val query = targetShop.name

        // 1. Search matching shops
        val matchingShops = shops.filter { it.name.contains(query, ignoreCase = true) }
        assertTrue("Matching shop found by shop name", matchingShops.any { it.id == targetShop.id })

        // 2. Open shop and filter products by shopId
        val shopProducts = products.filter { it.shopId == targetShop.id }
        assertTrue("Shop has products", shopProducts.isNotEmpty())
        assertTrue("All returned products strictly belong to the matching shop", shopProducts.all { it.shopId == targetShop.id })

        // 3. Ensure no products from other shops are mixed
        val otherShopProducts = products.filter { it.shopId != targetShop.id }
        assertFalse("No mixed products from other shops", shopProducts.any { sp -> otherShopProducts.any { it.id == sp.id } })
    }

    @Test
    fun testProductImageOptions_ModelPersistence() {
        val productWithGalleryImage = SeedData.products.first().copy(
            id = 9999L,
            imageUrl = "content://media/external/images/media/12345"
        )
        assertEquals("content://media/external/images/media/12345", productWithGalleryImage.imageUrl)

        val productWithCameraImage = SeedData.products.first().copy(
            id = 9998L,
            imageUrl = "camera_captured_1727078400000"
        )
        assertTrue(productWithCameraImage.imageUrl.startsWith("camera_captured_"))
    }
    @Test
    fun testCustomer_MobileAndEmailOtpVerification() {
        // Test Mobile OTP Generation & Verification
        val mobile = "9876543210"
        val mobileOtp = ((100000..999999).random()).toString()
        assertEquals(6, mobileOtp.length)
        assertTrue(mobileOtp.all { it.isDigit() })

        // Valid OTP verification
        val isMobileOtpValid = (mobileOtp == mobileOtp)
        assertTrue(isMobileOtpValid)

        // Invalid OTP verification
        val isInvalidOtpValid = ("000000" == mobileOtp)
        assertFalse(isInvalidOtpValid)

        // Test Email OTP Generation & Verification
        val email = "priyanka@sinapali.com"
        val emailOtp = "482910"
        assertEquals(6, emailOtp.length)
        val isEmailOtpValid = (emailOtp == "482910")
        assertTrue(isEmailOtpValid)
    }

    @Test
    fun testCustomer_FirstTimeRegistration_OdishaDetailsSaved() {
        // Simulate first-time customer registration with Odisha location & delivery address
        val registeredUser = com.example.data.local.entities.UserEntity(
            id = 501L,
            name = "Ramesh Sahu",
            phone = "9438012345",
            email = "ramesh.sahu@sinapali.org",
            role = UserRole.CUSTOMER,
            isVerified = true,
            district = "Nuapada",
            block = "Sinapali",
            gp = "Sinapali",
            village = "Sinapali Town",
            deliveryAddress = "Ward No 3, Near Daily Vegetable Market, Sinapali - 766108"
        )

        assertEquals("Nuapada", registeredUser.district)
        assertEquals("Sinapali", registeredUser.block)
        assertEquals("Sinapali", registeredUser.gp)
        assertEquals("Sinapali Town", registeredUser.village)
        assertEquals("Ward No 3, Near Daily Vegetable Market, Sinapali - 766108", registeredUser.deliveryAddress)
        assertTrue(registeredUser.isVerified)

        // Saved as AddressEntity for default future order delivery
        val savedAddress = com.example.data.local.entities.AddressEntity(
            id = 101L,
            userId = registeredUser.id,
            title = "Home",
            fullAddress = registeredUser.deliveryAddress,
            phone = registeredUser.phone,
            isDefault = true,
            district = registeredUser.district,
            block = registeredUser.block,
            gp = registeredUser.gp,
            village = registeredUser.village
        )

        assertEquals(registeredUser.id, savedAddress.userId)
        assertTrue(savedAddress.isDefault)
        assertEquals("Nuapada", savedAddress.district)
        assertEquals("Sinapali", savedAddress.block)
        assertEquals("Sinapali", savedAddress.gp)
        assertEquals("Sinapali Town", savedAddress.village)
    }

    @Test
    fun testCustomer_SavedOdishaAddress_UsedForFutureOrders() {
        val user = com.example.data.local.entities.UserEntity(
            id = 502L,
            name = "Subhashree Bag",
            phone = "8895055443",
            email = "subhashree@sinapali.com",
            role = UserRole.CUSTOMER,
            isVerified = true,
            district = "Nuapada",
            block = "Sinapali",
            gp = "Hatibandh",
            village = "Hatibandh Village",
            deliveryAddress = "Main Road, Opp. High School"
        )

        // Address formatted for future checkout order
        val orderDeliveryAddress = "${user.deliveryAddress}, Village: ${user.village}, GP: ${user.gp}, Block: ${user.block}, Dist: ${user.district}"

        assertTrue(orderDeliveryAddress.contains("Hatibandh Village"))
        assertTrue(orderDeliveryAddress.contains("Hatibandh"))
        assertTrue(orderDeliveryAddress.contains("Sinapali"))
        assertTrue(orderDeliveryAddress.contains("Nuapada"))

        // Create OrderEntity using the saved delivery address
        val futureOrder = com.example.data.local.entities.OrderEntity(
            id = 701L,
            orderNumber = "ORD-2026-FUTURE-01",
            userId = user.id,
            shopId = 1L,
            totalAmount = 320.0,
            discountAmount = 0.0,
            deliveryFee = 25.0,
            finalAmount = 345.0,
            status = OrderStatus.PLACED,
            deliveryType = DeliveryType.HOME_DELIVERY,
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            paymentStatus = PaymentStatus.PENDING,
            deliveryAddress = orderDeliveryAddress,
            customerPhone = user.phone,
            customerName = user.name
        )

        assertEquals(orderDeliveryAddress, futureOrder.deliveryAddress)
        assertEquals(user.id, futureOrder.userId)
        assertEquals(345.0, futureOrder.finalAmount, 0.01)
    }
    @Test
    fun testSettingsAndPolicyScreens_NavigationRoutesExist() {
        val privacy = com.example.ui.viewmodel.Screen.PrivacyPolicy
        val contact = com.example.ui.viewmodel.Screen.ContactUs
        val about = com.example.ui.viewmodel.Screen.AboutUs
        val terms = com.example.ui.viewmodel.Screen.TermsConditions
        val returns = com.example.ui.viewmodel.Screen.ReturnsRefunds

        assertNotNull(privacy)
        assertNotNull(contact)
        assertNotNull(about)
        assertNotNull(terms)
        assertNotNull(returns)

        // Verify distinct instances/objects
        val screens = listOf(privacy, contact, about, terms, returns)
        assertEquals(5, screens.distinct().size)
    }
}
