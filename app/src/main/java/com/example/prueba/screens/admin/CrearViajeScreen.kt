package com.logictruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Datos capturados al crear un viaje. Corresponde a un nuevo registro en la tabla VIAJE. */
data class NuevoViajeForm(
    val conductorId: Long? = null,
    val vehiculoId: Long? = null,
    val origen: String = "",
    val destino: String = "",
    val fechaSalida: String = "",
    val descripcionCarga: String = "",
    val pesoCargaKg: String = ""
)

private val conductoresDisponiblesDemo = listOf(
    Conductor(1, "Carlos Mendoza", EstadoConductor.DISPONIBLE, true),
    Conductor(2, "Rosa Salinas", EstadoConductor.DISPONIBLE, true),
    Conductor(3, "Jorge Huamán", EstadoConductor.EN_RUTA, true)
)

private val vehiculosDisponiblesDemo = listOf(
    Vehiculo(1, "T4X-882", "Volvo FH", EstadoVehiculo.OPERATIVO, emptyList(), 4200),
    Vehiculo(2, "V2P-014", "Scania R450", EstadoVehiculo.OPERATIVO, emptyList(), 1800)
)

/**
 * Pantalla del despachador para asignar un nuevo viaje.
 * TODO: filtrar conductores/vehículos por disponibilidad real desde Room antes de listar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearViajeScreen(
    conductoresDisponibles: List<Conductor> = conductoresDisponiblesDemo,
    vehiculosDisponibles: List<Vehiculo> = vehiculosDisponiblesDemo,
    onCrearViajeClick: (NuevoViajeForm) -> Unit = {},
    onVolverClick: () -> Unit = {}
) {
    var form by remember { mutableStateOf(NuevoViajeForm()) }
    var expandedConductor by remember { mutableStateOf(false) }
    var expandedVehiculo by remember { mutableStateOf(false) }

    val conductorSeleccionado = conductoresDisponibles.find { it.id == form.conductorId }
    val vehiculoSeleccionado = vehiculosDisponibles.find { it.id == form.vehiculoId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear viaje") },
                navigationIcon = { IconButton(onClick = onVolverClick) { Text("←") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ExposedDropdownMenuBox(expanded = expandedConductor, onExpandedChange = { expandedConductor = it }) {
                OutlinedTextField(
                    value = conductorSeleccionado?.nombre ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Conductor") },
                    placeholder = { Text("Selecciona un conductor disponible") },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedConductor, onDismissRequest = { expandedConductor = false }) {
                    conductoresDisponibles.forEach { conductor ->
                        DropdownMenuItem(
                            text = { Text("${conductor.nombre} (${conductor.estado.etiqueta})") },
                            onClick = {
                                form = form.copy(conductorId = conductor.id)
                                expandedConductor = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = expandedVehiculo, onExpandedChange = { expandedVehiculo = it }) {
                OutlinedTextField(
                    value = vehiculoSeleccionado?.let { "${it.placa} · ${it.modelo}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Vehículo") },
                    placeholder = { Text("Selecciona un vehículo operativo") },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedVehiculo, onDismissRequest = { expandedVehiculo = false }) {
                    vehiculosDisponibles.forEach { vehiculo ->
                        DropdownMenuItem(
                            text = { Text("${vehiculo.placa} · ${vehiculo.modelo}") },
                            onClick = {
                                form = form.copy(vehiculoId = vehiculo.id)
                                expandedVehiculo = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = form.origen,
                onValueChange = { form = form.copy(origen = it) },
                label = { Text("Origen") },
                placeholder = { Text("Lima") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.destino,
                onValueChange = { form = form.copy(destino = it) },
                label = { Text("Destino") },
                placeholder = { Text("Trujillo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.fechaSalida,
                onValueChange = { form = form.copy(fechaSalida = it) },
                label = { Text("Fecha y hora de salida") },
                placeholder = { Text("dd/mm/aaaa hh:mm") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.descripcionCarga,
                onValueChange = { form = form.copy(descripcionCarga = it) },
                label = { Text("Descripción de la carga") },
                placeholder = { Text("Cemento, 200 sacos") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.pesoCargaKg,
                onValueChange = { form = form.copy(pesoCargaKg = it) },
                label = { Text("Peso de la carga (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { onCrearViajeClick(form) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = form.conductorId != null && form.vehiculoId != null &&
                    form.origen.isNotBlank() && form.destino.isNotBlank()
            ) {
                Text("Asignar viaje", fontWeight = FontWeight.Medium)
            }
        }
    }
}
