@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.conductor

import com.example.prueba.data.AlmacenamientoRepository
import com.example.prueba.data.IncidenciaRepository
import com.example.prueba.data.SeveridadIncidencia

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prueba.components.BotonTomarFoto
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresChipNaranja
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck
import kotlinx.coroutines.launch

/**
 * Formulario para un nuevo reporte de incidencia durante el viaje activo. fotoUri es la foto
 * real tomada con la cámara (BotonTomarFoto), todavía solo local: se sube a Firebase Storage
 * recién al enviar el reporte (ver AlmacenamientoRepository), para no subir fotos de reportes
 * que el conductor termina descartando antes de enviar.
 */
data class ReporteIncidenciaForm(
    val descripcion: String = "",
    val severidad: SeveridadIncidencia = SeveridadIncidencia.LEVE,
    val fotoUri: Uri? = null
)

/**
 * Reporte de incidencia (avería, accidente, retraso, etc.) durante el viaje activo. Al enviar,
 * se guarda en Firebase (empresas/{empresaId}/incidencias vía IncidenciaRepository) y aparece
 * de inmediato en HistorialIncidenciasScreen del lado admin, con la foto de evidencia si se
 * adjuntó una (visible ahí y en el chat de la incidencia, ConversacionChatScreen). origenDestino
 * es texto libre, igual que en ChecklistPreviajeScreen: cualquier ruta entre provincias o
 * departamentos.
 */
@Composable
fun ReporteIncidenciaScreen(
    empresaId: String = "",
    conductorUid: String = "",
    conductorNombre: String = "",
    origenDestino: String = "Sin ruta asignada"
) {
    var form by remember { mutableStateOf(ReporteIncidenciaForm()) }
    var enviando by remember { mutableStateOf(false) }
    var enviado by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Reportar incidencia") },
                // Deja libre la franja de 56dp donde ConductorHostScreen superpone el ícono
                // de menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
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
                        label = { Text(severidad.etiqueta) },
                        colors = coloresChipNaranja()
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

            BotonTomarFoto(
                fotoUri = form.fotoUri,
                onFotoCapturada = { uri -> form = form.copy(fotoUri = uri) },
                etiqueta = "Adjuntar foto de evidencia"
            )

            if (enviado) {
                Text(
                    "Reporte enviado. El despachador ya puede verlo.",
                    color = NaranjaLogicTruck,
                    fontWeight = FontWeight.Medium
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    error = null
                    enviando = true
                    scope.launch {
                        try {
                            val idReal = IncidenciaRepository.crearId(empresaId)
                            var fotoUrl = ""
                            form.fotoUri?.let { uri ->
                                fotoUrl = AlmacenamientoRepository.subirFoto(
                                    empresaId = empresaId,
                                    ruta = "incidencias/$idReal.jpg",
                                    archivoLocal = uri
                                )
                            }
                            IncidenciaRepository.crear(
                                empresaId = empresaId,
                                idReal = idReal,
                                conductorUid = conductorUid,
                                conductorNombre = conductorNombre,
                                viajeOrigenDestino = origenDestino,
                                descripcion = form.descripcion,
                                severidad = form.severidad,
                                fotoUrl = fotoUrl
                            )
                            form = ReporteIncidenciaForm()
                            enviado = true
                        } catch (e: Exception) {
                            error = "No se pudo enviar el reporte, revisa tu conexión"
                        } finally {
                            enviando = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = form.descripcion.isNotBlank() && !enviando,
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaLogicTruck)
            ) {
                Text(if (enviando) "Enviando..." else "Enviar reporte", fontWeight = FontWeight.Medium)
            }
        }
    }
}
