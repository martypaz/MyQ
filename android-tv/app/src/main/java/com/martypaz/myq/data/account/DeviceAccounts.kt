package com.martypaz.myq.data.account

import android.accounts.AccountManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * What MyQ can learn about who is signed in to this television, without
 * asking them to sign in again.
 *
 * Reading the device's Google account is the cheap path — no OAuth, no
 * dependency, no second sign-in on a remote control — but it is not reliably
 * available. Since Android 8 an app only sees accounts whose authenticator has
 * made them visible to it, so [GET_ACCOUNTS] being granted is necessary and
 * not sufficient: the permission can be held and the list still come back
 * empty. That is the case a Gmail sign-in would exist to cover, so it has to
 * be distinguishable from an outright refusal.
 */
sealed interface DeviceAccountResult {

    /** The account the television is signed in with. */
    data class Found(val email: String) : DeviceAccountResult

    /** Permission held, but the platform showed us nothing. */
    data object Hidden : DeviceAccountResult

    /** Permission not granted. */
    data object Denied : DeviceAccountResult

    /** No Google account on the device at all to find. */
    data object None : DeviceAccountResult

    val summary: String
        get() = when (this) {
            is Found -> "Signed in as $email — no separate Gmail login needed."
            Hidden -> "Permission granted, but Android is not showing MyQ any account. " +
                "This is the case a Gmail sign-in would have to cover."
            Denied -> "Permission to read device accounts was refused."
            None -> "No Google account is set up on this television."
        }
}

/**
 * Looks for the device's Google account. Never throws: every failure is a
 * result the caller can act on.
 */
fun readDeviceGoogleAccount(context: Context): DeviceAccountResult {
    val granted = ContextCompat.checkSelfPermission(context, GET_ACCOUNTS) ==
        PackageManager.PERMISSION_GRANTED
    if (!granted) return DeviceAccountResult.Denied

    val accounts = runCatching {
        AccountManager.get(context).getAccountsByType(GOOGLE_ACCOUNT_TYPE)
    }.getOrDefault(emptyArray())

    val email = accounts.firstOrNull()?.name
    return when {
        !email.isNullOrBlank() -> DeviceAccountResult.Found(email)
        // Granted but empty is the interesting case: either nothing is signed
        // in, or the account exists and is simply invisible to us. The
        // platform does not distinguish them, and neither can we honestly.
        else -> DeviceAccountResult.Hidden
    }
}

const val GET_ACCOUNTS = "android.permission.GET_ACCOUNTS"
private const val GOOGLE_ACCOUNT_TYPE = "com.google"
