package com.houfukude.updatejunkie.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.houfukude.updatejunkie.model.AppInfo
import com.houfukude.updatejunkie.shizuku.ShizukuManager
import com.houfukude.updatejunkie.utils.MarketUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(includeSystemApps: Boolean = true): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        // 使用 MATCH_UNINSTALLED_PACKAGES 和 MATCH_DISABLED_COMPONENTS 获取完整列表
        val flags = PackageManager.GET_META_DATA or 
                    PackageManager.MATCH_DISABLED_COMPONENTS or 
                    PackageManager.MATCH_UNINSTALLED_PACKAGES
        
        val packages = pm.getInstalledPackages(flags)
        
        packages.mapNotNull { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            
            if (!includeSystemApps && isSystemApp) {
                return@mapNotNull null
            }

            val packageName = packageInfo.packageName
            val label = appInfo.loadLabel(pm).toString()
            val icon = appInfo.loadIcon(pm)
            val isEnabled = appInfo.enabled
            val userId = appInfo.uid / 100000
            
            val installerPackageName = getInstallerPackageName(pm, packageName, userId)
            val installerLabel = MarketUtils.getMarketLabel(installerPackageName)
            val isAdbInstalled = installerPackageName == null || installerPackageName == "com.android.shell"

            AppInfo(
                packageName = packageName,
                label = label,
                icon = icon,
                installerPackageName = installerPackageName,
                installerLabel = installerLabel,
                isSystemApp = isSystemApp,
                isEnabled = isEnabled,
                userId = userId,
                isAdbInstalled = isAdbInstalled
            )
        }.sortedBy { it.label.lowercase() }
    }

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
