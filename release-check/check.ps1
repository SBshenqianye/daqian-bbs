#requires -Version 5.1
<#
  版主角色化 / v039 发布检查脚本（离线可用）
  默认面向 PostgreSQL；也支持 MySQL（-DbType MySql）。

  依赖：PowerShell 5.1 + psql（PG）或 mysql（MySQL）。
  说明：脚本只执行只读查询；回滚 SQL 需手工确认后单独执行。

  用法（PG，最常见）：
    # 部署前
    .\check.ps1 -Mode Pre -DbName bbs_db -DbUser postgres
    # 部署后（含 API 检查）
    .\check.ps1 -Mode Post -DbName bbs_db -DbUser postgres `
      -BaseUrl "http://127.0.0.1:8080/bbs-server" -SuperUser SUPER1 -SuperPassword "***"

  如果本机没有 psql，可在装有 psql 的机器上运行，或用 -PgContainer 走本地容器（仅开发环境）。
#>
[CmdletBinding()]
param(
    [ValidateSet('Pre', 'Post', 'Api')]
    [string]$Mode = 'Pre',

    [ValidateSet('Pg', 'MySql')]
    [string]$DbType = 'Pg',

    # 数据库
    [string]$DbHost = '127.0.0.1',
    [int]$DbPort = 5432,
    [string]$DbUser = 'postgres',
    [string]$DbPassword = '',
    [string]$DbName = 'bbs_db',

    # 客户端可执行文件（PG 默认 psql；MySQL 默认 mysql）
    [string]$PsqlExe = 'psql',
    [string]$MySqlExe = 'mysql',

    # 可选：本地开发用，通过 WSL 里的 podman 容器执行 psql（生产不要用）
    [string]$PgContainer = '',

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

# 默认端口/用户随数据库类型调整（显式传参时以传参为准）
if (-not $PSBoundParameters.ContainsKey('DbPort')) {
    $DbPort = if ($DbType -eq 'Pg') { 5432 } else { 3306 }
}
if (-not $PSBoundParameters.ContainsKey('DbUser')) {
    $DbUser = if ($DbType -eq 'Pg') { 'postgres' } else { 'root' }
}

# ---- 编码兼容：脚本以 UTF-8 BOM 保存；运行时统一 UTF-8（无 BOM），兼容 GBK 控制台 ----
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
try { [Console]::OutputEncoding = $utf8NoBom } catch { }
try { $OutputEncoding = $utf8NoBom } catch { }
try { [Console]::InputEncoding = $utf8NoBom } catch { }
$env:PGCLIENTENCODING = 'UTF8'

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$OutDir = Join-Path $Root 'out'
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir | Out-Null }

$SqlDir = Join-Path $Root ("sql\{0}" -f $(if ($DbType -eq 'Pg') { 'pg' } else { 'mysql' }))

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

# ---------------------------------------------------------------- 数据库
function Invoke-DbSqlFile([string]$SqlFile) {
    if (-not (Test-Path $SqlFile)) { throw "找不到 SQL 文件：$SqlFile" }

    # 显式按 UTF-8 读取，避免 GBK 系统把中文 SQL 读坏；顺手去掉可能的 BOM
    $sql = Get-Content -Raw -Path $SqlFile -Encoding UTF8
    $sql = $sql.TrimStart([char]0xFEFF)
    $previousPg = $env:PGPASSWORD
    $previousMy = $env:MYSQL_PWD

    try {
        # 原生命令的 stderr（psql NOTICE、wsl 提示）不应该中断脚本；
        # 统一改为 Continue，执行后用 $LASTEXITCODE 判定成败。
        $previousEap = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'

        if ($DbType -eq 'Pg') {
            $env:PGCLIENTENCODING = 'UTF8'
            if ($PgContainer) {
                $raw = $sql | & wsl -e podman exec -e PGCLIENTENCODING=UTF8 -i $PgContainer `
                    psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -A -t -F "`t" --no-psqlrc 2>&1
            } else {
                if ($DbPassword) { $env:PGPASSWORD = $DbPassword }
                $raw = $sql | & $PsqlExe -h $DbHost -p $DbPort -U $DbUser -d $DbName `
                    -v ON_ERROR_STOP=1 -A -t -F "`t" --no-psqlrc 2>&1
            }
        } else {
            if ($DbPassword) { $env:MYSQL_PWD = $DbPassword }
            $raw = $sql | & $MySqlExe "--host=$DbHost" "--port=$DbPort" "--user=$DbUser" `
                "--database=$DbName" '--batch' '--skip-column-names' '--default-character-set=utf8mb4' 2>&1
        }

        $ErrorActionPreference = $previousEap

        if ($LASTEXITCODE -ne 0) {
            throw "数据库执行失败（exit=$LASTEXITCODE）：`n$($raw -join "`n")"
        }
    } finally {
        if ($null -eq $previousPg) { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }
        else { $env:PGPASSWORD = $previousPg }
        if ($null -eq $previousMy) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
        else { $env:MYSQL_PWD = $previousMy }
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

function Get-BaselinePath { Join-Path $OutDir ("precheck-baseline-{0}.json" -f $DbType.ToLower()) }

function Read-Baseline {
    $path = Get-BaselinePath
    if (-not (Test-Path $path)) { return $null }
    try { return Get-Content -Raw -Path $path -Encoding UTF8 | ConvertFrom-Json } catch { return $null }
}

function Show-DbMap($Map) {
    foreach ($key in ($Map.Keys | Sort-Object)) {
        Write-Host ("      {0} = {1}" -f $key, $Map[$key])
    }
}

function Invoke-PreCheck {
    Write-Section ("部署前：数据库只读检查（{0} / {1}）" -f $DbType, $DbName)
    $r = Invoke-DbSqlFile (Join-Path $SqlDir 'precheck.sql')
    Show-DbMap $r.Map

    $baseline = [ordered]@{
        savedAt       = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
        dbType        = $DbType
        database      = $DbName
        schemaVersion = $r.Map['schema_version']
        hasV039       = $r.Map['has_v039']
        hasV040       = $r.Map['has_v040']
        hasV041       = $r.Map['has_v041']
        totalUsers    = $r.Map['total_users']
        type1         = $r.Map['type_1']
        type2         = $r.Map['type_2']
        type3         = $r.Map['type_3']
        type4         = $r.Map['type_4']
        invalidType   = $r.Map['invalid_type']
        activeMods    = $r.Map['active_moderators']
        adminSuper    = $r.Map['admin_super']
        candidates    = $r.Map['candidates']
        commentHas4   = $r.Map['comment_has_4']
        candidateRows = @($r.Lines | Where-Object { $_ -like 'candidate_user:*' })
    }
    $baseline | ConvertTo-Json -Depth 4 | Set-Content -Path (Get-BaselinePath) -Encoding UTF8
    $stamped = Join-Path $OutDir ("precheck-{0}-{1}.json" -f $DbType.ToLower(), (Get-Date -Format 'yyyyMMdd-HHmmss'))
    Copy-Item (Get-BaselinePath) $stamped

    Assert-Equal "v039 尚未执行" '0' $r.Map['has_v039']
    Assert-Equal "v040 尚未执行" '0' $r.Map['has_v040']
    Assert-Equal "v041 尚未执行" '0' $r.Map['has_v041']
    Assert-Equal "v042 尚未执行" '0' $r.Map['has_v042']
    Assert-Equal "v043 尚未执行" '0' $r.Map['has_v043']
    Assert-Equal "用户主组字段尚未创建" '0' $r.Map['primary_group_column_exists']
    Assert-Equal "审计日志表尚未创建" '0' $r.Map['audit_table_exists']
    Assert-Equal "权限组表尚未创建" '0' $r.Map['permission_group_table_exists']
    Write-Host "      基线已保存：$stamped" -ForegroundColor Yellow
    Write-Host "      请确认 candidates（升级名单）符合业务预期：$($r.Map['candidates'])" -ForegroundColor Yellow
}

function Invoke-PostCheck {
    Write-Section ("部署后：数据库只读检查（{0} / {1}）" -f $DbType, $DbName)
    $r = Invoke-DbSqlFile (Join-Path $SqlDir 'postcheck.sql')
    Show-DbMap $r.Map

    Assert-Equal "v039 已记录" '1' $r.Map['has_v039']
    Assert-Equal "v040 已记录" '1' $r.Map['has_v040']
    Assert-Equal "v041 已记录" '1' $r.Map['has_v041']
    Assert-Equal "v042 已记录" '1' $r.Map['has_v042']
    Assert-Equal "v043 已记录" '1' $r.Map['has_v043']
    Assert-Equal "用户主组字段存在" '1' $r.Map['primary_group_column_exists']
    Assert-Equal "系统角色组数量为 4" '4' $r.Map['system_role_groups']
    Assert-Equal "不存在未回填主组的用户" '0' $r.Map['users_without_primary_group']
    Assert-Equal "审计日志表存在" '1' $r.Map['audit_table_exists']
    Assert-Equal "权限组表存在" '1' $r.Map['permission_group_table_exists']
    Assert-Equal "不存在非法 user_type" '0' $r.Map['invalid_type']
    Assert-Equal "没有遗漏的升级候选" '0' $r.Map['candidates_after']
    Assert-Equal "列注释包含 4：版主" '1' $r.Map['comment_has_4']

    $base = Read-Baseline
    if ($null -eq $base) {
        Test-Skip "与部署前基线的对比（未找到 out/precheck-baseline-$($DbType.ToLower()).json）"
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

# ---------------------------------------------------------------- API
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
        $text = $resp.Content
        try {
            if ($resp.RawContentStream) {
                $text = [System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
            }
        } catch { }
        return @{ Status = [int]$resp.StatusCode; Text = $text }
    } catch {
        $response = $_.Exception.Response
        if ($null -ne $response) {
            # 统一按 UTF-8 解码，避免 GBK 系统把中文响应解成乱码
            $stream = $response.GetResponseStream()
            $memory = New-Object System.IO.MemoryStream
            $stream.CopyTo($memory)
            $text = [System.Text.Encoding]::UTF8.GetString($memory.ToArray())
            $memory.Dispose()
            $stream.Close()
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
        Ok     = $true
        Status = $resp.Status
        Json   = $json
        User   = $json.obj.user
        Token  = [string]$json.obj.tokenHead + [string]$json.obj.token
    }
}

function Get-ApiCode($Response) {
    $json = Convert-JsonSafe $Response.Text
    if ($null -ne $json -and $null -ne $json.code) { return [int]$json.code }
    return [int]$Response.Status
}

# 后端把权限拒绝统一包装为 HTTP 200 + body.code=403
function Assert-ApiCode([string]$Name, $Expected, $Response) {
    $code = Get-ApiCode $Response
    if ("$Expected" -eq "$code") {
        Test-Pass $Name "code=$code (http=$($Response.Status))"
    } else {
        Test-Fail $Name "expected code=$Expected actual code=$code (http=$($Response.Status)) body=$($Response.Text)"
    }
}

function Invoke-ApiCheck {
    if (-not $BaseUrl) {
        Test-Skip "API 检查（未提供 -BaseUrl）"
        return
    }
    Write-Section "部署后：API 检查（$BaseUrl）"

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
    $logFile = Join-Path $OutDir ("check-{0}-{1}-{2}.log" -f $DbType.ToLower(), $Mode, (Get-Date -Format 'yyyyMMdd-HHmmss'))
    try { Start-Transcript -Path $logFile | Out-Null; $transcript = $logFile } catch { }

    Write-Host ("版主角色化 / v039 发布检查  模式={0}  数据库={1}  {2}" -f $Mode, $DbType, (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')) -ForegroundColor White

    # 数据库模式未提供密码时交互输入；容器/API 模式不需要
    if ($Mode -ne 'Api' -and -not $DbPassword -and -not $PgContainer -and $DbType -eq 'MySql' -and -not $env:MYSQL_PWD) {
        $secure = Read-Host "请输入 MySQL 密码（$DbUser@$DbHost）" -AsSecureString
        $DbPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
            [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
    }
    if ($Mode -ne 'Api' -and -not $DbPassword -and -not $PgContainer -and $DbType -eq 'Pg' -and -not $env:PGPASSWORD) {
        $secure = Read-Host "请输入 PostgreSQL 密码（$DbUser@$DbHost）" -AsSecureString
        $DbPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
            [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
    }

    switch ($Mode) {
        'Pre'  { Invoke-PreCheck }
        'Post' { Invoke-PostCheck; Invoke-ApiCheck }
        'Api'  { Invoke-ApiCheck }
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
