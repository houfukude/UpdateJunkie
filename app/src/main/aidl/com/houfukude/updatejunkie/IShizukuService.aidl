package com.houfukude.updatejunkie;

import android.os.Bundle;

interface IShizukuService {
    /**
     * 获取指定包的安装信息。
     * 返回的 Bundle 包含：
     * - "installer": String? (安装来源包名)
     * - "users": int[] (已安装该应用的用户 ID 列表)
     */
    Bundle getInstallInfo(String packageName) = 1;

    /**
     * 销毁服务进程。
     * Shizuku 规范要求的特殊交易代码。
     */
    void destroy() = 16777114;
}
