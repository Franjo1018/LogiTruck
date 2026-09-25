package com.example.prueba.data

import com.example.prueba.data.remote.RealtimeDb
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mensajería entre administrador/despachador y cada conductor, en Realtime Database bajo
 * empresas/{empresaId}/mensajes/{conductorUid}/{idReal} — un hilo por conductor (así se ve
 * igual desde el lado admin, que lista un hilo por conductor, y desde el lado conductor, que
 * solo tiene su propio hilo con la empresa). Empieza vacío, sin datos de ejemplo.
 */
object MensajeRepository {
    private val formatoHora = SimpleDateFormat("h:mm a", Locale("es", "PE"))

    suspend fun enviar(
        empresaId: String,
        conductorUid: String,
        autorUid: String,
        autorNombre: String,
        texto: String,
        esDeAdmin: Boolean
    ) {
        val idReal = RealtimeDb.push("empresas/$empresaId/mensajes/$conductorUid")
        val ahora = System.currentTimeMillis()
        RealtimeDb.escribir(
            "empresas/$empresaId/mensajes/$conductorUid/$idReal",
            mapOf(
                "autor" to autorNombre,
                "autorUid" to autorUid,
                "texto" to texto,
                "esDeAdmin" to esDeAdmin,
                "hora" to formatoHora.format(Date(ahora)),
                "timestampMs" to ahora
            )
        )
    }

    /** Mensajes de un hilo, ordenados del más antiguo al más reciente. [uidVisor] decide qué burbujas se ven "propias". */
    suspend fun listarMensajes(empresaId: String, conductorUid: String, uidVisor: String): List<MensajeChat> {
        val snapshot = RealtimeDb.leer("empresas/$empresaId/mensajes/$conductorUid")
        return snapshot.children.mapNotNull { hijo ->
            val texto = hijo.child("texto").getValue(String::class.java) ?: return@mapNotNull null
            val autorUid = hijo.child("autorUid").getValue(String::class.java) ?: ""
            val timestamp = (hijo.child("timestampMs").value as? Number)?.toLong() ?: 0L
            MensajeChat(
                id = timestamp,
                autor = hijo.child("autor").getValue(String::class.java) ?: "",
                texto = texto,
                hora = hijo.child("hora").getValue(String::class.java) ?: "",
                esPropio = autorUid == uidVisor,
                idReal = hijo.key ?: "",
                autorUid = autorUid
            )
        }.sortedBy { it.id }
    }

    /** Un resumen de conversación por cada conductor de la empresa (para la bandeja del admin). */
    suspend fun listarConversaciones(empresaId: String, conductores: List<Conductor>): List<ConversacionResumen> {
        val snapshot = RealtimeDb.leer("empresas/$empresaId/mensajes")
        val nombrePorUid = conductores.associateBy({ it.uid }, { it.nombre })
        return snapshot.children.mapNotNull { hiloConductor ->
            val conductorUid = hiloConductor.key ?: return@mapNotNull null
            val mensajes = hiloConductor.children.mapNotNull { hijo ->
                val texto = hijo.child("texto").getValue(String::class.java) ?: return@mapNotNull null
                val esDeAdmin = hijo.child("esDeAdmin").getValue(Boolean::class.java) ?: false
                val timestamp = (hijo.child("timestampMs").value as? Number)?.toLong() ?: 0L
                Triple(texto, esDeAdmin, timestamp)
            }.sortedBy { it.third }
            if (mensajes.isEmpty()) return@mapNotNull null
            val ultimo = mensajes.last()
            val ultimoAdminIndex = mensajes.indexOfLast { it.second }
            val noLeidos = mensajes.subList(ultimoAdminIndex + 1, mensajes.size).count { !it.second }
            ConversacionResumen(
                id = ultimo.third,
                nombreConductor = nombrePorUid[conductorUid] ?: conductorUid,
                ultimoMensaje = ultimo.first,
                hora = hiloConductor.children.lastOrNull()?.child("hora")?.getValue(String::class.java) ?: "",
                noLeidos = noLeidos,
                conductorUid = conductorUid
            )
        }.sortedByDescending { it.id }
    }
}
