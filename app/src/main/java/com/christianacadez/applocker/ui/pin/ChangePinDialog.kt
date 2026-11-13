package com.christianacadez.applocker.ui.pin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.christianacadez.applocker.data.PinResult
import com.christianacadez.applocker.data.PinStore

@Composable
fun ChangePinDialog(
    verify: (String) -> PinResult,
    onChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    val newIsValid = newPin.length >= PinStore.MIN_LENGTH && newPin == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar PIN") },
        text = {
            Column {
                PinField(current, { current = it }, "PIN actual")
                Spacer(Modifier.height(8.dp))
                PinField(newPin, { newPin = it }, "PIN nuevo (${PinStore.MIN_LENGTH} a ${PinStore.MAX_LENGTH} dígitos)")
                Spacer(Modifier.height(8.dp))
                PinField(confirm, { confirm = it }, "Repite el PIN nuevo")
                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = current.length >= PinStore.MIN_LENGTH && newIsValid,
                onClick = {
                    when (val result = verify(current)) {
                        PinResult.Ok -> onChange(newPin)
                        is PinResult.Wrong -> {
                            message = "PIN actual incorrecto. Intentos restantes: ${result.attemptsLeft}"
                            current = ""
                        }
                        is PinResult.LockedOut -> {
                            message = "Demasiados intentos. Prueba en ${(result.secondsLeft + 59) / 60} min."
                            current = ""
                        }
                    }
                }
            ) { Text("Cambiar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}