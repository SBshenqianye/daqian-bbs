# E2E 测试运行脚本
# 自动复制数据库 → 启动测试后端 → 启动前端(指向测试后端) → 运行 Playwright → 清理
# 用法: .\run-e2e.ps1 [-KeepDb] [-KeepServer] [-KeepFrontend] [-SkipCopy] [-Port 9084]
#
# 关键设计：前端 dev server 的 proxy 通过 DEV_BACKEND_URL / VUE_APP_BBS_BASE_API
# 环境变量重定向到测试后端端口，确保 E2E 测试不污染生产数据库。

param(
    [int]$Port = 9084,         # 测试后端端口
    [switch]$KeepDb,           # 保留测试数据库（调试用）
    [switch]$KeepServer,       # 保留测试服务器（调试用）
    [switch]$KeepFrontend,     # 保留前端进程（调试用）
    [switch]$SkipCopy          # 跳过数据库复制（服务器已在运行时用）
)

$ErrorActionPreference = "Stop"
$PROD_DB = "bbs_db"
$TEST_DB = "bbs_e2e_test"
$MYSQL = "mysql -u root -proot"
$USER_PORT = 9081
$ADMIN_PORT = 9082
$PROJECT_ROOT = "D:\workspace\20260522-20260615-daqianbbsfrontend\daqian-bbs"
$USER_DIR = "$PROJECT_ROOT\bbs-ui"
$ADMIN_DIR = "$PROJECT_ROOT\bbs-admin-ui"
$E2E_DIR = "$PROJECT_ROOT\bbs-server\src\test\e2e"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  大千智荟 BBS - E2E 测试（数据库隔离）" -ForegroundColor Cyan
Write-Host "  测试库: $TEST_DB | 后端端口: $Port" -ForegroundColor Cyan
Write-Host "  前端端口: 用户=$USER_PORT 管理=$ADMIN_PORT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ========== 1. 复制数据库 ==========
if (-not $SkipCopy) {
    Write-Host "`n[1/7] 复制数据库 $PROD_DB → $TEST_DB ..." -ForegroundColor Yellow

    Invoke-Expression "$MYSQL -e `"DROP DATABASE IF EXISTS $TEST_DB`""
    Write-Host "  已清理旧测试库"

    Invoke-Expression "$MYSQL -e `"CREATE DATABASE $TEST_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci`""
    Write-Host "  已创建测试库"

    $tables = Invoke-Expression "$MYSQL -N -e `"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='$PROD_DB' AND TABLE_TYPE='BASE TABLE'`""
    $tableList = $tables -split "`n" | Where-Object { $_.Trim() -ne "" }
    $count = 0
    foreach ($table in $tableList) {
        $table = $table.Trim()
        Invoke-Expression "$MYSQL -e `"CREATE TABLE $TEST_DB.$table LIKE $PROD_DB.$table`""
        Invoke-Expression "$MYSQL -e `"INSERT INTO $TEST_DB.$table SELECT * FROM $PROD_DB.$table`""
        $count++
    }
    Write-Host "  已复制 $count 张表" -ForegroundColor Green
} else {
    Write-Host "`n[1/7] 跳过数据库复制" -ForegroundColor Yellow
}

# ========== 2. 启动测试后端 ==========
$backendJob = $null
if (-not $KeepServer) {
    Write-Host "`n[2/7] 启动测试后端 (端口 $Port, profile=e2e) ..." -ForegroundColor Yellow

    $backendJob = Start-Job -ScriptBlock {
        param($port, $projectRoot)
        $env:SPRING_PROFILES_ACTIVE = "e2e"
        $env:BBS_SERVER_PORT = "$port"
        Set-Location "$projectRoot\bbs-server"
        mvn spring-boot:run "-Dmaven.repo.local=./target/test-repo" -q 2>&1
    } -ArgumentList $Port, $PROJECT_ROOT

    Write-Host "  等待后端启动..."
    $maxWait = 120
    $waited = 0
    while ($waited -lt $maxWait) {
        Start-Sleep -Seconds 3
        $waited += 3
        try {
            $r = Invoke-WebRequest -Uri "http://localhost:$Port/bbs-server/common/saOrgTree" -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
            if ($r.StatusCode -eq 200) {
                Write-Host "  ✅ 后端已启动 ($waited 秒)" -ForegroundColor Green
                break
            }
        } catch {}
        Write-Host "  等待中... ($waited s)"
    }
    if ($waited -ge $maxWait) {
        Write-Host "  ❌ 后端启动超时" -ForegroundColor Red
        Stop-Job $backendJob -ErrorAction SilentlyContinue
        exit 1
    }
} else {
    Write-Host "`n[2/7] 跳过后端启动（服务器已在运行）" -ForegroundColor Yellow
}

# ========== 3. 停止已有前端进程 ==========
Write-Host "`n[3/7] 停止已有前端进程 ..." -ForegroundColor Yellow
$nodeProcs = Get-Process -Name node -ErrorAction SilentlyContinue
foreach ($proc in $nodeProcs) {
    try {
        $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId=$($proc.Id)" -ErrorAction SilentlyContinue).CommandLine
        if ($cmdLine -match "bbs-ui" -or $cmdLine -match "bbs-admin-ui") {
            Write-Host "  停止 PID $($proc.Id): $($cmdLine.Substring(0, [Math]::Min(80, $cmdLine.Length)))"
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
    } catch {}
}
Start-Sleep -Seconds 2
Write-Host "  已清理" -ForegroundColor Green

# ========== 4. 启动前端（指向测试后端） ==========
Write-Host "`n[4/7] 启动前端 (proxy → localhost:$Port) ..." -ForegroundColor Yellow

# bbs-ui: DEV_BACKEND_URL 控制 proxy 目标
$userJob = Start-Job -ScriptBlock {
    param($dir, $backendPort)
    $env:DEV_BACKEND_URL = "http://127.0.0.1:$backendPort"
    Set-Location $dir
    npx vue-cli-service serve --port 9081 2>&1
} -ArgumentList $USER_DIR, $Port

# bbs-admin-ui: VUE_APP_BBS_BASE_API 控制 API proxy, DEV_BACKEND_URL 控制静态资源 proxy
$adminJob = Start-Job -ScriptBlock {
    param($dir, $backendPort)
    $env:VUE_APP_BBS_BASE_API = "http://127.0.0.1:$backendPort"
    $env:DEV_BACKEND_URL = "http://127.0.0.1:$backendPort"
    Set-Location $dir
    npx vue-cli-service serve --port 9082 2>&1
} -ArgumentList $ADMIN_DIR, $Port

# 等待前端启动
Write-Host "  等待前端启动..."
$maxFrontendWait = 90
$waited = 0
$userReady = $false
$adminReady = $false
while ($waited -lt $maxFrontendWait) {
    Start-Sleep -Seconds 5
    $waited += 5
    if (-not $userReady) {
        try {
            $r = Invoke-WebRequest -Uri "http://localhost:$USER_PORT/bbs-user/" -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
            if ($r.StatusCode -eq 200) { $userReady = $true; Write-Host "  ✅ 用户前端已启动 ($waited s)" -ForegroundColor Green }
        } catch {}
    }
    if (-not $adminReady) {
        try {
            $r = Invoke-WebRequest -Uri "http://localhost:$ADMIN_PORT/bbs-admin/" -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
            if ($r.StatusCode -eq 200) { $adminReady = $true; Write-Host "  ✅ 管理前端已启动 ($waited s)" -ForegroundColor Green }
        } catch {}
    }
    if ($userReady -and $adminReady) { break }
}
if (-not $userReady -or -not $adminReady) {
    Write-Host "  ⚠️ 前端启动超时（用户=$userReady 管理=$adminReady）" -ForegroundColor Red
}

# 验证 proxy 指向正确端口
Write-Host "  验证 proxy → 测试后端 $Port ..."
try {
    $r = Invoke-WebRequest -Uri "http://localhost:$USER_PORT/bbs-server/common/saOrgTree" -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
    Write-Host "  ✅ 用户前端 proxy 正常 (status $($r.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "  ⚠️ 用户前端 proxy 异常: $($_.Exception.Message)" -ForegroundColor Yellow
}

# ========== 5. 运行 Playwright 测试 ==========
Write-Host "`n[5/7] 运行 Playwright E2E 测试 ..." -ForegroundColor Yellow

$env:E2E_USER_PORT = "$USER_PORT"
$env:E2E_ADMIN_PORT = "$ADMIN_PORT"

$testExitCode = 0
try {
    Set-Location $E2E_DIR
    npx playwright test --reporter=list 2>&1
    $testExitCode = $LASTEXITCODE
} finally {
    Remove-Item Env:\E2E_USER_PORT -ErrorAction SilentlyContinue
    Remove-Item Env:\E2E_ADMIN_PORT -ErrorAction SilentlyContinue
    Set-Location $PROJECT_ROOT
}

if ($testExitCode -eq 0) {
    Write-Host "`n  ✅ 全部测试通过" -ForegroundColor Green
} else {
    Write-Host "`n  ❌ 有测试失败 (exit code: $testExitCode)" -ForegroundColor Red
}

# ========== 6. 停止前端 ==========
if (-not $KeepFrontend) {
    Write-Host "`n[6/7] 停止前端 ..." -ForegroundColor Yellow
    Stop-Job $userJob -ErrorAction SilentlyContinue
    Remove-Job $userJob -ErrorAction SilentlyContinue
    Stop-Job $adminJob -ErrorAction SilentlyContinue
    Remove-Job $adminJob -ErrorAction SilentlyContinue
    # 也清理残留的 node 进程
    $remaining = Get-Process -Name node -ErrorAction SilentlyContinue
    foreach ($proc in $remaining) {
        try {
            $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId=$($proc.Id)" -ErrorAction SilentlyContinue).CommandLine
            if ($cmdLine -match "bbs-ui" -or $cmdLine -match "bbs-admin-ui") {
                Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            }
        } catch {}
    }
    Write-Host "  已停止" -ForegroundColor Green
} else {
    Write-Host "`n[6/7] 跳过前端停止（-KeepFrontend）" -ForegroundColor Yellow
}

# ========== 7. 停止后端 & 清理数据库 ==========
if (-not $KeepServer -and $backendJob) {
    Write-Host "`n[7/7] 停止测试后端 ..." -ForegroundColor Yellow
    Stop-Job $backendJob -ErrorAction SilentlyContinue
    Remove-Job $backendJob -ErrorAction SilentlyContinue
    Write-Host "  已停止"
} else {
    Write-Host "`n[7/7] 跳过后端停止" -ForegroundColor Yellow
}

if (-not $KeepDb) {
    Write-Host "  清理测试数据库 ..." -ForegroundColor Yellow
    Invoke-Expression "$MYSQL -e `"DROP DATABASE IF EXISTS $TEST_DB`""
    Write-Host "  已删除 $TEST_DB" -ForegroundColor Green
} else {
    Write-Host "  保留测试数据库（-KeepDb）" -ForegroundColor Yellow
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  完成！" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

exit $testExitCode
