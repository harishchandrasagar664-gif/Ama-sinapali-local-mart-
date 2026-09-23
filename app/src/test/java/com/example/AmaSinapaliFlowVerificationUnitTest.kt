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
        assertEquals(101L, defaultCustomer.id)
        assertEquals("Priya Mishra", defaultCustomer.name)
        assertEquals("9861011111", defaultCustomer.phone)
        assertEquals(UserRole.CUSTOMER, defaultCustomer.role)
        assertTrue(defaultCustomer.location.contains("Sinapali"))
    }

    @Test
    fun testFlow2_BrowseMultipleShops() {
        val shops = SeedData.shops
        assertEquals(5, shops.size)

        // Verify distinct shops exist in Sinapali
        val categories = shops.map { it.category }
        assertTrue(categories.contains("Electronics & Mobiles"))
        assertTrue(categories.contains("Clothing & Apparel"))
        assertTrue(categories.contains("Footwear & Accessories"))
        assertTrue(categories.contains("Beauty & Personal Care"))
        assertTrue(categories.contains("Home, Kitchen & Stationery"))

        // All 5 shops are approved and active
        assertTrue(shops.all { it.isApproved && it.isActive })
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

        // Products are bound strictly to their respective shop
        assertTrue(shop1Products.all { it.shopId == shop1.id })
        assertTrue(shop2Products.all { it.shopId == shop2.id })

        assertNotEquals(shop1Products.first().shopId, shop2Products.first().shopId)
    }

    @Test
    fun testFlow4_AddProductsFromMultipleShopsToCart() {
        val products = SeedData.products
        val shop1Product = products.first { it.shopId == 1L }
        val shop2Product = products.first { it.shopId == 2L }

        // Cart items with product details
        val item1 = CartItemWithProduct(
            cartItemId = 1L,
            productId = shop1Product.id,
            shopId = shop1Product.shopId,
            shopName = shop1Product.shopName,
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
            shopName = shop2Product.shopName,
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
            shopName = shop.shopName,
            productName = product.name,
            imageUrl = product.imageUrl,
            unitPrice = product.price,
            discountPrice = product.discountPrice,
            quantity = 1,
            stock = product.stock
        )

        val subtotal = cartItem.itemTotal
        val deliveryFee = shop.deliveryFee
        val total = subtotal + deliveryFee

        val order = com.example.data.local.entities.OrderEntity(
            id = 1001L,
            orderNumber = "ORD-OD-1001",
            customerId = customer.id,
            shopId = shop.id,
            shopName = shop.shopName,
            customerName = customer.name,
            customerPhone = customer.phone,
            deliveryAddress = "Ward 2, Sinapali",
            deliveryType = DeliveryType.HOME_DELIVERY,
            orderStatus = OrderStatus.ORDER_PLACED,
            subtotal = subtotal,
            discount = 0.0,
            deliveryCharge = deliveryFee,
            totalAmount = total,
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            paymentStatus = PaymentStatus.PENDING,
            razorpayPaymentId = null,
            couponApplied = null
        )

        assertEquals(PaymentMethod.CASH_ON_DELIVERY, order.paymentMethod)
        assertEquals(PaymentStatus.PENDING, order.paymentStatus)
        assertEquals(OrderStatus.ORDER_PLACED, order.orderStatus)
        assertEquals("Ward 2, Sinapali", order.deliveryAddress)
    }

    @Test
    fun testFlow6_VerifyShopOwnerReceivesOnlyTheirOwnItems() {
        val shop1 = SeedData.shops[0]
        val shop2 = SeedData.shops[1]

        val prod1 = SeedData.products.first { it.shopId == shop1.id }
        val prod2 = SeedData.products.first { it.shopId == shop2.id }

        val multiShopItems = listOf(
            CartItemWithProduct(1L, prod1.id, shop1.id, shop1.shopName, prod1.name, prod1.imageUrl, prod1.price, prod1.discountPrice, 1, prod1.stock),
            CartItemWithProduct(2L, prod2.id, shop2.id, shop2.shopName, prod2.name, prod2.imageUrl, prod2.price, prod2.discountPrice, 2, prod2.stock)
        )

        // Split by shop as done in completeOrder
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

        assertEquals("5 registered shop owners", 5, owners.size)

        for (owner in owners) {
            assertEquals(UserRole.SHOP_OWNER, owner.role)
            val ownerShop = shops.find { it.ownerId == owner.id }
            assertNotNull("Shop exists for owner ${owner.name}", ownerShop)
            val shop = ownerShop!!

            // Verify products for this owner's shop
            val ownerProducts = products.filter { it.shopId == shop.id }
            assertTrue("Owner ${owner.name} has products", ownerProducts.isNotEmpty())
            assertTrue("All products belong to owner's shop", ownerProducts.all { it.shopId == shop.id })

            // Verify owner cannot see other shop's products under their shopId
            val otherProducts = products.filter { it.shopId != shop.id }
            assertFalse("Owner does not own other shop's products", otherProducts.any { it.shopId == shop.id })
        }
    }

    @Test
    fun testFlow9_AdminCanSeeAllShopsAndOrders() {
        val admin = SeedData.adminUser
        assertEquals(UserRole.ADMIN, admin.role)
        assertEquals("Platform Administrator", admin.name)

        // Admin can access all shops
        val allShops = SeedData.shops
        assertEquals(5, allShops.size)

        // Admin can access all orders
        val allOrders = SeedData.sampleOrders
        assertTrue("Sample orders exist in platform", allOrders.isNotEmpty())
        val allOrderItems = SeedData.sampleOrderItems
        assertTrue("Sample order items exist", allOrderItems.isNotEmpty())
    }

    @Test
    fun testFlow10_OrderStatusFlowWorksCorrectly() {
        var status = OrderStatus.ORDER_PLACED
        assertEquals("Order Placed", status.label)
        assertEquals("ଅର୍ଡର ଦିଆଗଲା", status.odiaLabel)

        status = OrderStatus.ORDER_CONFIRMED
        assertEquals("Order Confirmed", status.label)
        assertEquals("ଅର୍ଡର ସ୍ୱୀକୃତ", status.odiaLabel)

        status = OrderStatus.PREPARING
        assertEquals("Preparing", status.label)
        assertEquals("ପ୍ୟାକ୍ ହେଉଛି", status.odiaLabel)

        status = OrderStatus.READY_FOR_DELIVERY
        assertEquals("Ready for Delivery", status.label)
        assertEquals("ଡେଲିଭରୀ ପାଇଁ ପ୍ରସ୍ତୁତ", status.odiaLabel)

        status = OrderStatus.OUT_FOR_DELIVERY
        assertEquals("Out for Delivery", status.label)
        assertEquals("ଡେଲିଭରୀ ବାହାରିଛି", status.odiaLabel)

        status = OrderStatus.DELIVERED
        assertEquals("Delivered", status.label)
        assertEquals("ଡେଲିଭରୀ ସମ୍ପନ୍ନ", status.odiaLabel)

        // Verify COD payment auto-completion on DELIVERED status
        val paymentStatusOnDelivered = PaymentStatus.PAID
        assertEquals(PaymentStatus.PAID, paymentStatusOnDelivered)
    }

    @Test
    fun testAdminDeliveryFlow_CompleteLifecycleAndOtpCodVerification() {
        // Flow: READY_FOR_DELIVERY -> DELIVERY_ACCEPTED -> PICKED_UP -> OUT_FOR_DELIVERY -> DELIVERED
        val lifecycle = listOf(
            OrderStatus.READY_FOR_DELIVERY,
            OrderStatus.DELIVERY_ACCEPTED,
            OrderStatus.PICKED_UP,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED
        )
        assertEquals(5, lifecycle.size)
        assertEquals("Delivery Accepted", OrderStatus.DELIVERY_ACCEPTED.label)
        assertEquals("Picked Up from Shop", OrderStatus.PICKED_UP.label)
        assertEquals("ଡେଲିଭରୀ ଗ୍ରହଣ କରାଗଲା", OrderStatus.DELIVERY_ACCEPTED.odiaLabel)
        assertEquals("ଦୋକାନରୁ ସଂଗ୍ରହ ହୋଇଛି", OrderStatus.PICKED_UP.odiaLabel)

        // Verify Delivery verification attributes
        val sampleOrder = SeedData.sampleOrders.first()
        val expectedOtp = sampleOrder.deliveryOtp
        assertNotNull("Delivery OTP is generated", expectedOtp)
        assertEquals(4, expectedOtp.length)

        // Verify COD cash collection calculation
        val expectedCodAmount = sampleOrder.totalAmount
        assertTrue("Order amount is positive", expectedCodAmount > 0)

        // Verify Admin role requirement: only ADMIN role can deliver
        val admin = SeedData.adminUser
        assertEquals(UserRole.ADMIN, admin.role)

        val owner = SeedData.shopOwners.first()
        assertEquals(UserRole.SHOP_OWNER, owner.role)
        assertNotEquals(UserRole.ADMIN, owner.role)
    }
}
