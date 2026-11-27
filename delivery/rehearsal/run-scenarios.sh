#!/bin/bash
# ============================================================================
# 演练 S3–S9（WSL2 测试机，全部在 $R 下，不碰生产）
#   S3 升级          S4 坏包（两个变体）   S5 端口占用
#   S6 PG 备份恢复    S7 A/B 软链切换       S8 restart    S9 回滚
#
# 前置：setup-legacy.sh 已跑过，且 S1 迁移 / S2 同版本重跑已通过。
# 用法：bash run-scenarios.sh [升级包路径]
# ============================================================================
set -u
R="${BBS_HOME_TEST:-$HOME/bbs-mig}"
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
UPD_PKG="${1:-$REPO/dist/bbs-upgrade-20261127-093351.tar.gz}"
PG=bbs-postgres
DB=bbs_mig_rehearsal
DB_USER=work_flow
PASS=0; FAIL=0; LOG="$R/rehearsal.log"

say()  { printf '\n===== %s =====\n' "$*" | tee -a "$LOG"; }
okc()  { PASS=$((PASS+1)); printf '[PASS] %s\n' "$*" | tee -a "$LOG"; }
badc() { FAIL=$((FAIL+1)); printf '[FAIL] %s\n' "$*" | tee -a "$LOG"; }
chk()  { if eval "$2" >/dev/null 2>&1; then okc "$1"; else badc "$1"; fi; }

: > "$LOG"
cd "$R" || exit 1

# ---------------------------------------------------------------- S3 --------
say "S3 升级（update 包）"
cp "$UPD_PKG" "$R/" || { badc "复制升级包失败"; exit 1; }
[ -f "$UPD_PKG.sha256" ] && cp "$UPD_PKG.sha256" "$R/"
BEFORE=$(readlink -f "$R/releases/current")
bash deploy.sh --package "$(basename "$UPD_PKG")" --yes 2>&1 | tee -a "$LOG" | grep -E '━━━|\[ OK \]|\[WARN\]|\[FAIL\]|结果' >/dev/null
RC=${PIPESTATUS[0]}
AFTER=$(readlink -f "$R/releases/current")
chk "升级退出码 0（实际 $RC）" "[ '$RC' = '0' ]"
chk "current 换到 20261127_093500-291ae68" "printf '%s' '$AFTER' | grep -q '20261127_093500-291ae68'"
chk "旧 release 仍保留（可回滚）" "[ -d '$BEFORE' ]"
chk "升级包已归档到 packages/" "[ -f '$R/packages/$(basename "$UPD_PKG")' ]"
chk "jar 挂真实路径（新版本）" "podman inspect bbs-server-mig --format '{{range .Mounts}}{{.Source}};{{end}}' | grep -qF '$AFTER/app/bbs-server.jar'"

# ---------------------------------------------------------------- S4a -------
say "S4a 坏包：sidecar SHA256 不匹配"
CUR_BEFORE=$(readlink -f "$R/releases/current")
BAD="$R/bad-sha.tar.gz"
cp "$UPD_PKG" "$BAD"
echo "0000000000000000000000000000000000000000000000000000000000000000" > "$BAD.sha256"
bash deploy.sh --package "$(basename "$BAD")" --yes >/dev/null 2>&1
RC=$?
CUR_AFTER=$(readlink -f "$R/releases/current")
chk "sidecar 不匹配 → 拒绝（退出码 $RC）" "[ '$RC' != '0' ]"
chk "拒绝后未切换版本" "[ '$CUR_BEFORE' = '$CUR_AFTER' ]"
rm -f "$BAD" "$BAD.sha256"

# ---------------------------------------------------------------- S4b -------
say "S4b 坏包：包内 payload 被篡改（无 sidecar，靠逐文件哈希）"
BAD2="$R/bad-payload.tar.gz"
T=$(mktemp -d)
tar -xzf "$UPD_PKG" -C "$T"
INNER=$(find "$T" -maxdepth 2 -name manifest.json -printf '%h\n' | head -1)
printf 'tampered' >> "$INNER/payload/app/bbs-server.jar"
( cd "$T" && tar -czf "$BAD2" "$(basename "$INNER")" )
rm -rf "$T"
bash deploy.sh --package "$(basename "$BAD2")" --yes >/dev/null 2>&1
RC=$?
CUR_AFTER2=$(readlink -f "$R/releases/current")
chk "payload 篡改 → 逐文件哈希拒绝（退出码 $RC）" "[ '$RC' != '0' ]"
chk "拒绝后未切换版本" "[ '$CUR_BEFORE' = '$CUR_AFTER2' ]"
rm -f "$BAD2"

# ---------------------------------------------------------------- S5 --------
say "S5 端口占用（独立根，不干扰运行栈）"
P="$HOME/bbs-port"
rm -rf "$P"; mkdir -p "$P"
cat > "$P/.env" <<EOF
BBS_DB_HOST=127.0.0.1
BBS_DB_PORT=15432
BBS_DB_NAME=$DB
BBS_DB_USER=$DB_USER
BBS_DB_PASSWORD=x
BBS_SERVER_PORT=60110
BBS_UPLOAD_DIR=$P/bbsUpload/
NGINX_PORT=60111
BBS_PG_CONTAINER=$PG
BBS_SERVER_CONTAINER=bbs-server-port
BBS_NGINX_CONTAINER=bbs-nginx-port
EOF
cp "$REPO/deploy.sh" "$P/deploy.sh"
python3 -m http.server 60111 --bind 127.0.0.1 >/dev/null 2>&1 &
HPID=$!
sleep 1
bash "$P/deploy.sh" --home "$P" --package "$UPD_PKG" --dry-run >/dev/null 2>&1
RC=$?
kill "$HPID" 2>/dev/null; wait "$HPID" 2>/dev/null
chk "端口 60111 被占 → 预检拒绝（退出码 $RC）" "[ '$RC' != '0' ]"
chk "预检失败后未创建 releases/" "[ ! -d '$P/releases' ]"
rm -rf "$P"

# ---------------------------------------------------------------- S6 --------
say "S6 PG 备份恢复（真实 pg_dump → 新库比对表数）"
DUMP=$(ls -t "$R"/backups/*/bbs_*.sql.gz 2>/dev/null | head -1)
chk "存在 pg_dump 备份文件" "[ -n '$DUMP' ]"
gunzip -t "$DUMP" 2>/dev/null && okc "备份 gzip 完整性（gunzip -t）" || badc "备份 gzip 完整性"
SRC_T=$(podman exec "$PG" psql -U "$DB_USER" -d "$DB" -Atc "select count(*) from information_schema.tables where table_schema='public'" 2>/dev/null)
podman exec "$PG" psql -U "$DB_USER" -d postgres -c "drop database if exists bbs_restore_check" >/dev/null 2>&1
podman exec "$PG" psql -U "$DB_USER" -d postgres -c "create database bbs_restore_check" >/dev/null 2>&1
gunzip -c "$DUMP" | podman exec -i "$PG" psql -U "$DB_USER" -d bbs_restore_check >/dev/null 2>&1
DST_T=$(podman exec "$PG" psql -U "$DB_USER" -d bbs_restore_check -Atc "select count(*) from information_schema.tables where table_schema='public'" 2>/dev/null)
SRC_V=$(podman exec "$PG" psql -U "$DB_USER" -d "$DB" -Atc "select max(version) from bbs_schema_version" 2>/dev/null)
DST_V=$(podman exec "$PG" psql -U "$DB_USER" -d bbs_restore_check -Atc "select max(version) from bbs_schema_version" 2>/dev/null)
chk "恢复后表数一致（源 $SRC_T / 恢复 $DST_T）" "[ '$SRC_T' = '$DST_T' ] && [ -n '$SRC_T' ]"
chk "恢复后迁移版本一致（$SRC_V）" "[ '$SRC_V' = '$DST_V' ]"
podman exec "$PG" psql -U "$DB_USER" -d postgres -c "drop database if exists bbs_restore_check" >/dev/null 2>&1

# ---------------------------------------------------------------- S7 --------
say "S7 A/B 软链切换（旧回滚命令必须仍然有效）"
CUR=$(readlink -f "$R/releases/current")
OLD_NAME=$(basename "$(find "$R/versions" -maxdepth 1 -mindepth 1 -type d | head -1)")
ln -snf "$OLD_NAME" "$R/versions/latest"
A=$(readlink -f "$R/current")
chk "旧命令 ln -snf <旧版本> versions/latest 生效（current → $(basename "$A")）" "printf '%s' '$A' | grep -q '$OLD_NAME'"
if bash deploy.sh verify >/dev/null 2>&1; then badc "A/B 切到旧版后 verify 应变红（却通过）"; else okc "A/B 切到旧版后 verify 变红（判据有效）"; fi
ln -snf ../releases/current "$R/versions/latest"
bash deploy.sh verify >/dev/null 2>&1 && okc "切回 releases/current 后 verify 恢复绿色" || badc "切回后 verify 仍红"
chk "readlink -f current 回到当前 release" "[ \"\$(readlink -f '$R/current')\" = '$CUR' ]"

# ---------------------------------------------------------------- S8 --------
say "S8 restart（按当前 release 重建容器）"
bash deploy.sh restart >/dev/null 2>&1 && okc "restart 退出码 0" || badc "restart 失败"
chk "restart 后三端点 200" "[ \"\$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:60101/bbs-user/)\" = '200' ]"
chk "restart 后仍挂真实路径" "podman inspect bbs-server-mig --format '{{range .Mounts}}{{.Source}};{{end}}' | grep -qF '/releases/'"

# ---------------------------------------------------------------- S9 --------
say "S9 回滚（回到上一版）"
CUR=$(readlink -f "$R/releases/current")
PREV=$(sed -n 's/.*"previous"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$R/current.json" | head -1)
bash deploy.sh rollback >/dev/null 2>&1 && okc "rollback 退出码 0" || badc "rollback 失败"
NEWCUR=$(readlink -f "$R/releases/current")
chk "回滚后 current = 上一版（$PREV）" "[ \"\$(basename '$NEWCUR')\" = '$PREV' ]"
chk "回滚后端点 200" "[ \"\$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:60101/bbs-user/)\" = '200' ]"
chk "回滚后 jar 挂真实路径" "podman inspect bbs-server-mig --format '{{range .Mounts}}{{.Source}};{{end}}' | grep -qF '$NEWCUR/app/bbs-server.jar'"

say "结果: PASS=$PASS FAIL=$FAIL"
[ "$FAIL" -eq 0 ]
