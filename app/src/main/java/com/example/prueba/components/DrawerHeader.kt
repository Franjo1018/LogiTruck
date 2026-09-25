package com.example.prueba.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

// Mismo naranja del botón "Iniciar sesión" en LoginScreen (NaranjaCTA = 0xFFE0983E), para que
// el menú lateral se sienta parte de la misma app en vez de usar el lila por defecto de
// Material. NaranjaClaro es un tono más claro para la parte "de brillo" del fondo diagonal, y
// PlomoOscuro/PlomoMedio son los grises ("plomo") que pidió Francisco en vez del morado.
val NaranjaLogicTruck = Color(0xFFE0983E)
private val NaranjaClaro = Color(0xFFF2B569)
private val PlomoOscuro = Color(0xFF57534E)
val PlomoMedio = Color(0xFF6B6B6B)

/** Colores para NavigationDrawerItem: ítem seleccionado en naranja, el resto en plomo. */
@Composable
fun coloresMenuLateral() = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = NaranjaLogicTruck.copy(alpha = 0.12f),
    selectedIconColor = NaranjaLogicTruck,
    selectedTextColor = NaranjaLogicTruck,
    unselectedIconColor = PlomoMedio,
    unselectedTextColor = Color(0xFF3C3C3C)
)

/** Colores para FilterChip: chip seleccionado en naranja, igual que el resto del menú. */
@Composable
fun coloresChipNaranja() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = NaranjaLogicTruck.copy(alpha = 0.15f),
    selectedLabelColor = NaranjaLogicTruck,
    selectedLeadingIconColor = NaranjaLogicTruck
)

/**
 * Colores para el TopAppBar de cualquier pantalla: fondo naranja (el mismo de los botones
 * principales) en vez del blanco por defecto de Material, con el título y los íconos (incluido
 * el botón de menú ☰ superpuesto por AdminHostScreen/ConductorHostScreen) en blanco para que
 * se noten sobre el naranja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun coloresTopBarNaranja() = TopAppBarDefaults.topAppBarColors(
    containerColor = NaranjaLogicTruck,
    titleContentColor = Color.White,
    navigationIconContentColor = Color.White,
    actionIconContentColor = Color.White
)

// Paleta usada solo para el patrón de fondo "low poly": tonos derivados de NaranjaLogicTruck y
// PlomoMedio/PlomoOscuro, más un par de tonos claros para que el mosaico tenga variedad, en vez
// de una transición lisa de dos colores nada más. A partir de la imagen de referencia que envió
// Francisco (triángulos tipo "low poly"), pero recoloreada a naranja/plomo.
private val NaranjaOscuroPatron = Color(0xFFA8641E)
private val NaranjaClaroPatron = Color(0xFFF0BD78)
private val CremaPatron = Color(0xFFF7D6A8)
private val PlomoClaroPatron = Color(0xFF96968F)
private val GrisBeigePatron = Color(0xFFC4C1BC)

private data class TrianguloPatron(val p1: Offset, val p2: Offset, val p3: Offset, val color: Color)

/**
 * Fondo tipo "mosaico low poly" (triángulos con degradado naranja → plomo, mismo estilo que la
 * imagen de referencia que mostró Francisco pero con la paleta de la app) para usar detrás del
 * contenido de cualquier pantalla en vez de un fondo blanco liso. Se mezcla al 30% con blanco
 * para que no le quite legibilidad a lo que va encima (nivel elegido por Francisco de las tres
 * opciones que se le mostraron). No se usa en "Panel", que ya tiene su propio encabezado
 * naranja de bienvenida.
 *
 * La malla de triángulos se genera una sola vez por tamaño de pantalla (drawWithCache, con una
 * semilla fija) en vez de recalcularse en cada frame, y en vez de una verdadera triangulación de
 * Delaunay (no hay una librería de eso en Compose) se arma con una grilla con "jitter" (los
 * puntos de la grilla se desplazan un poco al azar), que da un efecto visual muy parecido.
 */
fun Modifier.fondoPatronLogicTruck(): Modifier = this.drawWithCache {
    val columnas = 6
    val filas = 11
    val rnd = Random(7)
    val anchoCelda = size.width / columnas
    val altoCelda = size.height / filas

    val puntos = Array(filas + 1) { fila ->
        Array(columnas + 1) { columna ->
            val jitterX = (rnd.nextFloat() - 0.5f) * anchoCelda * 0.7f
            val jitterY = (rnd.nextFloat() - 0.5f) * altoCelda * 0.7f
            Offset(columna * anchoCelda + jitterX, fila * altoCelda + jitterY)
        }
    }

    val calidos = listOf(NaranjaOscuroPatron, NaranjaLogicTruck, NaranjaClaroPatron, CremaPatron)
    val frios = listOf(PlomoOscuro, PlomoMedio, PlomoClaroPatron, GrisBeigePatron)

    fun colorEnPosicion(cx: Float, cy: Float): Color {
        var t = (cx / size.width) * 0.5f + (cy / size.height) * 0.5f
        t += (rnd.nextFloat() - 0.5f) * 0.3f
        t = t.coerceIn(0f, 1f)
        val base = if (t < 0.5f) {
            lerp(calidos.first(), calidos.last(), t / 0.5f)
        } else {
            lerp(frios.first(), frios.last(), (t - 0.5f) / 0.5f)
        }
        return if (rnd.nextFloat() < 0.25f) {
            lerp(base, calidos[rnd.nextInt(calidos.size)], 0.35f)
        } else {
            base
        }
    }

    val triangulos = mutableListOf<TrianguloPatron>()
    for (fila in 0 until filas) {
        for (columna in 0 until columnas) {
            val p00 = puntos[fila][columna]
            val p10 = puntos[fila][columna + 1]
            val p01 = puntos[fila + 1][columna]
            val p11 = puntos[fila + 1][columna + 1]
            if (rnd.nextBoolean()) {
                triangulos += TrianguloPatron(p00, p10, p11, colorEnPosicion((p00.x + p10.x + p11.x) / 3f, (p00.y + p10.y + p11.y) / 3f))
                triangulos += TrianguloPatron(p00, p11, p01, colorEnPosicion((p00.x + p11.x + p01.x) / 3f, (p00.y + p11.y + p01.y) / 3f))
            } else {
                triangulos += TrianguloPatron(p00, p10, p01, colorEnPosicion((p00.x + p10.x + p01.x) / 3f, (p00.y + p10.y + p01.y) / 3f))
                triangulos += TrianguloPatron(p10, p11, p01, colorEnPosicion((p10.x + p11.x + p01.x) / 3f, (p10.y + p11.y + p01.y) / 3f))
            }
        }
    }

    onDrawBehind {
        drawRect(color = Color.White)
        triangulos.forEach { triangulo ->
            val colorTenue = lerp(Color.White, triangulo.color, 0.30f)
            val path = Path().apply {
                moveTo(triangulo.p1.x, triangulo.p1.y)
                lineTo(triangulo.p2.x, triangulo.p2.y)
                lineTo(triangulo.p3.x, triangulo.p3.y)
                close()
            }
            drawPath(path = path, color = colorTenue)
        }
    }
}

/**
 * Encabezado del menú lateral: fondo diagonal naranja/plomo (mismo espíritu que el fondo del
 * login, en vez del morado por defecto de Material) con un avatar circular de iniciales, el
 * nombre de la persona y un subtítulo (su rol). No hay foto de perfil real ni varias cuentas
 * (a diferencia de apps como Gmail), así que el encabezado se simplifica a eso.
 */
@Composable
fun DrawerHeader(nombre: String, subtitulo: String) {
    val iniciales = nombre.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .ifBlank { "?" }

    Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ancho = size.width
            val alto = size.height

            drawRect(color = PlomoOscuro)

            drawPath(
                path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(ancho * 0.78f, 0f)
                    lineTo(0f, alto)
                    close()
                },
                color = NaranjaLogicTruck
            )
            drawPath(
                path = Path().apply {
                    moveTo(ancho * 0.32f, 0f)
                    lineTo(ancho, 0f)
                    lineTo(ancho, alto * 0.5f)
                    lineTo(ancho * 0.58f, alto)
                    lineTo(ancho * 0.18f, alto)
                    close()
                },
                color = NaranjaClaro.copy(alpha = 0.55f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(iniciales, color = NaranjaLogicTruck, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(subtitulo, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
    }
}
