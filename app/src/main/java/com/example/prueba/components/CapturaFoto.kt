package com.example.prueba.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

/**
 * Toma la foto con la cámara real del dispositivo (reemplaza los flags true/false de "foto
 * adjuntada" que había antes en ChecklistPreviajeScreen y ReporteIncidenciaScreen). Pide el
 * permiso de cámara si hace falta, crea un archivo temporal en cache/fotos/ (ver
 * res/xml/file_paths.xml), lanza la app de cámara del sistema para que escriba la foto ahí, y
 * al terminar entrega su Uri local. Subirla a Firebase Storage (AlmacenamientoRepository) es
 * responsabilidad de quien use este botón, recién cuando se envía el formulario completo.
 */
private fun crearUriTemporal(context: Context): Uri {
    val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
    val archivo = File.createTempFile("foto_", ".jpg", carpeta)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}

@Composable
private fun rememberLanzadorFoto(onFotoCapturada: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    var uriPendiente by remember { mutableStateOf<Uri?>(null) }

    val lanzadorCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val uri = uriPendiente
        if (exito && uri != null) onFotoCapturada(uri)
    }

    val lanzadorPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            val uri = crearUriTemporal(context)
            uriPendiente = uri
            lanzadorCamara.launch(uri)
        }
    }

    return {
        val permisoConcedido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (permisoConcedido) {
            val uri = crearUriTemporal(context)
            uriPendiente = uri
            lanzadorCamara.launch(uri)
        } else {
            lanzadorPermiso.launch(Manifest.permission.CAMERA)
        }
    }
}

/**
 * Botón grande con vista previa: para adjuntar una sola foto importante (factura de
 * combustible, evidencia de una incidencia). Si ya hay una foto (fotoUri no nulo) se ve la
 * miniatura arriba del botón, y el botón cambia a "Volver a tomar foto".
 */
@Composable
fun BotonTomarFoto(
    fotoUri: Uri?,
    onFotoCapturada: (Uri) -> Unit,
    etiqueta: String = "Adjuntar foto",
    modifier: Modifier = Modifier
) {
    val lanzar = rememberLanzadorFoto(onFotoCapturada)
    Column(modifier = modifier) {
        if (fotoUri != null) {
            AsyncImage(
                model = fotoUri,
                contentDescription = etiqueta,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(onClick = lanzar, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (fotoUri != null) "Volver a tomar foto" else etiqueta)
        }
    }
}

/**
 * Ícono pequeño para adjuntar foto dentro de una fila (los ítems del checklist previaje que
 * requieren evidencia): se pinta de naranja en cuanto hay una foto tomada.
 */
@Composable
fun IconoTomarFoto(
    fotoUri: Uri?,
    onFotoCapturada: (Uri) -> Unit,
    contentDescription: String = "Tomar foto de evidencia"
) {
    val lanzar = rememberLanzadorFoto(onFotoCapturada)
    IconButton(onClick = lanzar) {
        Icon(
            Icons.Filled.CameraAlt,
            contentDescription = contentDescription,
            tint = if (fotoUri != null) NaranjaLogicTruck else MaterialTheme.colorScheme.outline
        )
    }
}
