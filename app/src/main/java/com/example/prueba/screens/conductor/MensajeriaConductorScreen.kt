@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.conductor

import com.example.prueba.data.MensajeChat
import com.example.prueba.data.MensajeRepository

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck
import kotlinx.coroutines.launch

/**
 * Chat del conductor con el administrador/despachador de su empresa — un solo hilo, ya que el
 * conductor solo conversa con "la empresa" (no con otros conductores), guardado en el mismo
 * nodo que usa MensajeriaScreen del lado admin (empresas/{empresaId}/mensajes/{miUid}).
 */
@Composable
fun MensajeriaConductorScreen(
    empresaId: String,
    conductorUid: String,
    conductorNombre: String
) {
    var mensajes by remember { mutableStateOf<List<MensajeChat>>(emptyList()) }
    var textoNuevo by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun recargar() {
        mensajes = MensajeRepository.listarMensajes(empresaId, conductorUid, conductorUid)
    }

    LaunchedEffect(empresaId, conductorUid) {
        cargando = true
        try {
            recargar()
        } finally {
            cargando = false
        }
    }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Mensajería con la empresa") },
                // Deja libre la franja de 56dp donde ConductorHostScreen superpone el ícono
                // de menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textoNuevo,
                    onValueChange = { textoNuevo = it },
                    placeholder = { Text("Escribe un mensaje") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                IconButton(onClick = {
                    val texto = textoNuevo
                    if (texto.isNotBlank()) {
                        textoNuevo = ""
                        scope.launch {
                            MensajeRepository.enviar(empresaId, conductorUid, conductorUid, conductorNombre, texto, esDeAdmin = false)
                            recargar()
                        }
                    }
                }) {
                    Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = NaranjaLogicTruck)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (mensajes.isEmpty()) {
                Text(
                    "Todavía no hay mensajes. Escribe al despachador si necesitas algo.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mensajes) { mensaje ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (mensaje.esPropio) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (mensaje.esPropio)
                                        NaranjaLogicTruck.copy(alpha = 0.15f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(mensaje.texto, style = MaterialTheme.typography.bodyMedium)
                                    Text(mensaje.hora, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
