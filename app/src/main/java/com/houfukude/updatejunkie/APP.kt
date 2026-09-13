package com.houfukude.updatejunkie

import android.app.Application
import com.houfukude.updatejunkie.shizuku.ShizukuManager

/**
 * 应用全局 Application 类。
 *
 * 负责全局性的初始化操作。
 */
class APP : Application() {

    companion object {
        /** 是否开启吵闹模式。 */
        var isNoisyMode: Boolean = false

        var isTvMode: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        // 检测是否为 TV 模式
        // 暂时 用 TV 模式代替手机模式
        //isTvMode = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        // 初始化 Shizuku 管理器
        ShizukuManager.init()
    }
}
