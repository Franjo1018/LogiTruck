package com.example.prueba.data

import com.example.prueba.data.local.EmpresaDao
import com.example.prueba.data.local.EmpresaEntity
import com.example.prueba.data.local.Rol
import com.example.prueba.data.local.UsuarioDao
import com.example.prueba.data.local.UsuarioEntity
import com.example.prueba.data.remote.RealtimeDb
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.tasks.await

sealed class AuthResultado {
    data class Exito(val usuario: UsuarioEntity) : AuthResultado()
    data class Error(val mensaje: String) : AuthResultado()
}

private const val DOMINIO_CORREO_SINTETICO = "logictruck.app"

/**
 * Repositorio de autenticación multiempresa.
 *
 * La identidad y la contraseña ahora las maneja Firebase Authentication (no Room): el
 * "usuario" que la persona escribe en el login se traduce a un correo sintético
 * `usuario@logictruck.app` para poder usar el proveedor Email/Password de Firebase sin
 * pedirle un correo real a nadie (hay que habilitar ese proveedor en Firebase Console →
 * Authentication → Sign-in method).
 *
 * Los datos "de negocio" (a qué empresa pertenece cada usuario, qué rol tiene) viven en
 * Realtime Database bajo `empresas/{empresaId}/usuarios/{uid}`. `membresias/{uid}` es un
 * índice de solo-lectura-tras-crear que le dice a las reglas de seguridad de Firebase a qué
 * empresa pertenece cada usuario autenticado — así la base de datos, no solo la app, impide
 * que una empresa vea los datos de otra (ver database.rules.json).
 *
 * Room sigue existiendo, pero como caché: después de un login/registro exitoso se guarda el
 * perfil localmente para que la próxima vez que se abra la app pueda mostrar el último perfil
 * conocido sin señal. El login/registro en sí siempre necesita conexión, como en cualquier
 * app que usa Firebase Auth.
 */
class AuthRepository(
    private val usuarioDao: UsuarioDao,
    private val empresaDao: EmpresaDao
) {
    private val auth = FirebaseAuth.getInstance()

    private fun correoSintetico(usuario: String): String {
        val normalizado = usuario.trim().lowercase().filter { it.isLetterOrDigit() || it == '.' }
        return "$normalizado@$DOMINIO_CORREO_SINTETICO"
    }

    suspend fun login(usuario: String, password: String): AuthResultado {
        if (usuario.isBlank() || password.isBlank()) {
            return AuthResultado.Error("Ingresa usuario y contraseña")
        }
        return try {
            val credencial = auth.signInWithEmailAndPassword(correoSintetico(usuario), password).await()
            val uid = credencial.user?.uid
                ?: return AuthResultado.Error("No se pudo iniciar sesión, intenta de nuevo")
            cargarPerfilDesdeFirebase(uid, usuario.trim())
        } catch (e: Exception) {
            AuthResultado.Error(mensajeDeError(e))
        }
    }

    /** Registro creando una empresa nueva. Quien registra queda como ADMINISTRADOR de esa empresa. */
    suspend fun registrarCrearEmpresa(
        nombreCompleto: String,
        usuario: String,
        password: String,
        nombreEmpresa: String
    ): AuthResultado {
        if (nombreEmpresa.isBlank()) return AuthResultado.Error("Ingresa el nombre de la empresa")
        return try {
            val (usuarioFirebase, usuarioFinal) = crearCuentaConUsuarioDisponible(usuario, password)
            val uid = usuarioFirebase.uid
            val empresaId = RealtimeDb.push("empresas")

            // Orden importante: primero membresias/{uid} (no depende de nada más), y recién
            // después las rutas de empresas/, cuya regla de seguridad lee membresias/{uid} con
            // root.child(...) — así esa lectura ve un dato ya confirmado, no uno "en camino"
            // dentro de la misma operación (eso era lo que fallaba con permission-denied antes).
            RealtimeDb.escribir(
                "membresias/$uid",
                mapOf("empresaId" to empresaId, "rol" to Rol.ADMINISTRADOR.name)
            )
            RealtimeDb.escribir("empresas/$empresaId/nombre", nombreEmpresa.trim())
            RealtimeDb.escribir(
                "empresas/$empresaId/usuarios/$uid",
                mapOf(
                    "nombreCompleto" to nombreCompleto.trim(),
                    "usuario" to usuarioFinal,
                    "rol" to Rol.ADMINISTRADOR.name
                )
            )

            val perfil = UsuarioEntity(uid, empresaId, nombreCompleto.trim(), usuarioFinal, Rol.ADMINISTRADOR)
            guardarEnCache(perfil, EmpresaEntity(empresaId, nombreEmpresa.trim()))
            AuthResultado.Exito(perfil)
        } catch (e: Exception) {
            AuthResultado.Error(mensajeDeError(e))
        }
    }

    /** Registro uniéndose a una empresa existente mediante un código de invitación generado por un admin. */
    suspend fun registrarConCodigo(
        nombreCompleto: String,
        usuario: String,
        password: String,
        codigoInvitacion: String
    ): AuthResultado {
        val codigo = codigoInvitacion.trim().uppercase()
        if (codigo.isBlank()) return AuthResultado.Error("Ingresa el código de invitación")

        return try {
            val invitacion = RealtimeDb.leer("invitaciones/$codigo")
            if (!invitacion.exists()) {
                return AuthResultado.Error("Código de invitación inválido")
            }
            val empresaId = invitacion.child("empresaId").getValue(String::class.java)
                ?: return AuthResultado.Error("Código de invitación inválido")
            val rol = invitacion.child("rol").getValue(String::class.java)?.let {
                runCatching { Rol.valueOf(it) }.getOrNull()
            } ?: Rol.CONDUCTOR

            val (usuarioFirebase, usuarioFinal) = crearCuentaConUsuarioDisponible(usuario, password)
            val uid = usuarioFirebase.uid

            // Mismo orden que en registrarCrearEmpresa: membresias/{uid} primero, y solo
            // después empresas/{empresaId}/usuarios/{uid}, para que la regla de esta última
            // (que verifica membresias/{uid} con root.child(...)) lea un dato ya confirmado.
            RealtimeDb.escribir("membresias/$uid", mapOf("empresaId" to empresaId, "rol" to rol.name))
            RealtimeDb.escribir(
                "empresas/$empresaId/usuarios/$uid",
                mapOf(
                    "nombreCompleto" to nombreCompleto.trim(),
                    "usuario" to usuarioFinal,
                    "rol" to rol.name
                )
            )

            val nombreEmpresa = RealtimeDb.leer("empresas/$empresaId/nombre").getValue(String::class.java) ?: ""

            val perfil = UsuarioEntity(uid, empresaId, nombreCompleto.trim(), usuarioFinal, rol)
            guardarEnCache(perfil, EmpresaEntity(empresaId, nombreEmpresa))
            AuthResultado.Exito(perfil)
        } catch (e: Exception) {
            AuthResultado.Error(mensajeDeError(e))
        }
    }

    /** Si ya hay una sesión de Firebase activa (login previo en este dispositivo), recarga su perfil. */
    suspend fun sesionActual(): AuthResultado? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            cargarPerfilDesdeFirebase(uid, usuario = "")
        } catch (e: Exception) {
            usuarioDao.obtener(uid)?.let { AuthResultado.Exito(it) }
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }

    /**
     * Crea una cuenta de administrador de prueba (admin / admin123) con una empresa demo la
     * primera vez que hay señal, para poder probar el panel administrativo sin registrarse
     * manualmente. Se llama al arrancar la app; si ya existe, no hace nada (y no deja la
     * sesión abierta como admin: la app siempre arranca en la pantalla de login).
     * TODO: quitar esto antes de publicar la app de verdad.
     */
    suspend fun sembrarAdminDePruebaSiNoExiste() {
        if (auth.currentUser != null) return // por seguridad: nunca reemplazar una sesión activa
        try {
            auth.signInWithEmailAndPassword(correoSintetico("admin"), "admin123").await()
            auth.signOut()
            return
        } catch (e: Exception) {
            // No existe todavía (o no hay señal): se intenta crear a continuación.
        }
        try {
            val credencial = auth.createUserWithEmailAndPassword(correoSintetico("admin"), "admin123").await()
            val uid = credencial.user?.uid ?: return
            val empresaId = RealtimeDb.push("empresas")
            RealtimeDb.escribir(
                "membresias/$uid",
                mapOf("empresaId" to empresaId, "rol" to Rol.ADMINISTRADOR.name)
            )
            RealtimeDb.escribir("empresas/$empresaId/nombre", "Empresa Demo")
            RealtimeDb.escribir(
                "empresas/$empresaId/usuarios/$uid",
                mapOf(
                    "nombreCompleto" to "Administrador de prueba",
                    "usuario" to "admin",
                    "rol" to Rol.ADMINISTRADOR.name
                )
            )
            auth.signOut()
        } catch (e: Exception) {
            // Sin señal en el primer arranque: se reintentará el próximo arranque con red.
        }
    }

    /**
     * Crea la cuenta de Firebase Auth para `usuarioBase`. Como el nombre de usuario ahora se
     * sugiere solo a partir del nombre completo (ver TextoUtil.kt), es normal que dos personas
     * con nombres parecidos generen el mismo usuario (dos "Juan Pérez", por ejemplo); en vez de
     * obligar a la persona a inventarse otro, si ya está tomado se le agrega un número al final
     * (juanperez, juanperez2, juanperez3, ...) hasta encontrar uno libre. Devuelve la cuenta
     * creada y el usuario final que quedó asignado, para guardarlo y mostrárselo a la persona.
     */
    private suspend fun crearCuentaConUsuarioDisponible(usuarioBase: String, password: String): Pair<FirebaseUser, String> {
        val base = usuarioBase.trim().ifBlank { "usuario" }
        var intento = 0
        while (true) {
            val candidato = if (intento == 0) base else "$base${intento + 1}"
            try {
                val credencial = auth.createUserWithEmailAndPassword(correoSintetico(candidato), password).await()
                val usuarioFirebase = credencial.user
                    ?: throw IllegalStateException("No se pudo crear la cuenta, intenta de nuevo")
                return usuarioFirebase to candidato
            } catch (e: Exception) {
                val usuarioYaExiste = e is FirebaseAuthUserCollisionException ||
                    (e.message?.contains("already in use", ignoreCase = true) == true)
                if (!usuarioYaExiste || intento >= 30) throw e
                intento++
            }
        }
    }

    private suspend fun cargarPerfilDesdeFirebase(uid: String, usuario: String): AuthResultado {
        val membresia = RealtimeDb.leer("membresias/$uid")
        val empresaId = membresia.child("empresaId").getValue(String::class.java)
            ?: return AuthResultado.Error("Tu cuenta no está asociada a ninguna empresa")

        val datosUsuario = RealtimeDb.leer("empresas/$empresaId/usuarios/$uid")
        val nombreCompleto = datosUsuario.child("nombreCompleto").getValue(String::class.java) ?: ""
        val usuarioGuardado = datosUsuario.child("usuario").getValue(String::class.java) ?: usuario
        val rol = datosUsuario.child("rol").getValue(String::class.java)?.let {
            runCatching { Rol.valueOf(it) }.getOrNull()
        } ?: Rol.CONDUCTOR

        val nombreEmpresa = RealtimeDb.leer("empresas/$empresaId/nombre").getValue(String::class.java) ?: ""

        val perfil = UsuarioEntity(uid, empresaId, nombreCompleto, usuarioGuardado, rol)
        guardarEnCache(perfil, EmpresaEntity(empresaId, nombreEmpresa))
        return AuthResultado.Exito(perfil)
    }

    private suspend fun guardarEnCache(usuario: UsuarioEntity, empresa: EmpresaEntity) {
        empresaDao.guardar(empresa)
        usuarioDao.guardar(usuario)
    }

    private fun mensajeDeError(e: Exception): String {
        val mensaje = e.message ?: return "Ocurrió un error, intenta de nuevo"
        return when {
            mensaje.contains("badly formatted", ignoreCase = true) -> "Ese nombre de usuario no es válido"
            mensaje.contains("password is invalid", ignoreCase = true) ||
                mensaje.contains("no user record", ignoreCase = true) ||
                mensaje.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "Usuario o contraseña incorrectos"
            mensaje.contains("already in use", ignoreCase = true) -> "Ese usuario ya existe"
            mensaje.contains("network error", ignoreCase = true) -> "Sin conexión a internet"
            mensaje.contains("weak password", ignoreCase = true) -> "La contraseña es muy débil (mínimo 6 caracteres)"
            else -> mensaje
        }
    }
}
