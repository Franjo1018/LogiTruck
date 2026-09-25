@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.admin

import com.example.prueba.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.PlomoMedio
import com.example.prueba.components.coloresTopBarNaranja
import java.util.Calendar

// Mismo tono de brillo/plomo oscuro que usa DrawerHeader (naranja/plomo, en vez de colores al
// azar) para las tarjetitas de la parte de abajo.
private val NaranjaClaro = Color(0xFFF2B569)
private val PlomoOscuro = Color(0xFF57534E)

/**
 * Dashboard compartido por Administrador y Despachador. El encabezado (saludo + "Ver todo" +
 * tarjetas con franja de color a la izquierda + grilla de indicadores con cuadritos de color)
 * sigue el mismo patrón visual que Francisco mostró de referencia, adaptado a la paleta
 * naranja/plomo de la app en vez de colores sueltos.
 *
 * Indicadores y alertas son datos reales de Firebase, sin ejemplos hardcodeados:
 * - Indicadores: viajes activos y completados del mes (ViajeRepository), incidencias abiertas
 *   (IncidenciaRepository) y huella de carbono acumulada (CombustibleRepository).
 * - Alertas: una por cada viaje que todavía no completó su checklist previaje (estado
 *   PROGRAMADO) más una por cada incidencia abierta, las más graves primero.
 */
@Composable
fun AdminDashboardScreen(
    empresaId: String,
    nombre: String = "",
    onAlertaClick: (AlertaDashboard) -> Unit = {},
    onVerFlotaClick: () -> Unit = {}
) {
    var kpis by remember { mutableStateOf(KpisFlota(0, 0, 0, 0.0)) }
    var alertas by remember { mutableStateOf<List<AlertaDashboard>>(emptyList()) }

    LaunchedEffect(empresaId) {
        try {
            val viajes = ViajeRepository.listarTodos(empresaId)
            val incidencias = IncidenciaRepository.listar(empresaId)
            val combustible = CombustibleRepository.listarTodos(empresaId)

            val ahora = Calendar.getInstance()
            val viajesCompletadosMes = viajes.count { viaje ->
                if (viaje.estado != EstadoViaje.COMPLETADO || viaje.timestampMs == 0L) return@count false
                val fechaViaje = Calendar.getInstance().apply { timeInMillis = viaje.timestampMs }
                fechaViaje.get(Calendar.MONTH) == ahora.get(Calendar.MONTH) &&
                    fechaViaje.get(Calendar.YEAR) == ahora.get(Calendar.YEAR)
            }

            kpis = KpisFlota(
                viajesActivos = viajes.count { it.estado != EstadoViaje.COMPLETADO },
                viajesCompletadosMes = viajesCompletadosMes,
                incidenciasAbiertas = incidencias.count { !it.resuelta },
                huellaCarbonoKg = combustible.sumOf { it.galones * CombustibleRepository.FACTOR_KG_CO2_POR_GALON }
            )

            val alertasIncidencias = incidencias
                .filter { !it.resuelta }
                .map { AlertaDashboard("${it.conductor}: ${it.descripcion}", it.severidad) }
            val alertasChecklist = viajes
                .filter { it.estado == EstadoViaje.PROGRAMADO }
                .map {
                    AlertaDashboard(
                        "${it.conductorNombre.ifBlank { "Conductor" }} aún no completa el checklist previaje",
                        SeveridadIncidencia.LEVE
                    )
                }

            alertas = (alertasIncidencias + alertasChecklist).sortedByDescending { it.severidad.ordinal }
        } catch (e: Exception) {
            // Sin conexión: se mantiene el último estado conocido (o vacío la primera vez).
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel administrativo") },
                // Deja libre la franja de 56dp donde AdminHostScreen superpone el ícono de
                // menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        "Bienvenido, $nombre".let { if (nombre.isBlank()) "Bienvenido," else "$it," },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        "Aquí tienes el resumen de tu flota hoy",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        "Ver todo",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onVerFlotaClick)
                    )
                }
            }

            item {
                Text(
                    "Alertas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            if (alertas.isEmpty()) {
                item {
                    Text(
                        "Sin alertas por ahora.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            items(alertas) { alerta ->
                AlertaItem(
                    alerta,
                    onClick = { onAlertaClick(alerta) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Text(
                    "Indicadores",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        IndicadorItem(NaranjaLogicTruck, "Viajes activos", kpis.viajesActivos.toString(), Modifier.weight(1f))
                        IndicadorItem(PlomoOscuro, "Completados (mes)", kpis.viajesCompletadosMes.toString(), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        IndicadorItem(NaranjaClaro, "Incidencias abiertas", kpis.incidenciasAbiertas.toString(), Modifier.weight(1f))
                        IndicadorItem(PlomoMedio, "Huella CO₂ (kg)", "%.1f".format(kpis.huellaCarbonoKg), Modifier.weight(1f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun IndicadorItem(color: Color, label: String, valor: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(valor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlertaItem(alerta: AlertaDashboard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = when (alerta.severidad) {
        SeveridadIncidencia.GRAVE -> NaranjaLogicTruck
        SeveridadIncidencia.MODERADA -> PlomoMedio
        SeveridadIncidencia.LEVE -> PlomoOscuro.copy(alpha = 0.4f)
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(color)
            )
            Text(
                alerta.texto,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
