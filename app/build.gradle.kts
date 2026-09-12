import com.android.build.api.variant.BuildConfigField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.houfukude.updatejunkie"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.houfukude.updatejunkie"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "debug.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }
}

// 1. 定义动态时间来源，专门用于解决 Configuration Cache 开启时不更新的问题
abstract class BuildTimeValueSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        // 强制使用东八区 (UTC+8) 时间，确保 GitHub Actions 与本地时间一致
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+8")
        val date = dateFormat.format(Date())
        println("[INFO] BuildTime (GMT+8): $date")
        return date
    }
}

// 自定义输出文件名 (适配 AGP 8.0+)
androidComponents {
    onVariants { variant ->
        // 2. 获取动态时间的 Provider (它在任务执行阶段才会被调用)
        val buildTimeProvider = providers.of(BuildTimeValueSource::class.java) {}

        // 3. 注入 BUILD_TIME 字段
        variant.buildConfigFields?.put("BUILD_TIME", buildTimeProvider.map { time ->
            BuildConfigField("String", "\"$time\"", "Build Time")
        })

        variant.outputs.forEach { output ->
            val versionName = android.defaultConfig.versionName ?: "1.1"
            val applicationId = android.defaultConfig.applicationId ?: "com.houfukude.updatejunkie"
            output.outputFileName.set("${applicationId}_${versionName}_${variant.name}.apk")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}