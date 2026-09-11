package com.houfukude.updatejunkie.data

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * 应用特定的配置仓库，用于存储每个应用的自定义更新 URL 等信息。
 *
 * 数据以 JSON 格式存储在应用私有目录的 `app_configs.json` 文件中。
 *
 * @property context 应用上下文
 */
class AppConfigRepository(context: Context) {
    private val configFile = File(context.filesDir, "app_configs.json")
    private var configs = JSONObject()

    init {
        loadConfigs()
    }

    /**
     * 从磁盘加载配置文件。如果文件不存在或损坏，将使用空的 JSON 对象。
     */
    private fun loadConfigs() {
        if (configFile.exists()) {
            try {
                val jsonString = configFile.readText()
                configs = JSONObject(jsonString)
            } catch (e: Exception) {
                Log.e("AppConfigRepository", "Error loading configs", e)
                configs = JSONObject()
            }
        }
    }

    /**
     * 将当前配置保存到磁盘。
     */
    private fun saveConfigs() {
        try {
            configFile.writeText(configs.toString(2))
        } catch (e: Exception) {
            Log.e("AppConfigRepository", "Error saving configs", e)
        }
    }

    /**
     * 获取指定应用的更新 URL。
     *
     * @param packageName 应用包名
     * @return 配置的 URL，若未设置则为 null
     */
    fun getUpdateUrl(packageName: String): String? {
        return if (configs.has(packageName)) {
            configs.optJSONObject(packageName)?.optString("updateUrl", null)
        } else {
            null
        }
    }

    /**
     * 设置并保存指定应用的更新 URL。
     *
     * @param packageName 应用包名
     * @param url 更新地址
     */
    fun setUpdateUrl(packageName: String, url: String) {
        val appConfig = configs.optJSONObject(packageName) ?: JSONObject()
        appConfig.put("updateUrl", url.trim())
        configs.put(packageName, appConfig)
        saveConfigs()
    }
}
