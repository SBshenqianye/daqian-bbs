#!/bin/bash
# ============================================
# BBS 基础镜像构建脚本
# 构建/保存/加载运行时基础镜像（不含应用代码）。
# 包含依赖变更检测（md5 checksum）和旧镜像归档。
#
# 用法:
#   bash scripts/build/base.sh                # 检测 + 构建（如需）
#   bash scripts/build/base.sh --save         # 构建 + 导出 tar
#   bash scripts/build/base.sh --load         # 从 tar 加载到本地
#   bash scripts/build/base.sh --force-rebuild # 强制重建所有
#   bash scripts/build/base.sh --check        # 只检测变更状态
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-build}"  # build | save | load | force-rebuild | check

# --------------- 颜色 ---------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1" >&2; }
ok()    { echo -e "${GREEN}[OK]${NC} $1" >&2; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1" >&2; }
err()   { echo -e "${RED}[ERR]${NC} $1" >&2; }

# --------------- 进度指示库 ---------------
source scripts/lib/progress.sh

# --------------- 容器运行时检测 ---------------
detect_runner() {
    if command -v podman >/dev/null 2>&1; then
        echo "podman"
    elif command -v docker >/dev/null 2>&1; then
        echo "docker"
    else
        echo ""
    fi
}

RUNNER=$(detect_runner)
if [ -z "$RUNNER" ]; then
    err "未找到 podman 或 docker"
    exit 1
fi

# --------------- 依赖声明 ---------------
# 每个基础镜像关联一个"依赖文件列表"（相对于项目根目录）
declare -A BASE_IMAGE_DEPS
BASE_IMAGE_DEPS["bbs-server-base"]="bbs-server/Dockerfile.base"
BASE_IMAGE_DEPS["bbs-nginx-base"]="nginx/Dockerfile.base nginx/nginx.conf.template"

# 每个基础镜像对应的 Dockerfile 路径
declare -A BASE_IMAGE_DOCKERFILE
BASE_IMAGE_DOCKERFILE["bbs-server-base"]="bbs-server/Dockerfile.base"
BASE_IMAGE_DOCKERFILE["bbs-nginx-base"]="nginx/Dockerfile.base"

# 构建上下文目录
declare -A BASE_IMAGE_CONTEXT
BASE_IMAGE_CONTEXT["bbs-server-base"]="bbs-server"
BASE_IMAGE_CONTEXT["bbs-nginx-base"]="."

CHECKSUM_FILE="dist/base-images/.base_checksum"
ARCHIVE_DIR="dist/archive"
REBUILT_FLAG="false"  # 全局标记，供 save 时判断

# --------------- 计算依赖文件联合 md5 ---------------
calc_checksum() {
    local dep_files="$1"
    local combined=""
    for f in $dep_files; do
        if [ -f "$f" ]; then
            local h
            h=$(md5sum "$f" 2>/dev/null | cut -d' ' -f1)
            combined="${combined}${h}"
        else
            warn "依赖文件不存在: $f"
            combined="${combined}00000000000000000000000000000000"
        fi
    done
    echo "$combined" | md5sum | cut -d' ' -f1
}

# --------------- 读取缓存的 checksum ---------------
get_cached_checksum() {
    local image_name="$1"
    if [ -f "$CHECKSUM_FILE" ]; then
        grep "^${image_name} " "$CHECKSUM_FILE" 2>/dev/null | cut -d' ' -f2
    fi
}

# --------------- 更新 checksum 缓存 ---------------
update_checksum() {
    local image_name="$1"
    local hash="$2"
    mkdir -p "$(dirname "$CHECKSUM_FILE")"
    grep -v "^${image_name} " "$CHECKSUM_FILE" 2>/dev/null > "${CHECKSUM_FILE}.tmp" || true
    echo "${image_name} ${hash}" >> "${CHECKSUM_FILE}.tmp"
    mv "${CHECKSUM_FILE}.tmp" "$CHECKSUM_FILE"
}

# --------------- 检查镜像是否需要重建 ---------------
needs_rebuild() {
    local image_name="$1"
    local dep_files="$2"
    local force="${3:-false}"

    # 强制重建
    if [ "$force" = "true" ]; then
        return 0
    fi

    # 镜像不存在
    if ! $RUNNER image exists "${image_name}:latest" 2>/dev/null; then
        info "${image_name}:latest 不存在"
        return 0
    fi

    # checksum 变更
    local current_hash
    current_hash=$(calc_checksum "$dep_files")
    local cached_hash
    cached_hash=$(get_cached_checksum "$image_name")

    if [ -n "$cached_hash" ] && [ "$current_hash" = "$cached_hash" ]; then
        return 1  # 不需要重建
    fi

    info "依赖文件已变更，需要重建 ${image_name}"
    return 0
}

# --------------- 构建单个基础镜像 ---------------
build_base_image() {
    local image_name="$1"
    local force="${2:-false}"
    local dep_files="${BASE_IMAGE_DEPS[$image_name]}"
    local dockerfile="${BASE_IMAGE_DOCKERFILE[$image_name]}"
    local context="${BASE_IMAGE_CONTEXT[$image_name]}"

    if [ -z "$dockerfile" ] || [ -z "$context" ]; then
        err "未知的基础镜像: $image_name"
        return 1
    fi

    local current_hash
    current_hash=$(calc_checksum "$dep_files")
    local cached_hash
    cached_hash=$(get_cached_checksum "$image_name")

    if ! needs_rebuild "$image_name" "$dep_files" "$force"; then
        info "${image_name} 依赖未变更，跳过构建"
        return 0
    fi

    # 有变更 → 提醒
    if [ -n "$cached_hash" ] && [ "$current_hash" != "$cached_hash" ]; then
        warn "${image_name} 的依赖文件 ${dep_files// /、} 已变更，正在重新构建..."
    fi

    run_with_timer "构建 $image_name" "$RUNNER" build \
        -t "${image_name}:latest" \
        -f "$dockerfile" \
        "$context"

    REBUILT_FLAG="true"
    update_checksum "$image_name" "$current_hash"
    ok "${image_name} 构建完成"
    return 0
}

# --------------- 导出基础镜像到 tar ---------------
save_base_image() {
    local image_name="$1"
    local save_dir="dist/base-images"
    local timestamp
    timestamp=$(date +%Y%m%d_%H%M%S)

    mkdir -p "$save_dir"

    # 如果本次重建了且旧 tar 存在，先归档
    if [ "$REBUILT_FLAG" = "true" ] && [ -f "${save_dir}/${image_name}.tar" ]; then
        mkdir -p "$ARCHIVE_DIR"
        mv "${save_dir}/${image_name}.tar" \
           "${ARCHIVE_DIR}/${image_name}-${timestamp}.tar"
        warn "旧基础镜像已归档: ${ARCHIVE_DIR}/${image_name}-${timestamp}.tar"
    fi

    run_with_timer "导出 ${image_name}.tar" "$RUNNER" save \
        -o "${save_dir}/${image_name}.tar" \
        "${image_name}:latest"

    local size
    size=$(du -h "${save_dir}/${image_name}.tar" | cut -f1)
    ok "${image_name}.tar 导出完成 (${size})"
}

# --------------- 从 tar 加载基础镜像 ---------------
load_base_image() {
    local image_name="$1"
    local tar_path="dist/base-images/${image_name}.tar"

    if $RUNNER image exists "${image_name}:latest" 2>/dev/null; then
        info "${image_name} 已存在，跳过加载"
        return 0
    fi

    if [ ! -f "$tar_path" ]; then
        err "找不到 ${tar_path}，请先运行 --save"
        return 1
    fi

    run_with_timer "加载 ${image_name}.tar" "$RUNNER" load -i "$tar_path"
    ok "${image_name} 加载完成"
}

# --------------- 主流程 ---------------
case "$MODE" in
    --check|check)
        # 只检测，不构建
        show_header "BBS 基础镜像依赖检测"
        any_changed=0
        for img in "${!BASE_IMAGE_DEPS[@]}"; do
            if needs_rebuild "$img" "${BASE_IMAGE_DEPS[$img]}" "false"; then
                warn "${img}: 需要重建（依赖已变更或镜像不存在）"
                any_changed=1
            else
                info "${img}: 依赖未变更，镜像已就绪"
            fi
        done
        exit $any_changed
        ;;

    --load|load)
        show_header "BBS 基础镜像加载"
        all_ok=0
        for img in "${!BASE_IMAGE_DEPS[@]}"; do
            load_base_image "$img" || all_ok=1
        done
        exit $all_ok
        ;;

    --save|save)
        show_header "BBS 基础镜像构建 + 导出"
        total=${#BASE_IMAGE_DEPS[@]}
        idx=0
        for img in "${!BASE_IMAGE_DEPS[@]}"; do
            idx=$((idx + 1))
            show_step "$idx" "$total" "构建 ${img}"
            build_base_image "$img" "false"
            save_base_image "$img"
        done
        ok "全部基础镜像已导出到 dist/base-images/"
        ;;

    --force-rebuild|force-rebuild)
        show_header "BBS 基础镜像强制重建"
        total=${#BASE_IMAGE_DEPS[@]}
        idx=0
        for img in "${!BASE_IMAGE_DEPS[@]}"; do
            idx=$((idx + 1))
            show_step "$idx" "$total" "强制重建 ${img}"
            build_base_image "$img" "true"
        done
        ok "全部基础镜像强制重建完成"
        ;;

    *)  # build（默认）
        show_header "BBS 基础镜像构建"
        total=${#BASE_IMAGE_DEPS[@]}
        idx=0
        for img in "${!BASE_IMAGE_DEPS[@]}"; do
            idx=$((idx + 1))
            show_step "$idx" "$total" "构建 ${img}"
            build_base_image "$img" "false"
        done
        ok "基础镜像构建完成"
        ;;
esac

# --------------- 输出摘要 ---------------
if [ "$MODE" != "check" ] && [ "$MODE" != "--check" ]; then
    echo ""
    echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║${NC}  基础镜像信息"
    echo -e "${GREEN}║${NC}"
    for img in "${!BASE_IMAGE_DEPS[@]}"; do
        if $RUNNER image exists "${img}:latest" 2>/dev/null; then
            size=$($RUNNER image inspect "${img}:latest" --format '{{.Size}}' 2>/dev/null | awk '{printf "%.0f MB", $1/1024/1024}' 2>/dev/null || echo "?")
            echo -e "${GREEN}║${NC}  ${img}:latest  ${size}"
        fi
    done
    echo -e "${GREEN}║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
fi
