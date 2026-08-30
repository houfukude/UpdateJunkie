package com.houfukude.updatejunkie.data

/**
 * 应用主题模式配置。
 *
 * 该枚举的 [name] 会被持久化到 DataStore 中，因此枚举项名称不可随意更改，
 * 否则旧版本存储的取值将无法反序列化（内部已做了异常兜底）。
 *
 * @property FOLLOW_SYSTEM 跟随系统深浅色设置
 * @property LIGHT 强制使用亮色主题
 * @property DARK 强制使用暗色主题
 */
enum class ThemeConfig {
    FOLLOW_SYSTEM, LIGHT, DARK
}
