package com.logictruck.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PuntoCarbono(val etiqueta: String, val kgCO2: Double)

private enum class Agrupacion(val etiqueta: String) { VIAJE("Por viaje"), VEHICULO("Por vehículo"), PERIODO("Por periodo") }

private val datosPorPeriodoDemo = listOf(
    PuntoCarbono("Mar", 210.0),
    PuntoCarbono("Abr", 260.0),
    PuntoCarbono("May", 190.0),
    PuntoCarbono("Jun", 300.0),
    PuntoCarbono("Jul", 245.0)
)

private val datosPorVehiculoDemo = listOf(
    PuntoCarbono("T4X-882", 480.0),
    PuntoCarbono("V2P-014", 320.0),
    PuntoCarbono("W9L-330", 270.0)
)

/**
 * Reporte con gráfico de barras del CO2 emitido, agrupable por viaje, vehículo o periodo.
 * Usa un Canvas propio para no depender de una librería de gráficos externa.
 * TODO: calcular kgCO2 real a partir de combustible cargado (litros * factor de emisión)
 * agregado desde Room, en lugar de los datos demo.
 */
@Composable
fun ReporteHuellaCarbonoScreen(
    totalAcumuladoKg: Double = 1250.5
) {
    var agrupacion by remember { mutableStateOf(Agrupacion.PERIODO) }
    val datos = when (agrupacion) {
        Agrupacion.PERIODO -> datosPorPeriodoDemo
        Agrupacion.VEHICULO -> datosPorVehiculoDemo
        Agrupacion.VIAJE -> datosPorPeriodoDemo // TODO: reemplazar por agregación real por viaje
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Huella de carbono") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total acumulado", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "%.1f kg CO₂".format(totalAcumuladoKg),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Agrupacion.entries) { opcion ->
                    FilterChip(
                        selected = agrupacion == opcion,
                        onClick = { agrupacion = opcion },
                        label = { Text(opcion.etiqueta) }
                    )
                }
            }

            BarChart(datos = datos, modifier = Modifier.fillMaxWidth().height(220.dp))
        }
    }
}

@Composable
private fun BarChart(datos: List<PuntoCarbono>, modifier: Modifier = Modifier) {
    val colorBarra = MaterialTheme.colorScheme.primary
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
