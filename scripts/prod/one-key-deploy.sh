#!/bin/bash
# ============================================================================
# 兼容入口（2026-11-27 起）
#
#   scripts/prod/one-key-deploy.sh → exec <部署根>/deploy.sh
#
# 旧实现已归档到 legacy/one-key-deploy.sh.2026-11-27（仅供追溯）。
# ============================================================================
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"
[ -f "$ROOT/deploy.sh" ] || {
    echo "[FAIL] 找不到新入口: $ROOT/deploy.sh（旧实现见 legacy/one-key-deploy.sh.2026-11-27）" >&2
    exit 3
}
exec bash "$ROOT/deploy.sh" "$@"
