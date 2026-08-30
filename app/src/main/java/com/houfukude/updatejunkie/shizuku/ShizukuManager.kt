package com.houfukude.updatejunkie.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuManager {
    const val REQUEST_CODE_SHIZUKU = 1001

    private val GENERIC_INSTALLERS = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.shell",
        "android"
    )

    fun getDetailedInstaller(packageName: String, userId: Int): String? {
        if (!hasPermission()) return null
        
        return try {
            // 使用 dumpsys package 获取详细的安装来源信息，采用 Shell 方案以绕过复杂的反射签名问题
            val command = "dumpsys package $packageName"
            
            // 注意：在某些版本的 Shizuku 中 newProcess 是私有的，通过反射调用以确保兼容性
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            
            val reader = process.inputStream.bufferedReader()
            
            var initiating: String? = null
            var originating: String? = null
            var installing: String? = null

            reader.forEachLine { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("initiatingPackageName=") || trimmed.startsWith("initiatingPackage=") -> {
                        initiating = parseValue(trimmed)
                    }
                    trimmed.startsWith("originatingPackageName=") -> {
                        originating = parseValue(trimmed)
                    }
                    trimmed.startsWith("installerPackageName=") -> {
                        installing = parseValue(trimmed)
                    }
                }
            }
            process.waitFor()

            // 优先级逻辑：
            // 1. 原始来源 (Originating) 如果不是通用安装器，则是下载来源（如 Chrome）
            if (originating != null && !GENERIC_INSTALLERS.contains(originating)) {
                return originating
            }
            // 2. 发起者 (Initiating) 如果不是通用安装器，则是真正的商店来源
            if (initiating != null && !GENERIC_INSTALLERS.contains(initiating)) {
                return initiating
            }
            // 3. 回退到原始来源
            if (originating != null) {
                return originating
            }
            // 4. 回退到发起者（如 ADB 等）
            if (initiating != null) {
                return initiating
            }
            // 5. 最后回退到当前记录的安装器
            return installing
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun parseValue(line: String): String? {
        return line.substringAfter("=")
            .split(" ", ",")
            .firstOrNull { it.isNotBlank() && it != "null" }
    }

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun requestPermission(requestCode: Int = REQUEST_CODE_SHIZUKU) {
        try {
            if (!Shizuku.isPreV11()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun getUid(): Int {
        return try {
            Shizuku.getUid()
        } catch (e: Throwable) {
            e.printStackTrace()
            -1
        }
    }
}
