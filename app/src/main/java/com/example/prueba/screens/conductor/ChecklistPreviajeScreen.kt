package com.logictruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Un ítem del checklist previaje, con posibilidad de adjuntar foto (CameraX) como evidencia.
 * fotoTomada es solo un flag de UI aquí; TODO: guardar el path/uri real de la imagen capturada.
 */
data class ItemChecklistPrevio(
    val id: String,
    val descripcion: String,
    val requiereFoto: Boolean = false,
    val completado: Boolean = false,
    val fotoTomada: Boolean = false
)

private val checklistDemo = listOf(
    ItemChecklistPrevio("frenos", "Revisión de frenos", requiereFoto = false),
    ItemChecklistPrevio("aceite", "Nivel de aceite", requiereFoto = false),
    ItemChecklistPrevio("llantas", "Presión y estado de llantas", requiereFoto = true),
    ItemChecklistPrevio("luces", "Luces delanteras y posteriores", requiereFoto = false),
    ItemChecklistPrevio("carga", "Carga bien asegurada", requiereFoto = true),
    ItemChecklistPrevio("documentos", "Documentos del vehículo a bordo", requiereFoto = false)
)

/**
 * Checklist previaje que el conductor completa antes de iniciar cada viaje.
 * TODO: al confirmar, persistir en Room ligado al VIAJE correspondiente y bloquear el
 * inicio del viaje (botón "Iniciar viaje") hasta que todos los ítems obligatorios estén ok.
 */
@Composable
fun ChecklistPreviajeScreen(
    origenDestino: String = "Lima → Trujillo",
    items: List<ItemChecklistPrevio> = checklistDemo,
    onItemToggle: (ItemChecklistPrevio) -> Unit = {},
    onTomarFotoClick: (ItemChecklistPrevio) -> Unit = {},
    onIniciarViajeClick: () -> Unit = {}
) {
    val todosCompletos = items.all { it.completado && (!it.requiereFoto || it.fotoTomada) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Checklist previaje") }) },
        bottomBar = {
            Button(
                onClick = onIniciarViajeClick,
                enabled = todosCompletos,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp)
            ) {
                Text(if (todosCompletos) "Iniciar viaje" else "Completa el checklist para continuar")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                origenDestino,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
            )
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items) { item ->
                    ChecklistFila(
                        item = item,
                        onToggle = { onItemToggle(item) },
                        onTomarFoto = { onTomarFotoClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistFila(
    item: ItemChecklistPrevio,
    onToggle: () -> Unit,
    onTomarFoto: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.completado, onCheckedChange = { onToggle() })
        Text(item.descripcion, modifier = Modifier.weight(1f))
        if (item.requiereFoto) {
            IconButton(onClick = onTomarFoto) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "Tomar foto de evidencia",
                    tint = if (item.fotoTomada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
