@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck
import com.example.prueba.data.VehiculoRepository
import kotlinx.coroutines.launch

/**
 * Formulario para registrar un vehículo nuevo en la empresa. Una vez guardado aparece en la
 * lista de "Vehículos" y desde ahí se le puede asignar un conductor (tocar el vehículo →
 * elegir de la lista de conductores de la empresa).
 */
@Composable
fun RegistrarVehiculoScreen(empresaId: String, onVehiculoRegistrado: () -> Unit = {}) {
    var placa by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Registrar vehículo") },
                // Deja libre la franja de 56dp donde AdminHostScreen superpone el ícono de
                // menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = placa,
                onValueChange = { placa = it; error = null },
                label = { Text("Placa") },
                placeholder = { Text("T4X-882") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = modelo,
                onValueChange = { modelo = it; error = null },
                label = { Text("Modelo") },
                placeholder = { Text("Volvo FH") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    error = null
                    guardando = true
                    scope.launch {
                        try {
                            VehiculoRepository.registrar(empresaId, placa, modelo)
                            onVehiculoRegistrado()
                        } catch (e: Exception) {
                            error = "No se pudo registrar el vehículo, revisa tu conexión"
                        } finally {
                            guardando = false
                        }
                    }
                },
                enabled = !guardando && placa.isNotBlank() && modelo.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaLogicTruck)
            ) {
                Text(if (guardando) "Guardando..." else "Registrar vehículo", fontWeight = FontWeight.Medium)
            }

            error?.let {
                Text(text = it, color = Color(0xFFB3261E))
            }
        }
    }
}
