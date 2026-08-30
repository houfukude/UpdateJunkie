package com.houfukude.updatejunkie.model

import android.graphics.drawable.Drawable

/**
 * 一个已安装应用的完整信息模型。
 *
 * @property packageName 应用包名，如 `com.android.vending`
 * @property label 应用显示名称
 * @property icon 应用图标，加载失败时为 null
 * @property versionName 版本名，如 `1.2.3`，可能为空
 * @property versionCode 版本号（内部版本号），统一使用 Long 以兼容 Android P 及以上
 * @property installerPackageName 安装来源包名，如 `com.xiaomi.market`，未知时为 null
 * @property installerLabel 安装来源的友好名称，由 [com.houfukude.updatejunkie.utils.MarketUtils.getMarketLabel] 映射而来
 * @property isSystemApp 是否为系统应用（预装）
 * @property isEnabled 是否处于启用状态，false 表示已被用户禁用或冻结
 * @property userId 应用安装所在的用户 ID；多用户同时安装时以英文逗号分隔，如 `0,10`
 * @property isAdbInstalled 是否为 ADB / 命令行安装（无安装来源记录）
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val versionName: String?,
    val versionCode: Long,
    val installerPackageName: String?,
    val installerLabel: String?,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val userId: String,
    val isAdbInstalled: Boolean
)
