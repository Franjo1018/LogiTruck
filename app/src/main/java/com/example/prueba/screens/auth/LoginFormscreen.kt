package com.example.prueba.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prueba.R

private val NaranjaCTA = Color(0xFFB5651D)
private val NaranjaClaro = Color(0xFFD9A468)

@Composable
fun LoginFormScreen(
    onIniciarSesionClick: (usuario: String, password: String) -> Unit,
    onNuevaCuentaClick: () -> Unit,
    onOlvidoPasswordClick: () -> Unit
) {
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {

        // Foto del camión, solo en la parte superior
        Image(
            painter = painterResource(id = R.drawable.bg_login_truck),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Espaciador que respeta la altura de la foto, menos el offset de superposición
            Box(modifier = Modifier.height(310.dp))

            // Card blanco con esquinas redondeadas arriba, superpuesto sobre la foto
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-32).dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    // Selector tipo tabs: Login / Nueva Cuenta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(0.15f)
                                .background(NaranjaCTA)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(0.15f)
                                .background(NaranjaClaro)
                                .padding(vertical = 14.dp)
                                .clickable(onClick = onNuevaCuentaClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Nueva", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Cuenta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    Text(
                        text = "Bienvenido a LogicTruck",
                        color = NaranjaCTA,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(top = 28.dp, bottom = 20.dp)
                    )

                    TextField(
                        value = usuario,
                        onValueChange = { usuario = it },
                        placeholder = { Text("Usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Button(
                        onClick = { onIniciarSesionClick(usuario, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.4f)
                            .height(86.dp)
                            .padding(top = 32.dp)
                            .clip(RoundedCornerShape(28.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaCTA)
                    ) {
                        Text("Iniciar Sesión", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Olvido su contraseña?",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clickable(onClick = onOlvidoPasswordClick),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}