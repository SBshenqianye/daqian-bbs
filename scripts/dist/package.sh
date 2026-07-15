#!/bin/bash
# ============================================
# BBS 离线打包脚本
# 将构建产物打包为可分发的 tar.gz 文件
# 用法:
#   bash scripts/dist/package.sh                      # 轻量升级包（默认，输出到 dist/）
#   bash scripts/dist/package.sh --upgrade            # 轻量升级包
#   bash scripts/dist/package.sh --full              # 完整环境包（含基础镜像）
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-upgrade}"  # upgrade(默认) | full

# --------------- 颜色 ---------------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1" >&2; }
ok()    { echo -e "${GREEN}[OK]${NC} $1" >&2; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1" >&2; }
err()   { echo -e "${RED}[ERR]${NC} $1" >&2; }

# --------------- 进度指示库 ---------------
source "$ROOT_DIR/scripts/lib/progress.sh"

# --------------- 检查构建产物 ---------------
check_artifacts() {
    local missing=0
    if [ ! -d "bbs-ui/dist" ];           then warn "bbs-ui/dist 不存在";         missing=1; fi
    if [ ! -d "bbs-admin-ui/dist" ];     then warn "bbs-admin-ui/dist 不存在";   missing=1; fi
    if [ ! -f "bbs-server/target/bbs-server.jar" ]; then warn "bbs-server.jar 不存在"; missing=1; fi

    if [ "$missing" -ne 0 ]; then
        echo ""; warn "部分构建产物缺失，请先运行: bash scripts/build/build.sh --wsl"; echo ""
        read -p "是否继续打包？(y/N) " -n 1 -r; echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then exit 1; fi
    fi
}


# --------------- 创建压缩包（输出到 dist/） ---------------
create_tarball() {
    local dir_name="$1"        # 压缩包内目录名
    local file_prefix="$2"     # 文件名前缀
    local OUTPUT_DIR="$ROOT_DIR/tmp/$dir_name"
    local VERSION; VERSION=$(date +%Y%m%d-%H%M%S)
    local PACKAGE_NAME="${file_prefix}-${VERSION}.tar.gz"

    info "===== 创建压缩包 ====="

    # 输出到 dist/ 目录
    mkdir -p "$ROOT_DIR/dist"
    run_with_spinner "正在压缩 $(basename "$OUTPUT_DIR")" \
        tar -czf "$ROOT_DIR/dist/$PACKAGE_NAME" -C "$ROOT_DIR/tmp" "$dir_name"

    local size
    size=$(du -h "$ROOT_DIR/dist/$PACKAGE_NAME" | cut -f1)
    ok "打包完成: $ROOT_DIR/dist/$PACKAGE_NAME (${size})"

    echo ""
    info "分发文件:"
    echo "  $ROOT_DIR/dist/$PACKAGE_NAME"
    echo ""

    # 清理临时目录
    info "清理临时目录..."
    rm -rf "$OUTPUT_DIR"
}

# --------------- 轻量升级包（不含基础镜像） ---------------
package_upgrade() {
    local TIMESTAMP; TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    local DIR_NAME="upgrade-${TIMESTAMP}"
    local OUTPUT_DIR="$ROOT_DIR/tmp/$DIR_NAME"

    rm -rf "$OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"

    info "===== 轻量升级包（时间戳: $TIMESTAMP） ====="

    # 1. 复制构建产物
    if [ -f "bbs-server/target/bbs-server.jar" ]; then
        cp bbs-server/target/bbs-server.jar "$OUTPUT_DIR/"
    else
        warn "bbs-server.jar 不存在，跳过"
    fi

    if [ -d "bbs-ui/dist" ]; then
        mkdir -p "$OUTPUT_DIR/bbs-ui"
        cp -r bbs-ui/dist "$OUTPUT_DIR/bbs-ui/"
    else
        warn "bbs-ui/dist 不存在，跳过"
    fi

    if [ -d "bbs-admin-ui/dist" ]; then
        mkdir -p "$OUTPUT_DIR/bbs-admin-ui"
        cp -r bbs-admin-ui/dist "$OUTPUT_DIR/bbs-admin-ui/"
    else
        warn "bbs-admin-ui/dist 不存在，跳过"
    fi

    # 2. .env.example
    cp scripts/.env.example "$OUTPUT_DIR/" 2>/dev/null || true

    # 3. 生成 upgrade.sh（时间戳已硬编码）
    cat > "$OUTPUT_DIR/upgrade.sh" << UPGRADEEOF
#!/bin/bash
# ============================================
# BBS 升级脚本 — 由 package.sh 在 ${TIMESTAMP} 生成
# 创建版本快照 + 切换软链 + 重启容器
# 用法:
#   bash upgrade.sh [/data/bbs]
# ============================================
set -e
TIMESTAMP="${TIMESTAMP}"
BBS_HOME="\${1:-/data/bbs}"
SCRIPT_DIR="\$(cd "\$(dirname "\$0")" && pwd)"

RUNNER="podman"; command -v podman >/dev/null 2>&1 || RUNNER="docker"

info()  { echo -e "\033[0;36m[INFO]\033[0m \$*"; }
ok()    { echo -e "\033[0;32m[OK]\033[0m \$*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m \$*"; }

# 1. 创建版本目录
mkdir -p "\$BBS_HOME/versions/\$TIMESTAMP/bbs-ui" "\$BBS_HOME/versions/\$TIMESTAMP/bbs-admin-ui"

# 2. 复制新产物
info "复制构建产物..."
[ -f "\$SCRIPT_DIR/bbs-server.jar" ] && cp "\$SCRIPT_DIR/bbs-server.jar" "\$BBS_HOME/versions/\$TIMESTAMP/"
[ -d "\$SCRIPT_DIR/bbs-ui/dist" ] && cp -r "\$SCRIPT_DIR/bbs-ui/dist" "\$BBS_HOME/versions/\$TIMESTAMP/bbs-ui/"
[ -d "\$SCRIPT_DIR/bbs-admin-ui/dist" ] && cp -r "\$SCRIPT_DIR/bbs-admin-ui/dist" "\$BBS_HOME/versions/\$TIMESTAMP/bbs-admin-ui/"

# 3. 更新软链
ln -snf "\$TIMESTAMP" "\$BBS_HOME/versions/latest"
ok "已切换版本: \$TIMESTAMP"

# 4. 重启容器
info "重启容器..."
for container in bbs-server bbs-nginx; do
    if \$RUNNER container exists "\$container" 2>/dev/null; then
        \$RUNNER restart "\$container"
        ok "\$container 已重启"
    else
        warn "\$container 容器不存在，跳过"
    fi
done

echo ""
echo -e "\033[0;32m╔══════════════════════════════════════════════╗\033[0m"
echo -e "\033[0;32m║\033[0m  升级完成！版本: \$TIMESTAMP"
echo -e "\033[0;32m║\033[0m  回滚: cd \$BBS_HOME && ls versions/ && ln -snf versions/<旧版本> versions/latest && \$RUNNER restart bbs-server bbs-nginx"
echo -e "\033[0;32m╚══════════════════════════════════════════════╝\033[0m"
UPGRADEEOF
    chmod +x "$OUTPUT_DIR/upgrade.sh"
    ok "upgrade.sh 已生成"

    echo "$OUTPUT_DIR"
}

# --------------- 完整环境包（含基础镜像） ---------------
package_full() {
    local TIMESTAMP; TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    local DIR_NAME="bbs-full-${TIMESTAMP}"
    local OUTPUT_DIR="$ROOT_DIR/tmp/$DIR_NAME"

    rm -rf "$OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"

    info "===== 完整环境包（时间戳: $TIMESTAMP） ====="

    # 1. 基础镜像
    if [ -d "dist/base-images" ] && ls dist/base-images/*.tar 2>/dev/null | grep -q .; then
        mkdir -p "$OUTPUT_DIR/base"
        cp dist/base-images/*.tar "$OUTPUT_DIR/base/"
        ok "基础镜像已复制"
    else
        warn "dist/base-images/ 目录为空，请先运行 build/base.sh --save"
    fi

    # 2. 构建产物（复用 package_upgrade 的逻辑）
    local upgrade_dir
    upgrade_dir=$(package_upgrade)
    if [ -n "$upgrade_dir" ] && [ -d "$upgrade_dir" ]; then
        local ts_name; ts_name=$(basename "$upgrade_dir")
        mv "$upgrade_dir" "$OUTPUT_DIR/$ts_name"
        ok "构建产物已复制"
    fi

    # 3. deploy-offline.sh
    if [ -f "scripts/deploy/offline.sh" ]; then
        cp scripts/deploy/offline.sh "$OUTPUT_DIR/deploy-offline.sh"
        chmod +x "$OUTPUT_DIR/deploy-offline.sh"
        ok "deploy-offline.sh 已复制"
    else
        warn "scripts/deploy/offline.sh 不存在，跳过"
    fi

    # 4. .env.example
    cp scripts/.env.example "$OUTPUT_DIR/" 2>/dev/null || true

    # 5. README
    cat > "$OUTPUT_DIR/README.md" << READMEEOF
# BBS 完整环境包（bind-mount 模式）

## 目录结构

\`\`\`
.
├── deploy-offline.sh              # 安装/升级入口
├── base/                          # 基础镜像（首次部署用）
│   ├── bbs-server-base.tar
│   └── bbs-nginx-base.tar
├── upgrade-${TIMESTAMP}/     # 初始构建产物 + upgrade 脚本
│   ├── upgrade.sh
│   ├── bbs-server.jar
│   ├── bbs-ui/dist/
│   └── bbs-admin-ui/dist/
├── .env.example
└── README.md
\`\`\`

## 首次安装

\`\`\`bash
sudo mkdir -p /data
sudo tar -xzf bbs-full-*.tar.gz -C /data
cd /data/bbs-full-*
sudo bash deploy-offline.sh --install
\`\`\`

## 升级

将 upgrade tar 包传到服务器后：

\`\`\`bash
sudo bash /data/bbs/deploy-offline.sh --upgrade bbs-upgrade-*.tar.gz
\`\`\`

## 回滚

\`\`\`bash
cd /data/bbs && ls versions/
ln -snf versions/<旧版本> versions/latest
podman restart bbs-server bbs-nginx
\`\`\`
READMEEOF
    ok "README.md 已生成"

    echo "$OUTPUT_DIR"
}

# --------------- 主流程 ---------------
show_header "BBS 打包分发"

case "$MODE" in
    --upgrade|upgrade)
        show_step 1 2 "准备构建产物"
        check_artifacts
        show_step 2 2 "打包轻量升级包"
        up_dir=$(package_upgrade)
        create_tarball "$(basename "$up_dir")" "bbs-upgrade"
        ;;
    --full|full)
        show_step 1 2 "打包完整环境"
        sp_dir=$(package_full)
        show_step 2 2 "创建压缩包"
        create_tarball "$(basename "$sp_dir")" "bbs-full"
        ;;
esac

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║${NC}  打包成功！"
echo -e "${GREEN}║${NC}  输出文件: $(ls -t dist/bbs-*.tar.gz 2>/dev/null | head -1)"
echo -e "${GREEN}║${NC}  （输出目录: dist/）"
