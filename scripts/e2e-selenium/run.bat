@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install JRE 8 first.
    pause
    exit /b 1
)

if not exist "e2e-ui.jar" (
    echo [ERROR] e2e-ui.jar is missing next to this bat file.
    pause
    exit /b 1
)

echo ============================================
echo  Daqian BBS UI E2E (Selenium)
echo  Config: config.properties
echo  Negative self-check: built in
echo ============================================
echo.

java -Dfile.encoding=UTF-8 -jar e2e-ui.jar %*
set CODE=%ERRORLEVEL%

echo.
echo Exit code: %CODE%
echo Reports: %~dp0reports
if exist "%~dp0reports" start "" "%~dp0reports"
pause
exit /b %CODE%
