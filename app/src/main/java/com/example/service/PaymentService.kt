package com.example.service

import com.example.BuildConfig
import com.example.domain.model.PaymentMethod

sealed class PaymentResult {
    data class Success(val transactionId: String, val paymentMethod: PaymentMethod) : PaymentResult()
    data class Failure(val errorCode: String, val message: String) : PaymentResult()
    object Cancelled : PaymentResult()
}

interface PaymentService {
    fun getRazorpayKeyId(): String
    fun isRazorpayConfigured(): Boolean
}

class RazorpayPaymentServiceImpl : PaymentService {
    override fun getRazorpayKeyId(): String {
        return try {
            BuildConfig.RAZORPAY_KEY_ID
        } catch (e: Exception) {
            "rzp_test_placeholder"
        }
    }

    override fun isRazorpayConfigured(): Boolean {
        val key = getRazorpayKeyId()
        return key.isNotBlank() && !key.contains("placeholder")
    }
}
