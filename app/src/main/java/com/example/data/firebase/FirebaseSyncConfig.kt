package com.example.data.firebase

/**
 * Configuration and safety switches for Firebase Cloud Sync integration.
 * In accordance with safe migration protocol, live cloud sync is disabled by default
 * until credentials are provided and initial verification is approved.
 */
object FirebaseSyncConfig {
    /**
     * Live Cloud Sync Switch: Set to TRUE to activate live bidirectional synchronization
     * with Firebase Firestore and cloud endpoints.
     */
    const val IS_LIVE_CLOUD_SYNC_ACTIVE: Boolean = true

    /**
     * Firestore Cloud Sync Switch: Set to TRUE for live Firestore document sync.
     */
    const val IS_FIRESTORE_SYNC_ENABLED: Boolean = true

    /**
     * Data Safety Constraint: Strictly prevents any deletion or reset of local SQLite database records.
     */
    const val PRESERVE_LOCAL_DATA_STRICT: Boolean = true

    /**
     * Connected Firebase Project Identifier from google-services.json.
     */
    const val DEFAULT_FIREBASE_PROJECT_ID: String = "ama-sinapali-local-mart"
    const val FIREBASE_PROJECT_NUMBER: String = "1020386431164"
    const val FIREBASE_API_KEY: String = "AIzaSyDLGCAzPBLwn-OSbseJ6ieB1SupfSktNho"
    const val FIREBASE_APP_ID: String = "1:1020386431164:android:ce9ad3202f7ffbd02d80b9"
    const val FIREBASE_STORAGE_BUCKET: String = "ama-sinapali-local-mart.firebasestorage.app"
    const val APPLICATION_PACKAGE_NAME: String = "com.aistudio.amalocalmart.oxrkpz"

    var FIREBASE_PROJECT_ID: String = DEFAULT_FIREBASE_PROJECT_ID

    /**
     * Cloud Firestore REST Base URL for direct collection/document access.
     */
    val FIRESTORE_BASE_URL: String
        get() = "https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents"

    /**
     * Cloud Firestore Orders Collection URL used for active endpoint verification.
     */
    val FIRESTORE_ORDERS_URL: String
        get() = if (FIREBASE_PROJECT_ID == DEFAULT_FIREBASE_PROJECT_ID) {
            "$FIRESTORE_BASE_URL/$COLLECTION_ORDERS?key=$FIREBASE_API_KEY"
        } else {
            "$FIRESTORE_BASE_URL/$COLLECTION_ORDERS"
        }

    val FIRESTORE_SHOPS_URL: String
        get() = if (FIREBASE_PROJECT_ID == DEFAULT_FIREBASE_PROJECT_ID) {
            "$FIRESTORE_BASE_URL/$COLLECTION_SHOPS?key=$FIREBASE_API_KEY"
        } else {
            "$FIRESTORE_BASE_URL/$COLLECTION_SHOPS"
        }

    val FIRESTORE_PRODUCTS_URL: String
        get() = if (FIREBASE_PROJECT_ID == DEFAULT_FIREBASE_PROJECT_ID) {
            "$FIRESTORE_BASE_URL/$COLLECTION_PRODUCTS?key=$FIREBASE_API_KEY"
        } else {
            "$FIRESTORE_BASE_URL/$COLLECTION_PRODUCTS"
        }

    /**
     * Firebase Realtime Database Base URL for instant real-time listener fallback.
     */
    val FIREBASE_RTDB_BASE_URL: String
        get() = "https://$FIREBASE_PROJECT_ID-rtdb.firebaseio.com"

    /**
     * Shadow validation flag: Allows local dry-run schema validation and serialization tests
     * without modifying local SQLite tables or sending network payloads.
     */
    const val DRY_RUN_VALIDATION_ENABLED: Boolean = true

    // Collection names in Firebase Firestore / Realtime Database
    const val COLLECTION_ORDERS = "orders"
    const val COLLECTION_SHOPS = "shops"
    const val COLLECTION_PRODUCTS = "products"
    const val COLLECTION_PROMOTIONS = "promotions"
    const val COLLECTION_COMPLAINTS = "complaints"
    const val COLLECTION_REVIEWS = "reviews"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_SYSTEM_METADATA = "system_metadata"

    // Document ID patterns
    const val KEY_PREFIX_ORDER = "order_"
    const val KEY_PREFIX_SHOP = "shop_"
    const val KEY_PREFIX_PRODUCT = "prod_"
    const val KEY_PREFIX_PROMOTION = "promo_"
    const val KEY_PREFIX_COMPLAINT = "comp_"

    // Network timeout settings
    const val CONNECTION_TIMEOUT_MS = 10000
    const val READ_TIMEOUT_MS = 12000
    const val MAX_RETRY_ATTEMPTS = 3
    const val SYNC_BATCH_SIZE = 50

    /**
     * Conflict Resolution Strategies:
     * - ORDERS: Immutable Order Numbers; Status changes follow latest placedAt/updatedAt timestamp.
     * - SHOPS/PRODUCTS: Last-Write-Wins (LWW) based on updatedAt.
     * - USERS: Scoped to Firebase Auth UID / local phone identity.
     */
    enum class ConflictStrategy {
        LAST_WRITE_WINS,
        IMMUTABLE_INSERT_ONLY,
        SERVER_TIMESTAMP_PREFERRED
    }
}
