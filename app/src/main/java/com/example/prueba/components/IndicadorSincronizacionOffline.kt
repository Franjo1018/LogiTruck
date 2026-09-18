package com.logictruck.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Estado de conectividad/sincronización con el backend (Firebase opcional).
 * Room/SQLite sigue siendo la fuente de verdad local; esto solo informa si lo pendiente
 * ya se sincronizó con el servidor.
 */
enum class EstadoSincronizacion { SINCRONIZADO, SIN_CONEXION, SINCRONIZANDO }

private data class BannerEstilo(
    val icono: ImageVector,
    val texto: String,
    val colorFondo: Color,
    val colorTexto: Color
)

/**
 * Banner compacto para mostrar en la parte superior de HomeScreen, ChecklistPreviajeScreen,
 * RegistroCombustibleScreen, etc. cuando hay datos pendientes de subir.
 * TODO: conectar a un SyncViewModel que observe conectividad (ConnectivityManager) y una
 * cola de operaciones pendientes en Room (WorkManager es una buena opción para reintentos).
 */
@Composable
fun IndicadorSincronizacionOffline(
    estado: EstadoSincronizacion,
    elementosPendientes: Int = 0,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = estado != EstadoSincronizacion.SINCRONIZADO || elementosPendientes > 0) {
        val estilo = when (estado) {
            EstadoSincronizacion.SINCRONIZADO -> BannerEstilo(
                Icons.Filled.CloudDone,
                "Todo sincronizado",
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
            EstadoSincronizacion.SIN_CONEXION -> BannerEstilo(
                Icons.Filled.CloudOff,
                if (elementosPendientes > 0)
                    "Sin conexión · $elementosPendientes por sincronizar"
                else
                    "Sin conexión",
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
            EstadoSincronizacion.SINCRONIZANDO -> BannerEstilo(
                Icons.Filled.Sync,
                "Sincronizando datos…",
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(estilo.colorFondo)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(estilo.icono, contentDescription = null, tint = estilo.colorTexto, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                estilo.texto,
                style = MaterialTheme.typography.labelMedium,
                color = estilo.colorTexto,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
