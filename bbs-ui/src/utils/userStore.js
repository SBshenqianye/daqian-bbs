/**
 * 用户状态全局 Store（单例）
 *
 * 架构说明：
 * - 基于 Vue.observable 实现响应式状态管理
 * - 统一管理用户登录状态、用户信息、权限等
 * - 所有组件通过 import 使用，无需 EventBus 或 Props 传递
 * - 与 auth.js 配合：auth.js 负责存储操作，userStore 负责响应式状态
 *
 * 使用示例：
 * <script>
 * import userStore from '@/utils/userStore'
 *
 * export default {
 *   computed: {
 *     isLoggedIn() { return userStore.isLoggedIn },
 *     currentUser() { return userStore.user },
 *     isAdmin() { return userStore.isAdmin },
 *   }
 * }
 * </script>
 */
import Vue from 'vue'
import { getUser, setUser, removeUser, getToken, setToken, removeToken } from './auth'

/** 响应式状态 */
const state = Vue.observable({
  /** 当前用户对象（null 表示未登录） */
  user: null,
  /** 是否已加载（避免重复初始化） */
  loaded: false,
})

/** 缓存的用户ID（用于检测变化） */
let cachedUserId = null

/**
 * 初始化用户状态（应用启动时调用一次）
 * 从 sessionStorage/localStorage 读取用户信息
 */
export function init() {
  if (state.loaded) return
  
  const user = getUser()
  if (user && user.id) {
    state.user = user
    cachedUserId = user.id
  }
  state.loaded = true
}

/**
 * 登录成功后设置用户状态
 * @param {Object} user - 用户对象
 * @param {boolean} remember - 是否记住我
 */
export function login(user, remember = false) {
  if (!user || !user.id) return
  
  setUser(user, remember)
  state.user = user
  cachedUserId = user.id
}

/**
 * 登出时清除用户状态
 */
export function logout() {
  removeToken()
  removeUser()
  state.user = null
  cachedUserId = null
}

/**
 * 更新用户信息（部分更新）
 * @param {Object} partial - 要更新的字段
 */
export function updateUser(partial) {
  if (!state.user) return
  
  const updated = { ...state.user, ...partial }
  state.user = updated
  
  // 同步到存储
  const remember = !!getToken()
  setUser(updated, remember)
}

/**
 * 刷新用户状态（从存储重新读取）
 * 用于其他标签页登录/登出后同步状态
 */
export function refresh() {
  const user = getUser()
  if (user && user.id) {
    state.user = user
    cachedUserId = user.id
  } else {
    state.user = null
    cachedUserId = null
  }
}

// ── 计算属性（只读） ──

/** 是否已登录 */
export function isLoggedIn() {
  return !!(state.user && state.user.id)
}

/** 是否是超级管理员（id=1） */
export function isAdmin() {
  return !!(state.user && (state.user.id === 1 || state.user.id === '1'))
}

/** 是否是版主 */
export function isModerator() {
  if (!state.user) return false
  const type = (state.user.userType || '').toLowerCase()
  return type === 'moderator' || type === 'admin'
}

/** 获取用户ID */
export function getUserId() {
  return state.user ? state.user.id : null
}

/** 获取用户昵称 */
export function getNickname() {
  return state.user ? (state.user.nickname || state.user.username || '') : ''
}

// ── 监听存储变化（跨标签页同步） ──
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (e) => {
    if (e.key === 'user') {
      refresh()
    }
  })
}

export default {
  state,
  init,
  login,
  logout,
  updateUser,
  refresh,
  isLoggedIn,
  isAdmin,
  isModerator,
  getUserId,
  getNickname,
}
