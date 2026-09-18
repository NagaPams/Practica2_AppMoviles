package com.pamsn.libreriaapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pamsn.libreriaapp.network.Book
import com.pamsn.libreriaapp.network.RetrofitClient
import com.pamsn.libreriaapp.network.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var books by remember { mutableStateOf(listOf<Book>()) }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Si el token venció o es inválido, el backend responde 401:
    // cerramos la sesión localmente y regresamos al login.
    fun handleSessionExpired() {
        SessionManager.clear()
        onLogout()
    }

    fun logout() {
        SessionManager.clear()
        onLogout()
    }

    // Cargar libros al iniciar la pantalla
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getBooks()
            if (response.isSuccessful) {
                books = response.body() ?: emptyList()
            } else if (response.code() == 401) {
                handleSessionExpired()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de Libros") },
                actions = {
                    TextButton(onClick = { logout() }) {
                        Text("Salir", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (books.isEmpty()) {
                Text("No hay libros. ¡Agrega el primero!", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(books) { book ->
                        BookItem(
                            book = book,
                            onBuy = {
                                // ACTUALIZAR (PUT): Resta 1 al stock simulando una compra
                                if (book.stock > 0) {
                                    scope.launch {
                                        val updatedBook = book.copy(stock = book.stock - 1)
                                        val res = RetrofitClient.apiService.updateBook(book.id, updatedBook)
                                        if (res.isSuccessful) {
                                            books = books.map { if (it.id == book.id) updatedBook else it }
                                        } else if (res.code() == 401) {
                                            handleSessionExpired()
                                        }
                                    }
                                }
                            },
                            onDelete = {
                                // BORRAR (DELETE)
                                scope.launch {
                                    val res = RetrofitClient.apiService.deleteBook(book.id)
                                    if (res.isSuccessful) {
                                        books = books.filter { it.id != book.id }
                                    } else if (res.code() == 401) {
                                        handleSessionExpired()
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Mostrar cuadro de diálogo para CREAR (POST)
        if (showDialog) {
            AddBookDialog(
                onDismiss = { showDialog = false },
                onAdd = { title, author, price, stock ->
                    scope.launch {
                        val newBook = Book(title = title, author = author, price = price, stock = stock)
                        val response = RetrofitClient.apiService.createBook(newBook)
                        if (response.isSuccessful) {
                            response.body()?.book?.let { created ->
                                books = books + created
                            }
                        } else if (response.code() == 401) {
                            handleSessionExpired()
                        }
                        showDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun BookItem(book: Book, onBuy: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(book.author, style = MaterialTheme.typography.bodyMedium)
                Text("Precio: $${book.price}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("Stock: ${book.stock}", style = MaterialTheme.typography.bodySmall, color = if (book.stock > 0) Color(0xFF4CAF50) else Color.Red)
            }
            Row {
                Button(onClick = onBuy, enabled = book.stock > 0, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Comprar")
                }
                IconButton(onClick = onDelete) {
                    Text("X", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddBookDialog(onDismiss: () -> Unit, onAdd: (String, String, Double, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Nuevo Libro") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Autor") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock inicial") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = price.toDoubleOrNull() ?: 0.0
                val s = stock.toIntOrNull() ?: 0
                if (title.isNotEmpty() && author.isNotEmpty()) {
                    onAdd(title, author, p, s)
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
