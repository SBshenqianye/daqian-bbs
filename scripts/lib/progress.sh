#!/bin/bash
# ============================================
# BBS 进度指示工具库
# 提供统一的步骤计数、spinner 动画、进度条功能
# 用法: source scripts/lib/progress.sh
# ============================================

# --------------- 颜色（兼容自身定义） ---------------
if [ -z "${RED:-}" ]; then
    RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; BLUE='\033[0;34m'; NC='\033[0m'
fi

# --------------- 显示库标题 ---------------
show_header() {
    local title="$1"
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}  $title"
    echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
    echo ""
}

# --------------- 步骤计数器 ---------------
# 用法: show_step <当前编号> <总步数> <描述>
# 示例: show_step 1 6 "构建前端"
_STEP_CURRENT=0
_STEP_TOTAL=0

show_step() {
    local current=$1 total=$2 desc=$3
    _STEP_CURRENT=$current
    _STEP_TOTAL=$total
    echo ""
    echo -e "${BLUE}━━━ [${current}/${total}] ${desc} ━━━${NC}"
}

# --------------- 实时计时器（单命令） ---------------
# 用法: run_with_timer <提示消息> <命令> [参数...]
# 功能: 后台运行命令，实时显示已用秒数
#       成功时显示 ✓ + 总耗时，失败时显示 ✗ + 日志
run_with_timer() {
    local msg="$1"
    shift
    local log_file
    log_file=$(mktemp /tmp/bbs-timer-XXXXXX.log)

    # 启动后台命令
    "$@" > "$log_file" 2>&1 &
    local pid=$!

    local start_time
    start_time=$(date +%s)

    # 固定占位，避免行长度跳动
    local cols=80
    [ -t 1 ] && cols=$(tput cols 2>/dev/null || echo 80)
    local max_msg_len=$(( cols - 20 ))
    local display_msg="${msg:0:$max_msg_len}"

    printf "${CYAN}[INFO]${NC} %s " "$display_msg"

    # 每秒更新已用时间
    while kill -0 "$pid" 2>/dev/null; do
        local now
        now=$(date +%s)
        local elapsed=$(( now - start_time ))
        printf "\r${CYAN}[INFO]${NC} %s [${elapsed}s]" "$display_msg"
        sleep 1
    done

    # 等待完成
    wait "$pid" 2>/dev/null
    local rc=$?
    local now
    now=$(date +%s)
    local total_sec=$(( now - start_time ))

    if [ $rc -eq 0 ]; then
        printf "\r${CYAN}[INFO]${NC} %s ${GREEN}✓${NC} [${total_sec}s]\n" "$display_msg"
        rm -f "$log_file"
    else
        printf "\r${CYAN}[INFO]${NC} %s ${RED}✗${NC} [${total_sec}s]\n" "$display_msg"
        # 输出错误日志（最多显示 30 行）
        if [ -f "$log_file" ]; then
            local line_count
            line_count=$(wc -l < "$log_file")
            echo -e "${YELLOW}--- 错误日志 (${line_count} 行) ---${NC}"
            if [ "$line_count" -gt 30 ]; then
                tail -30 "$log_file"
                echo -e "${YELLOW}... (省略 $(( line_count - 30 )) 行)${NC}"
            else
                cat "$log_file"
            fi
            echo -e "${YELLOW}---${NC}"
        fi
        rm -f "$log_file"
        return $rc
    fi
}

# （向下兼容）run_with_spinner 是 run_with_timer 的别名
run_with_spinner() {
    run_with_timer "$@"
}

# --------------- 并行任务监控 ---------------
# 用法: track_parallel <标题> <PID1> [标签1] <PID2> [标签2] ...
# 监视多个后台进程，实时显示各进程运行时间
track_parallel() {
    local title="$1"
    shift

    # 解析 PID 对
    local -a pids=()
    local -a labels=()
    local -a done=()
    local count=0

    while [ $# -gt 0 ]; do
        if [[ "$1" =~ ^[0-9]+$ ]]; then
            pids+=("$1")
            labels+=("Task $((count + 1))")
            done+=(0)
            count=$((count + 1))
            shift
        else
            local label="$1"
            shift
            if [ $# -gt 0 ] && [[ "$1" =~ ^[0-9]+$ ]]; then
                pids+=("$1")
                labels+=("$label")
                done+=(0)
                count=$((count + 1))
                shift
            else
                break
            fi
        fi
    done

    if [ $count -eq 0 ]; then
        return
    fi

    # done[j]: 0=运行中, 1=成功, 2=失败
    local start_time
    start_time=$(date +%s)

    echo -e "${CYAN}[INFO]${NC} $title"

    while true; do
        local all_done=1
        local line="  "
        local j
        for j in $(seq 0 $((count - 1))); do
            case ${done[$j]} in
                0)  # 运行中
                    if kill -0 "${pids[$j]}" 2>/dev/null; then
                        all_done=0
                        local now; now=$(date +%s)
                        local sec=$(( now - start_time ))
                        line+="${labels[$j]}:[${sec}s]  "
                    else
                        # 进程刚结束
                        wait "${pids[$j]}" 2>/dev/null
                        if [ $? -eq 0 ]; then
                            done[$j]=1
                            line+="${labels[$j]}:${GREEN}✓${NC}  "
                        else
                            done[$j]=2
                            line+="${labels[$j]}:${RED}✗${NC}  "
                        fi
                    fi
                    ;;
                1)  line+="${labels[$j]}:${GREEN}✓${NC}  " ;;
                2)  line+="${labels[$j]}:${RED}✗${NC}  " ;;
            esac
        done

        printf "\r%-80b" "$line"
        [ $all_done -eq 1 ] && break
        sleep 1
    done
    echo ""

    # 检查是否有失败的任务
    for j in $(seq 0 $((count - 1))); do
        [ ${done[$j]} -eq 2 ] && return 1
    done
    return 0
}

# --------------- 进度条（百分比） ---------------
# 用法: progress_bar <当前值> <总值> [标签] [宽度]
# 示例: progress_bar 5 10 "处理中" 30
progress_bar() {
    local current=$1 total=$2 label="${3:-Progress}" width="${4:-30}"
    local pct=$(( current * 100 / total ))
    local filled=$(( current * width / total ))
    local empty=$(( width - filled ))

    [ "$total" -eq 0 ] && { pct=0; filled=0; empty=$width; }

    local bar
    bar=$(printf "%${filled}s" | tr ' ' '█')
    bar+=$(printf "%${empty}s" | tr ' ' '·')

    printf "\r${CYAN}[INFO]${NC} %-20s [${bar}] %3d%%" "$label" "$pct"
    if [ "$current" -ge "$total" ]; then
        echo ""
    fi
}

# --------------- 等待提示（轮询循环用） ---------------
# 用法: polling_spinner <消息前缀> <当前轮次> <总轮次> <开始时间戳>
# 显示: "[INFO] 等待后端就绪... [5s] (3/30)"
polling_spinner() {
    local msg=$1 current=$2 total=$3 start_time=$4
    local now; now=$(date +%s)
    local elapsed=$(( now - start_time ))
    printf "\r${CYAN}[INFO]${NC} ${msg}... [${elapsed}s] (${current}/${total})"
}

# --------------- 清理状态 ---------------
polling_clear() {
    printf "\r%80s\r"
}

# --------------- 计时器 ---------------
# 用法: timer_start; ... duration=$(timer_end)
timer_start() {
    date +%s%N
}

timer_end() {
    local start_time=$1
    local end_time
    end_time=$(date +%s%N)
    local elapsed_ns=$(( end_time - start_time ))
    local elapsed_ms=$(( elapsed_ns / 1000000 ))
    local sec=$(( elapsed_ms / 1000 ))
    local ms=$(( elapsed_ms % 1000 ))

    if [ $sec -ge 60 ]; then
        local min=$(( sec / 60 ))
        sec=$(( sec % 60 ))
        echo "${min}m${sec}s"
    elif [ $sec -ge 1 ]; then
        echo "${sec}.$(printf "%03d" $ms | cut -c1-1)s"
    else
        echo "${ms}ms"
    fi
}
