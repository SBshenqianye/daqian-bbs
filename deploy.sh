#!/bin/bash
# ============================================
# 一键部署入口（根目录简写）
#
#   ./deploy.sh                 → WSL 联调（默认）
#   sudo ./deploy.sh prod       → 生产（交互确认）
#   sudo ./deploy.sh p -y       → 生产，免确认
#   ./deploy.sh -n              → 只预检（dry-run）
#
# 等价于：
#   bash scripts/prod/one-key-deploy.sh <目标> [选项]
# 完整选项见 scripts/prod/one-key-deploy.sh --help
# ============================================
set -u

DIR="$(cd "$(dirname "$0")" && pwd)"
exec bash "$DIR/scripts/prod/one-key-deploy.sh" "$@"
