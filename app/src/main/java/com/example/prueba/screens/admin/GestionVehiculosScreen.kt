package com.logictruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val vehiculosDemo = listOf(
    Vehiculo(
        id = 1,
        placa = "T4X-882",
        modelo = "Volvo FH",
        estado = EstadoVehiculo.OPERATIVO,
        documentos = listOf(
            DocumentoVehiculo("SOAT", true, "12/2026"),
            DocumentoVehiculo("Revisión técnica", false, "08/2026")
        ),
        proximoMantenimientoKm = 4200
    ),
    Vehiculo(
        id = 2,
        placa = "V2P-014",
        modelo = "Scania R450",
        estado = EstadoVehiculo.EN_MANTENIMIENTO,
        documentos = listOf(
            DocumentoVehiculo("SOAT", true, "03/2027"),
            DocumentoVehiculo("Revisión técnica", true, "11/2026")
        ),
        proximoMantenimientoKm = 0
    )
)

/**
 * Lista de camiones con estado operativo, próximo mantenimiento y vigencia de documentos.
 * TODO: alimentar desde VehiculoRepository (tabla VEHICULO).
 */
@Composable
fun GestionVehiculosScreen(
    vehiculos: List<Vehiculo> = vehiculosDemo,
    onVehiculoClick: (Vehiculo) -> Unit = {},
    onAgregarClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Vehículos") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAgregarClick) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar vehículo")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(vehiculos) { vehiculo ->
                OutlinedCard(onClick = { onVehiculoClick(vehiculo) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${vehiculo.placa} · ${vehiculo.modelo}", fontWeight = FontWeight.Medium)
                            AssistChip(onClick = {}, label = { Text(vehiculo.estado.etiqueta) })
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (vehiculo.proximoMantenimientoKm in 1..5000) {
                            Text(
                                "Mantenimiento en ${vehiculo.proximoMantenimientoKm} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        vehiculo.documentos.forEach { doc ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!doc.vigente) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    "${doc.nombre}: ${if (doc.vigente) "vigente hasta" else "vencido"} ${doc.vencimiento}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (doc.vigente) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
