package com.example.prueba.data

import com.example.prueba.data.remote.RealtimeDb

/**
 * Lee y escribe los vehículos de una empresa en Realtime Database
 * (empresas/{empresaId}/vehiculos/{vehiculoId}). Documentos (SOAT, revisión técnica) todavía
 * no tienen un formulario de carga en la app, así que por ahora siempre vuelven vacíos.
 * TODO: agregar un formulario para cargar/editar los documentos de cada vehículo.
 */
object VehiculoRepository {
    suspend fun listar(empresaId: String): List<Vehiculo> {
        val snapshot = RealtimeDb.leer("empresas/$empresaId/vehiculos")
        return snapshot.children.mapNotNull { hijo ->
            val placa = hijo.child("placa").getValue(String::class.java) ?: return@mapNotNull null
            val modelo = hijo.child("modelo").getValue(String::class.java) ?: ""
            val estado = hijo.child("estado").getValue(String::class.java)?.let {
                runCatching { EstadoVehiculo.valueOf(it) }.getOrNull()
            } ?: EstadoVehiculo.OPERATIVO
            val proximoMantenimientoKm = (hijo.child("proximoMantenimientoKm").value as? Number)?.toInt() ?: 0
            val conductorUid = hijo.child("conductorAsignadoUid").getValue(String::class.java)
            val conductorNombre = hijo.child("conductorAsignadoNombre").getValue(String::class.java)
            Vehiculo(
                id = 0L,
                placa = placa,
                modelo = modelo,
                estado = estado,
                documentos = emptyList(),
                proximoMantenimientoKm = proximoMantenimientoKm,
                idReal = hijo.key ?: "",
                conductorAsignadoUid = conductorUid,
                conductorAsignadoNombre = conductorNombre
            )
        }
    }

    /** Registra un vehículo nuevo en la empresa. */
    suspend fun registrar(empresaId: String, placa: String, modelo: String) {
        val vehiculoId = RealtimeDb.push("empresas/$empresaId/vehiculos")
        RealtimeDb.escribir(
            "empresas/$empresaId/vehiculos/$vehiculoId",
            mapOf(
                "placa" to placa.trim().uppercase(),
                "modelo" to modelo.trim(),
                "estado" to EstadoVehiculo.OPERATIVO.name,
                "proximoMantenimientoKm" to 0
            )
        )
    }

    /**
     * Asigna un conductor a un vehículo ya registrado, o lo desasigna si conductorUid es null.
     */
    suspend fun asignarConductor(
        empresaId: String,
        vehiculoId: String,
        conductorUid: String?,
        conductorNombre: String?
    ) {
        RealtimeDb.escribir("empresas/$empresaId/vehiculos/$vehiculoId/conductorAsignadoUid", conductorUid)
        RealtimeDb.escribir("empresas/$empresaId/vehiculos/$vehiculoId/conductorAsignadoNombre", conductorNombre)
    }
}
