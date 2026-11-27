#!/bin/bash
# ============================================================================
# tidy.sh —— 部署根盘点 + 可选清理
#
#   bash tidy.sh                          # 只读盘点：哪些是新的、哪些是旧的、能回收多少
#   bash tidy.sh --clean                  # 按保留数清理（会先列清单，输入 yes 才执行）
#   bash tidy.sh --clean --keep-versions 3 --keep-archive 3
#
# 默认保留：versions 旧版本目录 5 个、archive 旧升级包 5 个（最新的留下）
#
# 只做这些写操作（且必须 --clean + 输入 yes）：
#   - 根目录的 bbs-*.tar.gz 移进 packages/
#   - archive/ 里超期（不是最新 N 个）的 bbs-*.tar.gz 删除
#   - versions/ 里超期（不是最新 M 个）的旧版本目录删除
# 永不触碰：releases/ current versions/latest scripts/ bbsUpload/ .env backups/
#           base-images/ legacy/ sql_backups/ collect/ 以及所有不认识的目录
# ============================================================================
set -u

H="${BBS_HOME:-/data/bbs}"
KEEP_V=5
KEEP_A=5
CLEAN=0
MOVE=1

while [ $# -gt 0 ]; do
    case "$1" in
        --clean) CLEAN=1; shift ;;
        --keep-versions) KEEP_V="${2:-5}"; shift 2 ;;
        --keep-versions=*) KEEP_V="${1#*=}"; shift ;;
        --keep-archive) KEEP_A="${2:-5}"; shift 2 ;;
        --keep-archive=*) KEEP_A="${1#*=}"; shift ;;
        --no-move) MOVE=0; shift ;;
        -h|--help) sed -n '2,/^set -u/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "未知参数: $1"; exit 2 ;;
    esac
done

[ -d "$H" ] || { echo "[FAIL] 部署根不存在: $H"; exit 3; }
cd "$H" || exit 3

hr() { echo "------------------------------------------------------------"; }
sz() { du -sh "$1" 2>/dev/null | awk '{print $1}'; }

CUR_REL=""
[ -L releases/current ] && CUR_REL=$(basename "$(readlink -f releases/current 2>/dev/null)")
CUR_LEGACY=""
if [ -L versions/latest ]; then
    CUR_LEGACY=$(basename "$(readlink -f versions/latest 2>/dev/null)")
    case "$(readlink versions/latest 2>/dev/null)" in
        ../releases/current*) CUR_LEGACY="" ;;
    esac
fi

echo "部署根: $H"
hr
echo "【1】现在到底在用哪个"
if [ -n "$CUR_REL" ]; then
    echo "  新布局（当前运行）: releases/$CUR_REL"
    echo "  容器挂的就是它下面的 app/（真实路径）"
else
    echo "  新布局: 未启用（还没迁移，或已撤销迁移）"
fi
if [ -n "$CUR_LEGACY" ]; then
    echo "  旧布局（容器挂 current/...）: versions/$CUR_LEGACY"
fi
[ -f current.json ] && sed -n 's/^ *"\(current\|previous\|failed\)": *"\([^"]*\)".*/  \1 = \2/p' current.json
hr

echo "【2】根目录全量速查（★=在用，○=历史，？=契约外/未识别）"
role() {
    case "$1" in
        releases)          echo "★|新布局版本（运行中）" ;;
        versions)          echo "○|旧布局版本（迁移后仅历史/取证）" ;;
        packages)          echo "★|新归档包（deploy.sh 写）" ;;
        archive)           echo "○|旧归档包（迁移前脚本写）" ;;
        backups)           echo "★|DB/配置备份（别删）" ;;
        log|logs)          echo "★|日志/诊断包目录" ;;
        data)              echo "★|预留数据目录" ;;
        upload)            echo "★|上传目录（通常软链到 bbsUpload）" ;;
        bbsUpload)         echo "★|上传文件（业务数据，别删）" ;;
        src)               echo "★|便捷软链 → current/app" ;;
        pgbin)             echo "★|pgfix 生成的 pg_dump/psql 包装" ;;
        scripts)           echo "★|运维脚本（新版）" ;;
        base-images)       echo "★|基础镜像缓存" ;;
        legacy)            echo "○|旧入口脚本副本（可留可删）" ;;
        tmp)               echo "★|临时目录（可清）" ;;
        deploy.env)        echo "★|主配置（存在则优先于 .env）" ;;
        .env)              echo "★|兼容配置（容器/旧脚本读它）" ;;
        current)           echo "★|兼容软链 → versions/latest" ;;
        current.json)      echo "★|当前/上一版状态" ;;
        deploy.sh)         echo "★|唯一入口" ;;
        deploy-offline.sh) echo "○|兼容壳（转 deploy.sh）" ;;
        pgfix.sh)          echo "★|现场工具（一次性，可删）" ;;
        tidy.sh)           echo "★|现场工具（一次性，可删）" ;;
        .deploy.lock)      echo "？|锁残留（确认无部署在跑后可删）" ;;
        .env.bak-*)        echo "○|历史配置备份（可删）" ;;
        *.tar.gz.sha256)   echo "★|包校验文件（跟着包一起归档）" ;;
        bbs-*.tar.gz)      echo "？|根目录待发包（应进 packages/）" ;;
        *.tar.gz)          echo "？|非部署包（测试/临时，确认后可删）" ;;
        *.log)             echo "？|日志文件（可删）" ;;
        sql_backups)       echo "？|契约外：历史 DB 备份（建议并入 backups/db）" ;;
        collect)           echo "？|契约外：采集产物（建议移出部署根）" ;;
        bbs-new-env)       echo "？|契约外：就地解压的全量包（确认后可删）" ;;
        scripts-bak)       echo "？|契约外：手工脚本备份（可删）" ;;
        *.tar.gz)          echo "？|根目录待发包（应进 packages/）" ;;
        *)                 echo "？|契约外/未识别（不动）" ;;
    esac
}
ls -A1 "$H" 2>/dev/null | sort | while IFS= read -r e; do
    [ -e "$e" ] || [ -L "$e" ] || continue
    info=$(role "$e")
    mark=${info%%|*}
    note=${info#*|}
    if [ -L "$e" ]; then
        target=$(readlink "$e" 2>/dev/null)
        printf '  %s %-20s %-8s %s  -> %s\n' "$mark" "$e" "-" "$note" "$target"
    elif [ -d "$e" ]; then
        n=$(find "$e" -maxdepth 1 -mindepth 1 2>/dev/null | wc -l)
        printf '  %s %-20s %-8s %s  [%s 项]\n' "$mark" "$e" "$(sz "$e")" "$note" "$n"
    else
        printf '  %s %-20s %-8s %s\n' "$mark" "$e" "$(sz "$e")" "$note"
    fi
done
UNK=$(ls -A1 "$H" 2>/dev/null | while IFS= read -r e; do role "$e"; done | grep -c '^？')
echo "  契约外/未识别: $UNK 项（上面标 ？ 的；脚本不会动它们）"
hr

echo "【3】旧版本目录 versions/（保留最新 $KEEP_V 个）"
if [ -d versions ]; then
    ls -1 versions 2>/dev/null | grep -v '^latest$' | sort -r > /tmp/.tidy_v.$$
    total=$(wc -l < /tmp/.tidy_v.$$)
    echo "  共 $total 个，合计 $(sz versions)"
    head -3 /tmp/.tidy_v.$$ | sed 's/^/    最新: /'
    tail -1 /tmp/.tidy_v.$$ | sed 's/^/    最旧: /'
    if [ "$total" -gt "$KEEP_V" ]; then
        echo "  可按 --keep-versions $KEEP_V 回收: $((total - KEEP_V)) 个"
        tail -n $((total - KEEP_V)) /tmp/.tidy_v.$$ | sed 's/^/    候选: /'
    fi
    rm -f /tmp/.tidy_v.$$
fi
hr

echo "【4】归档包（保留最新 $KEEP_A 个/每处）"
for d in archive packages; do
    [ -d "$d" ] || continue
    ls -1 "$d"/*.tar.gz 2>/dev/null | sort -r > /tmp/.tidy_a.$$
    n=$(wc -l < /tmp/.tidy_a.$$)
    [ "$n" -eq 0 ] && { echo "  $d/: 空"; rm -f /tmp/.tidy_a.$$; continue; }
    echo "  $d/: $n 个，合计 $(sz "$d")（最新: $(basename "$(head -1 /tmp/.tidy_a.$$)")）"
    [ "$n" -gt "$KEEP_A" ] && echo "    可按 --keep-archive $KEEP_A 回收: $((n - KEEP_A)) 个"
    rm -f /tmp/.tidy_a.$$
done
echo "  根目录待发包:"
ls -1 bbs-*.tar.gz 2>/dev/null | sed 's/^/    /' || true
[ -z "$(ls -1 bbs-*.tar.gz 2>/dev/null)" ] && echo "    （无）"
hr

echo "【5】备份 backups/"
if [ -d backups ]; then
    n=$(find backups -maxdepth 1 -mindepth 1 -type d | wc -l)
    echo "  $n 份，合计 $(sz backups)；最近一次: $(ls -1t backups 2>/dev/null | head -1)"
    [ -f "$(ls -1dt backups/*/latest-dump.path 2>/dev/null | head -1)" ] && \
        echo "  最近 dump: $(cat "$(ls -1dt backups/*/latest-dump.path | head -1)")"
else
    echo "  无"
fi
hr

if [ "$CLEAN" = "0" ]; then
    echo "以上只读，没有改动任何东西。"
    echo "要清理就跑（会给清单让你输 yes）："
    echo "    bash tidy.sh --clean"
    exit 0
fi

# ---------------------------------------------------------------- 清理 ----------
echo "【清理预览】"
ACTIONS=0
if [ "$MOVE" = "1" ]; then
    for f in bbs-*.tar.gz; do
        [ -f "$f" ] || continue
        if [ -f "packages/$f" ] && cmp -s "$f" "packages/$f"; then
            echo "  dedupe $f（packages/ 已有同一份，删根目录副本）"
        else
            echo "  move  $f -> packages/"
        fi
        ACTIONS=$((ACTIONS + 1))
    done
fi
if [ -d archive ]; then
    n=$(ls -1 archive/*.tar.gz 2>/dev/null | wc -l)
    if [ "$n" -gt "$KEEP_A" ]; then
        ls -1t archive/*.tar.gz | tail -n $((n - KEEP_A)) | while read -r f; do echo "  del   $f"; done
        ACTIONS=$((ACTIONS + n - KEEP_A))
    fi
fi
if [ -d versions ]; then
    n=$(ls -1 versions 2>/dev/null | grep -v '^latest$' | wc -l)
    if [ "$n" -gt "$KEEP_V" ]; then
        ls -1 versions | grep -v '^latest$' | sort -r | tail -n $((n - KEEP_V)) | while read -r v; do
            [ -n "$CUR_LEGACY" ] && [ "$v" = "$CUR_LEGACY" ] && { echo "  skip  versions/$v（当前旧布局在用）"; continue; }
            echo "  del   versions/$v"
        done
        ACTIONS=$((ACTIONS + n - KEEP_V))
    fi
fi

if [ "$ACTIONS" -eq 0 ]; then
    echo "  没有可清理的（已经是最新 $KEEP_V/$KEEP_A 之内）"
    exit 0
fi
echo ""
printf '确认执行？输入 yes 回车，其它任意键取消: '
read -r ans
[ "$ans" = "yes" ] || { echo "已取消，未做任何改动"; exit 0; }

echo "【执行】"
if [ "$MOVE" = "1" ]; then
    mkdir -p packages
    for f in bbs-*.tar.gz; do
        [ -f "$f" ] || continue
        if [ -f "packages/$f" ] && cmp -s "$f" "packages/$f"; then
            rm -f "$f" "$f.sha256" && echo "  deduped $f（已删除根目录重复副本）"
        else
            mv -f "$f" packages/ && echo "  moved $f"
            [ -f "$f.sha256" ] && mv -f "$f.sha256" packages/ 2>/dev/null
        fi
    done
fi
if [ -d archive ]; then
    n=$(ls -1 archive/*.tar.gz 2>/dev/null | wc -l)
    if [ "$n" -gt "$KEEP_A" ]; then
        ls -1t archive/*.tar.gz | tail -n $((n - KEEP_A)) | while read -r f; do
            rm -f "$f" "$f.sha256" && echo "  deleted $f"
        done
    fi
fi
if [ -d versions ]; then
    n=$(ls -1 versions 2>/dev/null | grep -v '^latest$' | wc -l)
    if [ "$n" -gt "$KEEP_V" ]; then
        ls -1 versions | grep -v '^latest$' | sort -r | tail -n $((n - KEEP_V)) | while read -r v; do
            [ -n "$CUR_LEGACY" ] && [ "$v" = "$CUR_LEGACY" ] && { echo "  skip versions/$v"; continue; }
            rm -rf "versions/$v" && echo "  deleted versions/$v"
        done
    fi
fi
echo ""
echo "清理完成。当前磁盘: $(df -hP . | awk 'NR==2{print $4" 可用（"$5" 已用）"}')"
echo "如需再核对：bash deploy.sh status"
