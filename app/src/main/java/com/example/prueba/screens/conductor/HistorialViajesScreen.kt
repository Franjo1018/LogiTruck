@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.conductor

import com.example.prueba.data.EstadoViaje
import com.example.prueba.data.ViajeRepository

import androidx.compose.foundation.background
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
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.PlomoMedio
import com.example.prueba.components.coloresChipNaranja
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck

data class ViajeHistorialItem(
    val id: Long,
    val origenDestino: String,
    val fecha: String,
    val estado: EstadoViaje
)

private enum class FiltroHistorial(val etiqueta: String) { TODOS("Todos"), COMPLETADOS("Completados"), EN_CURSO("En curso") }

/**
 * Historial de viajes del conductor autenticado, leído en tiempo real desde Firebase
 * (empresas/{empresaId}/viajes vía ViajeRepository, filtrado por conductorUid). Empieza vacío
 * hasta que el despachador le asigne al menos un viaje.
 */
@Composable
fun HistorialViajesScreen(empresaId: String = "", conductorUid: String = "") {
    var viajes by remember { mutableStateOf<List<ViajeHistorialItem>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var filtro by remember { mutableStateOf(FiltroHistorial.TODOS) }

    LaunchedEffect(empresaId, conductorUid) {
        cargando = true
        try {
            viajes = ViajeRepository.listarPorConductor(empresaId, conductorUid).map {
                ViajeHistorialItem(
                    id = it.timestampMs,
                    origenDestino = "${it.origen} → ${it.destino}",
                    fecha = it.fecha,
                    estado = it.estado
                )
            }
        } catch (e: Exception) {
            // Sin conexión: se muestra el historial vacío.
        } finally {
            cargando = false
        }
    }

    val viajesFiltrados = when (filtro) {
        FiltroHistorial.TODOS -> viajes
        FiltroHistorial.COMPLETADOS -> viajes.filter { it.estado == EstadoViaje.COMPLETADO }
        FiltroHistorial.EN_CURSO -> viajes.filter { it.estado != EstadoViaje.COMPLETADO }
    }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Historial de viajes") },
                // Deja libre la franja de 56dp donde ConductorHostScreen superpone el ícono
                // de menú (☰), para que no quede debajo del título.
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
                items(FiltroHistorial.entries) { opcion ->
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
                viajesFiltrados.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Todavía no tienes viajes en tu historial.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viajesFiltrados) { viaje ->
                        val colorFranja = if (viaje.estado == EstadoViaje.COMPLETADO) PlomoMedio.copy(alpha = 0.4f) else NaranjaLogicTruck
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(colorFranja))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(viaje.origenDestino, fontWeight = FontWeight.Medium)
                                        Text(viaje.fecha, style = MaterialTheme.typography.bodySmall)
                                    }
                                    AssistChip(onClick = {}, label = { Text(viaje.estado.etiqueta) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
