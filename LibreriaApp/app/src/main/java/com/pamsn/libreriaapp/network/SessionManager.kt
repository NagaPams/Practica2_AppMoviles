package com.pamsn.libreriaapp.network

// Guarda el token de sesión (JWT) mientras la app está en memoria.
// RetrofitClient lo usa para adjuntarlo en cada petición autenticada.
object SessionManager {
    var token: String? = null

    fun clear() {
        token = null
    }
}
