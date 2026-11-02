#requires -Version 5.1
<#
  版主角色化 / v039 发布检查脚本（离线可用，仅依赖 PowerShell 5.1 + mysql 客户端）
  详细步骤见同目录 README.md。

  用法示例：
    # 部署前：只读检查 + 保存基线
    .\check.ps1 -Mode Pre -MySqlExe "C:\mysql\bin\mysql.exe" -DbUser root -DbName bbs_db

    # 部署后：数据库检查（自动对比基线）+ API 检查
    .\check.ps1 -Mode Post -MySqlExe "C:\mysql\bin\mysql.exe" -DbUser root -DbName bbs_db `
      -BaseUrl "http://127.0.0.1:8080/bbs-server" -SuperUser SUPER1 -SuperPassword "***"

    # 只跑 API（不连数据库）
    .\check.ps1 -Mode Api -BaseUrl "http://127.0.0.1:8080/bbs-server" -SuperUser SUPER1 -SuperPassword "***"

  说明：
    - 脚本不会执行任何写 SQL；回滚 SQL 需要手工确认后单独执行。
    - 密码优先取参数，其次取环境变量 MYSQL_PWD；都没给时交互输入。
#>
[CmdletBinding()]
param(
    [ValidateSet('Pre', 'Post', 'Api')]
    [string]$Mode = 'Pre',

    # 数据库
    [string]$MySqlExe = 'mysql',
    [string]$DbHost = '127.0.0.1',
    [int]$DbPort = 3306,
    [string]$DbUser = 'root',
    [string]$DbPassword = '',
    [string]$DbName = 'bbs_db',

    # API（含 context-path，例如 http://127.0.0.1:8080/bbs-server）
    [string]$BaseUrl = '',

    # 账号（可选；不提供的用例会显示 SKIP）
    [string]$SuperUser = '',
    [string]$SuperPassword = '',
    [string]$AdminUser = '',
    [string]$AdminPassword = '',
    [string]$ModeratorUser = '',
    [string]$ModeratorPassword = '',
    [string]$NormalUser = '',
    [string]$NormalPassword = ''
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$OutDir = Join-Path $Root 'out'
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir | Out-Null }

$script:PassCount = 0
$script:FailCount = 0
$script:SkipCount = 0

function Write-Section([string]$Text) {
    Write-Host ''
    Write-Host "=== $Text ===" -ForegroundColor Cyan
}

function Test-Pass([string]$Name, [string]$Detail = '') {
    $script:PassCount++
    Write-Host ("PASS  {0}{1}" -f $Name, $(if ($Detail) { "  ($Detail)" } else { '' })) -ForegroundColor Green
}

function Test-Fail([string]$Name, [string]$Detail = '') {
    $script:FailCount++
    Write-Host ("FAIL  {0}{1}" -f $Name, $(if ($Detail) { "  ($Detail)" } else { '' })) -ForegroundColor Red
}

function Test-Skip([string]$Name) {
    $script:SkipCount++
    Write-Host ("SKIP  {0}" -f $Name) -ForegroundColor DarkGray
}

function Assert-Equal([string]$Name, $Expected, $Actual) {
    if ("$Expected" -eq "$Actual") { Test-Pass $Name "=$Actual" }
    else { Test-Fail $Name "expected=[$Expected] actual=[$Actual]" }
}

function Assert-Contains([string]$Name, [string]$Text, [string]$Part) {
    if ($Text -and $Text.Contains($Part)) { Test-Pass $Name }
    else { Test-Fail $Name "text=[$Text] 不包含 [$Part]" }
}

function Get-ApiCode($Response) {
    $json = Convert-JsonSafe $Response.Text
    if ($null -ne $json -and $null -ne $json.code) { return [int]$json.code }
    return [int]$Response.Status
}

# 注意：业务层 403 可能以 HTTP 200 + body.code=403 返回，必须看 body.code
function Assert-ApiCode([string]$Name, $Expected, $Response) {
    $code = Get-ApiCode $Response
    if ("$Expected" -eq "$code") {
        Test-Pass $Name "code=$code (http=$($Response.Status))"
    } else {
        Test-Fail $Name "expected code=$Expected actual code=$code (http=$($Response.Status)) body=$($Response.Text)"
    }
}

# ---------------------------------------------------------------- 数据库检查
function Invoke-DbSqlFile([string]$SqlFile) {
    if (-not (Test-Path $SqlFile)) { throw "找不到 SQL 文件：$SqlFile" }

    $previous = $env:MYSQL_PWD
    if ($DbPassword) { $env:MYSQL_PWD = $DbPassword }
    try {
        $sql = Get-Content -Raw -Path $SqlFile
        $raw = $sql | & $MySqlExe "--host=$DbHost" "--port=$DbPort" "--user=$DbUser" "--database=$DbName" '--batch' '--skip-column-names' 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "mysql 执行失败（exit=$LASTEXITCODE）：`n$($raw -join "`n")"
        }
    } finally {
        if ($null -eq $previous) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
        else { $env:MYSQL_PWD = $previous }
    }

    $map = @{}
    $lines = @()
    foreach ($line in $raw) {
        $text = [string]$line
        if ($text -match "`t") {
            $parts = $text -split "`t", 2
            $map[$parts[0]] = $parts[1]
        }
        $lines += $text
    }
    return @{ Map = $map; Lines = $lines }
}

function Get-BaselinePath { Join-Path $OutDir 'precheck-baseline.json' }

function Read-Baseline {
    $path = Get-BaselinePath
    if (-not (Test-Path $path)) { return $null }
    try { return Get-Content -Raw -Path $path | ConvertFrom-Json } catch { return $null }
}

function Show-DbMap($Map) {
    foreach ($key in ($Map.Keys | Sort-Object)) {
        Write-Host ("      {0} = {1}" -f $key, $Map[$key])
    }
}

function Invoke-PreCheck {
    Write-Section "部署前：数据库只读检查（$DbName@$DbHost`:$DbPort）"
    $r = Invoke-DbSqlFile (Join-Path $Root 'sql\precheck.sql')
    Show-DbMap $r.Map

    $baseline = [ordered]@{
        savedAt       = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
        database      = $DbName
        schemaVersion = $r.Map['schema_version']
        hasV039       = $r.Map['has_v039']
        totalUsers    = $r.Map['total_users']
        type1         = $r.Map['type_1']
        type2         = $r.Map['type_2']
        type3         = $r.Map['type_3']
        type4         = $r.Map['type_4']
        invalidType   = $r.Map['invalid_type']
        activeMods    = $r.Map['active_moderators']
        adminSuper    = $r.Map['admin_super']
        candidates    = $r.Map['candidates']
        comment       = $r.Map['comment']
        candidateRows = @($r.Lines | Where-Object { $_ -like 'candidate_user:*' })
    }
    $baseline | ConvertTo-Json -Depth 4 | Set-Content -Path (Get-BaselinePath) -Encoding UTF8
    $stamped = Join-Path $OutDir ("precheck-{0}.json" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
    Copy-Item (Get-BaselinePath) $stamped

    Assert-Equal "v039 尚未执行" '0' $r.Map['has_v039']
    Write-Host "      基线已保存：$stamped" -ForegroundColor Yellow
    Write-Host "      请确认 candidates（升级名单）符合业务预期：$($r.Map['candidates'])" -ForegroundColor Yellow
}

function Invoke-PostCheck {
    Write-Section "部署后：数据库只读检查（$DbName@$DbHost`:$DbPort）"
    $r = Invoke-DbSqlFile (Join-Path $Root 'sql\postcheck.sql')
    Show-DbMap $r.Map

    Assert-Equal "v039 已记录" '1' $r.Map['has_v039']
    Assert-Equal "不存在非法 user_type" '0' $r.Map['invalid_type']
    Assert-Equal "没有遗漏的升级候选" '0' $r.Map['candidates_after']
    Assert-Contains "列注释包含 4：版主" $r.Map['comment'] '4：版主'

    $base = Read-Baseline
    if ($null -eq $base) {
        Test-Skip "与部署前基线的对比（未找到 out/precheck-baseline.json）"
    } else {
        Assert-Equal "用户总数未变化" $base.totalUsers $r.Map['total_users']
        Assert-Equal "管理员数量未变化" $base.type2 $r.Map['type_2']
        Assert-Equal "超管数量未变化" $base.type3 $r.Map['type_3']
        Assert-Equal "ADMIN+SUPER 总数未变化" $base.adminSuper $r.Map['admin_super']

        $before4 = [int]$base.type4
        $after4 = [int]$r.Map['type_4']
        $candidates = [int]$base.candidates
        if ($after4 -ge ($before4 + $candidates)) {
            Test-Pass "版主数量增加符合预期" "before=$before4 candidates=$candidates after=$after4"
        } else {
            Test-Fail "版主数量增加不足" "before=$before4 candidates=$candidates after=$after4"
        }

        $before1 = [int]$base.type1
        $after1 = [int]$r.Map['type_1']
        $invalid = [int]$base.invalidType
        $expected1 = $before1 - $candidates + $invalid
        if ($after1 -eq $expected1) {
            Test-Pass "普通用户数量符合预期" "expected=$expected1 actual=$after1"
        } else {
            Test-Fail "普通用户数量异常" "expected=$expected1 actual=$after1（before=$before1 candidates=$candidates invalid=$invalid）"
        }
    }
}

# ---------------------------------------------------------------- API 检查
function Invoke-Http {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token = '',
        $Body = $null
    )
    $uri = $BaseUrl.TrimEnd('/') + $Path
    $headers = @{}
    if ($Token) { $headers['Authorization'] = $Token }

    try {
        $req = @{
            UseBasicParsing = $true
            Method          = $Method
            Uri             = $uri
            Headers         = $headers
            TimeoutSec      = 30
        }
        if ($null -ne $Body) {
            $req['ContentType'] = 'application/json'
            $req['Body'] = ($Body | ConvertTo-Json -Compress)
        }
        $resp = Invoke-WebRequest @req
        return @{ Status = [int]$resp.StatusCode; Text = $resp.Content }
    } catch {
        $response = $_.Exception.Response
        if ($null -ne $response) {
            $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
            $text = $reader.ReadToEnd()
            $reader.Close()
            return @{ Status = [int]$response.StatusCode; Text = $text }
        }
        throw
    }
}

function Convert-JsonSafe([string]$Text) {
    if (-not $Text) { return $null }
    try { return $Text | ConvertFrom-Json } catch { return $null }
}

function Invoke-Login([string]$User, [string]$Password) {
    $resp = Invoke-Http -Method 'POST' -Path '/common/login' -Body @{
        username = $User
        password = $Password
        channel  = '02'
    }
    $json = Convert-JsonSafe $resp.Text
    if ($resp.Status -ne 200 -or $null -eq $json -or $null -eq $json.obj) {
        return @{ Ok = $false; Status = $resp.Status; Json = $json; Text = $resp.Text }
    }
    return @{
        Ok    = $true
        Status = $resp.Status
        Json  = $json
        User  = $json.obj.user
        Token = [string]$json.obj.tokenHead + [string]$json.obj.token
    }
}

function Invoke-ApiCheck {
    if (-not $BaseUrl) {
        Test-Skip "API 检查（未提供 -BaseUrl）"
        return
    }
    Write-Section "部署后：API 检查（$BaseUrl）"

    # 1) 登录 + 会话
    if (-not $SuperUser) {
        Test-Skip "超级管理员登录与权限矩阵（未提供 -SuperUser）"
        return
    }
    $super = Invoke-Login $SuperUser $SuperPassword
    if (-not $super.Ok) {
        Test-Fail "超级管理员登录" "status=$($super.Status) text=$($super.Text)"
        return
    }
    Test-Pass "超级管理员登录" "role=$($super.User.role)"
    Assert-Equal "登录角色为 SUPER_ADMIN" 'SUPER_ADMIN' $super.User.role
    if ($super.User.permissions -contains 'permission:manage') {
        Test-Pass "登录返回 permission:manage"
    } else {
        Test-Fail "登录返回 permission:manage" "permissions=$($super.User.permissions -join ',')"
    }

    $info = Invoke-Http -Method 'GET' -Path '/common/user/info' -Token $super.Token
    $infoJson = Convert-JsonSafe $info.Text
    # 该接口直接返回用户对象（没有 code/obj 包装），兼容两种形态
    $infoUser = if ($infoJson -and $infoJson.obj) { $infoJson.obj } else { $infoJson }
    Assert-Equal "会话刷新接口 200" '200' $info.Status
    if ($infoUser -and $infoUser.role) {
        Assert-Equal "会话刷新返回角色" 'SUPER_ADMIN' $infoUser.role
        if ($null -ne $infoUser.permissions) { Test-Pass "会话刷新返回 permissions" } else { Test-Fail "会话刷新返回 permissions" }
        if ($null -ne $infoUser.boardScopes) { Test-Pass "会话刷新返回 boardScopes" } else { Test-Fail "会话刷新返回 boardScopes" }
    } else {
        Test-Fail "会话刷新返回结构" $info.Text
    }

    # 2) 权限矩阵
    $matrix = Invoke-Http -Method 'GET' -Path '/admin/permission/matrix' -Token $super.Token
    $matrixJson = Convert-JsonSafe $matrix.Text
    Assert-ApiCode "权限矩阵接口 200" 200 $matrix
    if ($matrixJson -and $matrixJson.obj) {
        $count = @($matrixJson.obj.endpoints).Count
        if ($count -gt 0) { Test-Pass "权限矩阵返回接口清单" "endpoints=$count" }
        else { Test-Fail "权限矩阵返回接口清单" "endpoints=0" }
        if (@($matrixJson.obj.roles).Count -eq 4) { Test-Pass "权限矩阵返回 4 个角色" }
        else { Test-Fail "权限矩阵返回 4 个角色" "roles=$(@($matrixJson.obj.roles).Count)" }
    } else {
        Test-Fail "权限矩阵返回结构" $matrix.Text
    }

    # 3) ADMIN（可选）
    if ($AdminUser) {
        $admin = Invoke-Login $AdminUser $AdminPassword
        if (-not $admin.Ok) {
            Test-Fail "管理员登录" "status=$($admin.Status)"
        } else {
            Test-Pass "管理员登录" "role=$($admin.User.role)"
            $adminMatrix = Invoke-Http -Method 'GET' -Path '/admin/permission/matrix' -Token $admin.Token
            Assert-ApiCode "ADMIN 访问权限矩阵 403" 403 $adminMatrix
        }
    } else {
        Test-Skip "ADMIN 权限矩阵 403（未提供 -AdminUser）"
    }

    # 4) MODERATOR（可选）
    if ($ModeratorUser) {
        $mod = Invoke-Login $ModeratorUser $ModeratorPassword
        if (-not $mod.Ok) {
            Test-Fail "版主登录" "status=$($mod.Status) text=$($mod.Text)"
        } else {
            Test-Pass "版主登录" "role=$($mod.User.role) boards=$(@($mod.User.boardScopes).Count)"

            $modMatrix = Invoke-Http -Method 'GET' -Path '/admin/permission/matrix' -Token $mod.Token
            Assert-ApiCode "MODERATOR 访问权限矩阵 403" 403 $modMatrix

            $article = Invoke-Http -Method 'POST' -Path '/admin/article/list' -Token $mod.Token -Body @{ page = 1; size = 10 }
            Assert-ApiCode "MODERATOR 业务列表 200" 200 $article

            $userSearch = Invoke-Http -Method 'GET' -Path '/admin/user/search?keyword=x' -Token $mod.Token
            Assert-ApiCode "MODERATOR 访问用户管理 403" 403 $userSearch
        }
    } else {
        Test-Skip "MODERATOR 权限边界（未提供 -ModeratorUser）"
    }

    # 5) 普通用户走 channel=02 必须失败（可选）
    if ($NormalUser) {
        $normal = Invoke-Login $NormalUser $NormalPassword
        if ($normal.Ok) {
            Test-Fail "普通用户 channel=02 登录应失败" "role=$($normal.User.role)"
        } else {
            Test-Pass "普通用户 channel=02 登录失败"
        }
    } else {
        Test-Skip "普通用户 channel=02 登录失败（未提供 -NormalUser）"
    }
}

# ---------------------------------------------------------------- main
$transcript = $null
try {
    $logFile = Join-Path $OutDir ("check-{0}-{1}.log" -f $Mode, (Get-Date -Format 'yyyyMMdd-HHmmss'))
    try { Start-Transcript -Path $logFile | Out-Null; $transcript = $logFile } catch { }

    Write-Host "版主角色化 / v039 发布检查  模式=$Mode  时间=$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White

    # 数据库模式未提供密码时交互输入（API 模式不需要）
    if ($Mode -ne 'Api' -and -not $DbPassword -and -not $env:MYSQL_PWD) {
        $secure = Read-Host "请输入 MySQL 密码（$DbUser@$DbHost）" -AsSecureString
        $DbPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
            [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
    }

    switch ($Mode) {
        'Pre' { Invoke-PreCheck }
        'Post' {
            Invoke-PostCheck
            Invoke-ApiCheck
        }
        'Api' { Invoke-ApiCheck }
    }

    Write-Section "结果"
    Write-Host ("PASS={0}  FAIL={1}  SKIP={2}" -f $script:PassCount, $script:FailCount, $script:SkipCount)
    if ($script:FailCount -gt 0) {
        Write-Host "存在失败项，请先按上面的提示排查，不要继续发布。" -ForegroundColor Red
        exit 1
    }
    Write-Host "检查通过。" -ForegroundColor Green
    if ($transcript) { Write-Host "日志已保存：$transcript" -ForegroundColor Yellow }
}
finally {
    try { Stop-Transcript | Out-Null } catch { }
}
