package com.christianacadez.applocker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.christianacadez.applocker.data.PinStore
import com.christianacadez.applocker.data.UsagePermission
import com.christianacadez.applocker.ui.AppRoot
import com.christianacadez.applocker.ui.pin.PinGateScreen
import com.christianacadez.applocker.ui.pin.PinSetupScreen
import com.christianacadez.applocker.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private var unlocked by mutableStateOf(false)
    private var stoppedAt = 0L

    override fun onStart() {
        super.onStart()
        // Si saliste hace más de 30 s, vuelve a pedir el PIN
        if (unlocked && SystemClock.elapsedRealtime() - stoppedAt > GRACE_MS) unlocked = false
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    val context = LocalContext.current

                    // Permiso de notificaciones (Android 13+)
                    val notificationLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Permiso de acceso a uso: se revisa de nuevo al volver de Ajustes
                    var granted by remember { mutableStateOf(UsagePermission.isGranted(context)) }
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                granted = UsagePermission.isGranted(context)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    val pinStore = remember { PinStore(context) }
                    var hasPin by remember { mutableStateOf(pinStore.hasPin()) }

                    when {
                        !granted -> Column(
                            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Necesito acceso a las estadísticas de uso")
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { UsagePermission.openSettings(context) }) {
                                Text("Conceder permiso")
                            }
                        }

                        !hasPin -> PinSetupScreen(onPinSet = { pin ->
                            pinStore.setPin(pin)
                            hasPin = true
                            unlocked = true
                        })

                        !unlocked -> PinGateScreen(
                            verify = pinStore::verify,
                            onSuccess = { unlocked = true }
                        )

                        else -> AppRoot()
                    }
                }
            }
        }
    }

    private companion object {
        const val GRACE_MS = 30_000L
    }
}