package com.logictruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Formulario para un nuevo reporte de incidencia durante el viaje activo. */
data class ReporteIncidenciaForm(
    val descripcion: String = "",
    val severidad: SeveridadIncidencia = SeveridadIncidencia.LEVE,
    val fotoAdjunta: Boolean = false
)

/**
 * Reporte de incidencia (avería, accidente, retraso, etc.) durante el viaje activo.
 * TODO: al enviar, insertar en la tabla de incidencias ligada al VIAJE y, si hay conexión,
 * notificar en tiempo real al despachador; si no hay señal, encolar para sincronizar después
 * (ver IndicadorSincronizacionOffline).
 */
@Composable
fun ReporteIncidenciaScreen(
    origenDestino: String = "Lima → Trujillo",
    onEnviarClick: (ReporteIncidenciaForm) -> Unit = {},
    onTomarFotoClick: () -> Unit = {}
) {
    var form by remember { mutableStateOf(ReporteIncidenciaForm()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reportar incidencia") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(origenDestino, style = MaterialTheme.typography.bodyMedium)

            Text("Severidad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeveridadIncidencia.entries.forEach { severidad ->
                    FilterChip(
                        selected = form.severidad == severidad,
                        onClick = { form = form.copy(severidad = severidad) },
                        label = { Text(severidad.etiqueta) }
                    )
                }
            }

            OutlinedTextField(
                value = form.descripcion,
                onValueChange = { form = form.copy(descripcion = it) },
                label = { Text("Descripción de lo ocurrido") },
                placeholder = { Text("Ej: llanta baja a la altura de Chimbote") },
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )

            OutlinedButton(
                onClick = {
                    form = form.copy(fotoAdjunta = true)
                    onTomarFotoClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (form.fotoAdjunta) "Foto adjuntada" else "Adjuntar foto de evidencia")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onEnviarClick(form) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = form.descripcion.isNotBlank()
            ) {
                Text("Enviar reporte", fontWeight = FontWeight.Medium)
            }
        }
    }
}
