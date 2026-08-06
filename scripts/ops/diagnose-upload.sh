#!/bin/bash
# ============================================
# 上传 404 一键诊断（只读，不改任何状态，仅一个临时写测试文件并自清理）
# 用法: bash scripts/ops/diagnose-upload.sh [UPLOAD_DIR] [NGINX_PORT]
#   UPLOAD_DIR  上传目录（默认 /data/bbs/bbsUpload，也可从 .env 读取）
#   NGINX_PORT  nginx 端口（默认 60001）
# 输出 6 项证据，用于定位"上传成功但 GET 404"的根因。
# ============================================
set -u

UPLOAD_DIR="${1:-}"
NGINX_PORT="${2:-60001}"
CONTAINER="bbs-server"

# 未指定目录时尝试从部署目录 .env 读取
if [ -z "$UPLOAD_DIR" ] && [ -f /data/bbs/.env ]; then
    BBS_ENV_UPLOAD=$(grep -E '^BBS_UPLOAD_DIR=' /data/bbs/.env 2>/dev/null | head -1 | cut -d= -f2-)
    [ -n "$BBS_ENV_UPLOAD" ] && UPLOAD_DIR="$BBS_ENV_UPLOAD"
fi
UPLOAD_DIR="${UPLOAD_DIR:-/data/bbs/bbsUpload}"

RUNNER="podman"; command -v podman >/dev/null 2>&1 || RUNNER="docker"

info()  { echo -e "\033[0;36m[INFO]\033[0m $1"; }
ok()    { echo -e "\033[0;32m[OK]\033[0m $1"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m $1"; }
err()   { echo -e "\033[0;31m[ERR]\033[0m $1"; }

echo ""
echo "========== BBS 上传 404 诊断（$RUNNER / $UPLOAD_DIR） =========="

# 1) 后端应用实际生效的 storage.path（通过 HTTP 直接探测）
#    /files/** 是 permitAll；若探针文件可被 GET 到 → 应用读的就是 $UPLOAD_DIR
echo ""
info "【1】应用 storage.path 探测（GET /files/.deploy-mount-probe）"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${NGINX_PORT}/bbs-server/files/.deploy-mount-probe" 2>/dev/null || echo 000)
if [ "$CODE" = "200" ]; then
    ok "GET /files/.deploy-mount-probe → 200：应用 storage.path = $UPLOAD_DIR（与宿主一致）"
elif [ "$CODE" = "404" ]; then
    err "GET /files/.deploy-mount-probe → 404：应用 storage.path ≠ $UPLOAD_DIR（配置不匹配！）"
    err "  宿主有 $UPLOAD_DIR/.deploy-mount-probe，但应用读不到 → 应用在写/读别的目录"
else
    warn "GET /files/.deploy-mount-probe → $CODE（后端未就绪或端口不对？）"
fi

# 2) 容器内挂载是否附着
echo ""
info "【2】容器内挂载状态（/proc/mounts）"
if $RUNNER exec "$CONTAINER" grep -q "$UPLOAD_DIR" /proc/mounts 2>/dev/null; then
    $RUNNER exec "$CONTAINER" grep "$UPLOAD_DIR" /proc/mounts 2>/dev/null
else
    err "容器内 /proc/mounts 中没有 $UPLOAD_DIR → 挂载未附着！"
fi

# 3) 宿主 vs 容器视角的上传目录（关键对比）
echo ""
info "【3】宿主 vs 容器视角上传目录内容"
echo "  --- 宿主 $UPLOAD_DIR/common/upload/ ---"
ls -la "$UPLOAD_DIR/common/upload/" 2>&1 | head -12
echo "  --- 容器内 $UPLOAD_DIR/common/upload/ ---"
$RUNNER exec "$CONTAINER" ls -la "$UPLOAD_DIR/common/upload/" 2>&1 | head -12

# 4) 写入可见性测试（写一个临时文件，看宿主是否可见；自清理）
echo ""
info "【4】写入可见性测试（容器写 → 宿主读）"
WRITE_TEST="diag-write-test-$$.tmp"
if $RUNNER exec "$CONTAINER" touch "$UPLOAD_DIR/$WRITE_TEST" 2>/dev/null; then
    if [ -f "$UPLOAD_DIR/$WRITE_TEST" ]; then
        ok "容器写入 $WRITE_TEST 宿主可见 → 当前挂载双向正常（此刻上传会落到宿主）"
        rm -f "$UPLOAD_DIR/$WRITE_TEST"
    else
        err "容器写入成功但宿主看不到 → 写入进了容器可写层（挂载此时失效）"
    fi
else
    err "容器内无法写入 $UPLOAD_DIR（SELinux 拒绝或目录只读）"
fi

# 5) 上传文件进了容器可写层吗（podman diff）
echo ""
info "【5】容器可写层新增文件（podman diff，重点看 upload）"
$RUNNER diff "$CONTAINER" 2>/dev/null | grep -i upload | tail -20
if [ -z "$($RUNNER diff "$CONTAINER" 2>/dev/null | grep -i upload | head -1)" ]; then
    info "容器可写层没有 upload 相关文件"
fi

# 6) 容器环境变量与日志错误
echo ""
info "【6】容器 BBS_UPLOAD_DIR 环境变量"
$RUNNER inspect "$CONTAINER" --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null | grep -i -E 'BBS_UPLOAD|UPLOAD' || err "容器 env 中没有 BBS_UPLOAD_DIR（应用将用默认值 /data/bbs/bbsUpload/）"

echo ""
info "【7】后端日志最近的图片保存错误"
$RUNNER logs --tail 300 "$CONTAINER" 2>&1 | grep -i -E '图片保存失败|上传|upload' | tail -10 || echo "  （无相关日志）"

echo ""
echo "========== 诊断完成 =========="
echo "把以上输出（或直接告诉我数字编号对应的行）贴回来即可定位。"
