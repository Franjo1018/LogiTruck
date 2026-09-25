@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.admin

import com.example.prueba.data.*

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ItemChecklist(val descripcion: String, val completado: Boolean)

data class EventoViaje(val descripcion: String, val hora: String)

data class ViajeCargaDetalle(
    val origenDestino: String,
    val conductor: String,
    val placa: String,
    val cargaDescripcion: String,
    val pesoCargaKg: Int,
    val estado: EstadoViaje,
    val checklist: List<ItemChecklist>,
    val bitacora: List<EventoViaje>,
    val incidencias: List<Incidencia>,
    val combustible: List<RegistroCombustible>
)

private val detalleAdminDemo = ViajeCargaDetalle(
    origenDestino = "Trujillo → Chiclayo",
    conductor = "Carlos Mendoza",
    placa = "T4X-882",
    cargaDescripcion = "Cemento, 200 sacos",
    pesoCargaKg = 10000,
    estado = EstadoViaje.EN_RUTA,
    checklist = listOf(
        ItemChecklist("Revisión de frenos", true),
        ItemChecklist("Nivel de aceite", true),
        ItemChecklist("Presión de llantas", false)
    ),
    bitacora = listOf(
        EventoViaje("Salida de almacén", "6:05 pm"),
        EventoViaje("Paso por Pacasmayo", "8:40 pm")
    ),
    incidencias = listOf(
        Incidencia(1, "Trujillo → Chiclayo", "Carlos Mendoza", "Llanta baja cerca de Pacasmayo", SeveridadIncidencia.MODERADA, "17/09", false)
    ),
    combustible = listOf(
        RegistroCombustible(80.0, 320.0, "Grifo Primax - Pativilca", "8:50 pm")
    )
)

/**
 * Vista ampliada de un viaje para administrador/despachador: incluye checklist previaje,
 * bitácora, incidencias reportadas y combustible cargado — a diferencia de ViajeDetalleScreen
 * (la vista simplificada de seguimiento que ve el conductor).
 * TODO: alimentar desde TripViewModel combinando VIAJE, UBICACION, y las tablas de checklist/incidencia.
 */
@Composable
fun ViajeCargaDetalleScreen(
    viaje: ViajeCargaDetalle = detalleAdminDemo,
    onVolverClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viaje.origenDestino) },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Conductor", style = MaterialTheme.typography.bodySmall)
                    Text(viaje.conductor, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Vehículo", style = MaterialTheme.typography.bodySmall)
                    Text(viaje.placa, fontWeight = FontWeight.Medium)
                }
                AssistChip(onClick = {}, label = { Text(viaje.estado.etiqueta) })
            }

            Seccion("Carga") {
                Text("${viaje.cargaDescripcion} · ${viaje.pesoCargaKg} kg")
            }

            Seccion("Checklist previaje") {
                viaje.checklist.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (item.completado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(item.descripcion, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Seccion("Bitácora") {
                viaje.bitacora.forEach { evento ->
                    Text("${evento.descripcion} — ${evento.hora}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Seccion("Combustible") {
                viaje.combustible.forEach { registro ->
                    Text(
                        "${registro.galones} gal · S/ ${registro.costo} · ${registro.lugar} (${registro.hora})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Seccion("Incidencias") {
                if (viaje.incidencias.isEmpty()) {
                    Text("Sin incidencias reportadas.", style = MaterialTheme.typography.bodySmall)
                }
                viaje.incidencias.forEach { incidencia ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(incidencia.descripcion, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = contenido)
    }
}
