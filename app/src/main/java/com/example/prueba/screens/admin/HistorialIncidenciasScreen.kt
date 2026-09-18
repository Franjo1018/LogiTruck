package com.logictruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val incidenciasDemo = listOf(
    Incidencia(1, "Lima → Trujillo", "Carlos Mendoza", "Llanta baja en Chimbote", SeveridadIncidencia.MODERADA, "17/09", false),
    Incidencia(2, "Lima → Arequipa", "Rosa Salinas", "Retraso por bloqueo de vía", SeveridadIncidencia.LEVE, "15/09", true),
    Incidencia(3, "Lima → Ica", "Jorge Huamán", "Falla de frenos, viaje suspendido", SeveridadIncidencia.GRAVE, "12/09", false)
)

private enum class FiltroIncidencia(val etiqueta: String) {
    TODAS("Todas"), ABIERTAS("Abiertas"), RESUELTAS("Resueltas")
}

/**
 * Lista filtrable de todos los reportes de incidencia de la flota.
 * TODO: alimentar desde IncidenciaRepository; el filtro por severidad/estado se puede
 * mover a una consulta Room (DAO) en lugar de filtrar en memoria como aquí.
 */
@Composable
fun HistorialIncidenciasScreen(
    incidencias: List<Incidencia> = incidenciasDemo,
    onIncidenciaClick: (Incidencia) -> Unit = {}
) {
    var filtro by remember { mutableStateOf(FiltroIncidencia.TODAS) }

    val incidenciasFiltradas = when (filtro) {
        FiltroIncidencia.TODAS -> incidencias
        FiltroIncidencia.ABIERTAS -> incidencias.filter { !it.resuelta }
        FiltroIncidencia.RESUELTAS -> incidencias.filter { it.resuelta }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Historial de incidencias") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                modifier = Modifier.padding(16.dp, 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FiltroIncidencia.entries) { opcion ->
                    FilterChip(
                        selected = filtro == opcion,
                        onClick = { filtro = opcion },
                        label = { Text(opcion.etiqueta) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(incidenciasFiltradas) { incidencia ->
                    OutlinedCard(onClick = { onIncidenciaClick(incidencia) }) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(incidencia.viajeOrigenDestino, fontWeight = FontWeight.Medium)
                                SeveridadBadge(incidencia.severidad)
                            }
                            Text(incidencia.descripcion, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${incidencia.conductor} · ${incidencia.fecha} · ${if (incidencia.resuelta) "Resuelta" else "Abierta"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeveridadBadge(severidad: SeveridadIncidencia) {
    val color = when (severidad) {
        SeveridadIncidencia.GRAVE -> MaterialTheme.colorScheme.error
        SeveridadIncidencia.MODERADA -> Color(0xFFB5651D)
        SeveridadIncidencia.LEVE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    AssistChip(
        onClick = {},
        label = { Text(severidad.etiqueta) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color)
    )
}
