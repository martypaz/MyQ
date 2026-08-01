package com.martypaz.myq.data.streaming

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Hands a programme off to the service that carries it, taking the deepest
 * route the device actually supports:
 *
 * 1. the installed app, opened on a search for the title;
 * 2. the installed app's launcher screen, if it does not claim its own links;
 * 3. the same search URL in a browser, on devices that have one;
 * 4. the app's store page, so an interested viewer can install it.
 *
 * Returns false only when every route failed, which the caller should treat as
 * "tell the viewer", not "fail silently".
 */
fun Context.openInStreamingApp(app: StreamingApp, title: String): Boolean {
    val installed = app.packages.firstOrNull { packageManager.isInstalled(it) }
    val search = Uri.parse(app.searchUrl(title))
    val store = app.storePackage

    val routes = buildList {
        if (installed != null) {
            add(Intent(Intent.ACTION_VIEW, search).setPackage(installed))
            packageManager.launchIntentFor(installed)?.let(::add)
        } else {
            add(Intent(Intent.ACTION_VIEW, search))
            add(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$store")))
            add(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$store"),
                ),
            )
        }
    }

    return routes.any { start(it) }
}

private fun Context.start(intent: Intent): Boolean =
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

private fun PackageManager.isInstalled(packageName: String): Boolean =
    runCatching { getPackageInfo(packageName, 0) }.isSuccess

/** TV apps expose a leanback entry point; fall back for phone-only builds. */
private fun PackageManager.launchIntentFor(packageName: String): Intent? =
    getLeanbackLaunchIntentForPackage(packageName) ?: getLaunchIntentForPackage(packageName)
