@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.conductor

import com.example.prueba.data.AlmacenamientoRepository
import com.example.prueba.data.ChecklistRepository
import com.example.prueba.data.CombustibleRepository

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.components.BotonTomarFoto
import com.example.prueba.components.IconoTomarFoto
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.PlomoMedio
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck
import kotlinx.coroutines.launch

/**
 * Un ítem del checklist previaje. Si requiereFoto es true, hay que tomar una foto real de
 * evidencia (con la cámara del dispositivo, ver IconoTomarFoto) además de marcar el checkbox
 * para poder darlo por completado; fotoUri es esa foto (local, no se sube a Storage: solo sirve
 * para exigir la evidencia en el momento, no queda guardada para verse después desde el admin).
 */
data class ItemChecklistPrevio(
    val id: String,
    val descripcion: String,
    val requiereFoto: Boolean = false,
    val completado: Boolean = false,
    val fotoUri: Uri? = null
)

private val checklistBase = listOf(
    ItemChecklistPrevio("frenos", "Revisión de frenos", requiereFoto = false),
    ItemChecklistPrevio("aceite", "Nivel de aceite", requiereFoto = false),
    ItemChecklistPrevio("llantas", "Presión y estado de llantas", requiereFoto = true),
    ItemChecklistPrevio("luces", "Luces delanteras y posteriores", requiereFoto = false),
    ItemChecklistPrevio("carga", "Carga bien asegurada", requiereFoto = true),
    ItemChecklistPrevio("documentos", "Documentos del vehículo a bordo", requiereFoto = false)
)

/**
 * Checklist previaje que el conductor completa antes de iniciar cada viaje: ítems de inspección
 * del vehículo, kilometraje con el que sale, y la carga inicial de combustible (en galones, con
 * foto real de la factura). El encabezado (saludo naranja + tarjetas con franja de color a la
 * izquierda) sigue el mismo patrón visual naranja/plomo que AdminDashboardScreen. origenDestino
 * es texto libre: la ruta puede ser entre cualquier par de provincias o departamentos, no solo
 * Lima–Trujillo (ese fue solo un ejemplo usado durante el desarrollo).
 *
 * Al tocar "Iniciar viaje" se guarda el checklist (ChecklistRepository, una vez por viaje) — si
 * se tomó foto de la factura, primero se sube a Firebase Storage (AlmacenamientoRepository) y se
 * guarda su URL junto con el resto — y, si se declaró combustible inicial, se registra como un
 * RegistroCombustible más ligado a este viaje (CombustibleRepository) — la misma carga inicial
 * cuenta para el reporte de huella de carbono del admin. El registro de combustible NO termina
 * ahí: durante el viaje el conductor puede volver a cargar combustible una o más veces desde
 * "Registro de combustible" en el menú; cada carga (la inicial de aquí y las de en ruta) queda
 * ligada al mismo viaje.
 */
@Composable
fun ChecklistPreviajeScreen(
    empresaId: String = "",
    viajeIdReal: String = "",
    conductorUid: String = "",
    conductorNombre: String = "",
    nombre: String = "",
    origenDestino: String = "Sin ruta asignada",
    onIniciarViajeClick: () -> Unit = {}
) {
    // Se resetea si cambia el viaje (viajeIdReal como key), para no arrastrar el checklist de
    // un viaje anterior al siguiente.
    var items by remember(viajeIdReal) { mutableStateOf(checklistBase) }
    var kilometrajeInicial by remember(viajeIdReal) { mutableStateOf("") }
    var combustibleInicialGalones by remember(viajeIdReal) { mutableStateOf("") }
    var facturaUri by remember(viajeIdReal) { mutableStateOf<Uri?>(null) }
    var guardando by remember(viajeIdReal) { mutableStateOf(false) }
    var error by remember(viajeIdReal) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val itemsCompletos = items.all { it.completado && (!it.requiereFoto || it.fotoUri != null) }
    val datosCompletos = kilometrajeInicial.isNotBlank() && combustibleInicialGalones.isNotBlank()
    val todosCompletos = itemsCompletos && datosCompletos && !guardando

    fun iniciarViaje() {
        error = null
        val galones = combustibleInicialGalones.replace(",", ".").toDoubleOrNull()
        if (galones == null) {
            error = "El combustible cargado debe ser un número válido"
            return
        }
        guardando = true
        scope.launch {
            try {
                var facturaUrl = ""
                facturaUri?.let { uri ->
                    facturaUrl = AlmacenamientoRepository.subirFoto(
                        empresaId = empresaId,
                        ruta = "checklist/$viajeIdReal/factura.jpg",
                        archivoLocal = uri
                    )
                }
                ChecklistRepository.guardar(
                    empresaId = empresaId,
                    viajeIdReal = viajeIdReal,
                    items = items.associate { it.id to it.completado },
                    kilometrajeInicial = kilometrajeInicial,
                    facturaCombustibleAdjunta = facturaUri != null,
                    facturaCombustibleUrl = facturaUrl
                )
                if (galones > 0) {
                    CombustibleRepository.registrar(
                        empresaId = empresaId,
                        conductorUid = conductorUid,
                        conductorNombre = conductorNombre,
                        galones = galones,
                        lugar = "Carga inicial (checklist previaje)",
                        viajeIdReal = viajeIdReal
                    )
                }
                onIniciarViajeClick()
            } catch (e: Exception) {
                error = "No se pudo guardar el checklist, revisa tu conexión"
            } finally {
                guardando = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Checklist previaje") },
                // Deja libre la franja de 56dp donde AdminHostScreen/ConductorHostScreen
                // superponen el ícono de menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                Button(
                    onClick = ::iniciarViaje,
                    enabled = todosCompletos,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaLogicTruck)
                ) {
                    Text(
                        when {
                            guardando -> "Guardando..."
                            !itemsCompletos -> "Completa el checklist para continuar"
                            !datosCompletos -> "Completa kilometraje y combustible"
                            else -> "Iniciar viaje"
                        }
                    )
                }
            }
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
                        "Tu próximo viaje: $origenDestino",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            item {
                Text(
                    "Checklist previaje",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(items) { item ->
                ChecklistFila(
                    item = item,
                    onToggle = {
                        items = items.map { if (it.id == item.id) it.copy(completado = !it.completado) else it }
                    },
                    onFotoCapturada = { uri ->
                        items = items.map { if (it.id == item.id) it.copy(fotoUri = uri) else it }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item {
                Text(
                    "Carga inicial de combustible",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = kilometrajeInicial,
                        onValueChange = { kilometrajeInicial = it },
                        label = { Text("Kilometraje de salida") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = combustibleInicialGalones,
                        onValueChange = { combustibleInicialGalones = it },
                        label = { Text("Combustible cargado (galones)") },
                        placeholder = { Text("Ej: 300") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    BotonTomarFoto(
                        fotoUri = facturaUri,
                        onFotoCapturada = { uri -> facturaUri = uri },
                        etiqueta = "Adjuntar foto de la factura (opcional)"
                    )
                    Text(
                        "Si en la ruta vuelves a cargar combustible, regístralo aparte desde " +
                            "\"Registro de combustible\" en el menú: puedes hacerlo más de una vez por viaje.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ChecklistFila(
    item: ItemChecklistPrevio,
    onToggle: () -> Unit,
    onFotoCapturada: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorFranja = if (item.completado) NaranjaLogicTruck else PlomoMedio.copy(alpha = 0.4f)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(colorFranja)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = item.completado, onCheckedChange = { onToggle() })
                Text(item.descripcion, modifier = Modifier.weight(1f))
                if (item.requiereFoto) {
                    IconoTomarFoto(
                        fotoUri = item.fotoUri,
                        onFotoCapturada = onFotoCapturada,
                        contentDescription = "Tomar foto de evidencia"
                    )
                }
            }
        }
    }
}
