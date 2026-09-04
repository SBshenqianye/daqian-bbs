#!/bin/bash
# ============================================
# PostgreSQL 数据库备份/恢复脚本
# 自动检测环境：容器 or 宿主机原生 PostgreSQL
# 容器名从 .env 的 BBS_PG_CONTAINER 读取，未配置则自动扫描
# 用法:
#   bash scripts/ops/pg-backup.sh                          # 备份到当前目录
#   bash scripts/ops/pg-backup.sh --restore <file.sql.gz>  # 恢复指定备份
#   bash scripts/ops/pg-backup.sh --verify [file.sql.gz]   # 验证备份（自动对比行数）
#   bash scripts/ops/pg-backup.sh --env                    # 显示当前检测到的环境
# ============================================
set -e

GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERR]${NC} $1"; }

# ---------- 加载 .env ----------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../../.env"
if [ -f "$ENV_FILE" ]; then
    while IFS= read -r line; do
        case "$line" in
            \#*|"") continue ;;
            *) eval "export $line" 2>/dev/null ;;
        esac
    done < "$ENV_FILE"
    info "已加载 .env: $ENV_FILE"
fi

# ---------- 读取配置（.env 优先，否则用默认值）----------
DB_HOST="${BBS_DB_HOST:-127.0.0.1}"
DB_PORT="${BBS_DB_PORT:-15432}"
DB_NAME="${BBS_DB_NAME:-bbs}"
DB_USER="${BBS_DB_USER:-work_flow}"
DB_PASS="${BBS_DB_PASSWORD:-}"

# 容器内 PG 监听端口（可能不是默认 5432，需与容器内 postgres -p 一致）
# 优先 .env 配置，否则自动检测
detect_pg_port() {
    if [ -n "$BBS_PG_INTERNAL_PORT" ]; then
        echo "$BBS_PG_INTERNAL_PORT"
        return
    fi
    # 自动检测：从容器内 postgres 进程参数中提取端口
    local port
    port=$($RUNNER exec "$PG_CONTAINER" sh -c 'cat /proc/1/cmdline 2>/dev/null | tr "\0" "\n" | grep -A1 "^-p$" | tail -1' 2>/dev/null)
    if [ -n "$port" ] && [ "$port" -gt 0 ] 2>/dev/null; then
        echo "$port"
        return
    fi
    # 备选：直接查 postgres 进程
    port=$($RUNNER exec "$PG_CONTAINER" ps aux 2>/dev/null | grep 'postgres -p' | grep -oP '\-p \K[0-9]+' | head -1)
    if [ -n "$port" ] && [ "$port" -gt 0 ] 2>/dev/null; then
        echo "$port"
        return
    fi
    # 兜底：默认 5432
    echo "5432"
}
PG_INTERNAL_PORT=""
# 在 detect_env 之后、实际使用前调用

# ---------- 检测运行时 ----------
RUNNER=""
if command -v podman >/dev/null 2>&1; then
    RUNNER="podman"
elif command -v docker >/dev/null 2>&1; then
    RUNNER="docker"
fi

# ---------- 自动发现 PostgreSQL 容器 ----------
find_pg_container() {
    # 优先用 .env 配置的容器名
    if [ -n "$BBS_PG_CONTAINER" ]; then
        if $RUNNER ps --filter "name=^${BBS_PG_CONTAINER}$" --filter "status=running" 2>/dev/null | grep -q "$BBS_PG_CONTAINER"; then
            echo "$BBS_PG_CONTAINER"
            return
        fi
        warn ".env 配置的容器 $BBS_PG_CONTAINER 未运行，尝试自动扫描..."
    fi

    # 自动扫描：找正在运行的 postgres 容器（按镜像名匹配）
    local found
    found=$($RUNNER ps --format '{{.Names}}' 2>/dev/null | while read -r name; do
        local image
        image=$($RUNNER inspect --format '{{.Config.Image}}' "$name" 2>/dev/null)
        if echo "$image" | grep -qi 'postgres'; then
            echo "$name"
            break
        fi
    done)

    if [ -n "$found" ]; then
        echo "$found"
    fi
}

# ---------- 环境检测 ----------
detect_env() {
    # 容器模式
    if [ -n "$RUNNER" ]; then
        PG_CONTAINER="$(find_pg_container)"
        if [ -n "$PG_CONTAINER" ]; then
            ENV_MODE="container"
            info "检测到容器环境: $RUNNER / $PG_CONTAINER"
            return
        fi
    fi

    # 原生模式
    PG_BIN=""
    for candidate in \
        "/usr/pgsql-13/bin/pg_dump" \
        "/usr/pgsql-14/bin/pg_dump" \
        "/usr/pgsql-15/bin/pg_dump" \
        "/usr/pgsql-16/bin/pg_dump" \
        "$(which pg_dump 2>/dev/null)" \
        ""; do
        if [ -n "$candidate" ] && [ -x "$candidate" ]; then
            PG_BIN="$(dirname "$candidate")"
            break
        fi
    done

    if [ -n "$PG_BIN" ]; then
        ENV_MODE="native"
        info "检测到原生 PostgreSQL: $PG_BIN"
        return
    fi

    err "未检测到可用的 PostgreSQL 环境（容器或原生）"
    err "请检查："
    err "  1. .env 中 BBS_PG_CONTAINER 是否配置正确"
    err "  2. 容器是否在运行: podman ps | grep postgres"
    err "  3. 或安装 pg_dump: yum install postgresql13 (匹配你的 PG 版本)"
    exit 1
}

# ---------- 备份 ----------
do_backup() {
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="bbs_${TIMESTAMP}.sql.gz"

    info "开始备份数据库 $DB_NAME ..."
    info "环境: $ENV_MODE | 用户: $DB_USER | 主机: $DB_HOST:$DB_PORT"

    case "$ENV_MODE" in
        container)
            info "容器: $PG_CONTAINER"
            $RUNNER exec "$PG_CONTAINER" pg_dump \
                -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" --no-owner --no-privileges "$DB_NAME" \
                | gzip > "$BACKUP_FILE"
            ;;
        native)
            PG_DUMP_BIN="${PG_BIN}/pg_dump"
            [ ! -x "$PG_DUMP_BIN" ] && PG_DUMP_BIN="pg_dump"
            export PGPASSWORD="$DB_PASS"
            "$PG_DUMP_BIN" \
                -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" \
                --no-owner --no-privileges "$DB_NAME" \
                | gzip > "$BACKUP_FILE"
            unset PGPASSWORD
            ;;
    esac

    FILE_SIZE=$(ls -lh "$BACKUP_FILE" | awk '{print $5}')
    ok "备份完成: $BACKUP_FILE ($FILE_SIZE)"

    # 自动验证
    if [ "${BBS_BACKUP_VERIFY:-true}" = "true" ]; then
        info "自动验证备份完整性..."
        do_verify "$BACKUP_FILE"
    else
        info "跳过自动验证（BBS_BACKUP_VERIFY=false）"
        info "手动验证: bash $0 --verify $BACKUP_FILE"
    fi
}

# ---------- 恢复 ----------
do_restore() {
    RESTORE_FILE="$1"
    if [ -z "$RESTORE_FILE" ] || [ ! -f "$RESTORE_FILE" ]; then
        err "文件不存在: $RESTORE_FILE"
        exit 1
    fi

    warn "即将恢复数据库 $DB_NAME，当前数据会被覆盖！"
    warn "文件: $RESTORE_FILE"
    read -p "确认恢复？(y/N): " CONFIRM
    if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
        info "已取消"
        exit 0
    fi

    info "开始恢复 $RESTORE_FILE ..."

    DECOMPRESS="gunzip -c"
    [[ "$RESTORE_FILE" != *.gz ]] && DECOMPRESS="cat"

    case "$ENV_MODE" in
        container)
            $DECOMPRESS "$RESTORE_FILE" | $RUNNER exec -i "$PG_CONTAINER" \
                psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d "$DB_NAME" --single-transaction
            ;;
        native)
            PG_PSQL_BIN="${PG_BIN}/psql"
            [ ! -x "$PG_PSQL_BIN" ] && PG_PSQL_BIN="psql"
            export PGPASSWORD="$DB_PASS"
            $DECOMPRESS "$RESTORE_FILE" | "$PG_PSQL_BIN" \
                -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" \
                -d "$DB_NAME" --single-transaction
            unset PGPASSWORD
            ;;
    esac

    ok "恢复完成"
}

# ---------- 验证备份 ----------
do_verify() {
    RESTORE_FILE="${1:-}"
    if [ -z "$RESTORE_FILE" ] || [ ! -f "$RESTORE_FILE" ]; then
        RESTORE_FILE=$(ls -t bbs_*.sql.gz 2>/dev/null | head -1)
        if [ -z "$RESTORE_FILE" ]; then
            err "没有找到备份文件"
            exit 1
        fi
        info "未指定文件，使用最新备份: $RESTORE_FILE"
    fi

    VERIFY_DB="bbs_verify_$(date +%s)"
    VERIFY_SQL="/tmp/verify_${VERIFY_DB}.sql"
    info "验证备份: $RESTORE_FILE"
    info "将创建临时数据库: $VERIFY_DB"

    DECOMPRESS="gunzip -c"
    [[ "$RESTORE_FILE" != *.gz ]] && DECOMPRESS="cat"

    case "$ENV_MODE" in
        container)
            # 1. 创建临时数据库
            info "[1/6] 创建临时数据库 $VERIFY_DB ..."
            $RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d postgres -c "CREATE DATABASE $VERIFY_DB;" -q

            # 2. 恢复备份到临时库
            info "[2/6] 恢复备份到临时数据库 ..."
            $DECOMPRESS "$RESTORE_FILE" | $RUNNER exec -i "$PG_CONTAINER" \
                psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d "$VERIFY_DB" --single-transaction -q

            # 3. 对比数据库总大小
            info "[3/6] 对比数据库大小..."
            ORIG_SIZE=$($RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d "$DB_NAME" -t -A -c \
                "SELECT pg_database_size('$DB_NAME');")
            VERIFY_SIZE=$($RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d "$VERIFY_DB" -t -A -c \
                "SELECT pg_database_size('$VERIFY_DB');")
            ORIG_SIZE_PRETTY=$(echo "$ORIG_SIZE" | numfmt --to=iec 2>/dev/null || echo "$ORIG_SIZE bytes")
            VERIFY_SIZE_PRETTY=$(echo "$VERIFY_SIZE" | numfmt --to=iec 2>/dev/null || echo "$VERIFY_SIZE bytes")
            # 允许 5% 误差（vacuum/统计信息差异）
            THRESHOLD=$(echo "$ORIG_SIZE * 95 / 100" | bc 2>/dev/null || echo 0)
            if [ "$VERIFY_SIZE" -ge "$THRESHOLD" ] 2>/dev/null; then
                ok "  原库: $ORIG_SIZE_PRETTY | 验证库: $VERIFY_SIZE_PRETTY ✓"
            else
                err "  原库: $ORIG_SIZE_PRETTY | 验证库: $VERIFY_SIZE_PRETTY ✗ (差异超过5%)"
            fi

            # 4. 对比表数量 + 索引数量
            info "[4/6] 对比表和索引数量..."
            $RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d postgres -c "
                WITH orig_tables AS (
                    SELECT count(*) AS cnt FROM information_schema.tables
                    WHERE table_schema='public' AND table_type='BASE TABLE'
                ),
                verify_tables AS (
                    SELECT count(*) AS cnt FROM information_schema.tables
                    WHERE table_schema='public' AND table_type='BASE TABLE'
                ),
                orig_idx AS (
                    SELECT count(*) AS cnt FROM pg_indexes WHERE schemaname='public'
                ),
                verify_idx AS (
                    SELECT count(*) AS cnt FROM pg_indexes WHERE schemaname='public'
                )
                SELECT
                    '表数量' AS item, o.cnt AS original, v.cnt AS verify,
                    CASE WHEN o.cnt = v.cnt THEN '✓' ELSE '✗' END AS status
                FROM orig_tables o, verify_tables v
                UNION ALL
                SELECT
                    '索引数量', o.cnt, v.cnt,
                    CASE WHEN o.cnt = v.cnt THEN '✓' ELSE '✗' END
                FROM orig_idx o, verify_idx v;
            " -q

            # 5. 逐表对比：行数 + 数据校验和（md5 拼接所有行）
            info "[5/6] 逐表对比行数 + 数据校验和..."
            echo ""
            echo "  表名                    原库行数  验证行数  校验和一致  状态"
            echo "  ─────────────────────── ────────  ────────  ──────────  ────"

            # 获取所有用户表名
            TABLES=$($RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d "$DB_NAME" -t -A -c \
                "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename;")

            ALL_OK=true
            for TABLE in $TABLES; do
                # 行数
                ORIG_COUNT=$($RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d "$DB_NAME" -t -A -c \
                    "SELECT count(*) FROM \"$TABLE\";")
                VERIFY_COUNT=$($RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d "$VERIFY_DB" -t -A -c \
                    "SELECT count(*) FROM \"$TABLE\";")

                # 数据校验和：对所有列拼接后取 md5
                # 动态拼接所有列为文本
                ORIG_HASH=$($RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d "$DB_NAME" -t -A -c "
                    SELECT md5(string_agg(row_hash, ''))
                    FROM (
                        SELECT md5(CAST(t.*)::text) AS row_hash
                        FROM \"$TABLE\" t
                        ORDER BY 1
                    ) sub;
                " 2>/dev/null || echo "HASH_ERR")

                VERIFY_HASH=$($RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d "$VERIFY_DB" -t -A -c "
                    SELECT md5(string_agg(row_hash, ''))
                    FROM (
                        SELECT md5(CAST(t.*)::text) AS row_hash
                        FROM \"$TABLE\" t
                        ORDER BY 1
                    ) sub;
                " 2>/dev/null || echo "HASH_ERR")

                # 对比
                COUNT_MATCH="✗"
                HASH_MATCH="✗"
                [ "$ORIG_COUNT" = "$VERIFY_COUNT" ] && COUNT_MATCH="✓"
                [ "$ORIG_HASH" = "$VERIFY_HASH" ] && HASH_MATCH="✓"

                if [ "$COUNT_MATCH" = "✗" ] || [ "$HASH_MATCH" = "✗" ]; then
                    ALL_OK=false
                fi

                # 格式化输出（固定宽度对齐）
                printf "  %-23s %7s  %7s  %10s  " "$TABLE" "$ORIG_COUNT" "$VERIFY_COUNT" "$HASH_MATCH"
                if [ "$COUNT_MATCH" = "✓" ] && [ "$HASH_MATCH" = "✓" ]; then
                    echo "✓"
                else
                    echo "✗ (行数:$COUNT_MATCH 校验:$HASH_MATCH)"
                fi
            done

            echo ""

            # 6. 对比序列（自增 ID）
            info "[6/6] 对比序列值..."
            $RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d postgres -c "
                WITH orig_seq AS (
                    SELECT sequencename, last_value
                    FROM pg_sequences WHERE schemaname='public'
                ),
                verify_seq AS (
                    SELECT sequencename, last_value
                    FROM pg_sequences WHERE schemaname='public'
                )
                SELECT
                    COALESCE(o.sequencename, v.sequencename) AS sequence,
                    COALESCE(o.last_value, 0) AS original,
                    COALESCE(v.last_value, 0) AS verify,
                    CASE WHEN COALESCE(o.last_value, 0) = COALESCE(v.last_value, 0) THEN '✓' ELSE '≈' END AS status
                FROM orig_seq o
                FULL OUTER JOIN verify_seq v ON o.sequencename = v.sequencename
                ORDER BY 1;
            " -q

            # 7. 清理临时数据库
            info "清理临时数据库 $VERIFY_DB ..."
            $RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d postgres -c "DROP DATABASE $VERIFY_DB;" -q

            if [ "$ALL_OK" = true ]; then
                ok "═══ 验证通过：所有表的行数和数据校验和一致 ═══"
            else
                err "═══ 验证失败：存在不一致的表，请检查上面的输出 ═══"
                exit 1
            fi
            ;;
        native)
            err "原生模式暂不支持 --verify，请手动操作"
            exit 1
            ;;
    esac
}

# ---------- 显示环境 ----------
show_env() {
    info "=== 环境信息 ==="
    info "检测模式: $ENV_MODE"
    info "数据库名: $DB_NAME"
    info "数据库用户: $DB_USER"
    info "数据库主机: $DB_HOST:$DB_PORT"
    if [ "$ENV_MODE" = "container" ]; then
        info "容器名: $PG_CONTAINER"
        info "容器内 PG 端口: $PG_INTERNAL_PORT"
        info "运行时: $RUNNER"
        $RUNNER inspect --format '镜像: {{.Config.Image}} | 状态: {{.State.Status}} | 启动: {{.State.StartedAt}}' "$PG_CONTAINER" 2>/dev/null
    else
        info "pg_dump 路径: ${PG_BIN}/pg_dump"
    fi

    info ""
    info "=== 数据库大小 ==="
    case "$ENV_MODE" in
        container)
            $RUNNER exec "$PG_CONTAINER" psql -h localhost -p $PG_INTERNAL_PORT -U "$DB_USER" -d "$DB_NAME" -t -A -c \
                "SELECT pg_size_pretty(pg_database_size('$DB_NAME'));" 2>/dev/null
            ;;
        native)
            export PGPASSWORD="$DB_PASS"
            "${PG_BIN}/psql" -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -A -c \
                "SELECT pg_size_pretty(pg_database_size('$DB_NAME'));" 2>/dev/null
            unset PGPASSWORD
            ;;
    esac
}

# ---------- 主入口 ----------
detect_env

# 容器模式下自动检测 PG 内部端口
if [ "$ENV_MODE" = "container" ]; then
    PG_INTERNAL_PORT="$(detect_pg_port)"
    info "容器内 PG 端口: $PG_INTERNAL_PORT"
fi

ACTION="${1:-backup}"

case "$ACTION" in
    --env)
        show_env
        ;;
    --restore)
        do_restore "$2"
        ;;
    --verify)
        do_verify "$2"
        ;;
    backup)
        do_backup
        ;;
    *)
        echo "用法:"
        echo "  bash $0                          # 备份到当前目录"
        echo "  bash $0 --restore <file.sql.gz>  # 恢复"
        echo "  bash $0 --verify [file.sql.gz]   # 验证备份（自动对比行数）"
        echo "  bash $0 --env                    # 显示环境信息"
        ;;
esac
