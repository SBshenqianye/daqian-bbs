/**
 * 通知未读数全局 Store（单例）
 *
 * 架构说明：
 * - 后端 /notification/unreadSummary 按"分类(category)"返回独立未读计数：
 *   interaction（互动消息，对应"回复我的"）与 system（系统通知，对应"消息通知"），
 *   total 恒等于各分类之和（可加性）。
 * - 本模块用 Vue.observable 提供全站唯一的响应式未读状态，所有角标/红点
 *   （BBSHeader 头像、菜单项、BBSMyReplies tab、BBSNotifications 页头）
 *   直接读取 state，不再通过 $bus 事件接力或各自轮询。
 * - 全站只有一个 30s 轮询定时器，由登录态入口（BBSHeader）启停。
 *
 * 扩展新通知类型：后端 NotificationCategory 注册 type → category 后，
 * 分类计数自动生效；若新增"分类"，仅需在此处 DEFAULT_CATEGORIES 加默认键，
 * 并在展示处绑定 store.count(category)。
 */
import Vue from 'vue'
import { getRequest, postRequest } from '@/api/api'
import { getUser } from '@/utils/auth'

/** 互动消息（回复/评论我）——对应"回复我的"页面 */
export const CATEGORY_INTERACTION = 'interaction'
/** 系统通知（采纳/审批/举报/违规等）——对应"消息通知"页面 */
export const CATEGORY_SYSTEM = 'system'

/** 已知分类默认值：保证键存在，Vue 2 响应式才能追踪后续赋值 */
const DEFAULT_CATEGORIES = {
  [CATEGORY_INTERACTION]: 0,
  [CATEGORY_SYSTEM]: 0,
}

const POLL_INTERVAL = 30000

const state = Vue.observable({
  /** 未读总数 = 各分类之和 */
  total: 0,
  /** 按分类未读计数 { interaction: n, system: n } */
  byCategory: { ...DEFAULT_CATEGORIES },
  /** 按类型未读计数 { reply: n, ... }（细粒度，备用） */
  byType: {},
  /** 是否已成功拉取过一次 */
  loaded: false,
})

let pollTimer = null
let refreshing = false

function loggedIn() {
  const user = getUser()
  return !!(user && user.id)
}

/** 拉取未读汇总并整体替换状态（整体替换保证新增分类键也响应式） */
export async function refresh() {
  const user = getUser()
  if (refreshing || !user || !user.id) return
  refreshing = true
  try {
    const resp = await getRequest(`/notification/unreadSummary?userId=${user.id}`)
    const summary = (resp && resp.obj) || {}
    const byCategory = { ...DEFAULT_CATEGORIES }
    const serverCats = summary.byCategory || {}
    Object.keys(serverCats).forEach(key => {
      byCategory[key] = Number(serverCats[key]) || 0
    })
    const byType = {}
    const serverTypes = summary.byType || {}
    Object.keys(serverTypes).forEach(key => {
      byType[key] = Number(serverTypes[key]) || 0
    })
    state.total = Number(summary.total) || 0
    state.byCategory = byCategory
    state.byType = byType
    state.loaded = true
  } catch (e) {
    // 静默失败：保持上一次计数，等待下次轮询
  } finally {
    refreshing = false
  }
}

/** 读取某分类未读数（组件模板/computed 直接调用即可，响应式） */
export function count(category) {
  return state.byCategory[category] || 0
}

/**
 * 标记某分类全部已读（乐观更新 + 服务端确认 + 回读校准）
 * 只影响该分类，其他分类未读不受影响。
 */
export async function markCategoryRead(category) {
  const user = getUser()
  if (!category || !user || !user.id) return
  // 乐观更新：立即清零该分类并同步扣减 total
  const removed = state.byCategory[category] || 0
  if (removed > 0) {
    const byCategory = { ...state.byCategory, [category]: 0 }
    state.byCategory = byCategory
    state.total = Math.max(0, state.total - removed)
  }
  try {
    await postRequest(`/notification/markRead?userId=${user.id}&category=${category}`)
  } catch (e) {
    // 失败时回读服务端真实计数
  }
  await refresh()
}

/** 标记所有通知已读（全部分类清零） */
export async function markAllRead() {
  const user = getUser()
  if (!user || !user.id) return
  try {
    await postRequest(`/notification/markAllRead?userId=${user.id}`)
  } catch (e) { /* 回读校准 */ }
  await refresh()
}

/** 启动全局轮询（幂等；未登录时不启动） */
export function startPolling() {
  if (!loggedIn()) return
  refresh()
  if (!pollTimer) {
    pollTimer = setInterval(refresh, POLL_INTERVAL)
  }
}

/** 停止轮询 */
export function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

/** 登出时清零状态并停轮询 */
export function reset() {
  stopPolling()
  state.total = 0
  state.byCategory = { ...DEFAULT_CATEGORIES }
  state.byType = {}
  state.loaded = false
}

export default {
  state,
  refresh,
  count,
  markCategoryRead,
  markAllRead,
  startPolling,
  stopPolling,
  reset,
}
