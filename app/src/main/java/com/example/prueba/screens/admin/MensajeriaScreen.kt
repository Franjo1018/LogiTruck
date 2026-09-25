@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.admin

import com.example.prueba.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.PlomoMedio
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck
import kotlinx.coroutines.launch

/**
 * Bandeja de conversaciones (un hilo por conductor) entre administrador/despachador y
 * conductores, leída desde Firebase vía MensajeRepository. Empieza vacía hasta que haya al
 * menos un mensaje enviado en cualquier dirección.
 */
@Composable
fun MensajeriaScreen(
    empresaId: String,
    onConversacionClick: (ConversacionResumen) -> Unit = {}
) {
    var conversaciones by remember { mutableStateOf<List<ConversacionResumen>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(empresaId) {
        cargando = true
        error = null
        try {
            val conductores = ConductorRepository.listarConductores(empresaId)
            conversaciones = MensajeRepository.listarConversaciones(empresaId, conductores)
        } catch (e: Exception) {
            error = "No se pudieron cargar las conversaciones, revisa tu conexión"
        } finally {
            cargando = false
        }
    }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Mensajería") },
                // Deja libre la franja de 56dp donde AdminHostScreen superpone el ícono de
                // menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
    ) { padding ->
        when {
            cargando -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            error != null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
            }

            conversaciones.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Todavía no hay conversaciones. Aparecerán aquí cuando un conductor escriba.",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversaciones) { conversacion ->
                    val colorFranja = if (conversacion.noLeidos > 0) NaranjaLogicTruck else PlomoMedio.copy(alpha = 0.3f)
                    Card(
                        onClick = { onConversacionClick(conversacion) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(colorFranja))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val iniciales = conversacion.nombreConductor.split(" ")
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
                                    Text(conversacion.nombreConductor, fontWeight = FontWeight.Medium)
                                    Text(
                                        conversacion.ultimoMensaje,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(conversacion.hora, style = MaterialTheme.typography.labelSmall)
                                    if (conversacion.noLeidos > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(NaranjaLogicTruck),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                conversacion.noLeidos.toString(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vista de un chat individual con un conductor (o, del lado conductor, con la empresa).
 * [viewerUid]/[viewerNombre] identifican quién envía ("Tú" en las burbujas propias);
 * [esDeAdmin] marca si quien escribe desde esta pantalla es el admin/despachador o el
 * conductor, para guardar el mensaje correctamente.
 *
 * [incidencia]: cuando el admin llega aquí desde HistorialIncidenciasScreen (tocando una
 * incidencia de este conductor), se muestra una tarjeta con lo reportado y, si todavía no está
 * resuelta, un botón para marcarla como resuelta — la idea es conversar con el conductor en este
 * mismo chat antes de cerrarla. Una vez resuelta ya no se puede volver a abrir (no hay botón para
 * deshacerlo). No aplica cuando esDeAdmin es false (el conductor no resuelve sus propias
 * incidencias), por eso queda opcional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversacionChatScreen(
    empresaId: String,
    conductorUid: String,
    viewerUid: String,
    viewerNombre: String,
    esDeAdmin: Boolean,
    nombreConductor: String = "",
    incidencia: Incidencia? = null,
    onVolverClick: () -> Unit = {}
) {
    var mensajes by remember { mutableStateOf<List<MensajeChat>>(emptyList()) }
    var textoNuevo by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }
    var incidenciaResuelta by remember(incidencia?.idReal) { mutableStateOf(incidencia?.resuelta ?: false) }
    var resolviendo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun recargar() {
        mensajes = MensajeRepository.listarMensajes(empresaId, conductorUid, viewerUid)
    }

    LaunchedEffect(conductorUid) {
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
                title = { Text(nombreConductor.ifBlank { "Mensajería" }) },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
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
                            MensajeRepository.enviar(empresaId, conductorUid, viewerUid, viewerNombre, texto, esDeAdmin)
                            recargar()
                        }
                    }
                }) {
                    Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = NaranjaLogicTruck)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (incidencia != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp, 12.dp, 12.dp, 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Incidencia reportada", fontWeight = FontWeight.Medium)
                            AssistChip(onClick = {}, label = { Text(incidencia.severidad.etiqueta) })
                        }
                        Text(
                            incidencia.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (incidencia.fotoUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = incidencia.fotoUrl,
                                contentDescription = "Foto de evidencia de la incidencia",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (incidenciaResuelta) {
                            Text(
                                "Incidencia resuelta",
                                color = NaranjaLogicTruck,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Button(
                                onClick = {
                                    resolviendo = true
                                    scope.launch {
                                        runCatching {
                                            IncidenciaRepository.marcarResuelta(empresaId, incidencia.idReal, true)
                                        }
                                        incidenciaResuelta = true
                                        resolviendo = false
                                    }
                                },
                                enabled = !resolviendo,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NaranjaLogicTruck)
                            ) {
                                Text(if (resolviendo) "Marcando..." else "Marcar incidencia como resuelta")
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (mensajes.isEmpty()) {
                    Text(
                        "Todavía no hay mensajes en esta conversación.",
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
}
