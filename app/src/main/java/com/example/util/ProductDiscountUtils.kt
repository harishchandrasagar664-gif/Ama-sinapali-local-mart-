package com.example.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class ProductDiscountInfo(
    val originalPrice: Double,
    val offerPrice: Double,
    val discountAmount: Double,
    val discountPercentExact: Double,
    val discountPercentRounded: Int,
    val discountBadgeShort: String,
    val discountPercentLabel: String,
    val formattedOriginalPrice: String,
    val formattedOfferPrice: String,
    val formattedSavings: String
)

sealed class PriceValidationResult {
    data class Valid(
        val originalPrice: Double,
        val offerPrice: Double?,
        val discountInfo: ProductDiscountInfo?
    ) : PriceValidationResult()

    data class Invalid(val errorMessage: String) : PriceValidationResult()
}

object ProductDiscountUtils {

    private val indianLocale = Locale("en", "IN")

    fun formatRupees(amount: Double): String {
        val safeAmount = if (amount.isNaN() || amount.isInfinite() || amount < 0.0) 0.0 else amount
        val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
            isGroupingUsed = true
        }
        return "₹${formatter.format(safeAmount)}"
    }

    fun formatPriceInput(amount: Double): String {
        if (amount.isNaN() || amount.isInfinite() || amount <= 0.0) return ""
        return if (abs(amount - amount.toLong().toDouble()) < 0.0001) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount).trimEnd('0').trimEnd('.')
        }
    }

    fun formatDiscountPercentage(percent: Double): String {
        if (percent.isNaN() || percent.isInfinite() || percent <= 0.0) return "0%"
        val roundedInt = percent.roundToInt()
        return if (abs(percent - roundedInt) < 0.005) {
            "$roundedInt%"
        } else {
            String.format(Locale.US, "%.2f%%", percent)
        }
    }

    fun calculate(originalPrice: Double, offerPrice: Double?): ProductDiscountInfo? {
        if (originalPrice.isNaN() || originalPrice.isInfinite() || originalPrice <= 0.0) return null
        if (offerPrice == null || offerPrice.isNaN() || offerPrice.isInfinite() || offerPrice <= 0.0) return null
        if (offerPrice >= originalPrice) return null

        val discountAmount = originalPrice - offerPrice
        if (discountAmount <= 0.0) return null

        val discountPercentExact = (discountAmount / originalPrice) * 100.0
        val discountPercentRounded = discountPercentExact.roundToInt()

        val shortPct = if (discountPercentRounded > 0) "$discountPercentRounded% OFF" else "${formatDiscountPercentage(discountPercentExact)} OFF"
        val exactPct = "${formatDiscountPercentage(discountPercentExact)} OFF"

        return ProductDiscountInfo(
            originalPrice = originalPrice,
            offerPrice = offerPrice,
            discountAmount = discountAmount,
            discountPercentExact = discountPercentExact,
            discountPercentRounded = discountPercentRounded,
            discountBadgeShort = shortPct,
            discountPercentLabel = exactPct,
            formattedOriginalPrice = formatRupees(originalPrice),
            formattedOfferPrice = formatRupees(offerPrice),
            formattedSavings = formatRupees(discountAmount)
        )
    }

    fun validatePriceInputs(originalPriceInput: String, offerPriceInput: String): PriceValidationResult {
        val cleanOrig = originalPriceInput.trim().replace(",", "").replace("₹", "")
        if (cleanOrig.isBlank()) {
            return PriceValidationResult.Invalid("Please enter Original Price (MRP).")
        }
        val orig = cleanOrig.toDoubleOrNull()
        if (orig == null || orig.isNaN() || orig.isInfinite() || orig <= 0.0) {
            return PriceValidationResult.Invalid("Original Price (MRP) must be a positive number.")
        }

        val cleanOffer = offerPriceInput.trim().replace(",", "").replace("₹", "")
        if (cleanOffer.isBlank()) {
            return PriceValidationResult.Valid(
                originalPrice = orig,
                offerPrice = null,
                discountInfo = null
            )
        }

        val offer = cleanOffer.toDoubleOrNull()
        if (offer == null || offer.isNaN() || offer.isInfinite() || offer <= 0.0) {
            return PriceValidationResult.Invalid("Offer Price must be a positive number.")
        }

        if (offer > orig) {
            return PriceValidationResult.Invalid("Offer Price cannot exceed Original Price (MRP).")
        }

        if (offer == orig) {
            return PriceValidationResult.Valid(
                originalPrice = orig,
                offerPrice = null,
                discountInfo = null
            )
        }

        val info = calculate(orig, offer)
        return PriceValidationResult.Valid(
            originalPrice = orig,
            offerPrice = offer,
            discountInfo = info
        )
    }
}
