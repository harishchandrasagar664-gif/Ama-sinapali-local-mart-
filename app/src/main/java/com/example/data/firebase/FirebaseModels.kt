package com.example.data.firebase

import com.example.data.local.entities.*
import com.example.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data Transfer Objects (DTOs) and bidirectional conversion mappers for Firebase Cloud Sync.
 * Prepared for Firebase Firestore / Realtime Database integration without altering Room entities
 * or existing database structures.
 */

data class FirebaseOrderDto(
    val orderNumber: String,
    val localId: Long = 0,
    val userId: Long,
    val shopId: Long,
    val totalAmount: Double,
    val discountAmount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val finalAmount: Double,
    val status: String = OrderStatus.PLACED.name,
    val deliveryType: String = DeliveryType.HOME_DELIVERY.name,
    val paymentMethod: String = PaymentMethod.CASH_ON_DELIVERY.name,
    val paymentStatus: String = PaymentStatus.PENDING.name,
    val deliveryAddress: String,
    val customerPhone: String,
    val customerName: String,
    val orderNotes: String = "",
    val placedAt: Long = System.currentTimeMillis(),
    val deliveryAgentAssigned: String? = null,
    val estimatedDeliveryTime: Long = 0,
    val items: List<FirebaseOrderItemDto> = emptyList(),
    val syncedAt: Long = System.currentTimeMillis(),
    val deviceSource: String = "android_client"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "orderNumber" to orderNumber,
            "localId" to localId,
            "userId" to userId,
            "shopId" to shopId,
            "totalAmount" to totalAmount,
            "discountAmount" to discountAmount,
            "deliveryFee" to deliveryFee,
            "finalAmount" to finalAmount,
            "status" to status,
            "deliveryType" to deliveryType,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to paymentStatus,
            "deliveryAddress" to deliveryAddress,
            "customerPhone" to customerPhone,
            "customerName" to customerName,
            "orderNotes" to orderNotes,
            "placedAt" to placedAt,
            "deliveryAgentAssigned" to deliveryAgentAssigned,
            "estimatedDeliveryTime" to estimatedDeliveryTime,
            "syncedAt" to syncedAt,
            "deviceSource" to deviceSource,
            "items" to items.map { it.toMap() }
        )
    }

    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("orderNumber", orderNumber)
        json.put("localId", localId)
        json.put("userId", userId)
        json.put("shopId", shopId)
        json.put("totalAmount", totalAmount)
        json.put("discountAmount", discountAmount)
        json.put("deliveryFee", deliveryFee)
        json.put("finalAmount", finalAmount)
        json.put("status", status)
        json.put("deliveryType", deliveryType)
        json.put("paymentMethod", paymentMethod)
        json.put("paymentStatus", paymentStatus)
        json.put("deliveryAddress", deliveryAddress)
        json.put("customerPhone", customerPhone)
        json.put("customerName", customerName)
        json.put("orderNotes", orderNotes)
        json.put("placedAt", placedAt)
        json.put("deliveryAgentAssigned", deliveryAgentAssigned ?: JSONObject.NULL)
        json.put("estimatedDeliveryTime", estimatedDeliveryTime)
        json.put("syncedAt", syncedAt)
        json.put("deviceSource", deviceSource)

        val itemsArray = JSONArray()
        items.forEach { itemsArray.put(it.toJsonObject()) }
        json.put("items", itemsArray)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): FirebaseOrderDto {
            val itemsList = mutableListOf<FirebaseOrderItemDto>()
            val itemsArray = json.optJSONArray("items")
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.optJSONObject(i)
                    if (itemObj != null) {
                        itemsList.add(FirebaseOrderItemDto.fromJsonObject(itemObj))
                    }
                }
            }

            return FirebaseOrderDto(
                orderNumber = json.optString("orderNumber", ""),
                localId = json.optLong("localId", 0L),
                userId = json.optLong("userId", 0L),
                shopId = json.optLong("shopId", 0L),
                totalAmount = json.optDouble("totalAmount", 0.0),
                discountAmount = json.optDouble("discountAmount", 0.0),
                deliveryFee = json.optDouble("deliveryFee", 0.0),
                finalAmount = json.optDouble("finalAmount", 0.0),
                status = json.optString("status", OrderStatus.PLACED.name),
                deliveryType = json.optString("deliveryType", DeliveryType.HOME_DELIVERY.name),
                paymentMethod = json.optString("paymentMethod", PaymentMethod.CASH_ON_DELIVERY.name),
                paymentStatus = json.optString("paymentStatus", PaymentStatus.PENDING.name),
                deliveryAddress = json.optString("deliveryAddress", ""),
                customerPhone = json.optString("customerPhone", ""),
                customerName = json.optString("customerName", ""),
                orderNotes = json.optString("orderNotes", ""),
                placedAt = json.optLong("placedAt", System.currentTimeMillis()),
                deliveryAgentAssigned = if (json.isNull("deliveryAgentAssigned")) null else json.optString("deliveryAgentAssigned"),
                estimatedDeliveryTime = json.optLong("estimatedDeliveryTime", 0L),
                items = itemsList,
                syncedAt = json.optLong("syncedAt", System.currentTimeMillis()),
                deviceSource = json.optString("deviceSource", "android_client")
            )
        }

        fun fromMap(map: Map<String, Any?>): FirebaseOrderDto {
            @Suppress("UNCHECKED_CAST")
            val rawItems = map["items"] as? List<Map<String, Any?>> ?: emptyList()
            val itemsList = rawItems.map { FirebaseOrderItemDto.fromMap(it) }

            return FirebaseOrderDto(
                orderNumber = map["orderNumber"] as? String ?: "",
                localId = (map["localId"] as? Number)?.toLong() ?: 0L,
                userId = (map["userId"] as? Number)?.toLong() ?: 0L,
                shopId = (map["shopId"] as? Number)?.toLong() ?: 0L,
                totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                discountAmount = (map["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                deliveryFee = (map["deliveryFee"] as? Number)?.toDouble() ?: 0.0,
                finalAmount = (map["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                status = map["status"] as? String ?: OrderStatus.PLACED.name,
                deliveryType = map["deliveryType"] as? String ?: DeliveryType.HOME_DELIVERY.name,
                paymentMethod = map["paymentMethod"] as? String ?: PaymentMethod.CASH_ON_DELIVERY.name,
                paymentStatus = map["paymentStatus"] as? String ?: PaymentStatus.PENDING.name,
                deliveryAddress = map["deliveryAddress"] as? String ?: "",
                customerPhone = map["customerPhone"] as? String ?: "",
                customerName = map["customerName"] as? String ?: "",
                orderNotes = map["orderNotes"] as? String ?: "",
                placedAt = (map["placedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                deliveryAgentAssigned = map["deliveryAgentAssigned"] as? String,
                estimatedDeliveryTime = (map["estimatedDeliveryTime"] as? Number)?.toLong() ?: 0L,
                items = itemsList,
                syncedAt = (map["syncedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                deviceSource = map["deviceSource"] as? String ?: "android_client"
            )
        }
    }
}

data class FirebaseOrderItemDto(
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int,
    val totalPrice: Double
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "productId" to productId,
            "productName" to productName,
            "unitPrice" to unitPrice,
            "quantity" to quantity,
            "totalPrice" to totalPrice
        )
    }

    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("productId", productId)
        json.put("productName", productName)
        json.put("unitPrice", unitPrice)
        json.put("quantity", quantity)
        json.put("totalPrice", totalPrice)
        return json
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): FirebaseOrderItemDto {
            return FirebaseOrderItemDto(
                id = (map["id"] as? Number)?.toLong() ?: 0L,
                productId = (map["productId"] as? Number)?.toLong() ?: 0L,
                productName = map["productName"] as? String ?: "",
                unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                quantity = (map["quantity"] as? Number)?.toInt() ?: 1,
                totalPrice = (map["totalPrice"] as? Number)?.toDouble() ?: 0.0
            )
        }

        fun fromJsonObject(json: JSONObject): FirebaseOrderItemDto {
            return FirebaseOrderItemDto(
                id = json.optLong("id", 0L),
                productId = json.optLong("productId", 0L),
                productName = json.optString("productName", ""),
                unitPrice = json.optDouble("unitPrice", 0.0),
                quantity = json.optInt("quantity", 1),
                totalPrice = json.optDouble("totalPrice", 0.0)
            )
        }
    }
}

data class FirebaseShopDto(
    val id: Long,
    val ownerId: Long,
    val name: String,
    val category: String,
    val address: String,
    val phone: String,
    val upiId: String,
    val rating: Double,
    val reviewCount: Int,
    val isOpen: Boolean,
    val deliveryAvailable: Boolean,
    val minOrderAmount: Double,
    val estimatedDeliveryMins: Int,
    val isApproved: Boolean,
    val imageUrl: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("ownerId", ownerId)
        json.put("name", name)
        json.put("category", category)
        json.put("address", address)
        json.put("phone", phone)
        json.put("upiId", upiId)
        json.put("rating", rating)
        json.put("reviewCount", reviewCount)
        json.put("isOpen", isOpen)
        json.put("deliveryAvailable", deliveryAvailable)
        json.put("minOrderAmount", minOrderAmount)
        json.put("estimatedDeliveryMins", estimatedDeliveryMins)
        json.put("isApproved", isApproved)
        json.put("imageUrl", imageUrl)
        json.put("updatedAt", updatedAt)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): FirebaseShopDto {
            return FirebaseShopDto(
                id = json.optLong("id", 0L),
                ownerId = json.optLong("ownerId", 0L),
                name = json.optString("name", ""),
                category = json.optString("category", ""),
                address = json.optString("address", ""),
                phone = json.optString("phone", ""),
                upiId = json.optString("upiId", ""),
                rating = json.optDouble("rating", 4.5),
                reviewCount = json.optInt("reviewCount", 0),
                isOpen = json.optBoolean("isOpen", true),
                deliveryAvailable = json.optBoolean("deliveryAvailable", true),
                minOrderAmount = json.optDouble("minOrderAmount", 50.0),
                estimatedDeliveryMins = json.optInt("estimatedDeliveryMins", 30),
                isApproved = json.optBoolean("isApproved", true),
                imageUrl = json.optString("imageUrl", "hero_local_mart"),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}

data class FirebaseProductDto(
    val id: Long,
    val shopId: Long,
    val name: String,
    val category: String,
    val description: String,
    val price: Double,
    val discountPrice: Double?,
    val unit: String,
    val stock: Int,
    val isAvailable: Boolean,
    val imageUrl: String,
    val isApproved: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("shopId", shopId)
        json.put("name", name)
        json.put("category", category)
        json.put("description", description)
        json.put("price", price)
        if (discountPrice != null) json.put("discountPrice", discountPrice) else json.put("discountPrice", JSONObject.NULL)
        json.put("unit", unit)
        json.put("stock", stock)
        json.put("isAvailable", isAvailable)
        json.put("imageUrl", imageUrl)
        json.put("isApproved", isApproved)
        json.put("updatedAt", updatedAt)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): FirebaseProductDto {
            return FirebaseProductDto(
                id = json.optLong("id", 0L),
                shopId = json.optLong("shopId", 0L),
                name = json.optString("name", ""),
                category = json.optString("category", ""),
                description = json.optString("description", ""),
                price = json.optDouble("price", 0.0),
                discountPrice = if (json.isNull("discountPrice")) null else json.optDouble("discountPrice"),
                unit = json.optString("unit", "kg"),
                stock = json.optInt("stock", 0),
                isAvailable = json.optBoolean("isAvailable", true),
                imageUrl = json.optString("imageUrl", "hero_local_mart"),
                isApproved = json.optBoolean("isApproved", true),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}

data class FirebasePromotionDto(
    val id: Long,
    val shopId: Long,
    val shopName: String,
    val title: String,
    val description: String,
    val type: String,
    val status: String,
    val targetProductId: Long?,
    val targetProductName: String?,
    val location: String,
    val durationDays: Int,
    val startDate: String,
    val endDate: String,
    val dailyRate: Double,
    val totalPrice: Double,
    val paymentStatus: String,
    val adminNotes: String,
    val discountPercent: Double,
    val couponCode: String,
    val submittedAt: Long,
    val approvedAt: Long?
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("shopId", shopId)
        json.put("shopName", shopName)
        json.put("title", title)
        json.put("description", description)
        json.put("type", type)
        json.put("status", status)
        if (targetProductId != null) json.put("targetProductId", targetProductId) else json.put("targetProductId", JSONObject.NULL)
        if (targetProductName != null) json.put("targetProductName", targetProductName) else json.put("targetProductName", JSONObject.NULL)
        json.put("location", location)
        json.put("durationDays", durationDays)
        json.put("startDate", startDate)
        json.put("endDate", endDate)
        json.put("dailyRate", dailyRate)
        json.put("totalPrice", totalPrice)
        json.put("paymentStatus", paymentStatus)
        json.put("adminNotes", adminNotes)
        json.put("discountPercent", discountPercent)
        json.put("couponCode", couponCode)
        json.put("submittedAt", submittedAt)
        if (approvedAt != null) json.put("approvedAt", approvedAt) else json.put("approvedAt", JSONObject.NULL)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): FirebasePromotionDto {
            return FirebasePromotionDto(
                id = json.optLong("id", 0L),
                shopId = json.optLong("shopId", 0L),
                shopName = json.optString("shopName", ""),
                title = json.optString("title", ""),
                description = json.optString("description", ""),
                type = json.optString("type", PromotionType.BANNER.name),
                status = json.optString("status", PromotionStatus.PENDING_APPROVAL.name),
                targetProductId = if (json.isNull("targetProductId")) null else json.optLong("targetProductId"),
                targetProductName = if (json.isNull("targetProductName")) null else json.optString("targetProductName"),
                location = json.optString("location", "Sinapali"),
                durationDays = json.optInt("durationDays", 7),
                startDate = json.optString("startDate", ""),
                endDate = json.optString("endDate", ""),
                dailyRate = json.optDouble("dailyRate", 50.0),
                totalPrice = json.optDouble("totalPrice", 350.0),
                paymentStatus = json.optString("paymentStatus", "Pending Payment"),
                adminNotes = json.optString("adminNotes", ""),
                discountPercent = json.optDouble("discountPercent", 0.0),
                couponCode = json.optString("couponCode", ""),
                submittedAt = json.optLong("submittedAt", System.currentTimeMillis()),
                approvedAt = if (json.isNull("approvedAt")) null else json.optLong("approvedAt")
            )
        }
    }
}

data class FirebaseComplaintDto(
    val id: Long,
    val orderId: Long,
    val userId: Long,
    val shopId: Long,
    val issueType: String,
    val description: String,
    val status: String,
    val resolutionNotes: String,
    val filedAt: Long
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("orderId", orderId)
        json.put("userId", userId)
        json.put("shopId", shopId)
        json.put("issueType", issueType)
        json.put("description", description)
        json.put("status", status)
        json.put("resolutionNotes", resolutionNotes)
        json.put("filedAt", filedAt)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): FirebaseComplaintDto {
            return FirebaseComplaintDto(
                id = json.optLong("id", 0L),
                orderId = json.optLong("orderId", 0L),
                userId = json.optLong("userId", 0L),
                shopId = json.optLong("shopId", 0L),
                issueType = json.optString("issueType", ""),
                description = json.optString("description", ""),
                status = json.optString("status", ComplaintStatus.OPEN.name),
                resolutionNotes = json.optString("resolutionNotes", ""),
                filedAt = json.optLong("filedAt", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Metadata record for sync tracking and conflict resolution
 */
data class FirebaseSyncMetadata(
    val schemaVersion: Int = 1,
    val entityName: String,
    val documentKey: String,
    val localId: Long,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "PREPARED",
    val checksum: String = ""
)

// ==========================================
// MAPPERS: Room Entity <-> Firebase DTO
// ==========================================

fun OrderEntity.toFirebaseDto(items: List<OrderItemEntity>): FirebaseOrderDto {
    return FirebaseOrderDto(
        orderNumber = this.orderNumber,
        localId = this.id,
        userId = this.userId,
        shopId = this.shopId,
        totalAmount = this.totalAmount,
        discountAmount = this.discountAmount,
        deliveryFee = this.deliveryFee,
        finalAmount = this.finalAmount,
        status = this.status.name,
        deliveryType = this.deliveryType.name,
        paymentMethod = this.paymentMethod.name,
        paymentStatus = this.paymentStatus.name,
        deliveryAddress = this.deliveryAddress,
        customerPhone = this.customerPhone,
        customerName = this.customerName,
        orderNotes = this.orderNotes,
        placedAt = this.placedAt,
        deliveryAgentAssigned = this.deliveryAgentAssigned,
        estimatedDeliveryTime = this.estimatedDeliveryTime,
        items = items.map { it.toFirebaseDto() },
        syncedAt = System.currentTimeMillis(),
        deviceSource = "android_client"
    )
}

fun OrderItemEntity.toFirebaseDto(): FirebaseOrderItemDto {
    return FirebaseOrderItemDto(
        id = this.id,
        productId = this.productId,
        productName = this.productName,
        unitPrice = this.unitPrice,
        quantity = this.quantity,
        totalPrice = this.totalPrice
    )
}

fun FirebaseOrderDto.toOrderEntity(resolvedLocalId: Long = 0L): OrderEntity {
    val parsedStatus = try {
        OrderStatus.valueOf(this.status)
    } catch (_: Throwable) {
        OrderStatus.PLACED
    }

    val parsedDeliveryType = try {
        DeliveryType.valueOf(this.deliveryType)
    } catch (_: Throwable) {
        DeliveryType.HOME_DELIVERY
    }

    val parsedPaymentMethod = try {
        PaymentMethod.valueOf(this.paymentMethod)
    } catch (_: Throwable) {
        PaymentMethod.CASH_ON_DELIVERY
    }

    val parsedPaymentStatus = try {
        PaymentStatus.valueOf(this.paymentStatus)
    } catch (_: Throwable) {
        PaymentStatus.PENDING
    }

    return OrderEntity(
        id = if (resolvedLocalId > 0) resolvedLocalId else this.localId,
        orderNumber = this.orderNumber,
        userId = this.userId,
        shopId = this.shopId,
        totalAmount = this.totalAmount,
        discountAmount = this.discountAmount,
        deliveryFee = this.deliveryFee,
        finalAmount = this.finalAmount,
        status = parsedStatus,
        deliveryType = parsedDeliveryType,
        paymentMethod = parsedPaymentMethod,
        paymentStatus = parsedPaymentStatus,
        deliveryAddress = this.deliveryAddress,
        customerPhone = this.customerPhone,
        customerName = this.customerName,
        orderNotes = this.orderNotes,
        placedAt = this.placedAt,
        deliveryAgentAssigned = this.deliveryAgentAssigned,
        estimatedDeliveryTime = this.estimatedDeliveryTime
    )
}

fun FirebaseOrderItemDto.toOrderItemEntity(orderId: Long): OrderItemEntity {
    return OrderItemEntity(
        id = 0L, // Room autogenerates local PK
        orderId = orderId,
        productId = this.productId,
        productName = this.productName,
        unitPrice = this.unitPrice,
        quantity = this.quantity,
        totalPrice = this.totalPrice
    )
}

fun ShopEntity.toFirebaseDto(): FirebaseShopDto {
    return FirebaseShopDto(
        id = this.id,
        ownerId = this.ownerId,
        name = this.name,
        category = this.category,
        address = this.address,
        phone = this.phone,
        upiId = this.upiId,
        rating = this.rating,
        reviewCount = this.reviewCount,
        isOpen = this.isOpen,
        deliveryAvailable = this.deliveryAvailable,
        minOrderAmount = this.minOrderAmount,
        estimatedDeliveryMins = this.estimatedDeliveryMins,
        isApproved = this.isApproved,
        imageUrl = this.imageUrl,
        updatedAt = System.currentTimeMillis()
    )
}

fun FirebaseShopDto.toEntity(): ShopEntity {
    return ShopEntity(
        id = this.id,
        ownerId = this.ownerId,
        name = this.name,
        category = this.category,
        address = this.address,
        phone = this.phone,
        upiId = this.upiId,
        rating = this.rating,
        reviewCount = this.reviewCount,
        isOpen = this.isOpen,
        deliveryAvailable = this.deliveryAvailable,
        minOrderAmount = this.minOrderAmount,
        estimatedDeliveryMins = this.estimatedDeliveryMins,
        isApproved = this.isApproved,
        imageUrl = this.imageUrl
    )
}

fun ProductEntity.toFirebaseDto(): FirebaseProductDto {
    return FirebaseProductDto(
        id = this.id,
        shopId = this.shopId,
        name = this.name,
        category = this.category,
        description = this.description,
        price = this.price,
        discountPrice = this.discountPrice,
        unit = this.unit,
        stock = this.stock,
        isAvailable = this.isAvailable,
        imageUrl = this.imageUrl,
        isApproved = this.isApproved,
        updatedAt = System.currentTimeMillis()
    )
}

fun FirebaseProductDto.toEntity(): ProductEntity {
    return ProductEntity(
        id = this.id,
        shopId = this.shopId,
        name = this.name,
        category = this.category,
        description = this.description,
        price = this.price,
        discountPrice = this.discountPrice,
        unit = this.unit,
        stock = this.stock,
        isAvailable = this.isAvailable,
        imageUrl = this.imageUrl,
        isApproved = this.isApproved
    )
}

fun PromotionEntity.toFirebaseDto(): FirebasePromotionDto {
    return FirebasePromotionDto(
        id = this.id,
        shopId = this.shopId,
        shopName = this.shopName,
        title = this.title,
        description = this.description,
        type = this.type.name,
        status = this.status.name,
        targetProductId = this.targetProductId,
        targetProductName = this.targetProductName,
        location = this.location,
        durationDays = this.durationDays,
        startDate = this.startDate,
        endDate = this.endDate,
        dailyRate = this.dailyRate,
        totalPrice = this.totalPrice,
        paymentStatus = this.paymentStatus,
        adminNotes = this.adminNotes,
        discountPercent = this.discountPercent,
        couponCode = this.couponCode,
        submittedAt = this.submittedAt,
        approvedAt = this.approvedAt
    )
}

fun FirebasePromotionDto.toEntity(): PromotionEntity {
    val parsedType = try {
        PromotionType.valueOf(this.type)
    } catch (_: Throwable) {
        PromotionType.BANNER
    }
    val parsedStatus = try {
        PromotionStatus.valueOf(this.status)
    } catch (_: Throwable) {
        PromotionStatus.PENDING_APPROVAL
    }

    return PromotionEntity(
        id = this.id,
        shopId = this.shopId,
        shopName = this.shopName,
        title = this.title,
        description = this.description,
        type = parsedType,
        status = parsedStatus,
        targetProductId = this.targetProductId,
        targetProductName = this.targetProductName,
        location = this.location,
        durationDays = this.durationDays,
        startDate = this.startDate,
        endDate = this.endDate,
        dailyRate = this.dailyRate,
        totalPrice = this.totalPrice,
        paymentStatus = this.paymentStatus,
        adminNotes = this.adminNotes,
        discountPercent = this.discountPercent,
        couponCode = this.couponCode,
        submittedAt = this.submittedAt,
        approvedAt = this.approvedAt
    )
}

fun ComplaintEntity.toFirebaseDto(): FirebaseComplaintDto {
    return FirebaseComplaintDto(
        id = this.id,
        orderId = this.orderId,
        userId = this.userId,
        shopId = this.shopId,
        issueType = this.issueType,
        description = this.description,
        status = this.status.name,
        resolutionNotes = this.resolutionNotes,
        filedAt = this.filedAt
    )
}

fun FirebaseComplaintDto.toEntity(): ComplaintEntity {
    val parsedStatus = try {
        ComplaintStatus.valueOf(this.status)
    } catch (_: Throwable) {
        ComplaintStatus.OPEN
    }

    return ComplaintEntity(
        id = this.id,
        orderId = this.orderId,
        userId = this.userId,
        shopId = this.shopId,
        issueType = this.issueType,
        description = this.description,
        status = parsedStatus,
        resolutionNotes = this.resolutionNotes,
        filedAt = this.filedAt
    )
}
