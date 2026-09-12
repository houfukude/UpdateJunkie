# 更新控 (Update Junkie)

![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-%E2%9C%93-4285F4?logo=jetpackcompose&logoColor=white)
![Shizuku](https://img.shields.io/badge/Shizuku-%E2%9C%93-3DDC84)
![Material 3](https://img.shields.io/badge/Material_3-%E2%9C%93-757575)
[![构建状态](https://img.shields.io/github/actions/workflow/status/houfukude/UpdateJunkie/build-release.yml?branch=release&label=%E6%9E%84%E5%BB%BA%E7%8A%B6%E6%80%81)](https://github.com/houfukude/UpdateJunkie/actions/workflows/build-release.yml)
[![发布日](https://img.shields.io/github/release-date-pre/houfukude/UpdateJunkie?label=%E5%8F%91%E5%B8%83%E6%97%A5)](https://github.com/houfukude/UpdateJunkie/releases)
[![版本](https://img.shields.io/github/v/release/houfukude/UpdateJunkie?include_prereleases&label=%E7%89%88%E6%9C%AC)](https://github.com/houfukude/UpdateJunkie/releases)

**更新控** 是一款专注于管理和追踪 Android 应用安装来源及更新地址的实用工具。通过集成的 Shizuku
权限管理，它可以精确还原每个应用的“真实”安装渠道，并允许用户自定义和共享应用更新配置。

## 📱 应用截图

<table>
  <tr>
    <td align="center" width="25%"><img src="https://raw.githubusercontent.com/houfukude/UpdateJunkie/master/screenshot/1.png" width="100%" alt="应用列表：Shizuku 已连接，逐条展示安装来源与版本" /></td>
    <td align="center" width="25%"><img src="https://raw.githubusercontent.com/houfukude/UpdateJunkie/master/screenshot/2.png" width="100%" alt="应用列表：持续还原系统应用与自更新应用的真实来源" /></td>
    <td align="center" width="25%"><img src="https://raw.githubusercontent.com/houfukude/UpdateJunkie/master/screenshot/3.png" width="100%" alt="导入配置：支持 URL、文件与局域网导入" /></td>
    <td align="center" width="25%"><img src="https://raw.githubusercontent.com/houfukude/UpdateJunkie/master/screenshot/4.png" width="100%" alt="导出配置：支持文件导出与局域网服务" /></td>
  </tr>
</table>

## 🚀 核心功能

- **深度安装来源识别**：
  - **打破“安装程序”迷雾**：不仅仅显示“软件包安装程序”，更能挖掘出其背后的发起者（如
    Chrome、系统浏览器等真实下载源）。
  - **智能优先级**：自动识别 `Originating`（原始来源）与 `Initiating`（发起者），呈现最有价值的来源信息。
- **强大的搜索与过滤**：
  - **混合搜索**：支持“应用名+包名”的多关键词混合搜索，空格分隔关键词，快速定位。
  - **精细过滤**：支持按系统应用、已禁用状态及特定的安装来源（如 Google Play、酷安、ADB 等）进行筛选。
- **自定义更新管理**：
  - **配置更新 URL**：为任意应用手动设置更新检查地址，方便追踪非商店应用的更新。
  - **一键跳转**：支持快速跳转至应用商店或自定义的更新 URL。
  - **配置响应机制**：支持应用感知并响应配置文件的动态变化。
- **配置导入与共享**：
  - **多维度导入**：支持通过本地 JSON 文件、远程 URL 链接或**局域网发现 (NSD)** 导入配置。
  - **局域网快速同步**：内置简单的 TCP 服务端，支持在同一网络下的多台设备间快速共享配置。
  - **查看原始配置**：支持在应用内直接查看格式化后的 JSON 配置文件。
- **高性能与稳定性**：
  - **流式增量加载**：解析完一个显示一个，实时进度追踪，彻底告别 Loading 等待。
  - **Shizuku 优化**：针对 Android 14+ 进行了 Binder 稳定性增强，支持服务断开自动重连与异常防护。
- **现代化交互设计**：完全基于 Jetpack Compose 与 Material 3 规范开发，支持深色模式与多语言（中/英）。

## 🛠️ 技术栈

- **Jetpack Compose**：全响应式 UI 构建。
- **Material 3**：Google 最新设计语言。
- **Shizuku API**：实现底层权限调用的核心方案。
- **Kotlin Flow & Coroutines**：异步非阻塞逻辑。
- **NSD (Network Service Discovery)**：局域网设备自动发现。

## 📋 系统要求

- **Android 版本**：Android 7.0 (API 24) 及以上。
- **权限组件**：建议运行 [Shizuku](https://shizuku.rikka.app/) 以获取深度安装来源和用户列表信息。

## 📦 打包与构建

### 1. 自动化构建 (GitHub Actions)

项目已集成 GitHub Actions。当代码推送到 `release` 分支时，会自动触发构建流水线：

- 自动提取版本号并创建 GitHub Release。
- 自动上传已签名的 APK 产物。
- 自动同步上传 `config` 目录下对应版本的配置文件。
- 自动生成 Release 说明：正文先列出本次构建的版本号、构建号、提交、构建时间与产物名称，其后拼接 GitHub
  依据提交记录生成的变更清单。

### 2. 本地构建

项目根目录提供了便捷的本地打包脚本：

- **Windows**: `buildRelease.bat`
- **macOS/Linux**: `buildRelease.sh`

#### 配置步骤：

1. 复制 `.env.sample` 为 `.env`。
2. 在 `.env` 文件中配置您的 JKS 密钥信息：
   ```env
   KEYSTORE_PATH=C:/path/to/your/release.jks
   KEYSTORE_PASSWORD=your_keystore_password
   KEY_ALIAS=your_key_alias
   KEY_PASSWORD=your_key_password
   ```
3. 运行对应的脚本，生成的已签名 APK 位于 `app/build/outputs/apk/release/app-release.apk`。

## ✅ 已实现的功能

- **操作增强**：在应用操作菜单中新增「复制包名」功能。
- **开发体验**：为 `debug` 构建变体添加 `.debug` 包名后缀，支持同设备共存测试。
- **筛选功能**：在筛选菜单中添加「只显示有配置的」项，仅显示已配置更新 URL 的应用。
- **筛选优化**：将「自更新软件」合并为筛选菜单中的单独一项，不再逐个包名展示，并优化排序逻辑（系统 > ADB >
  自更新 > 其他）。
- **导入体验**：通过 URL 导入时默认自动填充对应版本的官方 Release 配置文件地址，并持久化用户最后的输入。
- **搜索优化**：优化搜索模式 UI，确保功能按钮不被遮挡，并新增搜索无结果时的「重置筛选条件」功能。
- **稳定性修复**：解决扫描过程中重复触发刷新可能导致的崩溃问题。
- **更新日志**：支持在设置页点击「构建时间」直接查看当前版本的更新内容。

## 📝 TODO

待实现的功能规划：

- [ ] 更多功能待补充……

---

*保持应用更新，从掌控来源开始。*
