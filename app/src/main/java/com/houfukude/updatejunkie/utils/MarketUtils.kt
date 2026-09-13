package com.houfukude.updatejunkie.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.ColorRes
import androidx.core.net.toUri
import com.houfukude.updatejunkie.R

/**
 * 应用市场 / 安装来源相关的工具类。
 *
 * 提供安装来源包名到友好名称、列表背景色、跳转 Intent 的映射。
 */
object MarketUtils {
    /** 系统预装应用使用的虚拟安装来源标识（并非真实包名）。 */
    const val SYSTEM_APP_INSTALLER = "system_app"

    /** 已知安装来源包名与其展示名称的映射表（仅包含英文名或内部 ID）。 */
    private val MARKET_MAP = mapOf(
        "com.android.vending" to "Google Play",
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

    /**
     * 将安装来源包名转换为用于展示的友好名称。
     *
     * @param context 用于获取本地化字符串的上下文
     * @param installerPackageName 安装来源包名，为 null 表示未知来源
     * @return 已知市场返回映射名称；未知来源返回 "Unknown"；未收录的包名原样返回
     */
    fun getMarketLabel(context: Context, installerPackageName: String?): String {
        if (installerPackageName == null) return context.getString(R.string.unknown)
        if (installerPackageName == SYSTEM_APP_INSTALLER) return context.getString(R.string.system_app)
        return MARKET_MAP[installerPackageName] ?: installerPackageName
    }

    /**
     * 根据安装来源获取应用列表项的底色资源。
     *
     * @param installerPackageName 安装来源包名
     * @return 颜色资源 ID；对 Google Play、系统应用等主流来源返回 null（使用默认背景）
     */
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

    /**
     * 构造跳转到对应应用市场详情页的 Intent。
     *
     * 对酷安、小米、华为使用其私有 Scheme 直达，其余来源统一使用标准的 `market://` 协议。
     *
     * @param packageName 目标应用的包名
     * @param installerPackageName 安装来源包名，决定跳转到哪个市场
     * @return 可用于 `startActivity` 的 [Intent]
     */
    fun getMarketIntent(packageName: String, installerPackageName: String?): Intent {
        return when (installerPackageName) {
            "com.coolapk.market" -> {
                Intent(Intent.ACTION_VIEW, "coolmarket://apk/$packageName".toUri())
            }

            "com.xiaomi.market" -> {
                Intent(Intent.ACTION_VIEW, "mimarket://details?id=$packageName".toUri())
            }

            "com.huawei.appmarket" -> {
                Intent(Intent.ACTION_VIEW, "appmarket://details?id=$packageName".toUri())
            }

            else -> {
                // Default market intent
                Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
            }
        }
    }

    /**
     * 启动应用市场详情页；若指定的市场应用无法处理（如未安装），则回退到系统通用的市场跳转协议。
     *
     * 整个过程静默失败：若系统中未安装任何应用市场，则不做处理。
     *
     * @param context 用于启动 Activity 的上下文
     * @param packageName 目标应用的包名
     * @param installerPackageName 安装来源包名
     */
    fun launchMarket(context: Context, packageName: String, installerPackageName: String?) {
        try {
            val intent = getMarketIntent(packageName, installerPackageName).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MarketUtils", "Failed to launch market", e)
            // Fallback to universal market URI instead of hardcoded web link
            try {
                val fallbackIntent = Intent(
                    Intent.ACTION_VIEW,
                    "market://details?id=$packageName".toUri()
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
            }
        }
    }
}
