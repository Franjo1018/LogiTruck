package com.logictruck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val conductoresDemo = listOf(
    Conductor(1, "Carlos Mendoza", EstadoConductor.EN_RUTA, true, "T4X-882"),
    Conductor(2, "Rosa Salinas", EstadoConductor.DISPONIBLE, true, "V2P-014"),
    Conductor(3, "Jorge Huamán", EstadoConductor.DESCANSO, false, null)
)

/**
 * Lista de choferes con su estado, vigencia de licencia y vehículo asignado.
 * TODO: alimentar desde UserRepository (tabla USUARIO filtrada por rol = CONDUCTOR).
 */
@Composable
fun GestionConductoresScreen(
    conductores: List<Conductor> = conductoresDemo,
    onConductorClick: (Conductor) -> Unit = {},
    onAgregarClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Conductores") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAgregarClick) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar conductor")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conductores) { conductor ->
                OutlinedCard(onClick = { onConductorClick(conductor) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iniciales = conductor.nombre.split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .joinToString("")
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(iniciales, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(conductor.nombre, fontWeight = FontWeight.Medium)
                            Text(
                                conductor.vehiculoAsignado ?: "Sin vehículo asignado",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!conductor.licenciaVigente) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Licencia vencida",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        AssistChip(onClick = {}, label = { Text(conductor.estado.etiqueta) })
                    }
                }
            }
        }
    }
}
