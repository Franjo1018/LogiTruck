package com.example.prueba.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.prueba.data.AuthRepository
import com.example.prueba.data.AuthResultado
import com.example.prueba.data.local.UsuarioEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Inicial : AuthUiState()
    data object Cargando : AuthUiState()
    data class Autenticado(val usuario: UsuarioEntity) : AuthUiState()
    data class Error(val mensaje: String) : AuthUiState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Inicial)
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        viewModelScope.launch {
            // Primero se revisa si ya había una sesión de Firebase abierta en este dispositivo
            // (Firebase Auth la deja guardada sola entre arranques). Si la hay, se entra directo
            // sin pasar por login. El sembrado de la cuenta admin de prueba solo se intenta
            // cuando NO hay nadie logueado — de lo contrario forzaría un cambio de cuenta y un
            // signOut() que cerraría la sesión de la persona que ya estaba logueada.
            when (val resultado = repository.sesionActual()) {
                is AuthResultado.Exito -> _uiState.value = AuthUiState.Autenticado(resultado.usuario)
                else -> repository.sembrarAdminDePruebaSiNoExiste()
            }
        }
    }

    fun login(usuario: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Cargando
            when (val resultado = repository.login(usuario, password)) {
                is AuthResultado.Exito -> _uiState.value = AuthUiState.Autenticado(resultado.usuario)
                is AuthResultado.Error -> _uiState.value = AuthUiState.Error(resultado.mensaje)
            }
        }
    }

    fun registrarCrearEmpresa(nombreCompleto: String, usuario: String, password: String, nombreEmpresa: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Cargando
            when (val resultado = repository.registrarCrearEmpresa(nombreCompleto, usuario, password, nombreEmpresa)) {
                is AuthResultado.Exito -> _uiState.value = AuthUiState.Autenticado(resultado.usuario)
                is AuthResultado.Error -> _uiState.value = AuthUiState.Error(resultado.mensaje)
            }
        }
    }

    fun registrarConCodigo(nombreCompleto: String, usuario: String, password: String, codigoInvitacion: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Cargando
            when (val resultado = repository.registrarConCodigo(nombreCompleto, usuario, password, codigoInvitacion)) {
                is AuthResultado.Exito -> _uiState.value = AuthUiState.Autenticado(resultado.usuario)
                is AuthResultado.Error -> _uiState.value = AuthUiState.Error(resultado.mensaje)
            }
        }
    }

    fun cerrarSesion() {
        repository.cerrarSesion()
        _uiState.value = AuthUiState.Inicial
    }

    /** Vuelve al estado inicial, p.ej. al cambiar de pantalla de login a registro. */
    fun limpiarEstado() {
        _uiState.value = AuthUiState.Inicial
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(repository) as T
        }
    }
}
