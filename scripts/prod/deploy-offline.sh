#!/bin/bash
# ============================================================================
# 兼容入口（2026-11-27 起）：deploy-offline.sh
#
# 现场 /data/bbs/deploy-offline.sh 与历史手册里的这些写法继续可用：
#   bash deploy-offline.sh                       → deploy.sh（自动：迁移/升级/校验）
#   bash deploy-offline.sh --install             → deploy.sh deploy
#   bash deploy-offline.sh --upgrade <pkg>       → deploy.sh --package <pkg>
#   bash deploy-offline.sh <pkg>                 → deploy.sh --package <pkg>
#   bash deploy-offline.sh --repair              → deploy.sh repair
#
# 注意：旧包（内含 upgrade-*/upgrade.sh、无 manifest.json）不受支持，
#       请用 scripts/prod/offline.sh 处理，或先用 package.sh 重打成新包。
# ============================================================================
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"
# 现场布局：deploy-offline.sh 与 deploy.sh 同在部署根
if [ -f "$DIR/deploy.sh" ]; then ROOT="$DIR"; fi
[ -f "$ROOT/deploy.sh" ] || {
    echo "[FAIL] 找不到新入口: $ROOT/deploy.sh" >&2
    exit 3
}

case "${1:-}" in
    --install|install)   shift; exec bash "$ROOT/deploy.sh" deploy "$@" ;;
    --upgrade|upgrade)   shift; exec bash "$ROOT/deploy.sh" --package "$1" ;;
    --repair|repair)     shift; exec bash "$ROOT/deploy.sh" repair "$@" ;;
    -h|--help|help)      exec bash "$ROOT/deploy.sh" help ;;
    *.tar.gz)            exec bash "$ROOT/deploy.sh" --package "$1" ;;
    ""|[a-z]*)           exec bash "$ROOT/deploy.sh" "$@" ;;
    *)                   echo "[FAIL] 无法识别的参数: $1（bash $ROOT/deploy.sh help）" >&2; exit 2 ;;
esac
