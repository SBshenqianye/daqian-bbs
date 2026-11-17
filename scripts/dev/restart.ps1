# 重启开发服务（先按端口杀，再启动）
# 用法：
#   powershell -File scripts/dev/restart.ps1
#   powershell -File scripts/dev/restart.ps1 -Target server
param(
    [ValidateSet('all', 'server', 'ui', 'admin')]
    [string]$Target = 'all'
)

. "$PSScriptRoot\_common.ps1"

Write-Step "重启：$Target"
& "$PSScriptRoot\down.ps1" -Target $Target
if (-not $?) { exit 1 }

switch ($Target) {
    'all'    { & "$PSScriptRoot\up.ps1" }
    'server' { & "$PSScriptRoot\up.ps1" -SkipUi -SkipAdmin }
    'ui'     { & "$PSScriptRoot\up.ps1" -SkipServer -SkipAdmin }
    'admin'  { & "$PSScriptRoot\up.ps1" -SkipServer -SkipUi }
}
exit 0

