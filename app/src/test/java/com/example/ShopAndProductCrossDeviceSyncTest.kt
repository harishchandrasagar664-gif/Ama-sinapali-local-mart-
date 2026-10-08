package com.example

import com.example.data.firebase.FirestoreSyncService
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.ShopEntity
import com.example.data.local.entities.UserEntity
import com.example.domain.model.UserRole
import org.junit.Assert.*
import org.junit.Test

/**
 * Cross-device sync verification tests ensuring that when Mobile 1 adds/updates
 * a shop or product, the data is properly serialized for Firestore (including product
 * image URL and shop ID), and Mobile 2 receives and displays all active shops and products in real time.
 */
class ShopAndProductCrossDeviceSyncTest {

    @Test
    fun testMobile1ShopCreationAndFirestoreSerialization() {
        val ownerMobile1 = UserEntity(
            id = 55L,
            name = "Bipin Bihari Sahu",
            phone = "9437088990",
            email = "bipin@sinapalimart.local",
            role = UserRole.SHOP_OWNER,
            isVerified = true
        )

        val shopMobile1 = ShopEntity(
            id = 10L,
            ownerId = ownerMobile1.id,
            name = "Sinapali Fresh Organics",
            category = "Fresh Vegetables & Fruits",
            address = "Near Panchayat High School, Sinapali",
            phone = "9437088990",
            upiId = "9437088990@upi",
            rating = 5.0,
            reviewCount = 1,
            isOpen = true,
            deliveryAvailable = true,
            minOrderAmount = 40.0,
            estimatedDeliveryMins = 20,
            isApproved = true,
            imageUrl = "https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/shops/shop_organics.jpg"
        )

        // Verify Firebase SDK Map serialization
        val map = FirestoreSyncService.shopToMap(shopMobile1, ownerMobile1)
        assertEquals(10L, map["id"])
        assertEquals(55L, map["ownerId"])
        assertEquals("Sinapali Fresh Organics", map["name"])
        assertEquals("Fresh Vegetables & Fruits", map["category"])
        assertEquals("9437088990", map["phone"])
        assertEquals("https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/shops/shop_organics.jpg", map["imageUrl"])
        assertEquals(true, map["isOpen"])
        assertEquals(true, map["isApproved"])
        assertEquals("Bipin Bihari Sahu", map["ownerName"])

        // Verify REST JSON payload serialization
        val jsonPayload = FirestoreSyncService.shopToFirestorePayload(shopMobile1, ownerMobile1)
        val fields = jsonPayload.getJSONObject("fields")
        assertEquals("10", fields.getJSONObject("id").getString("integerValue"))
        assertEquals("55", fields.getJSONObject("ownerId").getString("integerValue"))
        assertEquals("Sinapali Fresh Organics", fields.getJSONObject("name").getString("stringValue"))
        assertEquals("https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/shops/shop_organics.jpg", fields.getJSONObject("imageUrl").getString("stringValue"))
    }

    @Test
    fun testMobile2ReceivesAndParsesMobile1ShopInRealTime() {
        val payloadFromMobile1 = org.json.JSONObject("""
            {
                "name": "projects/ama-sinapali-local-mart/databases/(default)/documents/shops/shop_10",
                "fields": {
                    "id": { "integerValue": "10" },
                    "ownerId": { "integerValue": "55" },
                    "name": { "stringValue": "Sinapali Fresh Organics" },
                    "category": { "stringValue": "Fresh Vegetables & Fruits" },
                    "address": { "stringValue": "Near Panchayat High School, Sinapali" },
                    "phone": { "stringValue": "9437088990" },
                    "upiId": { "stringValue": "9437088990@upi" },
                    "rating": { "doubleValue": 4.9 },
                    "reviewCount": { "integerValue": "8" },
                    "isOpen": { "booleanValue": true },
                    "deliveryAvailable": { "booleanValue": true },
                    "minOrderAmount": { "doubleValue": 40.0 },
                    "estimatedDeliveryMins": { "integerValue": "20" },
                    "isApproved": { "booleanValue": true },
                    "imageUrl": { "stringValue": "https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/shops/shop_organics.jpg" },
                    "ownerName": { "stringValue": "Bipin Bihari Sahu" }
                }
            }
        """.trimIndent())

        // Mobile 2 parses document received from Firebase in real time
        val shopOnMobile2 = FirestoreSyncService.firestorePayloadToShop(payloadFromMobile1)
        assertNotNull("Shop must parse successfully on Mobile 2", shopOnMobile2)
        assertEquals(10L, shopOnMobile2!!.id)
        assertEquals(55L, shopOnMobile2.ownerId)
        assertEquals("Sinapali Fresh Organics", shopOnMobile2.name)
        assertEquals("Fresh Vegetables & Fruits", shopOnMobile2.category)
        assertEquals("9437088990", shopOnMobile2.phone)
        assertEquals("https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/shops/shop_organics.jpg", shopOnMobile2.imageUrl)
        assertTrue("Shop on Mobile 2 is open", shopOnMobile2.isOpen)
        assertTrue("Shop on Mobile 2 is approved", shopOnMobile2.isApproved)
    }

    @Test
    fun testMobile1ProductCreationWithShopIdAndImageUrlAndFirestoreSerialization() {
        val productMobile1 = ProductEntity(
            id = 101L,
            shopId = 10L,
            name = "Organic Sinapali Cauliflower (ଫୁଲକୋବି)",
            category = "Fresh Vegetables",
            description = "Directly harvested farm-fresh pesticide-free cauliflower from Sinapali fields.",
            price = 45.0,
            discountPrice = 40.0,
            unit = "piece",
            stock = 30,
            isAvailable = true,
            imageUrl = "https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/products/cauliflower_sinapali.jpg",
            isApproved = true
        )

        // Verify Firebase SDK Map serialization contains shopId and imageUrl
        val map = FirestoreSyncService.productToMap(productMobile1)
        assertEquals(101L, map["id"])
        assertEquals(10L, map["shopId"])
        assertEquals("Organic Sinapali Cauliflower (ଫୁଲକୋବି)", map["name"])
        assertEquals(45.0, map["price"])
        assertEquals(40.0, map["discountPrice"])
        assertEquals(30, map["stock"])
        assertEquals(true, map["isAvailable"])
        assertEquals("https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/products/cauliflower_sinapali.jpg", map["imageUrl"])

        // Verify REST JSON payload contains shopId and imageUrl
        val jsonPayload = FirestoreSyncService.productToFirestorePayload(productMobile1)
        val fields = jsonPayload.getJSONObject("fields")
        assertEquals("101", fields.getJSONObject("id").getString("integerValue"))
        assertEquals("10", fields.getJSONObject("shopId").getString("integerValue"))
        assertEquals("Organic Sinapali Cauliflower (ଫୁଲକୋବି)", fields.getJSONObject("name").getString("stringValue"))
        assertEquals("https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/products/cauliflower_sinapali.jpg", fields.getJSONObject("imageUrl").getString("stringValue"))
        assertEquals(45.0, fields.getJSONObject("price").getDouble("doubleValue"), 0.01)
    }

    @Test
    fun testMobile2ReceivesAndParsesMobile1ProductInRealTime() {
        val payloadFromMobile1 = org.json.JSONObject("""
            {
                "name": "projects/ama-sinapali-local-mart/databases/(default)/documents/products/prod_101",
                "fields": {
                    "id": { "integerValue": "101" },
                    "shopId": { "integerValue": "10" },
                    "name": { "stringValue": "Organic Sinapali Cauliflower (ଫୁଲକୋବି)" },
                    "category": { "stringValue": "Fresh Vegetables" },
                    "description": { "stringValue": "Directly harvested farm-fresh pesticide-free cauliflower." },
                    "price": { "doubleValue": 45.0 },
                    "discountPrice": { "doubleValue": 40.0 },
                    "unit": { "stringValue": "piece" },
                    "stock": { "integerValue": "30" },
                    "isAvailable": { "booleanValue": true },
                    "imageUrl": { "stringValue": "https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/products/cauliflower_sinapali.jpg" },
                    "isApproved": { "booleanValue": true }
                }
            }
        """.trimIndent())

        // Mobile 2 parses product document received from Firebase in real time
        val productOnMobile2 = FirestoreSyncService.firestorePayloadToProduct(payloadFromMobile1)
        assertNotNull("Product must parse successfully on Mobile 2", productOnMobile2)
        assertEquals(101L, productOnMobile2!!.id)
        assertEquals(10L, productOnMobile2.shopId)
        assertEquals("Organic Sinapali Cauliflower (ଫୁଲକୋବି)", productOnMobile2.name)
        assertEquals("Fresh Vegetables", productOnMobile2.category)
        assertEquals(45.0, productOnMobile2.price, 0.01)
        assertEquals(40.0, productOnMobile2.discountPrice ?: 0.0, 0.01)
        assertEquals(30, productOnMobile2.stock)
        assertTrue("Product available on Mobile 2", productOnMobile2.isAvailable)
        assertEquals("https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/products/cauliflower_sinapali.jpg", productOnMobile2.imageUrl)
    }

    @Test
    fun testMobile1ProductStockToggleAndPriceUpdateSyncsToMobile2() {
        // Mobile 1 updates stock & price
        val updatedPayloadFromMobile1 = org.json.JSONObject("""
            {
                "name": "projects/ama-sinapali-local-mart/databases/(default)/documents/products/prod_101",
                "fields": {
                    "id": { "integerValue": "101" },
                    "shopId": { "integerValue": "10" },
                    "name": { "stringValue": "Organic Sinapali Cauliflower (ଫୁଲକୋବି)" },
                    "category": { "stringValue": "Fresh Vegetables" },
                    "description": { "stringValue": "Directly harvested farm-fresh pesticide-free cauliflower." },
                    "price": { "doubleValue": 50.0 },
                    "discountPrice": { "doubleValue": 45.0 },
                    "unit": { "stringValue": "piece" },
                    "stock": { "integerValue": "0" },
                    "isAvailable": { "booleanValue": false },
                    "imageUrl": { "stringValue": "https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/products/cauliflower_sinapali.jpg" },
                    "isApproved": { "booleanValue": true }
                }
            }
        """.trimIndent())

        val updatedProductMobile2 = FirestoreSyncService.firestorePayloadToProduct(updatedPayloadFromMobile1)
        assertNotNull(updatedProductMobile2)
        assertEquals(50.0, updatedProductMobile2!!.price, 0.01)
        assertEquals(0, updatedProductMobile2.stock)
        assertFalse("Stock toggled off on Mobile 2", updatedProductMobile2.isAvailable)
    }

    @Test
    fun testCrossDeviceIsolationAndMultipleShopsProductsAssociation() {
        // Mobile 1 created two products: one for Shop 10, one for Shop 20
        val p1 = ProductEntity(
            id = 101L,
            shopId = 10L,
            name = "Product Shop 10",
            category = "Cat A",
            description = "Desc A",
            price = 50.0,
            imageUrl = "url_a"
        )
        val p2 = ProductEntity(
            id = 102L,
            shopId = 20L,
            name = "Product Shop 20",
            category = "Cat B",
            description = "Desc B",
            price = 75.0,
            imageUrl = "url_b"
        )

        val productsOnMobile2 = listOf(p1, p2)
        val shop10Products = productsOnMobile2.filter { it.shopId == 10L }
        val shop20Products = productsOnMobile2.filter { it.shopId == 20L }

        assertEquals(1, shop10Products.size)
        assertEquals("Product Shop 10", shop10Products[0].name)
        assertEquals(1, shop20Products.size)
        assertEquals("Product Shop 20", shop20Products[0].name)
    }
}
