/**
 * 管理员状态全局 Store（单例）
 *
 * 架构说明：
 * - 基于 Vue.observable 实现响应式状态管理
 * - 统一管理管理员登录状态、管理员信息
 * - 所有组件通过 import 使用，无需 EventBus 或 Props 传递
 *
 * 使用示例：
 * <script>
 * import adminStore from '@/utils/adminStore'
 *
 * export default {
 *   computed: {
 *     isLoggedIn() { return adminStore.isLoggedIn },
 *     currentAdmin() { return adminStore.admin },
 *     isAdminId1() { return adminStore.isAdminId1 },
 *   }
 * }
 * </script>
 */
import Vue from 'vue'

/** 响应式状态 */
const state = Vue.observable({
  /** 当前管理员对象（null 表示未登录） */
  admin: null,
  /** 是否已加载（避免重复初始化） */
  loaded: false,
})

const ADMIN_KEY = 'admin'
const TOKEN_KEY = 'tokenStr'

// ── 安全读写辅助 ──

function safeGet(key) {
  try { return sessionStorage.getItem(key) } catch (e) { return null }
}

function safeSet(key, value) {
  try { sessionStorage.setItem(key, value) } catch (e) { /* 静默忽略 */ }
}

function safeRemove(key) {
  try { sessionStorage.removeItem(key) } catch (e) { /* 静默忽略 */ }
}

/**
 * 初始化管理员状态（应用启动时调用一次）
 */
export function init() {
  if (state.loaded) return
  
  const raw = safeGet(ADMIN_KEY)
  if (raw) {
    try {
      state.admin = JSON.parse(raw)
    } catch (e) {
      state.admin = null
    }
  }
  state.loaded = true
}

/**
 * 登录成功后设置管理员状态
 * @param {Object} admin - 管理员对象
 * @param {string} token - JWT token
 */
export function login(admin, token) {
  if (!admin) return
  
  safeSet(ADMIN_KEY, JSON.stringify(admin))
  if (token) {
    safeSet(TOKEN_KEY, token)
  }
  state.admin = admin
}

/**
 * 登出时清除管理员状态
 */
export function logout() {
  safeRemove(ADMIN_KEY)
  safeRemove(TOKEN_KEY)
  state.admin = null
}

/**
 * 更新管理员信息（部分更新）
 * @param {Object} partial - 要更新的字段
 */
export function updateAdmin(partial) {
  if (!state.admin) return
  
  const updated = { ...state.admin, ...partial }
  state.admin = updated
  safeSet(ADMIN_KEY, JSON.stringify(updated))
}

// ── 计算属性（只读） ──

/** 是否已登录 */
export function isLoggedIn() {
  return !!(state.admin && state.admin.id)
}

/** 是否是超级管理员（id=1） */
export function isAdminId1() {
  return !!(state.admin && (state.admin.id === 1 || state.admin.id === '1'))
}

/** 获取管理员ID */
export function getAdminId() {
  return state.admin ? state.admin.id : null
}

/** 获取管理员用户名 */
export function getUsername() {
  return state.admin ? (state.admin.username || '') : ''
}

/** 获取Token */
export function getToken() {
  return safeGet(TOKEN_KEY)
}

export default {
  state,
  init,
  login,
  logout,
  updateAdmin,
  isLoggedIn,
  isAdminId1,
  getAdminId,
  getUsername,
  getToken,
}
