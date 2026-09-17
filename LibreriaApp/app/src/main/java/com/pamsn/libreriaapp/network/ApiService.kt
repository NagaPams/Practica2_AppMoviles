package com.pamsn.libreriaapp.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    // === AUTENTICACIÓN ===
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // === CRUD DE LIBROS ===
    @GET("books")
    suspend fun getBooks(): Response<List<Book>>

    @POST("books")
    suspend fun createBook(@Body book: Book): Response<BookResponse>

    @PUT("books/{id}")
    suspend fun updateBook(@Path("id") id: Int, @Body book: Book): Response<BookResponse>

    @DELETE("books/{id}")
    suspend fun deleteBook(@Path("id") id: Int): Response<MessageResponse>
}
