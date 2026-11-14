package com.christianacadez.applocker.ui.blocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.christianacadez.applocker.data.UsageReader
import com.christianacadez.applocker.data.db.AppDatabase
import com.christianacadez.applocker.data.db.UnlockEvent
import com.christianacadez.applocker.ui.theme.AppTheme
import kotlinx.coroutines.launch
import com.christianacadez.applocker.data.PinStore
import com.christianacadez.applocker.data.SafetySettings

class LockActivity : ComponentActivity() {

    private var blockedPackage: String? = null
    private var appName by mutableStateOf("")

    private val pinStore by lazy { PinStore(this) }
    private var resetAtMs by mutableLongStateOf(0L)
    private var launchId by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE)
        appName = labelOf(blockedPackage)
        resetAtMs = intent.getLongExtra(EXTRA_RESET_AT, 0L)
        val dao = AppDatabase.get(this).unlockEventDao()


        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    val scope = rememberCoroutineScope()
                    var emergenciesLeft by remember { mutableIntStateOf(0) }
                    val settings = remember { SafetySettings(this@LockActivity) }
                    var values by remember { mutableStateOf(settings.load()) }


                    // Recalcula las emergencias disponibles cada vez que se muestra el candado
                    LaunchedEffect(launchId) {
                        values = settings.load()
                        val used = dao.since(UsageReader.startOfToday()).size
                        emergenciesLeft = (values.maxUnlocksPerDay - used).coerceAtLeast(0)
                    }

                    BackHandler { goHome() }

                    LockScreen(
                        emergenciesLeft = emergenciesLeft,
                        waitSeconds = values.waitSeconds,
                        extraMinutes = values.extraMinutes,
                        resetAtMs = resetAtMs,
                        requiresPin = pinStore.hasPin(),
                        verifyPin = pinStore::verify,
                        appName = appName,
                        onGranted = {
                            val pkg = blockedPackage
                            if (pkg == null) {
                                goHome()
                            } else {
                                scope.launch {
                                    dao.insert(
                                        UnlockEvent(
                                            packageName = pkg,
                                            timestamp = System.currentTimeMillis(),
                                            extraMinutes = values.extraMinutes
                                        )
                                    )
                                    finish() // revela la app que estaba debajo
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE)
        appName = labelOf(blockedPackage)
        resetAtMs = intent.getLongExtra(EXTRA_RESET_AT, 0L)
        launchId++
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "blocked_package"
        const val EXTRA_RESET_AT = "reset_at"
    }

    @Suppress("DEPRECATION")
    private fun labelOf(pkg: String?): String =
        pkg?.let {
            runCatching {
                packageManager.getApplicationInfo(it, 0).loadLabel(packageManager).toString()
            }.getOrNull()
        } ?: ""
}