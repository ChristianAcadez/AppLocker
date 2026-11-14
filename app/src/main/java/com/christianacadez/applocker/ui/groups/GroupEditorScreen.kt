package com.christianacadez.applocker.ui.groups

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.christianacadez.applocker.data.InstalledApp
import com.christianacadez.applocker.data.db.GroupMember
import com.christianacadez.applocker.data.db.GroupRule
import com.christianacadez.applocker.data.db.MemberKind
import com.christianacadez.applocker.data.repository.GroupDetails
import com.christianacadez.applocker.domain.LimitEngine
import com.christianacadez.applocker.domain.Rule

private data class RuleDraft(
    val startMinute: Int,
    val endMinute: Int,
    val limitText: String
)

@Composable
fun GroupEditorScreen(
    existing: GroupDetails?,
    allGroups: List<GroupDetails>,
    installed: List<InstalledApp>,
    onSave: (name: String, members: List<GroupMember>, rules: List<GroupRule>) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val id = existing?.group?.id ?: 0L

    var name by remember(id) { mutableStateOf(existing?.group?.name ?: "") }
    var selected by remember(id) {
        mutableStateOf(existing?.members?.map { it.target }?.toSet() ?: emptySet())
    }
    val rules = remember(id) {
        mutableStateListOf<RuleDraft>().apply {
            val stored = existing?.rules.orEmpty()
            if (stored.isEmpty()) {
                add(RuleDraft(0, 0, "60"))   // todo el día
            } else {
                stored.forEach {
                    add(RuleDraft(it.startMinute, it.endMinute % 1440, it.limitMinutes.toString()))
                }
            }
        }
    }
    var picking by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }  // (franja, true = inicio)
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler { if (showPicker) showPicker = false else onClose() }

    val labels = remember(installed) { installed.associate { it.packageName to it.label } }
    val takenByOther = remember(allGroups, id) {
        allGroups
            .filter { it.group.id != id }
            .flatMap { details -> details.members.map { it.target to details.group.name } }
            .toMap()
    }

    if (showPicker) {
        AppPickerScreen(
            installed = installed,
            selected = selected,
            takenByOther = takenByOther,
            onToggle = { pkg -> selected = if (pkg in selected) selected - pkg else selected + pkg },
            onDone = { showPicker = false }
        )
        return
    }

    val parsed = rules.map { Rule(it.startMinute, it.endMinute, it.limitText.toIntOrNull() ?: 0) }
    val overlap = parsed.indices.any { i ->
        (i + 1 until parsed.size).any { j -> LimitEngine.overlaps(parsed[i], parsed[j]) }
    }
    val error = when {
        name.isBlank() -> "Ponle un nombre al grupo"
        selected.isEmpty() -> "Elige al menos una app"
        rules.isEmpty() -> "Agrega al menos una franja"
        parsed.any { it.limitMinutes <= 0 } -> "Los minutos deben ser mayores a 0"
        overlap -> "Las franjas no pueden solaparse"
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
        Text(
            if (existing == null) "Nuevo grupo" else "Editar grupo",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Text("Apps", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (selected.isEmpty()) {
            Text("Ninguna todavía", color = muted)
        } else {
            selected
                .sortedBy { (labels[it] ?: it).lowercase() }
                .forEach { Text(labels[it] ?: it, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showPicker = true }) { Text("Elegir apps") }

        Spacer(Modifier.height(24.dp))
        Text("Franjas", style = MaterialTheme.typography.titleMedium)
        Text(
            "El tiempo solo cuenta dentro de las franjas; fuera de ellas no hay límite. " +
                    "Cada app tiene su propio tiempo.",
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )

        rules.forEachIndexed { index, draft ->
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { picking = index to true }) {
                    Text(formatMinuteOfDay(draft.startMinute))
                }
                Text("  –  ")
                OutlinedButton(onClick = { picking = index to false }) {
                    Text(formatMinuteOfDay(draft.endMinute))
                }
            }
            Text(
                describeRange(draft.startMinute, draft.endMinute),
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.limitText,
                onValueChange = { text ->
                    rules[index] = draft.copy(limitText = text.filter(Char::isDigit))
                },
                label = { Text("Minutos por app") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                TextButton(onClick = { rules[index] = draft.copy(startMinute = 0, endMinute = 0) }) {
                    Text("Todo el día")
                }
                if (rules.size > 1) {
                    TextButton(onClick = { rules.removeAt(index) }) { Text("Quitar franja") }
                }
            }
            HorizontalDivider()
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { rules.add(RuleDraft(6 * 60, 11 * 60, "15")) }) {
            Text("Añadir franja")
        }

        Spacer(Modifier.height(24.dp))
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = muted)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                onSave(
                    name.trim(),
                    selected.map { GroupMember(target = it, kind = MemberKind.APP, groupId = 0) },
                    rules.map {
                        GroupRule(
                            groupId = 0,
                            startMinute = it.startMinute,
                            endMinute = it.endMinute,
                            limitMinutes = it.limitText.toInt()
                        )
                    }
                )
            },
            enabled = error == null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar") }

        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
        if (existing != null) {
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Eliminar grupo")
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    picking?.let { (index, isStart) ->
        rules.getOrNull(index)?.let { draft ->
            TimeDialog(
                initialMinute = if (isStart) draft.startMinute else draft.endMinute,
                onConfirm = { minute ->
                    rules[index] = if (isStart) {
                        draft.copy(startMinute = minute)
                    } else {
                        draft.copy(endMinute = minute)
                    }
                    picking = null
                },
                onDismiss = { picking = null }
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar el grupo?") },
            text = { Text("Sus apps quedarán sin límite.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    initialMinute: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = { TimePicker(state = state) }
    )
}