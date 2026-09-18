@file:OptIn(ExperimentalMaterial3Api::class)

package com.logictruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val kpisDemo = KpisFlota(
    viajesActivos = 8,
    viajesCompletadosMes = 42,
    incidenciasAbiertas = 3,
    huellaCarbonoKg = 1250.5
)

private val alertasDemo = listOf(
    AlertaDashboard("Vehículo T4X-882 con revisión técnica vencida", SeveridadIncidencia.GRAVE),
    AlertaDashboard("Retraso reportado en viaje Lima → Arequipa", SeveridadIncidencia.MODERADA),
    AlertaDashboard("Conductor sin checklist previaje completado", SeveridadIncidencia.LEVE)
)

/**
 * Dashboard compartido por Administrador y Despachador.
 * TODO: alimentar con TripViewModel + IncidenciaViewModel en lugar de datos demo.
 */
@Composable
fun AdminDashboardScreen(
    kpis: KpisFlota = kpisDemo,
    alertas: List<AlertaDashboard> = alertasDemo,
    onAlertaClick: (AlertaDashboard) -> Unit = {},
    onVerFlotaClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Panel administrativo") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiCard("Viajes activos", kpis.viajesActivos.toString(), Modifier.weight(1f))
                    KpiCard("Completados (mes)", kpis.viajesCompletadosMes.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        "Incidencias abiertas",
                        kpis.incidenciasAbiertas.toString(),
                        Modifier.weight(1f),
                        destacar = kpis.incidenciasAbiertas > 0
                    )
                    KpiCard("Huella CO₂ (kg)", "%.1f".format(kpis.huellaCarbonoKg), Modifier.weight(1f))
                }
            }
            item {
                Button(onClick = onVerFlotaClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver mapa de flota en tiempo real")
                }
            }
            item {
                Text("Alertas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            }
            items(alertas) { alerta ->
                AlertaItem(alerta, onClick = { onAlertaClick(alerta) })
            }
        }
    }
}

@Composable
private fun KpiCard(label: String, valor: String, modifier: Modifier = Modifier, destacar: Boolean = false) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (destacar) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AlertaItem(alerta: AlertaDashboard, onClick: () -> Unit) {
    val color = when (alerta.severidad) {
        SeveridadIncidencia.GRAVE -> MaterialTheme.colorScheme.error
        SeveridadIncidencia.MODERADA -> Color(0xFFB5651D)
        SeveridadIncidencia.LEVE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    OutlinedCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(10.dp))
            Text(alerta.texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
