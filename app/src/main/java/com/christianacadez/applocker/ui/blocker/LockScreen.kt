package com.christianacadez.applocker.ui.blocker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.christianacadez.applocker.data.PinResult
import com.christianacadez.applocker.ui.pin.PinDialog
import kotlinx.coroutines.delay

private val CountdownStyle = TextStyle(
    fontSize = 56.sp,
    fontWeight = FontWeight.Light,
    fontFeatureSettings = "tnum"   // dígitos de ancho fijo
)

private val WaitStyle = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Light,
    fontFeatureSettings = "tnum"
)

@Composable
fun LockScreen(
    appName: String,
    emergenciesLeft: Int,
    resetAtMs: Long,
    waitSeconds: Int,
    extraMinutes: Int,
    requiresPin: Boolean,
    verifyPin: (String) -> PinResult,
    onGranted: () -> Unit
) {
    var remaining by remember { mutableStateOf(timeUntil(resetAtMs)) }
    var secondsLeft by remember { mutableStateOf<Int?>(null) }
    var askingPin by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.outline

    LaunchedEffect(Unit) { visible = true }

    // Reloj hasta el final de la franja
    LaunchedEffect(resetAtMs) {
        while (true) {
            remaining = timeUntil(resetAtMs)
            delay(1_000)
        }
    }

    // Cuenta atrás de la emergencia
    LaunchedEffect(secondsLeft != null) {
        if (secondsLeft == null) return@LaunchedEffect
        while ((secondsLeft ?: 0) > 0) {
            delay(1_000)
            secondsLeft = (secondsLeft ?: 0) - 1
        }
        onGranted()
    }

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(500))) {
        Box(Modifier.fillMaxSize().padding(24.dp)) {
            Column(
                Modifier.align(Alignment.TopCenter).padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SE REINICIA EN", style = MaterialTheme.typography.labelMedium, color = muted)
                Spacer(Modifier.height(8.dp))
                Text(remaining, style = CountdownStyle)
                if (appName.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$appName · límite alcanzado",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
            }

            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Bloqueado",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(40.dp))

                val waiting = secondsLeft
                if (waiting != null) {
                    Text("Espera $waiting s", style = WaitStyle)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { 1f - waiting / waitSeconds.toFloat() },
                        modifier = Modifier.width(160.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { secondsLeft = null }) { Text("Cancelar") }
                } else {
                    OutlinedButton(
                        onClick = {
                            if (requiresPin) askingPin = true else secondsLeft = waitSeconds
                        },
                        enabled = emergenciesLeft > 0
                    ) {
                        Text(if (emergenciesLeft > 0) "Emergencia" else "Sin emergencias hoy")
                    }
                    if (emergenciesLeft > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Quedan $emergenciesLeft hoy · +$extraMinutes min",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                    }
                }
            }
        }
    }

    if (askingPin) {
        PinDialog(
            title = "PIN para la emergencia",
            verify = verifyPin,
            onSuccess = {
                askingPin = false
                secondsLeft = waitSeconds
            },
            onDismiss = { askingPin = false }
        )
    }
}

/** Tiempo hasta [resetAtMs], en formato HH:MM. */
private fun timeUntil(resetAtMs: Long): String {
    val seconds = ((resetAtMs - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
    val minutes = (seconds + 59) / 60
    return "%02d:%02d".format(minutes / 60, minutes % 60)
}