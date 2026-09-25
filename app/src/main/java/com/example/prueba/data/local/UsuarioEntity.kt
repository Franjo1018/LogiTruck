package com.example.prueba.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Roles soportados por la app (tabla USUARIO del ERD). */
enum class Rol {
    CONDUCTOR,
    DESPACHADOR,
    ADMINISTRADOR
}

/**
 * Copia local (offline) del perfil del usuario que inició sesión en este dispositivo.
 * La identidad real y la contraseña las maneja Firebase Authentication — Room ya no guarda
 * contraseñas ni hashes, solo cachea el perfil para que la app funcione sin señal después
 * del primer login. `uid` es el mismo id que asigna Firebase Auth (no autogenerado).
 */
@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey
    val uid: String,
    val empresaId: String,
    val nombreCompleto: String,
    val usuario: String,
    val rol: Rol
)
