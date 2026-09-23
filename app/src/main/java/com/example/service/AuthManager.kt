package com.example.service

import com.example.data.local.SeedData
import com.example.data.local.entities.UserEntity
import com.example.data.repository.MartRepository
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(private val repository: MartRepository) {

    private val _currentUser = MutableStateFlow<UserEntity?>(SeedData.customers[0])
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.CUSTOMER)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    suspend fun loginAsCustomer(phone: String, name: String, location: String): UserEntity {
        var user = repository.getUserByPhone(phone)
        if (user == null) {
            val newUser = UserEntity(
                role = UserRole.CUSTOMER,
                phone = phone,
                name = name.ifBlank { "Local Shopper" },
                location = location.ifBlank { "Sinapali, Nuapada" }
            )
            val newId = repository.registerUser(newUser)
            user = newUser.copy(id = newId)
        }
        _currentUser.value = user
        _currentRole.value = UserRole.CUSTOMER
        return user
    }

    suspend fun loginAsShopOwner(phone: String): UserEntity? {
        val user = repository.getUserByPhone(phone)
        if (user != null && user.role == UserRole.SHOP_OWNER) {
            _currentUser.value = user
            _currentRole.value = UserRole.SHOP_OWNER
            return user
        }
        // Fallback to first shop owner if not found
        val fallback = SeedData.shopOwners[0]
        _currentUser.value = fallback
        _currentRole.value = UserRole.SHOP_OWNER
        return fallback
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
