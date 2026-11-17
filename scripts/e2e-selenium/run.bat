@echo off
chcp 65001 >nul
cd /d "%~dp0"

java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未检测到 Java，请先安装 JRE/JDK 8（离线包需自带安装程序）。
    pause
    exit /b 1
)

echo ============================================
echo  大千智荟论坛 · 界面级 E2E（Selenium）
echo  配置文件：config.properties
echo ============================================

if /I "%~1"=="selfcheck" (
    echo 模式：负向自检（验证判据能变红，只读）
    java -Dfile.encoding=UTF-8 -jar e2e-ui.jar --selfcheck
) else (
    java -Dfile.encoding=UTF-8 -jar e2e-ui.jar %*
)
set CODE=%errorlevel%

echo.
echo 报告目录：%~dp0reports
start "" "%~dp0reports"
pause
exit /b %CODE%
