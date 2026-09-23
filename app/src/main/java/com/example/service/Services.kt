package com.example.service

import com.example.BuildConfig
import com.example.data.local.SeedData
import com.example.data.local.entities.UserEntity
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
