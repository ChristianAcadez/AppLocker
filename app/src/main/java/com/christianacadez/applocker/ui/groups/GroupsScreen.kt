package com.christianacadez.applocker.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.christianacadez.applocker.data.repository.GroupDetails

@Composable
fun GroupsScreen(
    groups: List<GroupDetails>,
    labels: Map<String, String>,
    serviceEnabled: Boolean,
    onEnableService: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit
) {
    val muted = MaterialTheme.colorScheme.outline

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grupos", fontSize = 28.sp, fontWeight = FontWeight.Light)
            TextButton(onClick = onSettings) { Text("Ajustes") }
        }

        if (!serviceEnabled) {
            Spacer(Modifier.height(8.dp))
            Text("El bloqueo está apagado", style = MaterialTheme.typography.titleSmall)
            Text(
                "Activa el servicio de accesibilidad para que los límites funcionen.",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onEnableService) { Text("Activar") }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
        }

        Spacer(Modifier.height(16.dp))
        if (groups.isEmpty()) {
            Text("Aún no tienes grupos. Crea uno para limitar tus apps.", color = muted)
        }

        LazyColumn(Modifier.weight(1f)) {
            items(groups, key = { it.group.id }) { details ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(details.group.id) }
                        .padding(vertical = 14.dp)
                ) {
                    Text(details.group.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        details.members.joinToString(" · ") { labels[it.target] ?: it.target },
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                    details.rules.forEach { rule ->
                        Text(describeRule(rule), style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
            }
        }

        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text("Nuevo grupo")
        }
        Spacer(Modifier.height(16.dp))
    }
}