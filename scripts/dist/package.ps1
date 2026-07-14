<#
.SYNOPSIS
    BBS 离线打包脚本 (Windows PowerShell 版)
.DESCRIPTION
    将构建产物打包为可分发的 tar.gz 文件，与 Linux 版 package.sh 完全一致。
    前置条件: 先运行 scripts/build/build.sh 或 scripts/build/build.sh --native
.PARAMETER WithSource
    打包构建产物 + 源代码
.PARAMETER Minimal
    仅打包运行所需最小文件
.EXAMPLE
    .\scripts\dist\package.ps1
    .\scripts\dist\package.ps1 -WithSource
    .\scripts\dist\package.ps1 -Minimal
#>

param(
    [switch]$WithSource,
    [switch]$Minimal
)

$ErrorActionPreference = "Stop"

# --------------- 路径 ---------------
$ROOT_DIR = Resolve-Path "$PSScriptRoot/../.."
$VERSION = Get-Date -Format "yyyyMMdd-HHmmss"
$OUTPUT_DIR = "$ROOT_DIR/bbs-deploy"
$PACKAGE_NAME = "bbs-deploy-$VERSION.tar.gz"

# --------------- 颜色输出 ---------------
function Write-Info  { Write-Host "[INFO]" -ForegroundColor Cyan -NoNewline; Write-Host " $args" }
function Write-Ok   { Write-Host "[OK]" -ForegroundColor Green -NoNewline; Write-Host " $args" }
function Write-Warn { Write-Host "[WARN]" -ForegroundColor Yellow -NoNewline; Write-Host " $args" }
function Write-Err  { Write-Host "[ERR]" -ForegroundColor Red -NoNewline; Write-Host " $args" }

# --------------- 检查构建产物 ---------------
function Check-Artifacts {
    Write-Progress -Activity "检查构建产物" -Status "bbs-ui/dist" -PercentComplete 33
    $missing = @()
    if (-not (Test-Path "$ROOT_DIR/bbs-ui/dist"))   { $missing += "bbs-ui/dist" }

    Write-Progress -Activity "检查构建产物" -Status "bbs-admin-ui/dist" -PercentComplete 66
    if (-not (Test-Path "$ROOT_DIR/bbs-admin-ui/dist")) { $missing += "bbs-admin-ui/dist" }

    Write-Progress -Activity "检查构建产物" -Status "bbs-server.jar" -PercentComplete 100
    if (-not (Test-Path "$ROOT_DIR/bbs-server/target/bbs-server.jar")) { $missing += "bbs-server/target/bbs-server.jar" }

    Write-Progress -Activity "检查构建产物" -Completed

    if ($missing.Count -gt 0) {
        Write-Warn "部分构建产物缺失:"
        $missing | ForEach-Object { Write-Host "  - $_" }
        Write-Host ""
        Write-Warn "请先运行: bash scripts/build/build.sh --native"
        Write-Host "  或在 wsl 中执行，或在 Linux 构建机上运行"
        Write-Host ""
        $answer = Read-Host "是否继续打包？(y/N)"
        if ($answer -ne "y") { exit 1 }
    }
}

# --------------- 准备打包目录 ---------------
function Prepare-Package {
    Write-Info "创建打包目录: $OUTPUT_DIR"
    if (Test-Path $OUTPUT_DIR) { Remove-Item -Recurse -Force $OUTPUT_DIR }
    New-Item -ItemType Directory -Path $OUTPUT_DIR -Force | Out-Null
}

# --------------- 标准打包 ---------------
function Package-Standard {
    Write-Info "===== 打包构建产物（标准模式） ====="
    $steps = @(
        @{Name="bbs-ui/dist";      Path="$ROOT_DIR/bbs-ui/dist";             Dest="$OUTPUT_DIR/bbs-ui";          IsDir=$true},
        @{Name="bbs-admin-ui/dist";Path="$ROOT_DIR/bbs-admin-ui/dist";       Dest="$OUTPUT_DIR/bbs-admin-ui";    IsDir=$true},
        @{Name="bbs-server.jar";   Path="$ROOT_DIR/bbs-server/target/bbs-server.jar"; Dest="$OUTPUT_DIR/";      IsDir=$false},
        @{Name="Nginx 配置";        Path="$ROOT_DIR/nginx/nginx.conf.template"; Dest="$OUTPUT_DIR/nginx/";        IsDir=$false},
        @{Name="Nginx Dockerfile";  Path="$ROOT_DIR/nginx/Dockerfile";        Dest="$OUTPUT_DIR/nginx/";          IsDir=$false},
        @{Name="数据库初始化 SQL";   Path="$ROOT_DIR/bbs-server/src/main/resources/db/init/init-pg.sql"; Dest="$OUTPUT_DIR/db/init/"; IsDir=$false}
    )

    $total = $steps.Count
    for ($i = 0; $i -lt $total; $i++) {
        $pct = [math]::Round(($i + 1) / $total * 100)
        Write-Progress -Activity "打包构建产物（标准模式）" -Status $steps[$i].Name -PercentComplete $pct

        $destDir = if ($steps[$i].IsDir) { $steps[$i].Dest } else { Split-Path $steps[$i].Dest -Parent }
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null

        if ($steps[$i].IsDir) {
            if (Test-Path $steps[$i].Path) {
                Copy-Item -Recurse $steps[$i].Path $steps[$i].Dest
            }
        } else {
            if (Test-Path $steps[$i].Path) {
                Copy-Item $steps[$i].Path $steps[$i].Dest
            }
        }
    }

    # 文档
    if (Test-Path "$ROOT_DIR/DEPLOY.md") {
        Copy-Item "$ROOT_DIR/DEPLOY.md" "$OUTPUT_DIR/"
    }

    Write-Progress -Activity "打包构建产物（标准模式）" -Completed

    # README
    @"
========================================
 BBS 部署包
========================================

部署步骤:

  1. 复制环境变量配置:
     cp scripts/.env.example .env
     vi .env         # 修改数据库密码等配置

  2. 部署后端 (需要 JDK 8+):
     nohup java -jar bbs-server.jar --server.port=9083 &

  3. 配置 Nginx:
     将 bbs-ui/dist 和 bbs-admin-ui/dist 部署到 Nginx html 目录
     参考 nginx/nginx.conf.template 配置反向代理

  4. 访问:
     用户前端:  http://<服务器IP>:19848/bbs-user/
     管理后台:  http://<服务器IP>:19848/bbs-admin/

详细文档请参见 DEPLOY.md
========================================
"@ | Out-File -FilePath "$OUTPUT_DIR/README.txt" -Encoding ASCII
}

# --------------- 最小打包 ---------------
function Package-Minimal {
    Write-Info "===== 打包最小运行文件 ====="

    $minimalSteps = @(
        @{Name="bbs-server.jar";     Path="$ROOT_DIR/bbs-server/target/bbs-server.jar";   Dest="$OUTPUT_DIR/";           Type="file"},
        @{Name="bbs-ui/dist";        Path="$ROOT_DIR/bbs-ui/dist/*";                      Dest="$OUTPUT_DIR/bbs-ui/";    Type="dir"},
        @{Name="bbs-admin-ui/dist";  Path="$ROOT_DIR/bbs-admin-ui/dist/*";                Dest="$OUTPUT_DIR/bbs-admin-ui/"; Type="dir"},
        @{Name="Nginx 配置";          Path="$ROOT_DIR/nginx/nginx.conf.template";         Dest="$OUTPUT_DIR/nginx/";    Type="file"},
        @{Name="数据库初始化 SQL";    Path="$ROOT_DIR/bbs-server/src/main/resources/db/init/init-pg.sql"; Dest="$OUTPUT_DIR/db/init/"; Type="file"},
        @{Name="环境变量模板";        Path="$ROOT_DIR/scripts/.env.example";               Dest="$OUTPUT_DIR/";          Type="file"}
    )

    for ($i = 0; $i -lt $minimalSteps.Count; $i++) {
        $pct = [math]::Round(($i + 1) / $minimalSteps.Count * 100)
        Write-Progress -Activity "打包最小运行文件" -Status $minimalSteps[$i].Name -PercentComplete $pct

        $destDir = Split-Path $minimalSteps[$i].Dest -Parent
        if ($destDir -eq $minimalSteps[$i].Dest) { $destDir = $minimalSteps[$i].Dest }
        New-Item -ItemType Directory -Path $minimalSteps[$i].Dest -Force | Out-Null

        if ($minimalSteps[$i].Type -eq "dir") {
            Copy-Item -Recurse $minimalSteps[$i].Path $minimalSteps[$i].Dest
        } else {
            Copy-Item $minimalSteps[$i].Path $minimalSteps[$i].Dest
        }
    }
    Write-Progress -Activity "打包最小运行文件" -Completed
}

# --------------- 含源代码打包 ---------------
function Package-WithSource {
    Write-Info "===== 打包构建产物 + 源代码 ====="

    Package-Standard

    Write-Progress -Activity "打包源代码" -Status "bbs-server/src" -PercentComplete 25
    New-Item -ItemType Directory -Path "$OUTPUT_DIR/source" -Force | Out-Null
    Copy-Item -Recurse "$ROOT_DIR/bbs-server/src" "$OUTPUT_DIR/source/bbs-server/src"
    Copy-Item "$ROOT_DIR/bbs-server/pom.xml" "$OUTPUT_DIR/source/bbs-server/"

    Write-Progress -Activity "打包源代码" -Status "bbs-ui/src" -PercentComplete 50
    if (Test-Path "$ROOT_DIR/bbs-ui/src") {
        Copy-Item -Recurse "$ROOT_DIR/bbs-ui/src" "$OUTPUT_DIR/source/bbs-ui/src"
    }
    if (Test-Path "$ROOT_DIR/bbs-ui/package.json") {
        Copy-Item "$ROOT_DIR/bbs-ui/package.json" "$OUTPUT_DIR/source/bbs-ui/"
    }

    Write-Progress -Activity "打包源代码" -Status "bbs-admin-ui/src" -PercentComplete 75
    if (Test-Path "$ROOT_DIR/bbs-admin-ui/src") {
        Copy-Item -Recurse "$ROOT_DIR/bbs-admin-ui/src" "$OUTPUT_DIR/source/bbs-admin-ui/src"
    }
    if (Test-Path "$ROOT_DIR/bbs-admin-ui/package.json") {
        Copy-Item "$ROOT_DIR/bbs-admin-ui/package.json" "$OUTPUT_DIR/source/bbs-admin-ui/"
    }

    Write-Progress -Activity "打包源代码" -Status "Nginx 配置" -PercentComplete 95
    if (Test-Path "$ROOT_DIR/nginx") {
        Copy-Item -Recurse "$ROOT_DIR/nginx" "$OUTPUT_DIR/source/nginx/"
    }

    Write-Progress -Activity "打包源代码" -Completed
}

# --------------- 创建压缩包 ---------------
function New-Tarball {
    Write-Info "===== 创建压缩包 ====="

    Push-Location $ROOT_DIR
    try {
        # Windows 10+ 内置 tar.exe，支持 -czf
        $parentDir = Split-Path $OUTPUT_DIR -Parent
        $dirName   = Split-Path $OUTPUT_DIR -Leaf

        Write-Progress -Activity "创建压缩包" -Status "正在压缩 ${dirName}..." -PercentComplete 50
        tar -czf "$PACKAGE_NAME" -C "$parentDir" "$dirName"
        Write-Progress -Activity "创建压缩包" -Completed

        $size = (Get-Item "$PACKAGE_NAME").Length
        if ($size -gt 1MB) {
            $sizeStr = "{0:N1} MB" -f ($size / 1MB)
        } else {
            $sizeStr = "{0:N0} KB" -f ($size / 1KB)
        }
        Write-Ok "打包完成: $ROOT_DIR/$PACKAGE_NAME ($sizeStr)"

        Write-Host ""
        Write-Info "分发文件:"
        Write-Host "  $ROOT_DIR/$PACKAGE_NAME"
        Write-Host ""
        Write-Info "在目标服务器上解压:"
        Write-Host "  tar -xzf $PACKAGE_NAME"
        Write-Host "  cd $dirName"
        Write-Host "  # 按 README.txt 步骤部署"
    }
    finally {
        Pop-Location
    }
}

# --------------- 清理 ---------------
function Cleanup-Output {
    Write-Info "清理临时打包目录..."
    if (Test-Path $OUTPUT_DIR) { Remove-Item -Recurse -Force $OUTPUT_DIR }
}

# --------------- 主流程 ---------------
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " BBS 离线打包 v$VERSION" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

Write-Progress -Activity "BBS 打包 v$VERSION" -Status "检查构建产物" -PercentComplete 5
Check-Artifacts

Write-Progress -Activity "BBS 打包 v$VERSION" -Status "准备打包目录" -PercentComplete 10
Prepare-Package

Write-Progress -Activity "BBS 打包 v$VERSION" -Status "打包文件" -PercentComplete 20
if ($WithSource) {
    Package-WithSource
} elseif ($Minimal) {
    Package-Minimal
} else {
    Package-Standard
}

Write-Progress -Activity "BBS 打包 v$VERSION" -Status "创建压缩包" -PercentComplete 80
New-Tarball

Write-Progress -Activity "BBS 打包 v$VERSION" -Status "清理临时文件" -PercentComplete 95
Cleanup-Output

Write-Progress -Activity "BBS 打包 v$VERSION" -Completed

Write-Host ""
Write-Ok "========================================"
Write-Ok "打包成功！分发文件:"
Write-Ok "  $ROOT_DIR/$PACKAGE_NAME"
Write-Host ""
Write-Host "将此文件传输到目标服务器即可部署"
Write-Host "========================================"
