package com.logictruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Formulario para un nuevo registro de combustible. */
data class RegistroCombustibleForm(
    val litros: String = "",
    val costo: String = "",
    val lugar: String = "",
    val kilometrajeActual: String = ""
)

private val registrosDemo = listOf(
    RegistroCombustible(80.0, 320.0, "Grifo Primax - Pativilca", "8:50 pm"),
    RegistroCombustible(60.0, 235.0, "Grifo Repsol - Chimbote", "11:15 pm")
)

/**
 * Registro de combustible cargado en el viaje actual, con historial de cargas previas.
 * TODO: al guardar, insertar en Room ligado al VIAJE activo y actualizar el consumo estimado
 * usado luego en ReporteHuellaCarbonoScreen.
 */
@Composable
fun RegistroCombustibleScreen(
    registrosPrevios: List<RegistroCombustible> = registrosDemo,
    onGuardarClick: (RegistroCombustibleForm) -> Unit = {}
) {
    var form by remember { mutableStateOf(RegistroCombustibleForm()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Registrar combustible") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = form.litros,
                onValueChange = { form = form.copy(litros = it) },
                label = { Text("Litros cargados") },
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

            Button(
                onClick = { onGuardarClick(form) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = form.litros.isNotBlank() && form.costo.isNotBlank() && form.lugar.isNotBlank()
            ) {
                Text("Guardar registro", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Cargas en este viaje", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(registrosPrevios) { registro ->
                    OutlinedCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${registro.litros} L · S/ ${registro.costo}", style = MaterialTheme.typography.bodyMedium)
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
