package com.houfukude.updatejunkie.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    companion object {
        @Volatile
        private var instance: AppConfigRepository? = null

        fun getInstance(context: Context): AppConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: AppConfigRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val configFile = File(context.filesDir, "app_configs.json")
    private val _configsFlow = MutableStateFlow(JSONObject())
    val configsFlow: StateFlow<JSONObject> = _configsFlow.asStateFlow()

    private var configs: JSONObject
        get() = _configsFlow.value
        set(value) {
            _configsFlow.value = value
        }

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
     * 将当前配置保存到磁盘并通知订阅者。
     */
    private fun saveConfigs() {
        try {
            val jsonString = configs.toString(2)
            configFile.writeText(jsonString)
            // 重新创建一个新的 JSONObject 以触发 StateFlow 的更新通知
            _configsFlow.value = JSONObject(jsonString)
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
        return configs.optJSONObject(packageName)?.opt("updateUrl") as? String
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

    /**
     * 获取所有应用配置的 JSON 字符串。
     */
    fun getAllConfigs(): JSONObject = configs

    /**
     * 从 JSON 对象导入配置并持久化。
     */
    fun importConfigs(newConfigs: JSONObject) {
        configs = newConfigs
        saveConfigs()
    }
}
