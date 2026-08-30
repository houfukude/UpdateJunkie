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

sealed class AppLoadResult {
    data class Total(val count: Int) : AppLoadResult()
    data class App(val app: AppInfo) : AppLoadResult()
}

class AppRepository(private val context: Context) {

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
            val userId = appInfo.uid / 100000

            val installerPackageName = if (isSystemApp) {
                MarketUtils.SYSTEM_APP_INSTALLER
            } else {
                getInstallerPackageName(pm, packageName, userId)
            }
            
            val installerLabel = MarketUtils.getMarketLabel(installerPackageName)
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

    private fun getInstallerPackageName(pm: PackageManager, packageName: String, userId: Int): String? {
        // 尝试使用 Shizuku 获取更详细的安装来源信息
        val shizukuInstaller = ShizukuManager.getDetailedInstaller(packageName, userId)
        if (shizukuInstaller != null) return shizukuInstaller

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
