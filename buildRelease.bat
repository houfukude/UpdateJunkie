@echo off
:: =================================================================
:: Update Junkie 本地打包脚本 (Windows) - 支持 .env
:: =================================================================

if not exist .env (
    echo [!] .env file not found. Please create one based on .env.sample
    pause
    exit /b 1
)

echo [*] Loading configuration from .env...
for /f "usebackq tokens=1* delims==" %%a in (".env") do (
    set "line=%%a"
    if defined line (
        if not "x!line:~0,1!"=="x#" (
            set "%%a=%%b"
        )
    )
)
:: 上面的逻辑在没有开启延迟扩展时会失败。
:: 换一种更可靠的方式：

for /f "usebackq delims=" %%i in (".env") do (
    echo %%i | findstr /v "^#" > nul && set "%%i"
)

echo [*] Starting Build Release APK...
call gradlew.bat :app:assembleRelease

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [+] Build Successful!
    echo [+] APK Location: app\build\outputs\apk\release\
) else (
    echo.
    echo [-] Build Failed!
)

pause
