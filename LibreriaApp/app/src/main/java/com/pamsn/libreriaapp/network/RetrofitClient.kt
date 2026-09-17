package com.pamsn.libreriaapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.pamsn.libreriaapp.BuildConfig

object RetrofitClient {
    // La URL base ahora se toma dinámicamente desde local.properties mediante BuildConfig
    private val BASE_URL = BuildConfig.BACKEND_BASE_URL

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
