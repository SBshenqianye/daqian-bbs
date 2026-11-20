# 编译桌面启动器（.NET Framework 4.0+，Win7 可用）
# 用法：powershell -File scripts/test/e2e-selenium/build-exe.ps1
param(
    [string]$Out = (Join-Path $PSScriptRoot 'target\DaqianBBS-E2E.exe')
)

$csc = 'C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe'
if (-not (Test-Path $csc)) { $csc = 'C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe' }
if (-not (Test-Path $csc)) { throw 'csc.exe 未找到：需要 .NET Framework 4.0+' }

$src = Join-Path $PSScriptRoot 'launcher\Launcher.cs'
if (-not (Test-Path $src)) { throw "缺少源文件：$src" }

$dir = Split-Path $Out -Parent
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }

& $csc /nologo /codepage:65001 /target:winexe /out:$Out `
    /reference:System.Windows.Forms.dll /reference:System.Drawing.dll $src

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Built: $Out"
