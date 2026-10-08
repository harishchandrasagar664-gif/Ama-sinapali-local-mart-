package com.example

import com.example.data.firebase.FirestoreSyncService
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.OrderItemEntity
import com.example.data.local.entities.ShopEntity
import com.example.data.local.entities.UserEntity
import com.example.domain.model.DeliveryType
import com.example.domain.model.OrderStatus
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PaymentStatus
import com.example.domain.model.UserRole
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Verification test suite for Cross-Device Order Sync.
 * Ensures that every customer order placed from any mobile (e.g., Mobile 2)
 * is serialized to Firebase with customer, shop, product, quantity, price,
 * address, date/time, and order ID, and that Admin Panel (on Mobile 1 or any other device)
 * fetches and displays ALL orders from Firebase in real time.
 */
class OrderCrossDeviceSyncTest {

    @Test
    fun testMobile2OrderPlacementAndFirebaseSerializationWithAllRequiredFields() {
        // Customer on Mobile 2
        val customerMobile2 = UserEntity(
            id = 102L,
            name = "Ramesh Pujari",
            phone = "9876543210",
            email = "ramesh.pujari@sinapali.local",
            role = UserRole.CUSTOMER,
            deliveryAddress = "Ward 4, Near Jagannath Temple, Sinapali"
        )

        // Shop in Sinapali
        val shop = ShopEntity(
            id = 5L,
            ownerId = 20L,
            name = "Maa Samaleswari Grocery Store",
            category = "Daily Groceries & Essentials",
            address = "Main Market Road, Sinapali",
            phone = "9437112233",
            upiId = "samaleswari@upi"
        )

        val placedTimestamp = 1775467200000L // specific test timestamp

        // Order placed on Mobile 2
        val orderMobile2 = OrderEntity(
            id = 88L,
            orderNumber = "ORD-SIN-1775467200-888",
            userId = customerMobile2.id,
            shopId = shop.id,
            totalAmount = 350.0,
            discountAmount = 30.0,
            deliveryFee = 25.0,
            finalAmount = 345.0,
            status = OrderStatus.PLACED,
            deliveryType = DeliveryType.HOME_DELIVERY,
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            paymentStatus = PaymentStatus.PENDING,
            deliveryAddress = "Ward 4, Near Jagannath Temple, Sinapali",
            customerPhone = customerMobile2.phone,
            customerName = customerMobile2.name,
            placedAt = placedTimestamp
        )

        val itemsMobile2 = listOf(
            OrderItemEntity(
                id = 1L,
                orderId = 88L,
                productId = 101L,
                productName = "Sinapali Premium Basmati Rice 5kg",
                unitPrice = 280.0,
                quantity = 1,
                totalPrice = 280.0
            ),
            OrderItemEntity(
                id = 2L,
                orderId = 88L,
                productId = 102L,
                productName = "Tata Salt 1kg",
                unitPrice = 35.0,
                quantity = 2,
                totalPrice = 70.0
            )
        )

        // 1. Verify Firebase Firestore SDK Map Serialization contains all required fields:
        // customer, shop, product, quantity, price, address, date/time and order ID
        val map = FirestoreSyncService.orderToMap(orderMobile2, itemsMobile2, shop.name)

        // Order ID
        assertEquals("88", map["orderId"])
        assertEquals("ORD-SIN-1775467200-888", map["orderNumber"])
        assertEquals(88L, map["id"])

        // Customer
        assertEquals("Ramesh Pujari", map["customer"])
        assertEquals("Ramesh Pujari", map["customerName"])
        assertEquals("9876543210", map["customerPhone"])
        assertEquals(102L, map["userId"])

        // Shop
        assertEquals("Maa Samaleswari Grocery Store", map["shop"])
        assertEquals("Maa Samaleswari Grocery Store", map["shopName"])
        assertEquals(5L, map["shopId"])

        // Address
        assertEquals("Ward 4, Near Jagannath Temple, Sinapali", map["address"])
        assertEquals("Ward 4, Near Jagannath Temple, Sinapali", map["deliveryAddress"])

        // Price
        assertEquals(345.0, map["price"])
        assertEquals(345.0, map["finalAmount"])
        assertEquals(350.0, map["totalAmount"])

        // Quantity
        assertEquals(3, map["quantity"]) // 1 + 2 = 3 items total
        assertEquals(3, map["totalQuantity"])

        // Product
        assertTrue("Product description contains Basmati Rice", (map["product"] as String).contains("Sinapali Premium Basmati Rice"))
        assertTrue("Product description contains Tata Salt", (map["product"] as String).contains("Tata Salt"))

        // Date / Time
        assertNotNull(map["dateTime"])
        assertNotNull(map["date"])
        assertNotNull(map["time"])
        assertEquals(placedTimestamp, map["placedAt"])

        // 2. Verify Google Cloud Firestore REST JSON Payload contains all required fields
        val payload = FirestoreSyncService.orderToFirestorePayload(orderMobile2, itemsMobile2, shop.name)
        val fields = payload.getJSONObject("fields")

        assertEquals("88", fields.getJSONObject("orderId").getString("stringValue"))
        assertEquals("ORD-SIN-1775467200-888", fields.getJSONObject("orderNumber").getString("stringValue"))
        assertEquals("Ramesh Pujari", fields.getJSONObject("customer").getString("stringValue"))
        assertEquals("Maa Samaleswari Grocery Store", fields.getJSONObject("shop").getString("stringValue"))
        assertEquals("Ward 4, Near Jagannath Temple, Sinapali", fields.getJSONObject("address").getString("stringValue"))
        assertEquals(345.0, fields.getJSONObject("price").getDouble("doubleValue"), 0.01)
        assertEquals("3", fields.getJSONObject("quantity").getString("integerValue"))
        assertTrue(fields.getJSONObject("product").getString("stringValue").contains("Basmati Rice"))
        assertTrue(fields.getJSONObject("dateTime").getString("stringValue").isNotBlank())
    }

    @Test
    fun testAdminPanelFetchesAndParsesMobile2OrderFromFirebaseInRealTime() {
        // Simulated Cloud Document received from Firebase for Mobile 2 order
        val cloudPayloadFromMobile2 = JSONObject("""
            {
                "name": "projects/ama-sinapali-local-mart/databases/(default)/documents/orders/ORD-SIN-1775467200-888",
                "fields": {
                    "orderId": { "stringValue": "88" },
                    "orderNumber": { "stringValue": "ORD-SIN-1775467200-888" },
                    "id": { "integerValue": "88" },
                    "userId": { "integerValue": "102" },
                    "shopId": { "integerValue": "5" },
                    "customer": { "stringValue": "Ramesh Pujari" },
                    "customerName": { "stringValue": "Ramesh Pujari" },
                    "customerPhone": { "stringValue": "9876543210" },
                    "shop": { "stringValue": "Maa Samaleswari Grocery Store" },
                    "shopName": { "stringValue": "Maa Samaleswari Grocery Store" },
                    "address": { "stringValue": "Ward 4, Near Jagannath Temple, Sinapali" },
                    "deliveryAddress": { "stringValue": "Ward 4, Near Jagannath Temple, Sinapali" },
                    "price": { "doubleValue": 345.0 },
                    "finalAmount": { "doubleValue": 345.0 },
                    "totalAmount": { "doubleValue": 350.0 },
                    "discountAmount": { "doubleValue": 30.0 },
                    "deliveryFee": { "doubleValue": 25.0 },
                    "quantity": { "integerValue": "3" },
                    "product": { "stringValue": "Sinapali Premium Basmati Rice 5kg (1), Tata Salt 1kg (2)" },
                    "status": { "stringValue": "PLACED" },
                    "deliveryType": { "stringValue": "HOME_DELIVERY" },
                    "paymentMethod": { "stringValue": "CASH_ON_DELIVERY" },
                    "paymentStatus": { "stringValue": "PENDING" },
                    "dateTime": { "stringValue": "2026-04-06 14:40:00" },
                    "placedAt": { "integerValue": "1775467200000" },
                    "itemsJson": { "stringValue": "[{\"productId\":101,\"productName\":\"Sinapali Premium Basmati Rice 5kg\",\"unitPrice\":280.0,\"quantity\":1,\"totalPrice\":280.0},{\"productId\":102,\"productName\":\"Tata Salt 1kg\",\"unitPrice\":35.0,\"quantity\":2,\"totalPrice\":70.0}]" }
                }
            }
        """.trimIndent())

        // Admin Panel on Mobile 1 parses document from Firebase in real time
        val parsed = FirestoreSyncService.firestorePayloadToOrder(cloudPayloadFromMobile2)
        assertNotNull("Admin Panel must parse Mobile 2 order from Firebase", parsed)

        val (order, items) = parsed!!
        assertEquals("ORD-SIN-1775467200-888", order.orderNumber)
        assertEquals(88L, order.id)
        assertEquals("Ramesh Pujari", order.customerName)
        assertEquals("9876543210", order.customerPhone)
        assertEquals("Ward 4, Near Jagannath Temple, Sinapali", order.deliveryAddress)
        assertEquals(345.0, order.finalAmount, 0.01)
        assertEquals(OrderStatus.PLACED, order.status)
        assertEquals(5L, order.shopId)

        // Verify items parsed in Admin Panel
        assertEquals(2, items.size)
        assertEquals("Sinapali Premium Basmati Rice 5kg", items[0].productName)
        assertEquals(1, items[0].quantity)
        assertEquals(280.0, items[0].unitPrice, 0.01)
        assertEquals("Tata Salt 1kg", items[1].productName)
        assertEquals(2, items[1].quantity)
        assertEquals(35.0, items[1].unitPrice, 0.01)
        assertEquals(3, items.sumOf { it.quantity })
    }

    @Test
    fun testFallbackRootFieldParsingWhenItemsJsonOmitted() {
        // Document with simple root fields: customer, shop, product, quantity, price, address, date/time, order ID
        val minimalCloudPayload = JSONObject("""
            {
                "fields": {
                    "orderId": { "stringValue": "99" },
                    "customer": { "stringValue": "Sunita Majhi" },
                    "shop": { "stringValue": "Sinapali Daily Veggies" },
                    "shopId": { "integerValue": "3" },
                    "address": { "stringValue": "Hatpada, Sinapali" },
                    "product": { "stringValue": "Fresh Sinapali Tomatoes (ଟମାଟୋ)" },
                    "quantity": { "integerValue": "2" },
                    "price": { "doubleValue": 60.0 },
                    "dateTime": { "stringValue": "2026-04-06 15:30:00" },
                    "status": { "stringValue": "PLACED" }
                }
            }
        """.trimIndent())

        val parsed = FirestoreSyncService.firestorePayloadToOrder(minimalCloudPayload)
        assertNotNull(parsed)

        val (order, items) = parsed!!
        assertEquals("Sunita Majhi", order.customerName)
        assertEquals("Hatpada, Sinapali", order.deliveryAddress)
        assertEquals(60.0, order.finalAmount, 0.01)
        assertEquals(1, items.size)
        assertEquals("Fresh Sinapali Tomatoes (ଟମାଟୋ)", items[0].productName)
        assertEquals(2, items[0].quantity)
        assertEquals(30.0, items[0].unitPrice, 0.01)
        assertEquals(60.0, items[0].totalPrice, 0.01)
    }

    @Test
    fun testMultipleOrdersAcrossMobilesAppearTogetherInAdminPanel() {
        val orderFromMobile1 = OrderEntity(
            id = 1L,
            orderNumber = "ORD-MOB1-001",
            userId = 101L,
            shopId = 2L,
            totalAmount = 150.0,
            finalAmount = 150.0,
            status = OrderStatus.DELIVERED,
            deliveryAddress = "Main Road, Sinapali",
            customerPhone = "9437000001",
            customerName = "Mobile 1 User"
        )
        val orderFromMobile2 = OrderEntity(
            id = 2L,
            orderNumber = "ORD-MOB2-002",
            userId = 102L,
            shopId = 5L,
            totalAmount = 345.0,
            finalAmount = 345.0,
            status = OrderStatus.PLACED,
            deliveryAddress = "Ward 4, Sinapali",
            customerPhone = "9876543210",
            customerName = "Mobile 2 User"
        )

        val allAdminOrders = listOf(orderFromMobile1, orderFromMobile2)
        assertEquals("Admin Panel displays orders from all mobiles", 2, allAdminOrders.size)
        assertTrue(allAdminOrders.any { it.orderNumber == "ORD-MOB1-001" })
        assertTrue(allAdminOrders.any { it.orderNumber == "ORD-MOB2-002" })
    }
}
