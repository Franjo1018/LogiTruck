package com.example.prueba.data

import com.example.prueba.data.local.Rol
import com.example.prueba.data.remote.RealtimeDb

/**
 * Lee los conductores reales (usuarios con rol = CONDUCTOR) de una empresa desde Realtime
 * Database, para GestionConductoresScreen. No usa Room porque esta lista siempre debe estar
 * al día cuando el admin la abre (a diferencia del perfil propio, no hace falta cachearla
 * offline); se vuelve a leer cada vez que se entra a la pantalla.
 *
 * estado y vehiculoAsignado se calculan a partir del viaje activo real de cada conductor (ver
 * ViajeRepository): si tiene un viaje sin completar se muestra "En ruta" con la placa de ese
 * viaje, si no, "Disponible". licenciaVigente sigue en true por defecto: todavía no hay una
 * tabla de licencias/documentos del conductor conectada.
 */
object ConductorRepository {
    suspend fun listarConductores(empresaId: String): List<Conductor> {
        val snapshot = RealtimeDb.leer("empresas/$empresaId/usuarios")
        val viajeActivoPorUid = ViajeRepository.listarTodos(empresaId)
            .filter { it.estado != EstadoViaje.COMPLETADO }
            .associateBy { it.conductorUid }

        return snapshot.children.mapIndexedNotNull { indice, hijo ->
            val rol = hijo.child("rol").getValue(String::class.java)
            if (rol != Rol.CONDUCTOR.name) return@mapIndexedNotNull null
            val nombre = hijo.child("nombreCompleto").getValue(String::class.java) ?: return@mapIndexedNotNull null
            val usuario = hijo.child("usuario").getValue(String::class.java) ?: ""
            val uid = hijo.key ?: ""
            val viajeActivo = viajeActivoPorUid[uid]
            Conductor(
                id = indice.toLong(),
                nombre = nombre,
                estado = if (viajeActivo != null) EstadoConductor.EN_RUTA else EstadoConductor.DISPONIBLE,
                licenciaVigente = true,
                vehiculoAsignado = viajeActivo?.vehiculoPlaca?.takeIf { it.isNotBlank() },
                uid = uid,
                usuario = usuario
            )
        }
    }
}
