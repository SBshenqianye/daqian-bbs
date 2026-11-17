# 轻量负向自检：故意制造“坏的”输入，确认判据会报错
# 只在只读接口上做，不写任何业务数据。
# 用法：
#   powershell -File scripts/dev/negative-smoke.ps1
#   powershell -File scripts/dev/negative-smoke.ps1 -ApiBase http://127.0.0.1:60001/bbs-server   # WSL2
param(
    [string]$ApiBase = 'http://127.0.0.1:9083/bbs-server',
    [int]$TimeoutSec = 10
)

. "$PSScriptRoot\_common.ps1"

$script:passed = 0
$script:failed = 0

function Record([bool]$Ok, [string]$Name, [string]$Detail) {
    if ($Ok) {
        Write-Ok $Name
        $script:passed++
    } else {
        Write-Fail ("{0} - {1}" -f $Name, $Detail)
        $script:failed++
    }
}

function Assert-Equal([object]$Expected, [object]$Actual, [string]$Label) {
    if ($Expected -ne $Actual) {
        throw ("断言不一致：{0}（expected={1} actual={2}）" -f $Label, $Expected, $Actual)
    }
}

Write-Step "负向自检（API: $ApiBase）"
Write-Host '说明：这些是“注入检查”，失败数不计入业务验收，单独统计。' -ForegroundColor DarkGray

# 1) 基线：正常接口必须可用
$baseline = $null
try { $baseline = Invoke-RestMethod -Uri "$ApiBase/common/featured/notices" -Method Get -TimeoutSec $TimeoutSec } catch { }
$baselineOk = ($null -ne $baseline -and $baseline.code -eq 200)
Record $baselineOk '1/4 基线：公开接口可访问且业务码 200' ("无法访问 {0}/common/featured/notices" -f $ApiBase)
if (-not $baselineOk) {
    Write-Fail '基线不通过：先修环境（9083/60000 是否已启动），再做负向自检'
    exit 1
}

# 2) 错误的 baseUrl 必须被识别为连接失败
$connectFailed = $false
try {
    Invoke-RestMethod -Uri 'http://127.0.0.1:1/bbs-server/common/featured/notices' -TimeoutSec 3 | Out-Null
} catch {
    $connectFailed = $true
}
Record $connectFailed '2/4 错误 baseUrl 能被识别为失败' '错误地址竟然请求成功，判据不可靠'

# 3) 错误的断言必须被捕获
$mismatchDetected = $false
try {
    Assert-Equal '__MUST_NOT_EXIST__' $baseline.message 'injection'
} catch {
    $mismatchDetected = $true
}
Record $mismatchDetected '3/4 错误断言能被判据捕获' '断言没有报错，判据可能是装饰品'

# 4) 未登录访问业务接口必须被拒绝
$authRejected = $false
try {
    $r = Invoke-RestMethod -Uri "$ApiBase/notification/unreadSummary?userId=1" -TimeoutSec $TimeoutSec
    $authRejected = ($r.code -eq 401)
} catch {
    try { $authRejected = ([int]$_.Exception.Response.StatusCode -eq 401) } catch { $authRejected = $false }
}
Record $authRejected '4/4 未登录访问业务接口被拒绝' '未登录竟然可以访问 /notification/unreadSummary'

Write-Host ''
Write-Host ("[mutation] 注入检查 {0} 项：通过 {1}，失败 {2}" -f ($script:passed + $script:failed), $script:passed, $script:failed)
if ($script:failed -gt 0) {
    Write-Fail '负向自检未通过：判据需要先修好，再谈业务验收'
    exit 1
}
Write-Ok '负向自检通过：判据能在坏输入下变红'
exit 0
