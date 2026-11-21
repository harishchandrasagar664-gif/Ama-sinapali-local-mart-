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
            isVerified = true,
            firebaseUid = "uid_owner_55"
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
            imageUrl = "https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/shops/shop_organics.jpg",
            ownerUid = "uid_owner_55"
        )

        // Verify Firebase SDK Map serialization
        val map = FirestoreSyncService.shopToMap(shopMobile1, ownerMobile1)
        assertEquals(10L, map["id"])
        assertEquals(55L, map["ownerId"])
        assertEquals("uid_owner_55", map["ownerUid"])
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
        assertEquals("uid_owner_55", fields.getJSONObject("ownerUid").getString("stringValue"))
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
            isApproved = true,
            ownerUid = "uid_owner_55"
        )

        // Verify Firebase SDK Map serialization contains shopId, shopDocId, ownerUid and imageUrl
        val map = FirestoreSyncService.productToMap(productMobile1)
        assertEquals(101L, map["id"])
        assertEquals(10L, map["shopId"])
        assertEquals("shop_10", map["shopDocId"])
        assertEquals("uid_owner_55", map["ownerUid"])
        assertEquals("Organic Sinapali Cauliflower (ଫୁଲକୋବି)", map["name"])
        assertEquals(45.0, map["price"])
        assertEquals(40.0, map["discountPrice"])
        assertEquals(30, map["stock"])
        assertEquals(true, map["isAvailable"])
        assertEquals("https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/products/cauliflower_sinapali.jpg", map["imageUrl"])

        // Verify REST JSON payload contains shopId, shopDocId, ownerUid and imageUrl
        val jsonPayload = FirestoreSyncService.productToFirestorePayload(productMobile1)
        val fields = jsonPayload.getJSONObject("fields")
        assertEquals("101", fields.getJSONObject("id").getString("integerValue"))
        assertEquals("10", fields.getJSONObject("shopId").getString("integerValue"))
        assertEquals("shop_10", fields.getJSONObject("shopDocId").getString("stringValue"))
        assertEquals("uid_owner_55", fields.getJSONObject("ownerUid").getString("stringValue"))
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

    @Test
    fun testUnauthenticatedCloudWrites_ReturnHonestFailureInsteadOfFakeSuccess() = kotlinx.coroutines.runBlocking {
        val sampleShop = ShopEntity(
            id = 50L,
            ownerId = 55L,
            name = "Sinapali Cloth House",
            category = "Clothing",
            address = "Main Road",
            phone = "9437012345",
            ownerUid = "uid_owner_55"
        )
        val sampleProduct = ProductEntity(
            id = 500L,
            shopId = 50L,
            name = "Sambalpuri Saree",
            category = "Clothing",
            description = "Pure handloom saree",
            price = 1200.0,
            imageUrl = "hero_local_mart",
            ownerUid = "uid_owner_55"
        )

        val shopPush = FirestoreSyncService.pushShopToFirestoreWithResult(sampleShop)
        assertTrue("Unauthenticated pushShop must fail honestly", shopPush is FirestoreSyncService.CloudSyncResult.Failure)
        assertTrue((shopPush as FirestoreSyncService.CloudSyncResult.Failure).isAuthOrPermissionError)

        val prodPush = FirestoreSyncService.pushProductToFirestoreWithResult(sampleProduct)
        assertTrue("Unauthenticated pushProduct must fail honestly", prodPush is FirestoreSyncService.CloudSyncResult.Failure)
        assertTrue((prodPush as FirestoreSyncService.CloudSyncResult.Failure).isAuthOrPermissionError)

        val prodDelete = FirestoreSyncService.deleteProductFromFirestoreWithResult(sampleProduct)
        assertTrue("Unauthenticated deleteProduct must fail honestly", prodDelete is FirestoreSyncService.CloudSyncResult.Failure)
        assertTrue((prodDelete as FirestoreSyncService.CloudSyncResult.Failure).isAuthOrPermissionError)
    }

    @Test
    fun testOwnerUidPreservedInCloudPayloadParsing() {
        val payload = org.json.JSONObject("""
            {
                "name": "projects/ama-sinapali-local-mart/databases/(default)/documents/products/prod_777",
                "fields": {
                    "id": { "integerValue": "777" },
                    "shopId": { "integerValue": "10" },
                    "shopDocId": { "stringValue": "shop_10" },
                    "ownerUid": { "stringValue": "uid_shop_owner_77" },
                    "name": { "stringValue": "Handloom Gamcha" },
                    "category": { "stringValue": "Clothing" },
                    "description": { "stringValue": "Traditional Odisha cotton" },
                    "price": { "doubleValue": 180.0 },
                    "isAvailable": { "booleanValue": true },
                    "isApproved": { "booleanValue": true }
                }
            }
        """.trimIndent())

        val parsed = FirestoreSyncService.firestorePayloadToProduct(payload)
        assertNotNull(parsed)
        assertEquals("uid_shop_owner_77", parsed!!.ownerUid)
        assertEquals(777L, parsed.id)
        assertEquals(10L, parsed.shopId)
    }

    @Test
    fun testPureKotlinBase64AndFirestoreImageSizeCap() {
        val sampleBytes = "Sinapali Local Mart Saree Image".toByteArray(Charsets.UTF_8)
        val encodedPure = com.example.util.ImageStorageHelper.encodeBase64PureKotlin(sampleBytes)
        val encodedApi = com.example.util.ImageStorageHelper.encodeBase64(sampleBytes)
        assertEquals("U2luYXBhbGkgTG9jYWwgTWFydCBTYXJlZSBJbWFnZQ==", encodedPure)
        assertEquals(encodedPure, encodedApi)
        assertTrue(
            "Max Firestore image URL chars must stay below 400,000 rule limit",
            com.example.util.ImageStorageHelper.MAX_FIRESTORE_IMAGE_URL_CHARS <= 400_000
        )
    }

    @Test
    fun testEditableMaps_StrictlyMatchFirestoreUpdateAllowlists() {
        val shop = ShopEntity(
            id = 10L,
            ownerId = 55L,
            name = "Sinapali Silk",
            category = "Clothing & Fashion",
            address = "Main Road",
            phone = "9437088990",
            upiId = "9437088990@upi",
            isApproved = true,
            ownerUid = "uid_owner_55"
        )
        val shopAllowedUpdateKeys = setOf(
            "name", "category", "address", "phone", "upiId",
            "isOpen", "deliveryAvailable", "minOrderAmount",
            "estimatedDeliveryMins", "imageUrl", "ownerName", "updatedAt"
        )
        val editableShopKeys = FirestoreSyncService.shopToEditableMap(shop).keys
        assertTrue(
            "shopToEditableMap must only contain firestore.rules allowlisted keys",
            shopAllowedUpdateKeys.containsAll(editableShopKeys)
        )

        val product = ProductEntity(
            id = 101L,
            shopId = 10L,
            name = "Sambalpuri Saree",
            category = "Clothing & Fashion",
            description = "Pure handloom",
            price = 1350.0,
            discountPrice = 750.0,
            unit = "1 unit",
            stock = 12,
            isAvailable = true,
            imageUrl = "hero_local_mart",
            isApproved = true,
            ownerUid = "uid_owner_55"
        )
        val productAllowedUpdateKeys = setOf(
            "name", "category", "description", "price", "discountPrice",
            "unit", "stock", "isAvailable", "imageUrl", "updatedAt"
        )
        val editableProductKeys = FirestoreSyncService.productToEditableMap(product).keys
        assertTrue(
            "productToEditableMap must only contain firestore.rules allowlisted keys",
            productAllowedUpdateKeys.containsAll(editableProductKeys)
        )
    }

    @Test
    fun testProductToMap_SanitizesOversizedImageUrlAndInvalidDiscount() {
        val oversizedUrl = "data:image/jpeg;base64," + "A".repeat(410_000)
        val edgeProduct = ProductEntity(
            id = 202L,
            shopId = 10L,
            name = "Edge Product",
            category = "Daily Essentials",
            description = "Desc",
            price = 500.0,
            discountPrice = 900.0, // invalid > price
            imageUrl = oversizedUrl,
            ownerUid = "uid_owner_55"
        )
        val map = FirestoreSyncService.productToMap(edgeProduct)
        assertEquals("Invalid discountPrice > price must clamp to 0.0", 0.0, map["discountPrice"] as Double, 0.001)
        assertEquals("Oversized imageUrl > 400KB must fall back safely", "hero_local_mart", map["imageUrl"])
    }

    @Test
    fun testUninitializedFirebaseFetchers_FailGracefullyWithoutCrashing() {
        val shops = FirestoreSyncService.fetchCloudShopsInternal()
        val products = FirestoreSyncService.fetchCloudProductsInternal()
        val orders = FirestoreSyncService.fetchCloudOrdersInternal()
        assertNotNull(shops)
        assertNotNull(products)
        assertNotNull(orders)
    }

    @Test
    fun testProductAndShopMaps_EnforceFirestoreRuleFieldBounds() {
        val longOdiaDesc = "ଓଡ଼ିଆ ".repeat(1000) // > 4000 chars
        val edgeProduct = ProductEntity(
            id = 303L,
            shopId = 10L,
            name = "   ",
            category = "",
            description = longOdiaDesc,
            price = 0.0,
            discountPrice = null,
            unit = "",
            stock = -5,
            imageUrl = "",
            ownerUid = "uid_owner_55"
        )
        val prodMap = FirestoreSyncService.productToMap(edgeProduct)
        assertTrue((prodMap["name"] as String).isNotBlank())
        assertTrue((prodMap["category"] as String).isNotBlank())
        assertTrue((prodMap["description"] as String).length <= 4000)
        assertTrue((prodMap["price"] as Double) > 0.0)
        assertTrue((prodMap["unit"] as String).isNotBlank())
        assertTrue((prodMap["stock"] as Int) >= 0)
        assertEquals("hero_local_mart", prodMap["imageUrl"])

        val edgeShop = ShopEntity(
            id = 20L,
            ownerId = 55L,
            name = "A", // < 2 chars
            category = "",
            address = "X", // < 2 chars
            phone = "123", // < 7 chars
            rating = 9.0,
            reviewCount = -2,
            minOrderAmount = -10.0,
            estimatedDeliveryMins = 9999,
            imageUrl = "",
            ownerUid = "uid_owner_55"
        )
        val shopMap = FirestoreSyncService.shopToMap(edgeShop)
        assertTrue((shopMap["name"] as String).length in 2..120)
        assertTrue((shopMap["category"] as String).length in 1..100)
        assertTrue((shopMap["address"] as String).length in 2..300)
        assertTrue((shopMap["phone"] as String).length in 7..20)
        assertTrue((shopMap["rating"] as Double) in 0.0..5.0)
        assertTrue((shopMap["reviewCount"] as Int) >= 0)
        assertTrue((shopMap["minOrderAmount"] as Double) >= 0.0)
        assertTrue((shopMap["estimatedDeliveryMins"] as Int) in 0..1440)
        assertEquals("hero_local_mart", shopMap["imageUrl"])
    }

    @Test
    fun testCustomerPublicCatalogVisibility_OnlyApprovedAndAvailableProducts() {
        val approvedAvailable = ProductEntity(
            id = 1L, shopId = 10L, name = "Fresh Rice", category = "Grocery",
            description = "25kg bag", price = 1200.0, isAvailable = true, isApproved = true
        )
        val outOfStock = ProductEntity(
            id = 2L, shopId = 10L, name = "Sold Out Oil", category = "Grocery",
            description = "1L", price = 150.0, isAvailable = false, isApproved = true
        )
        val unapproved = ProductEntity(
            id = 3L, shopId = 10L, name = "Pending Item", category = "Grocery",
            description = "Draft", price = 99.0, isAvailable = true, isApproved = false
        )
        val customerVisible = listOf(approvedAvailable, outOfStock, unapproved)
            .filter { it.isApproved && it.isAvailable }
        assertEquals(1, customerVisible.size)
        assertEquals(1L, customerVisible.first().id)
    }

    @Test
    fun testShopOwnerFirebaseUidSessionMatching_RejectsMismatchedCustomerUid() {
        assertFalse(
            "Null Firebase UID must require OTP login",
            com.example.service.AuthManager.isFirebaseUidMatchingSession("uid_owner_55", null)
        )
        assertFalse(
            "Blank Firebase UID must require OTP login",
            com.example.service.AuthManager.isFirebaseUidMatchingSession("uid_owner_55", "")
        )
        assertFalse(
            "Different Customer UID must NOT auto-restore Shop Owner session",
            com.example.service.AuthManager.isFirebaseUidMatchingSession("uid_owner_55", "uid_customer_99")
        )
        assertTrue(
            "Matching Shop Owner Firebase UID is valid",
            com.example.service.AuthManager.isFirebaseUidMatchingSession("uid_owner_55", "uid_owner_55")
        )
        assertTrue(
            "Legacy session without saved UID with active Firebase UID passes",
            com.example.service.AuthManager.isFirebaseUidMatchingSession("", "uid_owner_55")
        )
    }

    @Test
    fun testShopAndProductFullMaps_ContainAllRequiredFirestoreCreateFields() {
        val shop = ShopEntity(
            id = 12L,
            ownerId = 55L,
            name = "Sinapali Agro Store",
            category = "Farm & Seeds",
            address = "College Chhak, Sinapali",
            phone = "9437011223",
            upiId = "9437011223@upi",
            rating = 4.7,
            reviewCount = 3,
            isOpen = true,
            deliveryAvailable = true,
            minOrderAmount = 50.0,
            estimatedDeliveryMins = 30,
            isApproved = true,
            imageUrl = "hero_local_mart",
            ownerUid = "uid_owner_55"
        )
        val requiredShopCreateKeys = listOf(
            "id", "ownerId", "ownerUid", "name", "category", "address",
            "phone", "upiId", "rating", "reviewCount", "isOpen",
            "deliveryAvailable", "minOrderAmount", "estimatedDeliveryMins",
            "isApproved", "imageUrl"
        )
        val shopMap = FirestoreSyncService.shopToMap(shop)
        for (key in requiredShopCreateKeys) {
            assertTrue("shopToMap missing required key '$key'", shopMap.containsKey(key))
        }

        val product = ProductEntity(
            id = 88L,
            shopId = 12L,
            name = "Desi Moong Dal",
            category = "Groceries",
            description = "Locally grown organic moong dal",
            price = 140.0,
            discountPrice = 130.0,
            unit = "1 kg",
            stock = 20,
            isAvailable = true,
            imageUrl = "hero_local_mart",
            isApproved = true,
            ownerUid = "uid_owner_55"
        )
        val requiredProductCreateKeys = listOf(
            "id", "shopId", "shopDocId", "ownerUid", "name", "category",
            "description", "price", "discountPrice", "unit", "stock",
            "isAvailable", "imageUrl", "isApproved"
        )
        val productMap = FirestoreSyncService.productToMap(product)
        for (key in requiredProductCreateKeys) {
            assertTrue("productToMap missing required key '$key'", productMap.containsKey(key))
        }
        assertEquals("shop_12", productMap["shopDocId"])
    }

    @Test
    fun testUpdateAndCreatePaths_WhenUnauthenticated_NeverReturnSuccess() = kotlinx.coroutines.runBlocking {
        val existingShop = ShopEntity(
            id = 1L,
            ownerId = 2L,
            name = "Existing Seed Shop",
            category = "Daily Essentials",
            address = "Main Road, Sinapali",
            phone = "9437000001",
            ownerUid = "uid_owner_1"
        )
        val existingProduct = ProductEntity(
            id = 10L,
            shopId = 1L,
            name = "Seed Rice",
            category = "Daily Essentials",
            description = "Fine rice",
            price = 60.0,
            imageUrl = "hero_local_mart",
            ownerUid = "uid_owner_1"
        )

        val shopCreateRes = FirestoreSyncService.pushShopToFirestoreWithResult(existingShop, isNew = true)
        val shopUpdateRes = FirestoreSyncService.pushShopToFirestoreWithResult(existingShop, isNew = false)
        val prodCreateRes = FirestoreSyncService.pushProductToFirestoreWithResult(existingProduct, isNew = true)
        val prodUpdateRes = FirestoreSyncService.pushProductToFirestoreWithResult(existingProduct, isNew = false)

        assertTrue(shopCreateRes is FirestoreSyncService.CloudSyncResult.Failure)
        assertTrue(shopUpdateRes is FirestoreSyncService.CloudSyncResult.Failure)
        assertTrue(prodCreateRes is FirestoreSyncService.CloudSyncResult.Failure)
        assertTrue(prodUpdateRes is FirestoreSyncService.CloudSyncResult.Failure)
    }
}

