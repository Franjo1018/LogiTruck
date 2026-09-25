@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.prueba.screens.admin

import com.example.prueba.data.ChecklistPrevio
import com.example.prueba.data.ChecklistRepository
import com.example.prueba.data.CombustibleRepository
import com.example.prueba.data.RegistroCombustible
import com.example.prueba.data.UbicacionConductor
import com.example.prueba.data.UbicacionRepository
import com.example.prueba.data.Viaje
import com.example.prueba.data.ViajeRepository

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import com.example.prueba.components.NaranjaLogicTruck
import com.example.prueba.components.PlomoMedio
import com.example.prueba.components.coloresTopBarNaranja
import com.example.prueba.components.fondoPatronLogicTruck
import kotlinx.coroutines.delay
import com.example.prueba.BuildConfig
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

/** Cada cuántos milisegundos se vuelve a leer Firebase para refrescar las posiciones. */
private const val INTERVALO_REFRESCO_MS = 15_000L

/**
 * Vista con la última ubicación GPS reportada por cada conductor (empresas/{empresaId}/
 * ubicaciones vía UbicacionRepository), dibujada sobre un mapa real (calles/terreno) con
 * MapLibre + tiles de MapTiler (API key propia y gratuita, ver app/build.gradle y
 * local.properties — MAPTILER_API_KEY). Se cambió de osmdroid/OpenStreetMap a esto porque los
 * tiles anónimos de OSM se bloquean para apps reales (su política es solo para pruebas chicas);
 * con una key propia de MapTiler cada quien tiene su propia cuota, sin compartirla con miles de
 * otras apps. Un marcador por conductor, más una lista debajo con el detalle de cada uno. Se
 * refresca sola cada 15s mientras la pantalla está abierta, para ver movimiento sin recargar a
 * mano.
 */
@Composable
fun FlotaMapaScreen(empresaId: String) {
    var vehiculos by remember { mutableStateOf<List<UbicacionConductor>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var conductorSeleccionado by remember { mutableStateOf<UbicacionConductor?>(null) }

    LaunchedEffect(empresaId) {
        while (true) {
            try {
                vehiculos = UbicacionRepository.listar(empresaId)
            } catch (e: Exception) {
                // Sin conexión en este ciclo: se mantiene la última lista conocida.
            } finally {
                cargando = false
            }
            delay(INTERVALO_REFRESCO_MS)
        }
    }

    Scaffold(
        modifier = Modifier.fondoPatronLogicTruck(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Flota en tiempo real") },
                // Deja libre la franja de 56dp donde AdminHostScreen superpone el ícono de
                // menú (☰), para que no quede debajo del título.
                navigationIcon = { Spacer(modifier = Modifier.width(56.dp)) },
                colors = coloresTopBarNaranja()
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(NaranjaLogicTruck.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    cargando -> CircularProgressIndicator()
                    vehiculos.isEmpty() -> Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = "Mapa de flota",
                        tint = NaranjaLogicTruck,
                        modifier = Modifier.size(40.dp)
                    )
                    else -> MapaMapLibre(
                        vehiculos = vehiculos,
                        modifier = Modifier.fillMaxSize(),
                        onConductorClick = { conductorSeleccionado = it }
                    )
                }
            }

            Text(
                "Vehículos activos (${vehiculos.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
            )

            if (vehiculos.isEmpty() && !cargando) {
                Text(
                    "Todavía no hay conductores reportando ubicación. Se llena cuando un " +
                        "conductor abre la app con el GPS activado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vehiculos) { vehiculo ->
                    val segundosDesdeActualizacion = (System.currentTimeMillis() - vehiculo.timestampMs) / 1000
                    val reciente = segundosDesdeActualizacion < 120
                    val colorFranja = if (reciente) NaranjaLogicTruck else PlomoMedio.copy(alpha = 0.4f)
                    Card(
                        onClick = { conductorSeleccionado = vehiculo },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(colorFranja))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(vehiculo.nombre, fontWeight = FontWeight.Medium)
                                    Text(
                                        "%.5f, %.5f".format(vehiculo.lat, vehiculo.lon),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    if (segundosDesdeActualizacion < 60) "hace ${segundosDesdeActualizacion}s"
                                    else "hace ${segundosDesdeActualizacion / 60} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    conductorSeleccionado?.let { conductor ->
        DetalleConductorSheet(
            empresaId = empresaId,
            conductor = conductor,
            onDismiss = { conductorSeleccionado = null }
        )
    }
}

/**
 * Mapa real (MapLibre + estilo de MapTiler) con un marcador por conductor. Con un solo conductor
 * se centra en él con un zoom cómodo (calle/barrio); con varios, la cámara se ajusta para que
 * todos queden visibles a la vez. El MapView es una vista clásica de Android (no Compose), así
 * que se integra con AndroidView; se recuerda una sola instancia y se actualizan sus marcadores
 * cuando cambia la lista de vehículos, en vez de recrearla en cada recomposición.
 *
 * Si BuildConfig.MAPTILER_API_KEY está vacío (no configuraste local.properties todavía), el
 * estilo no carga y el mapa se ve en blanco/gris — no es un error de código, falta la key.
 */
@Composable
private fun MapaMapLibre(
    vehiculos: List<UbicacionConductor>,
    modifier: Modifier = Modifier,
    onConductorClick: (UbicacionConductor) -> Unit = {}
) {
    val context = LocalContext.current
    var mapaListo by remember { mutableStateOf<MapLibreMap?>(null) }
    // A qué conductor corresponde cada marcador dibujado, para saber a quién se tocó al hacer clic.
    val conductorPorMarcador = remember { mutableMapOf<Long, UbicacionConductor>() }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            onStart()
            onResume()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            val estiloUrl = "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
            map.setStyle(Style.Builder().fromUri(estiloUrl))
            map.setOnMarkerClickListener { marcador ->
                conductorPorMarcador[marcador.id]?.let { onConductorClick(it) }
                // true = ya se manejó el clic (abrimos el detalle); no muestra el globito
                // (infowindow) por defecto de MapLibre encima del marcador.
                true
            }
            mapaListo = map
        }
    }

    LaunchedEffect(vehiculos, mapaListo) {
        val map = mapaListo ?: return@LaunchedEffect
        map.clear()
        conductorPorMarcador.clear()
        vehiculos.forEach { vehiculo ->
            val marcador = map.addMarker(
                MarkerOptions()
                    .position(LatLng(vehiculo.lat, vehiculo.lon))
                    .title(vehiculo.nombre)
            )
            conductorPorMarcador[marcador.id] = vehiculo
        }
        when {
            vehiculos.size == 1 -> {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(vehiculos[0].lat, vehiculos[0].lon), 15.0)
                )
            }
            vehiculos.size > 1 -> {
                val limites = LatLngBounds.Builder()
                vehiculos.forEach { limites.include(LatLng(it.lat, it.lon)) }
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(limites.build(), 100))
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/**
 * Detalle de un conductor al tocar su marcador en el mapa o su tarjeta en la lista de abajo:
 * su viaje activo (ruta, vehículo, carga), el checklist previaje que declaró (kilometraje,
 * si adjuntó factura) y el combustible cargado en ese viaje hasta ahora. Todo son datos reales
 * de Firebase (ViajeRepository, ChecklistRepository, CombustibleRepository).
 * TODO: cuando se integre una cámara real, acá se mostraría la miniatura de la foto de la
 * factura en vez de solo el flag de sí/no.
 */
@Composable
private fun DetalleConductorSheet(
    empresaId: String,
    conductor: UbicacionConductor,
    onDismiss: () -> Unit
) {
    var viaje by remember(conductor.conductorUid) { mutableStateOf<Viaje?>(null) }
    var checklist by remember(conductor.conductorUid) { mutableStateOf<ChecklistPrevio?>(null) }
    var cargasCombustible by remember(conductor.conductorUid) { mutableStateOf<List<RegistroCombustible>>(emptyList()) }
    var cargando by remember(conductor.conductorUid) { mutableStateOf(true) }

    LaunchedEffect(empresaId, conductor.conductorUid) {
        cargando = true
        try {
            val viajeActivo = ViajeRepository.viajeActivoPorConductor(empresaId, conductor.conductorUid)
            viaje = viajeActivo
            if (viajeActivo != null) {
                checklist = ChecklistRepository.leer(empresaId, viajeActivo.idReal)
                cargasCombustible = CombustibleRepository.listarPorViaje(empresaId, viajeActivo.idReal)
            }
        } catch (e: Exception) {
            // Sin conexión: se queda sin detalle, pero igual se ve el nombre del conductor.
        } finally {
            cargando = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(conductor.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (cargando) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val v = viaje
                if (v == null) {
                    Text(
                        "Este conductor no tiene un viaje activo asignado en este momento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    DetalleFila("Ruta", "${v.origen} → ${v.destino}")
                    DetalleFila("Vehículo", v.vehiculoPlaca.ifBlank { "Sin asignar" })
                    DetalleFila(
                        "Carga",
                        v.descripcionCarga.ifBlank { "-" } +
                            if (v.pesoCargaKg.isNotBlank()) " (${v.pesoCargaKg} kg)" else ""
                    )
                    DetalleFila("Estado del viaje", v.estado.etiqueta)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Checklist previaje", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    val datosChecklist = checklist
                    if (datosChecklist == null) {
                        Text(
                            "Todavía no completa el checklist previaje de este viaje.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        DetalleFila(
                            "Kilometraje de salida",
                            datosChecklist.kilometrajeInicial.ifBlank { "-" }.let { if (it == "-") it else "$it km" }
                        )
                        DetalleFila(
                            "Factura de combustible",
                            if (datosChecklist.facturaCombustibleAdjunta) "Adjuntada" else "No adjuntada"
                        )
                        if (datosChecklist.facturaCombustibleUrl.isNotBlank()) {
                            AsyncImage(
                                model = datosChecklist.facturaCombustibleUrl,
                                contentDescription = "Foto de la factura de combustible",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    val totalGalones = cargasCombustible.sumOf { it.galones }
                    DetalleFila(
                        "Combustible cargado en este viaje",
                        if (cargasCombustible.isEmpty()) "Sin registros todavía"
                        else "%.1f gal (%d carga%s)".format(
                            totalGalones,
                            cargasCombustible.size,
                            if (cargasCombustible.size == 1) "" else "s"
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DetalleFila(etiqueta: String, valor: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
