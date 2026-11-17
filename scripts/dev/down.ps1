# 按端口停止开发服务（默认三个都停）
# 用法：
#   powershell -File scripts/dev/down.ps1
#   powershell -File scripts/dev/down.ps1 -Target server
param(
    [ValidateSet('all', 'server', 'ui', 'admin')]
    [string]$Target = 'all'
)

. "$PSScriptRoot\_common.ps1"

$ports = Get-ServicePorts
$selected = @()
switch ($Target) {
    'all'    { $selected = @('server', 'ui', 'admin') }
    'server' { $selected = @('server') }
    'ui'     { $selected = @('ui') }
    'admin'  { $selected = @('admin') }
}

Write-Step "停止服务：$Target"
foreach ($name in $selected) {
    $port = $ports[$name]
    if (Stop-Port $port) {
        if (Wait-PortFree $port 20) {
            Write-Ok "$name ($port) 已停止"
        } else {
            Write-Fail "$name ($port) 仍在监听，请手动检查"
        }
    } else {
        Write-Host "[skip] $name ($port) 未在运行" -ForegroundColor DarkGray
    }
}
