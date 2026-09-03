# E2E 测试运行脚本
# 自动复制数据库 → 启动测试后端 → 运行 Playwright → 清理
# 用法: .\run-e2e.ps1 [-KeepDb] [-KeepServer] [-SkipCopy] [-Port 9084]

param(
    [int]$Port = 9084,         # 测试后端端口
    [switch]$KeepDb,           # 保留测试数据库（调试用）
    [switch]$KeepServer,       # 保留测试服务器（调试用）
    [switch]$SkipCopy          # 跳过数据库复制（服务器已在运行时用）
)

$ErrorActionPreference = "Stop"
$PROD_DB = "bbs_db"
$TEST_DB = "bbs_e2e_test"
$MYSQL = "mysql -u root -proot"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  大千智荟 BBS - E2E 测试" -ForegroundColor Cyan
Write-Host "  测试库: $TEST_DB | 端口: $Port" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ========== 1. 复制数据库 ==========
if (-not $SkipCopy) {
    Write-Host "`n[1/5] 复制数据库 $PROD_DB → $TEST_DB ..." -ForegroundColor Yellow

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
    Write-Host "`n[1/5] 跳过数据库复制" -ForegroundColor Yellow
}

# ========== 2. 启动测试后端 ==========
$backendJob = $null
if (-not $KeepServer) {
    Write-Host "`n[2/5] 启动测试后端 (端口 $Port, profile=e2e) ..." -ForegroundColor Yellow

    $backendJob = Start-Job -ScriptBlock {
        param($port)
        $env:SPRING_PROFILES_ACTIVE = "e2e"
        $env:BBS_SERVER_PORT = "$port"
        Set-Location "D:\workspace\20260522-20260615-daqianbbsfrontend\daqian-bbs\bbs-server"
        mvn spring-boot:run "-Dmaven.repo.local=./target/test-repo" -q 2>&1
    } -ArgumentList $Port

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
    Write-Host "`n[2/5] 跳过启动（服务器已在运行）" -ForegroundColor Yellow
}

# ========== 3. 运行 Playwright 测试 ==========
Write-Host "`n[3/5] 运行 Playwright E2E 测试 ..." -ForegroundColor Yellow

# 通过环境变量让 playwright.config.ts 使用测试端口
$env:E2E_USER_PORT = "$Port"
$env:E2E_ADMIN_PORT = "$Port"  # 管理端也用同一个后端

$testExitCode = 0
try {
    Set-Location "D:\workspace\20260522-20260615-daqianbbsfrontend\daqian-bbs\bbs-server\src\test\e2e"
    npx playwright test --reporter=list 2>&1
    $testExitCode = $LASTEXITCODE
} finally {
    Remove-Item Env:\E2E_USER_PORT -ErrorAction SilentlyContinue
    Remove-Item Env:\E2E_ADMIN_PORT -ErrorAction SilentlyContinue
    Set-Location "D:\workspace\20260522-20260615-daqianbbsfrontend\daqian-bbs"
}

if ($testExitCode -eq 0) {
    Write-Host "`n  ✅ 全部测试通过" -ForegroundColor Green
} else {
    Write-Host "`n  ❌ 有测试失败 (exit code: $testExitCode)" -ForegroundColor Red
}

# ========== 4. 停止后端 ==========
if (-not $KeepServer -and $backendJob) {
    Write-Host "`n[4/5] 停止测试后端 ..." -ForegroundColor Yellow
    Stop-Job $backendJob -ErrorAction SilentlyContinue
    Remove-Job $backendJob -ErrorAction SilentlyContinue
    Write-Host "  已停止"
} else {
    Write-Host "`n[4/5] 跳过停止" -ForegroundColor Yellow
}

# ========== 5. 清理数据库 ==========
if (-not $KeepDb) {
    Write-Host "`n[5/5] 清理测试数据库 ..." -ForegroundColor Yellow
    Invoke-Expression "$MYSQL -e `"DROP DATABASE IF EXISTS $TEST_DB`""
    Write-Host "  已删除 $TEST_DB" -ForegroundColor Green
} else {
    Write-Host "`n[5/5] 保留测试数据库（-KeepDb）" -ForegroundColor Yellow
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  完成！" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

exit $testExitCode
