package com.christianacadez.applocker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.christianacadez.applocker.data.AccessibilityStatus
import com.christianacadez.applocker.data.InstalledApp
import com.christianacadez.applocker.data.InstalledApps
import com.christianacadez.applocker.data.db.AppDatabase
import com.christianacadez.applocker.data.repository.GroupRepository
import com.christianacadez.applocker.ui.groups.GroupEditorScreen
import com.christianacadez.applocker.ui.groups.GroupsScreen
import com.christianacadez.applocker.ui.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

sealed interface Screen {
    data object Groups : Screen
    data class Editor(val groupId: Long) : Screen   // 0 = grupo nuevo
    data object Settings : Screen
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { GroupRepository(AppDatabase.get(context).groupDao()) }

    val groups by remember { repo.observeAll() }.collectAsState(initial = emptyList())
    val installed by produceState(initialValue = emptyList<InstalledApp>()) {
        value = withContext(Dispatchers.IO) { InstalledApps.load(context) }
    }
    val labels = remember(installed) { installed.associate { it.packageName to it.label } }

    var serviceEnabled by remember { mutableStateOf(AccessibilityStatus.isEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceEnabled = AccessibilityStatus.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var screen by remember { mutableStateOf<Screen>(Screen.Groups) }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "screen"
        ) { current ->
            when (current) {
                Screen.Groups -> GroupsScreen(
                    groups = groups,
                    labels = labels,
                    serviceEnabled = serviceEnabled,
                    onEnableService = { AccessibilityStatus.openSettings(context) },
                    onAdd = { screen = Screen.Editor(0) },
                    onOpen = { screen = Screen.Editor(it) },
                    onSettings = { screen = Screen.Settings }
                )

                Screen.Settings -> SettingsScreen(
                    serviceEnabled = serviceEnabled,
                    onOpenAccessibility = { AccessibilityStatus.openSettings(context) },
                    onBack = { screen = Screen.Groups }
                )

                is Screen.Editor -> GroupEditorScreen(
                    existing = groups.find { it.group.id == current.groupId },
                    allGroups = groups,
                    installed = installed,
                    onSave = { name, members, rules ->
                        scope.launch {
                            repo.save(current.groupId, name, members, rules)
                            screen = Screen.Groups
                        }
                    },
                    onDelete = {
                        scope.launch {
                            repo.delete(current.groupId)
                            screen = Screen.Groups
                        }
                    },
                    onClose = { screen = Screen.Groups }
                )
            }
        }
    }
}