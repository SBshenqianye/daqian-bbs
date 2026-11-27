#!/bin/bash
# ============================================================================
# BBS 离线打包（新契约 v2，2026-11-27）
#
#   payload/app/{bbs-server.jar,bbs-ui/dist/,bbs-admin-ui/dist/}
#   scripts/                     全量运维脚本（含 lib/prod/dev/test）
#   manifest.json / manifest.env / SHA256SUMS
#
# 用法:
#   bash scripts/prod/package.sh                      # 轻量升级包（mode=update）
#   bash scripts/prod/package.sh --full               # 完整环境包（含基础镜像）
#   bash scripts/prod/package.sh --from <旧包.tar.gz>  # 用旧包内产物重打（不改代码）
#   bash scripts/prod/package.sh --mode migrate       # 结构迁移包（payload=现网同版本）
#   bash scripts/prod/package.sh --stage candidate    # 候选包（手测用）
#   bash scripts/prod/package.sh --commit 291ae68     # 指定 commit（默认取 git HEAD）
#   bash scripts/prod/package.sh --db-compat forward-only
#
# 产物:
#   dist/bbs-<mode>-<YYYYMMDD-HHMMSS>.tar.gz
#   dist/bbs-<mode>-<YYYYMMDD-HHMMSS>.tar.gz.sha256   （大写哈希）
#
# 说明:
#   - 不再生成 upgrade.sh（已废弃）；升级由 deploy.sh 负责，先验证后切换、失败自动回滚。
#   - 包内**不得**包含 deploy.sh（避免覆盖现场入口脚本）。
#   - mode=migrate 用于"只改结构不改代码"：payload 与现网同版本，deploy.sh 会校验 jar 哈希一致。
# ============================================================================
set -u

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR" || exit 1

MODE="update"          # update | full | migrate
STAGE="release"        # candidate | release
COMMIT=""
VERSION_OVERRIDE=""
DB_COMPAT="backward-compatible"
FROM_PKG=""
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; RED='\033[0;31m'; NC='\033[0m'
info() { printf '%s[INFO]%s %s\n' "$CYAN" "$NC" "$*" >&2; }
ok()   { printf '%s[ OK ]%s %s\n' "$GREEN" "$NC" "$*" >&2; }
warn() { printf '%s[WARN]%s %s\n' "$YELLOW" "$NC" "$*" >&2; }
err()  { printf '%s[FAIL]%s %s\n' "$RED" "$NC" "$*" >&2; exit 1; }

while [ $# -gt 0 ]; do
    case "$1" in
        --full|full) MODE="full"; shift ;;
        --upgrade|upgrade|update|--update) MODE="update"; shift ;;
        --migrate|migrate) MODE="migrate"; shift ;;
        --mode) MODE="${2:-}"; shift 2 ;;
        --mode=*) MODE="${1#*=}"; shift ;;
        --stage) STAGE="${2:-release}"; shift 2 ;;
        --stage=*) STAGE="${1#*=}"; shift ;;
        --commit) COMMIT="${2:-}"; shift 2 ;;
        --commit=*) COMMIT="${1#*=}"; shift ;;
        --db-compat) DB_COMPAT="${2:-}"; shift 2 ;;
        --db-compat=*) DB_COMPAT="${1#*=}"; shift ;;
        --version) VERSION_OVERRIDE="${2:-}"; shift 2 ;;
        --version=*) VERSION_OVERRIDE="${1#*=}"; shift ;;
        --from) FROM_PKG="${2:-}"; shift 2 ;;
        --from=*) FROM_PKG="${1#*=}"; shift ;;
        -h|--help) sed -n '2,/^set -u/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) err "未知参数: $1" ;;
    esac
done

case "$MODE" in update|full|migrate) ;; *) err "--mode 只能是 update|full|migrate" ;; esac
case "$STAGE" in candidate|release) ;; *) err "--stage 只能是 candidate|release" ;; esac

command -v python3 >/dev/null 2>&1 || err "需要 python3（manifest 生成与瘦 jar）"
command -v sha256sum >/dev/null 2>&1 || err "需要 sha256sum"

# ---------------------------------------------------------------- 产物 ------
JAR_FAT="bbs-server/target/bbs-server.jar"
UI_DIST="bbs-ui/dist"
ADMIN_DIST="bbs-admin-ui/dist"

check_artifacts() {
    local missing=0
    [ -f "$JAR_FAT" ]  || { warn "缺少 $JAR_FAT"; missing=1; }
    [ -d "$UI_DIST" ]  || { warn "缺少 $UI_DIST"; missing=1; }
    [ -d "$ADMIN_DIST" ] || { warn "缺少 $ADMIN_DIST"; missing=1; }
    if [ "$missing" = "1" ]; then
        err "构建产物缺失：Windows 侧先跑两个 npm run build + mvn clean package -DskipTests（或用 --from 复用旧包产物）"
    fi
}

# 瘦 jar：剔除 BOOT-INF/lib（依赖由基础镜像 /app/lib 提供）
make_thin_jar() {
    local fat="$1" thin="$2"
    info "生成瘦 jar: $(basename "$thin")"
    python3 - "$fat" "$thin" <<'PY'
import zipfile, sys
src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        if item.filename.startswith('BOOT-INF/lib/'):
            continue
        zout.writestr(item, zin.read(item.filename))
PY
    python3 - "$thin" <<'PY'
import sys, zipfile
z = zipfile.ZipFile(sys.argv[1])
bad = [n for n in z.namelist() if n.startswith('BOOT-INF/lib/')]
sys.exit(1 if bad else 0)
PY
    [ $? -eq 0 ] || err "瘦 jar 仍包含 BOOT-INF/lib"
    ok "瘦 jar 校验通过（不含 BOOT-INF/lib）"
}

# ---------------------------------------------------------------- 组装 ------
WORK="$ROOT_DIR/tmp/pkg-$TIMESTAMP"
PKG_BASENAME=""
VERSION=""
COMMIT_FINAL=""
SOURCE_NOTE="build"

prepare_from_pkg() {
    local pkg="$1"
    [ -f "$pkg" ] || err "旧包不存在: $pkg"
    local x="$WORK/_from"
    mkdir -p "$x"
    tar -xzf "$pkg" -C "$x" || err "旧包解压失败"
    local inner
    inner=$(find "$x" -maxdepth 1 -mindepth 1 -type d | head -1)
    [ -n "$inner" ] || err "旧包内无目录"
    FROM_DIR=""
    # 旧契约：upgrade-*/bbs-server.jar；新契约：payload/app/
    if [ -f "$inner/payload/app/bbs-server.jar" ]; then FROM_DIR="$inner/payload/app"
    elif [ -f "$inner/bbs-server.jar" ]; then FROM_DIR="$inner"
    else
        FROM_DIR=$(find "$x" -maxdepth 3 -name bbs-server.jar -printf '%h\n' | head -1)
    fi
    [ -n "$FROM_DIR" ] || err "旧包里找不到 bbs-server.jar"
    VERSION="$(date -r "$inner" +%Y%m%d_%H%M%S 2>/dev/null || echo "$TIMESTAMP")"
    if [ -f "$inner/version.txt" ]; then
        local v c
        v=$(sed -n 's/^build_time=//p' "$inner/version.txt" | head -1)
        c=$(sed -n 's/^git_commit=//p' "$inner/version.txt" | head -1)
        [ -n "$v" ] && VERSION="$v"
        [ -n "$c" ] && COMMIT="$c"
    elif [ -f "$inner/manifest.env" ]; then
        # 新契约包没有 version.txt，版本/commit 在 manifest.env
        local v c
        v=$(sed -n 's/^M_VERSION=//p' "$inner/manifest.env" | head -1)
        c=$(sed -n 's/^M_COMMIT=//p' "$inner/manifest.env" | head -1)
        [ -n "$v" ] && VERSION="$v"
        [ -n "$c" ] && COMMIT="$c"
    fi
    SOURCE_NOTE="from:$(basename "$pkg")"
    [ -n "$VERSION_OVERRIDE" ] && { VERSION="$VERSION_OVERRIDE"; SOURCE_NOTE="$SOURCE_NOTE+version:$VERSION_OVERRIDE"; }
    info "复用旧包产物：version=$VERSION commit=${COMMIT:-unknown} dir=$FROM_DIR"
}

assemble() {
    local out="$1"
    mkdir -p "$out/payload/app" "$out/scripts" "$out/docs"

    if [ -n "$FROM_PKG" ]; then
        cp -a "$FROM_DIR/bbs-server.jar" "$out/payload/app/" || err "复制 jar 失败"
        [ -d "$FROM_DIR/bbs-ui/dist" ] && mkdir -p "$out/payload/app/bbs-ui" && cp -a "$FROM_DIR/bbs-ui/dist" "$out/payload/app/bbs-ui/"
        [ -d "$FROM_DIR/bbs-admin-ui/dist" ] && mkdir -p "$out/payload/app/bbs-admin-ui" && cp -a "$FROM_DIR/bbs-admin-ui/dist" "$out/payload/app/bbs-admin-ui/"
        # 旧包内是瘦 jar，直接复用，不再二次剔除
        ok "payload 来自旧包（未改代码）"
    else
        make_thin_jar "$JAR_FAT" "$out/payload/app/bbs-server.jar"
        mkdir -p "$out/payload/app/bbs-ui" "$out/payload/app/bbs-admin-ui"
        cp -a "$UI_DIST" "$out/payload/app/bbs-ui/"
        cp -a "$ADMIN_DIST" "$out/payload/app/bbs-admin-ui/"
    fi

    # scripts 全量（排除 .env / 构建产物，避免把开发环境变量和 40MB 测试 jar 带进现场）
    ( cd "$ROOT_DIR/scripts" && tar -cf - \
        --exclude='.env' \
        --exclude='./test/e2e-selenium/target' \
        --exclude='*/node_modules' \
        --exclude='*.log' \
        --exclude='.cache_checksum' . ) | ( cd "$out/scripts" && tar -xf - ) \
        || err "复制 scripts 失败"
    chmod +x "$out"/scripts/prod/*.sh "$out"/scripts/dev/*.sh 2>/dev/null || true

    # 文档（可选，便于现场离线查阅）
    [ -f "$ROOT_DIR/DEPLOY.md" ] && cp -a "$ROOT_DIR/DEPLOY.md" "$out/docs/" 2>/dev/null
    [ -f "$ROOT_DIR/scripts/SCRIPT.md" ] && cp -a "$ROOT_DIR/scripts/SCRIPT.md" "$out/docs/" 2>/dev/null
}

write_manifest() {
    local out="$1"
    [ -n "$VERSION" ] || VERSION="$TIMESTAMP"
    if [ -n "$COMMIT" ] && [ "$COMMIT" != "unknown" ]; then COMMIT_FINAL="$COMMIT"
    else COMMIT_FINAL=$(git rev-parse --short HEAD 2>/dev/null || echo unknown); fi

    python3 - "$out" "$VERSION" "$COMMIT_FINAL" "$MODE" "$STAGE" "$DB_COMPAT" "$SOURCE_NOTE" <<'PY'
import hashlib, json, os, sys
out, version, commit, mode, stage, db_compat, note = sys.argv[1:8]
files = []
for base in ("payload", "scripts"):
    root = os.path.join(out, base)
    if not os.path.isdir(root):
        continue
    for dirpath, _, names in os.walk(root):
        for n in sorted(names):
            p = os.path.join(dirpath, n)
            rel = os.path.relpath(p, out)
            h = hashlib.sha256(open(p, "rb").read()).hexdigest()
            files.append({"path": rel, "sha256": h, "size": os.path.getsize(p)})
m = {
    "product": "daqian-bbs",
    "version": version,
    "commit": commit,
    "mode": mode,
    "stage": stage,
    "built_at": os.environ.get("PKG_BUILT_AT", ""),
    "min_deploy_version": "1.0.0",
    "db_compat": db_compat,
    "source": note,
    "entrypoints": {"deploy": "deploy.sh", "verify": "deploy.sh verify", "diag": "deploy.sh diag"},
    "files": files,
    "deleted": [],
}
open(os.path.join(out, "manifest.json"), "w").write(json.dumps(m, ensure_ascii=False, indent=2) + "\n")
with open(os.path.join(out, "manifest.env"), "w") as f:
    f.write("M_PRODUCT=%s\nM_VERSION=%s\nM_COMMIT=%s\nM_MODE=%s\nM_STAGE=%s\nM_DB_COMPAT=%s\n"
            % ("daqian-bbs", version, commit, mode, stage, db_compat))
with open(os.path.join(out, "SHA256SUMS"), "w") as f:
    for it in files:
        f.write("%s  %s\n" % (it["sha256"], it["path"]))
print("files=%d" % len(files))
PY
    [ $? -eq 0 ] || err "manifest 生成失败"
    ok "manifest.json / manifest.env / SHA256SUMS 已生成"
}

# ---------------------------------------------------------------- 打包 ------
PKG_BUILT_AT="$TIMESTAMP" ; export PKG_BUILT_AT

rm -rf "$WORK"
mkdir -p "$WORK"
cleanup() { [ -n "${WORK:-}" ] && [ -d "$WORK" ] && rm -rf "$WORK"; }
trap cleanup EXIT INT TERM

if [ -n "$FROM_PKG" ]; then
    prepare_from_pkg "$FROM_PKG"
else
    check_artifacts
    VERSION="$TIMESTAMP"
fi
[ -n "$VERSION_OVERRIDE" ] && VERSION="$VERSION_OVERRIDE"

PKG_BASENAME="bbs-$( [ "$MODE" = "update" ] && echo upgrade || echo "$MODE")-$(echo "$TIMESTAMP" | tr '_' '-')"
PKG_DIR="$WORK/$PKG_BASENAME"
mkdir -p "$PKG_DIR"

info "===== 组装 $PKG_BASENAME（version=$VERSION commit=${COMMIT:-auto} mode=$MODE stage=$STAGE） ====="
assemble "$PKG_DIR"

if [ "$MODE" = "full" ]; then
    if [ -d "dist/base-images" ] && ls dist/base-images/*.tar >/dev/null 2>&1; then
        mkdir -p "$PKG_DIR/base"
        cp dist/base-images/*.tar "$PKG_DIR/base/"
        ok "基础镜像已包含（$(du -sh "$PKG_DIR/base" | cut -f1)）"
    else
        warn "dist/base-images/ 为空，完整包将不含基础镜像（仅适用于已加载镜像的机器）"
    fi
fi

write_manifest "$PKG_DIR"

# 包内不得含 deploy.sh
if find "$PKG_DIR" -maxdepth 2 -name 'deploy.sh' | grep -q .; then
    err "包内出现 deploy.sh，违反契约"
fi

mkdir -p "$ROOT_DIR/dist"
OUT="$ROOT_DIR/dist/$PKG_BASENAME.tar.gz"
info "压缩中..."
tar -czf "$OUT" -C "$WORK" "$PKG_BASENAME" || err "压缩失败"

SIZE_MB=$(du -m "$OUT" | cut -f1)
if [ "$MODE" != "full" ] && [ "$SIZE_MB" -gt 15 ]; then
    warn "包体积 ${SIZE_MB}MB > 15MB（预期瘦包 <10MB）"
fi
SHA=$(sha256sum "$OUT" | awk '{print toupper($1)}')
echo "$SHA" > "$OUT.sha256"

# ---------- 交付卡：本次需要上传几个文件 ----------
DEPLOY_SHA=$(sha256sum "$ROOT_DIR/deploy.sh" 2>/dev/null | awk '{print toupper($1)}')
TRACK="$ROOT_DIR/dist/.deploy.sh.sha256"
NEED_DEPLOY=0
if [ -n "$DEPLOY_SHA" ]; then
    if [ ! -f "$TRACK" ] || [ "$(cat "$TRACK" 2>/dev/null)" != "$DEPLOY_SHA" ]; then
        NEED_DEPLOY=1
    fi
    echo "$DEPLOY_SHA" > "$TRACK"
fi
UPLOAD_COUNT=1
[ "$NEED_DEPLOY" = "1" ] && UPLOAD_COUNT=$((UPLOAD_COUNT + 1))

echo ""
ok "打包完成: $OUT（${SIZE_MB}MB）"
echo "  SHA256 : $SHA"
echo "  版本   : $VERSION（commit ${COMMIT:-auto}）"
echo "  内容   : $(tar -tzf "$OUT" | grep -c .) 个条目"
echo ""
echo "================ 交付卡 ================"
echo "需要上传到内网的文件（共 ${UPLOAD_COUNT} 项）："
echo "  1) $(basename "$OUT")  ${SIZE_MB}MB  SHA256=${SHA}"
if [ "$NEED_DEPLOY" = "1" ]; then
    echo "  2) deploy.sh  入口脚本有更新，必须与包一起上传  SHA256=${DEPLOY_SHA}"
    echo "     原因：本包依赖新版 deploy.sh（旧入口不认识 manifest / 不会自动回滚）"
else
    echo "  （deploy.sh 与上次交付相同，不需要重复上传）"
fi
echo ""
echo "现场执行（一条命令，包放当前目录或 U 盘即可）："
case "$MODE" in
    migrate) echo "  sudo bash deploy.sh once migrate" ;;
    full)    echo "  sudo bash deploy.sh once install" ;;
    *)       echo "  sudo bash deploy.sh" ;;
esac
echo "  说明：脚本自动找包 → 校验 SHA → 备份 → 切换 → 验证；失败自动回滚，不用第二条命令"
echo "  回滚（万一）：sudo bash deploy.sh once rollback"
echo "========================================"
