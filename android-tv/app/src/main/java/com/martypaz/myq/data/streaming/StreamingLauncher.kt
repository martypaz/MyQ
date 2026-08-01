package com.martypaz.myq.data.streaming

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Hands a programme off to the service that carries it.
 *
 * Finding the app cannot be done by package name alone. BBC iPlayer ships as
 * `bbc.iplayer.android`, as `com.nvidia.bbciplayer` on Shield, and under
 * further names on some manufacturers' televisions; the same is true of most
 * UK broadcasters. So the installed leanback launchers are enumerated and
 * matched on the name the user sees, with the package list only as a fast
 * path.
 *
 * Every route is resolved before it is started. An intent that nothing can
 * handle produces a system "you don't have an app that can do this" message
 * rather than a caught exception, so checking first is the only way to fail
 * quietly and move on to the next route.
 */
fun Context.openInStreamingApp(app: StreamingApp, title: String): Boolean {
    val installed = findInstalledPackage(app)
    val search = Uri.parse(app.searchUrl(title))

    val routes = buildList {
        if (installed != null) {
            // Deep link first: it lands on the programme rather than the app's
            // home screen. Not every app claims its own links, hence the next.
            add(Intent(Intent.ACTION_VIEW, search).setPackage(installed))
            packageManager.launchIntentFor(installed)?.let(::add)
        }
        add(Intent(Intent.ACTION_VIEW, search))
        add(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.storePackage}")))
        add(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${app.storePackage}"),
            ),
        )
    }

    return routes.any { start(it) }
}

/**
 * The installed package for [app], by declared package name or, failing that,
 * by the label the launcher shows for it.
 */
internal fun Context.findInstalledPackage(app: StreamingApp): String? =
    app.packages.firstOrNull { packageManager.isInstalled(it) }
        ?: leanbackLaunchers().firstOrNull { app.matchesLabel(it.label) }?.packageName

internal data class InstalledApp(val packageName: String, val label: String)

/** Every app with a television launcher entry, as the launcher lists them. */
internal fun Context.leanbackLaunchers(): List<InstalledApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    val leanback = packageManager.queryIntentActivities(intent, 0)
    // Sideloaded phone builds are common on television; include them too.
    val standard = packageManager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        0,
    )
    return (leanback + standard).mapNotNull { resolved ->
        val activity = resolved.activityInfo ?: return@mapNotNull null
        InstalledApp(
            packageName = activity.packageName,
            label = resolved.loadLabel(packageManager)?.toString().orEmpty(),
        )
    }.distinctBy { it.packageName }
}

private fun Context.start(intent: Intent): Boolean {
    val launchable = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (packageManager.resolveActivity(launchable, 0) == null) return false
    return runCatching { startActivity(launchable) }.isSuccess
}

private fun PackageManager.isInstalled(packageName: String): Boolean =
    runCatching { getPackageInfo(packageName, 0) }.isSuccess

/** TV apps expose a leanback entry point; fall back for phone-only builds. */
private fun PackageManager.launchIntentFor(packageName: String): Intent? =
    getLeanbackLaunchIntentForPackage(packageName) ?: getLaunchIntentForPackage(packageName)
