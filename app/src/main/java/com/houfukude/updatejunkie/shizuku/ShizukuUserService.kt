package com.houfukude.updatejunkie.shizuku

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import com.houfukude.updatejunkie.APP
import com.houfukude.updatejunkie.IShizukuService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern
import kotlin.system.exitProcess

/**
 * 在 Shizuku 权限进程（ADB/Root）中运行的服务实现。
 * 纯粹通过解析 dumpsys 输出获取信息，绕过不稳定的系统 API。
 */
class ShizukuUserService(val context: Context? = null) : IShizukuService.Stub() {

    override fun getInstallInfo(packageName: String): Bundle {
        if (APP.isNoisyMode) {
            Log.d("ShizukuUserService", "getInstallInfo called for $packageName")
        }
        val userRegex = Pattern.compile("^\\s*User (\\d+):.*?\\binstalled=true\\b")
        val userIds = mutableListOf<Int>()

        var initiating: String? = null
        var originating: String? = null

        try {
            val process = Runtime.getRuntime().exec(arrayOf("dumpsys", "package", packageName))
            if (APP.isNoisyMode) {
                Log.i("ShizukuUserService", "Started dumpsys for $packageName")
            }
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                var lineCount = 0
                while (reader.readLine().also { line = it } != null) {
                    lineCount++
                    val trimmed = line!!.trim()
                    if (APP.isNoisyMode && lineCount <= 1) Log.i(
                        "ShizukuUserService",
                        "Reading output, first line: $trimmed"
                    )
                    // 1. 解析安装来源（排除 installerPackageName）
                    when {
                        trimmed.startsWith("initiatingPackageName=") || trimmed.startsWith("initiatingPackage=") -> {
                            initiating = parseValue(trimmed)
                        }

                        trimmed.startsWith("originatingPackageName=") -> {
                            originating = parseValue(trimmed)
                        }
                    }

                    // 2. 解析用户安装状态
                    val matcher = userRegex.matcher(line)
                    if (matcher.find()) {
                        matcher.group(1)?.toIntOrNull()?.let { userIds.add(it) }
                    }
                }
            }
            val exitCode = process.waitFor()
            if (APP.isNoisyMode) {
                Log.i(
                    "ShizukuUserService",
                    "dumpsys finished for $packageName with exit code $exitCode"
                )
            }
            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().readText()
                Log.e("ShizukuUserService", "dumpsys error: $error")
            }
        } catch (e: Exception) {
            Log.e("ShizukuUserService", "Service execution error", e)
            // 如果在执行过程中 Binder 死亡或出现异常，返回已获取的部分数据或空数据
        } catch (e: Throwable) {
            // 捕获可能的 NoSuchElementException 或其他非 Exception 异常
            Log.e("ShizukuUserService", "Unexpected error in service", e)
        }

        return bundleOf(
            "initiating" to initiating,
            "originating" to originating,
            "users" to userIds.toIntArray()
        )
    }

    private fun parseValue(line: String): String? {
        return line.substringAfter("=")
            .split(" ", ",")
            .firstOrNull { it.isNotBlank() && it != "null" }
    }

    override fun destroy() {
        exitProcess(0)
    }
}
