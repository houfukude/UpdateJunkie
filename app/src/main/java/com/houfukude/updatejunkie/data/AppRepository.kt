package com.houfukude.updatejunkie.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.houfukude.updatejunkie.model.AppInfo
import com.houfukude.updatejunkie.shizuku.ShizukuManager
import com.houfukude.updatejunkie.utils.MarketUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * 应用列表加载过程中的流式结果，用于向 UI 上报进度。
 *
 * @see AppLoadResult.Total 首个发射项，携带待加载的应用总数
 * @see AppLoadResult.App 每解析完一个应用发射一次
 */
sealed class AppLoadResult {
    /**
     * 加载开始时发射，携带符合条件的应用总数。
     *
     * @property count 需要加载的应用总数量
     */
    data class Total(val count: Int) : AppLoadResult()

    /**
     * 每解析完一个应用后发射。
     *
     * @property app 已解析完成的应用信息
     */
    data class App(val app: AppInfo) : AppLoadResult()
}

/**
 * 应用数据源仓库，负责从系统 PackageManager 及 Shizuku 中采集应用信息。
 *
 * 所有耗时操作均在 [Dispatchers.IO] 上执行。
 *
 * @property context 应用上下文，用于获取 PackageManager
 */
class AppRepository(private val context: Context) {

    /**
     * 以流式方式获取设备上已安装的应用列表。
     *
     * 会先发射 [AppLoadResult.Total] 告知总数，随后逐个发射 [AppLoadResult.App]，
     * 便于 UI 实时展示加载进度。
     *
     * 安装来源与用户信息的获取策略：
     * - 系统应用：直接使用固定标识与 UID 推算的用户 ID，不执行 dumpsys；
     * - 普通应用：优先通过 Shizuku 执行一次 `dumpsys package` 同时解析出安装来源与用户列表，
     *   若 Shizuku 不可用则回退到 PackageManager。
     *
     * @param includeSystemApps 是否包含系统应用，默认 true（列表过滤由上层负责）
     * @return 发射 [AppLoadResult] 的冷流，运行在 [Dispatchers.IO] 上
     */
    fun getInstalledAppsFlow(includeSystemApps: Boolean = true): Flow<AppLoadResult> = flow {
        val pm = context.packageManager
        val flags = PackageManager.GET_META_DATA or
                PackageManager.MATCH_DISABLED_COMPONENTS or
                PackageManager.MATCH_UNINSTALLED_PACKAGES

        val allPackages = pm.getInstalledPackages(flags)
        val filteredPackages = allPackages.filter { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@filter false
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystemApps && isSystemApp) false else true
        }

        // 确保 Shizuku 服务已尝试绑定
        android.util.Log.d("AppRepository", "Starting installed apps flow, binding Shizuku...")
        ShizukuManager.bindService(context)

        // 发送总数
        emit(AppLoadResult.Total(filteredPackages.size))

        for (packageInfo in filteredPackages) {
            val appInfo = packageInfo.applicationInfo!!
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val packageName = packageInfo.packageName
            val label = appInfo.loadLabel(pm).toString()
            val icon = appInfo.loadIcon(pm)
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            val isEnabled = appInfo.enabled

            // 只执行一次 dumpsys，同时拿到安装来源 PackageName 和 userId
            val fallbackUserId = appInfo.uid / 100000
            val installInfo = if (isSystemApp) null else ShizukuManager.getInstallInfo(packageName)

            val userId = if (isSystemApp) {
                fallbackUserId.toString()
            } else {
                // 可能有多个用户同时安装了该包，用逗号分隔展示
                installInfo?.users
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(",")
                    ?: fallbackUserId.toString()
            }

            val installerPackageName = if (isSystemApp) {
                MarketUtils.SYSTEM_APP_INSTALLER
            } else {
                installInfo?.installerPackageName
                    ?: getInstallerPackageName(pm, packageName)
            }
            
            val installerLabel = MarketUtils.getMarketLabel(context, installerPackageName)
            val isAdbInstalled = !isSystemApp && (installerPackageName == null || installerPackageName == "com.android.shell")

            emit(
                AppLoadResult.App(
                    AppInfo(
                        packageName = packageName,
                        label = label,
                        icon = icon,
                        versionName = versionName,
                        versionCode = versionCode,
                        installerPackageName = installerPackageName,
                        installerLabel = installerLabel,
                        isSystemApp = isSystemApp,
                        isEnabled = isEnabled,
                        userId = userId,
                        isAdbInstalled = isAdbInstalled
                    )
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 当 Shizuku 无权限或未提供时，回退到 PackageManager 获取安装来源包名。
     *
     * Android R（API 30）及以上使用 `getInstallSourceInfo`，以下使用已废弃的
     * `getInstallerPackageName`。
     *
     * @param pm 系统 PackageManager 实例
     * @param packageName 目标应用包名
     * @return 安装来源包名；获取失败或与应用商店无关时返回 null
     */
    private fun getInstallerPackageName(pm: PackageManager, packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }
    }
}
