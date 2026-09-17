package com.pamsn.libreriaapp.network

data class Book(
    val id: Int = 0,
    val title: String,
    val author: String,
    val price: Double,
    val stock: Int
)

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val status: String?, val message: String, val user_id: Int?, val username: String?)

data class RegisterRequest(val username: String, val password: String)
data class RegisterResponse(val message: String)

data class BookResponse(val message: String, val book: Book)
data class MessageResponse(val message: String)
