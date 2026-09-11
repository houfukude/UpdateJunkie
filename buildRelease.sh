#!/bin/bash
# =================================================================
# Update Junkie 本地打包脚本 (Linux/macOS) - 支持 .env
# =================================================================

if [ -f .env ]; then
    echo "[*] Loading configuration from .env..."
    # 导出 .env 中的所有变量，排除注释
    export $(grep -v '^#' .env | xargs)
else
    echo "[!] .env file not found. Please create one based on .env.sample"
    exit 1
fi

echo "[*] Starting Build Release APK..."
chmod +x gradlew
./gradlew :app:assembleRelease

if [ $? -eq 0 ]; then
    echo ""
    echo "[+] Build Successful!"
    echo "[+] APK Location: app/build/outputs/apk/release/"
else
    echo ""
    echo "[-] Build Failed!"
fi
