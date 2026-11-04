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

/** 管理员默认权限点（旧后端未返回 permissions 时的兼容回退；后端仍是最终边界） */
const ADMIN_DEFAULT_PERMISSIONS = [
  'admin:access',
  'user:read', 'user:create', 'user:update', 'user:delete',
  'config:read',
  'article:read', 'article:review', 'article:delete', 'article:feature', 'article:suggestion:adopt',
  'report:review', 'appeal:review',
  'moderator:manage', 'moderator:complaint:review', 'adopt:review',
  'featured:recommend', 'featured:review',
  'points:read', 'points:adjust',
  'dashboard:read',
]

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

/** 是否需要强制修改初始密码（管理端 C3） */
export function needsPasswordChange() {
  if (!state.admin) return false
  const flag = state.admin.isFirstLogin
  return flag === 1 || flag === '1'
}

/** 兼容旧调用的身份名称：由权限点与数据范围推导，仅用于文案/分支。 */
export function getRole() {
  if (!state.admin) return null
  if (isSuperAdmin()) return 'SUPER_ADMIN'
  if (isModerator()) return 'MODERATOR'
  if (hasPermission('admin:access')) return 'ADMIN'
  return 'USER'
}

/** 是否是超级管理员：由权限组授予 user:grant:admin 决定。 */
export function isSuperAdmin() {
  return hasPermission('user:grant:admin')
}

/** 是否是全局管理员：能进管理后台且数据范围为全局。 */
export function isGlobalAdmin() {
  return hasPermission('admin:access') && !isModerator()
}

/** 是否是版主账号：主权限组的数据范围为“按版主任职”。 */
export function isModerator() {
  if (!state.admin) return false
  if (state.admin.dataScopeType) return state.admin.dataScopeType === 'BOARD_MODERATOR'
  return state.admin.role === 'MODERATOR'
}

/** 兼容旧调用名：isAdminId1 现在语义为“是否超级管理员” */
export function isAdminId1() {
  return isSuperAdmin()
}

/** 当前账号权限点 */
export function getPermissions() {
  if (state.admin && Array.isArray(state.admin.permissions)) {
    return state.admin.permissions
  }
  // 兼容旧会话/旧后端：管理员按默认权限点渲染，具体操作仍由后端校验
  const role = state.admin && state.admin.role
  return role === 'ADMIN' || role === 'SUPER_ADMIN' ? ADMIN_DEFAULT_PERMISSIONS : []
}

/** 是否拥有权限点；未声明权限点的菜单/按钮默认放行 */
export function hasPermission(permission) {
  if (!permission) return true
  return getPermissions().indexOf(permission) !== -1
}

/** 是否拥有任意一个权限点 */
export function hasAnyPermission(permissions) {
  if (!Array.isArray(permissions) || permissions.length === 0) return true
  return permissions.some(p => hasPermission(p))
}

/** 当前账号版主板块范围 */
export function getBoardScopes() {
  if (!state.admin || !Array.isArray(state.admin.boardScopes)) return []
  return state.admin.boardScopes
}

/** 是否已分配板块（非版主账号默认 true；版主看 boardScopes） */
export function hasBoards() {
  if (!isModerator()) return true
  return getBoardScopes().length > 0
}

/** 是否是指定板块的版主 */
export function isBoardModerator(labelId) {
  return getBoardScopes().some(s => String(s.labelId) === String(labelId))
}

/** 获取管理员ID */
export function getAdminId() {
  return state.admin ? state.admin.id : null
}

/** 获取当前账号主权限组ID */
export function getPrimaryGroupId() {
  return state.admin ? (state.admin.primaryGroupId == null ? null : state.admin.primaryGroupId) : null
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
  needsPasswordChange,
  getRole,
  isSuperAdmin,
  isGlobalAdmin,
  isModerator,
  isAdminId1,
  getPermissions,
  hasPermission,
  hasAnyPermission,
  getBoardScopes,
  hasBoards,
  isBoardModerator,
  getAdminId,
  getPrimaryGroupId,
  getUsername,
  getToken,
}
