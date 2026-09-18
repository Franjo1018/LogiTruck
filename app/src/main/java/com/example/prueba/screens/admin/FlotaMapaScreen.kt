package com.logictruck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class VehiculoEnMapa(
    val placa: String,
    val conductor: String,
    val ubicacionTexto: String, // p.ej. "Panamericana Sur, km 210"
    val estado: EstadoViaje,
    val ultimaActualizacion: String
)

private val flotaDemo = listOf(
    VehiculoEnMapa("T4X-882", "Carlos Mendoza", "Panamericana Sur, km 210", EstadoViaje.EN_RUTA, "hace 2 min"),
    VehiculoEnMapa("V2P-014", "Rosa Salinas", "Terminal Lima Norte", EstadoViaje.PROGRAMADO, "hace 8 min"),
    VehiculoEnMapa("W9L-330", "Jorge Huamán", "Ica, desvío km 300", EstadoViaje.EN_RUTA, "hace 1 min")
)

/**
 * Vista de mapa con la ubicación GPS de todos los camiones activos.
 * TODO: reemplazar el placeholder por Google Maps Compose, alimentado por la tabla UBICACION
 * vía polling o Firebase Realtime Database para actualizaciones en vivo.
 */
@Composable
fun FlotaMapaScreen(
    vehiculos: List<VehiculoEnMapa> = flotaDemo,
    onVehiculoClick: (VehiculoEnMapa) -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Flota en tiempo real") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = "Mapa de flota",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                "Vehículos activos (${vehiculos.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
            )

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vehiculos) { vehiculo ->
                    OutlinedCard(onClick = { onVehiculoClick(vehiculo) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${vehiculo.placa} · ${vehiculo.conductor}", fontWeight = FontWeight.Medium)
                                Text(vehiculo.ubicacionTexto, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Actualizado ${vehiculo.ultimaActualizacion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(onClick = {}, label = { Text(vehiculo.estado.etiqueta) })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
