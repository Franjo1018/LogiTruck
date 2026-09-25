@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.conductor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck

/**
 * Se muestra en vez del checklist previaje cuando el conductor todavía no tiene ningún viaje
 * asignado por el despachador. El checklist no tiene sentido sin un viaje real detrás (no habría
 * qué "iniciar" al terminarlo), así que se oculta hasta que ConductorHostScreen detecte un
 * viaje activo (ver ViajeRepository.viajeActivoPorConductor).
 */
@Composable
fun SinViajeAsignadoScreen(nombre: String = "") {
    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Checklist previaje") },
                // Deja libre la franja de 56dp donde ConductorHostScreen superpone el ícono
                // de menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.LocalShipping,
                contentDescription = null,
                tint = NaranjaLogicTruck,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (nombre.isBlank()) "Todavía no tienes ningún viaje asignado" else "$nombre, todavía no tienes ningún viaje asignado",
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Cuando el despachador te asigne un viaje, va a aparecer aquí y vas a poder " +
                    "completar el checklist previaje para empezarlo.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
