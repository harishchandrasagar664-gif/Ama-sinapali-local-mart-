package com.example

import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testOrderStatusesExist() {
        assertEquals("Order Placed", OrderStatus.ORDER_PLACED.label)
        assertEquals("Delivered", OrderStatus.DELIVERED.label)
        assertEquals("ଡେଲିଭରୀ ସମ୍ପନ୍ନ", OrderStatus.DELIVERED.odiaLabel)
    }

    @Test
    fun testPaymentMethods() {
        assertTrue(PaymentMethod.CASH_ON_DELIVERY.label.contains("Cash on Delivery"))
        assertTrue(PaymentMethod.RAZORPAY_ONLINE.label.contains("Razorpay Online"))
    }

    @Test
    fun testDeliveryTypes() {
        assertEquals("Home Delivery (ଘରକୁ ଡେଲିଭରୀ)", DeliveryType.HOME_DELIVERY.label)
        assertEquals("Self Store Pickup (ଦୋକାନରୁ ନିଜେ ନିଅନ୍ତୁ)", DeliveryType.STORE_PICKUP.label)
    }

    @Test
    fun testDiscountCalculation() {
        val subtotal = 1000.0
        val discountPercent = 10 // 10%
        val maxDiscount = 50.0
        val calculatedDiscount = (subtotal * discountPercent / 100.0).coerceAtMost(maxDiscount)
        assertEquals(50.0, calculatedDiscount, 0.001)

        val deliveryFee = 25.0
        val total = (subtotal - calculatedDiscount + deliveryFee).coerceAtLeast(0.0)
        assertEquals(975.0, total, 0.001)
    }

    @Test
    fun testPromotionTypes() {
        assertEquals(499.0, PromotionType.SPONSORED_SHOP.defaultPrice, 0.001)
        assertEquals(199.0, PromotionType.SPONSORED_PRODUCT.defaultPrice, 0.001)
    }
}
