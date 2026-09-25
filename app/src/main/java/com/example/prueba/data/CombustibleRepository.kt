package com.example.prueba.data

import com.example.prueba.data.remote.RealtimeDb
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registros de combustible cargado, en Realtime Database bajo
 * empresas/{empresaId}/combustible/{idReal}. Se mide en galones (no litros), que es como se
 * factura normalmente el combustible en Perú. Alimenta tanto el historial del conductor
 * (RegistroCombustibleScreen) como el cálculo real de huella de carbono del admin
 * (ReporteHuellaCarbonoScreen). Empieza vacío, sin datos de ejemplo.
 */
object CombustibleRepository {
    private val formatoHora = SimpleDateFormat("h:mm a", Locale("es", "PE"))
    private val formatoFecha = SimpleDateFormat("dd/MM", Locale.getDefault())
    private val formatoMes = SimpleDateFormat("MMM", Locale("es", "PE"))

    /** Kg de CO2 emitidos por galón de diésel quemado (factor estándar EPA, ~10.21 kg CO2/galón). */
    const val FACTOR_KG_CO2_POR_GALON = 10.21

    suspend fun registrar(
        empresaId: String,
        conductorUid: String,
        conductorNombre: String,
        galones: Double,
        // Opcional: la carga inicial declarada en el checklist previaje normalmente no tiene
        // boleta/factura en el momento (se adjunta la foto, no siempre el monto exacto), así que
        // se permite guardar sin costo. Las recargas en ruta (RegistroCombustibleScreen) sí lo piden.
        costo: Double = 0.0,
        lugar: String,
        // Viaje al que pertenece esta carga (puede haber más de una por viaje: la inicial del
        // checklist y luego recargas en ruta). Vacío solo se usa si no hay viaje activo.
        viajeIdReal: String = ""
    ) {
        val idReal = RealtimeDb.push("empresas/$empresaId/combustible")
        val ahora = System.currentTimeMillis()
        RealtimeDb.escribir(
            "empresas/$empresaId/combustible/$idReal",
            mapOf(
                "conductorUid" to conductorUid,
                "conductorNombre" to conductorNombre,
                "galones" to galones,
                "costo" to costo,
                "lugar" to lugar,
                "viajeIdReal" to viajeIdReal,
                "hora" to formatoHora.format(Date(ahora)),
                "fecha" to formatoFecha.format(Date(ahora)),
                "mesEtiqueta" to formatoMes.format(Date(ahora)).replaceFirstChar { it.uppercase() },
                "timestampMs" to ahora
            )
        )
    }

    suspend fun listarTodos(empresaId: String): List<RegistroCombustible> {
        val snapshot = RealtimeDb.leer("empresas/$empresaId/combustible")
        return snapshot.children.mapNotNull { hijo ->
            val galones = (hijo.child("galones").value as? Number)?.toDouble() ?: return@mapNotNull null
            val timestamp = (hijo.child("timestampMs").value as? Number)?.toLong() ?: 0L
            RegistroCombustible(
                galones = galones,
                costo = (hijo.child("costo").value as? Number)?.toDouble() ?: 0.0,
                lugar = hijo.child("lugar").getValue(String::class.java) ?: "",
                hora = hijo.child("hora").getValue(String::class.java) ?: "",
                idReal = hijo.key ?: "",
                fecha = hijo.child("fecha").getValue(String::class.java) ?: "",
                conductorUid = hijo.child("conductorUid").getValue(String::class.java) ?: "",
                conductorNombre = hijo.child("conductorNombre").getValue(String::class.java) ?: "",
                viajeIdReal = hijo.child("viajeIdReal").getValue(String::class.java) ?: ""
            ).let { it to timestamp }
        }.sortedByDescending { it.second }.map { it.first }
    }

    suspend fun listarPorConductor(empresaId: String, conductorUid: String): List<RegistroCombustible> {
        return listarTodos(empresaId).filter { it.conductorUid == conductorUid }
    }

    suspend fun listarPorViaje(empresaId: String, viajeIdReal: String): List<RegistroCombustible> {
        return listarTodos(empresaId).filter { it.viajeIdReal == viajeIdReal }
    }
}
