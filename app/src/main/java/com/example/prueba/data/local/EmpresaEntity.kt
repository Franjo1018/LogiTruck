package com.example.prueba.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Copia local de la empresa a la que pertenece el usuario de este dispositivo.
 * `id` es la misma key que el nodo `empresas/{id}` en Firebase Realtime Database.
 */
@Entity(tableName = "empresa")
data class EmpresaEntity(
    @PrimaryKey
    val id: String,
    val nombre: String
)
