package com.christianacadez.applocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.christianacadez.applocker.data.TrackedApps
import com.christianacadez.applocker.data.UsageReader
import com.christianacadez.applocker.data.db.AppDatabase
import com.christianacadez.applocker.data.db.MemberKind
import com.christianacadez.applocker.data.db.UnlockEventDao
import com.christianacadez.applocker.domain.LimitEngine
import com.christianacadez.applocker.domain.Rule
import com.christianacadez.applocker.domain.Window
import com.christianacadez.applocker.ui.blocker.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

class MonitorAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var reader: UsageReader
    private lateinit var unlockDao: UnlockEventDao

    @Volatile private var groupOf: Map<String, Long> = emptyMap()       // paquete -> grupo
    @Volatile private var rulesOf: Map<Long, List<Rule>> = emptyMap()   // grupo -> franjas
    private var lastPackage: String? = null

    // Avisos ya enviados: "paquete|inicio de franja|límite"
    private val warned = ConcurrentHashMap.newKeySet<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        reader = UsageReader(this)
        Notifier.ensureChannel(this)
        val db = AppDatabase.get(this)
        unlockDao = db.unlockEventDao()
        val groupDao = db.groupDao()

        scope.launch {
            combine(groupDao.observeMembers(), groupDao.observeRules()) { members, rules ->
                members to rules
            }.collect { (members, rules) ->
                groupOf = members
                    .filter { it.kind == MemberKind.APP }
                    .associate { it.target to it.groupId }
                rulesOf = rules
                    .groupBy { it.groupId }
                    .mapValues { (_, list) ->
                        list.map { Rule(it.startMinute, it.endMinute, it.limitMinutes) }
                    }
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

        if (pkg in groupOf) scope.launch { checkLimits() }
    }

    private suspend fun checkLimits() {
        val groups = groupOf
        if (groups.isEmpty()) return
        val rules = rulesOf
        val nowMs = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()

        // Franja en curso de cada app vigilada (las que no tienen franja ahora quedan libres)
        val windows = HashMap<String, Window>()
        for ((pkg, groupId) in groups) {
            val groupRules = rules[groupId] ?: continue
            LimitEngine.activeWindow(groupRules, nowMs, zone)?.let { windows[pkg] = it }
        }
        if (windows.isEmpty()) return

        // Una consulta de uso por cada inicio de franja distinto
        for ((startMs, entries) in windows.entries.groupBy { it.value.startMs }) {
            val packages = entries.map { it.key }.toSet()
            val snapshot = reader.snapshot(startMs, packages)
            val extras = unlockDao.since(startMs)
                .groupBy { it.packageName }
                .mapValues { (_, events) -> events.sumOf { it.extraMinutes } }

            for (pkg in snapshot.foreground) {
                val window = windows[pkg] ?: continue
                val extra = extras[pkg] ?: 0
                val remainingMs = LimitEngine.remainingMs(window, snapshot.totals[pkg] ?: 0L, extra)
                when {
                    remainingMs <= 0 -> {
                        showLock(pkg, window.endMs)
                        return
                    }
                    remainingMs <= WARNING_MS ->
                        warnOnce(pkg, window.startMs, window.limitMinutes + extra, remainingMs)
                }
            }
        }
    }

    private fun warnOnce(pkg: String, windowStartMs: Long, limitMin: Int, remainingMs: Long) {
        val key = "$pkg|$windowStartMs|$limitMin"
        if (!warned.add(key)) return

        val minutes = ((remainingMs + 59_999) / 60_000).toInt()
        Notifier.warn(this, pkg, TrackedApps.all[pkg] ?: pkg, minutes)
    }

    private fun showLock(pkg: String, resetAtMs: Long) {
        startActivity(
            Intent(this, LockActivity::class.java)
                .putExtra(LockActivity.EXTRA_PACKAGE, pkg)
                .putExtra(LockActivity.EXTRA_RESET_AT, resetAtMs)
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