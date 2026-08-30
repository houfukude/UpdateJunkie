package com.houfukude.updatejunkie.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val installerPackageName: String?,
    val installerLabel: String?,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val userId: Int,
    val isAdbInstalled: Boolean
)
