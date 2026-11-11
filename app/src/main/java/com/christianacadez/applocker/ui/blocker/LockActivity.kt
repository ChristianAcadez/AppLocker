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
import com.christianacadez.applocker.data.SafetyConfig
import com.christianacadez.applocker.data.UsageReader
import com.christianacadez.applocker.data.db.AppDatabase
import com.christianacadez.applocker.data.db.UnlockEvent
import com.christianacadez.applocker.ui.theme.AppTheme
import kotlinx.coroutines.launch

class LockActivity : ComponentActivity() {

    private var blockedPackage: String? = null
    private var launchId by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE)
        val dao = AppDatabase.get(this).unlockEventDao()

        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    val scope = rememberCoroutineScope()
                    var emergenciesLeft by remember { mutableIntStateOf(0) }

                    // Recalcula las emergencias disponibles cada vez que se muestra el candado
                    LaunchedEffect(launchId) {
                        val used = dao.since(UsageReader.startOfToday()).size
                        emergenciesLeft = (SafetyConfig.MAX_UNLOCKS_PER_DAY - used).coerceAtLeast(0)
                    }

                    BackHandler { goHome() }

                    LockScreen(
                        emergenciesLeft = emergenciesLeft,
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
                                            extraMinutes = SafetyConfig.EXTRA_MINUTES
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
    }
}