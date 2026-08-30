package com.houfukude.updatejunkie.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku 权限与 Shell 命令的封装管理器。
 *
 * 通过 Shizuku 以 Shell 身份执行 `dumpsys package`，从而读取普通 API 无法获取的
 * 安装来源（initiating / originating / installer）与各用户安装状态。
 *
 * 所有接口均已做异常兜底，Shizuku 不可用时会安全降级而不会崩溃。
 */
object ShizukuManager {
    /** 请求 Shizuku 授权时使用的一次性请求码。 */
    const val REQUEST_CODE_SHIZUKU = 1001

    /**
     * 系统内置的安装器包名集合。
     * 这些来源属于通用安装通道，不代表真正的应用商店，解析时应被跳过。
     */
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
     *
     * @param packageName 目标应用包名
     * @return 解析得到的安装信息；Shizuku 未授权或命令执行失败时返回 null
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

    /**
     * 根据优先级逻辑解析最终安装来源包名。
     *
     * 优先级：非通用安装器的 originating → 非通用安装器的 initiating →
     * originating → initiating → installing。
     *
     * @param initiating 发起安装的包名
     * @param originating 原始下载来源的包名（如浏览器）
     * @param installing 系统当前记录的安装器包名
     * @return 最终认定的安装来源包名，全部为空时返回 null
     */
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
     *
     * @param output `dumpsys package` 的完整输出
     * @return 该包已安装的所有用户 ID 列表，无匹配时为空列表
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

    /**
     * 获取该包实际安装所在 userId；无已安装用户时回退到主用户 0。
     *
     * @param packageName 目标应用包名
     * @return 实际安装的 userId，无法确定时返回 0
     */
    fun resolveUserId(packageName: String): Int {
        return getInstallInfo(packageName)?.users?.firstOrNull() ?: 0
    }

    /**
     * 从 `key=value` 形式的 dumpsys 行中提取有效值。
     *
     * 会跳过空串与字面量 `null`。
     *
     * @param line 形如 `installerPackageName=com.android.vending` 的行
     * @return 解析出的值；无有效值时返回 null
     */
    private fun parseValue(line: String): String? {
        return line.substringAfter("=")
            .split(" ", ",")
            .firstOrNull { it.isNotBlank() && it != "null" }
    }

    /**
     * 通过 Shizuku 以 Shell 方式执行命令并返回完整输出。
     * 在某些版本的 Shizuku 中 newProcess 是私有的，通过反射调用以确保兼容性。
     *
     * @param command 待执行的 Shell 命令
     * @return 命令的标准输出；未授权、反射失败或执行异常时返回 null
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

    /**
     * 一次 dumpsys 解析得到的完整安装信息。
     *
     * @property installerPackageName 最终认定的安装来源包名，无法确定时为 null
     * @property users 该包已安装的所有用户 ID
     */
    data class PackageInstallInfo(
        val installerPackageName: String?,
        val users: List<Int>
    )

    /**
     * 返回该包在哪些用户（User）下已安装的 userId 列表。
     *
     * @param packageName 目标应用包名
     * @return 已安装的用户 ID 列表，获取失败时为空列表
     */
    fun getInstalledUsers(packageName: String): List<Int> {
        return getInstallInfo(packageName)?.users ?: emptyList()
    }

    /**
     * 判断设备上是否安装了 Shizuku 应用。
     *
     * @param context 应用上下文
     * @return 已安装返回 true，否则返回 false
     */
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

    /**
     * 判断 Shizuku 服务是否正在运行（Binder 可连通）。
     *
     * @return 服务已启动且可用返回 true，否则返回 false
     */
    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 判断本应用是否已被授予 Shizuku 授权。
     *
     * Shizuku v11 以下版本（pre-v11）不支持运行时权限，直接返回 false。
     *
     * @return 已授权返回 true，否则返回 false
     */
    fun hasPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 向 Shizuku 发起授权请求。
     *
     * 结果需通过 `Shizuku.addRequestPermissionResultListener` 监听。
     *
     * @param requestCode 请求码，默认使用 [REQUEST_CODE_SHIZUKU]
     */
    fun requestPermission(requestCode: Int = REQUEST_CODE_SHIZUKU) {
        try {
            if (!Shizuku.isPreV11()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * 获取 Shizuku 服务运行所使用的 UID（通常为 adb 或 root 的 UID）。
     *
     * @return Shizuku 的 UID；获取失败时返回 -1
     */
    fun getUid(): Int {
        return try {
            Shizuku.getUid()
        } catch (e: Throwable) {
            e.printStackTrace()
            -1
        }
    }
}
