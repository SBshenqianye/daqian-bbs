#!/bin/bash
# ============================================================================
# 兼容入口（2026-11-27 起）
#
#   /data/bbs/scripts/prod/one_key_deploy.sh   → exec /data/bbs/deploy.sh
#
# 历史文档（AGENTS.md / DEPLOY.md / SCRIPT.md 旧版）写的是这个下划线名字，
# 现场快捷方式和习惯命令都指向它，因此保留为薄壳，不再包含任何实现。
# 真实实现见部署根目录 deploy.sh（v1.0.0+）。
# ============================================================================
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"
[ -x "$ROOT/deploy.sh" ] || [ -f "$ROOT/deploy.sh" ] || {
    echo "[FAIL] 找不到新入口: $ROOT/deploy.sh" >&2
    echo "       请先同步 deploy.sh 到部署根目录（与升级包放在一起）" >&2
    exit 3
}
exec bash "$ROOT/deploy.sh" "$@"
