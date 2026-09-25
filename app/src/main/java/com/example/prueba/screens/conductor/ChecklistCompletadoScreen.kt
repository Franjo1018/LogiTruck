@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.conductor

import com.example.prueba.data.ChecklistPrevio
import com.example.prueba.data.ChecklistRepository
import com.example.prueba.data.CombustibleRepository
import com.example.prueba.data.RegistroCombustible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck

/**
 * Se muestra en vez de ChecklistPreviajeScreen cuando el viaje activo ya pasó de PROGRAMADO a
 * EN_RUTA: el checklist de ese viaje ya se hizo una vez (no tiene sentido volver a mostrar
 * checkboxes vacíos ni el formulario de combustible inicial para llenar de nuevo). En su lugar,
 * esto es de solo lectura: confirma que ya está hecho, el destino del viaje, y un resumen de lo
 * que se declaró (kilometraje de salida, la foto real de la factura si se adjuntó una) más el
 * combustible cargado en este viaje hasta ahora (la carga inicial del checklist, más cualquier
 * recarga en ruta desde "Registro de combustible" — puede haber más de una).
 */
@Composable
fun ChecklistCompletadoScreen(
    empresaId: String,
    viajeIdReal: String,
    nombre: String = "",
    origenDestino: String = "Sin ruta asignada"
) {
    var checklist by remember(viajeIdReal) { mutableStateOf<ChecklistPrevio?>(null) }
    var cargasCombustible by remember(viajeIdReal) { mutableStateOf<List<RegistroCombustible>>(emptyList()) }
    var cargando by remember(viajeIdReal) { mutableStateOf(true) }

    LaunchedEffect(empresaId, viajeIdReal) {
        cargando = true
        try {
            checklist = ChecklistRepository.leer(empresaId, viajeIdReal)
            cargasCombustible = CombustibleRepository.listarPorViaje(empresaId, viajeIdReal)
        } catch (e: Exception) {
            // Sin conexión: se queda sin datos de detalle, pero igual se ve la confirmación.
        } finally {
            cargando = false
        }
    }

    val totalGalones = cargasCombustible.sumOf { it.galones }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Checklist previaje") },
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(NaranjaLogicTruck)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Text(
                        (if (nombre.isBlank()) "Bienvenido" else "Bienvenido, $nombre") + ",",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        "Tu viaje: $origenDestino",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NaranjaLogicTruck)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Checklist previaje ya realizado",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            "Este viaje ya está en ruta. No hace falta volver a llenar el checklist.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (cargando) {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else {
                            val datos = checklist
                            if (datos != null && datos.kilometrajeInicial.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                InfoFila("Kilometraje de salida", "${datos.kilometrajeInicial} km")
                                InfoFila(
                                    "Factura de combustible",
                                    if (datos.facturaCombustibleAdjunta) "Adjuntada" else "No adjuntada"
                                )
                                if (datos.facturaCombustibleUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AsyncImage(
                                        model = datos.facturaCombustibleUrl,
                                        contentDescription = "Foto de la factura de combustible",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoFila(
                                "Combustible cargado en este viaje",
                                if (cargasCombustible.isEmpty()) "Sin registros todavía"
                                else "%.1f gal (%d carga%s)".format(
                                    totalGalones,
                                    cargasCombustible.size,
                                    if (cargasCombustible.size == 1) "" else "s"
                                )
                            )
                        }
                    }
                }
            }

            if (cargasCombustible.isNotEmpty()) {
                item {
                    Text(
                        "Cargas de combustible de este viaje",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(cargasCombustible) { registro ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${registro.galones} gal · ${registro.lugar}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                registro.hora,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "¿Necesitas cargar combustible de nuevo en este viaje? Puedes registrarlo " +
                        "las veces que haga falta desde \"Registro de combustible\" en el menú.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoFila(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
