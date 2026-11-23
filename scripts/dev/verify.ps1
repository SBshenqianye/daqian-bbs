# 提交前验收：串行构建两个前端 + 后端编译 + 重启后端 + 负向自检
# 用法：
#   powershell -File scripts/dev/verify.ps1
#   powershell -File scripts/dev/verify.ps1 -SkipBuild
param(
    [switch]$SkipBuild,
    [switch]$SkipNegative
)

. "$PSScriptRoot\_common.ps1"

$root = Get-RepoRoot

if (-not $SkipBuild) {
    Write-Step '1/5 前后端 HTTP 方法契约（tests/dev/api-method-contract.test.js）'
    node (Join-Path $root 'tests/dev/api-method-contract.test.js')
    if ($LASTEXITCODE -ne 0) { Write-Fail '前后端 HTTP 方法不一致（前端调用方式与控制器注解不匹配）'; exit 1 }
    Write-Ok '方法契约通过'

    Write-Step '2/5 后端编译（mvn -DskipTests compile）'
    Push-Location (Join-Path $root 'bbs-server')
    mvn -q -DskipTests compile
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { Write-Fail '后端编译失败'; exit 1 }
    Write-Ok '后端编译通过'

    # 两个前端必须串行构建：同时跑容易 OOM
    Write-Step '3/5 构建 bbs-ui（串行）'
    $env:NODE_OPTIONS = '--max-old-space-size=4096'
    Push-Location (Join-Path $root 'bbs-ui')
    npm run build
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { Write-Fail 'bbs-ui 构建失败'; exit 1 }
    Write-Ok 'bbs-ui 构建通过'

    Write-Step '4/5 构建 bbs-admin-ui（串行）'
    Push-Location (Join-Path $root 'bbs-admin-ui')
    npm run build
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { Write-Fail 'bbs-admin-ui 构建失败'; exit 1 }
    Write-Ok 'bbs-admin-ui 构建通过'
}

Write-Step '5/5 重启后端并跑负向自检'
& "$PSScriptRoot\restart.ps1" -Target server
if (-not $?) { Write-Fail '后端重启失败'; exit 1 }

if (-not $SkipNegative) {
    & "$PSScriptRoot\negative-smoke.ps1"
    if (-not $?) { Write-Fail '负向自检未通过'; exit 1 }
}

Write-Host ''
Write-Ok '提交前验收通过：构建 0 error + 后端就绪 + 负向自检通过'
exit 0

