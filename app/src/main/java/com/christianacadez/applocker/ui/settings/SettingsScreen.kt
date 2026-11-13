package com.christianacadez.applocker.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.christianacadez.applocker.data.PinStore
import com.christianacadez.applocker.data.SafetySettings
import com.christianacadez.applocker.data.SafetyValues
import com.christianacadez.applocker.ui.pin.ChangePinDialog

@Composable
fun SettingsScreen(
    serviceEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SafetySettings(context) }
    val pinStore = remember { PinStore(context) }
    val saved = remember { settings.load() }

    var waitText by remember { mutableStateOf(saved.waitSeconds.toString()) }
    var extraText by remember { mutableStateOf(saved.extraMinutes.toString()) }
    var maxText by remember { mutableStateOf(saved.maxUnlocksPerDay.toString()) }
    var emergencyNotice by remember { mutableStateOf<String?>(null) }
    var pinNotice by remember { mutableStateOf<String?>(null) }
    var changingPin by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val wait = waitText.toIntOrNull()
    val extra = extraText.toIntOrNull()
    val max = maxText.toIntOrNull()
    val error = when {
        wait == null || wait !in SafetySettings.WAIT_RANGE ->
            "La espera debe estar entre ${SafetySettings.WAIT_RANGE.first} y ${SafetySettings.WAIT_RANGE.last} segundos"
        extra == null || extra !in SafetySettings.EXTRA_RANGE ->
            "El tiempo extra debe estar entre ${SafetySettings.EXTRA_RANGE.first} y ${SafetySettings.EXTRA_RANGE.last} minutos"
        max == null || max !in SafetySettings.MAX_RANGE ->
            "Las emergencias por día deben estar entre ${SafetySettings.MAX_RANGE.first} y ${SafetySettings.MAX_RANGE.last}"
        else -> null
    }
    val muted = MaterialTheme.colorScheme.outline

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ajustes", fontSize = 28.sp, fontWeight = FontWeight.Light)
            TextButton(onClick = onBack) { Text("Volver") }
        }

        // --- Servicio de bloqueo ---
        Spacer(Modifier.height(24.dp))
        Text("Servicio de bloqueo", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            if (serviceEnabled) "Activo" else "Apagado: los límites no se aplican",
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
        if (!serviceEnabled) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenAccessibility) { Text("Activar") }
        }

        // --- Emergencias ---
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))
        Text("Emergencias", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        NumberField(waitText, { waitText = it }, "Espera antes de desbloquear (segundos)")
        Spacer(Modifier.height(8.dp))
        NumberField(extraText, { extraText = it }, "Minutos extra por emergencia")
        Spacer(Modifier.height(8.dp))
        NumberField(maxText, { maxText = it }, "Emergencias por día (0 = desactivadas)")
        Spacer(Modifier.height(8.dp))
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = muted)
            Spacer(Modifier.height(8.dp))
        }
        emergencyNotice?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = muted)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (wait != null && extra != null && max != null) {
                    settings.save(SafetyValues(wait, extra, max))
                    emergencyNotice = "Guardado"
                }
            },
            enabled = error == null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar") }

        // --- PIN ---
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))
        Text("PIN", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        pinNotice?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = muted)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = { pinNotice = null; changingPin = true }) { Text("Cambiar PIN") }
        Spacer(Modifier.height(32.dp))
    }

    if (changingPin) {
        ChangePinDialog(
            verify = pinStore::verify,
            onChange = { newPin ->
                pinStore.setPin(newPin)
                pinNotice = "PIN actualizado"
                changingPin = false
            },
            onDismiss = { changingPin = false }
        )
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}