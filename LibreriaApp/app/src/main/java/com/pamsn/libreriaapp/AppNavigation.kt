package com.pamsn.libreriaapp

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // El NavHost maneja las rutas de nuestras pantallas
    NavHost(navController = navController, startDestination = "login") {
        
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = { 
                    // Cuando haya éxito, limpiamos el historial y vamos al catálogo
                    navController.navigate("catalog") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("register") {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { 
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }
        
        composable("catalog") {
            CatalogScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("catalog") { inclusive = true }
                    }
                }
            )
        }
    }
}
