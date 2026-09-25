package com.example.prueba.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.prueba.data.sugerirUsuarioDesdeNombre

private val NaranjaCTA = Color(0xFFB5651D)

/** De qué forma se registra la cuenta nueva: fundando una empresa o uniéndose a una existente. */
enum class TipoRegistro { CREAR_EMPRESA, UNIRSE_CODIGO }

/** Datos capturados en el formulario de registro. */
data class RegistroForm(
    val tipo: TipoRegistro = TipoRegistro.CREAR_EMPRESA,
    val nombreCompleto: String = "",
    val usuario: String = "",
    val password: String = "",
    val confirmarPassword: String = "",
    val nombreEmpresa: String = "",
    val codigoInvitacion: String = ""
)

/**
 * Pantalla de registro de una nueva cuenta. Como la app ahora es multiempresa, hay dos formas
 * de registrarse: creando una empresa nueva (quien la crea queda como administrador) o
 * uniéndose a una empresa existente con un código de invitación que le da un administrador
 * (esa persona entra como conductor/despachador, según lo que diga la invitación).
 * `errorServidor` es lo que AuthRepository/Firebase devuelve cuando el registro falla.
 */
@Composable
fun RegistroScreen(
    onCrearCuentaClick: (RegistroForm) -> Unit = {},
    onYaTengoCuentaClick: () -> Unit = {},
    errorServidor: String? = null,
    cargando: Boolean = false
) {
    var form by remember { mutableStateOf(RegistroForm()) }
    var error by remember { mutableStateOf<String?>(null) }
    // Mientras la persona no toque el campo "Usuario" a mano, se lo sugerimos solo con su
    // nombre (sin tildes ni espacios) para que no tenga que inventarse uno — pensado para
    // choferes a los que no les gusta o no recuerdan usar un correo/usuario aparte. Si lo
    // edita directamente, se respeta lo que escriba y se deja de autocompletar.
    var usuarioEditadoAMano by remember { mutableStateOf(false) }

    val datosBaseValidos = form.nombreCompleto.isNotBlank() &&
        form.usuario.isNotBlank() &&
        form.password.length >= 6 &&
        form.password == form.confirmarPassword

    val formValido = datosBaseValidos && when (form.tipo) {
        TipoRegistro.CREAR_EMPRESA -> form.nombreEmpresa.isNotBlank()
        TipoRegistro.UNIRSE_CODIGO -> form.codigoInvitacion.isNotBlank()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_login_truck),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.height(270.dp))

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
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Text(
                        text = "Crear cuenta",
                        color = NaranjaCTA,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        TipoRegistro.entries.forEach { tipo ->
                            val seleccionado = tipo == form.tipo
                            val texto = if (tipo == TipoRegistro.CREAR_EMPRESA) "Nueva empresa" else "Unirme con código"
                            if (seleccionado) {
                                Button(
                                    onClick = { form = form.copy(tipo = tipo); error = null },
                                    modifier = Modifier.weight(1f).padding(end = if (tipo == TipoRegistro.CREAR_EMPRESA) 4.dp else 0.dp, start = if (tipo == TipoRegistro.UNIRSE_CODIGO) 4.dp else 0.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaCTA)
                                ) { Text(texto, fontSize = 12.sp) }
                            } else {
                                OutlinedButton(
                                    onClick = { form = form.copy(tipo = tipo); error = null },
                                    modifier = Modifier.weight(1f).padding(end = if (tipo == TipoRegistro.CREAR_EMPRESA) 4.dp else 0.dp, start = if (tipo == TipoRegistro.UNIRSE_CODIGO) 4.dp else 0.dp)
                                ) { Text(texto, fontSize = 12.sp) }
                            }
                        }
                    }

                    TextField(
                        value = form.nombreCompleto,
                        onValueChange = { nuevoNombre ->
                            form = form.copy(
                                nombreCompleto = nuevoNombre,
                                usuario = if (usuarioEditadoAMano) form.usuario else sugerirUsuarioDesdeNombre(nuevoNombre)
                            )
                            error = null
                        },
                        placeholder = { Text("Nombre completo") },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    TextField(
                        value = form.usuario,
                        onValueChange = {
                            form = form.copy(usuario = it)
                            usuarioEditadoAMano = true
                            error = null
                        },
                        placeholder = { Text("Usuario") },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Text(
                        text = "Se genera solo con tu nombre — es lo que vas a usar para iniciar sesión. " +
                            "Puedes cambiarlo si quieres.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (form.tipo == TipoRegistro.CREAR_EMPRESA) {
                        TextField(
                            value = form.nombreEmpresa,
                            onValueChange = { form = form.copy(nombreEmpresa = it); error = null },
                            placeholder = { Text("Nombre de la empresa") },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    } else {
                        TextField(
                            value = form.codigoInvitacion,
                            onValueChange = { form = form.copy(codigoInvitacion = it); error = null },
                            placeholder = { Text("Código de invitación") },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }

                    TextField(
                        value = form.password,
                        onValueChange = { form = form.copy(password = it); error = null },
                        placeholder = { Text("Contraseña (mínimo 6 caracteres)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    TextField(
                        value = form.confirmarPassword,
                        onValueChange = { form = form.copy(confirmarPassword = it); error = null },
                        placeholder = { Text("Confirmar contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    if (form.password.isNotEmpty() && form.confirmarPassword.isNotEmpty() &&
                        form.password != form.confirmarPassword
                    ) {
                        Text(
                            text = "Las contraseñas no coinciden",
                            color = MaterialThemeErrorColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    error?.let {
                        Text(text = it, color = MaterialThemeErrorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    errorServidor?.let {
                        Text(text = it, color = MaterialThemeErrorColor, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    Button(
                        onClick = { onCrearCuentaClick(form) },
                        enabled = formValido && !cargando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 24.dp)
                            .clip(RoundedCornerShape(28.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaCTA)
                    ) {
                        Text(
                            if (cargando) "Creando cuenta..." else "Crear cuenta",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "¿Ya tienes cuenta? Inicia sesión",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clickable(onClick = onYaTengoCuentaClick),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private val MaterialThemeErrorColor = Color(0xFFB3261E)
