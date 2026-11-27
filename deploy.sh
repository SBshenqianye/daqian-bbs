#!/bin/bash
# ============================================================================
# 大千智荟论坛 · 统一部署入口（v1.0.0）
#
# 目标形态：一个入口、一个包、一条命令。首次安装 / 结构迁移 / 升级 / 修复 /
#           回滚 / 验证 / 诊断 全部走本脚本，现场不需要记第二个脚本名。
#
#   sudo bash deploy.sh                  # 日常升级/校验（默认只找包，不需要第二个参数）
#   sudo bash deploy.sh once migrate     # 一次性动作：结构迁移（once 可写 tmp）
#   sudo bash deploy.sh once rollback    # 一次性动作：回滚
#   sudo bash deploy.sh <包.tar.gz>      # 指定包（U 盘路径也行，脚本会自己拷进 packages/）
#   sudo bash deploy.sh verify           # 只读验证
#   sudo bash deploy.sh status           # 当前/上一版/磁盘/容器/端口
#   sudo bash deploy.sh diag             # 只读诊断包（脱敏，<20MB）
#   sudo bash deploy.sh repair | restart | sync-scripts | logs
#   bash deploy.sh help [cmd]            # 帮助；不 require root、不读 env、不加锁
#   sudo bash deploy.sh --dry-run        # 只预检
#
# 设计约定（现场只背两条）：
#   日常：bash deploy.sh            —— 包放哪都行（当前目录或 U 盘），自动找、自动校验、自动回滚
#   一次性：bash deploy.sh once <动作> —— once 是"只做这一次"的显式标记，防止手滑跑到日常路径
#
# 目录契约（见 交付现状.md §4）：
#   $BBS_HOME/
#   ├── deploy.sh  deploy.env  current.json
#   ├── releases/<版本>-<commit>/{app,scripts,manifest.json,SHA256SUMS}
#   ├── releases/current -> <版本>-<commit>
#   ├── versions/latest  -> ../releases/current     ★旧回滚命令依赖
#   ├── current          -> versions/latest         ★容器历史挂载源（兼容保留）
#   ├── src              -> current/app
#   ├── scripts/         （全量同步，真实目录）
#   ├── packages/  backups/  log/  data/  upload -> bbsUpload
#   ├── bbsUpload/  base-images/  archive/  legacy/
#   └── .env（真实文件，兼容保留）
#
# 硬约束：
#   1) 容器挂**真实路径**（releases/<id>/app/...），不挂兼容链；
#   2) 切换前完成全部校验与备份；切换后验证失败 → 自动回滚并 exit 1；
#   3) 同版本重跑 = 修复，不新建版本目录；
#   4) 失败不归档、不删包、保留现场；
#   5) 不执行 init-*.sql，不 DROP/TRUNCATE，不写业务数据。
#
# 退出码：0=全部通过  1=有失败项  2=用法错误  3=环境前置不满足
# ============================================================================
set -u

DEPLOY_VERSION="1.0.0"
SELF_DIR="$(cd "$(dirname "$0")" && pwd)"

CMD="auto"
ONCE=0
PKG_PATH=""
PKG_ORIGIN=""
DRY_RUN=0
ASSUME_YES=0
ALLOW_DOWNGRADE=0
SKIP_BACKUP=0
SOFT_RESTART=0
KEEP_BACKUPS="${BBS_BACKUP_KEEP:-30}"
ROLLBACK_TO=""
BBS_HOME="${BBS_HOME:-}"

TOTAL_STEPS=0
STEP=0
FAILS=0
WARNS=0
START_TS=$(date +%s)
ROLLBACK_ARMED=0
PREV_RELEASE=""
NEW_RELEASE=""
PKG_TMP=""
LOCK_DIR=""

if [ -t 1 ]; then
    C_R=$'\033[0;31m'; C_G=$'\033[0;32m'; C_Y=$'\033[1;33m'; C_C=$'\033[0;36m'; C_B=$'\033[1m'; C_N=$'\033[0m'
else
    C_R=""; C_G=""; C_Y=""; C_C=""; C_B=""; C_N=""
fi

info() { printf '%s[INFO]%s %s\n' "$C_C" "$C_N" "$*"; }
ok()   { printf '%s[ OK ]%s %s\n' "$C_G" "$C_N" "$*"; }
warn() { printf '%s[WARN]%s %s\n' "$C_Y" "$C_N" "$*"; WARNS=$((WARNS + 1)); }
err()  { printf '%s[FAIL]%s %s\n' "$C_R" "$C_N" "$*" >&2; }
die()  { err "$*"; exit 3; }
step() { STEP=$((STEP + 1)); printf '\n%s━━━ [%s/%s] %s ━━━%s\n' "$C_C" "$STEP" "$TOTAL_STEPS" "$*" "$C_N"; }

on_exit() {
    local rc=$?
    [ -n "$PKG_TMP" ] && [ -d "$PKG_TMP" ] && rm -rf "$PKG_TMP"
    [ -n "$LOCK_DIR" ] && [ -d "$LOCK_DIR" ] && rm -rf "$LOCK_DIR" 2>/dev/null
    exit $rc
}
trap on_exit EXIT INT TERM

# ---------------------------------------------------------------- help ------
usage() {
    sed -n '3,/^set -u/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'
    exit 0
}

help_sub() {
    case "${1:-}" in
        help)     usage ;;
        deploy|upgrade|auto) echo "usage: bash deploy.sh [包路径]"; echo "  日常升级：包放当前目录或 U 盘，直接 bash deploy.sh（自动找包→校验→备份→切换→验证→归档）"; echo "  没有包时只做校验，不改动任何东西"; exit 0 ;;
        verify)   echo "usage: bash deploy.sh verify [--quiet]"; echo "  只读验证：指针/产物哈希/容器挂载真实路径/挂载探针/HTTP 回读/三端点/DB 版本/重启计数"; echo "  退出码 0=全 PASS，1=有 FAIL"; exit 0 ;;
        status)   echo "usage: bash deploy.sh status"; echo "  打印当前版本、上一版、releases 列表、容器、端口、磁盘"; exit 0 ;;
        diag)     echo "usage: bash deploy.sh diag [--out DIR]"; echo "  只读诊断包：系统/容器/挂载/端口/DB/日志尾部，脱敏，输出 <20MB tar.gz"; exit 0 ;;
        rollback) echo "usage: bash deploy.sh once rollback [--to <release-id>]"; echo "  一次性动作：切回上一版并重建容器；db_compat=forward-only 时拒绝（需 --force）"; exit 0 ;;
        repair)   echo "usage: bash deploy.sh once repair"; echo "  一次性动作：不动版本，按当前 release 重建容器并验证"; exit 0 ;;
        restart)  echo "usage: bash deploy.sh once restart [--soft]"; echo "  一次性动作：默认 rm+run 重建容器（能换镜像/换 env）；--soft = podman restart"; exit 0 ;;
        migrate)  echo "usage: bash deploy.sh once migrate [包路径]"; echo "  一次性动作：把旧 versions/<ts>/ 布局导入 releases/，保留旧目录与兼容链"; exit 0 ;;
        sync-scripts) echo "usage: bash deploy.sh once sync-scripts [--package <pkg>]"; echo "  一次性动作：只同步脚本（rsync -a --delete）"; exit 0 ;;
        *)        usage ;;
    esac
}

# ---------------------------------------------------------------- args ------
while [ $# -gt 0 ]; do
    case "$1" in
        -h|--help|help)
            if [ "${2:-}" != "" ] && [ "${2#-}" = "$2" ]; then help_sub "$2"; fi
            usage ;;
        --help=*) usage ;;
        --dry-run|-n) DRY_RUN=1; shift ;;
        --yes|-y) ASSUME_YES=1; shift ;;
        --package) PKG_PATH="${2:-}"; shift 2 ;;
        --package=*) PKG_PATH="${1#*=}"; shift ;;
        --home) BBS_HOME="${2:-}"; shift 2 ;;
        --home=*) BBS_HOME="${1#*=}"; shift ;;
        --to) ROLLBACK_TO="${2:-}"; shift 2 ;;
        --to=*) ROLLBACK_TO="${1#*=}"; shift ;;
        --allow-downgrade) ALLOW_DOWNGRADE=1; shift ;;
        --skip-backup) SKIP_BACKUP=1; shift ;;
        --force) ALLOW_DOWNGRADE=1; shift ;;
        --keep) KEEP_BACKUPS="${2:-30}"; shift 2 ;;
        --soft) SOFT_RESTART=1; shift ;;
        --out) DIAG_OUT="${2:-}"; shift 2 ;;
        --quiet) QUIET=1; shift ;;
        once|tmp|oneoff) ONCE=1; shift ;;
        *.tar.gz) PKG_PATH="$1"; shift ;;
        auto|deploy|upgrade|install|migrate|verify|status|diag|rollback|repair|restart|logs|sync-scripts)
            CMD="$1"; shift ;;
        *) err "未知参数: $1（bash deploy.sh help 查看用法）"; exit 2 ;;
    esac
done
QUIET="${QUIET:-0}"

# ------------------------------------------------------------ 公共函数 ------
resolve_home() {
    local explicit=0
    [ -n "$BBS_HOME" ] && explicit=1
    # 源码仓库里误用部署入口：直接给出正确入口，绝不猜成 /data/bbs
    if [ "$explicit" = "0" ] && [ -f "$SELF_DIR/bbs-server/pom.xml" ]; then
        err "当前目录是源码仓库，不是部署机：deploy.sh 只用于部署根（含 versions/ 或 releases/）"
        echo "  开发机：bash scripts/dev/wsl.sh            # WSL 联调（60001）"
        echo "  打包：  bash scripts/prod/package.sh       # 产出 dist/bbs-*.tar.gz"
        echo "  演练：  bash delivery/rehearsal/setup-legacy.sh"
        echo "  确实要指定部署根：bash deploy.sh --home <部署根> ..."
        exit 2
    fi
    if [ -z "$BBS_HOME" ]; then
        if [ -f "$SELF_DIR/deploy.env" ] || [ -d "$SELF_DIR/versions" ] || [ -d "$SELF_DIR/releases" ]; then
            BBS_HOME="$SELF_DIR"
        elif [ -d /data/bbs ]; then
            BBS_HOME="/data/bbs"
        else
            BBS_HOME="$SELF_DIR"
        fi
    fi
    BBS_HOME="${BBS_HOME%/}"
    [ -d "$BBS_HOME" ] || die "部署根不存在: $BBS_HOME"
    # 源码仓库里误用部署入口：给出正确入口，避免在半成品目录里"部署"
    if [ "$explicit" = "0" ] && [ "$BBS_HOME" = "$SELF_DIR" ] && [ -f "$SELF_DIR/bbs-server/pom.xml" ]; then
        err "当前目录是源码仓库，不是部署机：deploy.sh 用于部署根（含 versions/ 或 releases/）"
        echo "  开发机：bash scripts/dev/wsl.sh            # WSL 联调（60001）"
        echo "  打包：  bash scripts/prod/package.sh       # 产出 dist/bbs-*.tar.gz"
        echo "  演练：  bash delivery/rehearsal/setup-legacy.sh"
        echo "  确实要在本机部署：bash deploy.sh --home <部署根> ..."
        exit 2
    fi
}

load_env() {
    local f
    for f in "$BBS_HOME/deploy.env" "$BBS_HOME/.env"; do
        [ -f "$f" ] || continue
        sed -i 's/\r$//' "$f" 2>/dev/null || true
        set -a
        # shellcheck disable=SC1090
        . "$f" 2>/dev/null || warn "读取配置失败: $f"
        set +a
        break
    done
    BBS_SERVER_PORT="${BBS_SERVER_PORT:-60000}"
    NGINX_PORT="${NGINX_PORT:-60001}"
    BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR:-$BBS_HOME/bbsUpload}"
    BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR%/}/"
    BBS_SERVER_CONTAINER="${BBS_SERVER_CONTAINER:-bbs-server}"
    BBS_NGINX_CONTAINER="${BBS_NGINX_CONTAINER:-bbs-nginx}"
    BBS_DB_HOST="${BBS_DB_HOST:-127.0.0.1}"
    BBS_DB_PORT="${BBS_DB_PORT:-15432}"
    BBS_DB_NAME="${BBS_DB_NAME:-bbs}"
    BBS_DB_USER="${BBS_DB_USER:-work_flow}"
    BBS_PG_CONTAINER="${BBS_PG_CONTAINER:-bbs-postgres}"
}

detect_runner() {
    if command -v podman >/dev/null 2>&1; then RUNNER=podman
    elif command -v docker >/dev/null 2>&1; then RUNNER=docker
    else RUNNER=""; fi
}

# podman 1.4.4（RHEL7）没有稳定的 `container exists`，统一用 inspect 判断
container_exists() {
    [ -n "$RUNNER" ] && $RUNNER inspect "$1" >/dev/null 2>&1
}

# 保留现网容器的重启策略；不存在时：新装默认 always，迁移沿用"无策略"
existing_restart_policy() {
    local c="$1" p=""
    container_exists "$c" && p=$($RUNNER inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$c" 2>/dev/null | head -1)
    case "$p" in
        ""|"<no value>"|"no") echo "no" ;;
        *) echo "$p" ;;
    esac
}

# 找宿主 pg_dump/psql（RHEL7 pgdg 装在 /usr/pgsql-NN/bin，不在 PATH）
pg_bin_dir() {
    if [ -n "${BBS_PG_BIN:-}" ] && [ -x "$BBS_PG_BIN/pg_dump" ]; then
        echo "$BBS_PG_BIN"; return 0
    fi
    if command -v pg_dump >/dev/null 2>&1; then
        dirname "$(command -v pg_dump)"; return 0
    fi
    local d
    for d in /usr/pgsql-*/bin /usr/lib/postgresql/*/bin /opt/pgsql/*/bin /opt/postgresql/*/bin; do
        [ -x "$d/pg_dump" ] && { echo "$d"; return 0; }
    done
    return 1
}

acquire_lock() {
    LOCK_DIR="$BBS_HOME/.deploy.lock"
    if mkdir "$LOCK_DIR" 2>/dev/null; then
        echo "pid=$$ at=$(date -Is) cmd=$CMD" > "$LOCK_DIR/owner" 2>/dev/null || true
        return 0
    fi
    local owner pid
    owner=$(cat "$LOCK_DIR/owner" 2>/dev/null || true)
    pid=$(printf '%s' "$owner" | sed -n 's/^pid=\([0-9][0-9]*\).*/\1/p')
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        die "已有部署在运行（$owner）；等它结束或确认后 rm -rf $LOCK_DIR"
    fi
    warn "清理过期锁（owner: ${owner:-空}）"
    rm -rf "$LOCK_DIR"
    mkdir "$LOCK_DIR" 2>/dev/null || die "无法创建锁目录: $LOCK_DIR"
    echo "pid=$$ at=$(date -Is) cmd=$CMD" > "$LOCK_DIR/owner" 2>/dev/null || true
}

# ---------- 指针 ----------
release_abs() { echo "$BBS_HOME/releases/$1"; }

release_of_pointer() {
    # 解析 releases/current，返回 release id（目录名）
    local p="$BBS_HOME/releases/current"
    [ -L "$p" ] || [ -d "$p" ] || { echo ""; return; }
    readlink -f "$p" 2>/dev/null | awk -F/ '{print $NF}'
}

current_release() { release_of_pointer; }

prev_release_from_state() {
    [ -f "$BBS_HOME/current.json" ] || { echo ""; return; }
    sed -n 's/.*"previous"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$BBS_HOME/current.json" | head -1
}

# previous 不允许指向"曾经验证失败"的版本，否则 rollback 会把人带到坑里
sanitize_prev() {
    local p="${1:-}" failed="${2:-}"
    [ -z "$p" ] && { echo ""; return; }
    [ "$p" = "$failed" ] && { echo ""; return; }
    [ -f "$BBS_HOME/releases/$p/.failed" ] && { echo ""; return; }
    echo "$p"
}

list_releases() {
    [ -d "$BBS_HOME/releases" ] || return 0
    find "$BBS_HOME/releases" -maxdepth 1 -mindepth 1 -type d -printf '%f\n' 2>/dev/null | grep -v '^current$' | sort
}

switch_pointers() {
    local id="$1"
    local rel="$BBS_HOME/releases/$id"
    [ -d "$rel" ] || die "release 不存在: $rel"
    # releases/current 原子替换
    ln -snf "$id" "$BBS_HOME/releases/.current.tmp" || die "创建临时指针失败"
    mv -T "$BBS_HOME/releases/.current.tmp" "$BBS_HOME/releases/current" || die "切换 releases/current 失败"
    # 兼容链：versions/latest -> ../releases/current
    ln -snf "../releases/current" "$BBS_HOME/versions/latest" || die "切换 versions/latest 失败"
    # 历史链：current -> versions/latest（保持现场手册有效）
    [ -e "$BBS_HOME/current" ] || [ -L "$BBS_HOME/current" ] || ln -snf "versions/latest" "$BBS_HOME/current"
    # 便捷链
    ln -snf "current/app" "$BBS_HOME/src"
    ok "指针已切换 → releases/$id"
}

write_current_json() {
    local cur="$1" prev="$2" failed="${3:-}"
    cat > "$BBS_HOME/current.json" <<EOF
{
  "product": "daqian-bbs",
  "current": "$cur",
  "previous": "$prev",
  "failed": "$failed",
  "current_path": "$BBS_HOME/releases/$cur",
  "updated_at": "$(date -Is)",
  "deploy_version": "$DEPLOY_VERSION",
  "manager": "deploy.sh"
}
EOF
}

# ---------- 容器 ----------
server_env_args() {
    printf '%s\n' \
        "-e BBS_DB_HOST=$BBS_DB_HOST" \
        "-e BBS_DB_PORT=$BBS_DB_PORT" \
        "-e BBS_DB_NAME=$BBS_DB_NAME" \
        "-e BBS_DB_USER=$BBS_DB_USER" \
        "-e BBS_DB_PASSWORD=${BBS_DB_PASSWORD:-}" \
        "-e BBS_SUPER_ADMIN_PASSWORD=${BBS_SUPER_ADMIN_PASSWORD:-1234@abcD}" \
        "-e BBS_UPLOAD_DIR=$BBS_UPLOAD_DIR" \
        "-e BBS_SERVER_PORT=$BBS_SERVER_PORT"
}

prepare_upload_dir() {
    mkdir -p "$BBS_UPLOAD_DIR" || die "无法创建上传目录: $BBS_UPLOAD_DIR"
    if command -v getenforce >/dev/null 2>&1 && [ "$(getenforce 2>/dev/null)" = "Enforcing" ]; then
        command -v chcon >/dev/null 2>&1 && chcon -Rt container_file_t "$BBS_UPLOAD_DIR" 2>/dev/null \
            || warn "chcon 失败；容器读不到上传目录时手工执行 chcon -Rt container_file_t $BBS_UPLOAD_DIR"
    fi
    echo ok > "$BBS_UPLOAD_DIR/.deploy-mount-probe"
}

run_server_container() {
    local jar="$1"
    [ -f "$jar" ] || { err "缺少 $jar"; return 1; }
    local -a envs=()
    while IFS= read -r line; do envs+=("$line"); done < <(server_env_args)
    local rp="always"
    container_exists "$BBS_SERVER_CONTAINER" && rp=$(existing_restart_policy "$BBS_SERVER_CONTAINER")
    info "启动 $BBS_SERVER_CONTAINER（jar: $jar；restart=$rp）"
    $RUNNER rm -f "$BBS_SERVER_CONTAINER" >/dev/null 2>&1 || true
    $RUNNER run -d \
        --name "$BBS_SERVER_CONTAINER" \
        --network host \
        --restart="$rp" \
        --entrypoint java \
        "${envs[@]}" \
        -v "$jar:/app/app.jar:Z" \
        -v "$BBS_UPLOAD_DIR:$BBS_UPLOAD_DIR:Z" \
        bbs-server-base:latest -Xmx2g -Dloader.path=/app/lib -jar /app/app.jar \
        --spring.profiles.active=podman >/dev/null \
        || { err "bbs-server 启动失败（$RUNNER logs $BBS_SERVER_CONTAINER）"; return 1; }
}

run_nginx_container() {
    local ui="$1" admin="$2"
    [ -d "$ui" ] || { err "缺少 $ui"; return 1; }
    [ -d "$admin" ] || { err "缺少 $admin"; return 1; }
    local rp="always"
    container_exists "$BBS_NGINX_CONTAINER" && rp=$(existing_restart_policy "$BBS_NGINX_CONTAINER")
    info "启动 $BBS_NGINX_CONTAINER（dist: $ui）"
    $RUNNER rm -f "$BBS_NGINX_CONTAINER" >/dev/null 2>&1 || true
    $RUNNER run -d \
        --name "$BBS_NGINX_CONTAINER" \
        --network host \
        --restart="$rp" \
        -e NGINX_PORT="$NGINX_PORT" \
        -e BBS_SERVER_PORT="$BBS_SERVER_PORT" \
        -v "$ui:/usr/share/nginx/html/bbs-ui:Z" \
        -v "$admin:/usr/share/nginx/html/bbs-admin:Z" \
        bbs-nginx-base:latest >/dev/null \
        || { err "bbs-nginx 启动失败（$RUNNER logs $BBS_NGINX_CONTAINER）"; return 1; }
}

# 新布局：releases/<id>/app/...
recreate_containers() {
    local rel_abs="$1"
    run_server_container "$rel_abs/app/bbs-server.jar" || return 1
    run_nginx_container "$rel_abs/app/bbs-ui/dist" "$rel_abs/app/bbs-admin-ui/dist" || return 1
}

# 旧布局：versions/<ts>/{bbs-server.jar,bbs-ui/dist,bbs-admin-ui/dist}（迁移失败时回退用）
recreate_legacy_stack() {
    local dir="$1"
    run_server_container "$dir/bbs-server.jar" || return 1
    run_nginx_container "$dir/bbs-ui/dist" "$dir/bbs-admin-ui/dist" || return 1
}

wait_health() {
    local i code
    for i in $(seq 1 30); do
        code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "http://127.0.0.1:${BBS_SERVER_PORT}/bbs-server/" 2>/dev/null) || code=000
        [ -n "$code" ] || code=000
        [ "$code" = "200" ] && { ok "后端就绪（约 $((i * 2))s）"; return 0; }
        [ "$i" = "1" ] && info "等待后端就绪（最多 60s）..."
        sleep 2
    done
    warn "后端 60s 内未就绪（最后 HTTP $code）"
    return 1
}

# ---------- 备份 ----------
disk_free_mb() { df -Pm "${1:-/}" 2>/dev/null | awk 'NR==2{print $4}'; }

dump_db() {
    local dest_dir="$1"
    local out="$dest_dir/bbs_$(date +%Y%m%d_%H%M%S).sql.gz"
    mkdir -p "$dest_dir" || return 1
    set -o pipefail
    local bin; bin=$(pg_bin_dir || true)
    if [ -n "$bin" ]; then
        local pgdump="$bin/pg_dump" ready="$bin/pg_isready"
        [ -x "$ready" ] || ready=""
        if [ -n "$ready" ] && ! "$ready" -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" >/dev/null 2>&1; then
            err "宿主 $ready 探测失败（$BBS_DB_HOST:$BBS_DB_PORT），请先确认 PG 在监听"
            set +o pipefail; return 1
        fi
        info "pg_dump（宿主 $pgdump → $BBS_DB_HOST:$BBS_DB_PORT/$BBS_DB_NAME）"
        PGPASSWORD="${BBS_DB_PASSWORD:-}" "$pgdump" -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" -U "$BBS_DB_USER" \
            --no-owner --no-privileges "$BBS_DB_NAME" | gzip > "$out" || { set +o pipefail; err "pg_dump 失败（$pgdump）"; return 1; }
    elif container_exists "$BBS_PG_CONTAINER"; then
        info "pg_dump（容器 $BBS_PG_CONTAINER）→ $out"
        $RUNNER exec "$BBS_PG_CONTAINER" pg_dump -h 127.0.0.1 -p 5432 -U "$BBS_DB_USER" \
            --no-owner --no-privileges "$BBS_DB_NAME" | gzip > "$out" || { set +o pipefail; return 1; }
    else
        set +o pipefail
        err "找不到 pg_dump（PATH 与 /usr/pgsql-*/bin 都没有）也没有 PG 容器（$BBS_PG_CONTAINER），无法备份"
        err "处理：设置 BBS_PG_BIN=<pg_dump 所在目录> 后重跑，或（明确不要备份时）加 --skip-backup"
        return 1
    fi
    set +o pipefail
    [ -s "$out" ] || { err "备份文件为空: $out"; return 1; }
    gunzip -t "$out" 2>/dev/null || { err "备份 gzip 校验失败: $out"; return 1; }
    local mb; mb=$(du -m "$out" | cut -f1)
    [ "$mb" -ge 1 ] || { err "备份体积异常: ${mb}MB"; return 1; }
    ok "DB 备份通过：$out（${mb}MB，gunzip -t OK）"
    echo "$out" > "$dest_dir/latest-dump.path"
}

prune_backups() {
    local keep="${1:-30}" dir="$BBS_HOME/backups"
    [ -d "$dir" ] || return 0
    local n; n=$(find "$dir" -maxdepth 1 -mindepth 1 -type d -name '20*' | wc -l)
    [ "$n" -le "$keep" ] && return 0
    find "$dir" -maxdepth 1 -mindepth 1 -type d -name '20*' -printf '%f\n' | sort | head -n $((n - keep)) | while read -r old; do
        rm -rf "$dir/$old" && info "清理旧备份: $old"
    done
}

backup_before_switch() {
    local cur="$1"
    local ts; ts=$(date +%Y%m%d_%H%M%S)
    local dir="$BBS_HOME/backups/$ts"
    mkdir -p "$dir" || die "无法创建备份目录 $dir"
    {
        echo "== time"; date -Is
        echo "== release_before"; echo "$cur"
        echo "== links"; readlink -f "$BBS_HOME/current" "$BBS_HOME/versions/latest" 2>/dev/null
        echo "== ls home"; ls -la "$BBS_HOME"
    } > "$dir/meta.txt"
    cp -a "$BBS_HOME/deploy.env" "$dir/deploy.env.bak" 2>/dev/null || cp -a "$BBS_HOME/.env" "$dir/env.bak" 2>/dev/null || true
    chmod 600 "$dir"/*.bak 2>/dev/null || true
    [ -n "$RUNNER" ] && $RUNNER inspect "$BBS_SERVER_CONTAINER" "$BBS_NGINX_CONTAINER" > "$dir/inspect.json" 2>/dev/null || true
    [ -n "$RUNNER" ] && $RUNNER images --digests > "$dir/images.txt" 2>/dev/null || true
    if [ -n "$cur" ] && [ -f "$BBS_HOME/releases/$cur/app/bbs-server.jar" ]; then
        sha256sum "$BBS_HOME/releases/$cur/app/bbs-server.jar" > "$dir/jar.sha256" 2>/dev/null || true
    fi
    if [ "$SKIP_BACKUP" = "1" ]; then
        warn "已跳过数据库备份（--skip-backup），本次升级没有 DB 退路"
    else
        dump_db "$dir" || die "数据库备份失败，已中止（确认可跳过时用 --skip-backup）"
    fi
    prune_backups "$KEEP_BACKUPS"
    LAST_BACKUP_DIR="$dir"
}

# ---------- 包 ----------
find_package() {
    local soft="${1:-}"
    if [ -n "$PKG_PATH" ]; then
        [ -f "$PKG_PATH" ] || die "包不存在: $PKG_PATH"
        case "$PKG_PATH" in
            /media/*|/run/media/*|/mnt/*) PKG_ORIGIN="removable" ;;
            *) PKG_ORIGIN="local" ;;
        esac
        return 0
    fi
    local f d
    f=$(ls -t "$BBS_HOME"/bbs-upgrade-*.tar.gz "$BBS_HOME"/bbs-migrate-*.tar.gz 2>/dev/null | head -1)
    [ -z "$f" ] && f=$(ls -t "$SELF_DIR"/bbs-upgrade-*.tar.gz "$SELF_DIR"/bbs-migrate-*.tar.gz 2>/dev/null | head -1)
    [ -z "$f" ] && f=$(ls -t "$SELF_DIR"/dist/bbs-upgrade-*.tar.gz "$SELF_DIR"/dist/bbs-migrate-*.tar.gz 2>/dev/null | head -1)
    # U 盘/挂载点：让现场少一步 cp（插上 U 盘就能一条命令）
    if [ -z "$f" ]; then
        for d in /media/* /media/*/* /run/media/*/* /mnt/*; do
            [ -d "$d" ] || continue
            f=$(ls -t "$d"/bbs-upgrade-*.tar.gz "$d"/bbs-migrate-*.tar.gz "$d"/bbs-full-*.tar.gz 2>/dev/null | head -1)
            [ -n "$f" ] && { PKG_ORIGIN="removable"; break; }
        done
    fi
    [ -z "$f" ] && f=$(ls -t "$BBS_HOME"/bbs-*.tar.gz "$SELF_DIR"/dist/bbs-*.tar.gz 2>/dev/null | grep -v 'bbs-full-' | head -1)
    [ -n "$f" ] || { [ "$soft" = "soft" ] && return 1; die "未找到升级包（部署根、当前目录、U 盘都没有 bbs-upgrade-*.tar.gz）；把包放进来后重跑同一条命令"; }
    PKG_PATH="$f"
    [ -z "$PKG_ORIGIN" ] && PKG_ORIGIN="local"
}

verify_sig() {
    local p="$1"
    if [ -f "$p.sha256" ]; then
        local exp act
        exp=$(tr -dc '0-9a-fA-F' < "$p.sha256" | tr '[:lower:]' '[:upper:]')
        if [ "${#exp}" -ne 64 ]; then
            die "校验文件异常（$(basename "$p").sha256 长度 ${#exp}，应为 64 位十六进制）；多半是拷贝中断，请重新拷包"
        fi
        act=$(sha256sum "$p" | awk '{print toupper($1)}')
        [ "$exp" = "$act" ] || die "包 SHA256 不匹配（期望 $exp 实际 $act）"
        ok "包 SHA256 校验通过"
    else
        warn "包无同名 .sha256，跳过整包校验（manifest 内逐文件哈希仍会校验）"
    fi
}

unpack_package() {
    local p="$1"
    PKG_TMP=$(mktemp -d) || die "mktemp 失败"
    # 路径安全：拒绝绝对路径与 ..
    if tar -tzf "$p" 2>/dev/null | grep -Eq '(^/|(^|/)\.\.(/|$))'; then
        die "包内含非法路径（绝对路径或 ..），拒绝解压"
    fi
    if tar -tzf "$p" 2>/dev/null | grep -Eq '(^|/)deploy\.sh$'; then
        die "包内不得包含 deploy.sh（会覆盖运行中的入口脚本）"
    fi
    tar -xzf "$p" -C "$PKG_TMP" --no-same-owner 2>/dev/null || die "包解压失败: $p"
    local root="$PKG_TMP"
    if [ ! -f "$root/manifest.json" ]; then
        root=$(find "$PKG_TMP" -maxdepth 2 -name manifest.json -printf '%h\n' 2>/dev/null | head -1)
    fi
    [ -n "$root" ] && [ -f "$root/manifest.json" ] || die "包内缺少 manifest.json（旧格式包请用 scripts/prod/offline.sh 处理，或先用 package.sh 重打成新包）"
    PKG_ROOT="$root"
    ok "包已解压: $PKG_ROOT"
}

load_manifest() {
    local root="$1"
    if [ -f "$root/manifest.env" ]; then
        set -a; . "$root/manifest.env"; set +a
    fi
    json_get() { sed -n "s/.*\"$1\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" "$root/manifest.json" | head -1; }
    M_PRODUCT="${M_PRODUCT:-$(json_get product)}"
    M_VERSION="${M_VERSION:-$(json_get version)}"
    M_COMMIT="${M_COMMIT:-$(json_get commit)}"
    M_MODE="${M_MODE:-$(json_get mode)}"
    M_STAGE="${M_STAGE:-$(json_get stage)}"
    M_DB_COMPAT="${M_DB_COMPAT:-$(json_get db_compat)}"
    [ -n "$M_VERSION" ] || die "manifest 缺少 version"
    M_ID="$M_VERSION"
    [ -n "$M_COMMIT" ] && [ "$M_COMMIT" != "unknown" ] && M_ID="${M_VERSION}-${M_COMMIT}"
    ok "包版本: $M_ID（mode=$M_MODE stage=${M_STAGE:-release} db_compat=${M_DB_COMPAT:-unknown}）"
}

verify_package_files() {
    local root="$1"
    [ -f "$root/SHA256SUMS" ] || { warn "包内无 SHA256SUMS，跳过逐文件校验"; return 0; }
    ( cd "$root" && sha256sum -c --quiet SHA256SUMS ) 2>/dev/null \
        || die "包内逐文件哈希校验失败（SHA256SUMS）"
    ok "包内逐文件哈希校验通过（$(grep -c . "$root/SHA256SUMS") 个文件）"
}

# ---------- 发布目录 ----------
stage_release() {
    local root="$1" id="$2"
    local dst="$BBS_HOME/releases/$id"
    if [ -d "$dst" ]; then
        warn "release 已存在，按修复处理（不覆盖 app）：$dst"
    fi
    mkdir -p "$dst/app" || die "无法创建 $dst"
    if [ -d "$root/payload/app" ]; then
        cp -a --no-preserve=context "$root/payload/app/." "$dst/app/" || die "复制 app 失败"
    else
        die "包内缺少 payload/app（旧布局包请用 legacy 流程）"
    fi
    mkdir -p "$dst/scripts"
    if [ -d "$root/scripts" ]; then
        cp -a --no-preserve=context "$root/scripts/." "$dst/scripts/" || die "复制 scripts 快照失败"
    fi
    # 兼容旧脚本/旧手册的路径：current/bbs-server.jar、current/bbs-ui/dist ...
    # （deploy.sh 自己挂 app/ 真实路径，这些只是给 legacy 工具和人工排查用）
    ( cd "$dst" && ln -sfn app/bbs-server.jar bbs-server.jar \
        && ln -sfn app/bbs-ui bbs-ui && ln -sfn app/bbs-admin-ui bbs-admin-ui ) \
        || die "创建兼容软链失败"
    cp -a --no-preserve=context "$root/manifest.json" "$dst/manifest.json" || die "复制 manifest 失败"
    [ -f "$root/manifest.env" ] && cp -a "$root/manifest.env" "$dst/manifest.env"
    # release 内的 SHA256SUMS 按 release 布局（app/、scripts/）重新生成，
    # 不能直接沿用包内的（包内路径是 payload/app/...）
    ( cd "$dst" && find app scripts -type f -print0 2>/dev/null | sort -z | xargs -0 -r sha256sum > SHA256SUMS )
    ok "release 落盘: $dst（$(grep -c . "$dst/SHA256SUMS" 2>/dev/null) 个文件已登记哈希）"
}

verify_staged_release() {
    local id="$1"
    local dst="$BBS_HOME/releases/$id"
    local jar="$dst/app/bbs-server.jar"
    [ -s "$jar" ] || { err "缺少 $jar"; return 1; }
    local magic; magic=$(head -c 2 "$jar" 2>/dev/null)
    [ "$magic" = "PK" ] || { err "jar 不是 ZIP 布局（magic=$magic）"; return 1; }
    [ -f "$dst/app/bbs-ui/dist/index.html" ] || { err "缺少 bbs-ui/dist/index.html"; return 1; }
    [ -f "$dst/app/bbs-admin-ui/dist/index.html" ] || { err "缺少 bbs-admin-ui/dist/index.html"; return 1; }
    if [ -f "$dst/SHA256SUMS" ]; then
        ( cd "$dst" && sha256sum -c --quiet SHA256SUMS ) 2>/dev/null \
            || { err "release 内逐文件哈希校验失败"; return 1; }
    fi
    local mb; mb=$(du -m "$jar" | cut -f1)
    [ "$mb" -lt 60 ] || warn "jar ${mb}MB 偏大（瘦包应 <10MB，fat jar 也兼容）"
    ok "release 预校验通过（jar ${mb}MB，两个前端 index.html 就位）"
}

sync_scripts() {
    local src="$1"
    [ -d "$src" ] || { warn "包内无 scripts/，跳过脚本同步"; return 0; }
    mkdir -p "$BBS_HOME/scripts"
    if command -v rsync >/dev/null 2>&1; then
        rsync -a --delete --exclude '.env' "$src/" "$BBS_HOME/scripts/" || die "rsync 脚本同步失败"
    else
        cp -a "$src/." "$BBS_HOME/scripts/" || die "复制脚本失败"
        # 无 rsync：删差集（只删源里不存在的文件）
        ( cd "$src" && find . -type f -printf '%P\n' ) | sort > /tmp/.keep.$$
        ( cd "$BBS_HOME/scripts" && find . -type f -printf '%P\n' ) | sort | comm -13 /tmp/.keep.$$ - \
            | while read -r f; do [ -n "$f" ] && rm -f "$BBS_HOME/scripts/$f"; done
        rm -f /tmp/.keep.$$
    fi
    ok "scripts 已全量同步: $BBS_HOME/scripts"
    cleanup_root_scripts
}

# 根目录只留部署入口：其它 .sh 要么已归位到 scripts/（同名同内容→删副本），
# 要么来历不明（→移入 legacy/root-scripts/ 保留，不静默删）
cleanup_root_scripts() {
    local f base dst
    for f in "$BBS_HOME"/*.sh; do
        [ -f "$f" ] || continue
        base=$(basename "$f")
        case "$base" in
            deploy.sh|deploy-offline.sh) continue ;;   # 部署入口 + 兼容入口
        esac
        dst=$(find "$BBS_HOME/scripts" -type f -name "$base" 2>/dev/null | head -1)
        if [ -n "$dst" ] && cmp -s "$f" "$dst"; then
            rm -f "$f" && info "根目录脚本已归位（删副本）: $base → $dst"
        elif [ -n "$dst" ]; then
            warn "根目录 $base 与 $dst 同名但内容不同，未删除（请人工确认哪份是新的）"
        else
            mkdir -p "$BBS_HOME/legacy/root-scripts"
            mv -f "$f" "$BBS_HOME/legacy/root-scripts/$base" \
                && warn "根目录脚本不在 scripts/ 里，已移入 legacy/root-scripts/: $base"
        fi
    done
}

# ---------- 验证 ----------
vcheck() { # $1=描述 $2=命令
    if eval "$2" >/dev/null 2>&1; then
        [ "$QUIET" = "1" ] || printf '  [PASS] %s\n' "$1"
        return 0
    fi
    printf '  [FAIL] %s\n' "$1"
    FAILS=$((FAILS + 1))
    return 1
}

verify_all() {
    FAILS=0
    local cur; cur=$(current_release)
    local rel="$BBS_HOME/releases/$cur"
    echo "===== 1. 指针与产物 ====="
    vcheck "releases/current 可解析（$cur）" "[ -n '$cur' ] && [ -d '$rel' ]"
    vcheck "versions/latest 解析到同一版本" "[ \"\$(readlink -f '$BBS_HOME/versions/latest' 2>/dev/null)\" = \"\$(readlink -f '$rel' 2>/dev/null)\" ]"
    vcheck "current 兼容链可解析" "[ \"\$(readlink -f '$BBS_HOME/current' 2>/dev/null)\" = \"\$(readlink -f '$rel' 2>/dev/null)\" ]"
    vcheck "src 指向 app" "[ \"\$(readlink -f '$BBS_HOME/src' 2>/dev/null)\" = \"\$(readlink -f '$rel/app' 2>/dev/null)\" ]"
    vcheck "jar 存在且非空" "[ -s '$rel/app/bbs-server.jar' ]"
    vcheck "两个前端 index.html 存在" "[ -f '$rel/app/bbs-ui/dist/index.html' ] && [ -f '$rel/app/bbs-admin-ui/dist/index.html' ]"
    if [ -f "$rel/SHA256SUMS" ]; then
        vcheck "release 内逐文件哈希" "( cd '$rel' && sha256sum -c --quiet SHA256SUMS )"
    fi

    echo "===== 2. 容器与挂载（必须是真实路径） ====="
    vcheck "$BBS_SERVER_CONTAINER 运行中" "[ \"\$($RUNNER inspect -f '{{.State.Running}}' '$BBS_SERVER_CONTAINER' 2>/dev/null)\" = 'true' ]"
    vcheck "$BBS_NGINX_CONTAINER 运行中" "[ \"\$($RUNNER inspect -f '{{.State.Running}}' '$BBS_NGINX_CONTAINER' 2>/dev/null)\" = 'true' ]"
    local mounts; mounts=$($RUNNER inspect -f '{{range .Mounts}}{{.Source}}{{"\n"}}{{end}}' "$BBS_SERVER_CONTAINER" 2>/dev/null)
    vcheck "jar 挂的是 releases/$cur 真实路径" "printf '%s' \"\$mounts\" | grep -qxF '$rel/app/bbs-server.jar'"
    vcheck "未挂兼容链（current/versions）" "! printf '%s' \"\$mounts\" | grep -qE '/(current|versions)/'"
    vcheck "$BBS_SERVER_CONTAINER 无重启循环" "[ \"\$($RUNNER inspect -f '{{.RestartCount}}' '$BBS_SERVER_CONTAINER' 2>/dev/null)\" -le 2 ]"

    echo "===== 3. 挂载探针（宿主写 → 容器读） ====="
    prepare_upload_dir >/dev/null 2>&1
    vcheck "容器可读宿主上传目录" "[ \"\$($RUNNER exec '$BBS_SERVER_CONTAINER' cat '$BBS_UPLOAD_DIR.deploy-mount-probe' 2>/dev/null)\" = 'ok' ]"

    echo "===== 4. HTTP 文件回读（端到端） ====="
    local day file url code
    day=$(date +%Y-%m-%d); file="bbs-serve-probe-$(date +%s)_.png"
    mkdir -p "$BBS_UPLOAD_DIR/common/upload/$day"
    printf '%s' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==' | base64 -d > "$BBS_UPLOAD_DIR/common/upload/$day/$file" 2>/dev/null \
        || echo -n png > "$BBS_UPLOAD_DIR/common/upload/$day/$file"
    url="http://127.0.0.1:${NGINX_PORT}/bbs-server/files/common/upload/$day/$file"
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$url" 2>/dev/null) || code=000
    [ -n "$code" ] || code=000
    vcheck "GET /files/... → 200（实际 $code）" "[ '$code' = '200' ]"
    rm -f "$BBS_UPLOAD_DIR/common/upload/$day/$file" 2>/dev/null

    echo "===== 5. 端点 ====="
    for p in "/bbs-server/" "/bbs-user/" "/bbs-admin/"; do
        code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "http://127.0.0.1:${NGINX_PORT}${p}" 2>/dev/null) || code=000
        [ -n "$code" ] || code=000
        vcheck "GET $p → 200（实际 $code）" "[ '$code' = '200' ]"
    done

    echo "===== 6. 版本一致性 ====="
    local mver mcommit
    mver=$(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$rel/manifest.json" 2>/dev/null | head -1)
    mcommit=$(sed -n 's/.*"commit"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$rel/manifest.json" 2>/dev/null | head -1)
    vcheck "manifest.version 与 release 目录一致（$mver）" "[ -n '$mver' ] && printf '%s' '$cur' | grep -qF '$mver'"
    [ -n "$mcommit" ] && vcheck "manifest.commit 记录为 $mcommit" "[ -n '$mcommit' ]"

    echo "===== 7. 数据库（只读） ====="
    local pbin; pbin=$(pg_bin_dir || true)
    local vcount="" can_query=0
    if [ -n "$pbin" ] && [ -x "$pbin/psql" ]; then
        can_query=1
        vcount=$(PGPASSWORD="${BBS_DB_PASSWORD:-}" "$pbin/psql" -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" \
            -U "$BBS_DB_USER" -d "$BBS_DB_NAME" -Atc 'select count(*) from bbs_schema_version' 2>/dev/null)
    elif container_exists "$BBS_PG_CONTAINER"; then
        can_query=1
        vcount=$($RUNNER exec "$BBS_PG_CONTAINER" psql -U "$BBS_DB_USER" -d "$BBS_DB_NAME" \
            -Atc 'select count(*) from bbs_schema_version' 2>/dev/null)
    else
        warn "未找到 psql（宿主/容器都不可用），跳过 DB 校验"
    fi
    [ "$can_query" = "1" ] && vcheck "DB 可查 bbs_schema_version（${vcount:-0} 条）" "[ -n '$vcount' ] && [ '$vcount' -ge 1 ]"

    echo ""
    echo "===== 结果: FAIL=$FAILS ====="
    [ "$FAILS" -eq 0 ]
}

# ---------------------------------------------------------------- 命令 ------
cmd_status() {
    resolve_home; load_env; detect_runner
    echo "部署根      : $BBS_HOME"
    echo "deploy.sh   : v$DEPLOY_VERSION ($SELF_DIR)"
    echo "当前版本    : $(current_release || echo '(无)')"
    echo "上一版      : $(prev_release_from_state || echo '(无)')"
    echo "releases    :"; list_releases | sed 's/^/  - /'
    echo "指针        :"
    echo "  current         -> $(readlink "$BBS_HOME/current" 2>/dev/null || echo '缺失')"
    echo "  versions/latest -> $(readlink "$BBS_HOME/versions/latest" 2>/dev/null || echo '缺失')"
    echo "  releases/current-> $(readlink "$BBS_HOME/releases/current" 2>/dev/null || echo '缺失')"
    echo "  readlink -f current = $(readlink -f "$BBS_HOME/current" 2>/dev/null)"
    echo "端口        : server=$BBS_SERVER_PORT nginx=$NGINX_PORT db=$BBS_DB_PORT($BBS_DB_NAME)"
    echo "磁盘 /      : $(disk_free_mb /)MB free"
    echo "磁盘 \$BBS_HOME: $(disk_free_mb "$BBS_HOME" 2>/dev/null)MB free"
    if [ -n "$RUNNER" ]; then
        echo "容器        :"
        $RUNNER ps -a --filter "name=$BBS_SERVER_CONTAINER" --filter "name=$BBS_NGINX_CONTAINER" \
            --format '  {{.Names}}  {{.Status}}  {{.Image}}' 2>/dev/null
    fi
    [ -f "$BBS_HOME/current.json" ] && { echo "current.json:"; sed 's/^/  /' "$BBS_HOME/current.json"; }
}

cmd_verify() {
    resolve_home; load_env; detect_runner
    [ -n "$RUNNER" ] || die "未找到 podman/docker"
    verify_all
}

cmd_diag() {
    resolve_home; load_env; detect_runner
    local out="${DIAG_OUT:-$BBS_HOME/log/diag-$(date +%Y%m%d_%H%M%S)}"
    mkdir -p "$out" || die "无法创建 $out"
    {
        echo "== time"; date -Is
        echo "== host"; hostname; uname -a; cat /etc/redhat-release 2>/dev/null
        echo "== selinux"; getenforce 2>/dev/null || echo n/a
        echo "== disk"; df -hP "$BBS_HOME" / 2>/dev/null
        echo "== mem"; free -m 2>/dev/null
        echo "== manager"; echo "deploy.sh v$DEPLOY_VERSION"
        echo "== links"; readlink "$BBS_HOME/current" "$BBS_HOME/versions/latest" "$BBS_HOME/releases/current" 2>/dev/null
        echo "== resolved"; readlink -f "$BBS_HOME/current" 2>/dev/null
    } > "$out/system.txt" 2>&1
    sed -E 's/(PASSWORD|password|PASSWD)=.*/\1=***/' "$BBS_HOME/deploy.env" > "$out/deploy.env.masked" 2>/dev/null \
        || sed -E 's/(PASSWORD|password|PASSWD)=.*/\1=***/' "$BBS_HOME/.env" > "$out/deploy.env.masked" 2>/dev/null || true
    ls -la "$BBS_HOME" > "$out/home.txt" 2>&1
    ls -la "$BBS_HOME/releases" >> "$out/home.txt" 2>&1
    sed 's/^/  /' "$BBS_HOME/current.json" > "$out/current.json" 2>/dev/null || true
    ss -ltnp 2>/dev/null | egrep ":($BBS_SERVER_PORT|$NGINX_PORT|$BBS_DB_PORT)\b" > "$out/ports.txt" 2>&1 || true
    if [ -n "$RUNNER" ]; then
        $RUNNER ps -a --format '{{.Names}}\t{{.Image}}\t{{.Status}}' > "$out/containers.txt" 2>&1 || true
        $RUNNER images --digests > "$out/images.txt" 2>&1 || true
        for c in "$BBS_SERVER_CONTAINER" "$BBS_NGINX_CONTAINER" "$BBS_PG_CONTAINER"; do
            $RUNNER inspect "$c" > "$out/inspect-$c.json" 2>/dev/null || true
            $RUNNER logs --tail 200 "$c" > "$out/log-$c.txt" 2>&1 || true
        done
    fi
    # DB 取证：宿主 psql 优先（生产是宿主 PG），其次容器
    local dpbin; dpbin=$(pg_bin_dir || true)
    if [ -n "$dpbin" ] && [ -x "$dpbin/psql" ]; then
        {
            echo "== server_version"
            PGPASSWORD="${BBS_DB_PASSWORD:-}" "$dpbin/psql" -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" -U "$BBS_DB_USER" \
                -d "$BBS_DB_NAME" -Atc 'show server_version' 2>&1 | head -1
            echo "== recent migrations"
            PGPASSWORD="${BBS_DB_PASSWORD:-}" "$dpbin/psql" -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" -U "$BBS_DB_USER" \
                -d "$BBS_DB_NAME" -Atc "select version, applied_at from bbs_schema_version order by applied_at desc limit 10" 2>&1
            echo "== backups schemas"
            PGPASSWORD="${BBS_DB_PASSWORD:-}" "$dpbin/psql" -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" -U "$BBS_DB_USER" \
                -d "$BBS_DB_NAME" -Atc "select nspname from pg_namespace where nspname like 'bbs_backup_%' order by 1 desc" 2>&1
        } > "$out/db.txt"
    elif [ -n "$RUNNER" ] && container_exists "$BBS_PG_CONTAINER"; then
        {
            $RUNNER exec "$BBS_PG_CONTAINER" psql -U "$BBS_DB_USER" -d "$BBS_DB_NAME" -Atc \
                "select version, applied_at from bbs_schema_version order by applied_at desc limit 10" 2>&1
            echo "== backups schemas"
            $RUNNER exec "$BBS_PG_CONTAINER" psql -U "$BBS_DB_USER" -d "$BBS_DB_NAME" -Atc \
                "select nspname from pg_namespace where nspname like 'bbs_backup_%' order by 1 desc" 2>&1
        } > "$out/db.txt"
    else
        echo "宿主与容器都没有 psql（生产是宿主 PG 时请确认 /usr/pgsql-*/bin 存在）" > "$out/db.txt"
    fi
    ls -la "$BBS_HOME/backups" > "$out/backups.txt" 2>&1 || true
    ls -1 "$BBS_HOME/packages" "$BBS_HOME/archive" > "$out/packages.txt" 2>&1 || true
    local tgz="$out.tar.gz"
    tar -czf "$tgz" -C "$(dirname "$out")" "$(basename "$out")" 2>/dev/null || die "打包诊断失败"
    local mb; mb=$(du -m "$tgz" | cut -f1)
    ok "诊断包: $tgz（${mb}MB）"
    [ "$mb" -lt 20 ] || warn "诊断包超过 20MB，请检查日志文件"
}

cmd_migrate() {
    resolve_home; load_env; detect_runner
    [ -n "$RUNNER" ] || die "未找到 podman/docker"
    [ "$DRY_RUN" = "1" ] || acquire_lock
    TOTAL_STEPS=8
    local legacy; legacy=$(readlink -f "$BBS_HOME/versions/latest" 2>/dev/null)
    # 迁移跑过一次后 versions/latest 会指向 releases/current；此时一致性基准要回到真正的旧目录
    case "$legacy" in
        */releases/*)
            legacy=$(find "$BBS_HOME/versions" -maxdepth 1 -mindepth 1 -type d ! -name latest -printf '%p\n' 2>/dev/null | sort | tail -1)
            ;;
    esac
    [ -n "$legacy" ] && [ -d "$legacy" ] || die "没有可迁移的旧版本（versions/latest 不存在）"

    step "预检"
    preflight
    step "定位并校验包"
    find_package; verify_sig "$PKG_PATH"; unpack_package "$PKG_PATH"; load_manifest "$PKG_ROOT"; verify_package_files "$PKG_ROOT"
    step "归档旧入口脚本到 legacy/"
    mkdir -p "$BBS_HOME/legacy"
    local ts; ts=$(date +%Y%m%d_%H%M%S)
    for f in deploy.sh deploy-offline.sh scripts/prod/one-key-deploy.sh scripts/prod/one_key_deploy.sh; do
        [ -f "$BBS_HOME/$f" ] || continue
        grep -q '兼容入口（2026-11-27' "$BBS_HOME/$f" 2>/dev/null && continue
        cmp -s "$BBS_HOME/$f" "$SELF_DIR/deploy.sh" 2>/dev/null && continue
        cp -a "$BBS_HOME/$f" "$BBS_HOME/legacy/$(basename "$f").$ts" \
            && info "归档 $f → legacy/$(basename "$f").$ts"
    done
    # .env 里有数据库口令：现网是 755，收成 600（不影响 root 运行）
    if [ -f "$BBS_HOME/.env" ]; then
        local perm; perm=$(stat -c '%a' "$BBS_HOME/.env" 2>/dev/null || echo "")
        case "$perm" in
            ""|600|400) ;;
            *) chmod 600 "$BBS_HOME/.env" && info ".env 权限 $perm → 600（含数据库口令）" ;;
        esac
    fi
    step "导入当前版本到 releases/"
    local legacy_id; legacy_id=$(basename "$legacy")
    local import_id="$legacy_id"
    if [ -f "$PKG_ROOT/payload/app/bbs-server.jar" ]; then
        import_id="$M_ID"
        info "使用包内 payload 作为基线（$M_ID）"
        # 迁移不得顺带改代码：包内 jar 必须与现网运行 jar 字节一致
        if [ -f "$legacy/bbs-server.jar" ]; then
            local h1 h2
            h1=$(sha256sum "$PKG_ROOT/payload/app/bbs-server.jar" | awk '{print $1}')
            h2=$(sha256sum "$legacy/bbs-server.jar" | awk '{print $1}')
            if [ "$h1" != "$h2" ]; then
                err "包内 jar 与现网运行 jar 不一致（$h1 vs $h2）"
                err "迁移包必须是「现网同版本」；要升级请另发 update 包"
                [ "$ALLOW_DOWNGRADE" = "1" ] || die "已中止（确认要在迁移同时换代码，加 --force）"
                warn "已按 --force 继续（本次迁移会同时改变运行代码）"
            else
                ok "包内 jar 与现网一致（$h1）"
            fi
        else
            warn "现网目录没有 bbs-server.jar（$legacy），跳过一致性校验"
        fi
        stage_release "$PKG_ROOT" "$import_id"
    else
        warn "包内无 payload，从现网目录导入 $legacy_id"
        mkdir -p "$BBS_HOME/releases/$import_id/app"
        cp -a --no-preserve=context "$legacy/." "$BBS_HOME/releases/$import_id/app/" || die "导入失败"
    fi
    verify_staged_release "$import_id" || die "导入结果校验失败"
    step "同步 scripts/"
    sync_scripts "$PKG_ROOT/scripts"
    # 现场如果还在用 deploy-offline.sh，把它换成兼容壳（旧文件已归档到 legacy/）
    if [ -f "$BBS_HOME/scripts/prod/deploy-offline.sh" ] && [ -f "$BBS_HOME/legacy/deploy-offline.sh.$ts" ]; then
        cp -a "$BBS_HOME/scripts/prod/deploy-offline.sh" "$BBS_HOME/deploy-offline.sh" \
            && chmod +x "$BBS_HOME/deploy-offline.sh" \
            && ok "deploy-offline.sh 已替换为兼容壳（旧文件: legacy/deploy-offline.sh.$ts）"
    fi
    step "备份（DB + 配置 + inspect）"
    PREV_RELEASE=""
    backup_before_switch ""
    step "重建容器（先起新容器，指针还没切；失败可原地退回）"
    ROLLBACK_ARMED=0
    [ -d "$BBS_HOME/releases/current" ] && [ ! -L "$BBS_HOME/releases/current" ] && die "releases/current 是真实目录，请先人工确认"
    if ! recreate_containers "$BBS_HOME/releases/$import_id"; then
        err "新容器启动失败 → 回退旧布局（指针未切换）"
        fallback_to_legacy "$legacy"
        return 1
    fi
    step "切换指针（原子）"
    switch_pointers "$import_id"
    write_current_json "$import_id" ""
    wait_health || true
    if verify_all; then
        archive_package
        cleanup_root_scripts
        ok "迁移完成：$import_id（旧 versions/ 原样保留）"
        echo "  日常升级：bash $SELF_DIR/deploy.sh        # 放好包，一条命令"
        return 0
    fi
    err "迁移后验证失败 → 自动回退旧布局"
    fallback_to_legacy "$legacy"
    return 1
}

# 旧布局（versions/<ts>/，容器挂 current/...）的健康检查：不套用 releases 那套指针断言
verify_legacy_stack() {
    local p code bad=0
    for p in "/bbs-server/" "/bbs-user/" "/bbs-admin/"; do
        code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "http://127.0.0.1:${NGINX_PORT}${p}" 2>/dev/null) || code=000
        [ "$code" = "200" ] || { bad=1; warn "旧布局 GET $p → $code"; }
    done
    prepare_upload_dir >/dev/null 2>&1
    if $RUNNER exec "$BBS_SERVER_CONTAINER" cat "$BBS_UPLOAD_DIR.deploy-mount-probe" 2>/dev/null | grep -q '^ok$'; then
        ok "旧布局：挂载探针通过"
    else
        bad=1; warn "旧布局：挂载探针失败"
    fi
    [ "$bad" = "0" ] && ok "旧布局：三端点 200"
}

# 迁移失败/主动撤销时把机器恢复成"没有迁移过"的样子（旧目录本来就没动）
fallback_to_legacy() {
    local legacy="$1"
    warn "恢复 versions/latest → $(basename "$legacy")，并用旧布局重建容器"
    ln -snf "$(basename "$legacy")" "$BBS_HOME/versions/latest"
    rm -f "$BBS_HOME/releases/current" "$BBS_HOME/src"
    recreate_legacy_stack "$legacy" || { err "旧布局容器也起不来，请按 backups/<ts>/inspect.json 手工恢复"; return 1; }
    wait_health || true
    verify_legacy_stack || warn "旧布局仍有失败项（可能本来就有问题，请看上面 WARN）"
    ok "已回退到迁移前状态；releases/ 里的失败版本保留供取证"
}

preflight() {
    local need
    for need in tar sha256sum curl; do
        command -v "$need" >/dev/null 2>&1 || die "缺少命令: $need"
    done
    [ -n "$RUNNER" ] || die "未找到 podman/docker"
    local free; free=$(disk_free_mb "$BBS_HOME")
    [ "${free:-0}" -ge 1024 ] || die "磁盘不足（$BBS_HOME 剩余 ${free}MB < 1024MB）"
    info "磁盘剩余 ${free}MB"
    # 端口：本栈容器存在就允许（下一步会 rm -f 释放）；否则必须空闲
    local p
    for p in "$BBS_SERVER_PORT" "$NGINX_PORT"; do
        if ss -ltn 2>/dev/null | awk '{print $4}' | grep -Eq "[:.]${p}$"; then
            if container_exists "$BBS_SERVER_CONTAINER"; then
                info "端口 $p 由本栈容器占用（将重建释放后复用）"
            else
                die "端口 $p 已被其他进程占用，先释放再部署"
            fi
        fi
    done
    if [ -f "$BBS_HOME/deploy.env" ]; then
        grep -q 'your_password_here' "$BBS_HOME/deploy.env" && die "deploy.env 的 BBS_DB_PASSWORD 仍是占位符"
    elif [ -f "$BBS_HOME/.env" ]; then
        grep -q 'your_password_here' "$BBS_HOME/.env" && die ".env 的 BBS_DB_PASSWORD 仍是占位符"
    fi
    if command -v getenforce >/dev/null 2>&1; then
        info "SELinux: $(getenforce 2>/dev/null)"
    fi
    # 数据库工具：生产是宿主 PG（不在 PATH，通常在 /usr/pgsql-NN/bin）
    local pbin; pbin=$(pg_bin_dir || true)
    if [ -n "$pbin" ]; then
        local sver=""
        [ -x "$pbin/psql" ] && sver=$(PGPASSWORD="${BBS_DB_PASSWORD:-}" "$pbin/psql" -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" \
            -U "$BBS_DB_USER" -d "$BBS_DB_NAME" -Atc 'show server_version' 2>/dev/null | head -1)
        info "DB 工具: $pbin/pg_dump（$("$pbin/pg_dump" --version 2>/dev/null | head -1)）"
        [ -n "$sver" ] && info "DB 版本: PostgreSQL $sver（$BBS_DB_HOST:$BBS_DB_PORT/$BBS_DB_NAME）" \
            || warn "psql 连不上数据库（将无法做迁移前备份）：检查 $pbin/psql 与 .env 账号"
    else
        if [ "$SKIP_BACKUP" = "1" ]; then
            warn "未找到 pg_dump；已显式 --skip-backup，本次没有数据库退路"
        else
            die "未找到 pg_dump（PATH 与 /usr/pgsql-*/bin 都没有）。设 BBS_PG_BIN 或加 --skip-backup 后重跑"
        fi
    fi
    ok "预检通过"
}

cmd_deploy() {
    resolve_home; load_env; detect_runner
    TOTAL_STEPS=6
    if ! find_package soft; then
        info "没有待部署的包：只做校验，不改动任何东西"
        cmd_status
        echo ""
        verify_all
        return $?
    fi
    [ "$DRY_RUN" = "1" ] || acquire_lock
    preflight
    info "升级包: $PKG_PATH${PKG_ORIGIN:+（来源: $PKG_ORIGIN）}"
    verify_sig "$PKG_PATH"
    unpack_package "$PKG_PATH"
    load_manifest "$PKG_ROOT"
    verify_package_files "$PKG_ROOT"

    if [ "$DRY_RUN" = "1" ]; then
        echo ""; ok "预检通过（--dry-run）：包=$M_ID 当前=$(current_release || echo 无)"
        echo "  计划: 备份 → 落 releases/$M_ID → 校验 → 切指针 → 重建容器 → verify（失败自动回滚）"
        return 0
    fi

    local cur; cur=$(current_release)
    local prev_before; prev_before=$(sanitize_prev "$(prev_release_from_state)" "$M_ID")
    PREV_RELEASE="$cur"
    if [ -n "$cur" ] && [ "$cur" != "$M_ID" ] && [ "$ALLOW_DOWNGRADE" != "1" ]; then
        # 版本号前缀为时间戳，可直接按字典序比较
        if [ "$(printf '%s\n%s\n' "$cur" "$M_ID" | sort | head -1)" = "$M_ID" ]; then
            die "包版本 $M_ID 低于当前 $cur；确认要降级请加 --allow-downgrade"
        fi
    fi
    step "部署前备份"
    backup_before_switch "$cur"

    step "落盘新版本"
    stage_release "$PKG_ROOT" "$M_ID"
    verify_staged_release "$M_ID" || die "新版本预校验失败，未切换（当前版本未受影响）"

    step "同步 scripts/"
    sync_scripts "$PKG_ROOT/scripts"

    step "切换指针（原子）"
    if [ -n "$cur" ] && [ "$cur" = "$M_ID" ]; then
        warn "同版本重跑 → 按修复处理，不新建版本"
    fi
    switch_pointers "$M_ID"
    ROLLBACK_ARMED=1

    step "重建容器（真实路径）"
    if ! recreate_containers "$BBS_HOME/releases/$M_ID"; then
        err "容器启动失败 → 自动回滚"
        auto_rollback "$cur" "$M_ID" "$prev_before" || err "自动回滚也失败，请按 交付现状.md §5.5 手工处理"
        return 1
    fi
    wait_health || true

    step "部署后验证"
    if verify_all; then
        rm -f "$BBS_HOME/releases/$M_ID/.failed"
        if [ -n "$cur" ] && [ "$cur" = "$M_ID" ]; then
            write_current_json "$M_ID" "$prev_before"
        else
            write_current_json "$M_ID" "$cur"
        fi
        archive_package
        cleanup_root_scripts
        ok "部署成功：$M_ID（耗时 $(( $(date +%s) - START_TS ))s）"
        echo "  回滚: bash $SELF_DIR/deploy.sh once rollback"
        return 0
    fi

    err "验证失败 → 自动回滚"
    auto_rollback "$cur" "$M_ID" "$prev_before" || err "自动回滚也失败，请按 交付现状.md §5.5 手工处理"
    return 1
}

archive_package() {
    local dest="$BBS_HOME/packages"
    mkdir -p "$dest" || { warn "无法创建归档目录: $dest"; return 1; }
    local mode="mv"
    [ "$PKG_ORIGIN" = "removable" ] && mode="cp"
    [ -f "$PKG_PATH.sha256" ] && { $mode -a "$PKG_PATH.sha256" "$dest/" 2>/dev/null || cp -a "$PKG_PATH.sha256" "$dest/" 2>/dev/null; }
    $mode -a "$PKG_PATH" "$dest/" 2>/dev/null || cp -a "$PKG_PATH" "$dest/" 2>/dev/null
    if [ -f "$dest/$(basename "$PKG_PATH")" ]; then
        ok "包已归档: $dest/$(basename "$PKG_PATH")（${mode}）"
        return 0
    fi
    warn "包归档失败（保留原位置）: $PKG_PATH"
    return 1
}

auto_rollback() {
    local cur="$1" failed="${2:-}" prev_before="${3:-}"
    ROLLBACK_ARMED=0
    if [ -z "$cur" ] || [ ! -d "$BBS_HOME/releases/$cur" ]; then
        err "没有可回滚的上一版（首次安装失败，未归档包，便于重试）"
        return 1
    fi
    # 坏版本打标记：以后 `once rollback --to` 不会误把它当目标
    [ -n "$failed" ] && [ -d "$BBS_HOME/releases/$failed" ] && : > "$BBS_HOME/releases/$failed/.failed"
    prev_before=$(sanitize_prev "$prev_before" "$failed")
    warn "切回 $cur 并重建容器"
    switch_pointers "$cur" || return 1
    recreate_containers "$BBS_HOME/releases/$cur" || return 1
    wait_health || true
    if verify_all; then
        # previous 保持"上一版好版本"，绝不写成刚失败的那个
        write_current_json "$cur" "$prev_before" "$failed"
        ok "已回滚到 $cur（失败版本 ${failed:-?} 标记 .failed 并保留在 releases/；包未归档）"
        return 0
    fi
    err "回滚后验证仍失败，保留现场：$BBS_HOME/log"
    return 1
}

cmd_rollback() {
    resolve_home; load_env; detect_runner
    [ "$DRY_RUN" = "1" ] || acquire_lock
    local cur prev
    cur=$(current_release)
    prev="${ROLLBACK_TO:-$(prev_release_from_state)}"
    if [ -z "$prev" ]; then
        # 迁移后的第一次回滚：没有"上一版 release"，但机器上还有旧 versions/<ts>
        local legacy
        legacy=$(find "$BBS_HOME/versions" -maxdepth 1 -mindepth 1 -type d ! -name latest -printf '%p\n' 2>/dev/null | sort | tail -1)
        if [ -n "$legacy" ] && [ -d "$legacy" ] && [ -n "$cur" ]; then
            warn "没有上一版 release；检测到迁移前的旧布局，执行「撤销迁移」→ $(basename "$legacy")"
            if [ "$DRY_RUN" = "1" ]; then ok "dry-run：未执行"; return 0; fi
            fallback_to_legacy "$legacy" || die "撤销迁移失败，请按 backups/<ts>/inspect.json 手工恢复"
            rm -f "$BBS_HOME/current.json"
            ok "已撤销迁移（容器回到 current/... 旧挂载，releases/ 保留）"
            return 0
        fi
        die "current.json 里没有上一版；用 --to <release-id> 指定"
    fi
    [ -d "$BBS_HOME/releases/$prev" ] || die "上一版目录不存在: releases/$prev"
    if [ -f "$BBS_HOME/releases/$prev/.failed" ] && [ "$ALLOW_DOWNGRADE" != "1" ]; then
        die "目标版本 $prev 上次验证失败（有 .failed 标记）；确认要回滚到它请加 --force"
    fi
    if [ -f "$BBS_HOME/releases/$cur/manifest.json" ]; then
        local compat; compat=$(sed -n 's/.*"db_compat"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$BBS_HOME/releases/$cur/manifest.json" | head -1)
        if [ "$compat" = "forward-only" ] && [ "$ALLOW_DOWNGRADE" != "1" ]; then
            die "当前版本 db_compat=forward-only，回滚会让旧代码跑在新库结构上；确认后加 --force（并先准备 DB 备份）"
        fi
    fi
    info "回滚: $cur → $prev"
    if [ "$DRY_RUN" = "1" ]; then ok "dry-run：未执行"; return 0; fi
    switch_pointers "$prev" || die "切换指针失败"
    recreate_containers "$BBS_HOME/releases/$prev" || die "重建容器失败"
    wait_health || true
    verify_all || die "回滚后验证失败，请检查 $BBS_HOME/log"
    rm -f "$BBS_HOME/releases/$prev/.failed"
    write_current_json "$prev" "$(sanitize_prev "$cur" "$prev")" ""
    ok "已回滚到 $prev"
}

cmd_repair() {
    resolve_home; load_env; detect_runner
    acquire_lock
    local cur; cur=$(current_release)
    [ -n "$cur" ] || die "没有当前版本，无法修复"
    info "修复：按 releases/$cur 重建容器"
    prepare_upload_dir
    recreate_containers "$BBS_HOME/releases/$cur" || die "重建失败"
    wait_health || true
    verify_all || die "修复后验证仍失败"
    cleanup_root_scripts
    ok "修复完成"
}

cmd_restart() {
    resolve_home; load_env; detect_runner
    acquire_lock
    local cur; cur=$(current_release)
    [ -n "$cur" ] || die "没有当前版本"
    if [ "$SOFT_RESTART" = "1" ]; then
        info "软重启（podman restart，不换镜像/不重读 env）"
        $RUNNER restart "$BBS_SERVER_CONTAINER" "$BBS_NGINX_CONTAINER" || die "restart 失败"
    else
        prepare_upload_dir
        recreate_containers "$BBS_HOME/releases/$cur" || die "重建失败"
    fi
    wait_health || true
    verify_all || die "重启后验证失败"
    ok "重启完成（$cur）"
}

cmd_sync_scripts() {
    resolve_home
    if [ -n "$PKG_PATH" ]; then
        unpack_package "$PKG_PATH"
        sync_scripts "$PKG_ROOT/scripts"
        return $?
    fi
    # 部署根里执行时，源和目标是同一个目录：只做根目录归位，不做 rsync 自拷
    if [ -d "$SELF_DIR/scripts" ] && [ "$(cd "$SELF_DIR/scripts" 2>/dev/null && pwd)" = "$(cd "$BBS_HOME/scripts" 2>/dev/null && pwd)" ]; then
        cleanup_root_scripts
        ok "已执行根目录脚本归位（未改动 scripts/）"
        return 0
    fi
    sync_scripts "$SELF_DIR/scripts"
}

cmd_logs() {
    resolve_home; load_env; detect_runner
    $RUNNER logs --tail 200 -f "$BBS_SERVER_CONTAINER" 2>/dev/null || die "容器不存在: $BBS_SERVER_CONTAINER"
}

# ---------------------------------------------------------------- main ------
[ "$ONCE" = "1" ] && info "一次性操作模式（once）：本次不走日常升级路径，执行完不改变日常用法"
case "$CMD" in
    auto)
        resolve_home
        if [ -d "$BBS_HOME/releases/current" ] || [ -L "$BBS_HOME/releases/current" ]; then
            cmd_deploy
        elif [ -L "$BBS_HOME/versions/latest" ] || [ -d "$BBS_HOME/versions" ]; then
            info "检测到旧布局（versions/latest），进入结构迁移"
            cmd_migrate
        else
            info "空目录，进入首次安装"
            cmd_deploy
        fi
        ;;
    deploy|upgrade|install) cmd_deploy ;;
    migrate) cmd_migrate ;;
    verify) cmd_verify ;;
    status) cmd_status ;;
    diag) cmd_diag ;;
    rollback) cmd_rollback ;;
    repair) cmd_repair ;;
    restart) cmd_restart ;;
    sync-scripts) cmd_sync_scripts ;;
    logs) cmd_logs ;;
    *) usage ;;
esac
