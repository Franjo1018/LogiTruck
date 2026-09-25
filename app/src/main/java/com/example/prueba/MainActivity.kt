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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.maplibre.android.MapLibre
import com.example.prueba.data.AuthRepository
import com.example.prueba.data.local.AppDatabase
import com.example.prueba.data.local.Rol
import com.example.prueba.data.local.UsuarioEntity
import com.example.prueba.screens.admin.AdminHostScreen
import com.example.prueba.screens.auth.LoginFormScreen
import com.example.prueba.screens.auth.LoginScreen
import com.example.prueba.screens.auth.RegistroScreen
import com.example.prueba.screens.auth.TipoRegistro
import com.example.prueba.screens.conductor.ConductorHostScreen
import com.example.prueba.viewmodel.AuthUiState
import com.example.prueba.viewmodel.AuthViewModel

// Pantallas disponibles. Es un enum a mano mientras el proyecto es chico;
// cuando crezca el número de pantallas de admin/conductor conviene migrar
// esto a un NavHost de Navigation Compose con rutas por rol.
private enum class Pantalla {
    BIENVENIDA, LOGIN_FORM, REGISTRO, ADMIN_DASHBOARD, CONDUCTOR_HOME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getInstance(applicationContext)
        val authRepository = AuthRepository(database.usuarioDao(), database.empresaDao())

        // MapLibre exige inicializarse una vez al inicio, antes de mostrar cualquier MapView —
        // ver FlotaMapaScreen, la única pantalla que dibuja un mapa real. A diferencia de
        // osmdroid (que se dejó de usar por los bloqueos de OpenStreetMap), acá el mapa se pide
        // con una API key propia de MapTiler (BuildConfig.MAPTILER_API_KEY, ver app/build.gradle
        // y local.properties), no a un servidor anónimo compartido.
        MapLibre.getInstance(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var pantallaActual by remember { mutableStateOf(Pantalla.BIENVENIDA) }
                    // Se guarda aparte del AuthUiState porque authViewModel.limpiarEstado()
                    // vuelve el estado a Inicial justo después de navegar, y AdminHostScreen/
                    // ConductorHostScreen necesitan saber la empresa/rol del usuario mientras
                    // esté en pantalla.
                    var usuarioActual by remember { mutableStateOf<UsuarioEntity?>(null) }

                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.Factory(authRepository)
                    )
                    val authState by authViewModel.uiState.collectAsState()

                    // Cuando el login, registro o una sesión previa autentica a alguien,
                    // navega según su rol.
                    if (authState is AuthUiState.Autenticado) {
                        val usuario = (authState as AuthUiState.Autenticado).usuario
                        usuarioActual = usuario
                        pantallaActual = when (usuario.rol) {
                            Rol.CONDUCTOR -> Pantalla.CONDUCTOR_HOME
                            Rol.DESPACHADOR, Rol.ADMINISTRADOR -> Pantalla.ADMIN_DASHBOARD
                        }
                        authViewModel.limpiarEstado()
                    }

                    when (pantallaActual) {
                        Pantalla.BIENVENIDA -> LoginScreen(
                            onIniciarSesionClick = {
                                pantallaActual = Pantalla.LOGIN_FORM
                            },
                            onRegistrarseClick = {
                                pantallaActual = Pantalla.REGISTRO
                            }
                        )

                        Pantalla.LOGIN_FORM -> LoginFormScreen(
                            onIniciarSesionClick = { usuario, password ->
                                authViewModel.login(usuario, password)
                            },
                            onNuevaCuentaClick = {
                                authViewModel.limpiarEstado()
                                pantallaActual = Pantalla.REGISTRO
                            },
                            onOlvidoPasswordClick = {
                                // TODO: pantalla de recuperar contraseña (aún no existe)
                            },
                            error = (authState as? AuthUiState.Error)?.mensaje,
                            cargando = authState is AuthUiState.Cargando
                        )

                        Pantalla.REGISTRO -> RegistroScreen(
                            onCrearCuentaClick = { form ->
                                when (form.tipo) {
                                    TipoRegistro.CREAR_EMPRESA -> authViewModel.registrarCrearEmpresa(
                                        nombreCompleto = form.nombreCompleto,
                                        usuario = form.usuario,
                                        password = form.password,
                                        nombreEmpresa = form.nombreEmpresa
                                    )
                                    TipoRegistro.UNIRSE_CODIGO -> authViewModel.registrarConCodigo(
                                        nombreCompleto = form.nombreCompleto,
                                        usuario = form.usuario,
                                        password = form.password,
                                        codigoInvitacion = form.codigoInvitacion
                                    )
                                }
                            },
                            onYaTengoCuentaClick = {
                                authViewModel.limpiarEstado()
                                pantallaActual = Pantalla.LOGIN_FORM
                            },
                            errorServidor = (authState as? AuthUiState.Error)?.mensaje,
                            cargando = authState is AuthUiState.Cargando
                        )

                        Pantalla.ADMIN_DASHBOARD -> usuarioActual?.let { usuario ->
                            AdminHostScreen(
                                usuario = usuario,
                                onCerrarSesion = {
                                    authViewModel.cerrarSesion()
                                    usuarioActual = null
                                    pantallaActual = Pantalla.LOGIN_FORM
                                }
                            )
                        }

                        Pantalla.CONDUCTOR_HOME -> usuarioActual?.let { usuario ->
                            ConductorHostScreen(
                                usuario = usuario,
                                onCerrarSesion = {
                                    authViewModel.cerrarSesion()
                                    usuarioActual = null
                                    pantallaActual = Pantalla.LOGIN_FORM
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
