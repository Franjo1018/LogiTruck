package com.example.prueba.data

import java.security.MessageDigest

/**
 * Hash de contraseñas con SHA-256. Suficiente para el alcance de la tesis (evita guardar
 * contraseñas en texto plano en el SQLite local), pero SHA-256 puro no es resistente a
 * ataques de fuerza bruta a gran escala como sí lo son bcrypt/Argon2.
 * TODO: si esto sale de un proyecto académico, migrar a una librería con "salt" y
 * factor de costo (bcrypt) antes de manejar datos reales de usuarios.
 */
object PasswordHasher {
    fun hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verificar(password: String, hash: String): Boolean {
        return hash(password) == hash
    }
}
