package com.christianacadez.applocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.christianacadez.applocker.data.TrackedApps
import com.christianacadez.applocker.data.UsageReader
import com.christianacadez.applocker.data.db.AppDatabase
import com.christianacadez.applocker.data.db.UnlockEventDao
import com.christianacadez.applocker.ui.blocker.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

class MonitorAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var reader: UsageReader

    private lateinit var unlockDao: UnlockEventDao

    @Volatile private var limits: Map<String, Int> = emptyMap()
    private var lastPackage: String? = null

    // Avisos ya enviados: "paquete|fecha|límite"
    private val warned = ConcurrentHashMap.newKeySet<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        reader = UsageReader(this)
        Notifier.ensureChannel(this)
        val dao = AppDatabase.get(this).appLimitDao()
        unlockDao = AppDatabase.get(this).unlockEventDao()

        scope.launch {
            dao.observeAll().collect { list ->
                limits = list.associate { it.packageName to it.minutes }
            }
        }
        scope.launch {
            while (isActive) {
                checkLimits()
                delay(5_000)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == lastPackage) return
        lastPackage = pkg

        if (pkg in limits) scope.launch { checkLimits() }
    }

    private suspend fun checkLimits() {
        val current = limits
        if (current.isEmpty()) return

        val start = UsageReader.startOfToday()
        val extras = unlockDao.since(start)
            .groupBy { it.packageName }
            .mapValues { (_, events) -> events.sumOf { it.extraMinutes } }

        val snapshot = reader.snapshot(start, current.keys)
        for (pkg in snapshot.foreground) {
            val limitMin = (current[pkg] ?: continue) + (extras[pkg] ?: 0)
            val remainingMs = limitMin * 60_000L - (snapshot.totals[pkg] ?: 0L)
            when {
                remainingMs <= 0 -> {
                    showLock(pkg)
                    return
                }
                remainingMs <= WARNING_MS -> warnOnce(pkg, limitMin, remainingMs)
            }
        }
    }

    private fun warnOnce(pkg: String, limitMin: Int, remainingMs: Long) {
        val key = "$pkg|${LocalDate.now()}|$limitMin"
        if (!warned.add(key)) return

        val minutes = ((remainingMs + 59_999) / 60_000).toInt()
        Notifier.warn(this, pkg, TrackedApps.all[pkg] ?: pkg, minutes)
    }

    private fun showLock(pkg: String) {
        startActivity(
            Intent(this, LockActivity::class.java)
                .putExtra(LockActivity.EXTRA_PACKAGE, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val WARNING_MS = 5 * 60_000L
    }
}