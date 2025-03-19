package com.washburn.habitguard

data class User(
    val name: String,
    val email: String,
    val profileImageUrl: String? = null,
    val age: Int? = null
)