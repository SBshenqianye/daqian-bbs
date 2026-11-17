# 启动本地开发栈（后端 9083 / 用户端 9081 / 管理端 9082）
# 用法：
#   powershell -File scripts/dev/up.ps1
#   powershell -File scripts/dev/up.ps1 -SkipAdmin
param(
    [switch]$SkipServer,
    [switch]$SkipUi,
    [switch]$SkipAdmin,
    [int]$ServerTimeoutSec = 240,
    [int]$UiTimeoutSec = 180
)

. "$PSScriptRoot\_common.ps1"

$root = Get-RepoRoot
$logDir = Get-LogDir
$ports = Get-ServicePorts
$failed = $false

function Start-DevService([string]$Name, [int]$Port, [string]$WorkDir, [string]$Command) {
    if (Test-PortListening $Port) {
        Write-Host "[skip] $Name ($Port) 已在本机运行" -ForegroundColor DarkGray
        return
    }
    $log = Join-Path $logDir ("{0}.log" -f $Name)
    $bat = Join-Path $logDir ("{0}.bat" -f $Name)
    $content = "@echo off`r`ncd /d `"$WorkDir`"`r`n$Command > `"$log`" 2>&1`r`n"
    Set-Content -Path $bat -Value $content -Encoding ASCII
    Write-Step "启动 $Name（端口 $Port，日志 $log）"
    Start-Process -FilePath $bat -WindowStyle Hidden | Out-Null
}

if (-not $SkipServer) {
    Start-DevService -Name 'server' -Port $ports['server'] -WorkDir (Join-Path $root 'bbs-server') -Command 'mvn -q -DskipTests spring-boot:run'
}
if (-not $SkipUi) {
    Start-DevService -Name 'ui' -Port $ports['ui'] -WorkDir (Join-Path $root 'bbs-ui') -Command 'npm run dev'
}
if (-not $SkipAdmin) {
    Start-DevService -Name 'admin' -Port $ports['admin'] -WorkDir (Join-Path $root 'bbs-admin-ui') -Command 'npm run dev'
}

if (-not $SkipServer) {
    Write-Step "等待后端就绪（最长 $ServerTimeoutSec 秒）"
    if (-not (Wait-Port $ports['server'] $ServerTimeoutSec)) {
        Write-Fail "后端未在 $ServerTimeoutSec 秒内监听 $($ports['server'])"
        Show-LogTail (Join-Path $logDir 'server.log')
        $failed = $true
    } else {
        Write-Ok "后端就绪：http://127.0.0.1:$($ports['server'])/bbs-server/"
    }
}
if (-not $SkipUi) {
    Write-Step "等待用户端就绪（最长 $UiTimeoutSec 秒）"
    if (-not (Wait-Port $ports['ui'] $UiTimeoutSec)) {
        Write-Fail "用户端未在 $UiTimeoutSec 秒内监听 $($ports['ui'])"
        Show-LogTail (Join-Path $logDir 'ui.log')
        $failed = $true
    } else {
        Write-Ok "用户端：http://127.0.0.1:$($ports['ui'])/bbs-user/"
    }
}
if (-not $SkipAdmin) {
    Write-Step "等待管理端就绪（最长 $UiTimeoutSec 秒）"
    if (-not (Wait-Port $ports['admin'] $UiTimeoutSec)) {
        Write-Fail "管理端未在 $UiTimeoutSec 秒内监听 $($ports['admin'])"
        Show-LogTail (Join-Path $logDir 'admin.log')
        $failed = $true
    } else {
        Write-Ok "管理端：http://127.0.0.1:$($ports['admin'])/bbs-admin/"
    }
}

if ($failed) { exit 1 }
Write-Host ''
Write-Ok '本地开发栈已就绪（前端源码改动走 HMR；配置/依赖改动需 restart）'
