package com.example

import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testOrderStatusesExist() {
        assertEquals("PLACED", OrderStatus.PLACED.name)
        assertEquals("DELIVERED", OrderStatus.DELIVERED.name)
        assertEquals("OUT_FOR_DELIVERY", OrderStatus.OUT_FOR_DELIVERY.name)
    }

    @Test
    fun testPaymentMethods() {
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.valueOf("CASH_ON_DELIVERY"))
        assertEquals(PaymentMethod.ONLINE_UPI, PaymentMethod.valueOf("ONLINE_UPI"))
        assertEquals(PaymentMethod.RAZORPAY, PaymentMethod.valueOf("RAZORPAY"))
    }

    @Test
    fun testDeliveryTypes() {
        assertEquals(DeliveryType.HOME_DELIVERY, DeliveryType.valueOf("HOME_DELIVERY"))
        assertEquals(DeliveryType.SELF_PICKUP, DeliveryType.valueOf("SELF_PICKUP"))
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
        assertEquals(PromotionType.BANNER, PromotionType.valueOf("BANNER"))
        assertEquals(PromotionType.FEATURED_PRODUCT, PromotionType.valueOf("FEATURED_PRODUCT"))
        assertEquals(PromotionType.DISCOUNT_COUPON, PromotionType.valueOf("DISCOUNT_COUPON"))
    }
}
