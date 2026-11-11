package com.christianacadez.applocker.ui.blocker

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.christianacadez.applocker.data.SafetyConfig
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun LockScreen(
    emergenciesLeft: Int,
    onGranted: () -> Unit
) {
    var remaining by remember { mutableStateOf(timeUntilReset()) }
    var secondsLeft by remember { mutableStateOf<Int?>(null) }

    // Reloj hasta el reinicio
    LaunchedEffect(Unit) {
        while (true) {
            remaining = timeUntilReset()
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

    Box(Modifier.fillMaxSize().padding(24.dp)) {
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Se reinicia en",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(remaining, fontSize = 44.sp, fontWeight = FontWeight.Light)
        }

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Bloqueado",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(32.dp))

            val waiting = secondsLeft
            if (waiting != null) {
                Text("Espera $waiting s", fontSize = 20.sp, fontWeight = FontWeight.Light)
                TextButton(onClick = { secondsLeft = null }) { Text("Cancelar") }
            } else {
                OutlinedButton(
                    onClick = { secondsLeft = SafetyConfig.WAIT_SECONDS },
                    enabled = emergenciesLeft > 0
                ) {
                    Text(if (emergenciesLeft > 0) "Emergencia" else "Sin emergencias hoy")
                }
                if (emergenciesLeft > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Quedan $emergenciesLeft hoy · +${SafetyConfig.EXTRA_MINUTES} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/** Tiempo hasta la medianoche, en formato HH:MM. */
private fun timeUntilReset(): String {
    val zone = ZoneId.systemDefault()
    val now = ZonedDateTime.now(zone)
    val next = now.toLocalDate().plusDays(1).atStartOfDay(zone)
    val seconds = Duration.between(now, next).seconds
    val minutes = (seconds + 59) / 60
    return "%02d:%02d".format(minutes / 60, minutes % 60)
}