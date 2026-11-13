package com.christianacadez.applocker.ui.pin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.christianacadez.applocker.data.PinResult
import com.christianacadez.applocker.data.PinStore

@Composable
fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(PinStore.MAX_LENGTH)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier.fillMaxWidth()
    )
}

/** Campo + botón para escribir el PIN y comprobarlo. Lo usan la pantalla de entrada y el diálogo. */
@Composable
fun PinEntry(verify: (String) -> PinResult, onSuccess: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Column {
        PinField(value = pin, onValueChange = { pin = it }, label = "PIN")
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                when (val result = verify(pin)) {
                    PinResult.Ok -> onSuccess()
                    is PinResult.Wrong -> {
                        message = "PIN incorrecto. Intentos restantes: ${result.attemptsLeft}"
                        pin = ""
                    }
                    is PinResult.LockedOut -> {
                        message = "Demasiados intentos. Prueba en ${(result.secondsLeft + 59) / 60} min."
                        pin = ""
                    }
                }
            },
            enabled = pin.length >= PinStore.MIN_LENGTH,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Confirmar") }
    }
}

/** Pantalla completa que se muestra al abrir la app. */
@Composable
fun PinGateScreen(verify: (String) -> PinResult, onSuccess: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Introduce tu PIN", fontSize = 28.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(24.dp))
        PinEntry(verify, onSuccess)
    }
}

/** Primera vez: crear el PIN. */
@Composable
fun PinSetupScreen(onPinSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val error = when {
        pin.length < PinStore.MIN_LENGTH -> "Mínimo ${PinStore.MIN_LENGTH} dígitos"
        pin != confirm -> "Los PIN no coinciden"
        else -> null
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Crea tu PIN", fontSize = 28.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(8.dp))
        Text(
            "Protege la app y las emergencias. Elige uno que no recuerdes con facilidad " +
                    "en un impulso; puedes anotarlo en otro lugar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(24.dp))
        PinField(pin, { pin = it }, "PIN (${PinStore.MIN_LENGTH} a ${PinStore.MAX_LENGTH} dígitos)")
        Spacer(Modifier.height(12.dp))
        PinField(confirm, { confirm = it }, "Repite el PIN")
        if (error != null && (pin.isNotEmpty() || confirm.isNotEmpty())) {
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onPinSet(pin) },
            enabled = error == null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar PIN") }
    }
}

/** Diálogo para pedir el PIN dentro de otra pantalla (la emergencia). */
@Composable
fun PinDialog(
    title: String,
    verify: (String) -> PinResult,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { PinEntry(verify, onSuccess) },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}