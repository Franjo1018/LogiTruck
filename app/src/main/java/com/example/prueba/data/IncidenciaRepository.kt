package com.example.prueba.data

import com.example.prueba.data.remote.RealtimeDb
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reportes de incidencia (avería, accidente, retraso, etc.) de la empresa, en Realtime
 * Database bajo empresas/{empresaId}/incidencias/{idReal}. Empieza vacío: no hay datos de
 * ejemplo, solo lo que cada conductor va reportando desde ReporteIncidenciaScreen.
 */
object IncidenciaRepository {
    private val formatoFecha = SimpleDateFormat("dd/MM", Locale.getDefault())

    /**
     * Genera el id real de una incidencia nueva antes de guardarla, para poder subir su foto
     * de evidencia a Firebase Storage (AlmacenamientoRepository, bajo ese mismo id) antes de
     * escribir el registro completo con [crear].
     */
    suspend fun crearId(empresaId: String): String = RealtimeDb.push("empresas/$empresaId/incidencias")

    suspend fun crear(
        empresaId: String,
        idReal: String,
        conductorUid: String,
        conductorNombre: String,
        viajeOrigenDestino: String,
        descripcion: String,
        severidad: SeveridadIncidencia,
        // URL de descarga en Firebase Storage de la foto de evidencia (ver
        // AlmacenamientoRepository), o vacío si no se adjuntó foto.
        fotoUrl: String = ""
    ) {
        val ahora = System.currentTimeMillis()
        RealtimeDb.escribir(
            "empresas/$empresaId/incidencias/$idReal",
            mapOf(
                "conductorUid" to conductorUid,
                "conductor" to conductorNombre,
                "viajeOrigenDestino" to viajeOrigenDestino.ifBlank { "Sin ruta asignada" },
                "descripcion" to descripcion,
                "severidad" to severidad.name,
                "fecha" to formatoFecha.format(Date(ahora)),
                "resuelta" to false,
                "fotoUrl" to fotoUrl,
                "timestampMs" to ahora
            )
        )
    }

    suspend fun listar(empresaId: String): List<Incidencia> {
        val snapshot = RealtimeDb.leer("empresas/$empresaId/incidencias")
        return snapshot.children.mapNotNull { hijo ->
            val descripcion = hijo.child("descripcion").getValue(String::class.java) ?: return@mapNotNull null
            val severidad = hijo.child("severidad").getValue(String::class.java)?.let {
                runCatching { SeveridadIncidencia.valueOf(it) }.getOrNull()
            } ?: SeveridadIncidencia.LEVE
            val timestamp = (hijo.child("timestampMs").value as? Number)?.toLong() ?: 0L
            Incidencia(
                id = timestamp,
                viajeOrigenDestino = hijo.child("viajeOrigenDestino").getValue(String::class.java) ?: "",
                conductor = hijo.child("conductor").getValue(String::class.java) ?: "",
                descripcion = descripcion,
                severidad = severidad,
                fecha = hijo.child("fecha").getValue(String::class.java) ?: "",
                resuelta = hijo.child("resuelta").getValue(Boolean::class.java) ?: false,
                idReal = hijo.key ?: "",
                conductorUid = hijo.child("conductorUid").getValue(String::class.java) ?: "",
                fotoUrl = hijo.child("fotoUrl").getValue(String::class.java) ?: ""
            )
        }.sortedByDescending { it.id }
    }

    suspend fun marcarResuelta(empresaId: String, idReal: String, resuelta: Boolean) {
        RealtimeDb.escribir("empresas/$empresaId/incidencias/$idReal/resuelta", resuelta)
    }
}
