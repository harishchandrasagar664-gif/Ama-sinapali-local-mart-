package com.example.service

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.local.SeedData
import com.example.data.local.entities.UserEntity
import com.example.data.local.entities.AddressEntity
import com.example.data.repository.MartRepository
import com.example.domain.model.PaymentMethod
import com.example.domain.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface PaymentService {
    fun getRazorpayKeyId(): String
    fun isRazorpayConfigured(): Boolean
}

sealed class PaymentResult {
    data class Success(val transactionId: String, val paymentMethod: PaymentMethod) : PaymentResult()
    data class Failure(val errorCode: String, val message: String) : PaymentResult()
    object Cancelled : PaymentResult()
}

class RazorpayPaymentServiceImpl : PaymentService {
    override fun getRazorpayKeyId(): String = BuildConfig.RAZORPAY_KEY_ID

    override fun isRazorpayConfigured(): Boolean {
        val key = getRazorpayKeyId()
        return key.isNotBlank() && !key.contains("placeholder")
    }
}

class AuthManager(
    private val repository: MartRepository,
    private val context: Context? = null
) {
    companion object {
        private const val PREFS_NAME = "ama_sinapali_customer_session"
        // Customer session keys
        private const val KEY_SAVED_CUSTOMER_ID = "saved_customer_id"
        private const val KEY_IS_LOGGED_IN = "is_customer_logged_in"
        private const val KEY_CUSTOMER_NAME = "saved_customer_name"
        private const val KEY_CUSTOMER_PHONE = "saved_customer_phone"
        private const val KEY_CUSTOMER_EMAIL = "saved_customer_email"
        private const val KEY_CUSTOMER_DISTRICT = "saved_customer_district"
        private const val KEY_CUSTOMER_BLOCK = "saved_customer_block"
        private const val KEY_CUSTOMER_GP = "saved_customer_gp"
        private const val KEY_CUSTOMER_VILLAGE = "saved_customer_village"
        private const val KEY_CUSTOMER_ADDRESS = "saved_customer_address"
        private const val KEY_CUSTOMER_PHOTO_URI = "saved_customer_photo_uri"
        private const val KEY_CUSTOMER_VERIFIED = "saved_customer_verified"

        // Shop Owner session keys
        private const val KEY_SAVED_SHOP_OWNER_ID = "saved_shop_owner_id"
        private const val KEY_IS_SHOP_OWNER_LOGGED_IN = "is_shop_owner_logged_in"
        private const val KEY_SHOP_OWNER_NAME = "saved_shop_owner_name"
        private const val KEY_SHOP_OWNER_PHONE = "saved_shop_owner_phone"
        private const val KEY_SHOP_OWNER_EMAIL = "saved_shop_owner_email"
        private const val KEY_SHOP_OWNER_SHOP_ID = "saved_shop_owner_shop_id"
        private const val KEY_SHOP_OWNER_DISTRICT = "saved_shop_owner_district"
        private const val KEY_SHOP_OWNER_BLOCK = "saved_shop_owner_block"
        private const val KEY_SHOP_OWNER_GP = "saved_shop_owner_gp"
        private const val KEY_SHOP_OWNER_VILLAGE = "saved_shop_owner_village"
        private const val KEY_SHOP_OWNER_ADDRESS = "saved_shop_owner_address"
        private const val KEY_SHOP_OWNER_PHOTO_URI = "saved_shop_owner_photo_uri"

        // Last active role
        private const val KEY_LAST_ACTIVE_ROLE = "last_active_role"
    }

    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun restoreCustomerSync(): UserEntity? {
        val isLoggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
        val savedId = prefs?.getLong(KEY_SAVED_CUSTOMER_ID, -1L) ?: -1L
        if (!isLoggedIn || savedId <= 0) return null

        val name = prefs.getString(KEY_CUSTOMER_NAME, null) ?: "Sinapali Customer"
        val phone = prefs.getString(KEY_CUSTOMER_PHONE, null) ?: ""
        val email = prefs.getString(KEY_CUSTOMER_EMAIL, null) ?: ""
        val district = prefs.getString(KEY_CUSTOMER_DISTRICT, null) ?: "Nuapada"
        val block = prefs.getString(KEY_CUSTOMER_BLOCK, null) ?: "Sinapali"
        val gp = prefs.getString(KEY_CUSTOMER_GP, null) ?: "Sinapali"
        val village = prefs.getString(KEY_CUSTOMER_VILLAGE, null) ?: "Sinapali"
        val address = prefs.getString(KEY_CUSTOMER_ADDRESS, null) ?: "Near Panchayat Samiti Office, Word No 4, Sinapali"
        val photoUri = prefs.getString(KEY_CUSTOMER_PHOTO_URI, null)
        val verified = prefs.getBoolean(KEY_CUSTOMER_VERIFIED, true)

        return UserEntity(
            id = savedId,
            name = name,
            phone = phone,
            email = email,
            role = UserRole.CUSTOMER,
            isVerified = verified,
            district = district,
            block = block,
            gp = gp,
            village = village,
            deliveryAddress = address,
            profilePhotoUri = photoUri
        )
    }

    private fun restoreShopOwnerSync(): UserEntity? {
        val isLoggedIn = prefs?.getBoolean(KEY_IS_SHOP_OWNER_LOGGED_IN, false) ?: false
        val savedId = prefs?.getLong(KEY_SAVED_SHOP_OWNER_ID, -1L) ?: -1L
        val phone = prefs?.getString(KEY_SHOP_OWNER_PHONE, null) ?: ""
        if (!isLoggedIn || (savedId <= 0 && phone.isBlank())) return null

        val name = prefs.getString(KEY_SHOP_OWNER_NAME, null) ?: "Shop Owner"
        val email = prefs.getString(KEY_SHOP_OWNER_EMAIL, null) ?: ""
        val shopId = prefs.getLong(KEY_SHOP_OWNER_SHOP_ID, -1L)
        val district = prefs.getString(KEY_SHOP_OWNER_DISTRICT, null) ?: "Nuapada"
        val block = prefs.getString(KEY_SHOP_OWNER_BLOCK, null) ?: "Sinapali"
        val gp = prefs.getString(KEY_SHOP_OWNER_GP, null) ?: "Sinapali"
        val village = prefs.getString(KEY_SHOP_OWNER_VILLAGE, null) ?: "Sinapali"
        val address = prefs.getString(KEY_SHOP_OWNER_ADDRESS, null) ?: "Sinapali Main Market"
        val photoUri = prefs.getString(KEY_SHOP_OWNER_PHOTO_URI, null)

        return UserEntity(
            id = if (savedId > 0) savedId else 0L,
            name = name,
            phone = phone,
            email = email,
            role = UserRole.SHOP_OWNER,
            shopId = if (shopId > 0) shopId else null,
            isVerified = true,
            district = district,
            block = block,
            gp = gp,
            village = village,
            deliveryAddress = address,
            profilePhotoUri = photoUri
        )
    }

    private val syncShopOwner: UserEntity? = restoreShopOwnerSync()
    private val syncCustomer: UserEntity? = restoreCustomerSync()

    private var activeShopOwner: UserEntity? = syncShopOwner
    private var activeCustomer: UserEntity? = syncCustomer

    private val initialRole: UserRole = if (syncShopOwner != null && prefs?.getBoolean(KEY_IS_SHOP_OWNER_LOGGED_IN, false) == true) {
        UserRole.SHOP_OWNER
    } else {
        UserRole.CUSTOMER
    }

    private val _currentUser = MutableStateFlow<UserEntity?>(
        if (initialRole == UserRole.SHOP_OWNER) (syncShopOwner ?: SeedData.shopOwners[0])
        else (syncCustomer ?: SeedData.customers[0])
    )
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow<UserRole>(initialRole)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    fun restoreSession(scope: CoroutineScope) {
        val isShopOwnerLoggedIn = prefs?.getBoolean(KEY_IS_SHOP_OWNER_LOGGED_IN, false) ?: false
        val savedShopOwnerId = prefs?.getLong(KEY_SAVED_SHOP_OWNER_ID, -1L) ?: -1L
        val savedShopOwnerPhone = prefs?.getString(KEY_SHOP_OWNER_PHONE, "") ?: ""

        val isCustLoggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
        val savedCustId = prefs?.getLong(KEY_SAVED_CUSTOMER_ID, -1L) ?: -1L

        scope.launch(Dispatchers.IO) {
            // Restore Shop Owner from DB if logged in
            if (isShopOwnerLoggedIn) {
                var dbOwner = if (savedShopOwnerId > 0) repository.getUserById(savedShopOwnerId) else null
                if (dbOwner == null && savedShopOwnerPhone.isNotBlank()) {
                    dbOwner = repository.getUserByPhone(savedShopOwnerPhone)
                }
                if (dbOwner != null && dbOwner.role == UserRole.SHOP_OWNER) {
                    val verifiedOwner = dbOwner.copy(isVerified = true)
                    activeShopOwner = verifiedOwner
                    if (_currentRole.value == UserRole.SHOP_OWNER) {
                        _currentUser.value = verifiedOwner
                    }
                } else if (activeShopOwner != null) {
                    val id = repository.insertUser(activeShopOwner!!)
                    activeShopOwner = activeShopOwner!!.copy(id = id)
                    prefs?.edit()?.putLong(KEY_SAVED_SHOP_OWNER_ID, id)?.apply()
                }
            }

            // Restore Customer from DB if logged in
            if (isCustLoggedIn && savedCustId > 0) {
                val dbUser = repository.getUserById(savedCustId)
                if (dbUser != null && dbUser.role == UserRole.CUSTOMER) {
                    val verifiedDbUser = dbUser.copy(isVerified = true)
                    activeCustomer = verifiedDbUser
                    if (_currentRole.value == UserRole.CUSTOMER) {
                        _currentUser.value = verifiedDbUser
                    }
                    if (!dbUser.isVerified) {
                        repository.updateUser(verifiedDbUser)
                    }
                } else if (activeCustomer != null) {
                    repository.insertUser(activeCustomer!!)
                }
            }
        }
    }

    fun persistShopOwnerSession(user: UserEntity) {
        val verifiedUser = user.copy(role = UserRole.SHOP_OWNER, isVerified = true)
        activeShopOwner = verifiedUser
        prefs?.edit()
            ?.putBoolean(KEY_IS_SHOP_OWNER_LOGGED_IN, true)
            ?.putLong(KEY_SAVED_SHOP_OWNER_ID, verifiedUser.id)
            ?.putString(KEY_SHOP_OWNER_NAME, verifiedUser.name)
            ?.putString(KEY_SHOP_OWNER_PHONE, verifiedUser.phone)
            ?.putString(KEY_SHOP_OWNER_EMAIL, verifiedUser.email)
            ?.putLong(KEY_SHOP_OWNER_SHOP_ID, verifiedUser.shopId ?: -1L)
            ?.putString(KEY_SHOP_OWNER_DISTRICT, verifiedUser.district)
            ?.putString(KEY_SHOP_OWNER_BLOCK, verifiedUser.block)
            ?.putString(KEY_SHOP_OWNER_GP, verifiedUser.gp)
            ?.putString(KEY_SHOP_OWNER_VILLAGE, verifiedUser.village)
            ?.putString(KEY_SHOP_OWNER_ADDRESS, verifiedUser.deliveryAddress)
            ?.putString(KEY_SHOP_OWNER_PHOTO_URI, verifiedUser.profilePhotoUri)
            ?.putString(KEY_LAST_ACTIVE_ROLE, "SHOP_OWNER")
            ?.commit()
    }

    fun clearShopOwnerSession() {
        prefs?.edit()
            ?.putBoolean(KEY_IS_SHOP_OWNER_LOGGED_IN, false)
            ?.remove(KEY_SAVED_SHOP_OWNER_ID)
            ?.remove(KEY_SHOP_OWNER_NAME)
            ?.remove(KEY_SHOP_OWNER_PHONE)
            ?.remove(KEY_SHOP_OWNER_EMAIL)
            ?.remove(KEY_SHOP_OWNER_SHOP_ID)
            ?.remove(KEY_SHOP_OWNER_DISTRICT)
            ?.remove(KEY_SHOP_OWNER_BLOCK)
            ?.remove(KEY_SHOP_OWNER_GP)
            ?.remove(KEY_SHOP_OWNER_VILLAGE)
            ?.remove(KEY_SHOP_OWNER_ADDRESS)
            ?.remove(KEY_SHOP_OWNER_PHOTO_URI)
            ?.putString(KEY_LAST_ACTIVE_ROLE, "CUSTOMER")
            ?.commit()
        activeShopOwner = null
    }

    fun isShopOwnerSessionActive(): Boolean {
        val hasActiveOwner = activeShopOwner != null && activeShopOwner?.role == UserRole.SHOP_OWNER
        val hasPref = prefs?.getBoolean(KEY_IS_SHOP_OWNER_LOGGED_IN, false) == true
        return hasActiveOwner || hasPref
    }

    fun restoreShopOwnerSession(): UserEntity? {
        val owner = activeShopOwner ?: restoreShopOwnerSync()
        if (owner != null) {
            activeShopOwner = owner
            _currentUser.value = owner
            _currentRole.value = UserRole.SHOP_OWNER
            prefs?.edit()?.putString(KEY_LAST_ACTIVE_ROLE, "SHOP_OWNER")?.commit()
            return owner
        }
        return null
    }

    fun getActiveShopOwner(): UserEntity? {
        return activeShopOwner ?: restoreShopOwnerSync()
    }

    private fun persistCustomerSession(user: UserEntity) {
        if (user.role == UserRole.CUSTOMER) {
            val verifiedUser = user.copy(isVerified = true)
            activeCustomer = verifiedUser
            prefs?.edit()
                ?.putLong(KEY_SAVED_CUSTOMER_ID, verifiedUser.id)
                ?.putBoolean(KEY_IS_LOGGED_IN, true)
                ?.putString(KEY_CUSTOMER_NAME, verifiedUser.name)
                ?.putString(KEY_CUSTOMER_PHONE, verifiedUser.phone)
                ?.putString(KEY_CUSTOMER_EMAIL, verifiedUser.email)
                ?.putString(KEY_CUSTOMER_DISTRICT, verifiedUser.district)
                ?.putString(KEY_CUSTOMER_BLOCK, verifiedUser.block)
                ?.putString(KEY_CUSTOMER_GP, verifiedUser.gp)
                ?.putString(KEY_CUSTOMER_VILLAGE, verifiedUser.village)
                ?.putString(KEY_CUSTOMER_ADDRESS, verifiedUser.deliveryAddress)
                ?.putString(KEY_CUSTOMER_PHOTO_URI, verifiedUser.profilePhotoUri)
                ?.putBoolean(KEY_CUSTOMER_VERIFIED, true)
                ?.putString(KEY_LAST_ACTIVE_ROLE, "CUSTOMER")
                ?.commit()
        }
    }

    suspend fun loginAsCustomer(name: String, phone: String, location: String): UserEntity {
        val existing = repository.getUserByPhone(phone)
        val user = if (existing != null && existing.role == UserRole.CUSTOMER) {
            existing
        } else {
            val newUser = UserEntity(
                name = name.ifBlank { "Customer" },
                phone = phone,
                role = UserRole.CUSTOMER
            )
            val id = repository.insertUser(newUser)
            newUser.copy(id = id)
        }
        persistCustomerSession(user)
        _currentUser.value = user
        _currentRole.value = UserRole.CUSTOMER
        return user
    }

    suspend fun findCustomerByPhoneOrEmail(identifier: String): UserEntity? {
        val trimmed = identifier.trim()
        val user = repository.getUserByPhoneOrEmail(trimmed)
        return if (user != null && user.role == UserRole.CUSTOMER) user else null
    }

    suspend fun registerCustomerWithOdishaAddress(
        name: String,
        phone: String,
        email: String,
        district: String,
        block: String,
        gp: String,
        village: String,
        fullAddress: String
    ): UserEntity {
        val existing = repository.getUserByPhoneOrEmail(phone.trim().ifBlank { email.trim() })
        val newUser = if (existing != null) {
            existing.copy(
                name = name.ifBlank { existing.name },
                phone = phone.ifBlank { existing.phone },
                email = email.ifBlank { existing.email },
                district = district,
                block = block,
                gp = gp,
                village = village,
                deliveryAddress = fullAddress,
                isVerified = true
            ).also { repository.updateUser(it) }
        } else {
            val entity = UserEntity(
                name = name.ifBlank { "Verified Customer" },
                phone = phone.trim(),
                email = email.trim(),
                role = UserRole.CUSTOMER,
                isVerified = true,
                district = district,
                block = block,
                gp = gp,
                village = village,
                deliveryAddress = fullAddress
            )
            val id = repository.insertUser(entity)
            entity.copy(id = id)
        }

        // Save default address entity for future orders
        val addressEntity = AddressEntity(
            userId = newUser.id,
            title = "Home",
            fullAddress = fullAddress,
            phone = newUser.phone,
            isDefault = true,
            district = district,
            block = block,
            gp = gp,
            village = village
        )
        repository.addAddress(addressEntity)

        persistCustomerSession(newUser)
        _currentUser.value = newUser
        _currentRole.value = UserRole.CUSTOMER
        return newUser
    }

    fun loginExistingCustomer(user: UserEntity) {
        val verified = user.copy(isVerified = true)
        persistCustomerSession(verified)
        _currentUser.value = verified
        _currentRole.value = UserRole.CUSTOMER
    }

    suspend fun updateCustomerAddress(
        userId: Long,
        district: String,
        block: String,
        gp: String,
        village: String,
        fullAddress: String
    ) {
        val user = _currentUser.value ?: return
        if (user.id == userId) {
            val updatedUser = user.copy(
                district = district,
                block = block,
                gp = gp,
                village = village,
                deliveryAddress = fullAddress
            )
            repository.updateUser(updatedUser)
            persistCustomerSession(updatedUser)
            _currentUser.value = updatedUser

            val defaultAddr = repository.getDefaultAddressForUser(userId)
            if (defaultAddr != null) {
                repository.updateAddress(
                    defaultAddr.copy(
                        district = district,
                        block = block,
                        gp = gp,
                        village = village,
                        fullAddress = fullAddress
                    )
                )
            } else {
                repository.addAddress(
                    AddressEntity(
                        userId = userId,
                        title = "Home",
                        fullAddress = fullAddress,
                        phone = user.phone,
                        isDefault = true,
                        district = district,
                        block = block,
                        gp = gp,
                        village = village
                    )
                )
            }
        }
    }

    suspend fun updateProfilePhoto(userId: Long, photoUri: String?) {
        val user = _currentUser.value ?: repository.getUserById(userId) ?: return
        val updatedUser = user.copy(profilePhotoUri = photoUri)
        repository.updateUser(updatedUser)
        if (updatedUser.role == UserRole.CUSTOMER) {
            persistCustomerSession(updatedUser)
        }
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updatedUser
        }
    }

    suspend fun loginAsShopOwner(phoneOrEmail: String): UserEntity? {
        val trimmed = phoneOrEmail.trim()
        val existing = repository.getUserByPhone(trimmed) ?: repository.getUserByPhoneOrEmail(trimmed)
        if (existing != null && existing.role == UserRole.SHOP_OWNER) {
            _currentUser.value = existing
            _currentRole.value = UserRole.SHOP_OWNER
            return existing
        }
        val seedOwner = SeedData.shopOwners.find { it.phone == trimmed || it.email.equals(trimmed, ignoreCase = true) }
        if (seedOwner != null) {
            _currentUser.value = seedOwner
            _currentRole.value = UserRole.SHOP_OWNER
            return seedOwner
        }
        return null
    }

    fun loginAsAdminWithPin(pin: String): Boolean {
        val trimmed = pin.trim()
        if (trimmed == "7894" || trimmed == "7894952827" || trimmed == "admin123" || trimmed == "admin") {
            _currentUser.value = SeedData.adminUser
            _currentRole.value = UserRole.ADMIN
            return true
        }
        return false
    }

    fun loginAsAdmin(): UserEntity {
        val admin = SeedData.adminUser
        _currentUser.value = admin
        _currentRole.value = UserRole.ADMIN
        return admin
    }

    fun switchToCustomerView() {
        _currentRole.value = UserRole.CUSTOMER
        val cust = activeCustomer ?: restoreCustomerSync() ?: SeedData.customers[0]
        _currentUser.value = cust
        prefs?.edit()?.putString(KEY_LAST_ACTIVE_ROLE, "CUSTOMER")?.apply()
    }

    fun switchUser(user: UserEntity) {
        _currentUser.value = user
        _currentRole.value = user.role
        when (user.role) {
            UserRole.CUSTOMER -> {
                persistCustomerSession(user)
                prefs?.edit()?.putString(KEY_LAST_ACTIVE_ROLE, "CUSTOMER")?.apply()
            }
            UserRole.SHOP_OWNER -> {
                persistShopOwnerSession(user)
                prefs?.edit()?.putString(KEY_LAST_ACTIVE_ROLE, "SHOP_OWNER")?.apply()
            }
            UserRole.ADMIN -> {}
        }
    }

    fun switchRole(role: UserRole) {
        _currentRole.value = role
        if (_currentUser.value?.role != role) {
            when (role) {
                UserRole.CUSTOMER -> {
                    _currentUser.value = activeCustomer ?: restoreCustomerSync() ?: SeedData.customers[0]
                    prefs?.edit()?.putString(KEY_LAST_ACTIVE_ROLE, "CUSTOMER")?.apply()
                }
                UserRole.SHOP_OWNER -> {
                    val owner = activeShopOwner ?: restoreShopOwnerSync() ?: SeedData.shopOwners[0]
                    _currentUser.value = owner
                    prefs?.edit()?.putString(KEY_LAST_ACTIVE_ROLE, "SHOP_OWNER")?.apply()
                }
                UserRole.ADMIN -> _currentUser.value = SeedData.adminUser
            }
        }
    }

    fun logoutShopOwner() {
        clearShopOwnerSession()
        _currentRole.value = UserRole.CUSTOMER
        _currentUser.value = activeCustomer ?: restoreCustomerSync() ?: SeedData.customers[0]
    }

    fun logoutCustomer() {
        prefs?.edit()
            ?.remove(KEY_SAVED_CUSTOMER_ID)
            ?.putBoolean(KEY_IS_LOGGED_IN, false)
            ?.remove(KEY_CUSTOMER_NAME)
            ?.remove(KEY_CUSTOMER_PHONE)
            ?.remove(KEY_CUSTOMER_EMAIL)
            ?.remove(KEY_CUSTOMER_DISTRICT)
            ?.remove(KEY_CUSTOMER_BLOCK)
            ?.remove(KEY_CUSTOMER_GP)
            ?.remove(KEY_CUSTOMER_VILLAGE)
            ?.remove(KEY_CUSTOMER_ADDRESS)
            ?.remove(KEY_CUSTOMER_PHOTO_URI)
            ?.remove(KEY_CUSTOMER_VERIFIED)
            ?.commit()
        activeCustomer = null
        if (_currentRole.value == UserRole.CUSTOMER) {
            _currentUser.value = SeedData.customers[0]
        }
    }

    fun logout() {
        logoutCustomer()
    }

    fun isCustomerSessionActive(): Boolean {
        val hasActiveCustomer = activeCustomer != null && activeCustomer?.role == UserRole.CUSTOMER
        val hasPref = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) == true && (prefs?.getLong(KEY_SAVED_CUSTOMER_ID, -1L) ?: -1L) > 0
        return hasActiveCustomer || hasPref
    }

    fun getPersistedCustomerId(): Long {
        return activeCustomer?.id ?: (prefs?.getLong(KEY_SAVED_CUSTOMER_ID, -1L) ?: -1L)
    }
}
