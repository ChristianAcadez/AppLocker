package com.christianacadez.applocker.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.christianacadez.applocker.data.InstalledApp

@Composable
fun AppPickerScreen(
    installed: List<InstalledApp>,
    selected: Set<String>,
    takenByOther: Map<String, String>,   // paquete -> nombre del grupo que ya la tiene
    onToggle: (String) -> Unit,
    onDone: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    // Las apps ya elegidas al abrir el selector siempre se muestran
    val initiallySelected = remember { selected }

    val visible = remember(query, showAll, installed) {
        installed.filter { app ->
            (showAll || app.suggested || app.packageName in initiallySelected) &&
                    app.label.contains(query, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Elegir apps", fontSize = 28.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mostrar todas las apps", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = showAll, onCheckedChange = { showAll = it })
        }

        LazyColumn(Modifier.weight(1f)) {
            items(visible, key = { it.packageName }) { app ->
                val otherGroup = takenByOther[app.packageName]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = otherGroup == null) { onToggle(app.packageName) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = app.packageName in selected,
                        onCheckedChange = null,
                        enabled = otherGroup == null
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(app.label)
                        if (otherGroup != null) {
                            Text(
                                "Ya está en «$otherGroup»",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Listo (${selected.size})")
        }
        Spacer(Modifier.height(16.dp))
    }
}