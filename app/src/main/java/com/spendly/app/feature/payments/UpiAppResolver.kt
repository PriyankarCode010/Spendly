package com.spendly.app.feature.payments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

data class UpiApp(val packageName: String, val label: String)

private const val TAG = "UpiAppResolver"

fun resolveInstalledUpiApps(context: Context): List<UpiApp> {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
    val packageManager = context.packageManager

    @Suppress("DEPRECATION")
    val resolveInfos = runCatching { packageManager.queryIntentActivities(intent, 0) }
        .onFailure { Log.e(TAG, "queryIntentActivities threw", it) }
        .getOrDefault(emptyList())

    Log.d(TAG, "queryIntentActivities(upi://pay) returned ${resolveInfos.size} result(s): " +
        resolveInfos.joinToString { it.activityInfo?.packageName ?: "unknown" })

    return resolveInfos.mapNotNull { info ->
        val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
        UpiApp(packageName = packageName, label = info.loadLabel(packageManager).toString())
    }.distinctBy { it.packageName }
}
