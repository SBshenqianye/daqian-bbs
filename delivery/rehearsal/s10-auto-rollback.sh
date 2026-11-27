#!/bin/bash
# ============================================================================
# 演练 S10：先验证后切换 + 失败自动回滚
#
# 构造一个"哈希全对、但起不来"的坏包（jar 是合法 ZIP 但不是 Spring 应用），
# 期望：deploy.sh 切换 → 容器起不来 → verify 失败 → **自动切回上一版** →
#       端点恢复 200 → 退出码 1 且包不归档。
# ============================================================================
set -u
R="${BBS_HOME_TEST:-$HOME/bbs-mig}"
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
BASE="${1:-$REPO/dist/bbs-upgrade-20261127-093351.tar.gz}"
PASS=0; FAIL=0
okc()  { PASS=$((PASS+1)); printf '[PASS] %s\n' "$*"; }
badc() { FAIL=$((FAIL+1)); printf '[FAIL] %s\n' "$*"; }
chk()  { if eval "$2" >/dev/null 2>&1; then okc "$1"; else badc "$1"; fi; }

cd "$R" || exit 1
CUR_BEFORE=$(readlink -f "$R/releases/current")
echo "===== S10 构造坏包（可启动性故障，哈希全对） ====="
echo "当前版本: $CUR_BEFORE"

T=$(mktemp -d)
tar -xzf "$BASE" -C "$T"
INNER=$(find "$T" -maxdepth 2 -name manifest.json -printf '%h\n' | head -1)
[ -n "$INNER" ] || { badc "基线包解压失败"; exit 1; }

# 生成一个"合法 ZIP"的假 jar
mkdir -p "$T/fake"
echo "not a spring boot jar" > "$T/fake/README.txt"
python3 - "$T/fake" "$INNER/payload/app/bbs-server.jar" <<'PY'
import zipfile, os, sys
src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(dst, 'w') as z:
    for n in os.listdir(src):
        z.write(os.path.join(src, n), n)
PY

# 改版本号，保证它是"新版本"（manifest 不在 SHA256SUMS 覆盖范围内）
V="20261127_100000"
sed -i "s/\"version\": \"[^\"]*\"/\"version\": \"$V\"/" "$INNER/manifest.json"
sed -i "s/^M_VERSION=.*/M_VERSION=$V/" "$INNER/manifest.env"

# 重新生成包内逐文件哈希（让坏 jar"合法通过"完整性校验）
( cd "$INNER" && find payload scripts -type f -print0 | sort -z | xargs -0 -r sha256sum > SHA256SUMS )

BAD="$R/bad-runtime.tar.gz"
( cd "$T" && tar -czf "$BAD" "$(basename "$INNER")" )
rm -rf "$T"
echo "坏包: $BAD（$(du -m "$BAD" | cut -f1)MB）"

echo "===== 执行 deploy（预期退出码 1 + 自动回滚） ====="
bash deploy.sh --package "$(basename "$BAD")" --yes > "$R/s10.log" 2>&1
RC=$?
sed -n '/重建容器/,$p' "$R/s10.log" | head -40

CUR_AFTER=$(readlink -f "$R/releases/current")
chk "坏版本切换后验证失败 → 退出码 1（实际 $RC）" "[ '$RC' = '1' ]"
chk "已自动回滚到原版本" "[ '$CUR_AFTER' = '$CUR_BEFORE' ]"
chk "回滚后后端 200" "[ \"\$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:60100/bbs-server/)\" = '200' ]"
chk "回滚后用户端 200" "[ \"\$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:60101/bbs-user/)\" = '200' ]"
chk "坏包未归档（保留现场）" "[ ! -f '$R/packages/$(basename "$BAD")' ]"
chk "坏 release 目录保留（取证）" "[ -d '$R/releases/$V-291ae68' ]"
chk "current.json 记录的是回滚后版本" "grep -q '$(basename "$CUR_BEFORE")' '$R/current.json'"

echo ""
echo "===== S10 结果: PASS=$PASS FAIL=$FAIL ====="
[ "$FAIL" -eq 0 ]
