#!/usr/bin/env bash
# v039-moderator-role 迁移 → 校验 → 回滚 → 再迁移（PostgreSQL 副本库）
#
# 依赖：bash + podman（默认从 WSL 里执行；PG 运行在 podman 容器中）
# 默认值可用环境变量覆盖：
#   PG_CONTAINER  podman 容器名        （默认 bbs-postgres）
#   PG_USER       数据库用户            （默认 work_flow）
#   PG_SOURCE_DB  只读源库（用于复制）   （默认 bbs）
#   PG_TEST_DB    测试副本库             （默认 bbs_moderator_test_pg，必须包含 test）
#
# 安全约束：
#   1) PG_TEST_DB 必须包含 "test"，且不能等于 PG_SOURCE_DB；
#   2) 只对副本库执行 DROP/CREATE/DML，源库仅用于 pg_dump 只读复制；
#   3) 源库不会被写入、不会被删除。

set -uo pipefail

PG_CONTAINER=${PG_CONTAINER:-bbs-postgres}
PG_USER=${PG_USER:-work_flow}
PG_SOURCE_DB=${PG_SOURCE_DB:-bbs}
PG_TEST_DB=${PG_TEST_DB:-bbs_moderator_test_pg}

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
PG_DIR="$ROOT/tests/db/pg"
MIGRATION_SQL="$ROOT/bbs-server/src/main/resources/db/init/upgrade-pg.sql"
GENERATED="$ROOT/tmp/v039-pg.generated.sql"

if [ "$PG_TEST_DB" = "$PG_SOURCE_DB" ]; then
  echo "拒绝执行：PG_TEST_DB 不能等于 PG_SOURCE_DB（$PG_SOURCE_DB）" >&2
  exit 2
fi
case "$PG_TEST_DB" in
  *test*) ;;
  *) echo "拒绝执行：PG_TEST_DB 必须包含 test（当前：$PG_TEST_DB）" >&2; exit 2 ;;
esac

q()        { podman exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_TEST_DB" -v ON_ERROR_STOP=1 -At -c "$1"; }
run_file() { podman exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_TEST_DB" -v ON_ERROR_STOP=1 -At < "$1"; }

pass=0; fail=0
check() {
  local id="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    echo "PASS $id"
    pass=$((pass + 1))
  else
    echo "FAIL $id expected=[$expected] actual=[$actual]"
    fail=$((fail + 1))
  fi
}

comment()     { q "SELECT col_description('bbs_user'::regclass, (SELECT attnum FROM pg_attribute WHERE attrelid = 'bbs_user'::regclass AND attname = 'user_type'))"; }
user_type_of(){ q "SELECT user_type FROM bbs_user WHERE id = $1"; }
type4_list()  { q "SELECT string_agg(id::text, ',' ORDER BY id) FROM bbs_user WHERE user_type = '4'"; }
type_counts() { q "SELECT string_agg(user_type || ':' || c, ',' ORDER BY user_type) FROM (SELECT user_type, count(1) c FROM bbs_user GROUP BY user_type) t"; }
admin_super() { q "SELECT string_agg(id || ':' || user_type, ',' ORDER BY id) FROM bbs_user WHERE id IN (9102, 9103, 9106)"; }
version_cnt() { q "SELECT count(1) FROM bbs_schema_version WHERE version = 'v039-moderator-role'"; }
backup_list() { q "SELECT string_agg(user_id || ':' || old_user_type, ',' ORDER BY user_id) FROM bbs_user_type_v039_backup"; }
candidates()  { q "SELECT string_agg(u.id::text, ',' ORDER BY u.id) FROM bbs_user u WHERE u.user_type = '1' AND u.is_delete = 0 AND u.id <> 1 AND EXISTS (SELECT 1 FROM bbs_board_moderator m WHERE m.user_id = u.id AND m.status = 1)"; }
illegal_cnt() { q "SELECT count(1) FROM bbs_user WHERE user_type IS NULL OR trim(user_type) = '' OR user_type NOT IN ('1','2','3','4')"; }

echo "=== 0. 重建副本库 $PG_TEST_DB（来自 $PG_SOURCE_DB） ==="
for _ in 1 2 3 4 5 6 7 8 9 10; do
  podman exec "$PG_CONTAINER" pg_isready -U "$PG_USER" >/dev/null 2>&1 && break
  sleep 1
done
podman exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -v ON_ERROR_STOP=1 -q -c "DROP DATABASE IF EXISTS $PG_TEST_DB"
podman exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -v ON_ERROR_STOP=1 -q -c "CREATE DATABASE $PG_TEST_DB"
podman exec "$PG_CONTAINER" sh -c "pg_dump -U $PG_USER -d $PG_SOURCE_DB > /tmp/bbs_pg_smoke_dump.sql && psql -U $PG_USER -d $PG_TEST_DB -q -f /tmp/bbs_pg_smoke_dump.sql" >/dev/null
users=$(q "SELECT count(1) FROM bbs_user")
tables=$(q "SELECT count(1) FROM information_schema.tables WHERE table_schema = 'public'")
echo "copy: users=$users tables=$tables"

echo "=== 1. 提取 v039 块 + 灌入测试数据 ==="
awk '/^-- @migration: v039-moderator-role/{p=1} p' "$MIGRATION_SQL" | tr -d '\r' > "$GENERATED"
if [ -s "$GENERATED" ]; then echo "PASS gen-block"; pass=$((pass + 1)); else echo "FAIL gen-block"; fail=$((fail + 1)); fi
run_file "$PG_DIR/moderator-seed.sql"

echo "=== 2. 迁移前基线 ==="
check "G1.9101" "1" "$(user_type_of 9101)"
check "G1.9108" "9" "$(user_type_of 9108)"
check "G1.candidates" "9101" "$(candidates)"
check "G1.v039" "0" "$(version_cnt)"
check "G1.comment-empty" "" "$(comment)"
check "G1.backup" "9101:1,9107:1" "$(backup_list)"

echo "=== 3. 第一次迁移 ==="
run_file "$GENERATED"
run_file "$PG_DIR/version-record.sql"
counts1=$(type_counts)
check "G3.9101" "4" "$(user_type_of 9101)"
check "G3.9102" "2" "$(user_type_of 9102)"
check "G3.9103" "3" "$(user_type_of 9103)"
check "G3.9106" "4" "$(user_type_of 9106)"
check "G3.9107-deleted" "1" "$(user_type_of 9107)"
check "G3.9108-invalid" "1" "$(user_type_of 9108)"
check "G3.type4" "9101,9106" "$(type4_list)"
check "G3.admin-super" "9102:2,9103:3,9106:4" "$(admin_super)"
check "G3.comment" "用户类型（1：普通用户，2：管理员，3：超级管理员，4：版主）" "$(comment)"
check "G3.v039" "1" "$(version_cnt)"
check "G3.illegal" "0" "$(illegal_cnt)"

echo "=== 4. 回滚 ==="
run_file "$PG_DIR/rollback.sql"
check "G5.9101" "1" "$(user_type_of 9101)"
check "G5.9106" "4" "$(user_type_of 9106)"
check "G5.comment-empty" "" "$(comment)"
check "G5.v039" "0" "$(version_cnt)"

echo "=== 5. 再次迁移（幂等对比） ==="
run_file "$GENERATED"
run_file "$PG_DIR/version-record.sql"
counts2=$(type_counts)
check "G6.type4" "9101,9106" "$(type4_list)"
check "G6.counts" "$counts1" "$counts2"
check "G6.comment" "用户类型（1：普通用户，2：管理员，3：超级管理员，4：版主）" "$(comment)"
check "G6.v039" "1" "$(version_cnt)"

echo "=== 6. 结果 ==="
echo "counts.first=$counts1"
echo "counts.second=$counts2"
if [ "$fail" -eq 0 ]; then
  echo "MIGRATION_SMOKE_OK pass=$pass fail=0"
else
  echo "MIGRATION_SMOKE_FAILED pass=$pass fail=$fail"
  exit 1
fi
