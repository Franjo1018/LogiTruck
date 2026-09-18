package com.example.prueba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.prueba.screens.auth.LoginFormScreen
import com.example.prueba.screens.auth.LoginScreen

// Pantallas disponibles. Cuando migres a Navigation Compose,
// esto se reemplaza por rutas de un NavHost.
private enum class Pantalla {
    BIENVENIDA, LOGIN_FORM
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var pantallaActual by remember { mutableStateOf(Pantalla.LOGIN_FORM) }

                    when (pantallaActual) {
                        Pantalla.BIENVENIDA -> LoginScreen(
                            onIniciarSesionClick = {
                                pantallaActual = Pantalla.LOGIN_FORM
                            },
                            onRegistrarseClick = {
                                pantallaActual = Pantalla.LOGIN_FORM
                            }
                        )

                        Pantalla.LOGIN_FORM -> LoginFormScreen(
                            onIniciarSesionClick = { usuario, password ->
                                // TODO: conectar con tu AuthViewModel, ej:
                                // viewModel.login(usuario, password)
                            },
                            onNuevaCuentaClick = {
                                // TODO: navegar a pantalla de registro cuando la tengas
                            },
                            onOlvidoPasswordClick = {
                                // TODO: navegar a pantalla de recuperar contraseña
                            }
                        )
                    }
                }
            }
        }
    }
}