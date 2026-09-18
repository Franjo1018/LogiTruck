package com.logictruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ViajeHistorialItem(
    val id: Long,
    val origenDestino: String,
    val fecha: String,
    val estado: EstadoViaje,
    val kilometros: Int
)

private val historialDemo = listOf(
    ViajeHistorialItem(1, "Lima → Trujillo", "17/09", EstadoViaje.EN_RUTA, 560),
    ViajeHistorialItem(2, "Lima → Arequipa", "10/09", EstadoViaje.COMPLETADO, 1010),
    ViajeHistorialItem(3, "Lima → Ica", "05/09", EstadoViaje.COMPLETADO, 300),
    ViajeHistorialItem(4, "Lima → Chiclayo", "28/08", EstadoViaje.COMPLETADO, 770)
)

private enum class FiltroHistorial(val etiqueta: String) { TODOS("Todos"), COMPLETADOS("Completados"), EN_CURSO("En curso") }

/**
 * Historial de viajes del conductor autenticado (tabla VIAJE filtrada por conductor_id).
 * TODO: paginar desde Room (PagingSource) cuando el historial crezca más allá de unas decenas de viajes.
 */
@Composable
fun HistorialViajesScreen(
    viajes: List<ViajeHistorialItem> = historialDemo,
    onViajeClick: (ViajeHistorialItem) -> Unit = {}
) {
    var filtro by remember { mutableStateOf(FiltroHistorial.TODOS) }

    val viajesFiltrados = when (filtro) {
        FiltroHistorial.TODOS -> viajes
        FiltroHistorial.COMPLETADOS -> viajes.filter { it.estado == EstadoViaje.COMPLETADO }
        FiltroHistorial.EN_CURSO -> viajes.filter { it.estado != EstadoViaje.COMPLETADO }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Historial de viajes") }) }
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
                        label = { Text(opcion.etiqueta) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viajesFiltrados) { viaje ->
                    OutlinedCard(onClick = { onViajeClick(viaje) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(viaje.origenDestino, fontWeight = FontWeight.Medium)
                                Text(
                                    "${viaje.fecha} · ${viaje.kilometros} km",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            AssistChip(onClick = {}, label = { Text(viaje.estado.etiqueta) })
                        }
                    }
                }
            }
        }
    }
}
