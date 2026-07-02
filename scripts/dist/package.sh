#!/bin/bash
# ============================================
# BBS 离线打包脚本
# 将构建产物打包为可分发的 tar.gz 文件
# 用法:
#   bash scripts/dist/package.sh                      # 容器离线包（默认）
#   bash scripts/dist/package.sh --native             # 原生部署包（JAR + 前端）
#   bash scripts/dist/package.sh --minimal            # 仅最小运行文件（原生）
#   bash scripts/dist/package.sh --with-source        # 含源代码（原生）
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-container}"  # container(默认) | native | minimal | with-source

# --------------- 颜色 ---------------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERR]${NC} $1"; }

# --------------- 进度指示库 ---------------
source "$ROOT_DIR/scripts/lib/progress.sh"

# --------------- 检测容器运行时 ---------------
detect_runner() {
    if command -v podman >/dev/null 2>&1; then
        echo "podman"
    elif command -v docker >/dev/null 2>&1; then
        echo "docker"
    else
        echo ""
    fi
}

# --------------- 检查构建产物（原生模式） ---------------
check_artifacts_native() {
    local missing=0
    if [ ! -d "bbs-ui/dist" ];           then warn "bbs-ui/dist 不存在";         missing=1; fi
    if [ ! -d "bbs-admin-ui/dist" ];     then warn "bbs-admin-ui/dist 不存在";   missing=1; fi
    if [ ! -f "bbs-server/target/bbs-server.jar" ]; then warn "bbs-server.jar 不存在"; missing=1; fi

    if [ "$missing" -ne 0 ]; then
        echo ""; warn "部分构建产物缺失，请先运行: bash scripts/build/build.sh --native"; echo ""
        read -p "是否继续打包？(y/N) " -n 1 -r; echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then exit 1; fi
    fi
}

# --------------- 检查构建产物（容器模式） ---------------
check_artifacts_container() {
    local RUNNER=$(detect_runner)
    if [ -z "$RUNNER" ]; then
        err "未找到 podman 或 docker，无法导出镜像"
        err "请先安装 Podman 或 Docker，或使用 --native 模式打包"
        exit 1
    fi
    info "使用容器引擎: $RUNNER"

    local missing=0
    for img in bbs-server:latest bbs-nginx:latest; do
        if $RUNNER image exists "$img" 2>/dev/null; then
            ok "镜像 $img 已存在"
        else
            err "镜像 $img 不存在"
            missing=1
        fi
    done
    if [ "$missing" -ne 0 ]; then
        err "缺少容器镜像，请先运行: bash scripts/build/build.sh"
        exit 1
    fi
}

# --------------- 容器离线分发包 ---------------
package_container() {
    local RUNNER=$(detect_runner)
    local OUTPUT_DIR="$ROOT_DIR/bbs"

    rm -rf "$OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"

    # 1. 导出镜像
    info "===== 导出容器镜像 ====="
    for img in bbs-server:latest bbs-nginx:latest; do
        local name="${img%%:*}"
        run_with_spinner "导出 ${img}" "$RUNNER" save -o "$OUTPUT_DIR/${name}.tar" "$img"
        local size
        size=$(du -h "$OUTPUT_DIR/${name}.tar" | cut -f1)
        ok "${name}.tar 导出完成 (${size})"
    done

    # 2. 部署脚本
    info "===== 复制部署脚本 ====="
    rm -rf "$OUTPUT_DIR/scripts"
    cp -r scripts "$OUTPUT_DIR/scripts"
    rm -f "$OUTPUT_DIR/scripts/.env.example"  # 单独处理

    # 3. Dockerfile（可选，方便内网重编）
    info "===== 复制 Dockerfile ====="
    mkdir -p "$OUTPUT_DIR/docker"
    cp nginx/Dockerfile             "$OUTPUT_DIR/docker/"
    cp bbs-server/Dockerfile        "$OUTPUT_DIR/docker/" 2>/dev/null || true

    # 4. 顶层一键部署脚本
    info "===== 生成一键部署脚本 ====="
    cat > "$OUTPUT_DIR/deploy.sh" << 'DEPLOYEOF'
#!/bin/bash
# ============================================
# BBS 一键部署（离线包入口）
# 用法:
#   cd /data/bbs && bash deploy.sh
# ============================================
set -e

RUNNER=""
if command -v podman >/dev/null 2>&1; then
    RUNNER=podman
elif command -v docker >/dev/null 2>&1; then
    RUNNER=docker
else
    echo "[ERR] 未找到 podman 或 docker"
    exit 1
fi
echo "[INFO] 使用容器引擎: $RUNNER"

# 1. 加载镜像
cd "$(dirname "$0")"
for img_file in bbs-server.tar bbs-nginx.tar; do
    img_name="${img_file%.tar}"
    if $RUNNER image exists "$img_name:latest" 2>/dev/null; then
        echo "[OK] $img_name 镜像已存在，跳过加载"
    else
        echo "[INFO] 加载 $img_file ..."
        $RUNNER load -i "$img_file"
        echo "[OK] $img_file 加载完成"
    fi
done

# 2. 环境变量
if [ ! -f ".env" ]; then
    echo "[INFO] 创建 .env（使用默认配置）..."
    cp scripts/.env.example .env
    echo "[WARN] 请检查 .env 中的数据库密码等配置：vi .env"
    echo "      按 Enter 使用默认配置继续，或 Ctrl+C 修改后重试"
    read -r
fi

# 3. 确认 PostgreSQL 已就绪
echo ""
echo "============================================"
echo "  ⚠ 请确保 PostgreSQL 已启动并正常运行"
echo ""
echo "  BBS_DB_HOST 配置方式（按实际情况选一种）:"
echo ""
echo "  方案 A) PG 是容器且加入 bbs-net 网络:"
echo "     podman network connect bbs-net <PG容器名>"
echo "     然后 BBS_DB_HOST=<PG容器名>"
echo ""
echo "  方案 B) PG 是同宿主机原生服务:"
echo "     BBS_DB_HOST=host.docker.internal 或宿主机 IP"
echo ""
echo "  方案 C) PG 在远程服务器:"
echo "     BBS_DB_HOST=<远程服务器 IP>"
echo ""
echo "  PostgreSQL 需自行管理，脚本不会自动启动或关闭"
echo "  默认端口: 15432"
echo "============================================"
echo ""

# 4. 部署
echo "[INFO] 启动容器部署..."
bash scripts/deploy/container.sh
DEPLOYEOF
    chmod +x "$OUTPUT_DIR/deploy.sh"

    # 5. README 简化
    cat > "$OUTPUT_DIR/README.md" << 'READMEEOF'
# BBS 离线部署包（容器模式）

## 前置条件

目标服务器需安装 **Podman**（推荐）或 **Docker**，以及可访问的 **PostgreSQL** 数据库（默认端口 15432）。

## 一键部署

```bash
# 解压到 /data/bbs
sudo mkdir -p /data
sudo tar -xzf bbs-offline-*.tar.gz -C /data
cd /data/bbs
sudo bash deploy.sh
```

`deploy.sh` 会自动完成：加载镜像 → 初始化配置 → 启动容器。

> PostgreSQL 需自行管理，部署脚本不会自动启动数据库。
> 部署前请确保 PG 已运行并在 `.env` 中正确配置 `BBS_DB_HOST`。

## 自定义配置

如需修改数据库密码等，在运行 `deploy.sh` 前编辑 `.env`：

```bash
cp scripts/.env.example .env
vi .env        # 修改数据库密码等
bash deploy.sh
```

## 清理

```bash
bash scripts/ops/teardown.sh
```

## 目录结构

```
/data/bbs/
├── deploy.sh                # 一键部署入口
├── bbs-server.tar           # 后端镜像
├── bbs-nginx.tar            # Nginx 镜像（含前端静态文件）
├── scripts/
│   ├── deploy/
│   │   └── container.sh     # 容器部署脚本
│   ├── ops/
│   │   ├── teardown.sh      # 清理脚本
│   │   └── init-db.sh       # 数据库初始化
│   ├── lib/
│   │   └── progress.sh      # 进度指示库
│   └── .env.example         # 配置模板
├── docker/                  # Dockerfile（内网重编用）
└── README.md
```
READMEEOF
    ok "README.md 已生成"

    # 返回输出目录路径供上层使用
    echo "$OUTPUT_DIR"
}

# --------------- 原生部署包（标准模式） ---------------
package_native() {
    local OUTPUT_DIR="$ROOT_DIR/bbs-deploy"
    rm -rf "$OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"

    info "===== 原生部署包（标准） ====="

    # 前端 dist
    if [ -d "bbs-ui/dist" ]; then
        cp -r bbs-ui/dist "$OUTPUT_DIR/bbs-ui"
    fi
    if [ -d "bbs-admin-ui/dist" ]; then
        cp -r bbs-admin-ui/dist "$OUTPUT_DIR/bbs-admin-ui"
    fi

    # 后端 JAR
    if [ -f "bbs-server/target/bbs-server.jar" ]; then
        cp bbs-server/target/bbs-server.jar "$OUTPUT_DIR/"
    fi

    # Nginx 配置
    mkdir -p "$OUTPUT_DIR/nginx"
    cp nginx/nginx.conf.template "$OUTPUT_DIR/nginx/"
    cp nginx/Dockerfile "$OUTPUT_DIR/nginx/"

    # 数据库初始化 SQL
    mkdir -p "$OUTPUT_DIR/db/init"
    cp bbs-server/src/main/resources/db/init/init-pg.sql "$OUTPUT_DIR/db/init/"

    # 脚本
    rm -rf "$OUTPUT_DIR/scripts"
    cp -r scripts "$OUTPUT_DIR/scripts"
    rm -f "$OUTPUT_DIR/scripts/.env.example"  # 单独处理

    # 文档
    cp DEPLOY.md "$OUTPUT_DIR/" 2>/dev/null || true

    # README
    cat > "$OUTPUT_DIR/README.txt" <<'EOF'
========================================
 BBS 部署包
========================================

部署步骤:

  1. 复制环境变量配置:
     cp scripts/.env.example .env
     vi .env         # 修改数据库密码等配置

  2. 确保 PostgreSQL 已运行并可在 .env 中配置的地址访问

  3. 选择部署方式:

     方式 A - 容器部署（需要 Podman/Docker）:
       bash scripts/deploy/container.sh

     方式 B - 原生部署（RHEL 7 / CentOS 7）:
       bash scripts/deploy/native.sh

  4. 访问:
     用户前端:  http://<服务器IP>:19848/bbs-user/
     管理后台:  http://<服务器IP>:19848/bbs-admin/

  5. 清理:
     bash scripts/ops/teardown.sh

详细文档请参见 DEPLOY.md
========================================
EOF

    echo "$OUTPUT_DIR"
}

# --------------- 最小打包（仅运行所需） ---------------
package_minimal() {
    local OUTPUT_DIR="$ROOT_DIR/bbs-deploy"
    rm -rf "$OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"

    info "===== 最小运行文件包 ====="

    if [ -f "bbs-server/target/bbs-server.jar" ]; then
        cp bbs-server/target/bbs-server.jar "$OUTPUT_DIR/"
    fi
    if [ -d "bbs-ui/dist" ]; then
        mkdir -p "$OUTPUT_DIR/bbs-ui"
        cp -r bbs-ui/dist/* "$OUTPUT_DIR/bbs-ui/"
    fi
    if [ -d "bbs-admin-ui/dist" ]; then
        mkdir -p "$OUTPUT_DIR/bbs-admin-ui"
        cp -r bbs-admin-ui/dist/* "$OUTPUT_DIR/bbs-admin-ui/"
    fi

    mkdir -p "$OUTPUT_DIR/nginx"
    cp nginx/nginx.conf.template "$OUTPUT_DIR/nginx/"

    mkdir -p "$OUTPUT_DIR/db/init"
    cp bbs-server/src/main/resources/db/init/init-pg.sql "$OUTPUT_DIR/db/init/"

    cp scripts/deploy/native.sh "$OUTPUT_DIR/"
    cp scripts/.env.example "$OUTPUT_DIR/"

    echo "$OUTPUT_DIR"
}

# --------------- 含源代码打包 ---------------
package_with_source() {
    info "===== 构建产物 + 源代码包 ====="

    local OUTPUT_DIR
    OUTPUT_DIR=$(package_native)

    mkdir -p "$OUTPUT_DIR/source"
    cp -r bbs-server/src "$OUTPUT_DIR/source/bbs-server/src"
    cp bbs-server/pom.xml "$OUTPUT_DIR/source/bbs-server/"
    cp -r bbs-ui/src        "$OUTPUT_DIR/source/bbs-ui/src"        2>/dev/null || true
    cp bbs-ui/package.json  "$OUTPUT_DIR/source/bbs-ui/"           2>/dev/null || true
    cp -r bbs-admin-ui/src       "$OUTPUT_DIR/source/bbs-admin-ui/src"       2>/dev/null || true
    cp bbs-admin-ui/package.json "$OUTPUT_DIR/source/bbs-admin-ui/"          2>/dev/null || true
    cp -r nginx "$OUTPUT_DIR/source/nginx/" 2>/dev/null || true

    echo "$OUTPUT_DIR"
}

# --------------- 创建压缩包 ---------------
create_tarball() {
    local dir_name="$1"        # 压缩包内目录名
    local file_prefix="$2"     # 文件名前缀
    local OUTPUT_DIR="$ROOT_DIR/$dir_name"
    local VERSION=$(date +%Y%m%d-%H%M%S)
    local PACKAGE_NAME="${file_prefix}-${VERSION}.tar.gz"

    info "===== 创建压缩包 ====="

    run_with_spinner "正在压缩 $(basename "$OUTPUT_DIR")" \
        tar -czf "$PACKAGE_NAME" -C "$ROOT_DIR" "$(basename "$OUTPUT_DIR")"

    local size
    size=$(du -h "$PACKAGE_NAME" | cut -f1)
    ok "打包完成: $ROOT_DIR/$PACKAGE_NAME (${size})"

    # latest 软链接
    ln -sf "$PACKAGE_NAME" "$ROOT_DIR/${file_prefix}-latest.tar.gz"

    echo ""
    info "分发文件:"
    echo "  $ROOT_DIR/$PACKAGE_NAME"
    echo "  $ROOT_DIR/${file_prefix}-latest.tar.gz (软链接)"
    echo ""

    # 清理临时目录
    info "清理临时目录..."
    rm -rf "$OUTPUT_DIR"
}

# --------------- 主流程 ---------------
show_header "BBS 打包分发"

case "$MODE" in
    --native|native)
        show_step 1 3 "检查构建产物"
        check_artifacts_native
        show_step 2 3 "打包原生部署文件"
        package_native
        show_step 3 3 "创建压缩包"
        create_tarball "bbs-deploy" "bbs-deploy"
        ;;
    --minimal|minimal)
        show_step 1 3 "检查构建产物"
        check_artifacts_native
        show_step 2 3 "打包最小运行文件"
        package_minimal
        show_step 3 3 "创建压缩包"
        create_tarball "bbs-deploy" "bbs-deploy"
        ;;
    --with-source|with-source)
        show_step 1 3 "检查构建产物"
        check_artifacts_native
        show_step 2 3 "打包构建产物 + 源代码"
        package_with_source
        show_step 3 3 "创建压缩包"
        create_tarball "bbs-deploy" "bbs-deploy"
        ;;
    *)  # container（默认）
        show_step 1 3 "检查容器镜像"
        check_artifacts_container
        show_step 2 3 "打包容器离线文件"
        package_container
        show_step 3 3 "创建压缩包"
        create_tarball "bbs" "bbs-offline"
        ;;
esac

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║${NC}  打包成功！"
echo -e "${GREEN}║${NC}  输出文件: $(ls -t bbs-*.tar.gz 2>/dev/null | head -1)"
echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
