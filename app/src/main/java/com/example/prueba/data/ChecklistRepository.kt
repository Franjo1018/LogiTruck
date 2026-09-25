package com.example.prueba.data

import com.example.prueba.data.remote.RealtimeDb

/**
 * Checklist previaje de cada viaje, en Realtime Database bajo
 * empresas/{empresaId}/viajes/{viajeIdReal}/checklist. Se guarda una sola vez, cuando el
 * conductor toca "Iniciar viaje" en ChecklistPreviajeScreen (ítems de inspección marcados,
 * kilometraje de salida y si adjuntó foto de la factura de la carga inicial de combustible).
 * La carga de combustible en sí NO se guarda aquí: se registra como un RegistroCombustible
 * normal (vía CombustibleRepository), ligada al mismo viajeIdReal, para que quede junto con
 * las demás cargas del viaje y se sume igual en el reporte de huella de carbono.
 */
object ChecklistRepository {
    suspend fun guardar(
        empresaId: String,
        viajeIdReal: String,
        items: Map<String, Boolean>,
        kilometrajeInicial: String,
        facturaCombustibleAdjunta: Boolean,
        // URL de Firebase Storage de la foto real de la factura (subida antes con
        // AlmacenamientoRepository), o vacío si no se tomó/adjuntó foto.
        facturaCombustibleUrl: String = ""
    ) {
        RealtimeDb.escribir(
            "empresas/$empresaId/viajes/$viajeIdReal/checklist",
            mapOf(
                "items" to items,
                "kilometrajeInicial" to kilometrajeInicial,
                "facturaCombustibleAdjunta" to facturaCombustibleAdjunta,
                "facturaCombustibleUrl" to facturaCombustibleUrl,
                "completadoEn" to System.currentTimeMillis()
            )
        )
    }

    suspend fun leer(empresaId: String, viajeIdReal: String): ChecklistPrevio? {
        val snapshot = RealtimeDb.leer("empresas/$empresaId/viajes/$viajeIdReal/checklist")
        if (!snapshot.exists()) return null
        val items = snapshot.child("items").children.associate { hijo ->
            (hijo.key ?: "") to (hijo.value as? Boolean ?: false)
        }
        return ChecklistPrevio(
            items = items,
            kilometrajeInicial = snapshot.child("kilometrajeInicial").getValue(String::class.java) ?: "",
            facturaCombustibleAdjunta = snapshot.child("facturaCombustibleAdjunta").value as? Boolean ?: false,
            completadoEn = (snapshot.child("completadoEn").value as? Number)?.toLong() ?: 0L,
            facturaCombustibleUrl = snapshot.child("facturaCombustibleUrl").getValue(String::class.java) ?: ""
        )
    }
}
