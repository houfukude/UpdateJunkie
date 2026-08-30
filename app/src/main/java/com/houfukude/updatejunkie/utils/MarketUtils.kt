package com.houfukude.updatejunkie.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.ColorRes
import com.houfukude.updatejunkie.R

object MarketUtils {
    const val SYSTEM_APP_INSTALLER = "system_app"

    private val MARKET_MAP = mapOf(
        "com.android.vending" to "Google Play",
        SYSTEM_APP_INSTALLER to "系统应用",
        "com.google.android.packageinstaller" to "Package Installer",
        "com.coolapk.market" to "Coolapk",
        "com.xiaomi.market" to "Xiaomi Market",
        "com.huawei.appmarket" to "Huawei AppGallery",
        "com.oppo.market" to "OPPO App Market",
        "com.bbk.appstore" to "vivo App Store",
        "com.tencent.android.qqdownloader" to "MyApp (Tencent)",
        "com.baidu.appsearch" to "Baidu Mobile Assistant",
        "com.wandoujia.phoenix2" to "Wandoujia",
        "com.dragon.android.pandaspace" to "91 Assistant",
        "com.android.chrome" to "Google Chrome",
        "com.UCMobile" to "UC Browser",
        "com.tencent.mtt" to "QQ Browser",
        "com.quark.browser" to "Quark Browser",
        "org.mozilla.firefox" to "Firefox",
        "com.microsoft.emmx" to "Microsoft Edge"
    )

    fun getMarketLabel(installerPackageName: String?): String {
        if (installerPackageName == null) return "Unknown"
        return MARKET_MAP[installerPackageName] ?: installerPackageName
    }

    @ColorRes
    fun getMarketColor(installerPackageName: String?): Int? {
        return when (installerPackageName) {
            "com.android.vending",
            SYSTEM_APP_INSTALLER -> null
            "com.google.android.packageinstaller" -> R.color.item_market_other_bg

            "com.coolapk.market",
            "com.xiaomi.market",
            "com.huawei.appmarket",
            "com.oppo.market",
            "com.bbk.appstore",
            "com.tencent.android.qqdownloader",
            "com.baidu.appsearch" -> R.color.item_market_known_bg

            else -> R.color.item_market_unknown_bg
        }
    }

    fun getMarketIntent(packageName: String, installerPackageName: String?): Intent {
        return when (installerPackageName) {
            "com.coolapk.market" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("coolmarket://apk/$packageName"))
            }

            "com.xiaomi.market" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("mimarket://details?id=$packageName"))
            }

            "com.huawei.appmarket" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("appmarket://details?id=$packageName"))
            }

            else -> {
                // Default market intent
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            }
        }
    }

    fun launchMarket(context: Context, packageName: String, installerPackageName: String?) {
        try {
            val intent = getMarketIntent(packageName, installerPackageName).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser or show error
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            } catch (ignored: Exception) {
            }
        }
    }
}
