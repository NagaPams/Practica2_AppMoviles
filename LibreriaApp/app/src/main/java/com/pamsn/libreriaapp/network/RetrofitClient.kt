package com.pamsn.libreriaapp.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.pamsn.libreriaapp.BuildConfig

object RetrofitClient {
    // La URL base ahora se toma dinámicamente desde local.properties mediante BuildConfig
    private val BASE_URL = BuildConfig.BACKEND_BASE_URL

    // Interceptor que adjunta el token de sesión (JWT) a cada petición,
    // requerido por el backend para las operaciones CRUD de /books.
    private val authInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val token = SessionManager.token

        val request = if (token != null) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
