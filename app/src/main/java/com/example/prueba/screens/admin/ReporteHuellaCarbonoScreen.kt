@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.admin

import com.example.prueba.data.CombustibleRepository
import com.example.prueba.data.RegistroCombustible

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresChipNaranja
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck

data class PuntoCarbono(val etiqueta: String, val kgCO2: Double)

private enum class Agrupacion(val etiqueta: String) { PERIODO("Por periodo"), CONDUCTOR("Por conductor") }

/**
 * Reporte con gráfico de barras de la huella de carbono real de la flota, calculada a partir
 * de los galones cargados en empresas/{empresaId}/combustible (galones * factor de emisión, ver
 * CombustibleRepository.FACTOR_KG_CO2_POR_GALON), agrupable por periodo (mes) o por conductor.
 * Empieza en 0 hasta que haya registros de combustible reales.
 */
@Composable
fun ReporteHuellaCarbonoScreen(empresaId: String) {
    var registros by remember { mutableStateOf<List<RegistroCombustible>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var agrupacion by remember { mutableStateOf(Agrupacion.PERIODO) }

    LaunchedEffect(empresaId) {
        cargando = true
        try {
            registros = CombustibleRepository.listarTodos(empresaId)
        } catch (e: Exception) {
            // Sin conexión: se muestra el reporte vacío.
        } finally {
            cargando = false
        }
    }

    val totalKg = registros.sumOf { it.galones * CombustibleRepository.FACTOR_KG_CO2_POR_GALON }

    val datos = when (agrupacion) {
        Agrupacion.PERIODO -> registros
            .groupBy { it.fecha.substringAfter('/').ifBlank { "?" } } // agrupa por mes (dd/MM -> MM)
            .map { (mes, lista) -> PuntoCarbono(mes, lista.sumOf { it.galones * CombustibleRepository.FACTOR_KG_CO2_POR_GALON }) }
            .sortedBy { it.etiqueta }
        Agrupacion.CONDUCTOR -> registros
            .groupBy { it.conductorNombre.ifBlank { "Sin nombre" } }
            .map { (nombre, lista) -> PuntoCarbono(nombre, lista.sumOf { it.galones * CombustibleRepository.FACTOR_KG_CO2_POR_GALON }) }
            .sortedByDescending { it.kgCO2 }
    }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Huella de carbono") },
                // Deja libre la franja de 56dp donde AdminHostScreen superpone el ícono de
                // menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = NaranjaLogicTruck.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total acumulado", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "%.1f kg CO₂".format(totalKg),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = NaranjaLogicTruck
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Agrupacion.entries) { opcion ->
                    FilterChip(
                        selected = agrupacion == opcion,
                        onClick = { agrupacion = opcion },
                        label = { Text(opcion.etiqueta) },
                        colors = coloresChipNaranja()
                    )
                }
            }

            when {
                cargando -> Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                datos.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Todavía no hay cargas de combustible registradas.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> BarChart(datos = datos, modifier = Modifier.fillMaxWidth().height(220.dp))
            }
        }
    }
}

@Composable
private fun BarChart(datos: List<PuntoCarbono>, modifier: Modifier = Modifier) {
    val colorBarra = NaranjaLogicTruck
    val colorEje = MaterialTheme.colorScheme.outlineVariant
    val maxValor = (datos.maxOfOrNull { it.kgCO2 } ?: 1.0).coerceAtLeast(1.0)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val espacio = size.width / datos.size
            val anchoBarra = espacio * 0.5f
            drawLine(colorEje, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            datos.forEachIndexed { index, punto ->
                val alturaBarra = (punto.kgCO2 / maxValor).toFloat() * size.height * 0.9f
                val x = index * espacio + (espacio - anchoBarra) / 2
                drawRect(
                    color = colorBarra,
                    topLeft = Offset(x, size.height - alturaBarra),
                    size = Size(anchoBarra, alturaBarra)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            datos.forEach { punto ->
                Text(
                    punto.etiqueta,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
