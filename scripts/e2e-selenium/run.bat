@echo off
chcp 65001 >nul
cd /d "%~dp0"

java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] ???? Java????? JRE/JDK 8?????????????
    pause
    exit /b 1
)

echo ============================================
echo  ?????? ? ??? E2E?Selenium?
echo  ?????config.properties
echo ============================================
java -Dfile.encoding=UTF-8 -jar e2e-ui.jar %*
set CODE=%errorlevel%

echo.
echo ?????%~dp0reports
start "" "%~dp0reports"
pause
exit /b %CODE%

