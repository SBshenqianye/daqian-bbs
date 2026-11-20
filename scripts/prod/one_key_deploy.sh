#!/bin/bash
# 兼容入口：bash one_key_deploy.sh（下划线写法）
# 实际逻辑在 one-key-deploy.sh，参数与说明完全一致。
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
exec bash "$DIR/one-key-deploy.sh" "$@"
