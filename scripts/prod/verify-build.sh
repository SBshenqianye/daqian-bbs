#!/bin/bash
# ============================================
# 一次性验证脚本（构建机 / 开发机跑）
# 验证"依赖下沉"改造后的构建产物是否合格。
# 前置：已完成 mvn clean package -DskipTests + bash scripts/prod/package.sh
# 用法: bash scripts/prod/verify-build.sh
# 退出码: 0=全部通过  1=有失败项
# ============================================
set -u

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

PASS=0; FAIL=0

check() { # $1=描述  $2=判断命令(返回0为通过)
    if eval "$2" >/dev/null 2>&1; then
        echo "  [PASS] $1"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] $1"
        FAIL=$((FAIL + 1))
    fi
}

FAT="bbs-server/target/bbs-server.jar"
LIB="bbs-server/target/lib"

echo "===== 1. 后端构建产物 ====="
check "fat jar 存在: $FAT"                    "[ -f '$FAT' ]"
check "依赖目录存在: $LIB"                     "[ -d '$LIB' ]"

if [ -f "$FAT" ]; then
    check "fat jar 为 PropertiesLauncher 布局" \
        "unzip -p '$FAT' META-INF/MANIFEST.MF | grep -q 'Main-Class: org.springframework.boot.loader.PropertiesLauncher'"
fi

if [ -f "$FAT" ] && [ -d "$LIB" ]; then
    # 正确标准：fat jar 内置依赖必须全部存在于外置 lib（一个都不能缺）；
    # 外置允许多出空壳 starter（pom 聚合 jar，无类，repackage 有意排除，无害）。
    # 豁免：spring-boot-jarmode-layertools-* 是 repackage 自动嵌入的专用 jar
    #      （仅供 -Djarmode=layertools 提取依赖用），非运行依赖，正常启动不加载。
    MISSING=$(python3 - "$FAT" "$LIB" <<'PY'
import zipfile, os, sys
with zipfile.ZipFile(sys.argv[1]) as z:
    inner = {os.path.basename(n) for n in z.namelist()
             if n.startswith('BOOT-INF/lib/') and n.endswith('.jar')
             and not os.path.basename(n).startswith('spring-boot-jarmode-layertools')}
outer = {n for n in os.listdir(sys.argv[2]) if n.endswith('.jar')}
print(' '.join(sorted(inner - outer)))
PY
)
    if [ -z "$MISSING" ]; then
        check "内置依赖全部在外置 lib 中（无缺失）" "true"
    else
        check "内置依赖全部在外置 lib 中（缺失: $MISSING）" "false"
    fi
fi

echo "===== 2. 升级包（新契约 v2） ====="
LATEST=$(ls -t dist/bbs-upgrade-*.tar.gz dist/bbs-migrate-*.tar.gz 2>/dev/null | head -1)
check "存在升级包" "[ -n '$LATEST' ]"

if [ -n "$LATEST" ]; then
    pkg_mb=$(du -m "$LATEST" | cut -f1)
    check "升级包体积 < 15MB（实际 ${pkg_mb}MB）" "[ '$pkg_mb' -lt 15 ]"

    TMP=$(mktemp -d)
    tar -xzf "$LATEST" -C "$TMP"
    INNER=$(find "$TMP" -maxdepth 2 -name manifest.json -printf '%h\n' | head -1)
    check "包内含 manifest.json" "[ -n '$INNER' ]"

    if [ -n "$INNER" ]; then
        check "包内含 manifest.env（deploy.sh 无需 python3）" "[ -f '$INNER/manifest.env' ]"
        check "包内含 SHA256SUMS" "[ -f '$INNER/SHA256SUMS' ]"
        check "包内逐文件哈希可校验" "( cd '$INNER' && sha256sum -c --quiet SHA256SUMS )"
        check "包内无 deploy.sh（不得覆盖现场入口）" "! find '$INNER' -maxdepth 2 -name deploy.sh | grep -q ."
        check "包内含 payload/app/bbs-server.jar" "[ -f '$INNER/payload/app/bbs-server.jar' ]"
        check "包内含 payload/app/bbs-ui/dist"     "[ -d '$INNER/payload/app/bbs-ui/dist' ]"
        check "包内含 payload/app/bbs-admin-ui/dist" "[ -d '$INNER/payload/app/bbs-admin-ui/dist' ]"
        check "包内含 scripts/prod/package.sh（脚本全量）" "[ -f '$INNER/scripts/prod/package.sh' ]"
        check "包内含 scripts/prod/one_key_deploy.sh（兼容壳）" "[ -f '$INNER/scripts/prod/one_key_deploy.sh' ]"
        check "包内不含 .env（避免把构建机配置带到现场）" "! find '$INNER/scripts' -maxdepth 1 -name '.env' | grep -q ."

        if [ -f "$INNER/manifest.json" ]; then
            MV=$(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$INNER/manifest.json" | head -1)
            MC=$(sed -n 's/.*"commit"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$INNER/manifest.json" | head -1)
            MM=$(sed -n 's/.*"mode"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$INNER/manifest.json" | head -1)
            MS=$(sed -n 's/.*"source"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$INNER/manifest.json" | head -1)
            echo "      manifest: version=$MV commit=$MC mode=$MM source=$MS"
            check "manifest.version 非空" "[ -n '$MV' ]"
            check "manifest.commit 非空（不是 unknown）" "[ -n '$MC' ] && [ '$MC' != 'unknown' ]"
            # 版本号必须来自产物（包内 version），不能是文件名时间戳：只校验形态
            check "manifest.version 形态合法（YYYYmmdd_HHMMSS）" "printf '%s' '$MV' | grep -Eq '^[0-9]{8}_[0-9]{6}$'"
            HEAD_SHORT=$(git rev-parse --short HEAD 2>/dev/null)
            case "$MS" in
                from:*)
                    echo "      注意：本包由旧包重打（$MS），commit 记录为 $MC，不做 HEAD 比对"
                    check "重打包记录了来源包" "[ -n '$MS' ]"
                    ;;
                *)
                    check "manifest.commit 与仓库 HEAD 一致（$HEAD_SHORT）" "[ '$MC' = '$HEAD_SHORT' ]"
                    ;;
            esac
        fi

        if [ -f "$INNER/payload/app/bbs-server.jar" ]; then
            jar_mb=$(du -m "$INNER/payload/app/bbs-server.jar" | cut -f1)
            check "包内 jar 为瘦 jar < 5MB（实际 ${jar_mb}MB）" "[ '$jar_mb' -lt 5 ]"
            check "瘦 jar 不含 BOOT-INF/lib" \
                "python3 -c \"import zipfile; z=zipfile.ZipFile('$INNER/payload/app/bbs-server.jar'); assert not any(n.startswith('BOOT-INF/lib/') for n in z.namelist())\""
        fi
    fi
    rm -rf "$TMP"
fi

echo ""
echo "===== 结果: PASS=$PASS  FAIL=$FAIL ====="
[ "$FAIL" -eq 0 ]
