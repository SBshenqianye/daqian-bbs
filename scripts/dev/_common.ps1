# 开发生命周期公共函数（PowerShell 5.1+）
# 端口约定：用户端 9081 / 管理端 9082 / 后端 9083
# WSL2（生产对齐）：nginx 60001 / 后端 60000 / PG 15432

$script:RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

function Get-RepoRoot { return $script:RepoRoot }

function Get-ServicePorts {
    return @{ server = 9083; ui = 9081; admin = 9082 }
}

function Test-PortListening([int]$Port) {
    $c = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return [bool]$c
}

function Stop-Port([int]$Port) {
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) { return $false }
    $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($p in $pids) {
        try {
            Stop-Process -Id $p -Force -ErrorAction Stop
        } catch {
            & taskkill /PID $p /T /F 2>$null | Out-Null
        }
    }
    return $true
}

function Wait-Port([int]$Port, [int]$TimeoutSec = 180) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortListening $Port) { return $true }
        Start-Sleep -Milliseconds 800
    }
    return $false
}

function Wait-PortFree([int]$Port, [int]$TimeoutSec = 20) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-PortListening $Port)) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return $false
}

function Write-Step([string]$Message) {
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Ok([string]$Message) {
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Fail([string]$Message) {
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Get-LogDir {
    $dir = Join-Path (Get-RepoRoot) 'tmp\run'
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    return $dir
}

function Show-LogTail([string]$Path, [int]$Lines = 20) {
    if (Test-Path $Path) {
        Write-Host "--- $Path (last $Lines) ---" -ForegroundColor DarkGray
        Get-Content $Path -Tail $Lines | ForEach-Object { Write-Host $_ }
        Write-Host '---' -ForegroundColor DarkGray
    }
}
