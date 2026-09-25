package com.example.prueba.data

import com.example.prueba.data.remote.RealtimeDb

/**
 * Última ubicación GPS conocida de cada conductor, en Realtime Database bajo
 * empresas/{empresaId}/ubicaciones/{conductorUid} — un solo nodo por conductor que se
 * sobrescribe con cada actualización (no un historial de puntos), para que FlotaMapaScreen
 * siempre lea la posición más reciente de cada camión. Empieza vacío hasta que el conductor
 * abre la app y su ubicación se empieza a reportar (ver ConductorHostScreen).
 *
 * Por privacidad, el conductor solo reporta (y por lo tanto el admin solo puede ver) su
 * ubicación mientras tiene un viaje activo: ConductorHostScreen deja de pedir actualizaciones
 * de GPS y llama a [eliminar] en cuanto detecta que ya no hay un viaje en curso, así ningún
 * conductor queda "visible" en el mapa fuera de un viaje.
 */
object UbicacionRepository {
    suspend fun actualizar(empresaId: String, conductorUid: String, nombre: String, lat: Double, lon: Double) {
        RealtimeDb.escribir(
            "empresas/$empresaId/ubicaciones/$conductorUid",
            mapOf(
                "nombre" to nombre,
                "lat" to lat,
                "lon" to lon,
                "timestampMs" to System.currentTimeMillis()
            )
        )
    }

    /** Borra la última ubicación conocida de un conductor (se llama cuando ya no tiene un viaje activo). */
    suspend fun eliminar(empresaId: String, conductorUid: String) {
        RealtimeDb.escribir("empresas/$empresaId/ubicaciones/$conductorUid", null)
    }

    suspend fun listar(empresaId: String): List<UbicacionConductor> {
        val snapshot = RealtimeDb.leer("empresas/$empresaId/ubicaciones")
        return snapshot.children.mapNotNull { hijo ->
            val lat = (hijo.child("lat").value as? Number)?.toDouble() ?: return@mapNotNull null
            val lon = (hijo.child("lon").value as? Number)?.toDouble() ?: return@mapNotNull null
            UbicacionConductor(
                conductorUid = hijo.key ?: "",
                nombre = hijo.child("nombre").getValue(String::class.java) ?: "",
                lat = lat,
                lon = lon,
                timestampMs = (hijo.child("timestampMs").value as? Number)?.toLong() ?: 0L
            )
        }
    }
}
