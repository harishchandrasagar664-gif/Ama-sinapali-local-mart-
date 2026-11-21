package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SeedData
import com.example.data.local.entities.*
import com.example.data.firebase.FirestoreSyncService
import com.example.data.firebase.FirebaseSyncConfig
import com.example.data.repository.CloudSaveResult
import com.example.data.repository.MartRepository
import com.example.domain.model.*
import com.example.service.AuthManager
import com.example.service.FirebasePhoneAuthHelper
import com.example.service.PhoneAuthSignInResult
import com.example.service.RazorpayPaymentServiceImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ProductSyncStatus {
    object Idle : ProductSyncStatus()
    object Syncing : ProductSyncStatus()
    data class Synced(val message: String, val timestamp: Long = System.currentTimeMillis()) : ProductSyncStatus()
    data class Error(val message: String, val requiresAuth: Boolean = false) : ProductSyncStatus()
}

data class PromotionPricingConfig(
    val productAdDailyRate: Double = 40.0,
    val promotionalBannerDailyRate: Double = 80.0,
    val featuredShopDailyRate: Double = 100.0,
    val locationBasedDailyRate: Double = 60.0,
    val dateBasedDailyRate: Double = 50.0
) {
    fun getDailyRate(type: PromotionType): Double = when (type) {
        PromotionType.PRODUCT_AD -> productAdDailyRate
        PromotionType.PROMOTIONAL_BANNER, PromotionType.BANNER -> promotionalBannerDailyRate
        PromotionType.FEATURED_SHOP -> featuredShopDailyRate
        PromotionType.LOCATION_BASED -> locationBasedDailyRate
        PromotionType.DATE_BASED -> dateBasedDailyRate
        PromotionType.FEATURED_PRODUCT -> productAdDailyRate
        PromotionType.DISCOUNT_COUPON -> promotionalBannerDailyRate
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = MartRepository(database.martDao())
    val authManager = AuthManager(repository, application.applicationContext)
    val paymentService = RazorpayPaymentServiceImpl()

    private val screenBackStack = ArrayDeque<Screen>()

    private val _currentScreen = MutableStateFlow<Screen>(
        if (authManager.isShopOwnerSessionActive()) Screen.ShopOwnerDashboard else Screen.CustomerHome
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _isOrderSyncing = MutableStateFlow(false)
    val isOrderSyncing: StateFlow<Boolean> = _isOrderSyncing.asStateFlow()

    private val _lastOrderSyncTime = MutableStateFlow<Long?>(null)
    val lastOrderSyncTime: StateFlow<Long?> = _lastOrderSyncTime.asStateFlow()

    private val _orderSyncStatusMessage = MutableStateFlow<String?>("Firebase Firestore Connection: Verifying...")
    val orderSyncStatusMessage: StateFlow<String?> = _orderSyncStatusMessage.asStateFlow()

    private val _firebaseVerificationReport = MutableStateFlow<FirestoreSyncService.FirebaseVerificationReport?>(null)
    val firebaseVerificationReport: StateFlow<FirestoreSyncService.FirebaseVerificationReport?> = _firebaseVerificationReport.asStateFlow()

    private val _isVerifyingFirebase = MutableStateFlow(false)
    val isVerifyingFirebase: StateFlow<Boolean> = _isVerifyingFirebase.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _isProductSyncing = MutableStateFlow(false)
    val isProductSyncing: StateFlow<Boolean> = _isProductSyncing.asStateFlow()

    private val _productSyncStatus = MutableStateFlow<ProductSyncStatus>(ProductSyncStatus.Idle)
    val productSyncStatus: StateFlow<ProductSyncStatus> = _productSyncStatus.asStateFlow()

    private val _isCustomerStoreLoading = MutableStateFlow(false)
    val isCustomerStoreLoading: StateFlow<Boolean> = _isCustomerStoreLoading.asStateFlow()

    private val _storeSyncError = MutableStateFlow<String?>(null)
    val storeSyncError: StateFlow<String?> = _storeSyncError.asStateFlow()

    private val _lastCatalogSyncTime = MutableStateFlow<Long?>(null)
    val lastCatalogSyncTime: StateFlow<Long?> = _lastCatalogSyncTime.asStateFlow()

    private var ordersCustomerListener: ListenerRegistration? = null
    private var ordersOwnerListener: ListenerRegistration? = null
    private var shopsPublicListener: ListenerRegistration? = null
    private var shopsOwnerListener: ListenerRegistration? = null
    private var productsPublicListener: ListenerRegistration? = null
    private var productsOwnerListener: ListenerRegistration? = null
    private val syncedShopIdsInSession = java.util.Collections.synchronizedSet(mutableSetOf<Long>())
    private val _currentFirebaseUid = MutableStateFlow(FirebasePhoneAuthHelper.currentUid())
    val currentFirebaseUid: StateFlow<String?> = _currentFirebaseUid.asStateFlow()

    private var lastObservedFirebaseUid: String? = FirebasePhoneAuthHelper.currentUid()

    private val authStateListener = FirebaseAuth.AuthStateListener {
        val currentUid = FirebasePhoneAuthHelper.currentUid()
        _currentFirebaseUid.value = currentUid
        if (currentUid != lastObservedFirebaseUid) {
            syncedShopIdsInSession.clear()
            lastObservedFirebaseUid = currentUid
        }
        attachRealtimeFirestoreListeners()
        verifyFirebaseConnection()
    }

    init {
        try {
            val prefs = application.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
            val savedProjectId = prefs.getString("firebase_project_id", null)
            if (!savedProjectId.isNullOrBlank() && savedProjectId != "ais-asia-southeast1-2c23554d43" && savedProjectId != "ama-sinapali-default") {
                FirebaseSyncConfig.FIREBASE_PROJECT_ID = savedProjectId.trim()
            } else {
                FirebaseSyncConfig.FIREBASE_PROJECT_ID = FirebaseSyncConfig.DEFAULT_FIREBASE_PROJECT_ID
            }
        } catch (_: Throwable) { }
        authManager.restoreSession(viewModelScope)
        if (authManager.isShopOwnerSessionActive()) {
            _currentScreen.value = Screen.ShopOwnerDashboard
        }
        try {
            FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        } catch (_: Throwable) { }
        verifyFirebaseConnection()
        syncOrdersFromCloud()
        syncShopsAndProductsFromCloud()
        startPeriodicOrderSync()
        attachRealtimeFirestoreListeners()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        } catch (_: Throwable) { }
        detachRealtimeFirestoreListeners()
    }

    private fun detachRealtimeFirestoreListeners() {
        ordersCustomerListener?.remove(); ordersCustomerListener = null
        ordersOwnerListener?.remove(); ordersOwnerListener = null
        shopsPublicListener?.remove(); shopsPublicListener = null
        shopsOwnerListener?.remove(); shopsOwnerListener = null
        productsPublicListener?.remove(); productsPublicListener = null
        productsOwnerListener?.remove(); productsOwnerListener = null
    }

    private fun attachRealtimeFirestoreListeners() {
        detachRealtimeFirestoreListeners()
        try {
            val firestore = FirebaseFirestore.getInstance()
            val uid = FirebasePhoneAuthHelper.currentUid()

            // Public approved shops listener (matches firestore.rules allow list)
            shopsPublicListener = firestore.collection(FirebaseSyncConfig.COLLECTION_SHOPS)
                .whereEqualTo("isApproved", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val msg = FirestoreSyncService.handleFirestoreError(error, FirestoreSyncService.OperationType.LIST, FirebaseSyncConfig.COLLECTION_SHOPS)
                        _storeSyncError.value = msg
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                FirestoreSyncService.firestoreDocToShop(doc)?.let {
                                    repository.insertOrUpdateShopFromCloud(it)
                                }
                            }
                        }
                    }
                }

            // Public approved & available products listener (matches firestore.rules allow list)
            productsPublicListener = firestore.collection(FirebaseSyncConfig.COLLECTION_PRODUCTS)
                .whereEqualTo("isApproved", true)
                .whereEqualTo("isAvailable", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val msg = FirestoreSyncService.handleFirestoreError(error, FirestoreSyncService.OperationType.LIST, FirebaseSyncConfig.COLLECTION_PRODUCTS)
                        _storeSyncError.value = msg
                        return@addSnapshotListener
                    }
                    _storeSyncError.value = null
                    _lastCatalogSyncTime.value = System.currentTimeMillis()
                    if (snapshot != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            if (snapshot.documentChanges.isNotEmpty()) {
                                for (change in snapshot.documentChanges) {
                                    when (change.type) {
                                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                            FirestoreSyncService.firestoreDocToProduct(change.document)?.let {
                                                repository.insertOrUpdateProductFromCloud(it)
                                            }
                                        }
                                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                            FirestoreSyncService.firestoreDocToProduct(change.document)?.let { removedProd ->
                                                val currentFirebaseUid = FirebasePhoneAuthHelper.currentUid()
                                                if (removedProd.ownerUid.isBlank() || removedProd.ownerUid != currentFirebaseUid) {
                                                    repository.deleteProductLocally(removedProd)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                for (doc in snapshot.documents) {
                                    FirestoreSyncService.firestoreDocToProduct(doc)?.let {
                                        repository.insertOrUpdateProductFromCloud(it)
                                    }
                                }
                            }
                        }
                    }
                }

            // Authenticated user-scoped listeners (own shops, own products, own orders)
            if (!uid.isNullOrBlank()) {
                shopsOwnerListener = firestore.collection(FirebaseSyncConfig.COLLECTION_SHOPS)
                    .whereEqualTo("ownerUid", uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            FirestoreSyncService.handleFirestoreError(error, FirestoreSyncService.OperationType.LIST, "${FirebaseSyncConfig.COLLECTION_SHOPS}?ownerUid")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                for (doc in snapshot.documents) {
                                    FirestoreSyncService.firestoreDocToShop(doc)?.let {
                                        repository.insertOrUpdateShopFromCloud(it)
                                    }
                                }
                            }
                        }
                    }

                productsOwnerListener = firestore.collection(FirebaseSyncConfig.COLLECTION_PRODUCTS)
                    .whereEqualTo("ownerUid", uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            val msg = FirestoreSyncService.handleFirestoreError(error, FirestoreSyncService.OperationType.LIST, "${FirebaseSyncConfig.COLLECTION_PRODUCTS}?ownerUid")
                            _productSyncStatus.value = ProductSyncStatus.Error(msg, requiresAuth = true)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                for (change in snapshot.documentChanges) {
                                    when (change.type) {
                                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                            FirestoreSyncService.firestoreDocToProduct(change.document)?.let {
                                                repository.insertOrUpdateProductFromCloud(it)
                                            }
                                        }
                                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                            FirestoreSyncService.firestoreDocToProduct(change.document)?.let {
                                                repository.deleteProductLocally(it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                ordersCustomerListener = firestore.collection(FirebaseSyncConfig.COLLECTION_ORDERS)
                    .whereEqualTo("customerUid", uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            FirestoreSyncService.handleFirestoreError(error, FirestoreSyncService.OperationType.LIST, "${FirebaseSyncConfig.COLLECTION_ORDERS}?customerUid")
                            return@addSnapshotListener
                        }
                        snapshot?.let { snap ->
                            viewModelScope.launch(Dispatchers.IO) {
                                for (doc in snap.documents) {
                                    FirestoreSyncService.firestoreDocToOrder(doc)?.let { (remoteOrder, remoteItems) ->
                                        repository.insertOrderFromCloud(remoteOrder, remoteItems)
                                    }
                                }
                            }
                        }
                    }

                ordersOwnerListener = firestore.collection(FirebaseSyncConfig.COLLECTION_ORDERS)
                    .whereEqualTo("shopOwnerUid", uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            FirestoreSyncService.handleFirestoreError(error, FirestoreSyncService.OperationType.LIST, "${FirebaseSyncConfig.COLLECTION_ORDERS}?shopOwnerUid")
                            return@addSnapshotListener
                        }
                        snapshot?.let { snap ->
                            viewModelScope.launch(Dispatchers.IO) {
                                for (doc in snap.documents) {
                                    FirestoreSyncService.firestoreDocToOrder(doc)?.let { (remoteOrder, remoteItems) ->
                                        repository.insertOrderFromCloud(remoteOrder, remoteItems)
                                    }
                                }
                            }
                        }
                    }
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainViewModel", "Could not initialize Realtime Firebase listeners: ${e.message}")
        }
    }

    fun syncShopsAndProductsFromCloud() {
        viewModelScope.launch {
            _isCustomerStoreLoading.value = true
            _isProductSyncing.value = true
            try {
                val report = repository.syncShopsAndProductsFromCloudWithResult()
                if (report.success) {
                    _storeSyncError.value = null
                    _lastCatalogSyncTime.value = System.currentTimeMillis()
                    if (FirebasePhoneAuthHelper.currentUid() != null) {
                        _productSyncStatus.value = ProductSyncStatus.Synced(
                            "Cloud Firestore Synced (${report.syncedShopsCount} shops, ${report.syncedProductsCount} products)"
                        )
                    }
                } else {
                    _storeSyncError.value = report.errorMessage ?: "Unable to sync products from Cloud Firestore."
                    _productSyncStatus.value = ProductSyncStatus.Error(
                        message = report.errorMessage ?: "Unable to sync products from Cloud Firestore.",
                        requiresAuth = report.isAuthOrPermissionError
                    )
                }
            } catch (e: Throwable) {
                val msg = e.message ?: "Cloud sync error"
                _storeSyncError.value = msg
                _productSyncStatus.value = ProductSyncStatus.Error(msg)
            } finally {
                _isCustomerStoreLoading.value = false
                _isProductSyncing.value = false
            }
        }
    }

    fun retryCatalogSync() {
        syncShopsAndProductsFromCloud()
    }

    fun retryProductSync() {
        viewModelScope.launch {
            _isProductSyncing.value = true
            _productSyncStatus.value = ProductSyncStatus.Syncing
            val uid = FirebasePhoneAuthHelper.currentUid()
            if (uid.isNullOrBlank()) {
                _isProductSyncing.value = false
                val msg = "Mobile OTP login expired or not active. Tap 'Re-login with OTP' to sync your shop & products to Cloud Firestore."
                _productSyncStatus.value = ProductSyncStatus.Error(msg, requiresAuth = true)
                _userMessage.value = msg
                return@launch
            }
            try {
                val user = currentUser.value
                val shops = repository.getAllShopsSync()
                val myShop = shops.find {
                    it.ownerId == user?.id ||
                    (user?.shopId != null && it.id == user.shopId) ||
                    (!user?.phone.isNullOrBlank() && it.phone == user?.phone)
                }
                if (myShop != null) {
                    val shopImg = withContext(Dispatchers.IO) {
                        com.example.util.ImageStorageHelper.toUniversalImageUrl(getApplication(), myShop.imageUrl)
                    }
                    val shopToSync = myShop.copy(ownerUid = uid, imageUrl = shopImg)
                    val shopSave = repository.updateShop(shopToSync, user?.copy(firebaseUid = uid))
                    if (!shopSave.cloudSynced) {
                        _productSyncStatus.value = ProductSyncStatus.Error(
                            shopSave.errorMessage ?: "Could not sync shop to Cloud Firestore.",
                            requiresAuth = shopSave.requiresAuth
                        )
                        _userMessage.value = shopSave.errorMessage
                        return@launch
                    }
                    syncedShopIdsInSession.add(myShop.id)
                    val myProducts = repository.getAllProductsSync().filter { it.shopId == myShop.id }
                    var syncedCount = 0
                    for (prod in myProducts) {
                        val prodImg = withContext(Dispatchers.IO) {
                            com.example.util.ImageStorageHelper.toUniversalImageUrl(getApplication(), prod.imageUrl)
                        }
                        val prodToSync = prod.copy(ownerUid = uid, imageUrl = prodImg)
                        val res = repository.updateProduct(prodToSync)
                        if (res.cloudSynced) syncedCount++
                        else {
                            _productSyncStatus.value = ProductSyncStatus.Error(
                                res.errorMessage ?: "Failed to sync '${prod.name}' to Cloud Firestore.",
                                requiresAuth = res.requiresAuth
                            )
                            _userMessage.value = res.errorMessage
                            return@launch
                        }
                    }
                }
                val report = repository.syncShopsAndProductsFromCloudWithResult()
                if (report.success) {
                    _storeSyncError.value = null
                    _lastCatalogSyncTime.value = System.currentTimeMillis()
                    _productSyncStatus.value = ProductSyncStatus.Synced("Cloud Firestore Synced ☁️")
                    _userMessage.value = "All shop products synced to Cloud Firestore! ☁️"
                } else {
                    _productSyncStatus.value = ProductSyncStatus.Error(
                        report.errorMessage ?: "Sync error",
                        requiresAuth = report.isAuthOrPermissionError
                    )
                }
            } finally {
                _isProductSyncing.value = false
            }
        }
    }

    fun verifyFirebaseConnection() {
        viewModelScope.launch {
            _isVerifyingFirebase.value = true
            try {
                val report = FirestoreSyncService.verifyFirebaseConnection()
                _firebaseVerificationReport.value = report
                _orderSyncStatusMessage.value = report.statusMessage
            } catch (_: Throwable) {
                // Ignore
            } finally {
                _isVerifyingFirebase.value = false
            }
        }
    }

    fun setFirebaseProjectId(newProjectId: String) {
        val cleanId = newProjectId.trim()
        FirebaseSyncConfig.FIREBASE_PROJECT_ID = cleanId
        try {
            val prefs = getApplication<Application>().getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("firebase_project_id", cleanId).apply()
        } catch (_: Throwable) { }
        verifyFirebaseConnection()
    }

    fun syncOrdersFromCloud() {
        viewModelScope.launch {
            _isOrderSyncing.value = true
            try {
                val count = repository.syncOrdersFromCloud()
                _lastOrderSyncTime.value = System.currentTimeMillis()
                _orderSyncStatusMessage.value = if (count > 0) "Synced $count orders from cloud across all devices ☁️" else "All town orders up to date with cloud ☁️"
            } catch (_: Throwable) {
                _orderSyncStatusMessage.value = "Local mode active"
            } finally {
                _isOrderSyncing.value = false
            }
        }
    }

    private fun startPeriodicOrderSync() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(15000)
                try {
                    repository.syncOrdersFromCloud()
                    val report = repository.syncShopsAndProductsFromCloudWithResult()
                    if (report.success) {
                        _storeSyncError.value = null
                        _lastCatalogSyncTime.value = System.currentTimeMillis()
                    }
                    _lastOrderSyncTime.value = System.currentTimeMillis()
                } catch (_: Throwable) {
                    // Ignore background sync errors
                }
            }
        }
    }

    private val _appLanguage = MutableStateFlow<com.example.domain.model.AppLanguage>(com.example.domain.model.AppLanguage.ENGLISH)
    val appLanguage: StateFlow<com.example.domain.model.AppLanguage> = _appLanguage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val activeShops: StateFlow<List<ShopEntity>> = repository.getActiveShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShops: StateFlow<List<ShopEntity>> = repository.getAllShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProducts: StateFlow<List<ProductEntity>> = repository.getActiveProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val banners: StateFlow<List<BannerEntity>> = repository.getActiveBanners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offers: StateFlow<List<OfferEntity>> = repository.getActiveOffers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<OrderEntity>> = repository.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allOrderItems: StateFlow<List<OrderItemEntity>> = repository.getAllOrderItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPromotions: StateFlow<List<PromotionEntity>> = repository.getAllPromotions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val approvedPromotions: StateFlow<List<PromotionEntity>> = repository.getApprovedPromotions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _promotionPricingConfig = MutableStateFlow(PromotionPricingConfig())
    val promotionPricingConfig: StateFlow<PromotionPricingConfig> = _promotionPricingConfig.asStateFlow()

    val allComplaints: StateFlow<List<ComplaintEntity>> = repository.getAllComplaints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<UserEntity?> = authManager.currentUser
    val currentRole: StateFlow<UserRole> = authManager.currentRole

    val cartItems: StateFlow<List<CartItemEntity>> = combine(currentUser, currentRole) { user, role ->
        Pair(user, role)
    }.flatMapLatest { (user, role) ->
        if (user != null && role == UserRole.CUSTOMER) repository.getCartItems(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detailedCartItems: StateFlow<List<CartItemWithProduct>> = combine(cartItems, allProducts, allShops) { carts, prods, shops ->
        val prodMap = prods.associateBy { it.id }
        val shopMap = shops.associateBy { it.id }
        carts.mapNotNull { cart ->
            val p = prodMap[cart.productId] ?: return@mapNotNull null
            val s = shopMap[p.shopId]
            CartItemWithProduct(
                cartItemId = cart.id,
                productId = p.id,
                shopId = p.shopId,
                shopName = s?.name ?: "Sinapali Shop",
                productName = p.name,
                imageUrl = p.imageUrl,
                unitPrice = p.price,
                discountPrice = p.discountPrice,
                quantity = cart.quantity,
                stock = p.stock
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _appliedCoupon = MutableStateFlow<OfferEntity?>(null)
    val appliedCoupon: StateFlow<OfferEntity?> = _appliedCoupon.asStateFlow()

    private val _couponError = MutableStateFlow<String?>(null)
    val couponError: StateFlow<String?> = _couponError.asStateFlow()

    private val _wishlistProductIds = MutableStateFlow<Set<Long>>(emptySet())
    val wishlistProductIds: StateFlow<Set<Long>> = _wishlistProductIds.asStateFlow()

    val notifications: StateFlow<List<NotificationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                if (user.role == UserRole.ADMIN || user.role == UserRole.SHOP_OWNER) {
                    repository.getAllNotifications()
                } else {
                    repository.getNotifications(user.id)
                }
            } else {
                repository.getAllNotifications()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _showPaymentModal = MutableStateFlow(false)
    val showPaymentModal: StateFlow<Boolean> = _showPaymentModal.asStateFlow()

    private val _pendingOrderParams = MutableStateFlow<OrderPlacementParams?>(null)
    val pendingOrderParams: StateFlow<OrderPlacementParams?> = _pendingOrderParams.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun navigateTo(screen: Screen) {
        val role = currentRole.value
        val user = currentUser.value

        when (screen) {
            is Screen.AdminDashboard,
            is Screen.AdminDelivery,
            is Screen.AdminOrders,
            is Screen.AdminShops,
            is Screen.AdminPromotions,
            is Screen.AdminProducts,
            is Screen.AdminUsers,
            is Screen.AdminComplaints -> {
                if (role != UserRole.ADMIN || user?.role != UserRole.ADMIN) {
                    _userMessage.value = "Access Denied: Only Admin (Harishchandra Sagar) can access the Admin Panel."
                    return
                }
            }

            is Screen.ShopOwnerDashboard,
            is Screen.ShopOrderManagement,
            is Screen.ShopProductManagement,
            is Screen.ShopOffersPromotions,
            is Screen.ShopProfile -> {
                if (role != UserRole.SHOP_OWNER || user?.role != UserRole.SHOP_OWNER) {
                    _userMessage.value = "Access Denied: Customers cannot access the Shop Owner dashboard."
                    return
                }
            }

            // Strict Shop Owner restrictions: Shop Owners must never access customer accounts, profiles, carts, order history, or other shops' data
            is Screen.CustomerProfile,
            is Screen.CustomerAuth,
            is Screen.Cart,
            is Screen.Checkout,
            is Screen.CustomerOrders,
            is Screen.OrderTracking -> {
                if (role == UserRole.SHOP_OWNER) {
                    _userMessage.value = "Access Denied: Shop Owners cannot access customer accounts, profiles, carts, or order history."
                    return
                }
            }

            is Screen.ShopDetail -> {
                if (role == UserRole.SHOP_OWNER) {
                    val myShop = allShops.value.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user?.shopId) }
                    if (myShop != null && screen.shopId != myShop.id) {
                        _userMessage.value = "Access Denied: Shop Owners can only view and manage their own shop."
                        return
                    }
                }
            }

            else -> {
                // Public customer screens accessible
            }
        }

        if (screen is Screen.ShopOwnerAuth && authManager.isShopOwnerFirebaseSessionValid()) {
            authManager.restoreShopOwnerSession()
            screenBackStack.removeAll { it is Screen.ShopOwnerAuth || it is Screen.CustomerAuth }
            _currentScreen.value = Screen.ShopOwnerDashboard
            return
        }

        val cur = _currentScreen.value
        if (cur is Screen.CustomerAuth && screen is Screen.CustomerHome) {
            screenBackStack.removeAll { it is Screen.CustomerAuth }
        }
        if (cur !is Screen.Splash && cur != screen) {
            if (screenBackStack.lastOrNull() != cur) {
                screenBackStack.addLast(cur)
            }
        }
        _currentScreen.value = screen
    }

    fun navigateBack() {
        // Back navigation must never log out the customer or clear the active customer session.
        // If a customer is logged in, never pop back into CustomerAuth.
        while (screenBackStack.isNotEmpty() && authManager.isCustomerSessionActive() && screenBackStack.last() is Screen.CustomerAuth) {
            screenBackStack.removeLast()
        }
        // If in Shop Owner role or has active shop owner session, stay in shop owner screens
        if (currentRole.value == UserRole.SHOP_OWNER || authManager.isShopOwnerSessionActive()) {
            screenBackStack.removeAll {
                it is Screen.CustomerProfile ||
                it is Screen.CustomerAuth ||
                it is Screen.ShopOwnerAuth ||
                it is Screen.Cart ||
                it is Screen.Checkout ||
                it is Screen.CustomerOrders ||
                it is Screen.OrderTracking ||
                it is Screen.CustomerHome
            }
            if (screenBackStack.isNotEmpty()) {
                val previous = screenBackStack.removeLast()
                _currentScreen.value = previous
            } else {
                _currentScreen.value = Screen.ShopOwnerDashboard
            }
            return
        }
        if (screenBackStack.isNotEmpty()) {
            val previous = screenBackStack.removeLast()
            _currentScreen.value = previous
        } else {
            _currentScreen.value = if (currentRole.value == UserRole.SHOP_OWNER) Screen.ShopOwnerDashboard else Screen.CustomerHome
        }
    }

    fun logoutShopOwner() {
        authManager.logoutShopOwner()
        _currentFirebaseUid.value = FirebasePhoneAuthHelper.currentUid()
        screenBackStack.clear()
        _currentScreen.value = Screen.CustomerHome
        _userMessage.value = "Shop Owner logged out successfully."
    }

    fun reauthenticateShopOwnerWithOtp() {
        authManager.clearShopOwnerSession()
        resetShopOwnerOtpState()
        _currentFirebaseUid.value = FirebasePhoneAuthHelper.currentUid()
        _currentScreen.value = Screen.ShopOwnerAuth
    }

    fun logoutCustomer() {
        authManager.logout()
        _currentFirebaseUid.value = FirebasePhoneAuthHelper.currentUid()
        screenBackStack.clear()
        _currentScreen.value = Screen.CustomerHome
        _userMessage.value = "Logged out successfully."
    }

    /**
     * Permanently deletes customer account, local records, cart, addresses,
     * and wipes data from Firebase backend. Complies with Google Play Data Deletion Policy.
     */
    fun deleteCustomerAccount() {
        val user = currentUser.value
        val userId = user?.id ?: authManager.getPersistedCustomerId()
        viewModelScope.launch {
            if (userId > 0L) {
                repository.deleteCustomerAccountAndData(userId)
            }
            authManager.logout()
            screenBackStack.clear()
            _currentScreen.value = Screen.CustomerHome
            _userMessage.value = "Your account and associated personal data have been permanently deleted from Ama Sinapali & Firebase."
        }
    }

    /**
     * Permanently deletes shop owner account, shop records, products,
     * promotions and wipes data from Firebase backend.
     */
    fun deleteShopOwnerAccount() {
        val user = currentUser.value
        val userId = user?.id ?: 0L
        val shopId = user?.shopId
        viewModelScope.launch {
            if (userId > 0L) {
                repository.deleteShopOwnerAccountAndData(userId, shopId)
            }
            authManager.logoutShopOwner()
            screenBackStack.clear()
            _currentScreen.value = Screen.CustomerHome
            _userMessage.value = "Shop Owner account, shop inventory, and cloud data permanently deleted."
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun toggleWishlist(productId: Long) {
        val current = _wishlistProductIds.value
        _wishlistProductIds.value = if (current.contains(productId)) current - productId else current + productId
    }

    fun addToCart(productId: Long, shopId: Long, quantity: Int = 1) {
        val user = currentUser.value ?: return
        if (currentRole.value == UserRole.SHOP_OWNER) {
            _userMessage.value = "Access Denied: Shop Owners cannot create or modify customer carts."
            return
        }
        viewModelScope.launch {
            repository.addToCart(user.id, productId, shopId, quantity)
            _userMessage.value = "Added to basket! 🛒"
        }
    }

    fun updateCartQuantity(cartItem: CartItemWithProduct, delta: Int) {
        viewModelScope.launch {
            val newQty = cartItem.quantity + delta
            if (newQty <= 0) {
                val rawItem = cartItems.value.find { it.id == cartItem.cartItemId }
                if (rawItem != null) repository.removeCartItem(rawItem)
            } else {
                repository.updateCartQuantity(cartItem.cartItemId, newQty)
            }
        }
    }

    fun applyCoupon(code: String) {
        viewModelScope.launch {
            val offer = repository.getOfferByCode(code.trim().uppercase())
            if (offer != null) {
                _appliedCoupon.value = offer
                _couponError.value = null
                _userMessage.value = "Coupon applied: ${offer.discountPercent}% OFF!"
            } else {
                _appliedCoupon.value = null
                _couponError.value = "Invalid or expired coupon"
            }
        }
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
        _couponError.value = null
    }

    fun initiateCheckout(
        deliveryAddress: String,
        deliveryType: DeliveryType,
        paymentMethod: PaymentMethod
    ) {
        val user = currentUser.value ?: return
        if (currentRole.value == UserRole.SHOP_OWNER) {
            _userMessage.value = "Access Denied: Shop Owners cannot access checkout or customer carts."
            return
        }
        val items = detailedCartItems.value
        if (items.isEmpty()) return

        val params = OrderPlacementParams(
            deliveryAddress = deliveryAddress,
            contactPhone = user.phone,
            customerName = user.name,
            deliveryType = deliveryType,
            paymentMethod = paymentMethod,
            couponCode = appliedCoupon.value?.couponCode
        )

        if (paymentMethod == PaymentMethod.RAZORPAY) {
            _pendingOrderParams.value = params
            _showPaymentModal.value = true
        } else {
            completeOrder(params, null)
        }
    }

    fun onRazorpayPaymentSuccess(razorpayPaymentId: String) {
        _showPaymentModal.value = false
        val params = _pendingOrderParams.value
        if (params != null) {
            completeOrder(params, razorpayPaymentId)
            _pendingOrderParams.value = null
        }
    }

    fun onRazorpayPaymentDismiss() {
        _showPaymentModal.value = false
        _pendingOrderParams.value = null
    }

    private fun completeOrder(params: OrderPlacementParams, razorpayId: String?) {
        val user = currentUser.value ?: return
        val items = detailedCartItems.value
        if (items.isEmpty()) return

        val shop = allShops.value.find { it.id == items.first().shopId } ?: allShops.value.firstOrNull() ?: return
        val subtotal = items.sumOf { it.itemTotal }
        val discount = appliedCoupon.value?.let { (subtotal * it.discountPercent / 100.0).coerceAtMost(it.maxDiscountAmount) } ?: 0.0
        val deliveryFee = if (params.deliveryType == DeliveryType.HOME_DELIVERY) 25.0 else 0.0

        viewModelScope.launch {
            val orderId = repository.placeOrder(
                customer = user,
                shop = shop,
                items = items,
                deliveryAddress = params.deliveryAddress,
                deliveryType = params.deliveryType,
                paymentMethod = params.paymentMethod,
                couponCode = params.couponCode,
                discountAmount = discount,
                deliveryFee = deliveryFee,
                razorpayPaymentId = razorpayId
            )
            removeCoupon()
            launch(Dispatchers.IO) {
                try {
                    repository.syncOrdersFromCloud()
                } catch (_: Throwable) { }
            }
            navigateTo(Screen.OrderTracking(orderId))
            _userMessage.value = "Order placed successfully! 🎉"
        }
    }

    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus) {
        val role = currentRole.value
        val user = currentUser.value

        if (role == UserRole.CUSTOMER) {
            _userMessage.value = "Access Denied: Customers cannot change order status."
            return
        }

        if (role == UserRole.SHOP_OWNER) {
            val myShop = allShops.value.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user.shopId) }
            val order = allOrders.value.find { it.id == orderId }
            if (myShop == null || order?.shopId != myShop.id) {
                _userMessage.value = "Access Denied: You can only update orders for your own shop."
                return
            }
        }

        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            _userMessage.value = "Order status updated to ${newStatus.name.replace('_', ' ')}"
        }
    }

    // Critical: Admin only handles delivery completion
    fun completeAdminDelivery(orderId: Long, enteredOtp: String, actualOtp: String, codCollected: Double?) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin (Harishchandra Sagar) can complete deliveries."
            return
        }
        viewModelScope.launch {
            repository.completeAdminDelivery(orderId, codCollected, "Harishchandra Sagar (Admin)")
            _userMessage.value = "Delivery marked complete! Cash collected & customer notified. 📦"
        }
    }

    fun addOrUpdateProduct(product: ProductEntity) {
        val role = currentRole.value
        val user = currentUser.value

        if (role == UserRole.CUSTOMER) {
            _userMessage.value = "Access Denied: Customers cannot manage products."
            return
        }

        val firebaseUid = FirebasePhoneAuthHelper.currentUid()
        viewModelScope.launch {
            _isProductSyncing.value = true
            _productSyncStatus.value = ProductSyncStatus.Syncing
            try {
                val shops = repository.getAllShopsSync()
                if (role == UserRole.SHOP_OWNER) {
                    val myShop = shops.find {
                        it.ownerId == user?.id ||
                        (user?.shopId != null && it.id == user.shopId) ||
                        (!user?.phone.isNullOrBlank() && it.phone == user?.phone)
                    }
                    if (myShop == null || product.shopId != myShop.id) {
                        _productSyncStatus.value = ProductSyncStatus.Error("Access Denied: You can only manage products for your own shop.")
                        _userMessage.value = "Access Denied: You can only manage products for your own shop."
                        return@launch
                    }
                }

                if (firebaseUid.isNullOrBlank()) {
                    _productSyncStatus.value = ProductSyncStatus.Error(
                        message = "Please re-login with Mobile OTP so your product can sync to Cloud Firestore.",
                        requiresAuth = true
                    )
                    _userMessage.value = "Sign in with Mobile OTP required to sync products to Cloud Firestore."
                    return@launch
                }

                val universalImageUrl = withContext(Dispatchers.IO) {
                    com.example.util.ImageStorageHelper.toUniversalImageUrl(
                        getApplication(),
                        product.imageUrl.ifBlank { "hero_local_mart" }
                    )
                }
                val resolvedUid = if (role == UserRole.SHOP_OWNER) firebaseUid else product.ownerUid.ifBlank { firebaseUid }
                val productToSave = product.copy(
                    imageUrl = universalImageUrl,
                    ownerUid = resolvedUid
                )

                // Ensure parent shop exists in Firestore with matching ownerUid before pushing product
                val parentShop = shops.find { it.id == productToSave.shopId }
                    ?: repository.getShopByIdSync(productToSave.shopId)
                var parentShopError: CloudSaveResult<Unit>? = null
                if (parentShop != null &&
                    (parentShop.ownerUid.isBlank() ||
                        parentShop.ownerUid != firebaseUid ||
                        !syncedShopIdsInSession.contains(parentShop.id))
                ) {
                    val shopImg = withContext(Dispatchers.IO) {
                        com.example.util.ImageStorageHelper.toUniversalImageUrl(getApplication(), parentShop.imageUrl)
                    }
                    val targetShopUid = if (role == UserRole.SHOP_OWNER) firebaseUid else parentShop.ownerUid.ifBlank { firebaseUid }
                    val shopSync = repository.updateShop(parentShop.copy(ownerUid = targetShopUid, imageUrl = shopImg), user)
                    if (shopSync.cloudSynced) {
                        syncedShopIdsInSession.add(parentShop.id)
                    } else {
                        parentShopError = shopSync
                    }
                }

                if (productToSave.id == 0L) {
                    var result: CloudSaveResult<Long> = repository.addProduct(productToSave)
                    if (!result.cloudSynced && result.requiresAuth && parentShop != null) {
                        val shopImg = withContext(Dispatchers.IO) {
                            com.example.util.ImageStorageHelper.toUniversalImageUrl(getApplication(), parentShop.imageUrl)
                        }
                        val targetShopUid = if (role == UserRole.SHOP_OWNER) firebaseUid else parentShop.ownerUid.ifBlank { firebaseUid }
                        val shopSync = repository.updateShop(
                            parentShop.copy(ownerUid = targetShopUid, imageUrl = shopImg),
                            user
                        )
                        if (shopSync.cloudSynced) {
                            syncedShopIdsInSession.add(parentShop.id)
                            result = repository.updateProduct(productToSave.copy(id = result.localId))
                                .let { upd -> CloudSaveResult(result.localId, upd.cloudSynced, upd.errorMessage, upd.requiresAuth) }
                        } else if (parentShopError == null) {
                            parentShopError = shopSync
                        }
                    }
                    if (result.cloudSynced) {
                        syncedShopIdsInSession.add(productToSave.shopId)
                        _productSyncStatus.value = ProductSyncStatus.Synced("Saved & synced '${productToSave.name}' to Cloud Firestore ☁️")
                        _userMessage.value = "Product added & synced to Cloud Firestore! ☁️"
                    } else {
                        val errMsg = parentShopError?.errorMessage ?: result.errorMessage ?: "Cloud Firestore sync failed."
                        val reqAuth = parentShopError?.requiresAuth == true || result.requiresAuth
                        android.util.Log.e(
                            "MainViewModel",
                            "addProduct Firestore sync failed (localId=${result.localId}, shopId=${productToSave.shopId}, uid=$firebaseUid, requiresAuth=$reqAuth): $errMsg"
                        )
                        _productSyncStatus.value = ProductSyncStatus.Error(errMsg, requiresAuth = reqAuth)
                        _userMessage.value = "Saved locally only — cloud sync failed: $errMsg"
                    }
                } else {
                    var result: CloudSaveResult<Unit> = repository.updateProduct(productToSave)
                    if (!result.cloudSynced && result.requiresAuth && parentShop != null) {
                        val shopImg = withContext(Dispatchers.IO) {
                            com.example.util.ImageStorageHelper.toUniversalImageUrl(getApplication(), parentShop.imageUrl)
                        }
                        val targetShopUid = if (role == UserRole.SHOP_OWNER) firebaseUid else parentShop.ownerUid.ifBlank { firebaseUid }
                        val shopSync = repository.updateShop(
                            parentShop.copy(ownerUid = targetShopUid, imageUrl = shopImg),
                            user
                        )
                        if (shopSync.cloudSynced) {
                            syncedShopIdsInSession.add(parentShop.id)
                            result = repository.updateProduct(productToSave)
                        } else if (parentShopError == null) {
                            parentShopError = shopSync
                        }
                    }
                    if (result.cloudSynced) {
                        syncedShopIdsInSession.add(productToSave.shopId)
                        _productSyncStatus.value = ProductSyncStatus.Synced("Updated & synced '${productToSave.name}' to Cloud Firestore ☁️")
                        _userMessage.value = "Product updated & synced to Cloud Firestore! ☁️"
                    } else {
                        val errMsg = parentShopError?.errorMessage ?: result.errorMessage ?: "Cloud Firestore sync failed."
                        val reqAuth = parentShopError?.requiresAuth == true || result.requiresAuth
                        android.util.Log.e(
                            "MainViewModel",
                            "updateProduct Firestore sync failed (productId=${productToSave.id}, shopId=${productToSave.shopId}, uid=$firebaseUid, requiresAuth=$reqAuth): $errMsg"
                        )
                        _productSyncStatus.value = ProductSyncStatus.Error(errMsg, requiresAuth = reqAuth)
                        _userMessage.value = "Updated locally only — cloud sync failed: $errMsg"
                    }
                }
            } finally {
                _isProductSyncing.value = false
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        val role = currentRole.value
        val user = currentUser.value

        if (role == UserRole.CUSTOMER) {
            _userMessage.value = "Access Denied: Customers cannot delete products."
            return
        }

        viewModelScope.launch {
            _isProductSyncing.value = true
            _productSyncStatus.value = ProductSyncStatus.Syncing
            try {
                if (role == UserRole.SHOP_OWNER) {
                    val myShop = repository.getAllShopsSync().find {
                        it.ownerId == user?.id ||
                        (user?.shopId != null && it.id == user.shopId) ||
                        (!user?.phone.isNullOrBlank() && it.phone == user?.phone)
                    }
                    if (myShop == null || product.shopId != myShop.id) {
                        _productSyncStatus.value = ProductSyncStatus.Error("Access Denied: You can only delete products from your own shop.")
                        _userMessage.value = "Access Denied: You can only delete products from your own shop."
                        return@launch
                    }
                }
                val result = repository.deleteProduct(product)
                if (result.cloudSynced) {
                    _productSyncStatus.value = ProductSyncStatus.Synced("Removed '${product.name}' from Cloud Firestore ☁️")
                    _userMessage.value = "Product removed & synced with Cloud Firestore."
                } else {
                    val errMsg = result.errorMessage ?: "Could not delete product from Cloud Firestore."
                    _productSyncStatus.value = ProductSyncStatus.Error(errMsg, requiresAuth = result.requiresAuth)
                    _userMessage.value = "Removed locally — cloud sync error: $errMsg"
                }
            } finally {
                _isProductSyncing.value = false
            }
        }
    }

    fun toggleProductStock(product: ProductEntity) {
        val role = currentRole.value
        val user = currentUser.value

        if (role == UserRole.CUSTOMER) {
            _userMessage.value = "Access Denied: Customers cannot modify products."
            return
        }

        viewModelScope.launch {
            _isProductSyncing.value = true
            try {
                val shops = repository.getAllShopsSync()
                if (role == UserRole.SHOP_OWNER) {
                    val myShop = shops.find {
                        it.ownerId == user?.id ||
                        (user?.shopId != null && it.id == user.shopId) ||
                        (!user?.phone.isNullOrBlank() && it.phone == user?.phone)
                    }
                    if (myShop == null || product.shopId != myShop.id) {
                        _userMessage.value = "Access Denied: You can only update stock for your own shop."
                        return@launch
                    }
                }
                val firebaseUid = FirebasePhoneAuthHelper.currentUid()
                if (firebaseUid.isNullOrBlank()) {
                    val msg = "Please re-login with Mobile OTP so stock updates sync to Cloud Firestore."
                    _productSyncStatus.value = ProductSyncStatus.Error(msg, requiresAuth = true)
                    _userMessage.value = msg
                    return@launch
                }
                val resolvedUid = if (role == UserRole.SHOP_OWNER) firebaseUid else product.ownerUid.ifBlank { firebaseUid }
                val parentShop = shops.find { it.id == product.shopId }
                var parentShopError: CloudSaveResult<Unit>? = null
                if (parentShop != null && !syncedShopIdsInSession.contains(parentShop.id)) {
                    val shopImg = withContext(Dispatchers.IO) {
                        com.example.util.ImageStorageHelper.toUniversalImageUrl(getApplication(), parentShop.imageUrl)
                    }
                    val targetShopUid = if (role == UserRole.SHOP_OWNER) firebaseUid else parentShop.ownerUid.ifBlank { firebaseUid }
                    val shopSync = repository.updateShop(parentShop.copy(ownerUid = targetShopUid, imageUrl = shopImg), user)
                    if (shopSync.cloudSynced) {
                        syncedShopIdsInSession.add(parentShop.id)
                    } else {
                        parentShopError = shopSync
                    }
                }
                val updated = product.copy(isAvailable = !product.isAvailable, ownerUid = resolvedUid)
                val result = repository.updateProduct(updated)
                if (result.cloudSynced) {
                    syncedShopIdsInSession.add(product.shopId)
                    _productSyncStatus.value = ProductSyncStatus.Synced("Stock status synced ☁️")
                } else {
                    val errMsg = parentShopError?.errorMessage ?: result.errorMessage ?: "Stock update failed to sync to Cloud Firestore."
                    val reqAuth = parentShopError?.requiresAuth == true || result.requiresAuth
                    android.util.Log.e(
                        "MainViewModel",
                        "toggleProductStock Firestore sync failed (productId=${product.id}, shopId=${product.shopId}, uid=$firebaseUid): $errMsg"
                    )
                    _productSyncStatus.value = ProductSyncStatus.Error(errMsg, requiresAuth = reqAuth)
                    _userMessage.value = "Stock updated locally only — cloud sync failed: $errMsg"
                }
            } finally {
                _isProductSyncing.value = false
            }
        }
    }

    fun createShopPromotion(
        shop: ShopEntity,
        type: PromotionType,
        title: String,
        description: String,
        targetProductId: Long? = null,
        targetProductName: String? = null,
        location: String = "Sinapali Town",
        durationDays: Int = 7,
        startDate: String = "",
        endDate: String = "",
        dailyRate: Double,
        totalPrice: Double,
        onSuccess: () -> Unit = {}
    ) {
        val role = currentRole.value
        val user = currentUser.value

        if (role == UserRole.SHOP_OWNER) {
            val myShop = allShops.value.find { it.ownerId == user?.id || (user?.shopId != null && it.id == user.shopId) }
            if (myShop == null || shop.id != myShop.id) {
                _userMessage.value = "Access Denied: You can only request promotions for your own shop."
                return
            }
        }

        viewModelScope.launch {
            val promo = PromotionEntity(
                shopId = shop.id,
                shopName = shop.name,
                title = title.ifBlank { "${shop.name} Promotion" },
                description = description,
                type = type,
                status = PromotionStatus.PENDING_APPROVAL,
                targetProductId = targetProductId,
                targetProductName = targetProductName,
                location = location,
                durationDays = durationDays,
                startDate = startDate,
                endDate = endDate,
                dailyRate = dailyRate,
                totalPrice = totalPrice,
                paymentStatus = "Pending Payment / Post-Approval",
                submittedAt = System.currentTimeMillis()
            )
            repository.createPromotion(promo)

            // Notify Admin Harishchandra Sagar
            val adminUser = SeedData.adminUser
            repository.insertNotification(
                NotificationEntity(
                    userId = adminUser.id,
                    title = "New Promotion Request 📢",
                    message = "Shop '${shop.name}' submitted a ${type.name.replace('_', ' ')}: '$title' for ₹${totalPrice.toInt()}. Pending your approval.",
                    type = NotificationType.SYSTEM
                )
            )

            _userMessage.value = "Promotion submitted! Status: Pending Admin Approval."
            onSuccess()
        }
    }

    fun submitPromotionRequest(shop: ShopEntity, type: PromotionType, targetId: Long, targetName: String, notes: String) {
        val config = _promotionPricingConfig.value
        val dailyRate = config.getDailyRate(type)
        createShopPromotion(
            shop = shop,
            type = type,
            title = "$targetName Promotion",
            description = notes,
            targetProductId = if (type == PromotionType.PRODUCT_AD || type == PromotionType.FEATURED_PRODUCT) targetId else null,
            targetProductName = if (type == PromotionType.PRODUCT_AD || type == PromotionType.FEATURED_PRODUCT) targetName else null,
            location = "Sinapali Town",
            durationDays = 7,
            startDate = "2026-09-29",
            endDate = "2026-10-06",
            dailyRate = dailyRate,
            totalPrice = dailyRate * 7
        )
    }

    fun updatePromotionDetailsByAdmin(
        id: Long,
        status: PromotionStatus,
        price: Double,
        duration: Int,
        startDate: String,
        endDate: String,
        location: String,
        notes: String,
        shopOwnerUserId: Long? = null,
        promoTitle: String = ""
    ) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin (Harishchandra Sagar) can review promotions."
            return
        }
        viewModelScope.launch {
            repository.updatePromotionDetailsByAdmin(
                id = id,
                status = status,
                price = price,
                duration = duration,
                startDate = startDate,
                endDate = endDate,
                location = location,
                notes = notes
            )
            // Notify Shop Owner of Admin decision
            if (shopOwnerUserId != null && shopOwnerUserId > 0) {
                val notifTitle = if (status == PromotionStatus.APPROVED || status == PromotionStatus.ACTIVE) {
                    "Promotion Approved! 🎉"
                } else if (status == PromotionStatus.REJECTED) {
                    "Promotion Update: Rejected ⚠️"
                } else {
                    "Promotion Details Updated 📝"
                }
                val notifMsg = if (status == PromotionStatus.APPROVED) {
                    "Your promotion '$promoTitle' was approved by Admin Harishchandra Sagar for ₹${price.toInt()} at $location. It is now visible to customers."
                } else if (status == PromotionStatus.REJECTED) {
                    "Your promotion '$promoTitle' was not approved. Note: $notes"
                } else {
                    "Admin updated promotion '$promoTitle': ₹${price.toInt()}, $duration days at $location."
                }
                repository.insertNotification(
                    NotificationEntity(
                        userId = shopOwnerUserId,
                        title = notifTitle,
                        message = notifMsg,
                        type = NotificationType.SYSTEM
                    )
                )
            }
            _userMessage.value = "Promotion ${status.name.lowercase().replace('_', ' ')} successfully."
        }
    }

    fun updatePromotionPaymentStatus(id: Long, paymentStatus: String) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin can update payment status."
            return
        }
        viewModelScope.launch {
            repository.updatePromotionPaymentStatus(id, paymentStatus)
            _userMessage.value = "Payment status updated to $paymentStatus"
        }
    }

    fun updatePromotionPricingConfig(newConfig: PromotionPricingConfig) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin can configure promotion prices."
            return
        }
        _promotionPricingConfig.value = newConfig
        _userMessage.value = "Admin promotion pricing rates updated."
    }

    fun updatePromotionStatus(promotionId: Long, status: PromotionStatus) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin can approve promotions."
            return
        }
        viewModelScope.launch {
            repository.updatePromotionStatus(promotionId, status)
            _userMessage.value = "Promotion status updated to ${status.name}"
        }
    }

    fun updateShopApproval(shop: ShopEntity, isApproved: Boolean) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin can approve or restrict shops."
            return
        }
        viewModelScope.launch {
            repository.updateShop(shop.copy(isApproved = isApproved))
            _userMessage.value = "Shop ${if (isApproved) "approved" else "restricted"}."
        }
    }

    fun updateProductApproval(product: ProductEntity, isApproved: Boolean) {
        if (currentRole.value != UserRole.ADMIN) {
            _userMessage.value = "Access Denied: Only Admin can approve or restrict products."
            return
        }
        viewModelScope.launch {
            repository.updateProduct(product.copy(isApproved = isApproved))
            _userMessage.value = "Product ${if (isApproved) "approved" else "restricted"}."
        }
    }

    // Role Authentication methods
    // Shop Owner Authentication & Registration with real Firebase Phone OTP
    private val _shopOwnerOtpMobile = MutableStateFlow("")
    val shopOwnerOtpMobile: StateFlow<String> = _shopOwnerOtpMobile.asStateFlow()

    private val _shopOwnerOtpCode = MutableStateFlow<String?>(null)
    val shopOwnerOtpCode: StateFlow<String?> = _shopOwnerOtpCode.asStateFlow()

    private val _isShopOwnerOtpSent = MutableStateFlow(false)
    val isShopOwnerOtpSent: StateFlow<Boolean> = _isShopOwnerOtpSent.asStateFlow()

    private val _shopOwnerAuthError = MutableStateFlow<String?>(null)
    val shopOwnerAuthError: StateFlow<String?> = _shopOwnerAuthError.asStateFlow()

    private val _identifiedShopOwner = MutableStateFlow<UserEntity?>(null)
    val identifiedShopOwner: StateFlow<UserEntity?> = _identifiedShopOwner.asStateFlow()

    private val _isNewShopOwnerRegistration = MutableStateFlow(false)
    val isNewShopOwnerRegistration: StateFlow<Boolean> = _isNewShopOwnerRegistration.asStateFlow()

    private val _isRegisteringShop = MutableStateFlow(false)
    val isRegisteringShop: StateFlow<Boolean> = _isRegisteringShop.asStateFlow()

    private var shopOwnerVerificationId: String? = null
    private var shopOwnerResendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendShopOwnerOtp(mobile: String, activity: Activity?, onAutoSignIn: ((Boolean, Boolean, String) -> Unit)? = null): Boolean {
        val trimmed = FirebasePhoneAuthHelper.extractTenDigitPhone(mobile)
        if (trimmed.length < 10) {
            _shopOwnerAuthError.value = "Please enter a valid 10-digit mobile number."
            return false
        }
        if (activity == null) {
            _shopOwnerAuthError.value = "Unable to start verification on this screen. Please reopen the login screen."
            return false
        }

        val allUsersList = allUsers.value
        val registeredOwner = allUsersList.find {
            it.role == UserRole.SHOP_OWNER &&
                (FirebasePhoneAuthHelper.extractTenDigitPhone(it.phone) == trimmed ||
                    (it.email.isNotBlank() && it.email.equals(mobile.trim(), ignoreCase = true)))
        } ?: SeedData.shopOwners.find { FirebasePhoneAuthHelper.extractTenDigitPhone(it.phone) == trimmed }

        _shopOwnerOtpMobile.value = trimmed
        _identifiedShopOwner.value = registeredOwner
        _isNewShopOwnerRegistration.value = (registeredOwner == null)
        _shopOwnerAuthError.value = null
        _isAuthLoading.value = true

        FirebasePhoneAuthHelper.sendOtp(
            activity = activity,
            rawPhone = mobile,
            resendToken = shopOwnerResendToken,
            onCodeSent = { verificationId, token ->
                shopOwnerVerificationId = verificationId
                shopOwnerResendToken = token
                _isAuthLoading.value = false
                _isShopOwnerOtpSent.value = true
                _shopOwnerAuthError.value = null
                _userMessage.value = "OTP sent via Firebase SMS to +91$trimmed"
            },
            onAutoVerified = { credential ->
                viewModelScope.launch {
                    val result = FirebasePhoneAuthHelper.signInWithPhoneCredential(credential)
                    handleShopOwnerSignInResult(result) { success, needsReg, msg ->
                        onAutoSignIn?.invoke(success, needsReg, msg)
                    }
                }
            },
            onFailed = { message ->
                _isAuthLoading.value = false
                _shopOwnerAuthError.value = message
            }
        )
        return true
    }

    fun verifyShopOwnerOtp(enteredCode: String, onResult: (Boolean, Boolean, String) -> Unit) {
        val verId = shopOwnerVerificationId
        val trimmed = enteredCode.trim()
        if (verId.isNullOrBlank()) {
            _shopOwnerAuthError.value = "Verification session expired. Please tap Resend OTP."
            onResult(false, false, "Session expired")
            return
        }
        if (trimmed.length < 6) {
            _shopOwnerAuthError.value = "Please enter the complete 6-digit SMS OTP."
            onResult(false, false, "Invalid OTP length")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = FirebasePhoneAuthHelper.signInWithOtpCode(verId, trimmed)
            handleShopOwnerSignInResult(result, onResult)
        }
    }

    private suspend fun handleShopOwnerSignInResult(
        result: PhoneAuthSignInResult,
        onResult: (Boolean, Boolean, String) -> Unit
    ) {
        when (result) {
            is PhoneAuthSignInResult.Failure -> {
                _isAuthLoading.value = false
                _shopOwnerAuthError.value = result.message
                onResult(false, false, result.message)
            }
            is PhoneAuthSignInResult.Success -> {
                val firebaseUser: FirebaseUser = result.user
                val uid = firebaseUser.uid
                _currentFirebaseUid.value = uid
                val normalizedPhone = FirebasePhoneAuthHelper.extractTenDigitPhone(
                    firebaseUser.phoneNumber ?: _shopOwnerOtpMobile.value
                )

                // Check Firestore & Room for existing shop owner and shop
                val remoteUser = FirestoreSyncService.fetchUserByUidFromFirestore(uid)
                val remoteShop = FirestoreSyncService.fetchShopByOwnerUidFromFirestore(uid)
                val localOwner = _identifiedShopOwner.value
                    ?: repository.getUserByPhone(normalizedPhone)?.takeIf { it.role == UserRole.SHOP_OWNER }
                    ?: allUsers.value.find {
                        it.role == UserRole.SHOP_OWNER &&
                            FirebasePhoneAuthHelper.extractTenDigitPhone(it.phone) == normalizedPhone
                    }
                    ?: SeedData.shopOwners.find {
                        FirebasePhoneAuthHelper.extractTenDigitPhone(it.phone) == normalizedPhone
                    }

                if (remoteShop != null) {
                    val shopWithUid = remoteShop.copy(ownerUid = uid)
                    repository.insertOrUpdateShopFromCloud(shopWithUid)
                    val baseOwner = localOwner ?: remoteUser ?: UserEntity(
                        name = "Shop Owner",
                        phone = normalizedPhone,
                        role = UserRole.SHOP_OWNER,
                        shopId = shopWithUid.id,
                        isVerified = true,
                        firebaseUid = uid
                    )
                    val syncedOwner = baseOwner.copy(
                        role = UserRole.SHOP_OWNER,
                        shopId = shopWithUid.id,
                        isVerified = true,
                        firebaseUid = uid
                    )
                    val ownerId = if (syncedOwner.id > 0L) {
                        repository.updateUser(syncedOwner); syncedOwner.id
                    } else {
                        repository.insertUser(syncedOwner)
                    }
                    val finalOwner = syncedOwner.copy(id = ownerId)
                    FirestoreSyncService.pushUserToFirestoreWithResult(finalOwner)

                    authManager.persistShopOwnerSession(finalOwner)
                    authManager.switchUser(finalOwner)
                    authManager.switchRole(UserRole.SHOP_OWNER)
                    attachRealtimeFirestoreListeners()
                    syncShopsAndProductsFromCloud()
                    resetShopOwnerOtpState()
                    _isAuthLoading.value = false
                    screenBackStack.removeAll { it is Screen.ShopOwnerAuth || it is Screen.CustomerAuth }
                    _currentScreen.value = Screen.ShopOwnerDashboard
                    _userMessage.value = "Welcome back, ${finalOwner.name}! Accessing ${shopWithUid.name}."
                    onResult(true, false, "Login successful")
                } else if (localOwner != null) {
                    val syncedOwner = localOwner.copy(
                        role = UserRole.SHOP_OWNER,
                        isVerified = true,
                        firebaseUid = uid
                    )
                    val ownerId = if (syncedOwner.id > 0L) {
                        repository.updateUser(syncedOwner); syncedOwner.id
                    } else {
                        repository.insertUser(syncedOwner)
                    }
                    val finalOwner = syncedOwner.copy(id = ownerId)
                    val myShop = repository.getAllShopsSync().find {
                        it.ownerId == finalOwner.id ||
                            (finalOwner.shopId != null && it.id == finalOwner.shopId) ||
                            (normalizedPhone.isNotBlank() && FirebasePhoneAuthHelper.extractTenDigitPhone(it.phone) == normalizedPhone)
                    }
                    if (myShop != null) {
                        val shopSync = repository.updateShop(myShop.copy(ownerUid = uid), finalOwner)
                        if (shopSync.cloudSynced) {
                            syncedShopIdsInSession.add(myShop.id)
                            _productSyncStatus.value = ProductSyncStatus.Synced("Shop '${myShop.name}' linked & synced to Cloud Firestore ☁️")
                        } else {
                            _productSyncStatus.value = ProductSyncStatus.Error(
                                shopSync.errorMessage ?: "Could not sync shop to Cloud Firestore.",
                                requiresAuth = shopSync.requiresAuth
                            )
                        }
                    }
                    FirestoreSyncService.pushUserToFirestoreWithResult(finalOwner)

                    authManager.persistShopOwnerSession(finalOwner)
                    authManager.switchUser(finalOwner)
                    authManager.switchRole(UserRole.SHOP_OWNER)
                    attachRealtimeFirestoreListeners()
                    syncShopsAndProductsFromCloud()
                    resetShopOwnerOtpState()
                    _isAuthLoading.value = false
                    screenBackStack.removeAll { it is Screen.ShopOwnerAuth || it is Screen.CustomerAuth }
                    _currentScreen.value = Screen.ShopOwnerDashboard
                    _userMessage.value = "Welcome back, ${finalOwner.name}! Accessing ${myShop?.name ?: "Shop Dashboard"}."
                    onResult(true, false, "Login successful")
                } else {
                    _isAuthLoading.value = false
                    _isNewShopOwnerRegistration.value = true
                    _shopOwnerAuthError.value = null
                    attachRealtimeFirestoreListeners()
                    onResult(true, true, "OTP verified, proceed to shop registration")
                }
            }
        }
    }

    fun registerNewShopOwnerAndShop(
        ownerName: String,
        shopName: String,
        category: String,
        address: String,
        phone: String,
        imageUrl: String,
        upiId: String = "sinapalimart@upi",
        description: String = "",
        deliveryAvailable: Boolean = true,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (ownerName.isBlank() || shopName.isBlank() || phone.isBlank()) {
            _shopOwnerAuthError.value = "Owner Name, Shop Name, and Mobile Number are required."
            onComplete(false, "Required fields missing")
            return
        }

        val firebaseUid = FirebasePhoneAuthHelper.currentUid()
        if (firebaseUid.isNullOrBlank()) {
            val msg = "Mobile OTP verification session expired. Please verify your mobile number with OTP again."
            _shopOwnerAuthError.value = msg
            onComplete(false, msg)
            return
        }

        viewModelScope.launch {
            _isRegisteringShop.value = true
            try {
                // 1. Create Shop Owner user linked with Firebase Auth UID
                val newOwner = UserEntity(
                    id = 0L,
                    name = ownerName.trim(),
                    phone = phone.trim(),
                    email = "${phone.trim()}@sinapalimart.local",
                    role = UserRole.SHOP_OWNER,
                    isVerified = true,
                    deliveryAddress = address.trim().ifBlank { "Sinapali Main Market" },
                    district = "Nuapada",
                    block = "Sinapali",
                    gp = "Sinapali",
                    village = "Sinapali",
                    firebaseUid = firebaseUid
                )
                val newOwnerId = repository.insertUser(newOwner)
                val persistedOwner = newOwner.copy(id = newOwnerId)

                // 2. Create ShopEntity linked to owner and Firebase Auth UID
                val universalShopImage = withContext(Dispatchers.IO) {
                    com.example.util.ImageStorageHelper.toUniversalImageUrl(
                        getApplication(),
                        imageUrl.trim().ifBlank { "hero_local_mart" }
                    )
                }
                val newShop = ShopEntity(
                    id = 0L,
                    ownerId = newOwnerId,
                    name = shopName.trim(),
                    category = category.trim().ifBlank { "Daily Essentials" },
                    address = address.trim().ifBlank { "Sinapali Main Market" },
                    phone = phone.trim(),
                    upiId = upiId.trim().ifBlank { "${phone.trim()}@upi" },
                    imageUrl = universalShopImage,
                    deliveryAvailable = deliveryAvailable,
                    isOpen = true,
                    isApproved = true,
                    rating = 5.0,
                    reviewCount = 1,
                    minOrderAmount = 40.0,
                    estimatedDeliveryMins = 25,
                    ownerUid = firebaseUid
                )
                val shopCreateResult = repository.createShop(newShop, persistedOwner)
                val persistedShop = newShop.copy(id = shopCreateResult.localId)

                // Update owner's shopId
                val updatedOwner = persistedOwner.copy(shopId = shopCreateResult.localId)
                repository.updateUser(updatedOwner)

                // 3. Push owner user profile directly to Cloud Firestore /users/{uid}
                val userSyncResult = FirestoreSyncService.pushUserToFirestoreWithResult(updatedOwner)

                // 4. Update honest cloud sync status
                if (shopCreateResult.cloudSynced) {
                    syncedShopIdsInSession.add(persistedShop.id)
                }
                if (shopCreateResult.cloudSynced && userSyncResult is FirestoreSyncService.CloudSyncResult.Success) {
                    _productSyncStatus.value = ProductSyncStatus.Synced("Shop '${persistedShop.name}' synced to Cloud Firestore ☁️")
                } else {
                    val syncMsg = shopCreateResult.errorMessage
                        ?: (userSyncResult as? FirestoreSyncService.CloudSyncResult.Failure)?.userMessage
                        ?: "Shop saved locally, but Cloud Firestore sync failed."
                    _productSyncStatus.value = ProductSyncStatus.Error(syncMsg, requiresAuth = shopCreateResult.requiresAuth)
                }

                // 5. Switch session to the newly created Shop Owner and persist session
                authManager.persistShopOwnerSession(updatedOwner)
                authManager.switchUser(updatedOwner)
                authManager.switchRole(UserRole.SHOP_OWNER)
                attachRealtimeFirestoreListeners()
                resetShopOwnerOtpState()

                screenBackStack.removeAll { it is Screen.ShopOwnerAuth || it is Screen.CustomerAuth }
                _currentScreen.value = Screen.ShopOwnerDashboard
                _userMessage.value = if (shopCreateResult.cloudSynced) {
                    "Shop '${persistedShop.name}' registered & synced to Cloud Firestore! ☁️"
                } else {
                    "Shop saved locally. Cloud sync pending: ${shopCreateResult.errorMessage}"
                }
                onComplete(true, "Shop created successfully")
            } catch (e: Throwable) {
                _shopOwnerAuthError.value = "Failed to register shop: ${e.message}"
                onComplete(false, e.message ?: "Registration error")
            } finally {
                _isRegisteringShop.value = false
            }
        }
    }

    fun resetShopOwnerOtpState() {
        _shopOwnerOtpCode.value = null
        _shopOwnerOtpMobile.value = ""
        _isShopOwnerOtpSent.value = false
        _shopOwnerAuthError.value = null
        _identifiedShopOwner.value = null
        _isNewShopOwnerRegistration.value = false
        _isRegisteringShop.value = false
        _isAuthLoading.value = false
        shopOwnerVerificationId = null
    }

    fun loginShopOwnerWithCredentials(phoneOrEmail: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val owner = authManager.loginAsShopOwner(phoneOrEmail)
            if (owner != null) {
                val myShop = allShops.value.find { it.ownerId == owner.id || (owner.shopId != null && it.id == owner.shopId) }
                screenBackStack.clear()
                _userMessage.value = "Welcome, ${owner.name}! Accessing ${myShop?.name ?: "your shop"}."
                navigateTo(Screen.ShopOwnerDashboard)
                onResult(true, "Authenticated as ${owner.name}")
            } else {
                onResult(false, "Access Denied: No Shop Owner account found with '$phoneOrEmail'. Customers cannot access the Shop Owner dashboard.")
            }
        }
    }

    fun loginAdminWithPin(pin: String, onResult: (Boolean, String) -> Unit) {
        val success = authManager.loginAsAdminWithPin(pin)
        if (success) {
            _userMessage.value = "Welcome, Harishchandra Sagar! Admin Panel unlocked."
            navigateTo(Screen.AdminDashboard)
            onResult(true, "Admin Panel Unlocked")
        } else {
            onResult(false, "Access Denied: Invalid Admin Security PIN. Admin Panel is restricted.")
        }
    }

    fun switchToCustomerView() {
        authManager.switchToCustomerView()
        _currentScreen.value = Screen.CustomerHome
        _userMessage.value = "Switched to Customer Store"
    }

    fun fileComplaint(shopId: Long, shopName: String, subject: String, description: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val complaint = ComplaintEntity(
                orderId = 0,
                userId = user.id,
                shopId = shopId,
                issueType = subject,
                description = description,
                status = ComplaintStatus.OPEN
            )
            repository.fileComplaint(complaint)
            _userMessage.value = "Complaint submitted. Admin will review promptly."
        }
    }

    fun resolveComplaint(complaint: ComplaintEntity) {
        viewModelScope.launch {
            repository.updateComplaint(complaint.copy(status = ComplaintStatus.RESOLVED))
            _userMessage.value = "Complaint resolved."
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // OTP and Customer Registration state (real Firebase Phone OTP)
    private val _otpTarget = MutableStateFlow("")
    val otpTarget: StateFlow<String> = _otpTarget.asStateFlow()

    private val _otpCode = MutableStateFlow<String?>(null)
    val otpCode: StateFlow<String?> = _otpCode.asStateFlow()

    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent.asStateFlow()

    private val _isOtpVerified = MutableStateFlow(false)
    val isOtpVerified: StateFlow<Boolean> = _isOtpVerified.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private var customerVerificationId: String? = null
    private var customerResendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendCustomerOtp(mobile: String, activity: Activity?, onAutoVerifiedUser: ((UserEntity?) -> Unit)? = null) {
        val digits = FirebasePhoneAuthHelper.extractTenDigitPhone(mobile)
        if (digits.length < 10) {
            _authErrorMessage.value = "Please enter a valid 10-digit mobile number."
            return
        }
        if (activity == null) {
            _authErrorMessage.value = "Unable to start OTP verification on this screen."
            return
        }

        _otpTarget.value = digits
        _isOtpVerified.value = false
        _authErrorMessage.value = null
        _isAuthLoading.value = true

        FirebasePhoneAuthHelper.sendOtp(
            activity = activity,
            rawPhone = mobile,
            resendToken = customerResendToken,
            onCodeSent = { verificationId, token ->
                customerVerificationId = verificationId
                customerResendToken = token
                _isAuthLoading.value = false
                _isOtpSent.value = true
                _authErrorMessage.value = null
                _userMessage.value = "SMS OTP sent via Firebase to +91$digits"
            },
            onAutoVerified = { credential ->
                viewModelScope.launch {
                    val result = FirebasePhoneAuthHelper.signInWithPhoneCredential(credential)
                    handleCustomerSignInResult(result) { success, existingCustomer, _ ->
                        if (success) {
                            onAutoVerifiedUser?.invoke(existingCustomer)
                        }
                    }
                }
            },
            onFailed = { message ->
                _isAuthLoading.value = false
                _authErrorMessage.value = message
            }
        )
    }

    fun verifyCustomerOtp(
        enteredCode: String,
        onResult: (Boolean, UserEntity?, String?) -> Unit
    ) {
        val verId = customerVerificationId
        val trimmed = enteredCode.trim()
        if (verId.isNullOrBlank()) {
            _authErrorMessage.value = "Verification session expired. Please tap Resend OTP."
            onResult(false, null, "Session expired")
            return
        }
        if (trimmed.length < 6) {
            _authErrorMessage.value = "Please enter the complete 6-digit SMS OTP."
            onResult(false, null, "Invalid OTP length")
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = FirebasePhoneAuthHelper.signInWithOtpCode(verId, trimmed)
            handleCustomerSignInResult(result, onResult)
        }
    }

    private suspend fun handleCustomerSignInResult(
        result: PhoneAuthSignInResult,
        onResult: (Boolean, UserEntity?, String?) -> Unit
    ) {
        when (result) {
            is PhoneAuthSignInResult.Failure -> {
                _isAuthLoading.value = false
                _authErrorMessage.value = result.message
                onResult(false, null, result.message)
            }
            is PhoneAuthSignInResult.Success -> {
                val firebaseUser = result.user
                val uid = firebaseUser.uid
                _currentFirebaseUid.value = uid
                val normalizedPhone = FirebasePhoneAuthHelper.extractTenDigitPhone(
                    firebaseUser.phoneNumber ?: _otpTarget.value
                )
                val remoteUser = FirestoreSyncService.fetchUserByUidFromFirestore(uid)
                val localUser = repository.getUserByPhoneOrEmail(normalizedPhone)?.takeIf { it.role == UserRole.CUSTOMER }
                val existing = (remoteUser ?: localUser)?.copy(
                    firebaseUid = uid,
                    isVerified = true,
                    phone = normalizedPhone.ifBlank { (remoteUser ?: localUser)?.phone ?: "" }
                )
                if (existing != null && existing.id == 0L && localUser != null) {
                    // keep Room ID
                    val merged = existing.copy(id = localUser.id)
                    repository.updateUser(merged)
                }
                _isOtpVerified.value = true
                _authErrorMessage.value = null
                _isAuthLoading.value = false
                attachRealtimeFirestoreListeners()
                onResult(true, existing, null)
            }
        }
    }

    fun resetCustomerOtpState() {
        _otpCode.value = null
        _otpTarget.value = ""
        _isOtpSent.value = false
        _isOtpVerified.value = false
        _authErrorMessage.value = null
        _isAuthLoading.value = false
        customerVerificationId = null
    }

    suspend fun checkCustomerExists(identifier: String): UserEntity? {
        return authManager.findCustomerByPhoneOrEmail(identifier)
    }

    fun loginCustomerWithVerifiedOtp(user: UserEntity, onSuccess: () -> Unit) {
        val uid = FirebasePhoneAuthHelper.currentUid() ?: user.firebaseUid
        val verified = user.copy(isVerified = true, firebaseUid = uid)
        viewModelScope.launch {
            val savedId = if (verified.id > 0L) {
                repository.updateUser(verified)
                verified.id
            } else {
                repository.insertUser(verified)
            }
            val finalUser = verified.copy(id = savedId)
            authManager.loginExistingCustomer(finalUser)
            FirestoreSyncService.pushUserToFirestoreWithResult(finalUser)
            resetCustomerOtpState()
            screenBackStack.removeAll { it is Screen.CustomerAuth }
            _userMessage.value = "Welcome back, ${finalUser.name}!"
            onSuccess()
        }
    }

    fun registerCustomerWithOdishaAddress(
        name: String,
        phone: String,
        email: String,
        district: String,
        block: String,
        gp: String,
        village: String,
        fullAddress: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val uid = FirebasePhoneAuthHelper.currentUid() ?: ""
            val user = authManager.registerCustomerWithOdishaAddress(
                name = name,
                phone = phone,
                email = email,
                district = district,
                block = block,
                gp = gp,
                village = village,
                fullAddress = fullAddress,
                firebaseUid = uid
            )
            FirestoreSyncService.pushUserToFirestoreWithResult(user)
            resetCustomerOtpState()
            screenBackStack.removeAll { it is Screen.CustomerAuth }
            _userMessage.value = "Registration complete! Odisha address saved."
            onSuccess()
        }
    }

    fun updateCustomerAddress(
        district: String,
        block: String,
        gp: String,
        village: String,
        fullAddress: String
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            authManager.updateCustomerAddress(
                userId = user.id,
                district = district,
                block = block,
                gp = gp,
                village = village,
                fullAddress = fullAddress
            )
            _userMessage.value = "Delivery address updated successfully."
        }
    }

    fun setLanguage(lang: com.example.domain.model.AppLanguage) {
        _appLanguage.value = lang
    }

    fun toggleLanguage() {
        _appLanguage.value = if (_appLanguage.value == com.example.domain.model.AppLanguage.ENGLISH) {
            com.example.domain.model.AppLanguage.ODIA
        } else {
            com.example.domain.model.AppLanguage.ENGLISH
        }
    }

    fun updateProfilePhoto(photoUri: String?) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            authManager.updateProfilePhoto(user.id, photoUri)
            _userMessage.value = if (photoUri != null) "Profile photo updated! 📸" else "Profile photo removed."
        }
    }

    fun updateShopPhoto(shopId: Long, photoUri: String) {
        viewModelScope.launch {
            val shop = repository.getShopByIdSync(shopId)
            if (shop != null) {
                val universalUrl = withContext(Dispatchers.IO) {
                    com.example.util.ImageStorageHelper.toUniversalImageUrl(getApplication(), photoUri)
                }
                val result = repository.updateShop(shop.copy(imageUrl = universalUrl), currentUser.value)
                _userMessage.value = if (result.cloudSynced) {
                    "Shop photo updated & synced! 🏪"
                } else {
                    "Shop photo saved locally (cloud sync pending: ${result.errorMessage ?: "Sign in required"})"
                }
            }
        }
    }

    fun useShopPhotoAsDp(shop: ShopEntity) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            authManager.updateProfilePhoto(user.id, shop.imageUrl)
            _userMessage.value = "Shop photo set as your DP! 🏪✨"
        }
    }
}
