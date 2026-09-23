package com.example.domain.model

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    ODIA("or", "Odia", "ଓଡ଼ିଆ");

    fun isOdia(): Boolean = this == ODIA
    fun isEnglish(): Boolean = this == ENGLISH
}
