#!/bin/bash
# ============================================================================
# pgfix.sh —— 现场一条命令修好 pg_dump
#
# 用法（只需这一句）：
#   bash pgfix.sh
#
# 它做什么：
#   1) 找本机所有 pg_dump（PATH / /usr/pgsql-*/bin / /usr/lib/postgresql/*/bin）
#   2) 逐个试"只导结构"（秒级，不写库），留下能用的那个
#   3) 都不行 → 用机器上现成的 postgres 镜像做临时客户端（写完即删）
#   4) 把结果写进 /data/bbs/.env 的 BBS_PG_BIN=...
#   5) 打印下一步那一条命令
#
# 只读库、只新增 /data/bbs/pgbin/（容器兜底时）和 .env 里一行 BBS_PG_BIN。
# 退出码：0=已修好  3=没修好（会打印原因，把这几行发回去即可）
# ============================================================================
set -u

H="${BBS_HOME:-/data/bbs}"
die() { echo "[FAIL] $*"; exit 3; }
info() { echo "[INFO] $*"; }
ok() { echo "[OK] $*"; }

[ -d "$H" ] || die "部署根不存在: $H"
[ -f "$H/.env" ] || die "缺少 $H/.env"

# 读配置（去 CRLF，避免 Windows 编辑过的 .env 触发莫名其妙的问题）
T=$(mktemp) || die "mktemp 失败"
sed 's/\r$//' "$H/.env" > "$T" 2>/dev/null
set -a
. "$T" 2>/dev/null
set +a
rm -f "$T"

DBH="${BBS_DB_HOST:-127.0.0.1}"
DBP="${BBS_DB_PORT:-15432}"
DBN="${BBS_DB_NAME:-bbs}"
DBU="${BBS_DB_USER:-work_flow}"
DBW="${BBS_DB_PASSWORD:-}"
[ -n "$DBW" ] || die ".env 里没有 BBS_DB_PASSWORD"

echo "== 目标库: $DBU@$DBH:$DBP/$DBN"

# ---------- 1. 收集候选 ----------
CANDS=""
if [ -n "${BBS_PG_BIN:-}" ] && [ -x "$BBS_PG_BIN/pg_dump" ]; then
    CANDS="$BBS_PG_BIN"
fi
if command -v pg_dump >/dev/null 2>&1; then
    d0="$(dirname "$(command -v pg_dump)")"
    case " $CANDS " in *" $d0 "*) ;; *) CANDS="$CANDS $d0" ;; esac
fi
for d in /usr/pgsql-*/bin /usr/lib/postgresql/*/bin /opt/pgsql/*/bin /opt/postgresql/*/bin; do
    [ -x "$d/pg_dump" ] || continue
    case " $CANDS " in *" $d "*) ;; *) CANDS="$CANDS $d" ;; esac
done

# ---------- 2. 逐个试 ----------
GOOD=""
for d in $CANDS; do
    ver=$("$d/pg_dump" --version 2>/dev/null | head -1)
    printf '== 试 %s（%s）... ' "$d" "$ver"
    err=$(PGPASSWORD="$DBW" "$d/pg_dump" -h "$DBH" -p "$DBP" -U "$DBU" \
            --no-owner --no-privileges -s "$DBN" 2>&1 >/dev/null)
    if [ $? -eq 0 ]; then
        echo "可用"
        GOOD="$d"
        break
    fi
    echo "不行"
    echo "$err" | head -3 | sed 's/^/     /'
done

# ---------- 3. 容器兜底 ----------
if [ -z "$GOOD" ]; then
    info "本机客户端都不可用，尝试用现成 postgres 镜像做临时客户端"
    command -v podman >/dev/null 2>&1 || die "既没有可用的 pg_dump，也没有 podman"
    IMGS=$(podman images --format '{{.Repository}}:{{.Tag}}' 2>/dev/null | grep -E '(^|/)postgres:[0-9]' | sort -t: -k2 -V -r)
    [ -n "$IMGS" ] || die "本机没有 postgres 镜像，也没有可用的 pg_dump；请把 pg_dump 装到机器上"

    mkdir -p "$H/pgbin" || die "无法创建 $H/pgbin"
    printf 'PGPASSWORD=%s\n' "$DBW" > "$H/pgbin/pg.env"
    chmod 600 "$H/pgbin/pg.env"

    for IMG in $IMGS; do
        printf '== 试镜像 %s ... ' "$IMG"
        cat > "$H/pgbin/pg_dump" <<EOF
#!/bin/sh
exec podman run --rm --network host -i --env-file "$H/pgbin/pg.env" "$IMG" pg_dump "\$@"
EOF
        cat > "$H/pgbin/psql" <<EOF
#!/bin/sh
exec podman run --rm --network host -i --env-file "$H/pgbin/pg.env" "$IMG" psql "\$@"
EOF
        cat > "$H/pgbin/pg_isready" <<EOF
#!/bin/sh
exec podman run --rm --network host "$IMG" pg_isready "\$@"
EOF
        chmod 755 "$H/pgbin/pg_dump" "$H/pgbin/psql" "$H/pgbin/pg_isready"
        if PGPASSWORD="$DBW" "$H/pgbin/pg_dump" -h "$DBH" -p "$DBP" -U "$DBU" \
             --no-owner --no-privileges -s "$DBN" >/dev/null 2>&1; then
            echo "可用"
            GOOD="$H/pgbin"
            ok "容器客户端可用（$IMG，客户端版本需 ≥ 服务端）"
            break
        fi
        echo "不行"
    done

    if [ -z "$GOOD" ]; then
        echo "== 所有镜像都失败，最后一个的原始报错："
        PGPASSWORD="$DBW" "$H/pgbin/pg_dump" -h "$DBH" -p "$DBP" -U "$DBU" \
            --no-owner --no-privileges -s "$DBN" 2>&1 >/dev/null | head -5 | sed 's/^/   /'
        die "两条路都不通；把上面报错发回即可"
    fi
fi

# ---------- 4. 写入配置 ----------
# deploy.sh 优先读 deploy.env，所以两个文件存在时都要写
for f in "$H/.env" "$H/deploy.env"; do
    [ -f "$f" ] || continue
    if grep -q '^BBS_PG_BIN=' "$f" 2>/dev/null; then
        sed -i "s#^BBS_PG_BIN=.*#BBS_PG_BIN=$GOOD#" "$f"
    else
        printf '\n# pg_dump 所在目录（pgfix.sh 自动写入）\nBBS_PG_BIN=%s\n' "$GOOD" >> "$f"
    fi
    chmod 600 "$f" 2>/dev/null || true
    ok "已写入 $f： BBS_PG_BIN=$GOOD"
done

# ---------- 5. 下一步 ----------
echo ""
ok "修好了。下一步只敲这一条："
echo ""
echo "    bash deploy.sh once migrate"
echo ""
echo "（它会先做一次真实备份，成功后才切版本；备份失败会再停下来，不留半成品）"
exit 0
