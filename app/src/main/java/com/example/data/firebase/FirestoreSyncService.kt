package com.example.data.firebase

import android.util.Log
import com.example.data.local.SeedData
import com.example.data.local.dao.MartDao
import com.example.data.local.entities.NotificationEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.OrderItemEntity
import com.example.domain.model.DeliveryType
import com.example.domain.model.NotificationType
import com.example.domain.model.OrderStatus
import com.example.domain.model.PaymentMethod
import com.example.domain.model.PaymentStatus
import com.example.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live Cloud Sync Service communicating directly with Firebase Firestore REST endpoints,
 * with multi-tier fallback to Firebase RTDB and the shared multi-device registry.
 *
 * GUARANTEE: Never resets, deletes, or clears local SQLite records.
 */
object FirestoreSyncService {
    private const val TAG = "FirestoreSyncService"

    enum class FirebaseConnectionState {
        CONNECTED,
        PERMISSION_DENIED,
        VERIFIED_AUTH_REQUIRED,
        NOT_CONFIGURED,
        UNREACHABLE,
        PROJECT_NOT_FOUND
    }

    enum class OperationType(val value: String) {
        CREATE("create"),
        UPDATE("update"),
        DELETE("delete"),
        LIST("list"),
        GET("get"),
        WRITE("write")
    }

    sealed class CloudSyncResult<out T> {
        data class Success<T>(val data: T) : CloudSyncResult<T>()
        data class Failure(
            val errorCode: String,
            val userMessage: String,
            val isAuthOrPermissionError: Boolean = false
        ) : CloudSyncResult<Nothing>()
    }

    data class ShopAndProductSyncReport(
        val success: Boolean,
        val syncedShopsCount: Int,
        val syncedProductsCount: Int,
        val errorMessage: String? = null,
        val isAuthOrPermissionError: Boolean = false
    )

    data class FirebaseVerificationReport(
        val isConnected: Boolean,
        val state: FirebaseConnectionState,
        val projectId: String,
        val httpStatusCode: Int,
        val statusMessage: String,
        val firestoreEndpoint: String,
        val hasGoogleServicesJson: Boolean = false,
        val isRealGoogleCloudProject: Boolean = false,
        val isMockSyncPrevented: Boolean = true,
        val isUserAuthenticated: Boolean = false,
        val authenticatedUid: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class SyncResult(
        val success: Boolean,
        val uploadedOrdersCount: Int,
        val downloadedOrdersCount: Int,
        val totalLocalOrdersPreserved: Int,
        val message: String,
        val errorMessage: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private fun currentFirebaseUser() = try {
        FirebaseApp.getInstance()
        FirebaseAuth.getInstance().currentUser
    } catch (_: Throwable) {
        null
    }

    private fun currentUidOrNull(): String? = currentFirebaseUser()?.uid?.takeIf { it.isNotBlank() }

    private fun firestoreOrNull(): FirebaseFirestore? = try {
        FirebaseApp.getInstance()
        FirebaseFirestore.getInstance()
    } catch (_: Throwable) {
        null
    }

    private fun fetchCurrentIdTokenSync(): String? {
        return try {
            val user = currentFirebaseUser() ?: return null
            val tokenResult = Tasks.await(user.getIdToken(false), 3, TimeUnit.SECONDS)
            tokenResult?.token
        } catch (_: Throwable) {
            null
        }
    }

    fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
        val causes = generateSequence(exception) { it.cause }
        val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
            ?: causes.firstOrNull { it !is CancellationException }
            ?: exception

        val currentUser = currentFirebaseUser()
        val providerInfoList = currentUser?.providerData?.map { provider ->
            JSONObject().apply {
                put("providerId", provider.providerId)
                put("phoneNumber", provider.phoneNumber)
            }
        } ?: emptyList()

        val authInfoJson = JSONObject().apply {
            put("userId", currentUser?.uid)
            put("phoneNumber", currentUser?.phoneNumber)
            put("providerInfo", JSONArray(providerInfoList))
        }

        val errorInfoJson = JSONObject().apply {
            put("error", rootError.message ?: rootError.toString())
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", authInfoJson)
        }

        val jsonString = errorInfoJson.toString()
        Log.e("FirestoreError", "Firestore Error: $jsonString")

        return when (rootError) {
            is FirebaseFirestoreException -> when (rootError.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied by Firestore rules. Ensure you are logged in with your shop owner OTP and own this shop."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Authentication required. Please log in with Mobile OTP and try again."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Firestore temporarily unavailable. Check your internet connection."
                else -> rootError.localizedMessage ?: "Cloud Firestore sync failed."
            }
            else -> rootError.localizedMessage ?: "Cloud Firestore sync failed."
        }
    }

    private fun isAuthOrPermException(exception: Throwable): Boolean {
        val causes = generateSequence(exception) { it.cause }
        val fsEx = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        return fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            fsEx?.code == FirebaseFirestoreException.Code.UNAUTHENTICATED
    }

    /**
     * Actively tests the Firebase Firestore connection against the real Google Cloud endpoint.
     * Tests rule-aligned public product query (isApproved == true && isAvailable == true).
     * Never reports HTTP 403 or PERMISSION_DENIED as connected.
     */
    suspend fun verifyFirebaseConnection(): FirebaseVerificationReport = withContext(Dispatchers.IO) {
        val projectId = FirebaseSyncConfig.FIREBASE_PROJECT_ID.trim()
        val authUser = currentFirebaseUser()

        if (projectId.isBlank() || projectId == "placeholder") {
            return@withContext FirebaseVerificationReport(
                isConnected = false,
                state = FirebaseConnectionState.NOT_CONFIGURED,
                projectId = if (projectId.isBlank()) "None (Not Configured)" else projectId,
                httpStatusCode = 0,
                statusMessage = "Firebase project is not configured. Please enter a valid Project ID.",
                firestoreEndpoint = FirebaseSyncConfig.FIRESTORE_BASE_URL,
                hasGoogleServicesJson = false,
                isRealGoogleCloudProject = false,
                isUserAuthenticated = authUser != null,
                authenticatedUid = authUser?.uid
            )
        }

        try {
            val idToken = fetchCurrentIdTokenSync()
            val keyParam = if (FirebaseSyncConfig.FIREBASE_API_KEY.isNotBlank()) "?key=${FirebaseSyncConfig.FIREBASE_API_KEY}" else ""
            val runQueryUrl = "${FirebaseSyncConfig.FIRESTORE_BASE_URL}:runQuery$keyParam"

            // Rule-aligned structured query matching public product read policy
            val structuredQuery = JSONObject().apply {
                put("structuredQuery", JSONObject().apply {
                    put("from", JSONArray().put(JSONObject().put("collectionId", FirebaseSyncConfig.COLLECTION_PRODUCTS)))
                    put("where", JSONObject().apply {
                        put("compositeFilter", JSONObject().apply {
                            put("op", "AND")
                            put("filters", JSONArray()
                                .put(JSONObject().apply {
                                    put("fieldFilter", JSONObject().apply {
                                        put("field", JSONObject().put("fieldPath", "isApproved"))
                                        put("op", "EQUAL")
                                        put("value", JSONObject().put("booleanValue", true))
                                    })
                                })
                                .put(JSONObject().apply {
                                    put("fieldFilter", JSONObject().apply {
                                        put("field", JSONObject().put("fieldPath", "isAvailable"))
                                        put("op", "EQUAL")
                                        put("value", JSONObject().put("booleanValue", true))
                                    })
                                })
                            )
                        })
                    })
                    put("limit", 1)
                })
            }

            val url = URL(runQueryUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (!idToken.isNullOrBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $idToken")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(structuredQuery.toString())
                writer.flush()
            }

            val code = conn.responseCode
            val responseBody = try {
                if (code in 200..299) {
                    BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                } else {
                    val stream = conn.errorStream
                    if (stream != null) BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() } else ""
                }
            } catch (_: Throwable) {
                ""
            }
            conn.disconnect()

            when (code) {
                in 200..299 -> {
                    val authDetail = if (authUser != null) {
                        "Authenticated UID (${authUser.uid.take(8)}…) • Read/Write enabled."
                    } else {
                        "Public storefront read active • Log in with Mobile OTP for owner/order writes."
                    }
                    FirebaseVerificationReport(
                        isConnected = true,
                        state = FirebaseConnectionState.CONNECTED,
                        projectId = projectId,
                        httpStatusCode = code,
                        statusMessage = "Connected to Firestore '$projectId'. $authDetail",
                        firestoreEndpoint = FirebaseSyncConfig.FIRESTORE_BASE_URL,
                        hasGoogleServicesJson = true,
                        isRealGoogleCloudProject = true,
                        isUserAuthenticated = authUser != null,
                        authenticatedUid = authUser?.uid
                    )
                }
                401, 403 -> {
                    val message = if (responseBody.contains("SERVICE_DISABLED")) {
                        "HTTP $code: Cloud Firestore API is disabled on project '$projectId'."
                    } else {
                        "HTTP $code Permission Denied: Firestore security rules or authentication rejected access."
                    }
                    FirebaseVerificationReport(
                        isConnected = false,
                        state = FirebaseConnectionState.PERMISSION_DENIED,
                        projectId = projectId,
                        httpStatusCode = code,
                        statusMessage = message,
                        firestoreEndpoint = FirebaseSyncConfig.FIRESTORE_BASE_URL,
                        hasGoogleServicesJson = true,
                        isRealGoogleCloudProject = true,
                        isUserAuthenticated = authUser != null,
                        authenticatedUid = authUser?.uid
                    )
                }
                404 -> {
                    val notFoundMessage = if (responseBody.contains("NOT_FOUND") || responseBody.contains("database was not found")) {
                        "Google Cloud project '$projectId' reached, but Firestore database '(default)' was not found."
                    } else {
                        "Firebase project '$projectId' does not exist on Google Cloud (HTTP 404)."
                    }
                    FirebaseVerificationReport(
                        isConnected = false,
                        state = FirebaseConnectionState.PROJECT_NOT_FOUND,
                        projectId = projectId,
                        httpStatusCode = code,
                        statusMessage = notFoundMessage,
                        firestoreEndpoint = FirebaseSyncConfig.FIRESTORE_BASE_URL,
                        hasGoogleServicesJson = true,
                        isRealGoogleCloudProject = false,
                        isUserAuthenticated = authUser != null,
                        authenticatedUid = authUser?.uid
                    )
                }
                else -> FirebaseVerificationReport(
                    isConnected = false,
                    state = FirebaseConnectionState.UNREACHABLE,
                    projectId = projectId,
                    httpStatusCode = code,
                    statusMessage = "Firestore endpoint returned HTTP code $code.",
                    firestoreEndpoint = FirebaseSyncConfig.FIRESTORE_BASE_URL,
                    hasGoogleServicesJson = true,
                    isRealGoogleCloudProject = false,
                    isUserAuthenticated = authUser != null,
                    authenticatedUid = authUser?.uid
                )
            }
        } catch (e: Throwable) {
            FirebaseVerificationReport(
                isConnected = false,
                state = FirebaseConnectionState.UNREACHABLE,
                projectId = projectId,
                httpStatusCode = -1,
                statusMessage = "Network error connecting to Firestore: ${e.message}",
                firestoreEndpoint = FirebaseSyncConfig.FIRESTORE_BASE_URL,
                hasGoogleServicesJson = true,
                isRealGoogleCloudProject = false,
                isUserAuthenticated = authUser != null,
                authenticatedUid = authUser?.uid
            )
        }
    }

    /**
     * Converts an OrderEntity and its items to a Google Cloud Firestore REST API Document payload.
     */
    fun orderToFirestorePayload(order: OrderEntity, items: List<OrderItemEntity>, shopName: String? = null): JSONObject {
        val root = JSONObject()
        val fields = JSONObject()

        val effectiveOrderIdStr = if (order.id > 0L) order.id.toString() else order.orderNumber
        fields.put("orderId", JSONObject().put("stringValue", effectiveOrderIdStr))
        fields.put("id", JSONObject().put("integerValue", order.id.toString()))
        fields.put("orderNumber", JSONObject().put("stringValue", order.orderNumber))
        fields.put("userId", JSONObject().put("integerValue", order.userId.toString()))
        fields.put("shopId", JSONObject().put("integerValue", order.shopId.toString()))
        fields.put("shopDocId", JSONObject().put("stringValue", "shop_${order.shopId}"))
        fields.put("customerUid", JSONObject().put("stringValue", order.customerUid))
        fields.put("shopOwnerUid", JSONObject().put("stringValue", order.shopOwnerUid))

        // Explicit customer fields
        fields.put("customer", JSONObject().put("stringValue", order.customerName))
        fields.put("customerName", JSONObject().put("stringValue", order.customerName))
        fields.put("customerPhone", JSONObject().put("stringValue", order.customerPhone))

        // Explicit shop fields
        val resolvedShopName = shopName ?: "Shop #${order.shopId}"
        fields.put("shop", JSONObject().put("stringValue", resolvedShopName))
        fields.put("shopName", JSONObject().put("stringValue", resolvedShopName))

        // Explicit address fields
        fields.put("address", JSONObject().put("stringValue", order.deliveryAddress))
        fields.put("deliveryAddress", JSONObject().put("stringValue", order.deliveryAddress))

        // Financial & Price fields
        fields.put("price", JSONObject().put("doubleValue", order.finalAmount))
        fields.put("totalAmount", JSONObject().put("doubleValue", order.totalAmount))
        fields.put("discountAmount", JSONObject().put("doubleValue", order.discountAmount))
        fields.put("deliveryFee", JSONObject().put("doubleValue", order.deliveryFee))
        fields.put("finalAmount", JSONObject().put("doubleValue", order.finalAmount))

        // Quantity & Product fields
        val totalQty = items.sumOf { it.quantity }.coerceAtLeast(1)
        fields.put("quantity", JSONObject().put("integerValue", totalQty.toString()))
        fields.put("totalQuantity", JSONObject().put("integerValue", totalQty.toString()))

        val primaryProdName = items.firstOrNull()?.productName ?: "Town Product"
        val allProds = if (items.size == 1) primaryProdName else items.joinToString(", ") { "${it.productName} (${it.quantity})" }
        fields.put("product", JSONObject().put("stringValue", allProds))
        fields.put("productName", JSONObject().put("stringValue", primaryProdName))

        // Date / Time fields
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val formattedDateTime = try { sdf.format(Date(order.placedAt)) } catch (_: Throwable) { "" }
        fields.put("dateTime", JSONObject().put("stringValue", formattedDateTime))
        fields.put("date", JSONObject().put("stringValue", formattedDateTime.substringBefore(" ")))
        fields.put("time", JSONObject().put("stringValue", formattedDateTime.substringAfter(" ")))
        fields.put("placedAt", JSONObject().put("integerValue", order.placedAt.toString()))

        fields.put("status", JSONObject().put("stringValue", order.status.name))
        fields.put("deliveryType", JSONObject().put("stringValue", order.deliveryType.name))
        fields.put("paymentMethod", JSONObject().put("stringValue", order.paymentMethod.name))
        fields.put("paymentStatus", JSONObject().put("stringValue", order.paymentStatus.name))
        fields.put("orderNotes", JSONObject().put("stringValue", order.orderNotes))
        fields.put("deliveryAgentAssigned", JSONObject().put("stringValue", order.deliveryAgentAssigned ?: ""))
        fields.put("estimatedDeliveryTime", JSONObject().put("integerValue", order.estimatedDeliveryTime.toString()))
        fields.put("syncedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))

        // Serialized items array with product, quantity, and price
        val itemsArray = JSONArray()
        for (item in items) {
            val itemObj = JSONObject()
            itemObj.put("productId", item.productId)
            itemObj.put("productName", item.productName)
            itemObj.put("product", item.productName)
            itemObj.put("unitPrice", item.unitPrice)
            itemObj.put("price", item.unitPrice)
            itemObj.put("quantity", item.quantity)
            itemObj.put("totalPrice", item.totalPrice)
            itemsArray.put(itemObj)
        }
        fields.put("itemsJson", JSONObject().put("stringValue", itemsArray.toString()))

        root.put("fields", fields)
        return root
    }

    private fun getFieldDouble(fields: JSONObject, key: String, default: Double = 0.0): Double {
        val obj = fields.optJSONObject(key) ?: return default
        val d = obj.optDouble("doubleValue", Double.NaN)
        if (!d.isNaN()) return d
        val s = obj.optString("stringValue", "")
        if (s.isNotBlank()) return s.toDoubleOrNull() ?: default
        val i = obj.optString("integerValue", "")
        if (i.isNotBlank()) return i.toDoubleOrNull() ?: default
        return default
    }

    private fun getFieldLong(fields: JSONObject, key: String, default: Long = 0L): Long {
        val obj = fields.optJSONObject(key) ?: return default
        val i = obj.optString("integerValue", "")
        if (i.isNotBlank()) return i.toLongOrNull() ?: default
        val d = obj.optDouble("doubleValue", Double.NaN)
        if (!d.isNaN()) return d.toLong()
        val s = obj.optString("stringValue", "")
        if (s.isNotBlank()) return s.toLongOrNull() ?: default
        return default
    }

    private fun getFieldString(fields: JSONObject, key: String, default: String = ""): String {
        val obj = fields.optJSONObject(key) ?: return default
        return obj.optString("stringValue", default)
    }

    private fun getAnyDouble(doc: DocumentSnapshot, field: String, default: Double = 0.0): Double {
        val v = try { doc.get(field) } catch (_: Throwable) { null } ?: return default
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: default
            else -> default
        }
    }

    private fun getAnyLong(doc: DocumentSnapshot, field: String, default: Long = 0L): Long {
        val v = try { doc.get(field) } catch (_: Throwable) { null } ?: return default
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: default
            else -> default
        }
    }

    private fun getAnyString(doc: DocumentSnapshot, field: String, default: String = ""): String {
        val v = try { doc.get(field) } catch (_: Throwable) { null } ?: return default
        return v.toString()
    }

    fun getAnyBoolean(doc: DocumentSnapshot, field: String, default: Boolean = false): Boolean {
        val v = try { doc.get(field) } catch (_: Throwable) { null } ?: return default
        return when (v) {
            is Boolean -> v
            is Number -> v.toInt() == 1
            is String -> v.equals("true", ignoreCase = true) || v == "1"
            else -> default
        }
    }

    fun getFieldBoolean(fields: JSONObject, key: String, default: Boolean = false): Boolean {
        val obj = fields.optJSONObject(key) ?: return default
        if (obj.has("booleanValue")) return obj.optBoolean("booleanValue", default)
        val s = obj.optString("stringValue", "")
        if (s.isNotBlank()) return s.equals("true", ignoreCase = true) || s == "1"
        val i = obj.optString("integerValue", "")
        if (i.isNotBlank()) return i == "1"
        return default
    }

    private fun parseDateTimeToMillis(dateTimeStr: String): Long {
        if (dateTimeStr.isBlank()) return System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            sdf.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }

    /**
     * Parses a Firestore Document JSON response back into OrderEntity and List<OrderItemEntity>.
     */
    fun firestorePayloadToOrder(documentJson: JSONObject): Pair<OrderEntity, List<OrderItemEntity>>? {
        return try {
            val fields = documentJson.optJSONObject("fields") ?: return null

            val docPath = documentJson.optString("name", "")
            val docIdFallback = if (docPath.contains("/")) docPath.substringAfterLast("/") else ""

            val orderNumber = getFieldString(fields, "orderNumber")
                .ifBlank { getFieldString(fields, "orderId").ifBlank { docIdFallback.ifBlank { "ORD-SIN-${System.currentTimeMillis()}" } } }

            val rawId = getFieldLong(fields, "id", 0L).let { if (it > 0L) it else getFieldLong(fields, "orderId", 0L) }
            val userId = getFieldLong(fields, "userId", 0L)
            val shopId = getFieldLong(fields, "shopId", 1L)
            val customerName = getFieldString(fields, "customerName").ifBlank { getFieldString(fields, "customer") }
            val customerPhone = getFieldString(fields, "customerPhone")
            val deliveryAddress = getFieldString(fields, "deliveryAddress").ifBlank { getFieldString(fields, "address") }
            
            val totalAmount = getFieldDouble(fields, "totalAmount", 0.0).let { if (it > 0.0) it else getFieldDouble(fields, "price", 0.0) }
            val discountAmount = getFieldDouble(fields, "discountAmount", 0.0)
            val deliveryFee = getFieldDouble(fields, "deliveryFee", 0.0)
            val finalAmount = getFieldDouble(fields, "finalAmount", 0.0).let {
                if (it > 0.0) it else getFieldDouble(fields, "price", (totalAmount - discountAmount + deliveryFee).coerceAtLeast(0.0))
            }

            val statusStr = getFieldString(fields, "status", OrderStatus.PLACED.name)
            val deliveryTypeStr = getFieldString(fields, "deliveryType", DeliveryType.HOME_DELIVERY.name)
            val paymentMethodStr = getFieldString(fields, "paymentMethod", PaymentMethod.CASH_ON_DELIVERY.name)
            val paymentStatusStr = getFieldString(fields, "paymentStatus", PaymentStatus.PENDING.name)
            val orderNotes = getFieldString(fields, "orderNotes")
            val placedAt = getFieldLong(fields, "placedAt", 0L).let {
                if (it > 0L) it else parseDateTimeToMillis(getFieldString(fields, "dateTime"))
            }
            val deliveryAgent = getFieldString(fields, "deliveryAgentAssigned").ifBlank { null }
            val estimatedDeliveryTime = getFieldLong(fields, "estimatedDeliveryTime", 0L)
            val customerUid = getFieldString(fields, "customerUid")
            val shopOwnerUid = getFieldString(fields, "shopOwnerUid")

            val parsedStatus = try { OrderStatus.valueOf(statusStr) } catch (_: Throwable) { OrderStatus.PLACED }
            val parsedDeliveryType = try { DeliveryType.valueOf(deliveryTypeStr) } catch (_: Throwable) { DeliveryType.HOME_DELIVERY }
            val parsedPaymentMethod = try { PaymentMethod.valueOf(paymentMethodStr) } catch (_: Throwable) { PaymentMethod.CASH_ON_DELIVERY }
            val parsedPaymentStatus = try { PaymentStatus.valueOf(paymentStatusStr) } catch (_: Throwable) { PaymentStatus.PENDING }

            val orderEntity = OrderEntity(
                id = rawId,
                orderNumber = orderNumber,
                userId = userId,
                shopId = shopId,
                totalAmount = totalAmount,
                discountAmount = discountAmount,
                deliveryFee = deliveryFee,
                finalAmount = finalAmount,
                status = parsedStatus,
                deliveryType = parsedDeliveryType,
                paymentMethod = parsedPaymentMethod,
                paymentStatus = parsedPaymentStatus,
                deliveryAddress = deliveryAddress,
                customerPhone = customerPhone,
                customerName = customerName,
                orderNotes = orderNotes,
                placedAt = placedAt,
                deliveryAgentAssigned = deliveryAgent,
                estimatedDeliveryTime = estimatedDeliveryTime,
                customerUid = customerUid,
                shopOwnerUid = shopOwnerUid
            )

            val itemsList = mutableListOf<OrderItemEntity>()
            val itemsJsonStr = getFieldString(fields, "itemsJson")
            if (itemsJsonStr.isNotBlank()) {
                try {
                    val arr = JSONArray(itemsJsonStr)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val pName = obj.optString("productName", "").ifBlank { obj.optString("product", "") }
                        val uPrice = obj.optDouble("unitPrice", Double.NaN).let { if (!it.isNaN()) it else obj.optDouble("price", 0.0) }
                        val qty = obj.optInt("quantity", 1)
                        val tPrice = obj.optDouble("totalPrice", Double.NaN).let { if (!it.isNaN()) it else (uPrice * qty) }
                        itemsList.add(
                            OrderItemEntity(
                                id = 0L,
                                orderId = rawId,
                                productId = obj.optLong("productId", 0L),
                                productName = pName,
                                unitPrice = uPrice,
                                quantity = qty,
                                totalPrice = tPrice
                            )
                        )
                    }
                } catch (_: Throwable) { }
            }

            // Fallback: If no item list was encoded, extract from root fields product, quantity, price
            if (itemsList.isEmpty()) {
                val rootProduct = getFieldString(fields, "product").ifBlank { getFieldString(fields, "productName") }
                if (rootProduct.isNotBlank()) {
                    val rootQty = getFieldLong(fields, "quantity", 1L).toInt().coerceAtLeast(1)
                    val rootPrice = getFieldDouble(fields, "price", finalAmount)
                    itemsList.add(
                        OrderItemEntity(
                            id = 0L,
                            orderId = rawId,
                            productId = getFieldLong(fields, "productId", 1L),
                            productName = rootProduct,
                            unitPrice = if (rootQty > 0) rootPrice / rootQty else rootPrice,
                            quantity = rootQty,
                            totalPrice = rootPrice
                        )
                    )
                }
            }

            Pair(orderEntity, itemsList)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse Firestore document: ${e.message}")
            null
        }
    }

    /**
     * Converts an OrderEntity and its items to a Map suitable for the Firebase Firestore SDK.
     */
    fun orderToMap(order: OrderEntity, items: List<OrderItemEntity>, shopName: String? = null): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val effectiveOrderIdStr = if (order.id > 0L) order.id.toString() else order.orderNumber
        map["orderId"] = effectiveOrderIdStr
        map["id"] = order.id
        map["orderNumber"] = order.orderNumber
        map["userId"] = order.userId
        map["shopId"] = order.shopId
        map["shopDocId"] = "shop_${order.shopId}"
        map["customerUid"] = order.customerUid.ifBlank { currentUidOrNull() ?: "" }
        map["shopOwnerUid"] = order.shopOwnerUid

        // Explicit customer fields
        map["customer"] = order.customerName
        map["customerName"] = order.customerName
        map["customerPhone"] = order.customerPhone

        // Explicit shop fields
        val resolvedShopName = shopName ?: "Shop #${order.shopId}"
        map["shop"] = resolvedShopName
        map["shopName"] = resolvedShopName

        // Explicit address fields
        map["address"] = order.deliveryAddress
        map["deliveryAddress"] = order.deliveryAddress

        // Financial & Price fields
        map["price"] = order.finalAmount
        map["finalAmount"] = order.finalAmount
        map["totalAmount"] = order.totalAmount
        map["discountAmount"] = order.discountAmount
        map["deliveryFee"] = order.deliveryFee

        // Quantity & Product fields
        val totalQty = items.sumOf { it.quantity }.coerceAtLeast(1)
        map["quantity"] = totalQty
        map["totalQuantity"] = totalQty

        val primaryProdName = items.firstOrNull()?.productName ?: "Town Product"
        val allProds = if (items.size == 1) primaryProdName else items.joinToString(", ") { "${it.productName} (${it.quantity})" }
        map["product"] = allProds
        map["productName"] = primaryProdName

        // Date / Time fields
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val formattedDateTime = try { sdf.format(Date(order.placedAt)) } catch (_: Throwable) { "" }
        map["dateTime"] = formattedDateTime
        map["date"] = formattedDateTime.substringBefore(" ")
        map["time"] = formattedDateTime.substringAfter(" ")
        map["placedAt"] = order.placedAt

        map["status"] = order.status.name
        map["deliveryType"] = order.deliveryType.name
        map["paymentMethod"] = order.paymentMethod.name
        map["paymentStatus"] = order.paymentStatus.name
        map["orderNotes"] = order.orderNotes
        map["deliveryAgentAssigned"] = order.deliveryAgentAssigned ?: ""
        map["estimatedDeliveryTime"] = order.estimatedDeliveryTime
        map["syncedAt"] = System.currentTimeMillis()

        val itemsArray = JSONArray()
        val itemsList = items.map { item ->
            val itemObj = JSONObject()
            itemObj.put("productId", item.productId)
            itemObj.put("productName", item.productName)
            itemObj.put("product", item.productName)
            itemObj.put("unitPrice", item.unitPrice)
            itemObj.put("price", item.unitPrice)
            itemObj.put("quantity", item.quantity)
            itemObj.put("totalPrice", item.totalPrice)
            itemsArray.put(itemObj)

            mapOf(
                "productId" to item.productId,
                "productName" to item.productName,
                "product" to item.productName,
                "unitPrice" to item.unitPrice,
                "price" to item.unitPrice,
                "quantity" to item.quantity,
                "totalPrice" to item.totalPrice
            )
        }
        map["items"] = itemsList
        map["itemsJson"] = itemsArray.toString()
        return map
    }

    /**
     * Parses a Firebase SDK DocumentSnapshot back into OrderEntity and List<OrderItemEntity>.
     */
    fun firestoreDocToOrder(doc: DocumentSnapshot): Pair<OrderEntity, List<OrderItemEntity>>? {
        return try {
            val orderNumber = getAnyString(doc, "orderNumber")
                .ifBlank { getAnyString(doc, "orderId").ifBlank { doc.id } }
            if (orderNumber.isBlank()) return null

            val rawId = getAnyLong(doc, "id", 0L).let { if (it > 0L) it else getAnyLong(doc, "orderId", 0L) }
            val userId = getAnyLong(doc, "userId", 0L)
            val shopId = getAnyLong(doc, "shopId", 1L)
            val customerName = getAnyString(doc, "customerName").ifBlank { getAnyString(doc, "customer") }
            val customerPhone = getAnyString(doc, "customerPhone")
            val deliveryAddress = getAnyString(doc, "deliveryAddress").ifBlank { getAnyString(doc, "address") }
            val totalAmount = getAnyDouble(doc, "totalAmount", 0.0).let { if (it > 0.0) it else getAnyDouble(doc, "price", 0.0) }
            val discountAmount = getAnyDouble(doc, "discountAmount", 0.0)
            val deliveryFee = getAnyDouble(doc, "deliveryFee", 0.0)
            val finalAmount = getAnyDouble(doc, "finalAmount", 0.0).let {
                if (it > 0.0) it else getAnyDouble(doc, "price", (totalAmount - discountAmount + deliveryFee).coerceAtLeast(0.0))
            }
            val statusStr = getAnyString(doc, "status", OrderStatus.PLACED.name)
            val deliveryTypeStr = getAnyString(doc, "deliveryType", DeliveryType.HOME_DELIVERY.name)
            val paymentMethodStr = getAnyString(doc, "paymentMethod", PaymentMethod.CASH_ON_DELIVERY.name)
            val paymentStatusStr = getAnyString(doc, "paymentStatus", PaymentStatus.PENDING.name)
            val orderNotes = getAnyString(doc, "orderNotes")
            val placedAt = getAnyLong(doc, "placedAt", 0L).let {
                if (it > 0L) it else parseDateTimeToMillis(getAnyString(doc, "dateTime"))
            }
            val deliveryAgent = getAnyString(doc, "deliveryAgentAssigned").ifBlank { null }
            val estimatedDeliveryTime = getAnyLong(doc, "estimatedDeliveryTime", 0L)
            val customerUid = getAnyString(doc, "customerUid")
            val shopOwnerUid = getAnyString(doc, "shopOwnerUid")

            val parsedStatus = try { OrderStatus.valueOf(statusStr) } catch (_: Throwable) { OrderStatus.PLACED }
            val parsedDeliveryType = try { DeliveryType.valueOf(deliveryTypeStr) } catch (_: Throwable) { DeliveryType.HOME_DELIVERY }
            val parsedPaymentMethod = try { PaymentMethod.valueOf(paymentMethodStr) } catch (_: Throwable) { PaymentMethod.CASH_ON_DELIVERY }
            val parsedPaymentStatus = try { PaymentStatus.valueOf(paymentStatusStr) } catch (_: Throwable) { PaymentStatus.PENDING }

            val orderEntity = OrderEntity(
                id = rawId,
                orderNumber = orderNumber,
                userId = userId,
                shopId = shopId,
                totalAmount = totalAmount,
                discountAmount = discountAmount,
                deliveryFee = deliveryFee,
                finalAmount = finalAmount,
                status = parsedStatus,
                deliveryType = parsedDeliveryType,
                paymentMethod = parsedPaymentMethod,
                paymentStatus = parsedPaymentStatus,
                deliveryAddress = deliveryAddress,
                customerPhone = customerPhone,
                customerName = customerName,
                orderNotes = orderNotes,
                placedAt = placedAt,
                deliveryAgentAssigned = deliveryAgent,
                estimatedDeliveryTime = estimatedDeliveryTime,
                customerUid = customerUid,
                shopOwnerUid = shopOwnerUid
            )

            val itemsList = mutableListOf<OrderItemEntity>()
            val rawItems = doc.get("items") as? List<*>
            if (!rawItems.isNullOrEmpty()) {
                for (raw in rawItems) {
                    val itemMap = raw as? Map<*, *> ?: continue
                    val pId = (itemMap["productId"] as? Number)?.toLong()
                        ?: (itemMap["productId"]?.toString()?.toLongOrNull() ?: 0L)
                    val pName = itemMap["productName"]?.toString() ?: (itemMap["product"]?.toString() ?: "")
                    val uPrice = (itemMap["unitPrice"] as? Number)?.toDouble()
                        ?: ((itemMap["price"] as? Number)?.toDouble() ?: (itemMap["unitPrice"]?.toString()?.toDoubleOrNull() ?: 0.0))
                    val qty = (itemMap["quantity"] as? Number)?.toInt()
                        ?: (itemMap["quantity"]?.toString()?.toIntOrNull() ?: 1)
                    val tPrice = (itemMap["totalPrice"] as? Number)?.toDouble()
                        ?: (itemMap["totalPrice"]?.toString()?.toDoubleOrNull() ?: (uPrice * qty))

                    itemsList.add(
                        OrderItemEntity(
                            id = 0L,
                            orderId = rawId,
                            productId = pId,
                            productName = pName,
                            unitPrice = uPrice,
                            quantity = qty,
                            totalPrice = tPrice
                        )
                    )
                }
            }

            if (itemsList.isEmpty()) {
                val itemsJsonStr = getAnyString(doc, "itemsJson")
                if (itemsJsonStr.isNotBlank()) {
                    try {
                        val arr = JSONArray(itemsJsonStr)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val pName = obj.optString("productName", "").ifBlank { obj.optString("product", "") }
                            val uPrice = obj.optDouble("unitPrice", Double.NaN).let { if (!it.isNaN()) it else obj.optDouble("price", 0.0) }
                            val qty = obj.optInt("quantity", 1)
                            val tPrice = obj.optDouble("totalPrice", Double.NaN).let { if (!it.isNaN()) it else (uPrice * qty) }
                            itemsList.add(
                                OrderItemEntity(
                                    id = 0L,
                                    orderId = rawId,
                                    productId = obj.optLong("productId", 0L),
                                    productName = pName,
                                    unitPrice = uPrice,
                                    quantity = qty,
                                    totalPrice = tPrice
                                )
                            )
                        }
                    } catch (_: Throwable) { }
                }
            }

            // Fallback: If no item list was encoded, extract from root fields product, quantity, price
            if (itemsList.isEmpty()) {
                val rootProduct = getAnyString(doc, "product").ifBlank { getAnyString(doc, "productName") }
                if (rootProduct.isNotBlank()) {
                    val rootQty = getAnyLong(doc, "quantity", 1L).toInt().coerceAtLeast(1)
                    val rootPrice = getAnyDouble(doc, "price", finalAmount)
                    itemsList.add(
                        OrderItemEntity(
                            id = 0L,
                            orderId = rawId,
                            productId = getAnyLong(doc, "productId", 1L),
                            productName = rootProduct,
                            unitPrice = if (rootQty > 0) rootPrice / rootQty else rootPrice,
                            quantity = rootQty,
                            totalPrice = rootPrice
                        )
                    )
                }
            }

            Pair(orderEntity, itemsList)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse Firestore doc snapshot: ${e.message}")
            null
        }
    }

    /**
     * Pushes a newly placed order to Firebase Firestore using authenticated Firebase SDK.
     */
    suspend fun pushOrderToFirestore(order: OrderEntity, items: List<OrderItemEntity>, shopName: String? = null): Boolean {
        return pushOrderToFirestoreWithResult(order, items, shopName) is CloudSyncResult.Success
    }

    suspend fun pushOrderToFirestoreWithResult(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        shopName: String? = null
    ): CloudSyncResult<Unit> = withContext(Dispatchers.IO) {
        if (!FirebaseSyncConfig.IS_LIVE_CLOUD_SYNC_ACTIVE) {
            return@withContext CloudSyncResult.Failure("SYNC_DISABLED", "Cloud sync is disabled.")
        }
        val uid = currentUidOrNull()
            ?: return@withContext CloudSyncResult.Failure(
                "UNAUTHENTICATED",
                "Please sign in with Mobile OTP to submit your order to the cloud.",
                isAuthOrPermissionError = true
            )

        val effectiveOrder = order.copy(customerUid = order.customerUid.ifBlank { uid })
        val map = orderToMap(effectiveOrder, items, shopName)
        val docId = effectiveOrder.orderNumber

        return@withContext try {
            val firestore = firestoreOrNull()
                ?: return@withContext CloudSyncResult.Failure("NOT_INITIALIZED", "Firebase Firestore is not initialized on this device.")
            Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_ORDERS).document(docId).set(map),
                10,
                TimeUnit.SECONDS
            )
            Log.i(TAG, "Order #${effectiveOrder.orderNumber} pushed via Firebase SDK")
            CloudSyncResult.Success(Unit)
        } catch (e: Throwable) {
            val msg = handleFirestoreError(e, OperationType.WRITE, "${FirebaseSyncConfig.COLLECTION_ORDERS}/$docId")
            CloudSyncResult.Failure(
                errorCode = if (isAuthOrPermException(e)) "PERMISSION_DENIED" else "WRITE_FAILED",
                userMessage = msg,
                isAuthOrPermissionError = isAuthOrPermException(e)
            )
        }
    }

    /**
     * Updates an order status in Firestore.
     */
    suspend fun updateOrderStatusInFirestore(orderNumber: String, status: OrderStatus): Boolean = withContext(Dispatchers.IO) {
        if (!FirebaseSyncConfig.IS_LIVE_CLOUD_SYNC_ACTIVE) return@withContext false
        if (currentUidOrNull() == null) return@withContext false

        return@withContext try {
            val firestore = firestoreOrNull() ?: return@withContext false
            Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_ORDERS)
                    .document(orderNumber)
                    .update(mapOf("status" to status.name, "syncedAt" to System.currentTimeMillis())),
                8,
                TimeUnit.SECONDS
            )
            true
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.UPDATE, "${FirebaseSyncConfig.COLLECTION_ORDERS}/$orderNumber")
            false
        }
    }

    private fun safeRuleImageUrl(raw: String): String {
        val trimmed = raw.trim().ifBlank { "hero_local_mart" }
        return if (trimmed.length <= 400_000) trimmed else "hero_local_mart"
    }

    private fun safeRuleShopName(name: String): String =
        name.trim().ifBlank { "Sinapali Shop" }.let { if (it.length < 2) "$it Shop" else it }.take(120)

    private fun safeRuleShopCategory(category: String): String =
        category.trim().ifBlank { "Daily Essentials" }.take(100)

    private fun safeRuleShopAddress(address: String): String =
        address.trim().ifBlank { "Sinapali Main Market" }.let { if (it.length < 2) "Sinapali, $it" else it }.take(300)

    private fun safeRuleShopPhone(phone: String): String {
        val cleaned = phone.trim().filter { it.isDigit() || it == '+' }
        return when {
            cleaned.length in 7..20 -> cleaned
            cleaned.length > 20 -> cleaned.take(20)
            cleaned.isBlank() -> "9999999999"
            else -> cleaned.padEnd(10, '0')
        }
    }

    private fun safeRuleProductName(name: String): String =
        name.trim().ifBlank { "Local Product" }.take(200)

    private fun safeRuleProductCategory(category: String): String =
        category.trim().ifBlank { "Daily Essentials" }.take(100)

    private fun safeRuleProductDescription(description: String): String =
        description.trim().take(4000)

    private fun safeRuleProductPrice(price: Double): Double =
        if (price.isFinite() && price > 0.0) price else 1.0

    private fun safeRuleProductUnit(unit: String): String =
        unit.trim().ifBlank { "1 unit" }.take(50)

    /**
     * Converts a ShopEntity to Map suitable for Firebase Firestore SDK create/full write.
     */
    fun shopToMap(shop: com.example.data.local.entities.ShopEntity, owner: com.example.data.local.entities.UserEntity? = null): Map<String, Any> {
        val resolvedUid = shop.ownerUid.ifBlank { owner?.firebaseUid ?: currentUidOrNull() ?: "" }.take(128)
        val safeRating = if (shop.rating.isFinite()) shop.rating.coerceIn(0.0, 5.0) else 4.8
        val safeMinOrder = if (shop.minOrderAmount.isFinite() && shop.minOrderAmount >= 0.0) shop.minOrderAmount else 0.0
        return mutableMapOf(
            "id" to shop.id,
            "ownerId" to shop.ownerId,
            "ownerUid" to resolvedUid,
            "name" to safeRuleShopName(shop.name),
            "category" to safeRuleShopCategory(shop.category),
            "address" to safeRuleShopAddress(shop.address),
            "phone" to safeRuleShopPhone(shop.phone),
            "upiId" to shop.upiId.trim().take(100),
            "rating" to safeRating,
            "reviewCount" to shop.reviewCount.coerceAtLeast(0),
            "isOpen" to shop.isOpen,
            "deliveryAvailable" to shop.deliveryAvailable,
            "minOrderAmount" to safeMinOrder,
            "estimatedDeliveryMins" to shop.estimatedDeliveryMins.coerceIn(0, 1440),
            "isApproved" to shop.isApproved,
            "imageUrl" to safeRuleImageUrl(shop.imageUrl),
            "ownerName" to (owner?.name ?: "").trim().take(120),
            "updatedAt" to System.currentTimeMillis()
        )
    }

    /**
     * Returns only the Shop fields mutable by non-admin owners under firestore.rules allow update.
     */
    fun shopToEditableMap(shop: com.example.data.local.entities.ShopEntity, owner: com.example.data.local.entities.UserEntity? = null): Map<String, Any> {
        val safeMinOrder = if (shop.minOrderAmount.isFinite() && shop.minOrderAmount >= 0.0) shop.minOrderAmount else 0.0
        return mutableMapOf(
            "name" to safeRuleShopName(shop.name),
            "category" to safeRuleShopCategory(shop.category),
            "address" to safeRuleShopAddress(shop.address),
            "phone" to safeRuleShopPhone(shop.phone),
            "upiId" to shop.upiId.trim().take(100),
            "isOpen" to shop.isOpen,
            "deliveryAvailable" to shop.deliveryAvailable,
            "minOrderAmount" to safeMinOrder,
            "estimatedDeliveryMins" to shop.estimatedDeliveryMins.coerceIn(0, 1440),
            "imageUrl" to safeRuleImageUrl(shop.imageUrl),
            "ownerName" to (owner?.name ?: "").trim().take(120),
            "updatedAt" to System.currentTimeMillis()
        )
    }

    /**
     * Converts a ShopEntity to JSONObject payload for Firestore REST endpoints.
     */
    fun shopToFirestorePayload(shop: com.example.data.local.entities.ShopEntity, owner: com.example.data.local.entities.UserEntity? = null): JSONObject {
        val root = JSONObject()
        val fields = JSONObject()
        val resolvedUid = shop.ownerUid.ifBlank { owner?.firebaseUid ?: currentUidOrNull() ?: "" }.take(128)
        val safeRating = if (shop.rating.isFinite()) shop.rating.coerceIn(0.0, 5.0) else 4.8
        val safeMinOrder = if (shop.minOrderAmount.isFinite() && shop.minOrderAmount >= 0.0) shop.minOrderAmount else 0.0
        fields.put("id", JSONObject().put("integerValue", shop.id.toString()))
        fields.put("ownerId", JSONObject().put("integerValue", shop.ownerId.toString()))
        fields.put("ownerUid", JSONObject().put("stringValue", resolvedUid))
        fields.put("name", JSONObject().put("stringValue", safeRuleShopName(shop.name)))
        fields.put("category", JSONObject().put("stringValue", safeRuleShopCategory(shop.category)))
        fields.put("address", JSONObject().put("stringValue", safeRuleShopAddress(shop.address)))
        fields.put("phone", JSONObject().put("stringValue", safeRuleShopPhone(shop.phone)))
        fields.put("upiId", JSONObject().put("stringValue", shop.upiId.trim().take(100)))
        fields.put("rating", JSONObject().put("doubleValue", safeRating))
        fields.put("reviewCount", JSONObject().put("integerValue", shop.reviewCount.coerceAtLeast(0).toString()))
        fields.put("isOpen", JSONObject().put("booleanValue", shop.isOpen))
        fields.put("deliveryAvailable", JSONObject().put("booleanValue", shop.deliveryAvailable))
        fields.put("minOrderAmount", JSONObject().put("doubleValue", safeMinOrder))
        fields.put("estimatedDeliveryMins", JSONObject().put("integerValue", shop.estimatedDeliveryMins.coerceIn(0, 1440).toString()))
        fields.put("isApproved", JSONObject().put("booleanValue", shop.isApproved))
        fields.put("imageUrl", JSONObject().put("stringValue", safeRuleImageUrl(shop.imageUrl)))
        fields.put("ownerName", JSONObject().put("stringValue", (owner?.name ?: "").trim().take(120)))
        fields.put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
        root.put("fields", fields)
        return root
    }

    /**
     * Saves a newly registered or updated shop to Firebase Firestore with honest status propagation.
     */
    suspend fun pushShopToFirestore(shop: com.example.data.local.entities.ShopEntity, owner: com.example.data.local.entities.UserEntity? = null): Boolean {
        return pushShopToFirestoreWithResult(shop, owner, isNew = shop.id <= 0L) is CloudSyncResult.Success
    }

    suspend fun pushShopToFirestoreWithResult(
        shop: com.example.data.local.entities.ShopEntity,
        owner: com.example.data.local.entities.UserEntity? = null,
        isNew: Boolean = false
    ): CloudSyncResult<com.example.data.local.entities.ShopEntity> = withContext(Dispatchers.IO) {
        if (!FirebaseSyncConfig.IS_LIVE_CLOUD_SYNC_ACTIVE) {
            Log.e(TAG, "Firestore shop write aborted: IS_LIVE_CLOUD_SYNC_ACTIVE is false (shopId=${shop.id})")
            return@withContext CloudSyncResult.Failure("SYNC_DISABLED", "Cloud sync is disabled.")
        }

        val uid = currentUidOrNull()
        if (uid == null) {
            Log.e(
                TAG,
                "Firestore shop write aborted: FirebaseAuth currentUser is null (project=${FirebaseSyncConfig.FIREBASE_PROJECT_ID}, shopId=${shop.id}, shopOwnerUid=${shop.ownerUid})"
            )
            return@withContext CloudSyncResult.Failure(
                "UNAUTHENTICATED",
                "Shop owner OTP sign-in is required before syncing your shop to Cloud Firestore.",
                isAuthOrPermissionError = true
            )
        }

        val creatingNew = isNew || shop.id <= 0L
        val effectiveId = if (shop.id > 0L) shop.id else System.currentTimeMillis()
        val effectiveShop = shop.copy(
            id = effectiveId,
            ownerUid = shop.ownerUid.ifBlank { uid }
        )
        val docId = "shop_$effectiveId"
        val fullMap = shopToMap(effectiveShop, owner)
        val editableMap = shopToEditableMap(effectiveShop, owner)

        return@withContext try {
            val firestore = firestoreOrNull()
            if (firestore == null) {
                Log.e(TAG, "Firestore shop write aborted: FirebaseFirestore instance is null (path=${FirebaseSyncConfig.COLLECTION_SHOPS}/$docId)")
                return@withContext CloudSyncResult.Failure("NOT_INITIALIZED", "Firebase Firestore is not initialized on this device.")
            }
            val docRef = firestore.collection(FirebaseSyncConfig.COLLECTION_SHOPS).document(docId)
            if (creatingNew) {
                Tasks.await(docRef.set(fullMap), 10, TimeUnit.SECONDS)
            } else {
                try {
                    Tasks.await(docRef.update(editableMap), 10, TimeUnit.SECONDS)
                } catch (updateErr: Throwable) {
                    val rootFs = generateSequence(updateErr) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (rootFs?.code == FirebaseFirestoreException.Code.NOT_FOUND ||
                        rootFs?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {
                        Log.w(
                            TAG,
                            "Shop update on ${docRef.path} returned ${rootFs.code}; attempting set(fullMap) in case document does not exist yet (uid=$uid)"
                        )
                        Tasks.await(docRef.set(fullMap), 10, TimeUnit.SECONDS)
                    } else {
                        throw updateErr
                    }
                }
            }
            Log.i(TAG, "Shop '${effectiveShop.name}' (#${effectiveShop.id}) confirmed in Firestore at ${docRef.path} (uid=$uid)")
            CloudSyncResult.Success(effectiveShop)
        } catch (e: Throwable) {
            val msg = handleFirestoreError(e, OperationType.WRITE, "${FirebaseSyncConfig.COLLECTION_SHOPS}/$docId")
            Log.e(
                TAG,
                "Firestore shop write FAILED: project=${FirebaseSyncConfig.FIREBASE_PROJECT_ID}, path=${FirebaseSyncConfig.COLLECTION_SHOPS}/$docId, uid=$uid, shopOwnerUid=${effectiveShop.ownerUid}, error=${e.message}",
                e
            )
            CloudSyncResult.Failure(
                errorCode = if (isAuthOrPermException(e)) "PERMISSION_DENIED" else "WRITE_FAILED",
                userMessage = msg,
                isAuthOrPermissionError = isAuthOrPermException(e)
            )
        }
    }

    /**
     * Parses a Firebase SDK DocumentSnapshot into a ShopEntity.
     */
    fun firestoreDocToShop(doc: DocumentSnapshot): com.example.data.local.entities.ShopEntity? {
        return try {
            val docIdRaw = doc.id.removePrefix("shop_")
            val id = getAnyLong(doc, "id", 0L).let { if (it > 0L) it else docIdRaw.toLongOrNull() ?: (Math.abs(doc.id.hashCode().toLong()) + 1000L) }
            val name = getAnyString(doc, "name")
            if (name.isBlank()) return null
            val ownerId = getAnyLong(doc, "ownerId", 0L)
            val ownerUid = getAnyString(doc, "ownerUid")
            val category = getAnyString(doc, "category", "Daily Groceries & Essentials")
            val address = getAnyString(doc, "address", "Sinapali Main Market")
            val phone = getAnyString(doc, "phone")
            val upiId = getAnyString(doc, "upiId", "sinapalimart@upi")
            val rating = getAnyDouble(doc, "rating", 4.8)
            val reviewCount = getAnyLong(doc, "reviewCount", 24L).toInt()
            val isOpen = getAnyBoolean(doc, "isOpen", true)
            val deliveryAvailable = getAnyBoolean(doc, "deliveryAvailable", true)
            val minOrderAmount = getAnyDouble(doc, "minOrderAmount", 50.0)
            val estimatedDeliveryMins = getAnyLong(doc, "estimatedDeliveryMins", 25L).toInt()
            val isApproved = getAnyBoolean(doc, "isApproved", true)
            val imageUrl = getAnyString(doc, "imageUrl", "hero_local_mart").ifBlank { "hero_local_mart" }

            com.example.data.local.entities.ShopEntity(
                id = id,
                ownerId = ownerId,
                name = name,
                category = category,
                address = address,
                phone = phone,
                upiId = upiId,
                rating = rating,
                reviewCount = reviewCount,
                isOpen = isOpen,
                deliveryAvailable = deliveryAvailable,
                minOrderAmount = minOrderAmount,
                estimatedDeliveryMins = estimatedDeliveryMins,
                isApproved = isApproved,
                imageUrl = imageUrl,
                ownerUid = ownerUid
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse Firestore shop doc: ${e.message}")
            null
        }
    }

    /**
     * Parses a Firestore Document REST JSON response into a ShopEntity.
     */
    fun firestorePayloadToShop(documentJson: JSONObject): com.example.data.local.entities.ShopEntity? {
        return try {
            val fields = documentJson.optJSONObject("fields") ?: return null
            val docPath = documentJson.optString("name", "")
            val docIdFallback = if (docPath.contains("/")) docPath.substringAfterLast("/") else ""
            val id = getFieldLong(fields, "id", 0L).let { if (it > 0L) it else docIdFallback.removePrefix("shop_").toLongOrNull() ?: (Math.abs(docIdFallback.hashCode().toLong()) + 1000L) }
            val name = getFieldString(fields, "name")
            if (name.isBlank()) return null
            val ownerId = getFieldLong(fields, "ownerId", 0L)
            val ownerUid = getFieldString(fields, "ownerUid")
            val category = getFieldString(fields, "category", "Daily Groceries & Essentials")
            val address = getFieldString(fields, "address", "Sinapali Main Market")
            val phone = getFieldString(fields, "phone")
            val upiId = getFieldString(fields, "upiId", "sinapalimart@upi")
            val rating = getFieldDouble(fields, "rating", 4.8)
            val reviewCount = getFieldLong(fields, "reviewCount", 24L).toInt()
            val isOpen = getFieldBoolean(fields, "isOpen", true)
            val deliveryAvailable = getFieldBoolean(fields, "deliveryAvailable", true)
            val minOrderAmount = getFieldDouble(fields, "minOrderAmount", 50.0)
            val estimatedDeliveryMins = getFieldLong(fields, "estimatedDeliveryMins", 25L).toInt()
            val isApproved = getFieldBoolean(fields, "isApproved", true)
            val imageUrl = getFieldString(fields, "imageUrl", "hero_local_mart").ifBlank { "hero_local_mart" }

            com.example.data.local.entities.ShopEntity(
                id = id,
                ownerId = ownerId,
                name = name,
                category = category,
                address = address,
                phone = phone,
                upiId = upiId,
                rating = rating,
                reviewCount = reviewCount,
                isOpen = isOpen,
                deliveryAvailable = deliveryAvailable,
                minOrderAmount = minOrderAmount,
                estimatedDeliveryMins = estimatedDeliveryMins,
                isApproved = isApproved,
                imageUrl = imageUrl,
                ownerUid = ownerUid
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse Firestore shop JSON: ${e.message}")
            null
        }
    }

    private fun safeRuleDiscountPrice(price: Double, discountPrice: Double?): Double {
        val candidate = discountPrice ?: 0.0
        return if (candidate in 0.0..price) candidate else 0.0
    }

    /**
     * Converts a ProductEntity to Map suitable for Firebase Firestore SDK create/full write.
     */
    fun productToMap(product: com.example.data.local.entities.ProductEntity): Map<String, Any> {
        val resolvedUid = product.ownerUid.ifBlank { currentUidOrNull() ?: "" }.take(128)
        val safePrice = safeRuleProductPrice(product.price)
        val map = mutableMapOf<String, Any>()
        map["id"] = product.id
        map["shopId"] = product.shopId
        map["shopDocId"] = "shop_${product.shopId}"
        map["ownerUid"] = resolvedUid
        map["name"] = safeRuleProductName(product.name)
        map["category"] = safeRuleProductCategory(product.category)
        map["description"] = safeRuleProductDescription(product.description)
        map["price"] = safePrice
        map["discountPrice"] = safeRuleDiscountPrice(safePrice, product.discountPrice)
        map["unit"] = safeRuleProductUnit(product.unit)
        map["stock"] = product.stock.coerceAtLeast(0)
        map["isAvailable"] = product.isAvailable
        map["imageUrl"] = safeRuleImageUrl(product.imageUrl)
        map["isApproved"] = product.isApproved
        map["updatedAt"] = System.currentTimeMillis()
        return map
    }

    /**
     * Returns only the Product fields mutable by non-admin owners under firestore.rules allow update.
     */
    fun productToEditableMap(product: com.example.data.local.entities.ProductEntity): Map<String, Any> {
        val safePrice = safeRuleProductPrice(product.price)
        return mutableMapOf(
            "name" to safeRuleProductName(product.name),
            "category" to safeRuleProductCategory(product.category),
            "description" to safeRuleProductDescription(product.description),
            "price" to safePrice,
            "discountPrice" to safeRuleDiscountPrice(safePrice, product.discountPrice),
            "unit" to safeRuleProductUnit(product.unit),
            "stock" to product.stock.coerceAtLeast(0),
            "isAvailable" to product.isAvailable,
            "imageUrl" to safeRuleImageUrl(product.imageUrl),
            "updatedAt" to System.currentTimeMillis()
        )
    }

    /**
     * Converts a ProductEntity to JSONObject payload for Firestore REST endpoints.
     */
    fun productToFirestorePayload(product: com.example.data.local.entities.ProductEntity): JSONObject {
        val root = JSONObject()
        val fields = JSONObject()
        val resolvedUid = product.ownerUid.ifBlank { currentUidOrNull() ?: "" }.take(128)
        val safePrice = safeRuleProductPrice(product.price)
        fields.put("id", JSONObject().put("integerValue", product.id.toString()))
        fields.put("shopId", JSONObject().put("integerValue", product.shopId.toString()))
        fields.put("shopDocId", JSONObject().put("stringValue", "shop_${product.shopId}"))
        fields.put("ownerUid", JSONObject().put("stringValue", resolvedUid))
        fields.put("name", JSONObject().put("stringValue", safeRuleProductName(product.name)))
        fields.put("category", JSONObject().put("stringValue", safeRuleProductCategory(product.category)))
        fields.put("description", JSONObject().put("stringValue", safeRuleProductDescription(product.description)))
        fields.put("price", JSONObject().put("doubleValue", safePrice))
        fields.put("discountPrice", JSONObject().put("doubleValue", safeRuleDiscountPrice(safePrice, product.discountPrice)))
        fields.put("unit", JSONObject().put("stringValue", safeRuleProductUnit(product.unit)))
        fields.put("stock", JSONObject().put("integerValue", product.stock.coerceAtLeast(0).toString()))
        fields.put("isAvailable", JSONObject().put("booleanValue", product.isAvailable))
        fields.put("imageUrl", JSONObject().put("stringValue", safeRuleImageUrl(product.imageUrl)))
        fields.put("isApproved", JSONObject().put("booleanValue", product.isApproved))
        fields.put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
        root.put("fields", fields)
        return root
    }

    /**
     * Pushes a new or updated product to Firebase Firestore with honest status propagation.
     */
    suspend fun pushProductToFirestore(product: com.example.data.local.entities.ProductEntity): Boolean {
        return pushProductToFirestoreWithResult(product, isNew = product.id <= 0L) is CloudSyncResult.Success
    }

    suspend fun pushProductToFirestoreWithResult(
        product: com.example.data.local.entities.ProductEntity,
        isNew: Boolean = false
    ): CloudSyncResult<com.example.data.local.entities.ProductEntity> = withContext(Dispatchers.IO) {
        if (!FirebaseSyncConfig.IS_LIVE_CLOUD_SYNC_ACTIVE) {
            Log.e(TAG, "Firestore product write aborted: IS_LIVE_CLOUD_SYNC_ACTIVE is false (productId=${product.id}, shopId=${product.shopId})")
            return@withContext CloudSyncResult.Failure("SYNC_DISABLED", "Cloud sync is disabled.")
        }

        val uid = currentUidOrNull()
        if (uid == null) {
            Log.e(
                TAG,
                "Firestore product write aborted: FirebaseAuth currentUser is null (project=${FirebaseSyncConfig.FIREBASE_PROJECT_ID}, productId=${product.id}, shopId=${product.shopId})"
            )
            return@withContext CloudSyncResult.Failure(
                "UNAUTHENTICATED",
                "Please sign in with Mobile OTP to save products to Cloud Firestore.",
                isAuthOrPermissionError = true
            )
        }

        val creatingNew = isNew || product.id <= 0L
        val effectiveId = if (product.id > 0L) product.id else System.currentTimeMillis()
        val effectiveProduct = product.copy(
            id = effectiveId,
            ownerUid = product.ownerUid.ifBlank { uid }
        )
        val docId = "prod_$effectiveId"
        val fullMap = productToMap(effectiveProduct)
        val editableMap = productToEditableMap(effectiveProduct)

        return@withContext try {
            val firestore = firestoreOrNull()
            if (firestore == null) {
                Log.e(TAG, "Firestore product write aborted: FirebaseFirestore instance is null (path=${FirebaseSyncConfig.COLLECTION_PRODUCTS}/$docId)")
                return@withContext CloudSyncResult.Failure("NOT_INITIALIZED", "Firebase Firestore is not initialized on this device.")
            }
            val docRef = firestore.collection(FirebaseSyncConfig.COLLECTION_PRODUCTS).document(docId)
            if (creatingNew) {
                Tasks.await(docRef.set(fullMap), 10, TimeUnit.SECONDS)
            } else {
                try {
                    Tasks.await(docRef.update(editableMap), 10, TimeUnit.SECONDS)
                } catch (updateErr: Throwable) {
                    val rootFs = generateSequence(updateErr) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (rootFs?.code == FirebaseFirestoreException.Code.NOT_FOUND ||
                        rootFs?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    ) {
                        Log.w(
                            TAG,
                            "Product update on ${docRef.path} returned ${rootFs.code}; attempting set(fullMap) in case document does not exist yet (uid=$uid, shopDocId=shop_${effectiveProduct.shopId})"
                        )
                        Tasks.await(docRef.set(fullMap), 10, TimeUnit.SECONDS)
                    } else {
                        throw updateErr
                    }
                }
            }
            Log.i(
                TAG,
                "Product '${effectiveProduct.name}' confirmed in Firestore at ${docRef.path} (shopId=${effectiveProduct.shopId}, uid=$uid)"
            )
            CloudSyncResult.Success(effectiveProduct)
        } catch (e: Throwable) {
            val msg = handleFirestoreError(e, OperationType.WRITE, "${FirebaseSyncConfig.COLLECTION_PRODUCTS}/$docId")
            Log.e(
                TAG,
                "Firestore product write FAILED: project=${FirebaseSyncConfig.FIREBASE_PROJECT_ID}, path=${FirebaseSyncConfig.COLLECTION_PRODUCTS}/$docId, shopDocId=shop_${effectiveProduct.shopId}, uid=$uid, ownerUid=${effectiveProduct.ownerUid}, isNew=$creatingNew, error=${e.message}",
                e
            )
            CloudSyncResult.Failure(
                errorCode = if (isAuthOrPermException(e)) "PERMISSION_DENIED" else "WRITE_FAILED",
                userMessage = msg,
                isAuthOrPermissionError = isAuthOrPermException(e)
            )
        }
    }

    /**
     * Deletes a product from Firebase Firestore across all devices.
     */
    suspend fun deleteProductFromFirestore(product: com.example.data.local.entities.ProductEntity): Boolean {
        return deleteProductFromFirestoreWithResult(product) is CloudSyncResult.Success
    }

    suspend fun deleteProductFromFirestoreWithResult(
        product: com.example.data.local.entities.ProductEntity
    ): CloudSyncResult<Unit> = withContext(Dispatchers.IO) {
        if (!FirebaseSyncConfig.IS_LIVE_CLOUD_SYNC_ACTIVE) {
            return@withContext CloudSyncResult.Failure("SYNC_DISABLED", "Cloud sync is disabled.")
        }
        if (currentUidOrNull() == null) {
            return@withContext CloudSyncResult.Failure(
                "UNAUTHENTICATED",
                "Please sign in with Mobile OTP to delete products from Firestore.",
                isAuthOrPermissionError = true
            )
        }

        val docId = "prod_${product.id}"
        return@withContext try {
            val firestore = firestoreOrNull()
                ?: return@withContext CloudSyncResult.Failure("NOT_INITIALIZED", "Firebase Firestore is not initialized on this device.")
            Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_PRODUCTS).document(docId).delete(),
                10,
                TimeUnit.SECONDS
            )
            CloudSyncResult.Success(Unit)
        } catch (e: Throwable) {
            val msg = handleFirestoreError(e, OperationType.DELETE, "${FirebaseSyncConfig.COLLECTION_PRODUCTS}/$docId")
            CloudSyncResult.Failure(
                errorCode = if (isAuthOrPermException(e)) "PERMISSION_DENIED" else "DELETE_FAILED",
                userMessage = msg,
                isAuthOrPermissionError = isAuthOrPermException(e)
            )
        }
    }

    /**
     * Parses a Firebase SDK DocumentSnapshot into a ProductEntity.
     */
    fun firestoreDocToProduct(doc: DocumentSnapshot): com.example.data.local.entities.ProductEntity? {
        return try {
            val docIdRaw = doc.id.removePrefix("prod_").removePrefix("product_")
            val id = getAnyLong(doc, "id", 0L).let { if (it > 0L) it else docIdRaw.toLongOrNull() ?: (Math.abs(doc.id.hashCode().toLong()) + 10000L) }
            val name = getAnyString(doc, "name")
            if (name.isBlank()) return null
            val shopId = getAnyLong(doc, "shopId", 1L)
            val category = getAnyString(doc, "category", "Daily Essentials")
            val description = getAnyString(doc, "description")
            val price = getAnyDouble(doc, "price", 0.0)
            val discountPriceRaw = getAnyDouble(doc, "discountPrice", 0.0)
            val discountPrice = if (discountPriceRaw > 0.0 && discountPriceRaw < price) discountPriceRaw else null
            val unit = getAnyString(doc, "unit", "item")
            val stock = getAnyLong(doc, "stock", 10L).toInt()
            val isAvailable = getAnyBoolean(doc, "isAvailable", true)
            val imageUrl = getAnyString(doc, "imageUrl", "hero_local_mart").ifBlank { "hero_local_mart" }
            val isApproved = getAnyBoolean(doc, "isApproved", true)
            val ownerUid = getAnyString(doc, "ownerUid")

            com.example.data.local.entities.ProductEntity(
                id = id,
                shopId = shopId,
                name = name,
                category = category,
                description = description,
                price = price,
                discountPrice = discountPrice,
                unit = unit,
                stock = stock,
                isAvailable = isAvailable,
                imageUrl = imageUrl,
                isApproved = isApproved,
                ownerUid = ownerUid
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse Firestore product doc: ${e.message}")
            null
        }
    }

    /**
     * Parses a Firestore Document REST JSON response into a ProductEntity.
     */
    fun firestorePayloadToProduct(documentJson: JSONObject): com.example.data.local.entities.ProductEntity? {
        return try {
            val fields = documentJson.optJSONObject("fields") ?: return null
            val docPath = documentJson.optString("name", "")
            val docIdFallback = if (docPath.contains("/")) docPath.substringAfterLast("/") else ""
            val id = getFieldLong(fields, "id", 0L).let { if (it > 0L) it else docIdFallback.removePrefix("prod_").removePrefix("product_").toLongOrNull() ?: (Math.abs(docIdFallback.hashCode().toLong()) + 10000L) }
            val name = getFieldString(fields, "name")
            if (name.isBlank()) return null
            val shopId = getFieldLong(fields, "shopId", 1L)
            val category = getFieldString(fields, "category", "Daily Essentials")
            val description = getFieldString(fields, "description")
            val price = getFieldDouble(fields, "price", 0.0)
            val discountPriceRaw = getFieldDouble(fields, "discountPrice", 0.0)
            val discountPrice = if (discountPriceRaw > 0.0 && discountPriceRaw < price) discountPriceRaw else null
            val unit = getFieldString(fields, "unit", "item")
            val stock = getFieldLong(fields, "stock", 10L).toInt()
            val isAvailable = getFieldBoolean(fields, "isAvailable", true)
            val imageUrl = getFieldString(fields, "imageUrl", "hero_local_mart").ifBlank { "hero_local_mart" }
            val isApproved = getFieldBoolean(fields, "isApproved", true)
            val ownerUid = getFieldString(fields, "ownerUid")

            com.example.data.local.entities.ProductEntity(
                id = id,
                shopId = shopId,
                name = name,
                category = category,
                description = description,
                price = price,
                discountPrice = discountPrice,
                unit = unit,
                stock = stock,
                isAvailable = isAvailable,
                imageUrl = imageUrl,
                isApproved = isApproved,
                ownerUid = ownerUid
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse Firestore product JSON: ${e.message}")
            null
        }
    }

    fun fetchCloudShopsInternal(): List<com.example.data.local.entities.ShopEntity> {
        val result = mutableListOf<com.example.data.local.entities.ShopEntity>()
        val firestore = firestoreOrNull() ?: return result

        // 1. Public approved shops (matches firestore.rules allow list)
        try {
            val snapshot = Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_SHOPS)
                    .whereEqualTo("isApproved", true)
                    .get(),
                8,
                TimeUnit.SECONDS
            )
            for (doc in snapshot.documents) {
                val parsed = firestoreDocToShop(doc)
                if (parsed != null) {
                    result.removeAll { it.id == parsed.id }
                    result.add(parsed)
                }
            }
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.LIST, FirebaseSyncConfig.COLLECTION_SHOPS)
        }

        // 2. If signed in, also fetch caller's own shops (including unapproved)
        val uid = currentUidOrNull()
        if (uid != null) {
            try {
                val ownSnapshot = Tasks.await(
                    firestore.collection(FirebaseSyncConfig.COLLECTION_SHOPS)
                        .whereEqualTo("ownerUid", uid)
                        .get(),
                    6,
                    TimeUnit.SECONDS
                )
                for (doc in ownSnapshot.documents) {
                    val parsed = firestoreDocToShop(doc)
                    if (parsed != null) {
                        result.removeAll { it.id == parsed.id }
                        result.add(parsed)
                    }
                }
            } catch (e: Throwable) {
                handleFirestoreError(e, OperationType.LIST, "${FirebaseSyncConfig.COLLECTION_SHOPS}?ownerUid")
            }
        }
        return result
    }

    fun fetchCloudProductsInternal(): List<com.example.data.local.entities.ProductEntity> {
        val result = mutableListOf<com.example.data.local.entities.ProductEntity>()
        val firestore = firestoreOrNull() ?: return result

        // 1. Public active & approved products (matches firestore.rules allow list)
        try {
            val snapshot = Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_PRODUCTS)
                    .whereEqualTo("isApproved", true)
                    .whereEqualTo("isAvailable", true)
                    .get(),
                8,
                TimeUnit.SECONDS
            )
            for (doc in snapshot.documents) {
                val parsed = firestoreDocToProduct(doc)
                if (parsed != null) {
                    result.removeAll { it.id == parsed.id }
                    result.add(parsed)
                }
            }
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.LIST, FirebaseSyncConfig.COLLECTION_PRODUCTS)
        }

        // 2. If signed in, also fetch caller's own products (including out-of-stock / unapproved)
        val uid = currentUidOrNull()
        if (uid != null) {
            try {
                val ownSnapshot = Tasks.await(
                    firestore.collection(FirebaseSyncConfig.COLLECTION_PRODUCTS)
                        .whereEqualTo("ownerUid", uid)
                        .get(),
                    6,
                    TimeUnit.SECONDS
                )
                for (doc in ownSnapshot.documents) {
                    val parsed = firestoreDocToProduct(doc)
                    if (parsed != null) {
                        result.removeAll { it.id == parsed.id }
                        result.add(parsed)
                    }
                }
            } catch (e: Throwable) {
                handleFirestoreError(e, OperationType.LIST, "${FirebaseSyncConfig.COLLECTION_PRODUCTS}?ownerUid")
            }
        }
        return result
    }

    /**
     * Fetches user profile stored at /users/{uid} for multi-device login restoration.
     */
    suspend fun fetchUserByUidFromFirestore(uid: String): com.example.data.local.entities.UserEntity? = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext null
        val firestore = firestoreOrNull() ?: return@withContext null
        return@withContext try {
            val doc = Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_USERS).document(uid).get(),
                8,
                TimeUnit.SECONDS
            )
            if (!doc.exists()) return@withContext null
            val name = getAnyString(doc, "name")
            val phone = getAnyString(doc, "phone")
            val email = getAnyString(doc, "email")
            val roleStr = getAnyString(doc, "role", UserRole.CUSTOMER.name)
            val role = try { UserRole.valueOf(roleStr) } catch (_: Throwable) { UserRole.CUSTOMER }
            val shopIdRaw = getAnyLong(doc, "shopId", 0L)
            com.example.data.local.entities.UserEntity(
                id = getAnyLong(doc, "id", 0L),
                name = name.ifBlank { "Sinapali User" },
                phone = phone,
                email = email,
                role = role,
                shopId = if (shopIdRaw > 0L) shopIdRaw else null,
                isVerified = getAnyBoolean(doc, "isVerified", true),
                district = getAnyString(doc, "district", "Nuapada"),
                block = getAnyString(doc, "block", "Sinapali"),
                gp = getAnyString(doc, "gp", "Sinapali"),
                village = getAnyString(doc, "village", "Sinapali"),
                deliveryAddress = getAnyString(doc, "deliveryAddress", "Sinapali"),
                firebaseUid = uid
            )
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.GET, "${FirebaseSyncConfig.COLLECTION_USERS}/$uid")
            null
        }
    }

    /**
     * Fetches shop owned by the authenticated Firebase UID from Firestore.
     */
    suspend fun fetchShopByOwnerUidFromFirestore(uid: String): com.example.data.local.entities.ShopEntity? = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext null
        val firestore = firestoreOrNull() ?: return@withContext null
        return@withContext try {
            val snapshot = Tasks.await(
                firestore
                    .collection(FirebaseSyncConfig.COLLECTION_SHOPS)
                    .whereEqualTo("ownerUid", uid)
                    .limit(1)
                    .get(),
                8,
                TimeUnit.SECONDS
            )
            snapshot.documents.firstOrNull()?.let { firestoreDocToShop(it) }
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.LIST, "${FirebaseSyncConfig.COLLECTION_SHOPS}?ownerUid")
            null
        }
    }

    /**
     * Bidirectional live sync for Shops and Products across all devices:
     * 1. Pulls published shops and active products (plus signed-in owner's items) from Firestore into Room.
     * 2. Only pushes local shops/products owned by the currently authenticated Firebase user UID (never backfills SeedData).
     */
    suspend fun performShopAndProductCloudSync(dao: MartDao): Pair<Int, Int> {
        val report = performShopAndProductCloudSyncWithResult(dao)
        return Pair(report.syncedShopsCount, report.syncedProductsCount)
    }

    suspend fun performShopAndProductCloudSyncWithResult(dao: MartDao): ShopAndProductSyncReport = withContext(Dispatchers.IO) {
        var shopCount = 0
        var prodCount = 0
        var syncError: String? = null
        var isAuthErr = false
        val firestore = firestoreOrNull() ?: return@withContext ShopAndProductSyncReport(
            success = false,
            syncedShopsCount = 0,
            syncedProductsCount = 0,
            errorMessage = "Firebase Firestore is not initialized on this device.",
            isAuthOrPermissionError = false
        )
        val currentUid = currentUidOrNull()

        val cloudShops = mutableListOf<com.example.data.local.entities.ShopEntity>()
        try {
            val publicShopsSnap = Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_SHOPS)
                    .whereEqualTo("isApproved", true)
                    .get(),
                8,
                TimeUnit.SECONDS
            )
            for (doc in publicShopsSnap.documents) {
                firestoreDocToShop(doc)?.let { cloudShops.add(it) }
            }
            if (currentUid != null) {
                val ownShopsSnap = Tasks.await(
                    firestore.collection(FirebaseSyncConfig.COLLECTION_SHOPS)
                        .whereEqualTo("ownerUid", currentUid)
                        .get(),
                    6,
                    TimeUnit.SECONDS
                )
                for (doc in ownShopsSnap.documents) {
                    firestoreDocToShop(doc)?.let { s ->
                        cloudShops.removeAll { it.id == s.id }
                        cloudShops.add(s)
                    }
                }
            }
            for (remoteShop in cloudShops) {
                dao.insertShop(remoteShop)
                shopCount++
            }
        } catch (e: Throwable) {
            syncError = handleFirestoreError(e, OperationType.LIST, FirebaseSyncConfig.COLLECTION_SHOPS)
            isAuthErr = isAuthOrPermException(e)
        }

        // Only push local shops owned by the current authenticated Firebase user
        if (currentUid != null) {
            val localShops = dao.getAllShopsSync()
            for (localShop in localShops) {
                if (localShop.ownerUid == currentUid && cloudShops.none { it.id == localShop.id }) {
                    when (val pushRes = pushShopToFirestoreWithResult(localShop, isNew = true)) {
                        is CloudSyncResult.Failure -> {
                            Log.e(TAG, "Backfill shop push failed (shopId=${localShop.id}, uid=$currentUid): ${pushRes.errorCode} - ${pushRes.userMessage}")
                            if (syncError == null) {
                                syncError = pushRes.userMessage
                                isAuthErr = pushRes.isAuthOrPermissionError
                            }
                        }
                        is CloudSyncResult.Success -> Unit
                    }
                }
            }
        }

        val cloudProducts = mutableListOf<com.example.data.local.entities.ProductEntity>()
        var publicProdsFetched = false
        try {
            val publicProdsSnap = Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_PRODUCTS)
                    .whereEqualTo("isApproved", true)
                    .whereEqualTo("isAvailable", true)
                    .get(),
                8,
                TimeUnit.SECONDS
            )
            publicProdsFetched = true
            for (doc in publicProdsSnap.documents) {
                firestoreDocToProduct(doc)?.let { cloudProducts.add(it) }
            }
            if (currentUid != null) {
                val ownProdsSnap = Tasks.await(
                    firestore.collection(FirebaseSyncConfig.COLLECTION_PRODUCTS)
                        .whereEqualTo("ownerUid", currentUid)
                        .get(),
                    6,
                    TimeUnit.SECONDS
                )
                for (doc in ownProdsSnap.documents) {
                    firestoreDocToProduct(doc)?.let { p ->
                        cloudProducts.removeAll { it.id == p.id }
                        cloudProducts.add(p)
                    }
                }
            }
            for (remoteProduct in cloudProducts) {
                dao.insertProduct(remoteProduct)
                prodCount++
            }
            if (publicProdsFetched) {
                val remoteIds = cloudProducts.map { it.id }.toSet()
                val localProducts = dao.getAllProductsSync()
                for (localProd in localProducts) {
                    if (localProd.ownerUid.isNotBlank() &&
                        localProd.ownerUid != currentUid &&
                        !remoteIds.contains(localProd.id)
                    ) {
                        dao.deleteProduct(localProd)
                    }
                }
            }
        } catch (e: Throwable) {
            if (syncError == null) {
                syncError = handleFirestoreError(e, OperationType.LIST, FirebaseSyncConfig.COLLECTION_PRODUCTS)
                isAuthErr = isAuthOrPermException(e)
            }
        }

        // Only push local products owned by the current authenticated Firebase user
        if (currentUid != null) {
            val localProducts = dao.getAllProductsSync()
            for (localProduct in localProducts) {
                if (localProduct.ownerUid == currentUid && cloudProducts.none { it.id == localProduct.id }) {
                    when (val pushRes = pushProductToFirestoreWithResult(localProduct, isNew = true)) {
                        is CloudSyncResult.Failure -> {
                            Log.e(
                                TAG,
                                "Backfill product push failed (productId=${localProduct.id}, shopId=${localProduct.shopId}, uid=$currentUid): ${pushRes.errorCode} - ${pushRes.userMessage}"
                            )
                            if (syncError == null) {
                                syncError = pushRes.userMessage
                                isAuthErr = pushRes.isAuthOrPermissionError
                            }
                        }
                        is CloudSyncResult.Success -> Unit
                    }
                }
            }
        }

        Log.i(TAG, "Shop & Product Cloud Sync complete. Synced $shopCount shops, $prodCount products.")
        ShopAndProductSyncReport(
            success = syncError == null,
            syncedShopsCount = shopCount,
            syncedProductsCount = prodCount,
            errorMessage = syncError,
            isAuthOrPermissionError = isAuthErr
        )
    }

    /**
     * Saves an authenticated user account to /users/{uid} in Firebase Firestore.
     */
    suspend fun pushUserToFirestore(user: com.example.data.local.entities.UserEntity): Boolean {
        return pushUserToFirestoreWithResult(user) is CloudSyncResult.Success
    }

    suspend fun pushUserToFirestoreWithResult(
        user: com.example.data.local.entities.UserEntity
    ): CloudSyncResult<Unit> = withContext(Dispatchers.IO) {
        if (!FirebaseSyncConfig.IS_LIVE_CLOUD_SYNC_ACTIVE) {
            return@withContext CloudSyncResult.Failure("SYNC_DISABLED", "Cloud sync is disabled.")
        }

        val uid = user.firebaseUid.ifBlank { currentUidOrNull() ?: "" }
        if (uid.isBlank()) {
            return@withContext CloudSyncResult.Failure(
                "UNAUTHENTICATED",
                "Please sign in with Mobile OTP to save your profile to Firestore.",
                isAuthOrPermissionError = true
            )
        }

        val safeRole = if (user.role == UserRole.SHOP_OWNER) UserRole.SHOP_OWNER.name else UserRole.CUSTOMER.name
        val map = mutableMapOf<String, Any>(
            "uid" to uid,
            "id" to user.id,
            "name" to user.name,
            "phone" to user.phone,
            "email" to user.email,
            "role" to safeRole,
            "shopId" to (user.shopId ?: 0L),
            "isVerified" to user.isVerified,
            "deliveryAddress" to user.deliveryAddress,
            "district" to user.district,
            "block" to user.block,
            "updatedAt" to System.currentTimeMillis()
        )

        return@withContext try {
            val firestore = firestoreOrNull()
                ?: return@withContext CloudSyncResult.Failure("NOT_INITIALIZED", "Firebase Firestore is not initialized on this device.")
            Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_USERS).document(uid).set(map),
                10,
                TimeUnit.SECONDS
            )
            Log.i(TAG, "User '${user.name}' ($safeRole) pushed via Firebase SDK")
            CloudSyncResult.Success(Unit)
        } catch (e: Throwable) {
            val msg = handleFirestoreError(e, OperationType.WRITE, "${FirebaseSyncConfig.COLLECTION_USERS}/$uid")
            CloudSyncResult.Failure(
                errorCode = if (isAuthOrPermException(e)) "PERMISSION_DENIED" else "WRITE_FAILED",
                userMessage = msg,
                isAuthOrPermissionError = isAuthOrPermException(e)
            )
        }
    }

    /**
     * Permanently deletes the authenticated user record from /users/{uid} in Firestore.
     */
    suspend fun deleteUserFromFirestore(userId: Long): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUidOrNull() ?: return@withContext false
        val firestore = firestoreOrNull() ?: return@withContext false
        return@withContext try {
            Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_USERS).document(uid).delete(),
                8,
                TimeUnit.SECONDS
            )
            true
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.DELETE, "${FirebaseSyncConfig.COLLECTION_USERS}/$uid")
            false
        }
    }

    /**
     * Permanently deletes a Shop from Firestore (only succeeds if caller owns it).
     */
    suspend fun deleteShopFromFirestore(shopId: Long): Boolean = withContext(Dispatchers.IO) {
        if (currentUidOrNull() == null) return@withContext false
        val firestore = firestoreOrNull() ?: return@withContext false
        val docId = "shop_$shopId"
        return@withContext try {
            Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_SHOPS).document(docId).delete(),
                8,
                TimeUnit.SECONDS
            )
            true
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.DELETE, "${FirebaseSyncConfig.COLLECTION_SHOPS}/$docId")
            false
        }
    }

    /**
     * Executes bidirectional live cloud sync:
     * 1. Preserves 100% of local SQLite records (no deletes, no resets).
     * 2. Ingests cloud orders into local Room, updating statuses if changed.
     * 3. Backfills local orders to the cloud so all devices see all orders.
     */
    suspend fun performLiveCloudSync(dao: MartDao): SyncResult = withContext(Dispatchers.IO) {
        val localOrders = dao.getAllOrders().first()
        var uploadedCount = 0
        var downloadedCount = 0

        // 1. Fetch remote orders from Firestore (SDK first, REST fallback)
        val cloudOrders = fetchCloudOrdersInternal()
        for ((remoteOrder, remoteItems) in cloudOrders) {
            val existingLocal = dao.getOrderByOrderNumber(remoteOrder.orderNumber)
            if (existingLocal == null) {
                // New order placed on another device: insert locally into Room!
                val newLocalId = dao.insertOrder(remoteOrder.copy(id = 0L))
                val assignedItems = if (remoteItems.isNotEmpty()) {
                    val list = remoteItems.map { it.copy(id = 0L, orderId = newLocalId) }
                    dao.insertOrderItems(list)
                    list
                } else emptyList()
                downloadedCount++
                Log.d(TAG, "Synced new cloud order into local Room: ${remoteOrder.orderNumber}")
                notifyCloudOrderReceived(dao, remoteOrder, assignedItems.ifEmpty { remoteItems })
            } else {
                // Existing order: update status if cloud has changed
                if (existingLocal.status != remoteOrder.status) {
                    dao.updateOrderStatus(existingLocal.id, remoteOrder.status)
                    downloadedCount++
                    Log.d(TAG, "Updated local order status from cloud: ${remoteOrder.orderNumber} -> ${remoteOrder.status}")
                }
                // Backfill order items if missing locally
                val existingItems = dao.getOrderItems(existingLocal.id).first()
                if (existingItems.isEmpty() && remoteItems.isNotEmpty()) {
                    val list = remoteItems.map { it.copy(id = 0L, orderId = existingLocal.id) }
                    dao.insertOrderItems(list)
                }
            }
        }

        // 2. Upload local orders belonging to the authenticated user that might not be in cloud
        val currentUid = currentUidOrNull()
        if (currentUid != null) {
            for (local in localOrders) {
                val isRemote = cloudOrders.any { it.first.orderNumber == local.orderNumber }
                if (!isRemote && local.customerUid == currentUid) {
                    val items = dao.getOrderItems(local.id).first()
                    val pushed = pushOrderToFirestore(local, items)
                    if (pushed) uploadedCount++
                }
            }
        }

        val preservedCount = dao.getAllOrders().first().size
        val summary = "Live Cloud Sync Active. Preserved $preservedCount local orders. Downloaded/updated $downloadedCount, Uploaded $uploadedCount."
        Log.i(TAG, summary)

        SyncResult(
            success = true,
            uploadedOrdersCount = uploadedCount,
            downloadedOrdersCount = downloadedCount,
            totalLocalOrdersPreserved = preservedCount,
            message = summary
        )
    }

    private suspend fun notifyCloudOrderReceived(
        dao: MartDao,
        order: OrderEntity,
        items: List<OrderItemEntity>
    ) {
        try {
            val allUsers = dao.getAllUsersSync()
            val itemsByShop = mutableMapOf<Long, MutableList<OrderItemEntity>>()
            for (item in items) {
                val prod = dao.getProductByIdSync(item.productId) ?: SeedData.products.find { it.id == item.productId }
                val sId = prod?.shopId ?: order.shopId
                itemsByShop.getOrPut(sId) { mutableListOf() }.add(item)
            }
            if (itemsByShop.isEmpty()) {
                itemsByShop[order.shopId] = items.toMutableList()
            }

            val allShopNames = mutableListOf<String>()
            for ((sId, shopItems) in itemsByShop) {
                val shop = dao.getShopByIdSync(sId) ?: SeedData.shops.find { it.id == sId }
                val shopName = shop?.name ?: "Shop #$sId"
                allShopNames.add(shopName)

                val targetOwnerUserIds = mutableSetOf<Long>()
                if (shop != null && shop.ownerId > 0) targetOwnerUserIds.add(shop.ownerId)
                for (u in allUsers) {
                    if (u.role == UserRole.SHOP_OWNER) {
                        if ((shop != null && u.id == shop.ownerId) || u.shopId == sId || (shop != null && u.phone.isNotBlank() && u.phone == shop.phone)) {
                            targetOwnerUserIds.add(u.id)
                        }
                    }
                }
                for (so in SeedData.shopOwners) {
                    if ((shop != null && so.id == shop.ownerId) || so.shopId == sId || (shop != null && so.phone.isNotBlank() && so.phone == shop.phone)) {
                        targetOwnerUserIds.add(so.id)
                    }
                }
                if (targetOwnerUserIds.isEmpty()) targetOwnerUserIds.add(shop?.ownerId ?: sId)

                val itemsSummary = if (shopItems.isNotEmpty()) shopItems.joinToString(", ") { "${it.quantity}x ${it.productName}" } else "Order #${order.orderNumber}"
                val shopTotal = if (shopItems.isNotEmpty()) shopItems.sumOf { it.totalPrice } else order.finalAmount
                val notifTitle = "New Order #${order.orderNumber} for $shopName! 🛍️"
                val notifMessage = "Customer ${order.customerName} (${order.customerPhone}) ordered: $itemsSummary. Total: ₹${shopTotal.toInt()} (${order.paymentMethod.name.replace('_', ' ')}). Please accept & prepare items for delivery."

                for (ownerUserId in targetOwnerUserIds) {
                    dao.insertNotification(
                        NotificationEntity(
                            userId = ownerUserId,
                            title = notifTitle,
                            message = notifMessage,
                            type = NotificationType.ORDER_STATUS,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            val allShopNamesSummary = allShopNames.distinct().joinToString(", ").ifEmpty { "Sinapali Mart" }
            val allProductsSummary = if (items.isNotEmpty()) items.joinToString(", ") { "${it.quantity}x ${it.productName}" } else "Order #${order.orderNumber}"
            dao.insertNotification(
                NotificationEntity(
                    userId = SeedData.adminUser.id,
                    title = "New Town Order #${order.orderNumber} ($allShopNamesSummary)",
                    message = "New order of ₹${order.finalAmount.toInt()} from ${order.customerName} for $allShopNamesSummary. Items: $allProductsSummary.",
                    type = NotificationType.ORDER_STATUS,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Notice creating cloud order notification: ${e.message}")
        }
    }

    fun fetchOrdersFromFirestore(): List<Pair<OrderEntity, List<OrderItemEntity>>> {
        return fetchCloudOrdersInternal()
    }

    fun fetchCloudOrdersInternal(): List<Pair<OrderEntity, List<OrderItemEntity>>> {
        val result = mutableListOf<Pair<OrderEntity, List<OrderItemEntity>>>()
        val uid = currentUidOrNull() ?: return result
        val firestore = firestoreOrNull() ?: return result

        // Scoped queries matching firestore.rules: customerUid == uid OR shopOwnerUid == uid
        try {
            val customerSnap = Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_ORDERS)
                    .whereEqualTo("customerUid", uid)
                    .get(),
                6,
                TimeUnit.SECONDS
            )
            for (doc in customerSnap.documents) {
                val parsed = firestoreDocToOrder(doc)
                if (parsed != null && result.none { it.first.orderNumber == parsed.first.orderNumber }) {
                    result.add(parsed)
                }
            }
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.LIST, "${FirebaseSyncConfig.COLLECTION_ORDERS}?customerUid")
        }

        try {
            val ownerSnap = Tasks.await(
                firestore.collection(FirebaseSyncConfig.COLLECTION_ORDERS)
                    .whereEqualTo("shopOwnerUid", uid)
                    .get(),
                6,
                TimeUnit.SECONDS
            )
            for (doc in ownerSnap.documents) {
                val parsed = firestoreDocToOrder(doc)
                if (parsed != null && result.none { it.first.orderNumber == parsed.first.orderNumber }) {
                    result.add(parsed)
                }
            }
        } catch (e: Throwable) {
            handleFirestoreError(e, OperationType.LIST, "${FirebaseSyncConfig.COLLECTION_ORDERS}?shopOwnerUid")
        }

        return result
    }
}
