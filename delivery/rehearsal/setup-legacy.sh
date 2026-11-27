#!/bin/bash
# ============================================================================
# 演练 0：搭一个"生产形态"的旧布局测试机（WSL2）
#
#   /data/bbs-mig/
#   ├── .env / deploy.env          端口 60100/60101，库 bbs_mig_rehearsal
#   ├── versions/20261126_102549/  与生产同版本产物（来自 dist/bbs-upgrade-20261126-102606）
#   ├── versions/latest -> 20261126_102549
#   ├── current -> versions/latest
#   ├── scripts/prod/offline.sh    旧入口（不含新 deploy.sh 实现）
#   ├── deploy-offline.sh          旧兼容脚本
#   ├── bbsUpload/ base-images/ archive/ logs/
#   └── deploy.sh                  新入口（单独交付，模拟"包+deploy.sh 放同目录"）
#
# 容器：bbs-server-mig / bbs-nginx-mig，挂的是**兼容链**（current/...），
#       与生产现状一致；迁移后应变成 releases/<id>/app/... 真实路径。
# ============================================================================
set -u
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
R="${BBS_HOME_TEST:-$HOME/bbs-mig}"
PKG_SRC="${PKG_SRC:-$REPO/dist/bbs-upgrade-20261126-102606.tar.gz}"
RUNNER=podman
PG=bbs-postgres
DB=bbs_mig_rehearsal
DB_USER=work_flow
# 密码取测试机现有配置（不写死；生产采集包里也是打码的）
DB_PASS="${BBS_DB_PASSWORD:-$(grep -E '^BBS_DB_PASSWORD=' /data/bbs/.env 2>/dev/null | head -1 | cut -d= -f2-)}"
DB_PASS="${DB_PASS:-rehearsal_pw}"

info() { printf '[INFO] %s\n' "$*"; }
die()  { printf '[FAIL] %s\n' "$*" >&2; exit 1; }

[ -f "$PKG_SRC" ] || die "缺少基线包: $PKG_SRC"
command -v podman >/dev/null || die "无 podman"

info "清理旧演练环境（只删 $R 与 *-mig 容器）"
podman rm -f bbs-server-mig bbs-nginx-mig >/dev/null 2>&1 || true
rm -rf "$R"
mkdir -p "$R"/{versions,scripts/prod,base-images,archive,logs,bbsUpload}

info "解出基线产物（291ae68 / 20261126_102549）"
T=$(mktemp -d)
tar -xzf "$PKG_SRC" -C "$T" || die "解包失败"
SRC=$(find "$T" -maxdepth 1 -mindepth 1 -type d | head -1)
V=$(basename "$SRC" | sed 's/^upgrade-//')
mkdir -p "$R/versions/$V/bbs-ui" "$R/versions/$V/bbs-admin-ui"
cp "$SRC/bbs-server.jar" "$R/versions/$V/"
cp -r "$SRC/bbs-ui/dist" "$R/versions/$V/bbs-ui/"
cp -r "$SRC/bbs-admin-ui/dist" "$R/versions/$V/bbs-admin-ui/"
cp "$SRC/version.txt" "$R/versions/$V/" 2>/dev/null || true
rm -rf "$T"
ln -snf "$V" "$R/versions/latest"
ln -snf versions/latest "$R/current"
info "基线版本目录: $V"

# 旧入口（模拟生产现场脚本）
cp "$REPO/scripts/prod/offline.sh" "$R/scripts/prod/offline.sh"
cp "$REPO/scripts/prod/verify-server.sh" "$R/scripts/prod/verify-server.sh" 2>/dev/null || true
cp "$REPO/scripts/prod/offline.sh" "$R/deploy-offline.sh"
cp "$REPO/legacy/one-key-deploy.sh.2026-11-27" "$R/scripts/prod/one-key-deploy.sh"
chmod +x "$R/scripts/prod/"*.sh "$R/deploy-offline.sh"

# 新入口单独交付到部署根
cp "$REPO/deploy.sh" "$R/deploy.sh"
chmod +x "$R/deploy.sh"

cat > "$R/.env" <<EOF
BBS_DB_HOST=127.0.0.1
BBS_DB_PORT=15432
BBS_DB_NAME=$DB
BBS_DB_USER=$DB_USER
BBS_DB_PASSWORD=$DB_PASS
BBS_SERVER_PORT=60100
BBS_UPLOAD_DIR=$R/bbsUpload/
BBS_SUPER_ADMIN_PASSWORD=1234@abcD
NGINX_PORT=60101
BBS_PG_CONTAINER=$PG
BBS_SERVER_CONTAINER=bbs-server-mig
BBS_NGINX_CONTAINER=bbs-nginx-mig
EOF
cp "$R/.env" "$R/deploy.env"
# 测试机没有宿主 pg_dump：用包装脚本模拟（生产上会自动找到 /usr/pgsql-*/bin）
if [ -x "$HOME/fake-pg/bin/pg_dump" ]; then
    echo "BBS_PG_BIN=$HOME/fake-pg/bin" >> "$R/deploy.env"
fi
chmod 600 "$R/deploy.env"

info "准备演练库 $DB（不存在才建，不删不覆盖）"
EXIST=$(podman exec "$PG" psql -U "$DB_USER" -d postgres -Atc "select 1 from pg_database where datname='$DB'" 2>/dev/null | head -1)
if [ "$EXIST" != "1" ]; then
    podman exec "$PG" psql -U "$DB_USER" -d postgres -c "create database $DB" >/dev/null || die "建库失败"
    info "已创建 $DB"
else
    info "$DB 已存在（复用，不重置）"
fi

info "启动旧形态容器（挂兼容链 current/...，与生产一致）"
podman run -d --name bbs-server-mig --network host --restart=always --entrypoint java \
    -e BBS_DB_HOST=127.0.0.1 -e BBS_DB_PORT=15432 -e BBS_DB_NAME="$DB" \
    -e BBS_DB_USER="$DB_USER" -e BBS_DB_PASSWORD="$DB_PASS" \
    -e BBS_SUPER_ADMIN_PASSWORD=1234@abcD -e BBS_UPLOAD_DIR="$R/bbsUpload/" -e BBS_SERVER_PORT=60100 \
    -v "$R/current/bbs-server.jar:/app/app.jar:Z" \
    -v "$R/bbsUpload/:$R/bbsUpload/:Z" \
    bbs-server-base:latest -Xmx2g -Dloader.path=/app/lib -jar /app/app.jar --spring.profiles.active=podman >/dev/null \
    || die "bbs-server-mig 启动失败"
podman run -d --name bbs-nginx-mig --network host --restart=always \
    -e NGINX_PORT=60101 -e BBS_SERVER_PORT=60100 \
    -v "$R/current/bbs-ui/dist:/usr/share/nginx/html/bbs-ui:Z" \
    -v "$R/current/bbs-admin-ui/dist:/usr/share/nginx/html/bbs-admin:Z" \
    bbs-nginx-base:latest >/dev/null || die "bbs-nginx-mig 启动失败"

info "等待后端就绪（首次会建表，最多 180s）"
for i in $(seq 1 90); do
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1:60100/bbs-server/ 2>/dev/null || echo 000)
    [ "$code" = "200" ] && { info "后端就绪（$((i*2))s）"; break; }
    [ "$i" = "90" ] && die "后端 180s 未就绪：podman logs bbs-server-mig | tail -50"
    sleep 2
done

info "旧形态自检"
echo "  backend  $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:60100/bbs-server/)"
echo "  user     $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:60101/bbs-user/)"
echo "  admin    $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:60101/bbs-admin/)"
echo "  mounts:"
podman inspect bbs-server-mig --format '{{range .Mounts}}    {{.Source}} -> {{.Destination}}{{"\n"}}{{end}}'
echo "  schema: $(podman exec "$PG" psql -U "$DB_USER" -d "$DB" -Atc 'select max(version) from bbs_schema_version' 2>/dev/null)"
echo "  tables: $(podman exec "$PG" psql -U "$DB_USER" -d "$DB" -Atc "select count(*) from information_schema.tables where table_schema='public'" 2>/dev/null)"
echo ""
info "旧形态就绪：$R（版本 $V，兼容链挂载）"
