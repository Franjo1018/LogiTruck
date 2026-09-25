package com.example.prueba.data

import com.example.prueba.data.remote.RealtimeDb

/**
 * Viajes asignados por el admin/despachador, en Realtime Database bajo
 * empresas/{empresaId}/viajes/{idReal}. Empieza vacío: se llena cuando el admin usa
 * CrearViajeScreen. El conductor solo puede usar el checklist previaje cuando tiene un viaje
 * "activo" (PROGRAMADO o EN_RUTA); ver [viajeActivoPorConductor].
 */
object ViajeRepository {
    suspend fun crear(
        empresaId: String,
        conductorUid: String,
        conductorNombre: String,
        vehiculoIdReal: String,
        vehiculoPlaca: String,
        origen: String,
        destino: String,
        fecha: String,
        hora: String,
        descripcionCarga: String,
        pesoCargaKg: String
    ) {
        val idReal = RealtimeDb.push("empresas/$empresaId/viajes")
        RealtimeDb.escribir(
            "empresas/$empresaId/viajes/$idReal",
            mapOf(
                "conductorUid" to conductorUid,
                "conductorNombre" to conductorNombre,
                "vehiculoIdReal" to vehiculoIdReal,
                "vehiculoPlaca" to vehiculoPlaca,
                "origen" to origen,
                "destino" to destino,
                "fecha" to fecha,
                "hora" to hora,
                "descripcionCarga" to descripcionCarga,
                "pesoCargaKg" to pesoCargaKg,
                "estado" to EstadoViaje.PROGRAMADO.name,
                "timestampMs" to System.currentTimeMillis()
            )
        )
    }

    /** Todos los viajes de la empresa (cualquier conductor), para el panel de administrador. */
    suspend fun listarTodos(empresaId: String): List<Viaje> {
        val snapshot = RealtimeDb.leer("empresas/$empresaId/viajes")
        return snapshot.children.mapNotNull { hijo ->
            val estado = hijo.child("estado").getValue(String::class.java)?.let {
                runCatching { EstadoViaje.valueOf(it) }.getOrNull()
            } ?: EstadoViaje.PROGRAMADO
            Viaje(
                idReal = hijo.key ?: "",
                conductorUid = hijo.child("conductorUid").getValue(String::class.java) ?: "",
                conductorNombre = hijo.child("conductorNombre").getValue(String::class.java) ?: "",
                vehiculoIdReal = hijo.child("vehiculoIdReal").getValue(String::class.java) ?: "",
                vehiculoPlaca = hijo.child("vehiculoPlaca").getValue(String::class.java) ?: "",
                origen = hijo.child("origen").getValue(String::class.java) ?: "",
                destino = hijo.child("destino").getValue(String::class.java) ?: "",
                fecha = hijo.child("fecha").getValue(String::class.java) ?: "",
                hora = hijo.child("hora").getValue(String::class.java) ?: "",
                descripcionCarga = hijo.child("descripcionCarga").getValue(String::class.java) ?: "",
                pesoCargaKg = hijo.child("pesoCargaKg").getValue(String::class.java) ?: "",
                estado = estado,
                timestampMs = (hijo.child("timestampMs").value as? Number)?.toLong() ?: 0L
            )
        }.sortedByDescending { it.timestampMs }
    }

    suspend fun listarPorConductor(empresaId: String, conductorUid: String): List<Viaje> {
        return listarTodos(empresaId).filter { it.conductorUid == conductorUid }
    }

    /** El viaje más reciente del conductor que todavía no está COMPLETADO, o null si no tiene ninguno. */
    suspend fun viajeActivoPorConductor(empresaId: String, conductorUid: String): Viaje? {
        return listarPorConductor(empresaId, conductorUid).firstOrNull { it.estado != EstadoViaje.COMPLETADO }
    }

    suspend fun marcarEstado(empresaId: String, idReal: String, estado: EstadoViaje) {
        RealtimeDb.escribir("empresas/$empresaId/viajes/$idReal/estado", estado.name)
    }
}
