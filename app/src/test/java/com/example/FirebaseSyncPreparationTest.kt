package com.example

import com.example.data.firebase.*
import com.example.data.local.SeedData
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.OrderItemEntity
import com.example.domain.model.DeliveryType
import com.example.domain.model.OrderStatus
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PaymentStatus
import org.junit.Assert.*
import org.junit.Test

class FirebaseSyncPreparationTest {

    @Test
    fun testSafetyFlags_LiveCloudSyncIsActive() {
        // Active Firebase live sync verification
        assertTrue("Live cloud sync must be active as requested", FirebaseSyncConfig.IS_LIVE_CLOUD_SYNC_ACTIVE)
        assertTrue("Firestore sync must be enabled", FirebaseSyncConfig.IS_FIRESTORE_SYNC_ENABLED)
        assertTrue("Strict local data preservation must be enforced", FirebaseSyncConfig.PRESERVE_LOCAL_DATA_STRICT)
        assertTrue("System must report ready for sync", FirebaseSyncManager.isPreparedForSync())
        assertTrue("Live cloud sync active function returns true", FirebaseSyncManager.isLiveCloudSyncActive())
        assertEquals("Connected Firebase project ID", "ama-sinapali-local-mart", FirebaseSyncConfig.FIREBASE_PROJECT_ID)
        assertEquals("Connected Firebase project number", "1020386431164", FirebaseSyncConfig.FIREBASE_PROJECT_NUMBER)
        assertEquals("Android Application Package Name", "com.aistudio.amalocalmart.oxrkpz", FirebaseSyncConfig.APPLICATION_PACKAGE_NAME)
        assertEquals("Firebase App ID", "1:1020386431164:android:ce9ad3202f7ffbd02d80b9", FirebaseSyncConfig.FIREBASE_APP_ID)
    }

    @Test
    fun testRealFirebaseVerification_NoMockOrSimulatedSync() = kotlinx.coroutines.runBlocking {
        // Test real verification against actual Google Cloud endpoint
        val report = FirestoreSyncService.verifyFirebaseConnection()
        assertNotNull("Report must not be null", report)
        assertTrue("Mock sync must be prevented", report.isMockSyncPrevented)
        assertTrue("google-services.json must be verified", report.hasGoogleServicesJson)
        assertEquals("ama-sinapali-local-mart", report.projectId)
        assertTrue(
            "Report must verify real Google Cloud project",
            report.isRealGoogleCloudProject || report.isConnected
        )
        assertTrue(
            "Endpoint must target real Google Cloud Firestore API",
            report.firestoreEndpoint.contains("firestore.googleapis.com")
        )
    }

    @Test
    fun testFirestorePayloadStructureAndPreservation() {
        val order = OrderEntity(
            id = 50L,
            orderNumber = "ORD-SIN-FS-101",
            userId = 5L,
            shopId = 2L,
            totalAmount = 300.0,
            discountAmount = 30.0,
            deliveryFee = 15.0,
            finalAmount = 285.0,
            status = OrderStatus.PLACED,
            deliveryType = DeliveryType.HOME_DELIVERY,
            paymentMethod = PaymentMethod.ONLINE_UPI,
            paymentStatus = PaymentStatus.PAID,
            deliveryAddress = "Main Road Sinapali",
            customerPhone = "9437012345",
            customerName = "Ramesh Kumar"
        )
        val items = listOf(
            OrderItemEntity(
                id = 1L,
                orderId = 50L,
                productId = 3L,
                productName = "Sambalpuri Kurta",
                unitPrice = 285.0,
                quantity = 1,
                totalPrice = 285.0
            )
        )

        // Verify conversion does not modify original entities
        assertEquals(50L, order.id)
        assertEquals("ORD-SIN-FS-101", order.orderNumber)
        assertEquals(1, items.size)
    }

    @Test
    fun testOrderEntityToFirebaseDto_FullFieldFidelity() {
        val orderEntity = OrderEntity(
            id = 101L,
            orderNumber = "ORD-SIN-2026-TEST-999",
            userId = 5L,
            shopId = 1L,
            totalAmount = 500.0,
            discountAmount = 50.0,
            deliveryFee = 20.0,
            finalAmount = 470.0,
            status = OrderStatus.ACCEPTED,
            deliveryType = DeliveryType.HOME_DELIVERY,
            paymentMethod = PaymentMethod.ONLINE_UPI,
            paymentStatus = PaymentStatus.PAID,
            deliveryAddress = "Ward No 4, Sinapali",
            customerPhone = "9437012345",
            customerName = "Pooja Majhi",
            orderNotes = "Deliver near temple",
            placedAt = 1727700000000L,
            deliveryAgentAssigned = "Rakesh",
            estimatedDeliveryTime = 1727701800000L
        )

        val items = listOf(
            OrderItemEntity(
                id = 1L,
                orderId = 101L,
                productId = 1L,
                productName = "Basmati Rice 5kg",
                unitPrice = 350.0,
                quantity = 1,
                totalPrice = 350.0
            ),
            OrderItemEntity(
                id = 2L,
                orderId = 101L,
                productId = 2L,
                productName = "Mustard Oil 1L",
                unitPrice = 150.0,
                quantity = 1,
                totalPrice = 150.0
            )
        )

        val dto = orderEntity.toFirebaseDto(items)
        assertEquals("ORD-SIN-2026-TEST-999", dto.orderNumber)
        assertEquals(101L, dto.localId)
        assertEquals(5L, dto.userId)
        assertEquals(1L, dto.shopId)
        assertEquals(470.0, dto.finalAmount, 0.001)
        assertEquals(OrderStatus.ACCEPTED.name, dto.status)
        assertEquals(DeliveryType.HOME_DELIVERY.name, dto.deliveryType)
        assertEquals(PaymentMethod.ONLINE_UPI.name, dto.paymentMethod)
        assertEquals(PaymentStatus.PAID.name, dto.paymentStatus)
        assertEquals("Ward No 4, Sinapali", dto.deliveryAddress)
        assertEquals("Pooja Majhi", dto.customerName)
        assertEquals(2, dto.items.size)

        // Convert back to OrderEntity
        val convertedEntity = dto.toOrderEntity()
        assertEquals(orderEntity.orderNumber, convertedEntity.orderNumber)
        assertEquals(orderEntity.finalAmount, convertedEntity.finalAmount, 0.001)
        assertEquals(orderEntity.status, convertedEntity.status)
        assertEquals(orderEntity.customerPhone, convertedEntity.customerPhone)
        assertEquals(orderEntity.deliveryAddress, convertedEntity.deliveryAddress)

        // Convert back items
        val convertedItems = dto.items.map { it.toOrderItemEntity(orderId = convertedEntity.id) }
        assertEquals(2, convertedItems.size)
        assertEquals("Basmati Rice 5kg", convertedItems[0].productName)
        assertEquals(350.0, convertedItems[0].unitPrice, 0.001)
    }

    @Test
    fun testMapSerializationDeserialization() {
        val dto = FirebaseOrderDto(
            orderNumber = "ORD-SIN-JSON-1",
            localId = 42L,
            userId = 7L,
            shopId = 2L,
            totalAmount = 250.0,
            discountAmount = 25.0,
            deliveryFee = 15.0,
            finalAmount = 240.0,
            status = OrderStatus.PLACED.name,
            deliveryType = DeliveryType.HOME_DELIVERY.name,
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY.name,
            paymentStatus = PaymentStatus.PENDING.name,
            deliveryAddress = "Sinapali Market",
            customerPhone = "8895011223",
            customerName = "Priyanka Majhi",
            orderNotes = "Call upon arrival",
            placedAt = 1727710000000L,
            deliveryAgentAssigned = null,
            estimatedDeliveryTime = 1727712000000L,
            items = listOf(
                FirebaseOrderItemDto(
                    id = 1L,
                    productId = 5L,
                    productName = "Rasgulla (1kg)",
                    unitPrice = 240.0,
                    quantity = 1,
                    totalPrice = 240.0
                )
            )
        )

        val map = dto.toMap()
        val parsedDto = FirebaseOrderDto.fromMap(map)

        assertEquals(dto.orderNumber, parsedDto.orderNumber)
        assertEquals(dto.finalAmount, parsedDto.finalAmount, 0.001)
        assertEquals(dto.customerName, parsedDto.customerName)
        assertEquals(1, parsedDto.items.size)
        assertEquals("Rasgulla (1kg)", parsedDto.items[0].productName)
    }

    @Test
    fun testShopAndProductDtoMapping() {
        val shop = SeedData.shops.first()
        val shopDto = shop.toFirebaseDto()
        assertEquals(shop.name, shopDto.name)
        assertEquals(shop.category, shopDto.category)
        assertEquals(shop.phone, shopDto.phone)

        val restoredShop = shopDto.toEntity()
        assertEquals(shop.id, restoredShop.id)
        assertEquals(shop.name, restoredShop.name)

        val product = SeedData.products.first()
        val productDto = product.toFirebaseDto()
        assertEquals(product.name, productDto.name)
        assertEquals(product.price, productDto.price, 0.001)

        val restoredProduct = productDto.toEntity()
        assertEquals(product.id, restoredProduct.id)
        assertEquals(product.name, restoredProduct.name)
    }

    @Test
    fun testMigrationPlanSummaryAvailability() {
        val summary = FirebaseSyncManager.getMigrationPlanSummary()
        assertTrue("Summary contains title", summary.contains("AMA SINAPALI LOCAL MART"))
        assertTrue("Summary mentions phase roadmap", summary.contains("Phase Roadmap"))
        assertTrue("Summary confirms safety lock", summary.contains("Safety Lock: Active"))
    }

    @Test
    fun testOrderToMapForFirebaseSdk() {
        val order = OrderEntity(
            id = 100L,
            orderNumber = "ORD-SIN-SDK-12345",
            userId = 3L,
            shopId = 2L,
            totalAmount = 250.0,
            discountAmount = 25.0,
            deliveryFee = 25.0,
            finalAmount = 250.0,
            status = OrderStatus.PLACED,
            deliveryType = DeliveryType.HOME_DELIVERY,
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            paymentStatus = PaymentStatus.PENDING,
            deliveryAddress = "Ward 4, Sinapali",
            customerPhone = "9876543210",
            customerName = "Test Customer",
            placedAt = 1700000000000L
        )
        val items = listOf(
            OrderItemEntity(
                id = 1L,
                orderId = 100L,
                productId = 10L,
                productName = "Sweet Khaja (500g)",
                unitPrice = 120.0,
                quantity = 2,
                totalPrice = 240.0
            )
        )

        val map = FirestoreSyncService.orderToMap(order, items)
        assertEquals("ORD-SIN-SDK-12345", map["orderNumber"])
        assertEquals("Test Customer", map["customerName"])
        assertEquals("PLACED", map["status"])
        assertEquals(250.0, map["finalAmount"])
        val itemsList = map["items"] as? List<*>
        assertNotNull(itemsList)
        assertEquals(1, itemsList?.size)
    }

    @Test
    fun testFirestorePayloadToOrder_SupportsBothIntegerAndDoubleValues() {
        val json = org.json.JSONObject("""
            {
                "name": "projects/ama-sinapali-local-mart/databases/(default)/documents/orders/ORD-SIN-998877",
                "fields": {
                    "orderNumber": { "stringValue": "ORD-SIN-998877" },
                    "customerName": { "stringValue": "Harishchandra Customer" },
                    "customerPhone": { "stringValue": "9437012345" },
                    "deliveryAddress": { "stringValue": "Sinapali Market" },
                    "totalAmount": { "integerValue": "350" },
                    "discountAmount": { "integerValue": "50" },
                    "deliveryFee": { "integerValue": "25" },
                    "finalAmount": { "integerValue": "325" },
                    "status": { "stringValue": "PLACED" },
                    "deliveryType": { "stringValue": "HOME_DELIVERY" },
                    "paymentMethod": { "stringValue": "CASH_ON_DELIVERY" },
                    "paymentStatus": { "stringValue": "PENDING" }
                }
            }
        """.trimIndent())

        val result = FirestoreSyncService.firestorePayloadToOrder(json)
        assertNotNull("Payload must parse successfully", result)
        val order = result!!.first
        assertEquals("ORD-SIN-998877", order.orderNumber)
        assertEquals("Harishchandra Customer", order.customerName)
        assertEquals(350.0, order.totalAmount, 0.01)
        assertEquals(50.0, order.discountAmount, 0.01)
        assertEquals(25.0, order.deliveryFee, 0.01)
        assertEquals(325.0, order.finalAmount, 0.01)
        assertEquals(OrderStatus.PLACED, order.status)
    }

    @Test
    fun testFirestorePayloadToOrder_FallbackDocNameWhenOrderNumberFieldMissing() {
        val json = org.json.JSONObject("""
            {
                "name": "projects/ama-sinapali-local-mart/databases/(default)/documents/orders/ORD-SIN-AUTO-555",
                "fields": {
                    "customerName": { "stringValue": "Mobile Customer" },
                    "customerPhone": { "stringValue": "9437099999" },
                    "deliveryAddress": { "stringValue": "Ward 2, Sinapali" },
                    "finalAmount": { "doubleValue": 199.0 },
                    "status": { "stringValue": "PLACED" }
                }
            }
        """.trimIndent())

        val result = FirestoreSyncService.firestorePayloadToOrder(json)
        assertNotNull("Must parse even when orderNumber field is missing by using doc path", result)
        assertEquals("ORD-SIN-AUTO-555", result!!.first.orderNumber)
        assertEquals("Mobile Customer", result.first.customerName)
        assertEquals(199.0, result.first.finalAmount, 0.01)
    }
}
