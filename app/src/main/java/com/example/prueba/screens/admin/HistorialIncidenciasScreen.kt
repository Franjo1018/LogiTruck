@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.admin

import com.example.prueba.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.PlomoMedio
import com.example.prueba.components.coloresChipNaranja
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck

private enum class FiltroIncidencia(val etiqueta: String) {
    TODAS("Todas"), ABIERTAS("Abiertas"), RESUELTAS("Resueltas")
}

/**
 * Lista filtrable de todos los reportes de incidencia de la flota, leída en tiempo real (al
 * entrar a la pantalla) desde Firebase vía IncidenciaRepository. Empieza vacía hasta que los
 * conductores reporten incidencias desde ReporteIncidenciaScreen. Tocar una tarjeta abre el chat
 * con ese conductor (onIncidenciaClick, manejado por AdminHostScreen): ahí se conversa sobre lo
 * ocurrido y recién ahí se marca como resuelta (ver ConversacionChatScreen), no desde esta lista.
 */
@Composable
fun HistorialIncidenciasScreen(empresaId: String, onIncidenciaClick: (Incidencia) -> Unit = {}) {
    var incidencias by remember { mutableStateOf<List<Incidencia>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filtro by remember { mutableStateOf(FiltroIncidencia.TODAS) }

    suspend fun recargar() {
        incidencias = IncidenciaRepository.listar(empresaId)
    }

    LaunchedEffect(empresaId) {
        cargando = true
        error = null
        try {
            recargar()
        } catch (e: Exception) {
            error = "No se pudo cargar el historial de incidencias, revisa tu conexión"
        } finally {
            cargando = false
        }
    }

    val incidenciasFiltradas = when (filtro) {
        FiltroIncidencia.TODAS -> incidencias
        FiltroIncidencia.ABIERTAS -> incidencias.filter { !it.resuelta }
        FiltroIncidencia.RESUELTAS -> incidencias.filter { it.resuelta }
    }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Historial de incidencias") },
                // Deja libre la franja de 56dp donde AdminHostScreen superpone el ícono de
                // menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
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
                        label = { Text(opcion.etiqueta) },
                        colors = coloresChipNaranja()
                    )
                }
            }

            when {
                cargando -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
                }

                incidenciasFiltradas.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Todavía no hay incidencias reportadas.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(incidenciasFiltradas) { incidencia ->
                        val colorFranja = when (incidencia.severidad) {
                            SeveridadIncidencia.GRAVE -> NaranjaLogicTruck
                            SeveridadIncidencia.MODERADA -> PlomoMedio
                            SeveridadIncidencia.LEVE -> PlomoMedio.copy(alpha = 0.4f)
                        }
                        Card(
                            onClick = { onIncidenciaClick(incidencia) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(colorFranja))
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(incidencia.viajeOrigenDestino, fontWeight = FontWeight.Medium)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (incidencia.fotoUrl.isNotBlank()) {
                                                Icon(
                                                    Icons.Filled.CameraAlt,
                                                    contentDescription = "Tiene foto de evidencia",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(end = 6.dp).size(16.dp)
                                                )
                                            }
                                            SeveridadBadge(incidencia.severidad)
                                        }
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
