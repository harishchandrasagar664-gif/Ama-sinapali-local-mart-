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

    private val syncCustomer: UserEntity? = restoreCustomerSync()
    private var activeCustomer: UserEntity? = syncCustomer

    private val _currentUser = MutableStateFlow<UserEntity?>(syncCustomer ?: SeedData.customers[0])
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow<UserRole>(UserRole.CUSTOMER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    fun restoreSession(scope: CoroutineScope) {
        val savedId = prefs?.getLong(KEY_SAVED_CUSTOMER_ID, -1L) ?: -1L
        val isLoggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
        if (isLoggedIn && savedId > 0) {
            scope.launch(Dispatchers.IO) {
                val dbUser = repository.getUserById(savedId)
                if (dbUser != null && dbUser.role == UserRole.CUSTOMER) {
                    val verifiedDbUser = dbUser.copy(isVerified = true)
                    activeCustomer = verifiedDbUser
                    _currentUser.value = verifiedDbUser
                    _currentRole.value = UserRole.CUSTOMER
                    if (!dbUser.isVerified) {
                        repository.updateUser(verifiedDbUser)
                    }
                } else if (activeCustomer != null) {
                    repository.insertUser(activeCustomer!!)
                }
            }
        }
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
    }

    fun switchUser(user: UserEntity) {
        _currentUser.value = user
        _currentRole.value = user.role
        if (user.role == UserRole.CUSTOMER) {
            persistCustomerSession(user)
        }
    }

    fun switchRole(role: UserRole) {
        _currentRole.value = role
        when (role) {
            UserRole.CUSTOMER -> _currentUser.value = activeCustomer ?: restoreCustomerSync() ?: SeedData.customers[0]
            UserRole.SHOP_OWNER -> _currentUser.value = SeedData.shopOwners[0]
            UserRole.ADMIN -> _currentUser.value = SeedData.adminUser
        }
    }

    fun logout() {
        prefs?.edit()
            ?.clear()
            ?.putBoolean(KEY_IS_LOGGED_IN, false)
            ?.commit()
        activeCustomer = null
        _currentUser.value = SeedData.customers[0]
        _currentRole.value = UserRole.CUSTOMER
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
