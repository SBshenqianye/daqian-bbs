#!/bin/bash
# ============================================
# 一次性验证脚本（生产服务器跑，只读 + 探针自检）
# 验证"依赖下沉"改造后服务器的部署结果。
# 前置：新基础镜像已加载（bbs-server-base 含 /app/lib）+ 已用瘦升级包升级
# 用法: bash verify-server.sh
# 可选: BBS_HOME=/data/bbs  BBS_SERVER_PORT=60000
# 退出码: 0=全部通过  1=有失败项
# ============================================
set -u

BBS_HOME="${BBS_HOME:-/data/bbs}"
BBS_SERVER_PORT="${BBS_SERVER_PORT:-60000}"
RUNNER="podman"; command -v podman >/dev/null 2>&1 || RUNNER="docker"

PASS=0; FAIL=0

check() { # $1=描述  $2=判断命令(返回0为通过)
    if eval "$2" >/dev/null 2>&1; then
        echo "  [PASS] $1"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] $1"
        FAIL=$((FAIL + 1))
    fi
}

echo "===== 1. 基础镜像 ====="
check "bbs-server-base:latest 镜像存在" "$RUNNER image exists bbs-server-base:latest"
if $RUNNER image exists bbs-server-base:latest 2>/dev/null; then
    check "镜像内含依赖目录 /app/lib" \
        "$RUNNER run --rm --entrypoint ls bbs-server-base:latest -d /app/lib"
fi

echo "===== 2. 后端容器 ====="
check "bbs-server 容器存在" "$RUNNER container exists bbs-server"
if $RUNNER container exists bbs-server 2>/dev/null; then
    ARGS=$($RUNNER inspect bbs-server --format '{{.Config.Cmd}}' 2>/dev/null)
    check "启动参数含 -Dloader.path=/app/lib" "echo '$ARGS' | grep -q 'loader.path=/app/lib'"
fi

echo "===== 3. 当前版本产物 ====="
CUR=$(readlink -f "$BBS_HOME/current" 2>/dev/null)
check "current 软链可解析" "[ -n '$CUR' ]"
if [ -n "$CUR" ] && [ -f "$CUR/bbs-server.jar" ]; then
    JAR_MB=$(du -m "$CUR/bbs-server.jar" | cut -f1)
    check "当前 jar 为瘦 jar < 10MB（实际 ${JAR_MB}MB）" "[ '$JAR_MB' -lt 10 ]"
fi

echo "===== 4. 服务健康 ====="
CODE=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${BBS_SERVER_PORT}/bbs-server/" 2>/dev/null || echo 000)
check "后端健康检查返回 200（实际 $CODE）" "[ '$CODE' = '200' ]"

echo "===== 5. 上传目录挂载自检（探针） ====="
UPLOAD_DIR=$(grep -E '^BBS_UPLOAD_DIR=' "$BBS_HOME/.env" 2>/dev/null | cut -d= -f2 | tr -d '"' | tr -d "'")
UPLOAD_DIR="${UPLOAD_DIR:-$BBS_HOME/bbsUpload}"
PROBE=".verify-lib-$$"
echo ok > "$UPLOAD_DIR/$PROBE" 2>/dev/null
if $RUNNER exec bbs-server cat "$UPLOAD_DIR/$PROBE" 2>/dev/null | grep -q '^ok$'; then
    check "容器可读宿主上传目录" "true"
else
    check "容器可读宿主上传目录" "false"
fi
rm -f "$UPLOAD_DIR/$PROBE" 2>/dev/null

echo ""
echo "===== 结果: PASS=$PASS  FAIL=$FAIL ====="
[ "$FAIL" -eq 0 ]
