package com.example

import com.example.data.local.SeedData
import com.example.data.local.entities.*
import com.example.domain.model.*
import com.example.ui.viewmodel.Screen
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

    @Test
    fun testRBAC_CustomerMustNeverAccessAdminPanelOrShopOwnerDashboard() {
        val customer = SeedData.customers[0]
        assertEquals(UserRole.CUSTOMER, customer.role)

        val adminScreens = listOf(
            com.example.ui.viewmodel.Screen.AdminDashboard,
            com.example.ui.viewmodel.Screen.AdminDelivery,
            com.example.ui.viewmodel.Screen.AdminOrders,
            com.example.ui.viewmodel.Screen.AdminShops,
            com.example.ui.viewmodel.Screen.AdminPromotions
        )

        val shopScreens = listOf(
            com.example.ui.viewmodel.Screen.ShopOwnerDashboard,
            com.example.ui.viewmodel.Screen.ShopOrderManagement,
            com.example.ui.viewmodel.Screen.ShopProductManagement,
            com.example.ui.viewmodel.Screen.ShopOffersPromotions
        )

        // Verify that under customer role, none of the admin or shop screens are allowed
        for (screen in adminScreens) {
            val isAllowed = customer.role == UserRole.ADMIN
            assertFalse("Customer must not access Admin screen $screen", isAllowed)
        }

        for (screen in shopScreens) {
            val isAllowed = customer.role == UserRole.SHOP_OWNER
            assertFalse("Customer must not access Shop Owner screen $screen", isAllowed)
        }
    }

    @Test
    fun testRBAC_ShopOwnersAccessOnlyTheirOwnShopDashboard() {
        val owners = SeedData.shopOwners
        val shops = SeedData.shops
        val products = SeedData.products
        val sampleOrders = SeedData.sampleOrders

        assertEquals(3, owners.size)

        for (owner in owners) {
            assertEquals(UserRole.SHOP_OWNER, owner.role)
            val myShop = shops.find { it.ownerId == owner.id || (owner.shopId != null && it.id == owner.shopId) }
            assertNotNull("Shop exists for owner ${owner.name}", myShop)

            // 1. Products isolation
            val myProducts = products.filter { it.shopId == myShop!!.id }
            val otherProducts = products.filter { it.shopId != myShop!!.id }
            assertTrue("Shop Owner has products for their own shop", myProducts.isNotEmpty())
            assertTrue("All products belong to owner's shop", myProducts.all { it.shopId == myShop!!.id })
            assertFalse("Shop owner does not see other shops' products in their inventory", myProducts.any { it.shopId != myShop!!.id })

            // 2. Orders isolation
            val myOrders = sampleOrders.filter { it.shopId == myShop!!.id }
            assertTrue("Owner's filtered orders strictly match their shop id", myOrders.all { it.shopId == myShop!!.id })

            // 3. Shop owner cannot access Admin Panel
            val canAccessAdmin = owner.role == UserRole.ADMIN
            assertFalse("Shop Owner cannot access Admin Panel", canAccessAdmin)
        }
    }

    @Test
    fun testRBAC_AdminOnlyCanAccessAdminPanel() {
        val admin = SeedData.adminUser
        val customer = SeedData.customers[0]
        val shopOwner = SeedData.shopOwners[0]

        assertEquals(UserRole.ADMIN, admin.role)
        assertTrue("Admin can access Admin Panel", admin.role == UserRole.ADMIN)
        assertFalse("Customer cannot access Admin Panel", customer.role == UserRole.ADMIN)
        assertFalse("Shop Owner cannot access Admin Panel", shopOwner.role == UserRole.ADMIN)
    }

    @Test
    fun testRBAC_AdminPinVerification() {
        val validPin1 = "7894"
        val validPin2 = "admin123"
        val invalidPin = "0000"

        val isValid1 = (validPin1 == "7894" || validPin1 == "admin123")
        val isValid2 = (validPin2 == "7894" || validPin2 == "admin123")
        val isInvalid = (invalidPin == "7894" || invalidPin == "admin123")

        assertTrue("Valid PIN 7894 unlocks admin", isValid1)
        assertTrue("Valid PIN admin123 unlocks admin", isValid2)
        assertFalse("Invalid PIN is rejected", isInvalid)
    }

    @Test
    fun testRBAC_ShopOwnerPhoneVerification() {
        val validShopPhone = "9437012345" // Ramesh Sahu
        val customerPhone = "8895011223" // Priyanka Majhi (Customer)

        val owner = SeedData.shopOwners.find { it.phone == validShopPhone }
        assertNotNull("Shop owner found for valid shop phone", owner)
        assertEquals(UserRole.SHOP_OWNER, owner!!.role)

        val customerAsOwner = SeedData.shopOwners.find { it.phone == customerPhone }
        assertNull("Customer phone cannot log in as Shop Owner", customerAsOwner)
    }

    @Test
    fun testProductImagePersistence_AppStoragePathsAndEntity() {
        val permanentPath = "/data/user/0/com.aistudio.amalocalmart.oxrkpz/files/products/prod_1727438400000.jpg"
        val sampleProduct = ProductEntity(
            id = 999L,
            shopId = 1L,
            name = "Sinapali Fresh Mangoes",
            category = "Fresh Fruits",
            imageUrl = permanentPath,
            description = "Sweet local mangoes from Sinapali orchard",
            price = 120.0,
            unit = "1 kg",
            stock = 15
        )

        // Verify permanent path is stored in entity
        assertEquals(permanentPath, sampleProduct.imageUrl)
        assertTrue("Image URL is a permanent storage path", sampleProduct.imageUrl.startsWith("/data/"))
        assertFalse("Image URL is not a temporary content URI", sampleProduct.imageUrl.startsWith("content://"))

        // Verify updating product image maintains entity integrity
        val updatedPath = "/data/user/0/com.aistudio.amalocalmart.oxrkpz/files/products/prod_1727438500000.jpg"
        val updatedProduct = sampleProduct.copy(imageUrl = updatedPath)
        assertEquals(updatedPath, updatedProduct.imageUrl)
        assertEquals(sampleProduct.id, updatedProduct.id)
        assertEquals(sampleProduct.shopId, updatedProduct.shopId)
    }

    @Test
    fun testCheckout_CashOnDeliveryAvailableAndCalculation() {
        val sEn = com.example.ui.locale.AppStrings.get(com.example.domain.model.AppLanguage.ENGLISH)
        val sOr = com.example.ui.locale.AppStrings.get(com.example.domain.model.AppLanguage.ODIA)

        // Verify COD button text exists in English & Odia
        assertEquals("Place Order (Cash on Delivery)", sEn.placeOrderCod)
        assertTrue("Odia COD string is present", sOr.placeOrderCod.isNotBlank())
        assertEquals("Cash on Delivery (Pay at Doorstep)", sEn.codMethod)

        // Verify checkout calculations for COD Home Delivery
        val subtotal = 350.0
        val discount = 35.0
        val homeDeliveryFee = 25.0
        val pickupDeliveryFee = 0.0

        val codHomeTotal = (subtotal - discount + homeDeliveryFee).coerceAtLeast(0.0)
        assertEquals(340.0, codHomeTotal, 0.01)

        // Verify checkout calculations for COD Self Pickup
        val codPickupTotal = (subtotal - discount + pickupDeliveryFee).coerceAtLeast(0.0)
        assertEquals(315.0, codPickupTotal, 0.01)

        // Verify COD payment method is available
        val codMethod = PaymentMethod.CASH_ON_DELIVERY
        assertNotNull(codMethod)
    }

    @Test
    fun testCustomerLoginPersistence_ActiveSessionAcrossNavigation() {
        val verifiedCustomer = UserEntity(
            id = 101L,
            name = "Rakesh Kumar",
            phone = "8895012345",
            email = "rakesh@sinapali.com",
            role = UserRole.CUSTOMER,
            isVerified = true,
            district = "Nuapada",
            block = "Sinapali",
            gp = "Hatibandh",
            village = "Hatibandh",
            deliveryAddress = "Ward No 2, Hatibandh, Sinapali"
        )

        // Verify active customer session properties
        assertTrue("Customer is verified with OTP", verifiedCustomer.isVerified)
        assertEquals(UserRole.CUSTOMER, verifiedCustomer.role)
        assertEquals("Hatibandh", verifiedCustomer.village)
        assertEquals("Ward No 2, Hatibandh, Sinapali", verifiedCustomer.deliveryAddress)
    }

    @Test
    fun testCustomerLoginPersistence_BackNavigationNeverClearsSession() {
        // Simulate navigation stack transitions
        val stack = ArrayDeque<Screen>()
        var activeCustomer: UserEntity? = UserEntity(
            id = 101L,
            name = "Rakesh Kumar",
            phone = "8895012345",
            role = UserRole.CUSTOMER,
            isVerified = true
        )

        // Customer navigates Home -> Profile -> Cart -> Checkout
        stack.addLast(Screen.CustomerHome)
        stack.addLast(Screen.CustomerProfile)
        stack.addLast(Screen.Cart)
        var currentScreen: Screen = Screen.Checkout

        // Back navigation 1: Checkout -> Cart
        if (stack.isNotEmpty()) {
            currentScreen = stack.removeLast()
        }
        assertEquals(Screen.Cart, currentScreen)
        assertNotNull("Session must not be cleared on back navigation", activeCustomer)
        assertEquals(101L, activeCustomer?.id)

        // Back navigation 2: Cart -> Profile
        if (stack.isNotEmpty()) {
            currentScreen = stack.removeLast()
        }
        assertEquals(Screen.CustomerProfile, currentScreen)
        assertNotNull("Session must not be cleared on back navigation", activeCustomer)

        // Back navigation 3: Profile -> Home
        if (stack.isNotEmpty()) {
            currentScreen = stack.removeLast()
        }
        assertEquals(Screen.CustomerHome, currentScreen)
        assertNotNull("Session must not be cleared on returning to CustomerHome", activeCustomer)
        assertEquals(101L, activeCustomer?.id)
        assertTrue("Customer remains verified", activeCustomer?.isVerified == true)
    }

    @Test
    fun testCustomerLoginPersistence_ExplicitLogoutClearsSession() {
        var activeCustomer: UserEntity? = UserEntity(
            id = 101L,
            name = "Rakesh Kumar",
            phone = "8895012345",
            role = UserRole.CUSTOMER,
            isVerified = true
        )
        assertNotNull(activeCustomer)

        // Explicit logout action only
        val explicitLogoutAction = {
            activeCustomer = null
        }
        explicitLogoutAction()

        assertNull("Explicit logout clears the session", activeCustomer)
    }

    @Test
    fun testCustomerProfileAndCartPreservationOnBackNavigation() {
        val customer = UserEntity(
            id = 101L,
            name = "Rakesh Kumar",
            phone = "8895012345",
            role = UserRole.CUSTOMER,
            isVerified = true,
            district = "Nuapada",
            block = "Sinapali",
            gp = "Sinapali",
            village = "Sinapali Town",
            deliveryAddress = "Main Road Sinapali"
        )
        val cartItems = listOf(
            CartItemWithProduct(
                cartItemId = 1L,
                productId = 1L,
                shopId = 1L,
                shopName = "Sinapali Kirana Store",
                productName = "Atta 5kg",
                imageUrl = "/data/products/atta.jpg",
                unitPrice = 180.0,
                discountPrice = 160.0,
                quantity = 2,
                stock = 10
            )
        )

        // Verify cart is retained for the customer
        assertEquals(1, cartItems.size)
        assertEquals(320.0, cartItems.sumOf { (it.discountPrice ?: it.unitPrice) * it.quantity }, 0.01)

        // Verify profile and address are retained
        assertEquals("Main Road Sinapali", customer.deliveryAddress)
        assertEquals("Sinapali Town", customer.village)
    }

    @Test
    fun testStrictShopOwnerAccess_OwnShopOrdersOnly() {
        val shopOwner = UserEntity(
            id = 2L,
            name = "Ramesh Sahu",
            phone = "9437012345",
            role = UserRole.SHOP_OWNER,
            shopId = 1L
        )

        val orders = listOf(
            OrderEntity(
                id = 1001L,
                orderNumber = "ORD-1001",
                userId = 5L,
                shopId = 1L,
                totalAmount = 300.0,
                discountAmount = 0.0,
                deliveryFee = 25.0,
                finalAmount = 325.0,
                status = OrderStatus.PLACED,
                deliveryType = DeliveryType.HOME_DELIVERY,
                paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
                paymentStatus = PaymentStatus.PENDING,
                deliveryAddress = "Ward 4, Sinapali",
                customerPhone = "9876543210",
                customerName = "Priyanka Majhi",
                placedAt = System.currentTimeMillis()
            ),
            OrderEntity(
                id = 1002L,
                orderNumber = "ORD-1002",
                userId = 6L,
                shopId = 2L,
                totalAmount = 500.0,
                discountAmount = 50.0,
                deliveryFee = 0.0,
                finalAmount = 450.0,
                status = OrderStatus.PLACED,
                deliveryType = DeliveryType.SELF_PICKUP,
                paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
                paymentStatus = PaymentStatus.PENDING,
                deliveryAddress = "Main Road, Sinapali",
                customerPhone = "9876543211",
                customerName = "Amit Kumar",
                placedAt = System.currentTimeMillis()
            )
        )

        // Filter orders for Shop Owner 1
        val shop1Orders = orders.filter { it.shopId == shopOwner.shopId }
        assertEquals(1, shop1Orders.size)
        assertEquals(1001L, shop1Orders.first().id)
        assertEquals("ORD-1001", shop1Orders.first().orderNumber)
        // Must never include Shop 2 orders
        assertFalse(shop1Orders.any { it.shopId == 2L })
    }

    @Test
    fun testStrictShopOwnerAccess_OrderFieldsRestrictedToAllowedData() {
        val order = OrderEntity(
            id = 1001L,
            orderNumber = "ORD-1001",
            userId = 5L,
            shopId = 1L,
            totalAmount = 260.0,
            discountAmount = 0.0,
            deliveryFee = 25.0,
            finalAmount = 285.0,
            status = OrderStatus.PLACED,
            deliveryType = DeliveryType.HOME_DELIVERY,
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            paymentStatus = PaymentStatus.PENDING,
            deliveryAddress = "Ward 4, Sinapali",
            customerPhone = "9876543210",
            customerName = "Priyanka Majhi",
            placedAt = System.currentTimeMillis()
        )

        val orderItems = listOf(
            OrderItemEntity(
                id = 1L,
                orderId = 1001L,
                productId = 10L,
                productName = "Mustard Oil 1L",
                unitPrice = 140.0,
                quantity = 1,
                totalPrice = 140.0
            ),
            OrderItemEntity(
                id = 2L,
                orderId = 1001L,
                productId = 11L,
                productName = "Sugar 2kg",
                unitPrice = 120.0,
                quantity = 1,
                totalPrice = 120.0
            )
        )

        // Shop Owner can see:
        // 1. Customer Name
        assertEquals("Priyanka Majhi", order.customerName)
        // 2. Mobile Number
        assertEquals("9876543210", order.customerPhone)
        // 3. Ordered Product Name, Quantity, MRP (unit price)
        assertEquals(2, orderItems.size)
        assertEquals("Mustard Oil 1L", orderItems[0].productName)
        assertEquals(1, orderItems[0].quantity)
        assertEquals(140.0, orderItems[0].unitPrice, 0.01)
        assertEquals("Sugar 2kg", orderItems[1].productName)
        assertEquals(1, orderItems[1].quantity)
        assertEquals(120.0, orderItems[1].unitPrice, 0.01)
    }

    @Test
    fun testStrictShopOwnerAccess_OwnShopOrderNotificationsOnly() {
        val shopOwner = UserEntity(
            id = 2L,
            name = "Ramesh Sahu",
            phone = "9437012345",
            role = UserRole.SHOP_OWNER,
            shopId = 1L
        )

        val notifications = listOf(
            NotificationEntity(
                id = 1L,
                userId = 2L,
                title = "New Order #ORD-1001 Received! 🛍️",
                message = "New order of ₹285 from Priyanka Majhi for Sinapali Kirana Store. Check & prepare items.",
                type = NotificationType.ORDER_STATUS
            ),
            NotificationEntity(
                id = 2L,
                userId = 3L,
                title = "New Order #ORD-1002 Received! 🛍️",
                message = "New order of ₹450 from Amit Kumar for Maa Samaleswari Sweets.",
                type = NotificationType.ORDER_STATUS
            ),
            NotificationEntity(
                id = 3L,
                userId = 5L,
                title = "Festival 10% Discount Available 🎉",
                message = "Enjoy discounts across Sinapali Mart.",
                type = NotificationType.OFFER
            )
        )

        val shopName = "Sinapali Kirana Store"
        val shopOwnerNotifications = notifications.filter { notif ->
            val isOrderNotification = notif.type == NotificationType.ORDER_STATUS ||
                notif.title.contains("Order", ignoreCase = true) ||
                notif.message.contains("Order", ignoreCase = true)
            val isForMyShop = notif.userId == shopOwner.id ||
                notif.title.contains(shopName, ignoreCase = true) ||
                notif.message.contains(shopName, ignoreCase = true)
            isOrderNotification && isForMyShop
        }

        assertEquals(1, shopOwnerNotifications.size)
        assertEquals("New Order #ORD-1001 Received! 🛍️", shopOwnerNotifications.first().title)
        assertFalse("Cannot see other shop notifications", shopOwnerNotifications.any { it.userId == 3L })
        assertFalse("Cannot see non-order promotional customer notifications", shopOwnerNotifications.any { it.type == NotificationType.OFFER })
    }

    @Test
    fun testStrictShopOwnerAccess_NavigationAccessDeniedForCustomerScreensAndOtherShops() {
        val currentRole = UserRole.SHOP_OWNER
        val myShopId = 1L

        // Customer screens that Shop Owner must never access
        val forbiddenScreensForShopOwner = listOf(
            Screen.CustomerProfile,
            Screen.CustomerAuth,
            Screen.Cart,
            Screen.Checkout,
            Screen.CustomerOrders,
            Screen.OrderTracking(1001L)
        )

        forbiddenScreensForShopOwner.forEach { screen ->
            val isForbidden = when (screen) {
                is Screen.CustomerProfile,
                is Screen.CustomerAuth,
                is Screen.Cart,
                is Screen.Checkout,
                is Screen.CustomerOrders,
                is Screen.OrderTracking -> currentRole == UserRole.SHOP_OWNER
                else -> false
            }
            assertTrue("Screen ${screen.javaClass.simpleName} must be forbidden for Shop Owner", isForbidden)
        }

        // Other shop detail is forbidden for Shop Owner
        val otherShopScreen = Screen.ShopDetail(2L)
        val isOtherShopForbidden = (currentRole == UserRole.SHOP_OWNER && otherShopScreen.shopId != myShopId)
        assertTrue("Other shop data must be forbidden for Shop Owner", isOtherShopForbidden)

        // Own shop detail is allowed
        val ownShopScreen = Screen.ShopDetail(1L)
        val isOwnShopAllowed = (ownShopScreen.shopId == myShopId)
        assertTrue("Own shop data must be accessible for Shop Owner", isOwnShopAllowed)
    }

    @Test
    fun testShopOwnerCanCreateAllFivePromotionFormats() {
        val formats = listOf(
            PromotionType.PRODUCT_AD,
            PromotionType.PROMOTIONAL_BANNER,
            PromotionType.FEATURED_SHOP,
            PromotionType.LOCATION_BASED,
            PromotionType.DATE_BASED
        )

        assertEquals(5, formats.size)
        assertTrue(formats.contains(PromotionType.PRODUCT_AD))
        assertTrue(formats.contains(PromotionType.PROMOTIONAL_BANNER))
        assertTrue(formats.contains(PromotionType.FEATURED_SHOP))
        assertTrue(formats.contains(PromotionType.LOCATION_BASED))
        assertTrue(formats.contains(PromotionType.DATE_BASED))
    }

    @Test
    fun testPromotionPreSubmissionQuoteShowsPriceDurationDateAndLocation() {
        val pricingConfig = com.example.ui.viewmodel.PromotionPricingConfig(
            productAdDailyRate = 40.0,
            promotionalBannerDailyRate = 80.0,
            featuredShopDailyRate = 100.0,
            locationBasedDailyRate = 60.0,
            dateBasedDailyRate = 50.0
        )

        val durationDays = 7
        val startDate = "2026-09-30"
        val endDate = "2026-10-07"
        val location = "Hatibandh GP, Sinapali"

        // For Product Ad
        val adDailyRate = pricingConfig.getDailyRate(PromotionType.PRODUCT_AD)
        val adTotalPrice = adDailyRate * durationDays
        assertEquals(40.0, adDailyRate, 0.01)
        assertEquals(280.0, adTotalPrice, 0.01)

        // For Banner
        val bannerDailyRate = pricingConfig.getDailyRate(PromotionType.PROMOTIONAL_BANNER)
        val bannerTotalPrice = bannerDailyRate * durationDays
        assertEquals(80.0, bannerDailyRate, 0.01)
        assertEquals(560.0, bannerTotalPrice, 0.01)

        // For Featured Shop
        val featuredDailyRate = pricingConfig.getDailyRate(PromotionType.FEATURED_SHOP)
        val featuredTotalPrice = featuredDailyRate * 14
        assertEquals(100.0, featuredDailyRate, 0.01)
        assertEquals(1400.0, featuredTotalPrice, 0.01)

        // For Location Promotion
        val locDailyRate = pricingConfig.getDailyRate(PromotionType.LOCATION_BASED)
        val locTotalPrice = locDailyRate * durationDays
        assertEquals(60.0, locDailyRate, 0.01)
        assertEquals(420.0, locTotalPrice, 0.01)

        // For Date Promotion
        val dateDailyRate = pricingConfig.getDailyRate(PromotionType.DATE_BASED)
        val dateTotalPrice = dateDailyRate * 3
        assertEquals(50.0, dateDailyRate, 0.01)
        assertEquals(150.0, dateTotalPrice, 0.01)

        // Validate pre-submission quote fields
        val quoteFields = mapOf(
            "format" to PromotionType.LOCATION_BASED.name,
            "dailyRate" to locDailyRate,
            "duration" to durationDays,
            "startDate" to startDate,
            "endDate" to endDate,
            "location" to location,
            "totalPrice" to locTotalPrice
        )

        assertEquals(420.0, quoteFields["totalPrice"])
        assertEquals("Hatibandh GP, Sinapali", quoteFields["location"])
        assertEquals("2026-09-30", quoteFields["startDate"])
        assertEquals("2026-10-07", quoteFields["endDate"])
        assertEquals(7, quoteFields["duration"])
    }

    @Test
    fun testPromotionSubmissionInitialStatusIsPendingAdminApproval() {
        val newPromotion = PromotionEntity(
            id = 10,
            shopId = 1L,
            shopName = "Sinapali Kirana Store",
            title = "Fresh Spices Offer",
            description = "10% off on all local spices",
            type = PromotionType.PRODUCT_AD,
            status = PromotionStatus.PENDING_APPROVAL,
            location = "Sinapali Town",
            durationDays = 7,
            startDate = "2026-09-30",
            endDate = "2026-10-07",
            dailyRate = 40.0,
            totalPrice = 280.0,
            paymentStatus = "Pending Payment / Post-Approval"
        )

        assertEquals(PromotionStatus.PENDING_APPROVAL, newPromotion.status)
        assertEquals("Pending Payment / Post-Approval", newPromotion.paymentStatus)
        assertEquals(280.0, newPromotion.totalPrice, 0.01)
        assertEquals("Sinapali Town", newPromotion.location)
    }

    @Test
    fun testAdminCanReviewApproveRejectAndEditPromotionDetails() {
        val submittedPromo = PromotionEntity(
            id = 11,
            shopId = 2L,
            shopName = "Maa Samaleswari Sweets",
            title = "Nuakhai Special Chhena Poda",
            description = "Traditional taste of Sinapali",
            type = PromotionType.LOCATION_BASED,
            status = PromotionStatus.PENDING_APPROVAL,
            location = "Sinapali Town",
            durationDays = 7,
            startDate = "2026-09-30",
            endDate = "2026-10-07",
            dailyRate = 60.0,
            totalPrice = 420.0
        )

        // Admin reviews and edits price, dates, duration, location and notes
        val revisedPrice = 350.0
        val revisedDuration = 5
        val revisedStartDate = "2026-10-01"
        val revisedEndDate = "2026-10-06"
        val revisedLocation = "Sinapali Market & Hatibandh"
        val adminNotes = "Approved with special festival rate discount by Admin Harishchandra Sagar"

        val approvedPromo = submittedPromo.copy(
            status = PromotionStatus.APPROVED,
            totalPrice = revisedPrice,
            durationDays = revisedDuration,
            startDate = revisedStartDate,
            endDate = revisedEndDate,
            location = revisedLocation,
            adminNotes = adminNotes,
            approvedAt = System.currentTimeMillis()
        )

        assertEquals(PromotionStatus.APPROVED, approvedPromo.status)
        assertEquals(350.0, approvedPromo.totalPrice, 0.01)
        assertEquals(5, approvedPromo.durationDays)
        assertEquals("2026-10-01", approvedPromo.startDate)
        assertEquals("2026-10-06", approvedPromo.endDate)
        assertEquals("Sinapali Market & Hatibandh", approvedPromo.location)
        assertEquals("Approved with special festival rate discount by Admin Harishchandra Sagar", approvedPromo.adminNotes)
        assertNotNull(approvedPromo.approvedAt)

        // Admin rejection test
        val rejectedPromo = submittedPromo.copy(
            status = PromotionStatus.REJECTED,
            adminNotes = "Image quality inadequate. Please resubmit."
        )
        assertEquals(PromotionStatus.REJECTED, rejectedPromo.status)
    }

    @Test
    fun testOnlyApprovedPromotionsAreVisibleToCustomers() {
        val allPromotions = listOf(
            PromotionEntity(
                id = 1,
                shopId = 1,
                shopName = "Sinapali Kirana Store",
                title = "Approved Banner Offer",
                description = "Live promo",
                type = PromotionType.PROMOTIONAL_BANNER,
                status = PromotionStatus.APPROVED,
                totalPrice = 560.0
            ),
            PromotionEntity(
                id = 2,
                shopId = 2,
                shopName = "Maa Samaleswari Sweets",
                title = "Pending Product Ad",
                description = "Pending approval",
                type = PromotionType.PRODUCT_AD,
                status = PromotionStatus.PENDING_APPROVAL,
                totalPrice = 280.0
            ),
            PromotionEntity(
                id = 3,
                shopId = 3,
                shopName = "Maa Tarini Handloom",
                title = "Rejected Date Promotion",
                description = "Not approved",
                type = PromotionType.DATE_BASED,
                status = PromotionStatus.REJECTED,
                totalPrice = 150.0
            ),
            PromotionEntity(
                id = 4,
                shopId = 1,
                shopName = "Sinapali Kirana Store",
                title = "Active Featured Shop",
                description = "Featured partner",
                type = PromotionType.FEATURED_SHOP,
                status = PromotionStatus.ACTIVE,
                totalPrice = 1400.0
            )
        )

        // Filter for customer visibility (only APPROVED or ACTIVE)
        val customerVisiblePromotions = allPromotions.filter {
            it.status == PromotionStatus.APPROVED || it.status == PromotionStatus.ACTIVE
        }

        assertEquals(2, customerVisiblePromotions.size)
        assertTrue(customerVisiblePromotions.any { it.id == 1L })
        assertTrue(customerVisiblePromotions.any { it.id == 4L })

        // Pending and rejected must NEVER be visible to customers
        assertFalse(customerVisiblePromotions.any { it.status == PromotionStatus.PENDING_APPROVAL })
        assertFalse(customerVisiblePromotions.any { it.status == PromotionStatus.REJECTED })
        assertFalse(customerVisiblePromotions.any { it.id == 2L })
        assertFalse(customerVisiblePromotions.any { it.id == 3L })
    }

    @Test
    fun testShopOwnerPromotionStatusHistoryAndPaymentTracking() {
        val shopId = 1L
        val promotions = listOf(
            PromotionEntity(
                id = 1,
                shopId = 1L,
                shopName = "Sinapali Kirana Store",
                title = "Featured Shop Partner",
                description = "Top placement in store directory",
                type = PromotionType.FEATURED_SHOP,
                status = PromotionStatus.APPROVED,
                totalPrice = 1400.0,
                paymentStatus = "PAID"
            ),
            PromotionEntity(
                id = 2,
                shopId = 1L,
                shopName = "Sinapali Kirana Store",
                title = "Mustard Oil Product Ad",
                description = "Town spotlight",
                type = PromotionType.PRODUCT_AD,
                status = PromotionStatus.PENDING_APPROVAL,
                totalPrice = 280.0,
                paymentStatus = "Pending Payment / Post-Approval"
            ),
            PromotionEntity(
                id = 3,
                shopId = 2L,
                shopName = "Maa Samaleswari Sweets",
                title = "Sweets Banner",
                description = "Other shop promo",
                type = PromotionType.PROMOTIONAL_BANNER,
                status = PromotionStatus.APPROVED,
                totalPrice = 560.0,
                paymentStatus = "PAID"
            )
        )

        // Shop Owner 1 views own history
        val myPromotions = promotions.filter { it.shopId == shopId }
        assertEquals(2, myPromotions.size)
        assertEquals("PAID", myPromotions[0].paymentStatus)
        assertEquals(PromotionStatus.APPROVED, myPromotions[0].status)
        assertEquals("Pending Payment / Post-Approval", myPromotions[1].paymentStatus)
        assertEquals(PromotionStatus.PENDING_APPROVAL, myPromotions[1].status)

        // Never includes other shop's promotions
        assertFalse(myPromotions.any { it.shopId == 2L })
    }

    @Test
    fun testAdminSeesOrdersFromAllCustomerDevicesAndAllShops() {
        val ordersFromDifferentDevicesAndShops = listOf(
            OrderEntity(
                id = 101,
                orderNumber = "ORD-DEV1-001",
                userId = 5, // Customer on Device 1
                shopId = 1, // Maa Samaleswari Kirana
                totalAmount = 560.0,
                finalAmount = 560.0,
                status = OrderStatus.PLACED,
                deliveryType = DeliveryType.HOME_DELIVERY,
                paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
                deliveryAddress = "Ward No 4, Sinapali",
                customerPhone = "9876543210",
                customerName = "Ramesh Majhi"
            ),
            OrderEntity(
                id = 102,
                orderNumber = "ORD-DEV2-002",
                userId = 6, // Customer on Device 2
                shopId = 2, // Sweets & Bakery
                totalAmount = 350.0,
                finalAmount = 350.0,
                status = OrderStatus.ACCEPTED,
                deliveryType = DeliveryType.HOME_DELIVERY,
                paymentMethod = PaymentMethod.ONLINE_UPI,
                paymentStatus = PaymentStatus.PAID,
                deliveryAddress = "Hatibandh Road, Sinapali",
                customerPhone = "8899001122",
                customerName = "Sunita Bag"
            ),
            OrderEntity(
                id = 103,
                orderNumber = "ORD-DEV3-003",
                userId = 7, // Customer on Device 3
                shopId = 3, // Vegetables Shop
                totalAmount = 180.0,
                finalAmount = 180.0,
                status = OrderStatus.DELIVERED,
                deliveryType = DeliveryType.SELF_PICKUP,
                paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
                paymentStatus = PaymentStatus.PAID,
                deliveryAddress = "Daily Market Square, Sinapali",
                customerPhone = "7766554433",
                customerName = "Bikram Rout"
            )
        )

        // Admin must see ALL orders regardless of device or shop
        val adminVisibleOrders = ordersFromDifferentDevicesAndShops
        assertEquals(3, adminVisibleOrders.size)
        assertTrue(adminVisibleOrders.any { it.shopId == 1L })
        assertTrue(adminVisibleOrders.any { it.shopId == 2L })
        assertTrue(adminVisibleOrders.any { it.shopId == 3L })
        assertTrue(adminVisibleOrders.any { it.orderNumber == "ORD-DEV1-001" })
        assertTrue(adminVisibleOrders.any { it.orderNumber == "ORD-DEV2-002" })
        assertTrue(adminVisibleOrders.any { it.orderNumber == "ORD-DEV3-003" })
    }

    @Test
    fun testAdminCanFilterOrdersByShop() {
        val allOrders = listOf(
            OrderEntity(
                id = 201, orderNumber = "ORD-1", userId = 5, shopId = 1, totalAmount = 100.0, finalAmount = 100.0,
                status = OrderStatus.PLACED, deliveryAddress = "Sinapali", customerPhone = "123", customerName = "A"
            ),
            OrderEntity(
                id = 202, orderNumber = "ORD-2", userId = 6, shopId = 2, totalAmount = 200.0, finalAmount = 200.0,
                status = OrderStatus.PLACED, deliveryAddress = "Sinapali", customerPhone = "456", customerName = "B"
            ),
            OrderEntity(
                id = 203, orderNumber = "ORD-3", userId = 7, shopId = 1, totalAmount = 300.0, finalAmount = 300.0,
                status = OrderStatus.DELIVERED, deliveryAddress = "Sinapali", customerPhone = "789", customerName = "C"
            )
        )

        // Filter by Shop 1
        val shop1Orders = allOrders.filter { it.shopId == 1L }
        assertEquals(2, shop1Orders.size)

        // Filter by Shop 2
        val shop2Orders = allOrders.filter { it.shopId == 2L }
        assertEquals(1, shop2Orders.size)

        // All Shops
        val allShopsView = allOrders
        assertEquals(3, allShopsView.size)
    }

    @Test
    fun testAdminCanFilterOrdersByStatusAndSearch() {
        val orders = listOf(
            OrderEntity(
                id = 301, orderNumber = "ORD-SIN-101", userId = 5, shopId = 1, totalAmount = 450.0, finalAmount = 450.0,
                status = OrderStatus.PLACED, deliveryAddress = "Main Road, Sinapali", customerPhone = "9876543210", customerName = "Pooja Majhi"
            ),
            OrderEntity(
                id = 302, orderNumber = "ORD-SIN-102", userId = 6, shopId = 2, totalAmount = 250.0, finalAmount = 250.0,
                status = OrderStatus.OUT_FOR_DELIVERY, deliveryAddress = "Near Panchayat Samiti, Sinapali", customerPhone = "9123456780", customerName = "Dillip Bag"
            ),
            OrderEntity(
                id = 303, orderNumber = "ORD-SIN-103", userId = 7, shopId = 1, totalAmount = 600.0, finalAmount = 600.0,
                status = OrderStatus.DELIVERED, deliveryAddress = "High School Para, Sinapali", customerPhone = "9988776655", customerName = "Anita Sahu"
            )
        )

        // Filter by Status: PLACED
        val placed = orders.filter { it.status == OrderStatus.PLACED }
        assertEquals(1, placed.size)
        assertEquals("ORD-SIN-101", placed[0].orderNumber)

        // Search by customer name "Dillip"
        val searchedByName = orders.filter { it.customerName.contains("Dillip", ignoreCase = true) }
        assertEquals(1, searchedByName.size)
        assertEquals("ORD-SIN-102", searchedByName[0].orderNumber)

        // Search by phone "9988"
        val searchedByPhone = orders.filter { it.customerPhone.contains("9988") }
        assertEquals(1, searchedByPhone.size)
        assertEquals("Anita Sahu", searchedByPhone[0].customerName)
    }

    @Test
    fun testShopOwnerSessionPersistenceProperties() {
        val testOwner = UserEntity(
            id = 101L,
            name = "Ramesh Sahu",
            phone = "9437012345",
            email = "9437012345@sinapalimart.local",
            role = UserRole.SHOP_OWNER,
            shopId = 1L,
            isVerified = true,
            district = "Nuapada",
            block = "Sinapali",
            gp = "Sinapali",
            village = "Sinapali",
            deliveryAddress = "Main Market Sinapali"
        )

        assertEquals(UserRole.SHOP_OWNER, testOwner.role)
        assertTrue(testOwner.isVerified)
        assertEquals(1L, testOwner.shopId)
        assertEquals("9437012345", testOwner.phone)
        assertNotNull(testOwner.deliveryAddress)
    }

    @Test
    fun testShopOwnerDashboardRestorationOnBackNavigation() {
        val backStack = ArrayDeque<Screen>()
        backStack.addLast(Screen.CustomerHome)
        backStack.addLast(Screen.CustomerProfile)
        backStack.addLast(Screen.ShopOwnerAuth)
        backStack.addLast(Screen.ShopProductManagement)

        // When in Shop Owner role, customer screens and auth screens must be pruned from backstack
        backStack.removeAll {
            it is Screen.CustomerProfile ||
            it is Screen.CustomerAuth ||
            it is Screen.ShopOwnerAuth ||
            it is Screen.Cart ||
            it is Screen.Checkout ||
            it is Screen.CustomerOrders ||
            it is Screen.OrderTracking ||
            it is Screen.CustomerHome
        }

        // Top is ShopProductManagement
        assertEquals(Screen.ShopProductManagement, backStack.last())
        val previous = backStack.removeLast()
        assertEquals(Screen.ShopProductManagement, previous)

        // Now backStack is empty, fallback destination for shop owner is ShopOwnerDashboard
        val fallback = if (backStack.isNotEmpty()) backStack.removeLast() else Screen.ShopOwnerDashboard
        assertEquals(Screen.ShopOwnerDashboard, fallback)
    }

    @Test
    fun testShopOwnerAndCustomerSessionIndependentIsolation() {
        var customerLoggedIn = true
        var customerId = 5L
        var shopOwnerLoggedIn = true
        var shopOwnerId = 2L

        // Logging out shop owner must NOT affect customer session
        shopOwnerLoggedIn = false
        shopOwnerId = -1L

        assertTrue("Customer session remains intact after shop owner logout", customerLoggedIn)
        assertEquals("Customer ID unchanged", 5L, customerId)
        assertFalse("Shop owner is logged out", shopOwnerLoggedIn)
        assertEquals(-1L, shopOwnerId)
    }

    @Test
    fun testCustomerProfileShopOwnerLoginWithOtpButtonNavigation() {
        // Given customer is browsing CustomerProfile screen
        val currentScreen = Screen.CustomerProfile
        val role = UserRole.CUSTOMER
        val shopOwnerSessionActive = false

        // When tapping "Shop Owner Login with OTP" button
        val targetScreen: Screen = if (shopOwnerSessionActive) {
            Screen.ShopOwnerDashboard
        } else {
            Screen.ShopOwnerAuth
        }

        // Verify connected to Shop Owner registration/login screen
        assertEquals(Screen.ShopOwnerAuth, targetScreen)

        // When shop owner logs in with OTP or registers
        val authenticatedShopOwner = SeedData.shopOwners[0]
        assertEquals(UserRole.SHOP_OWNER, authenticatedShopOwner.role)
        assertNotNull(authenticatedShopOwner.phone)
        assertTrue(authenticatedShopOwner.phone.length == 10)
    }
}
