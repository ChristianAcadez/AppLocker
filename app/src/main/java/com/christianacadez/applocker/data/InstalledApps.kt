package com.christianacadez.applocker.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri

data class InstalledApp(
    val packageName: String,
    val label: String,
    val suggested: Boolean   // red social, navegador o juego
)

object InstalledApps {

    /** Apps conocidas que a veces no declaran su categoría. Amplíala cuando quieras. */
    private val KNOWN = setOf(
        "com.facebook.katana",
        "com.facebook.lite",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.pinterest",
        "com.android.chrome",
        "com.brave.browser"
    )

    @Suppress("DEPRECATION")
    fun load(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val browsers = browserPackages(pm)

        return pm.queryIntentActivities(launcherIntent, 0)
            .map { info ->
                val pkg = info.activityInfo.packageName
                val app = info.activityInfo.applicationInfo
                val isSocial = app.category == ApplicationInfo.CATEGORY_SOCIAL
                val isGame = app.category == ApplicationInfo.CATEGORY_GAME ||
                        (app.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                InstalledApp(
                    packageName = pkg,
                    label = info.loadLabel(pm).toString(),
                    suggested = isSocial || isGame || pkg in browsers || pkg in KNOWN
                )
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** Apps capaces de abrir una página web cualquiera = navegadores. */
    @Suppress("DEPRECATION")
    private fun browserPackages(pm: PackageManager): Set<String> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .toSet()
    }
}