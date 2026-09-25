@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.admin

import com.example.prueba.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.PlomoMedio
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck
import kotlinx.coroutines.launch

/**
 * Lista de vehículos reales de la empresa (empresas/{empresaId}/vehiculos vía
 * VehiculoRepository), con estado y próximo mantenimiento. Documentos (SOAT, revisión técnica)
 * todavía no tienen un formulario de carga, así que esa parte queda vacía por ahora.
 *
 * El botón "+" abre RegistrarVehiculoScreen para dar de alta un vehículo nuevo. Tocar un
 * vehículo ya registrado abre un diálogo para asignarle (o quitarle) un conductor de la lista
 * de conductores de la empresa.
 */
@Composable
fun GestionVehiculosScreen(
    empresaId: String,
    onAgregarClick: () -> Unit = {}
) {
    var vehiculos by remember { mutableStateOf<List<Vehiculo>>(emptyList()) }
    var conductores by remember { mutableStateOf<List<Conductor>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var vehiculoParaAsignar by remember { mutableStateOf<Vehiculo?>(null) }
    var asignando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun recargar() {
        vehiculos = VehiculoRepository.listar(empresaId)
        conductores = ConductorRepository.listarConductores(empresaId)
    }

    LaunchedEffect(empresaId) {
        cargando = true
        error = null
        try {
            recargar()
        } catch (e: Exception) {
            error = "No se pudo cargar la lista de vehículos, revisa tu conexión"
        } finally {
            cargando = false
        }
    }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Vehículos") },
                // Deja libre la franja de 56dp donde AdminHostScreen superpone el ícono de
                // menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAgregarClick,
                containerColor = NaranjaLogicTruck,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar vehículo")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                error != null -> Text(
                    error ?: "",
                    color = Color(0xFFB3261E),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )

                vehiculos.isEmpty() -> Text(
                    "Todavía no hay vehículos registrados. Usa el botón + para agregar uno.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(vehiculos) { vehiculo ->
                        val hayDocumentoVencido = vehiculo.documentos.any { !it.vigente }
                        val colorFranja = when {
                            hayDocumentoVencido -> MaterialTheme.colorScheme.error
                            vehiculo.conductorAsignadoNombre != null -> NaranjaLogicTruck
                            else -> PlomoMedio.copy(alpha = 0.4f)
                        }
                        Card(
                            onClick = { vehiculoParaAsignar = vehiculo },
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
                                        Text("${vehiculo.placa} · ${vehiculo.modelo}", fontWeight = FontWeight.Medium)
                                        AssistChip(onClick = {}, label = { Text(vehiculo.estado.etiqueta) })
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        vehiculo.conductorAsignadoNombre?.let { "Asignado a $it" }
                                            ?: "Sin conductor asignado",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
        }
    }

    val vehiculoDialogo = vehiculoParaAsignar
    if (vehiculoDialogo != null) {
        AlertDialog(
            onDismissRequest = { if (!asignando) vehiculoParaAsignar = null },
            title = { Text("Asignar conductor a ${vehiculoDialogo.placa}") },
            text = {
                if (conductores.isEmpty()) {
                    Text("No hay conductores en la empresa todavía. Invita a uno primero desde \"Conductores\".")
                } else {
                    Column {
                        conductores.forEach { conductor ->
                            val yaAsignado = conductor.uid.isNotBlank() && conductor.uid == vehiculoDialogo.conductorAsignadoUid
                            TextButton(
                                onClick = {
                                    asignando = true
                                    scope.launch {
                                        try {
                                            VehiculoRepository.asignarConductor(
                                                empresaId, vehiculoDialogo.idReal, conductor.uid, conductor.nombre
                                            )
                                            recargar()
                                            vehiculoParaAsignar = null
                                        } catch (e: Exception) {
                                            // Se queda en el diálogo para reintentar.
                                        } finally {
                                            asignando = false
                                        }
                                    }
                                },
                                enabled = !asignando
                            ) {
                                Text(if (yaAsignado) "${conductor.nombre} (asignado)" else conductor.nombre)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        asignando = true
                        scope.launch {
                            try {
                                VehiculoRepository.asignarConductor(empresaId, vehiculoDialogo.idReal, null, null)
                                recargar()
                            } finally {
                                asignando = false
                                vehiculoParaAsignar = null
                            }
                        }
                    },
                    enabled = !asignando && vehiculoDialogo.conductorAsignadoUid != null
                ) { Text("Quitar asignación") }
            },
            dismissButton = {
                TextButton(onClick = { vehiculoParaAsignar = null }, enabled = !asignando) { Text("Cerrar") }
            }
        )
    }
}
