# 一键打包（构建 → 打包 → 产物验证 → 恢复开发栈）
#
# 背景：打包前需要停掉本地开发栈，否则 `mvn clean` 会与运行中的 devtools 抢 target/，
# 两个前端构建同时跑也容易 OOM。以前是手工停、手工再起，容易忘了把开发栈拉回来。
# 本脚本用 try/finally 保证：无论构建/打包成功与否，结束时都把 9081/9082/9083 拉起来。
#
# 用法：
#   powershell -File scripts/dev/package.ps1              # 全流程
#   powershell -File scripts/dev/package.ps1 -SkipBuild   # 复用已有产物，只打包（仍需停栈？不需要）
#   powershell -File scripts/dev/package.ps1 -NoRestart   # 打包后不拉起开发栈（CI 用）
param(
    [switch]$SkipBuild,
    [switch]$SkipPackage,
    [switch]$NoRestart
)

. "$PSScriptRoot\_common.ps1"

$root = Get-RepoRoot
$failed = $false
$artifact = $null

# Windows 路径 → WSL 路径（D:\a\b → /mnt/d/a/b）
function To-WslPath([string]$Path) {
    $normalized = $Path -replace '\\', '/'
    if ($normalized -match '^([A-Za-z]):(.*)$') {
        return "/mnt/$($matches[1].ToLower())$($matches[2])"
    }
    return $normalized
}

$wslRoot = To-WslPath $root
$stackWasRunning = (Test-PortListening 9081) -or (Test-PortListening 9082) -or (Test-PortListening 9083)

try {
    if (-not $SkipBuild) {
        Write-Step '1/5 停止本地开发栈（避免 mvn clean 与 devtools 抢 target/）'
        & (Join-Path $PSScriptRoot 'down.ps1') | Out-Null
        Write-Ok '开发栈已停止'

        Write-Step '2/5 构建 bbs-ui（串行）'
        $env:NODE_OPTIONS = '--max-old-space-size=4096'
        Push-Location (Join-Path $root 'bbs-ui')
        npm run build
        $code = $LASTEXITCODE
        Pop-Location
        if ($code -ne 0) { throw 'bbs-ui 构建失败' }
        Write-Ok 'bbs-ui 构建通过'

        Write-Step '3/5 构建 bbs-admin-ui（串行）'
        Push-Location (Join-Path $root 'bbs-admin-ui')
        npm run build
        $code = $LASTEXITCODE
        Pop-Location
        if ($code -ne 0) { throw 'bbs-admin-ui 构建失败' }
        Write-Ok 'bbs-admin-ui 构建通过'

        Write-Step '4/5 后端打包（mvn clean package -DskipTests）'
        Push-Location (Join-Path $root 'bbs-server')
        mvn clean package -DskipTests
        $code = $LASTEXITCODE
        Pop-Location
        if ($code -ne 0) { throw '后端打包失败' }
        Write-Ok 'bbs-server.jar 生成'
    } else {
        Write-Step '1-4/5 跳过构建（-SkipBuild），复用现有产物'
    }

    if (-not $SkipPackage) {
        Write-Step '5/5 WSL 打包 + 产物验证'
        $pkgCmd = "cd '$wslRoot' && bash scripts/prod/package.sh 2>&1 | tail -6"
        wsl -e bash -lc $pkgCmd
        if ($LASTEXITCODE -ne 0) { throw 'package.sh 失败' }

        wsl -e bash -lc "cd '$wslRoot' && bash scripts/prod/verify-build.sh 2>&1 | tail -4"
        if ($LASTEXITCODE -ne 0) { throw 'verify-build.sh 未通过' }

        $artifact = Get-ChildItem (Join-Path $root 'dist') -Filter 'bbs-upgrade-*.tar.gz' |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($artifact) {
            $sha = Get-Content ($artifact.FullName + '.sha256') -ErrorAction SilentlyContinue
            Write-Ok ("产物: {0} ({1:N2} MB)" -f $artifact.FullName, ($artifact.Length / 1MB))
            if ($sha) { Write-Ok "SHA256: $sha" }
        }
    } else {
        Write-Step '5/5 跳过打包（-SkipPackage）'
    }
} catch {
    $failed = $true
    Write-Fail $_.Exception.Message
} finally {
    if (-not $NoRestart) {
        Write-Step '恢复本地开发栈（无论构建/打包结果如何）'
        & (Join-Path $PSScriptRoot 'up.ps1')
        if ($?) {
            Write-Ok '开发栈已恢复：9081 / 9082 / 9083'
        } else {
            Write-Fail '开发栈恢复失败，请手动执行 scripts/dev/up.ps1'
            $failed = $true
        }
    } elseif ($stackWasRunning) {
        Write-Host '[-NoRestart] 打包前开发栈是运行状态，现已停止；需要时执行 scripts/dev/up.ps1' -ForegroundColor Yellow
    }
}

if ($failed) { exit 1 }
Write-Ok '打包完成'
exit 0
