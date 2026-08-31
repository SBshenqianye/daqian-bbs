// 全局错误捕获与自动恢复：防止渲染/patch 错误导致内容区白屏后只能靠手动刷新恢复。
// 必须在 new Vue() 之前调用 installErrorHandler(router)。
import Vue from 'vue'
import bus from '@/components/common/bus'

const REPAIR_KEY = 'bbs-admin-repair'         // 最近一次"软修复"时间戳
const RELOAD_KEY = 'bbs-admin-reload'         // 最近一次整页刷新时间戳
const CHUNK_RETRY_KEY = 'bbs-admin-chunk-retry' // 最近一次 chunk 重试时间戳

const REPAIR_WINDOW = 5000  // 两次软修复最小间隔；窗口内复发 → 硬刷新
const RELOAD_QUIET = 10000  // 硬刷新后静默期；期间再崩 → 放弃自动恢复（防确定性崩溃死循环）

const ts = () => Date.now()
const read = (key) => parseInt(window.sessionStorage.getItem(key) || '0', 10)
const write = (key, t) => window.sessionStorage.setItem(key, String(t))

let lastIntended = null // 最近一次导航意图（chunk 加载失败重试用；router.onError 拿不到 to）

function attemptRepair(router) {
    const t = ts()
    if (t - read(RELOAD_KEY) < RELOAD_QUIET) return // 刚硬刷新过又崩 → 确定性崩溃，只留日志
    if (t - read(REPAIR_KEY) < REPAIR_WINDOW) {
        // 软修复 5s 内复发 → 补丁树已损坏（vm._vnode 指向半成品 vnode），整页刷新
        write(RELOAD_KEY, t)
        window.location.reload()
        return
    }
    write(REPAIR_KEY, t)
    // Home.vue 收到后 pageNonce++ → router-view :key 变化 → 整页重挂载（URL 不变，
    // 不会在标签栏留下假标签）
    bus.$emit('repair-page')
}

function retryNavigation(router) {
    const t = ts()
    if (t - read(RELOAD_KEY) < RELOAD_QUIET) return // 刚刷新过 → 确定性失败，只留日志
    if (t - read(CHUNK_RETRY_KEY) < REPAIR_WINDOW) {
        // 重试后又失败 → chunk 列表已过期（如部署替换了构建产物），整页刷新拉新 index.html
        write(RELOAD_KEY, t)
        window.location.reload()
        return
    }
    write(CHUNK_RETRY_KEY, t)
    if (lastIntended && lastIntended !== router.currentRoute.fullPath) {
        router.replace(lastIntended).catch(() => {}) // NavigationDuplicated 等吞掉
    } else {
        write(RELOAD_KEY, t)
        window.location.reload()
    }
}

export function installErrorHandler(router) {
    // —— Vue 渲染/patch 错误 → 日志 + 自动修复 ——
    Vue.config.errorHandler = (err, vm, info) => {
        console.error('[VueError]', info, err, vm && vm.$options && vm.$options.name)
        // 只有 nextTick/render 层的错误（如 patch 中途异常）才会导致内容区空白；
        // watcher 回调/生命周期等局部错误不触发修复，避免误伤正常页面
        if (info === 'nextTick' || info === 'render') {
            // 排除无害的事件清理错误：组件销毁时 vnode.elm 为 undefined 导致
            // removeEventListener 失败，不影响功能，不应触发白屏修复
            const msg = (err && err.message) || ''
            if (msg.includes('removeEventListener')) {
                console.warn('[VueError:cleanup] 事件清理错误已忽略，不触发修复')
                return
            }
            setTimeout(() => attemptRepair(router), 50) // 等当前微任务队列沉淀
        }
    }

    // —— 未处理的 Promise 拒绝（如接口失败后的 resp.obj 报错）→ 仅日志 ——
    window.addEventListener('unhandledrejection', (e) => {
        console.error('[UnhandledRejection]', e.reason)
    })

    // —— 全局 error 事件 → 仅日志；捕获阶段才能收到资源（chunk script）加载错误 ——
    window.addEventListener('error', (e) => {
        if (e && e.target && e.target.tagName === 'SCRIPT') {
            console.error('[ScriptError]', e.target.src)
        } else if (e && e.error) {
            console.error('[WindowError]', e.error)
        }
    }, true)

    // —— chunk 加载失败 → 重试上次导航意图，再失败则整页刷新 ——
    router.beforeEach((to, from, next) => {
        lastIntended = to.fullPath
        next()
    })
    router.onError((err) => {
        console.error('[RouterError]', err)
        if (/Loading chunk \d+ failed/i.test((err && err.message) || '')) {
            retryNavigation(router)
        }
    })
}
