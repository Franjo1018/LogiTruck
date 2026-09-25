@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.conductor

import com.example.prueba.data.CombustibleRepository
import com.example.prueba.data.RegistroCombustible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck
import kotlinx.coroutines.launch

/** Formulario para un nuevo registro de combustible (en galones). */
data class RegistroCombustibleForm(
    val galones: String = "",
    val costo: String = "",
    val lugar: String = "",
    val kilometrajeActual: String = ""
)

/**
 * Registro de combustible cargado en el viaje actual, con historial de cargas previas del
 * conductor, guardado en Firebase (empresas/{empresaId}/combustible vía CombustibleRepository).
 * Se mide en galones (unidad normal de facturación en Perú), no en litros. Alimenta luego el
 * cálculo de huella de carbono del admin. Empieza vacío.
 */
@Composable
fun RegistroCombustibleScreen(
    empresaId: String = "",
    conductorUid: String = "",
    conductorNombre: String = "",
    // Viaje activo del conductor, si tiene uno: liga esta carga a su viaje (puede haber varias
    // por viaje, esta pantalla se puede usar más de una vez para el mismo viaje en ruta).
    viajeIdReal: String = ""
) {
    var form by remember { mutableStateOf(RegistroCombustibleForm()) }
    var registrosPrevios by remember { mutableStateOf<List<RegistroCombustible>>(emptyList()) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun recargar() {
        registrosPrevios = CombustibleRepository.listarPorConductor(empresaId, conductorUid)
    }

    LaunchedEffect(empresaId, conductorUid) {
        try {
            recargar()
        } catch (e: Exception) {
            // Sin conexión al abrir: se deja la lista vacía, se reintenta al guardar.
        }
    }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Registrar combustible") },
                // Deja libre la franja de 56dp donde ConductorHostScreen superpone el ícono
                // de menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = form.galones,
                onValueChange = { form = form.copy(galones = it) },
                label = { Text("Galones cargados") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.costo,
                onValueChange = { form = form.copy(costo = it) },
                label = { Text("Costo (S/)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.lugar,
                onValueChange = { form = form.copy(lugar = it) },
                label = { Text("Grifo / lugar") },
                placeholder = { Text("Grifo Primax - Pativilca") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = form.kilometrajeActual,
                onValueChange = { form = form.copy(kilometrajeActual = it) },
                label = { Text("Kilometraje actual") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    error = null
                    guardando = true
                    val galones = form.galones.replace(",", ".").toDoubleOrNull()
                    val costo = form.costo.replace(",", ".").toDoubleOrNull()
                    if (galones == null || costo == null) {
                        error = "Galones y costo deben ser números válidos"
                        guardando = false
                        return@Button
                    }
                    scope.launch {
                        try {
                            CombustibleRepository.registrar(
                                empresaId = empresaId,
                                conductorUid = conductorUid,
                                conductorNombre = conductorNombre,
                                galones = galones,
                                costo = costo,
                                lugar = form.lugar,
                                viajeIdReal = viajeIdReal
                            )
                            form = RegistroCombustibleForm()
                            recargar()
                        } catch (e: Exception) {
                            error = "No se pudo guardar el registro, revisa tu conexión"
                        } finally {
                            guardando = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = form.galones.isNotBlank() && form.costo.isNotBlank() && form.lugar.isNotBlank() && !guardando,
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaLogicTruck)
            ) {
                Text(if (guardando) "Guardando..." else "Guardar registro", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Cargas registradas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)

            if (registrosPrevios.isEmpty()) {
                Text(
                    "Todavía no registraste ninguna carga.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(registrosPrevios) { registro ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(NaranjaLogicTruck))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${registro.galones} gal · S/ ${registro.costo}", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${registro.lugar} · ${registro.hora}",
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
