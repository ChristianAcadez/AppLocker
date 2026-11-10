package com.christianacadez.applocker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.christianacadez.applocker.data.TrackedApps
import com.christianacadez.applocker.data.UsageReader
import com.christianacadez.applocker.data.db.AppDatabase
import com.christianacadez.applocker.data.db.AppLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UsageScreen() {
    val context = LocalContext.current
    val reader = remember { UsageReader(context) }
    val dao = remember { AppDatabase.get(context).appLimitDao() }
    val scope = rememberCoroutineScope()

    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    val limits by remember { dao.observeAll() }.collectAsState(initial = emptyList())
    val limitByPackage = limits.associate { it.packageName to it.minutes }
    var editingPackage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            usage = withContext(Dispatchers.IO) {
                reader.usageSince(UsageReader.startOfToday(), TrackedApps.all.keys)
            }
            delay(5_000)
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        items(TrackedApps.all.entries.toList()) { (pkg, name) ->
            val usedMs = usage[pkg] ?: 0L
            val limitMin = limitByPackage[pkg]

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { editingPackage = pkg }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(name)
                    Text(
                        if (limitMin == null) "Sin límite"
                        else "Límite: ${formatDuration(limitMin * 60_000L)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatDuration(usedMs))
                    if (limitMin != null) {
                        val remaining = limitMin * 60_000L - usedMs
                        Text(
                            if (remaining > 0) "Quedan ${formatDuration(remaining)}"
                            else "Límite alcanzado",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    editingPackage?.let { pkg ->
        LimitDialog(
            appName = TrackedApps.all[pkg] ?: pkg,
            currentMinutes = limitByPackage[pkg],
            onSave = { minutes ->
                scope.launch { dao.upsert(AppLimit(pkg, minutes)) }
                editingPackage = null
            },
            onRemove = {
                scope.launch { dao.delete(pkg) }
                editingPackage = null
            },
            onDismiss = { editingPackage = null }
        )
    }
}

@Composable
private fun LimitDialog(
    appName: String,
    currentMinutes: Int?,
    onSave: (Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentMinutes?.toString() ?: "") }
    val minutes = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Límite diario: $appName") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                label = { Text("Minutos por día") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { minutes?.let(onSave) },
                enabled = minutes != null && minutes > 0
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (currentMinutes != null) {
                    TextButton(onClick = onRemove) { Text("Quitar") }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    return "${minutes / 60}h ${minutes % 60}m"
}