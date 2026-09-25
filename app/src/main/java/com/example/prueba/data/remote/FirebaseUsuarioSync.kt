package com.example.prueba.data.remote

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Helper genérico para leer/escribir en Firebase Realtime Database con corrutinas.
 *
 * Nota: este archivo se sigue llamando "FirebaseUsuarioSync.kt" por historia (antes solo
 * subía usuarios), pero ahora que el árbol de datos está organizado por empresa
 * (empresas/{empresaId}/..., membresias/{uid}, invitaciones/{codigo}) pasó a ser un helper
 * genérico de lectura/escritura; AuthRepository arma las rutas concretas y llama a estas
 * funciones. Kotlin no exige que el nombre del archivo coincida con el del objeto, así que
 * renombrar el archivo más adelante (a RealtimeDb.kt) no rompe nada.
 */
object RealtimeDb {
    private val raiz get() = FirebaseDatabase.getInstance().reference

    /** Genera una key única nueva bajo [ruta] sin escribir nada todavía (operación local, sin red). */
    fun push(ruta: String): String {
        return raiz.child(ruta).push().key
            ?: throw IllegalStateException("No se pudo generar un id para $ruta")
    }

    suspend fun leer(ruta: String): DataSnapshot = suspendCancellableCoroutine { continuacion ->
        raiz.child(ruta).get()
            .addOnSuccessListener { snapshot ->
                if (continuacion.isActive) continuacion.resume(snapshot)
            }
            .addOnFailureListener { error ->
                if (continuacion.isActive) continuacion.resumeWithException(error)
            }
    }

    suspend fun escribir(ruta: String, valor: Any?): Unit = suspendCancellableCoroutine { continuacion ->
        raiz.child(ruta).setValue(valor)
            .addOnSuccessListener {
                if (continuacion.isActive) continuacion.resume(Unit)
            }
            .addOnFailureListener { error ->
                if (continuacion.isActive) continuacion.resumeWithException(error)
            }
    }

    /**
     * Escritura atómica de varias rutas a la vez (multi-location update). Importante para el
     * registro: `empresas/{empresaId}/usuarios/{uid}` y `membresias/{uid}` deben escribirse
     * juntos en una sola operación para que las reglas de seguridad (que comparan una ruta
     * contra la otra con root.child(...)) vean el estado nuevo de ambas al validar el permiso.
     * Las keys del mapa son rutas absolutas desde la raíz, p.ej. "membresias/$uid".
     */
    suspend fun actualizarVarias(valores: Map<String, Any?>): Unit = suspendCancellableCoroutine { continuacion ->
        raiz.updateChildren(valores)
            .addOnSuccessListener {
                if (continuacion.isActive) continuacion.resume(Unit)
            }
            .addOnFailureListener { error ->
                if (continuacion.isActive) continuacion.resumeWithException(error)
            }
    }
}
