package com.example

import com.example.data.firebase.FirestoreSyncService
import com.example.data.local.entities.ProductEntity
import com.example.util.PriceValidationResult
import com.example.util.ProductDiscountUtils
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DiscountAndOdiaDescriptionTest {

    @Test
    fun testExactExampleDiscountCalculation_1350And750() {
        val info = ProductDiscountUtils.calculate(1350.0, 750.0)
        assertNotNull("Discount info should be calculated when offer < original", info)
        info!!

        assertEquals(1350.0, info.originalPrice, 0.001)
        assertEquals(750.0, info.offerPrice, 0.001)
        assertEquals(600.0, info.discountAmount, 0.001)
        assertEquals(44.4444, info.discountPercentExact, 0.01)
        assertEquals(44, info.discountPercentRounded)
        assertEquals("44% OFF", info.discountBadgeShort)
        assertEquals("44.44% OFF", info.discountPercentLabel)
        assertEquals("₹1,350", info.formattedOriginalPrice)
        assertEquals("₹750", info.formattedOfferPrice)
        assertEquals("₹600", info.formattedSavings)
    }

    @Test
    fun testMultipleDiscountCalculations() {
        // 50% OFF
        val halfOff = ProductDiscountUtils.calculate(500.0, 250.0)!!
        assertEquals(250.0, halfOff.discountAmount, 0.001)
        assertEquals("50% OFF", halfOff.discountBadgeShort)
        assertEquals("50% OFF", halfOff.discountPercentLabel)
        assertEquals("₹250", halfOff.formattedSavings)

        // 25% OFF
        val quarterOff = ProductDiscountUtils.calculate(120.0, 90.0)!!
        assertEquals(30.0, quarterOff.discountAmount, 0.001)
        assertEquals("25% OFF", quarterOff.discountPercentLabel)
        assertEquals("₹30", quarterOff.formattedSavings)

        // Decimal discount percentage (e.g. 1999 -> 1499 = 25.01%)
        val decimalPct = ProductDiscountUtils.calculate(1999.0, 1499.0)!!
        assertEquals(500.0, decimalPct.discountAmount, 0.001)
        assertEquals("25% OFF", decimalPct.discountBadgeShort)
        assertEquals("25.01% OFF", decimalPct.discountPercentLabel)
        assertEquals("₹1,999", decimalPct.formattedOriginalPrice)
        assertEquals("₹1,499", decimalPct.formattedOfferPrice)
        assertEquals("₹500", decimalPct.formattedSavings)

        // Standard comma grouping format
        val fourDigit = ProductDiscountUtils.calculate(2450.0, 1850.0)!!
        assertEquals("₹2,450", fourDigit.formattedOriginalPrice)
        assertEquals("₹1,850", fourDigit.formattedOfferPrice)
        assertEquals("₹600", fourDigit.formattedSavings)
        assertEquals("24.49% OFF", fourDigit.discountPercentLabel)

        val highValue = ProductDiscountUtils.calculate(100000.0, 85000.0)!!
        assertTrue(highValue.formattedOriginalPrice == "₹1,00,000" || highValue.formattedOriginalPrice == "₹100,000")
        assertEquals("₹85,000", highValue.formattedOfferPrice)
        assertEquals("₹15,000", highValue.formattedSavings)
        assertEquals("15% OFF", highValue.discountPercentLabel)
    }

    @Test
    fun testDiscountValidationAndEdgeCases() {
        // Division by zero / non-positive guards
        assertNull(ProductDiscountUtils.calculate(0.0, 50.0))
        assertNull(ProductDiscountUtils.calculate(-100.0, 50.0))
        assertNull(ProductDiscountUtils.calculate(100.0, 0.0))
        assertNull(ProductDiscountUtils.calculate(100.0, -10.0))
        assertNull(ProductDiscountUtils.calculate(100.0, null))
        assertNull(ProductDiscountUtils.calculate(Double.NaN, 50.0))

        // Equal prices hide discount badge
        assertNull(ProductDiscountUtils.calculate(1000.0, 1000.0))
        val equalValidation = ProductDiscountUtils.validatePriceInputs("1000", "1000")
        assertTrue(equalValidation is PriceValidationResult.Valid)
        val equalValid = equalValidation as PriceValidationResult.Valid
        assertEquals(1000.0, equalValid.originalPrice, 0.001)
        assertNull("Equal offer price should normalize discountPrice to null", equalValid.offerPrice)
        assertNull(equalValid.discountInfo)

        // Blank offer price is valid with no discount
        val blankOffer = ProductDiscountUtils.validatePriceInputs("1350", "")
        assertTrue(blankOffer is PriceValidationResult.Valid)
        assertNull((blankOffer as PriceValidationResult.Valid).discountInfo)

        // Offer price exceeding original price is invalid
        val exceedOffer = ProductDiscountUtils.validatePriceInputs("750", "900")
        assertTrue(exceedOffer is PriceValidationResult.Invalid)

        // Non-positive prices are invalid
        assertTrue(ProductDiscountUtils.validatePriceInputs("0", "0") is PriceValidationResult.Invalid)
        assertTrue(ProductDiscountUtils.validatePriceInputs("-50", "10") is PriceValidationResult.Invalid)
        assertTrue(ProductDiscountUtils.validatePriceInputs("500", "0") is PriceValidationResult.Invalid)
        assertTrue(ProductDiscountUtils.validatePriceInputs("500", "-10") is PriceValidationResult.Invalid)
        assertTrue(ProductDiscountUtils.validatePriceInputs("", "") is PriceValidationResult.Invalid)
    }

    @Test
    fun testOdiaUnicodeMultilineDescriptionAndFirestoreRoundTrip() {
        val odiaMultilineDescription =
            "ଶୁଦ୍ଧ ସମ୍ବଲପୁରୀ ହ୍ୟାଣ୍ଡଲୁମ୍ ପାଟ ଶାଢ଼ୀ (ଓଡ଼ିଆ ସ୍ୱତନ୍ତ୍ର)\n" +
            "ରଙ୍ଗ: ଲାଲ୍ ଏବଂ ସୁନେଲି\n" +
            "ଗୁଣବତ୍ତା: ୧୦୦% ଶୁଦ୍ଧ ସୂତା ଓ ପାଟ\n" +
            "ଆକାର: ୫.୫ ମିଟର ବ୍ଲାଉଜ୍ ପିସ୍ ସହିତ\n" +
            "Best for Weddings & Nuakhai Festival!"

        val product = ProductEntity(
            id = 505L,
            shopId = 12L,
            name = "Sambalpuri Pata Saree (ସମ୍ବଲପୁରୀ ପାଟ ଶାଢ଼ୀ)",
            category = "Saree",
            imageUrl = "https://firebasestorage.googleapis.com/v0/b/ama-sinapali.appspot.com/products/saree.jpg",
            description = odiaMultilineDescription,
            price = 1350.0,
            discountPrice = 750.0,
            unit = "1 piece",
            stock = 15,
            isAvailable = true,
            isApproved = true
        )

        // Check SDK Map serialization
        val map = FirestoreSyncService.productToMap(product)
        assertEquals(odiaMultilineDescription, map["description"])
        assertEquals(1350.0, map["price"])
        assertEquals(750.0, map["discountPrice"])

        // Check REST JSON payload serialization + parsing round-trip
        val payload = FirestoreSyncService.productToFirestorePayload(product)
        val docWithName = JSONObject().apply {
            put("name", "projects/ama-sinapali-local-mart/databases/(default)/documents/products/prod_505")
            put("fields", payload.getJSONObject("fields"))
        }

        val parsed = FirestoreSyncService.firestorePayloadToProduct(docWithName)
        assertNotNull("Parsed product from Firestore payload must not be null", parsed)
        parsed!!
        assertEquals(odiaMultilineDescription, parsed.description)
        assertTrue("Line breaks must be preserved", parsed.description.contains("\n"))
        assertTrue("Odia characters must be preserved", parsed.description.contains("ଓଡ଼ିଆ"))
        assertEquals(1350.0, parsed.price, 0.001)
        assertNotNull(parsed.discountPrice)
        assertEquals(750.0, parsed.discountPrice!!, 0.001)

        val info = ProductDiscountUtils.calculate(parsed.price, parsed.discountPrice)
        assertNotNull(info)
        assertEquals("44.44% OFF", info!!.discountPercentLabel)
        assertEquals("₹600", info.formattedSavings)
    }

    @Test
    fun testBackwardCompatibilityForLegacyProductsWithoutDiscountOrDescription() {
        val legacyDoc = JSONObject("""
            {
                "name": "projects/ama-sinapali-local-mart/databases/(default)/documents/products/prod_77",
                "fields": {
                    "id": { "integerValue": "77" },
                    "shopId": { "integerValue": "3" },
                    "name": { "stringValue": "Sinapali Mustard Oil 1L" },
                    "category": { "stringValue": "Grocery" },
                    "price": { "doubleValue": 180.0 },
                    "unit": { "stringValue": "1L" },
                    "stock": { "integerValue": "25" },
                    "isAvailable": { "booleanValue": true },
                    "isApproved": { "booleanValue": true }
                }
            }
        """.trimIndent())

        val parsedLegacy = FirestoreSyncService.firestorePayloadToProduct(legacyDoc)
        assertNotNull(parsedLegacy)
        parsedLegacy!!
        assertEquals(180.0, parsedLegacy.price, 0.001)
        assertNull("Legacy product without discountPrice should stay null", parsedLegacy.discountPrice)
        assertEquals("", parsedLegacy.description)
        assertNull(ProductDiscountUtils.calculate(parsedLegacy.price, parsedLegacy.discountPrice))
    }
}
