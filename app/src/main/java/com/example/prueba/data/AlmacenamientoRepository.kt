package com.example.prueba.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Sube fotos (factura de combustible, evidencia de incidencias) a Firebase Storage, bajo
 * empresas/{empresaId}/... — mismo esquema de rutas que Realtime Database. Devuelve la URL de
 * descarga pública (firmada por Firebase), que es lo que se guarda en Realtime Database junto
 * con el resto del registro (ChecklistPrevio.facturaCombustibleUrl, Incidencia.fotoUrl) para
 * poder mostrar la foto después desde cualquier dispositivo (no solo el que la tomó).
 */
object AlmacenamientoRepository {
    suspend fun subirFoto(empresaId: String, ruta: String, archivoLocal: Uri): String {
        val referencia = FirebaseStorage.getInstance().reference.child("empresas/$empresaId/$ruta")
        referencia.putFile(archivoLocal).await()
        return referencia.downloadUrl.await().toString()
    }
}
