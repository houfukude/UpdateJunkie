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

    /**
     * 对指定包执行一次 `dumpsys package`，同时解析出：
     *  - 安装来源包名（PackageName）
     *  - 各用户（User）下的安装状态（userId）
     * 避免为安装来源和用户信息分别执行两次 dumpsys。
     */
    fun getInstallInfo(packageName: String): PackageInstallInfo? {
        // 使用 dumpsys package 获取详细的安装来源信息，采用 Shell 方案以绕过复杂的反射签名问题
        val output = runShellCommand("dumpsys package $packageName") ?: return null

        var initiating: String? = null
        var originating: String? = null
        var installing: String? = null

        output.lineSequence().forEach { line ->
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

        return PackageInstallInfo(
            installerPackageName = resolveInstaller(initiating, originating, installing),
            users = parseUsers(output)
        )
    }

    /** 根据优先级逻辑解析最终安装来源包名 */
    private fun resolveInstaller(initiating: String?, originating: String?, installing: String?): String? {
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
    }

    /**
     * 解析 dumpsys 输出中该包已安装的各用户（User）userId。
     * 例如 USER 0 未安装、USER 10 已安装时，会返回 [10]。
     */
    private fun parseUsers(output: String): List<Int> {
        val users = mutableListOf<Int>()
        val regex = Regex("""^\s*User (\d+):.*?\binstalled=true\b""")
        output.lineSequence().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                users.add(match.groupValues[1].toInt())
            }
        }
        return users
    }

    /** 获取该包实际安装所在 userId；无已安装用户时回退到主用户 0 */
    fun resolveUserId(packageName: String): Int {
        return getInstallInfo(packageName)?.users?.firstOrNull() ?: 0
    }

    private fun parseValue(line: String): String? {
        return line.substringAfter("=")
            .split(" ", ",")
            .firstOrNull { it.isNotBlank() && it != "null" }
    }

    /**
     * 通过 Shizuku 以 Shell 方式执行命令并返回完整输出。
     * 在某些版本的 Shizuku 中 newProcess 是私有的，通过反射调用以确保兼容性。
     */
    private fun runShellCommand(command: String): String? {
        if (!hasPermission()) return null

        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /** 一次 dumpsys 解析得到的完整安装信息 */
    data class PackageInstallInfo(
        val installerPackageName: String?,
        val users: List<Int>
    )

    /** 返回该包在哪些用户（User）下已安装的 userId 列表 */
    fun getInstalledUsers(packageName: String): List<Int> {
        return getInstallInfo(packageName)?.users ?: emptyList()
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
