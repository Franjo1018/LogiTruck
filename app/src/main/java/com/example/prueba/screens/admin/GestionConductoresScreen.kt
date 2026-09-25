@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.admin

import com.example.prueba.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck

/**
 * Lista de choferes de la empresa, leída en tiempo real desde Firebase (empresas/{empresaId}/
 * usuarios filtrado por rol = CONDUCTOR vía ConductorRepository) — ya no son datos de ejemplo,
 * así que cualquier conductor que se una (por código de invitación) aparece aquí. Se vuelve a
 * leer cada vez que se entra a esta pantalla, así que un conductor recién registrado aparece
 * la próxima vez que el admin abra esta sección.
 *
 * El botón "+" no abre un formulario manual: la única forma de agregar un conductor es
 * invitándolo con un código (la persona se registra ella misma con ese código), así que el "+"
 * lleva a la pantalla "Invitar a la empresa" con el rol Conductor ya preseleccionado.
 *
 * estado, licenciaVigente y vehiculoAsignado todavía son valores neutros por defecto (ver
 * ConductorRepository): la app aún no tiene las tablas VIAJE/VEHICULO conectadas para calcular
 * el estado real de cada conductor.
 */
@Composable
fun GestionConductoresScreen(
    empresaId: String,
    onConductorClick: (Conductor) -> Unit = {},
    onAgregarClick: () -> Unit = {}
) {
    var conductores by remember { mutableStateOf<List<Conductor>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(empresaId) {
        cargando = true
        error = null
        try {
            conductores = ConductorRepository.listarConductores(empresaId)
        } catch (e: Exception) {
            error = "No se pudo cargar la lista de conductores, revisa tu conexión"
        } finally {
            cargando = false
        }
    }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Conductores") },
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
                Icon(Icons.Filled.Add, contentDescription = "Invitar conductor")
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

                conductores.isEmpty() -> Text(
                    "Todavía no hay conductores en la empresa. Usa el botón + para invitar a uno.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conductores) { conductor ->
                        Card(
                            onClick = { onConductorClick(conductor) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(4.dp)
                                        .background(if (conductor.licenciaVigente) NaranjaLogicTruck else MaterialTheme.colorScheme.error)
                                )
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
                                            .background(NaranjaLogicTruck.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(iniciales, color = NaranjaLogicTruck, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(conductor.nombre, fontWeight = FontWeight.Medium)
                                        val subtitulo = conductor.vehiculoAsignado
                                            ?: conductor.usuario.takeIf { it.isNotBlank() }?.let { "@$it" }
                                            ?: "Sin vehículo asignado"
                                        Text(subtitulo, style = MaterialTheme.typography.bodySmall)
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
        }
    }
}
