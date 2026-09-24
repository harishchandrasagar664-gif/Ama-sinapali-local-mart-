package com.example.service

import com.example.BuildConfig
import com.example.data.local.SeedData
import com.example.data.local.entities.UserEntity
import com.example.data.local.entities.AddressEntity
import com.example.data.repository.MartRepository
import com.example.domain.model.PaymentMethod
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

class AuthManager(private val repository: MartRepository) {
    private val _currentUser = MutableStateFlow<UserEntity?>(SeedData.customers[0])
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow<UserRole>(UserRole.CUSTOMER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

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

        _currentUser.value = newUser
        _currentRole.value = UserRole.CUSTOMER
        return newUser
    }

    fun loginExistingCustomer(user: UserEntity) {
        _currentUser.value = user
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
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updatedUser
        }
    }

    suspend fun loginAsShopOwner(phone: String): UserEntity {
        val existing = repository.getUserByPhone(phone)
        val user = if (existing != null && existing.role == UserRole.SHOP_OWNER) {
            existing
        } else {
            SeedData.shopOwners[0]
        }
        _currentUser.value = user
        _currentRole.value = UserRole.SHOP_OWNER
        return user
    }

    fun loginAsAdmin(): UserEntity {
        val admin = SeedData.adminUser
        _currentUser.value = admin
        _currentRole.value = UserRole.ADMIN
        return admin
    }

    fun switchUser(user: UserEntity) {
        _currentUser.value = user
        _currentRole.value = user.role
    }

    fun switchRole(role: UserRole) {
        _currentRole.value = role
        when (role) {
            UserRole.CUSTOMER -> _currentUser.value = SeedData.customers[0]
            UserRole.SHOP_OWNER -> _currentUser.value = SeedData.shopOwners[0]
            UserRole.ADMIN -> _currentUser.value = SeedData.adminUser
        }
    }

    fun logout() {
        _currentUser.value = null
    }
}
