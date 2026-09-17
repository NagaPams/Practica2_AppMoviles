package com.pamsn.libreriaapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Cambiamos el 10.0.2.2 por tu IP local real para evitar el firewall/NAT de Linux en el emulador
    private const val BASE_URL = "http://192.168.69.9:5000/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
