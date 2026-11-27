#!/bin/bash
# ============================================================================
# 大千智荟论坛 · 现场只读采集（U 盘往返用）
#
# 用途：在**生产机**上一次性采集部署现状，回传到构建机，用于定稿迁移方案。
# 原则：只读。除本脚本自己的输出目录（默认 ./回传/<主机>-<时间戳>/）外，
#       不写任何文件、不启停容器、不改配置、不连库写数据。
#
# 用法（生产机，U 盘里的本目录）：
#   sudo bash collect.sh                 # 输出到 ./回传/<主机>-<时间戳>/
#   sudo bash collect.sh --out /root/回传
#   sudo bash collect.sh --home /data/bbs          # 非默认部署根
#   sudo bash collect.sh --with-probe              # 额外做一次挂载探针（会写 1 个临时文件，自动删）
#   bash collect.sh --help
#
# 退出码：0=采集完成（可能有 MISSING 项，见 19-summary.txt）
#         3=环境不满足（不是部署根 / 无容器运行时）
# ============================================================================
set -u

HOME_DIR="/data/bbs"
OUT_BASE="$(cd "$(dirname "$0")" && pwd)/回传"
WITH_PROBE=0
RUNNER=""
TS="$(date +%Y%m%d_%H%M%S)"
HOST="$(hostname 2>/dev/null || echo unknown)"
OUT=""
MISSING=0
NOTES=""

c_info() { printf '[INFO] %s\n' "$*"; }
c_ok()   { printf '[ OK ] %s\n' "$*"; }
c_warn() { printf '[WARN] %s\n' "$*"; }
c_err()  { printf '[FAIL] %s\n' "$*" >&2; }

usage() { sed -n '3,/^set -u/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 0; }

while [ $# -gt 0 ]; do
    case "$1" in
        --home) HOME_DIR="${2:-}"; shift 2 ;;
        --home=*) HOME_DIR="${1#*=}"; shift ;;
        --out) OUT_BASE="${2:-}"; shift 2 ;;
        --out=*) OUT_BASE="${1#*=}"; shift ;;
        --with-probe) WITH_PROBE=1; shift ;;
        -h|--help) usage ;;
        *) c_err "未知参数: $1"; exit 2 ;;
    esac
done

# ---------- 环境 ----------
if command -v podman >/dev/null 2>&1; then RUNNER=podman
elif command -v docker >/dev/null 2>&1; then RUNNER=docker
fi
[ -n "$RUNNER" ] || { c_err "未找到 podman/docker"; exit 3; }
[ -d "$HOME_DIR" ] || { c_err "部署根不存在: $HOME_DIR"; exit 3; }
if [ ! -d "$HOME_DIR/versions" ] && [ ! -d "$HOME_DIR/releases" ] && [ ! -f "$HOME_DIR/.env" ]; then
    c_err "$HOME_DIR 看起来不是 BBS 部署根（无 versions/ releases/ .env）；确认后用 --home 指定"
    exit 3
fi

OUT="$OUT_BASE/${HOST}-${TS}"
mkdir -p "$OUT" || { c_err "无法创建输出目录: $OUT"; exit 3; }
c_info "输出目录: $OUT"

# 读取配置（只解析，不 source，避免执行 env 里的任意命令）
env_get() {
    local key="$1" def="${2:-}" v=""
    for f in "$HOME_DIR/.env" "$HOME_DIR/deploy.env"; do
        [ -f "$f" ] || continue
        v=$(grep -E "^${key}=" "$f" 2>/dev/null | head -1 | cut -d= -f2- | tr -d '\r' | tr -d '"' | tr -d "'")
        [ -n "$v" ] && { echo "$v"; return; }
    done
    echo "$def"
}
SERVER_PORT="$(env_get BBS_SERVER_PORT 60000)"
NGINX_PORT="$(env_get NGINX_PORT 60001)"
DB_PORT="$(env_get BBS_DB_PORT 15432)"
DB_HOST="$(env_get BBS_DB_HOST 127.0.0.1)"
DB_NAME="$(env_get BBS_DB_NAME bbs)"
DB_USER="$(env_get BBS_DB_USER work_flow)"
DB_PASS="$(env_get BBS_DB_PASSWORD "")"
PG_CONTAINER="$(env_get BBS_PG_CONTAINER bbs-postgres)"
UPLOAD_DIR="$(env_get BBS_UPLOAD_DIR "$HOME_DIR/bbsUpload")"
SERVER_C="$(env_get BBS_SERVER_CONTAINER bbs-server)"
NGINX_C="$(env_get BBS_NGINX_CONTAINER bbs-nginx)"

mark_missing() { MISSING=$((MISSING + 1)); NOTES="${NOTES}  - MISSING: $1
"; }

# ---------- 采集 ----------
c_info "1/12 系统"
{
    echo "== collected_at"; date -Is
    echo "== hostname"; hostname
    echo "== kernel"; uname -a
    echo "== release"; cat /etc/redhat-release 2>/dev/null || cat /etc/os-release 2>/dev/null | head -3 || true
    echo "== uptime"; uptime
    echo "== whoami"; id
    echo "== timezone"; date +%Z; cat /etc/timezone 2>/dev/null || true
} > "$OUT/00-system.txt" 2>&1
[ -s "$OUT/00-system.txt" ] || mark_missing "system"

# 宿主 PG 工具（RHEL7 的 pgdg 不在 PATH）
pg_bin_dir() {
    if [ -n "${BBS_PG_BIN:-}" ] && [ -x "$BBS_PG_BIN/pg_dump" ]; then echo "$BBS_PG_BIN"; return 0; fi
    if command -v pg_dump >/dev/null 2>&1; then dirname "$(command -v pg_dump)"; return 0; fi
    for d in /usr/pgsql-*/bin /usr/lib/postgresql/*/bin /opt/pgsql/*/bin /opt/postgresql/*/bin; do
        [ -x "$d/pg_dump" ] && { echo "$d"; return 0; }
    done
    return 1
}
PGBIN="$(pg_bin_dir 2>/dev/null || true)"
[ -z "$PGBIN" ] && { BBS_PG_BIN="$(env_get BBS_PG_BIN "")"; PGBIN="$(pg_bin_dir 2>/dev/null || true)"; }

c_info "2/12 SELinux"
{
    echo "== getenforce"; getenforce 2>/dev/null || echo "n/a (非 RHEL 系)"
    echo "== sestatus"; sestatus 2>/dev/null | head -5 || echo "n/a"
    echo "== upload dir context"; ls -Z "$UPLOAD_DIR" 2>/dev/null | head -5
    echo "== bbs home context"; ls -Zd "$HOME_DIR" 2>/dev/null
} > "$OUT/01-selinux.txt" 2>&1

c_info "3/12 磁盘/内存"
{
    echo "== df -h"; df -hP 2>/dev/null
    echo "== df home"; df -hP "$HOME_DIR" 2>/dev/null
    echo "== inodes"; df -iP "$HOME_DIR" 2>/dev/null
    echo "== mem"; free -m 2>/dev/null
    echo "== du"; du -sh "$HOME_DIR"/* 2>/dev/null | sort -h | tail -20
} > "$OUT/02-disk.txt" 2>&1

c_info "4/12 部署根与指针"
{
    echo "== ls home"; ls -la "$HOME_DIR" 2>/dev/null
    echo "== versions"; ls -la "$HOME_DIR/versions" 2>/dev/null
    echo "== versions/latest"; readlink "$HOME_DIR/versions/latest" 2>/dev/null
    echo "== current"; readlink "$HOME_DIR/current" 2>/dev/null
    echo "== readlink -f"; readlink -f "$HOME_DIR/current" "$HOME_DIR/versions/latest" 2>/dev/null
    echo "== releases"; ls -la "$HOME_DIR/releases" 2>/dev/null
    echo "== releases/current"; readlink "$HOME_DIR/releases/current" 2>/dev/null
    echo "== src"; readlink "$HOME_DIR/src" 2>/dev/null
    echo "== archive"; ls -la "$HOME_DIR/archive" 2>/dev/null | tail -20
    echo "== packages"; ls -la "$HOME_DIR/packages" 2>/dev/null
    echo "== backups"; ls -la "$HOME_DIR/backups" 2>/dev/null | tail -20
    echo "== log"; ls -la "$HOME_DIR/logs" "$HOME_DIR/log" 2>/dev/null | head -20
    echo "== root tarballs"; ls -la "$HOME_DIR"/*.tar.gz 2>/dev/null
    echo "== legacy"; ls -la "$HOME_DIR/legacy" 2>/dev/null
} > "$OUT/03-home.txt" 2>&1

c_info "5/12 入口脚本"
{
    echo "== deploy.sh"; ls -la "$HOME_DIR/deploy.sh" 2>/dev/null; head -25 "$HOME_DIR/deploy.sh" 2>/dev/null
    echo "== deploy-offline.sh"; ls -la "$HOME_DIR/deploy-offline.sh" 2>/dev/null
    echo "== scripts/prod"; ls -la "$HOME_DIR/scripts/prod" 2>/dev/null
    echo "== scripts tree"; find "$HOME_DIR/scripts" -maxdepth 2 -type f 2>/dev/null | head -60
    echo "== hashes"; sha256sum "$HOME_DIR"/deploy.sh "$HOME_DIR"/deploy-offline.sh "$HOME_DIR"/scripts/prod/*.sh 2>/dev/null
} > "$OUT/04-entrypoints.txt" 2>&1

c_info "6/12 容器与镜像"
$RUNNER ps -a --format '{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.CreatedAt}}' > "$OUT/05-containers.txt" 2>&1 || mark_missing "podman ps"
$RUNNER images --digests > "$OUT/06-images.txt" 2>&1 || mark_missing "podman images"
{
    echo "== podman version"; $RUNNER --version
    echo "== rootless"; $RUNNER info --format '{{.Host.Security.Rootless}}' 2>/dev/null
    echo "== storage"; $RUNNER info --format '{{.Store.GraphRoot}}' 2>/dev/null
    echo "== restart policy"; $RUNNER inspect "$SERVER_C" "$NGINX_C" --format '{{.Name}} {{.HostConfig.RestartPolicy.Name}}' 2>/dev/null
} > "$OUT/07-runtime.txt" 2>&1

for c in "$SERVER_C" "$NGINX_C" "$PG_CONTAINER"; do
    $RUNNER inspect "$c" > "$OUT/inspect-$c.json" 2>/dev/null || mark_missing "inspect $c"
done

{
    for c in "$SERVER_C" "$NGINX_C" "$PG_CONTAINER"; do
        echo "== $c"
        $RUNNER inspect "$c" --format '{{.ImageName}}
network={{.HostConfig.NetworkMode}}
restart={{.HostConfig.RestartPolicy.Name}}
status={{.State.Status}} running={{.State.Running}} restarts={{.RestartCount}}' 2>/dev/null
        echo "-- mounts (source -> destination)"
        $RUNNER inspect "$c" --format '{{range .Mounts}}{{.Source}} -> {{.Destination}} ({{.Mode}})
{{end}}' 2>/dev/null
        echo "-- env (masked)"
        $RUNNER inspect "$c" --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null \
            | sed -E 's/(PASSWORD|PASSWD|SECRET|TOKEN)=.*/\1=***/'
        echo ""
    done
} > "$OUT/08-mounts-env.txt" 2>&1

c_info "7/12 端口"
{
    echo "== listening"; ss -ltnp 2>/dev/null | egrep ":($SERVER_PORT|$NGINX_PORT|$DB_PORT|5432)\b"
    echo "== firewall"; (firewall-cmd --list-ports 2>/dev/null; firewall-cmd --state 2>/dev/null) || echo "n/a"
    echo "== iptables (head)"; iptables -S 2>/dev/null | head -20 || echo "n/a"
} > "$OUT/09-ports.txt" 2>&1

c_info "8/12 配置（脱敏）"
{
    for f in "$HOME_DIR/.env" "$HOME_DIR/deploy.env"; do
        [ -f "$f" ] || continue
        echo "== $f (masked, $(stat -c '%a %U:%G %s bytes %y' "$f" 2>/dev/null))"
        sed -E 's/(PASSWORD|PASSWD|SECRET|TOKEN|KEY)=.*/\1=***/' "$f" 2>/dev/null
        echo "== CRLF?"; file "$f" 2>/dev/null; grep -c $'\r' "$f" 2>/dev/null | sed 's/^/crlf_lines=/'
    done
} > "$OUT/10-env-masked.txt" 2>&1

c_info "9/12 当前产物"
CUR="$(readlink -f "$HOME_DIR/current" 2>/dev/null)"
# 兼容两种布局：旧 versions/<ts>/bbs-server.jar；新 releases/<id>/app/bbs-server.jar
JAR="$(readlink -f "$HOME_DIR/current/bbs-server.jar" 2>/dev/null)"
[ -f "$JAR" ] || JAR="$(readlink -f "$HOME_DIR/releases/current/app/bbs-server.jar" 2>/dev/null)"
[ -f "$JAR" ] || JAR="$(find "$HOME_DIR/releases" "$HOME_DIR/versions" -maxdepth 3 -name bbs-server.jar 2>/dev/null | head -1)"
{
    echo "== current realpath"; echo "$CUR"
    echo "== jar path"; echo "$JAR"
    echo "== jar"; ls -la "$JAR" 2>/dev/null
    echo "== jar sha256"; sha256sum "$JAR" 2>/dev/null
    echo "== jar magic"; head -c 2 "$JAR" 2>/dev/null; echo
    echo "== dist"; ls -la "$(dirname "$JAR")/bbs-ui/dist/index.html" "$(dirname "$JAR")/bbs-admin-ui/dist/index.html" 2>/dev/null
    echo "== version.txt"; cat "$(dirname "$JAR")/version.txt" 2>/dev/null
    echo "== manifest"; cat "$(dirname "$JAR")/manifest.json" 2>/dev/null | head -40
} > "$OUT/11-current-artifact.txt" 2>&1
[ -f "$JAR" ] || mark_missing "bbs-server.jar（两种布局都没找到）"

c_info "10/12 HTTP 与挂载探针"
{
    echo "== backend $SERVER_PORT"; curl -s -o /dev/null -w '%{http_code}\n' --max-time 10 "http://127.0.0.1:${SERVER_PORT}/bbs-server/" 2>/dev/null
    echo "== user $NGINX_PORT";     curl -s -o /dev/null -w '%{http_code}\n' --max-time 10 "http://127.0.0.1:${NGINX_PORT}/bbs-user/" 2>/dev/null
    echo "== admin $NGINX_PORT";    curl -s -o /dev/null -w '%{http_code}\n' --max-time 10 "http://127.0.0.1:${NGINX_PORT}/bbs-admin/" 2>/dev/null
    echo "== file service probe (GET .deploy-mount-probe)"
    curl -s -o /dev/null -w '%{http_code}\n' --max-time 10 "http://127.0.0.1:${NGINX_PORT}/bbs-server/files/.deploy-mount-probe" 2>/dev/null
} > "$OUT/12-http.txt" 2>&1

if [ "$WITH_PROBE" = "1" ]; then
    PROBE="${UPLOAD_DIR%/}/.collect-probe-$$"
    if echo ok > "$PROBE" 2>/dev/null; then
        {
            echo "== host wrote: $PROBE"
            echo "== container read:"
            $RUNNER exec "$SERVER_C" cat "$PROBE" 2>&1
            rm -f "$PROBE"
        } > "$OUT/13-mount-probe.txt" 2>&1
    else
        echo "无法写入 $UPLOAD_DIR（权限？）" > "$OUT/13-mount-probe.txt"
    fi
else
    echo "未启用（默认只读）。需要时加 --with-probe（会写 1 个临时文件并自动删除）" > "$OUT/13-mount-probe.txt"
fi

c_info "11/12 数据库（只读 SELECT）"
{
    if [ -n "$PGBIN" ] && [ -x "$PGBIN/psql" ]; then
        echo "== tool: $PGBIN/psql（$("$PGBIN/psql" --version 2>/dev/null)）"
        echo "== pg_dump: $("$PGBIN/pg_dump" --version 2>/dev/null || echo 缺失)"
        run_q() { PGPASSWORD="$DB_PASS" "$PGBIN/psql" -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -A -t -c "$1" 2>&1; }
        echo "== server version"; run_q "select version()"
        echo "== db size";       run_q "select pg_size_pretty(pg_database_size('$DB_NAME'))"
        echo "== migrations(top10)"; run_q "select version, applied_at from bbs_schema_version order by applied_at desc limit 10"
        echo "== migration count";   run_q "select count(*) from bbs_schema_version"
        echo "== backup schemas";    run_q "select nspname from pg_namespace where nspname like 'bbs_backup_%' order by 1 desc"
        echo "== top tables";        run_q "select relname, n_live_tup from pg_stat_user_tables order by n_live_tup desc limit 15"
        echo "== charset";           run_q "select datname, pg_encoding_to_char(encoding), datcollate from pg_database where datname='$DB_NAME'"
    elif $RUNNER inspect "$PG_CONTAINER" >/dev/null 2>&1; then
        echo "== tool: container $PG_CONTAINER"
        P() { $RUNNER exec "$PG_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -A -t -c "$1" 2>&1; }
        echo "== server version"; P "select version()"
        echo "== db size";       P "select pg_size_pretty(pg_database_size('$DB_NAME'))"
        echo "== migrations(top10)"; P "select version, applied_at from bbs_schema_version order by applied_at desc limit 10"
        echo "== migration count";   P "select count(*) from bbs_schema_version"
        echo "== backup schemas";    P "select nspname from pg_namespace where nspname like 'bbs_backup_%' order by 1 desc"
        echo "== top tables";        P "select relname, n_live_tup from pg_stat_user_tables order by n_live_tup desc limit 15"
        echo "== charset";           P "select datname, pg_encoding_to_char(encoding), datcollate from pg_database where datname='$DB_NAME'"
    else
        echo "未找到 psql（PATH 与 /usr/pgsql-*/bin）也没有容器 $PG_CONTAINER"
        echo "生产是宿主 PG 时属正常：请手工补一次 pg_dump/psql 探测，或设置 BBS_PG_BIN"
    fi
} > "$OUT/14-db.txt" 2>&1
grep -q 'PostgreSQL' "$OUT/14-db.txt" 2>/dev/null || mark_missing "PG 查询（无 psql/容器，见 14-db.txt 说明）"

c_info "12/12 日志与备份"
{
    echo "== logs (last 200, errors only)"
    $RUNNER logs --tail 200 "$SERVER_C" 2>&1 | grep -iE 'error|exception|fatal|oom' | tail -30
    echo ""
    echo "== restart count"; $RUNNER inspect "$SERVER_C" --format '{{.RestartCount}} {{.State.StartedAt}}' 2>/dev/null
    echo "== started at";    $RUNNER inspect "$SERVER_C" --format '{{.State.StartedAt}}' 2>/dev/null
} > "$OUT/15-logs.txt" 2>&1

{
    echo "== backups dir"; ls -la "$HOME_DIR/backups" 2>/dev/null || echo "无 backups 目录（说明从未做过宿主机备份）"
    echo "== archive dir"; ls -la "$HOME_DIR/archive" 2>/dev/null | tail -10
    echo "== base-images"; ls -la "$HOME_DIR/base-images" 2>/dev/null
} > "$OUT/16-backups.txt" 2>&1

# ---------- 汇总 ----------
{
    echo "采集时间: $(date -Is)"
    echo "部署根  : $HOME_DIR"
    echo "输出目录: $OUT"
    echo ""
    echo "== 关键判读 =="
    echo "SELinux       : $(getenforce 2>/dev/null || echo n/a)"
    echo "容器运行时    : $RUNNER $($RUNNER --version 2>/dev/null | head -1)"
    echo "后端端口/HTTP : $SERVER_PORT / $(sed -n 's/^== backend .*//p' "$OUT/12-http.txt" 2>/dev/null | head -1)$(awk '/^== backend/{getline; print; exit}' "$OUT/12-http.txt" 2>/dev/null)"
    echo "Nginx 端口    : $NGINX_PORT"
    echo "当前版本      : $CUR"
    echo "当前 jar      : ${JAR:-未找到}"
    echo "PG 工具       : ${PGBIN:-未找到（宿主 PG 时需 /usr/pgsql-*/bin 或 BBS_PG_BIN）}"
    echo "迁移就绪条件  : $( [ -f "$JAR" ] && echo 'jar 就绪（可用于迁移包一致性比对）' || echo '缺 jar，需人工确认' )"
    echo ""
    echo "== 与脚本口径的差异检查（人工确认） =="
    grep -E 'network=|restart=' "$OUT/08-mounts-env.txt" 2>/dev/null | sed 's/^/  /'
    echo "  端口是否 $SERVER_PORT/$NGINX_PORT：$(grep -qE ":($SERVER_PORT|$NGINX_PORT)\b" "$OUT/09-ports.txt" 2>/dev/null && echo yes || echo check)"
    echo "  挂载是否仍用兼容链：$(grep -qE '/(current|versions)/' "$OUT/08-mounts-env.txt" 2>/dev/null && echo 'yes（历史布局）' || echo 'no（已是真实路径）')"
    echo ""
    echo "== MISSING 项（$MISSING） =="
    printf '%s' "$NOTES"
} > "$OUT/19-summary.txt" 2>&1

# ---------- 打包回传 ----------
TGZ="$OUT_BASE/${HOST}-${TS}.tar.gz"
if tar -czf "$TGZ" -C "$OUT_BASE" "${HOST}-${TS}" 2>/dev/null; then
    sha256sum "$TGZ" | awk '{print toupper($1)}' > "$TGZ.sha256"
    c_ok "回传包: $TGZ"
    c_ok "SHA256 : $(cat "$TGZ.sha256")"
else
    c_warn "打包失败，直接回传目录: $OUT"
fi

echo ""
echo "================ 采集完成 ================"
echo "MISSING 项: $MISSING（详见 $OUT/19-summary.txt）"
echo "把 $OUT_BASE 整个目录拷回 U 盘即可（不要只拷 .tar.gz 里的部分文件）"
[ "$MISSING" -eq 0 ] || echo "注意：有 MISSING 项，回传后需人工核对（不是失败，是没采到）"
exit 0
