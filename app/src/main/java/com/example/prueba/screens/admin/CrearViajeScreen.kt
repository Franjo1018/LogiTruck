package com.example.prueba.screens.admin

import com.example.prueba.data.*

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Datos capturados al crear un viaje. Corresponde a un nuevo registro en la tabla VIAJE.
 * conductorUid/vehiculoIdReal identifican al conductor/vehículo real elegido (las keys que usa
 * Firebase), no un índice: Conductor.id y Vehiculo.id ya no sirven como identificador único
 * cuando vienen de datos reales (VehiculoRepository siempre deja Vehiculo.id en 0).
 */
data class NuevoViajeForm(
    val conductorUid: String? = null,
    val vehiculoIdReal: String? = null,
    val origen: String = "",
    val destino: String = "",
    val fecha: String = "",
    val hora: String = "",
    val descripcionCarga: String = "",
    val pesoCargaKg: String = ""
)

/**
 * Ciudades/provincias entre las que la empresa opera. Se listan alfabéticamente en los combo
 * box de origen/destino (a pedido de Francisco) en vez de texto libre, para evitar errores de
 * tipeo. No es la lista completa de provincias del Perú, solo las relevantes para una empresa
 * de transporte de carga con base en Trujillo; se puede ampliar según se necesite.
 * TODO: mover esta lista a Firebase (empresas/{empresaId}/ciudadesHabilitadas) si algún día
 * cada empresa necesita su propio set de rutas en vez de una lista fija en el código.
 */
private val ciudadesDisponibles = listOf(
    "Arequipa", "Cajamarca", "Chachapoyas", "Chiclayo", "Chimbote", "Chincha Alta", "Cusco",
    "Huancayo", "Huaraz", "Ica", "Iquitos", "Lima", "Piura", "Pucallpa", "Puno", "Sullana",
    "Tacna", "Trujillo", "Tumbes"
).sorted()

/**
 * Pantalla del despachador para asignar un nuevo viaje. Conductores y vehículos ya no son
 * datos de ejemplo: se leen de Firebase (ConductorRepository/VehiculoRepository) filtrados por
 * la empresa del admin que tiene la sesión abierta, igual que en GestionConductoresScreen/
 * GestionVehiculosScreen. Empieza vacío hasta que la empresa tenga conductores/vehículos
 * registrados (se avisa en pantalla si falta alguno de los dos).
 * Origen/destino se eligen de [ciudadesDisponibles] (combo box, orden alfabético) en vez de
 * texto libre; fecha y hora de salida se capturan con DatePicker/TimePicker de Material3.
 * TODO: filtrar conductores/vehículos por disponibilidad real (sin viaje activo) antes de listar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearViajeScreen(
    empresaId: String,
    onVolverClick: () -> Unit = {}
) {
    var conductoresDisponibles by remember { mutableStateOf<List<Conductor>>(emptyList()) }
    var vehiculosDisponibles by remember { mutableStateOf<List<Vehiculo>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var creando by remember { mutableStateOf(false) }
    var creado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun recargarListas() {
        conductoresDisponibles = ConductorRepository.listarConductores(empresaId)
        vehiculosDisponibles = VehiculoRepository.listar(empresaId)
    }

    LaunchedEffect(empresaId) {
        cargando = true
        error = null
        try {
            recargarListas()
        } catch (e: Exception) {
            error = "No se pudo cargar conductores/vehículos, revisa tu conexión"
        } finally {
            cargando = false
        }
    }

    var form by remember { mutableStateOf(NuevoViajeForm()) }
    var expandedConductor by remember { mutableStateOf(false) }
    var expandedVehiculo by remember { mutableStateOf(false) }
    var expandedOrigen by remember { mutableStateOf(false) }
    var expandedDestino by remember { mutableStateOf(false) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var mostrarSelectorHora by remember { mutableStateOf(false) }

    val conductorSeleccionado = conductoresDisponibles.find { it.uid == form.conductorUid }
    val vehiculoSeleccionado = vehiculosDisponibles.find { it.idReal == form.vehiculoIdReal }

    // Los combo box y campos de este formulario llevan fondo blanco sólido (coloresCampoBlanco)
    // para que no se pierdan contra el patrón de mosaico de fondo; el patrón sigue ahí, solo que
    // no se ve "a través" de los campos.
    val coloresCampoBlanco = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White
    )

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Crear viaje") },
                navigationIcon = { IconButton(onClick = onVolverClick) { Text("←") } },
                colors = coloresTopBarNaranja()
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
            if (cargando) {
                Text("Cargando conductores y vehículos...", style = MaterialTheme.typography.bodySmall)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!cargando && conductoresDisponibles.isEmpty()) {
                Text(
                    "Todavía no hay conductores registrados en la empresa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!cargando && vehiculosDisponibles.isEmpty()) {
                Text(
                    "Todavía no hay vehículos registrados en la empresa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExposedDropdownMenuBox(expanded = expandedConductor, onExpandedChange = { expandedConductor = it }) {
                OutlinedTextField(
                    value = conductorSeleccionado?.nombre ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Conductor") },
                    placeholder = { Text("Selecciona un conductor disponible") },
                    colors = coloresCampoBlanco,
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedConductor, onDismissRequest = { expandedConductor = false }) {
                    conductoresDisponibles.forEach { conductor ->
                        DropdownMenuItem(
                            text = { Text("${conductor.nombre} (${conductor.estado.etiqueta})") },
                            onClick = {
                                form = form.copy(conductorUid = conductor.uid)
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
                    colors = coloresCampoBlanco,
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedVehiculo, onDismissRequest = { expandedVehiculo = false }) {
                    vehiculosDisponibles.forEach { vehiculo ->
                        DropdownMenuItem(
                            text = { Text("${vehiculo.placa} · ${vehiculo.modelo}") },
                            onClick = {
                                form = form.copy(vehiculoIdReal = vehiculo.idReal)
                                expandedVehiculo = false
                            }
                        )
                    }
                }
            }

            // Origen y destino uno junto al otro (no uno debajo del otro), cada uno como combo
            // box con las ciudades disponibles en orden alfabético.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expandedOrigen,
                    onExpandedChange = { expandedOrigen = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = form.origen,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Origen") },
                        placeholder = { Text("Ciudad") },
                        colors = coloresCampoBlanco,
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedOrigen, onDismissRequest = { expandedOrigen = false }) {
                        ciudadesDisponibles.forEach { ciudad ->
                            DropdownMenuItem(
                                text = { Text(ciudad) },
                                onClick = {
                                    form = form.copy(origen = ciudad)
                                    expandedOrigen = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedDestino,
                    onExpandedChange = { expandedDestino = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = form.destino,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destino") },
                        placeholder = { Text("Ciudad") },
                        colors = coloresCampoBlanco,
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedDestino, onDismissRequest = { expandedDestino = false }) {
                        ciudadesDisponibles.forEach { ciudad ->
                            DropdownMenuItem(
                                text = { Text(ciudad) },
                                onClick = {
                                    form = form.copy(destino = ciudad)
                                    expandedDestino = false
                                }
                            )
                        }
                    }
                }
            }

            // Fecha (calendario) y hora de salida, uno junto al otro. Los OutlinedTextField son
            // de solo lectura; el interactionSource detecta el toque para abrir el diálogo
            // correspondiente, ya que un campo readOnly no dispara onValueChange.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val interactionFecha = remember { MutableInteractionSource() }
                LaunchedEffect(interactionFecha) {
                    interactionFecha.interactions.collect { interaccion ->
                        if (interaccion is PressInteraction.Release) mostrarSelectorFecha = true
                    }
                }
                OutlinedTextField(
                    value = form.fecha,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha") },
                    placeholder = { Text("dd/mm/aaaa") },
                    trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    interactionSource = interactionFecha,
                    colors = coloresCampoBlanco,
                    modifier = Modifier.weight(1f)
                )

                val interactionHora = remember { MutableInteractionSource() }
                LaunchedEffect(interactionHora) {
                    interactionHora.interactions.collect { interaccion ->
                        if (interaccion is PressInteraction.Release) mostrarSelectorHora = true
                    }
                }
                OutlinedTextField(
                    value = form.hora,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hora") },
                    placeholder = { Text("hh:mm") },
                    trailingIcon = { Icon(Icons.Filled.AccessTime, contentDescription = null) },
                    interactionSource = interactionHora,
                    colors = coloresCampoBlanco,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = form.descripcionCarga,
                onValueChange = { form = form.copy(descripcionCarga = it) },
                label = { Text("Descripción de la carga") },
                placeholder = { Text("Cemento, 200 sacos") },
                colors = coloresCampoBlanco,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.pesoCargaKg,
                onValueChange = { form = form.copy(pesoCargaKg = it) },
                label = { Text("Peso de la carga (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = coloresCampoBlanco,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (creado) {
                Text(
                    "Viaje asignado. El conductor ya puede verlo y empezar su checklist.",
                    color = NaranjaLogicTruck,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val conductor = conductorSeleccionado
                    val vehiculo = vehiculoSeleccionado
                    if (conductor == null || vehiculo == null) return@Button
                    error = null
                    creando = true
                    creado = false
                    scope.launch {
                        try {
                            ViajeRepository.crear(
                                empresaId = empresaId,
                                conductorUid = conductor.uid,
                                conductorNombre = conductor.nombre,
                                vehiculoIdReal = vehiculo.idReal,
                                vehiculoPlaca = vehiculo.placa,
                                origen = form.origen,
                                destino = form.destino,
                                fecha = form.fecha,
                                hora = form.hora,
                                descripcionCarga = form.descripcionCarga,
                                pesoCargaKg = form.pesoCargaKg
                            )
                            form = NuevoViajeForm()
                            creado = true
                        } catch (e: Exception) {
                            error = "No se pudo asignar el viaje, revisa tu conexión"
                        } finally {
                            creando = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = form.conductorUid != null && form.vehiculoIdReal != null &&
                    form.origen.isNotBlank() && form.destino.isNotBlank() && !creando,
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaLogicTruck)
            ) {
                Text(if (creando) "Asignando..." else "Asignar viaje", fontWeight = FontWeight.Medium)
            }
        }
    }

    if (mostrarSelectorFecha) {
        val estadoFecha = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estadoFecha.selectedDateMillis?.let { millis ->
                            val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
                            form = form.copy(fecha = formato.format(Date(millis)))
                        }
                        mostrarSelectorFecha = false
                    }
                ) { Text("Aceptar", color = NaranjaLogicTruck) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(
                state = estadoFecha,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = NaranjaLogicTruck,
                    todayDateBorderColor = NaranjaLogicTruck,
                    todayContentColor = NaranjaLogicTruck
                )
            )
        }
    }

    if (mostrarSelectorHora) {
        val estadoHora = rememberTimePickerState(is24Hour = true)
        Dialog(onDismissRequest = { mostrarSelectorHora = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Hora de salida", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                    TimePicker(
                        state = estadoHora,
                        colors = TimePickerDefaults.colors(
                            selectorColor = NaranjaLogicTruck,
                            periodSelectorSelectedContainerColor = NaranjaLogicTruck.copy(alpha = 0.2f),
                            timeSelectorSelectedContainerColor = NaranjaLogicTruck.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = NaranjaLogicTruck
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { mostrarSelectorHora = false }) { Text("Cancelar") }
                        TextButton(
                            onClick = {
                                val hora = "%02d:%02d".format(estadoHora.hour, estadoHora.minute)
                                form = form.copy(hora = hora)
                                mostrarSelectorHora = false
                            }
                        ) { Text("Aceptar", color = NaranjaLogicTruck) }
                    }
                }
            }
        }
    }
}
