package com.example.prueba.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.R

// Paleta basada en el diseño (ajusta si tienes los hex exactos desde Dev Mode)
private val NaranjaCTA = Color(0xFFE0983E)
private val OverlayOscuro = Color(0xFF1A1410)

private val FondoAtardecer = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE07B39).copy(alpha = 0.35f), // naranja suave arriba
        Color(0xFFE0983E).copy(alpha = 0.55f), // naranja medio
        Color(0xFF8B4513).copy(alpha = 0.75f)  // marrón/naranja oscuro abajo
    )
)
@Composable
fun LoginScreen(
    onIniciarSesionClick: () -> Unit,
    onRegistrarseClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Imagen de fondo (camión al atardecer)
        Image(
            painter = painterResource(id = R.drawable.bg_login_truck),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Contenido: logo + botón + texto de registro
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Logo "LogicTruck" (usa tu vector exportado desde Figma)
            Image(
                painter = painterResource(id = R.drawable.img),
                contentDescription = "LogicTruck",
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(320.dp)
                    .padding(bottom = 32.dp),
                contentScale = ContentScale.Fit
            )

            Button(
                onClick = onIniciarSesionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .clip(RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaCTA)
            ) {
                Text(
                    text = "Iniciar Sesión",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = buildAnnotatedString {
                    append("Regístrese con una ")
                    withStyle(style = SpanStyle(color = NaranjaCTA, fontWeight = FontWeight.Bold)) {
                        append("nueva cuenta")
                    }
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(onIniciarSesionClick = {}, onRegistrarseClick = {})
}