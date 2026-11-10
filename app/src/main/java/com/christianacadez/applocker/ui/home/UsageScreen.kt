package com.christianacadez.applocker.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.christianacadez.applocker.data.TrackedApps
import com.christianacadez.applocker.data.UsageReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun UsageScreen() {
    val context = LocalContext.current
    val reader = remember { UsageReader(context) }
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

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
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name)
                Text(formatDuration(usage[pkg] ?: 0L))
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    return "${minutes / 60}h ${minutes % 60}m"
}