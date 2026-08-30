package com.houfukude.updatejunkie.shizuku

import android.content.pm.PackageManager
import android.os.Build
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object ShizukuManager {
    const val REQUEST_CODE_SHIZUKU = 1001

    private var _packageManager: Any? = null

    private fun getIPackageManager(): Any? {
        if (_packageManager != null) return _packageManager
        return try {
            val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
            val stubClass = Class.forName("android.content.pm.IPackageManager\$Stub")
            val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
            _packageManager = asInterfaceMethod.invoke(null, binder)
            _packageManager
        } catch (e: Throwable) {
            null
        }
    }

    fun getDetailedInstaller(packageName: String, userId: Int): String? {
        if (!hasPermission()) return null
        val ipm = getIPackageManager() ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                val getInstallSourceInfoMethod = ipm.javaClass.getMethod("getInstallSourceInfo", String::class.java, Int::class.java)
                val installSourceInfo = getInstallSourceInfoMethod.invoke(ipm, packageName, userId)
                if (installSourceInfo != null) {
                    val getInitiatingPackageNameMethod = installSourceInfo.javaClass.getMethod("getInitiatingPackageName")
                    val initiating = getInitiatingPackageNameMethod.invoke(installSourceInfo) as? String
                    
                    if (initiating != null && initiating != "com.android.shell" && initiating != "com.google.android.packageinstaller" && initiating != "com.android.packageinstaller") {
                        return initiating
                    }
                    
                    val getInstallingPackageNameMethod = installSourceInfo.javaClass.getMethod("getInstallingPackageName")
                    getInstallingPackageNameMethod.invoke(installSourceInfo) as? String
                } else null
            } else {
                // Pre-Android 11
                val getInstallerPackageNameMethod = ipm.javaClass.getMethod("getInstallerPackageName", String::class.java)
                getInstallerPackageNameMethod.invoke(ipm, packageName) as? String
            }
        } catch (e: Throwable) {
            null
        }
    }

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun requestPermission(requestCode: Int = REQUEST_CODE_SHIZUKU) {
        try {
            if (!Shizuku.isPreV11()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            // Log or handle error
        }
    }

    fun getUid(): Int {
        return try {
            Shizuku.getUid()
        } catch (e: Throwable) {
            -1
        }
    }
}
