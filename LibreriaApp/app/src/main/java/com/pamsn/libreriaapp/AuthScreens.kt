package com.pamsn.libreriaapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pamsn.libreriaapp.network.LoginRequest
import com.pamsn.libreriaapp.network.RegisterRequest
import com.pamsn.libreriaapp.network.RetrofitClient
import com.pamsn.libreriaapp.network.SessionManager
import kotlinx.coroutines.launch

// Menú desplegable con las opciones de navegación entre Inicio de Sesión y Registro,
// requerido por la práctica (Ejercicio 1).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTopBar(onGoToLogin: () -> Unit, onGoToRegister: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("LibreriaApp") },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menú de navegación")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Iniciar Sesión") },
                    onClick = { expanded = false; onGoToLogin() }
                )
                DropdownMenuItem(
                    text = { Text("Registro de Usuario") },
                    onClick = { expanded = false; onGoToRegister() }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { AuthTopBar(onGoToLogin = {}, onGoToRegister = onNavigateToRegister) }
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Mostrar mensaje de error si existe (Requisito de la práctica)
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage!!, color = Color.Red)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (username.isEmpty() || password.isEmpty()) {
                    errorMessage = "Llena todos los campos"
                    return@Button
                }
                
                isLoading = true
                errorMessage = null
                
                // Llamada real a la API
                scope.launch {
                    try {
                        val request = LoginRequest(username, password)
                        val response = RetrofitClient.apiService.login(request)
                        
                        val body = response.body()
                        if (response.isSuccessful && body?.status == "success" && body.token != null) {
                            // Guardamos el token de sesión para las peticiones CRUD posteriores
                            SessionManager.token = body.token
                            onLoginSuccess()
                        } else {
                            // Si el servidor responde 401 u otro error
                            errorMessage = "Credenciales incorrectas"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error de conexión: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("Entrar")
        }
        
        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { AuthTopBar(onGoToLogin = onNavigateBack, onGoToRegister = {}) }
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Crear Cuenta", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nuevo Usuario") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage!!, color = Color.Red)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (username.isEmpty() || password.isEmpty()) {
                    errorMessage = "Llena todos los campos"
                    return@Button
                }

                isLoading = true
                errorMessage = null

                // Llamada real a la API para registro
                scope.launch {
                    try {
                        val request = RegisterRequest(username, password)
                        val response = RetrofitClient.apiService.register(request)
                        
                        if (response.isSuccessful) {
                            onRegisterSuccess() // Se registró bien
                        } else {
                            errorMessage = "El usuario ya existe o hubo un error"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error de conexión: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("Registrarse")
        }
        
        TextButton(onClick = onNavigateBack) {
            Text("Regresar al Login")
        }
    }
    }
}
