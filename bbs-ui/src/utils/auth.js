/**
 * 身份认证工具 — 统一管理 token / user / 记住我凭证的读写。
 *
 * 所有 storage 操作都包裹 try-catch，防止浏览器禁用存储 / 配额超限时崩溃。
 *
 * 「记住我」策略：
 *  - remember=true  → token+user 同时写入 sessionStorage 和 localStorage
 *                     凭证 { username, password } 写入 localStorage
 *  - remember=false → token+user 仅写入 sessionStorage，清除 localStorage 中的旧数据
 *  - 读取时：sessionStorage > localStorage 兜底
 *  - 退出登录：清除 token+user（两个存储），但保留凭证用于登录页自动回填
 */

const TOKEN_KEY = 'tokenStr'
const USER_KEY = 'user'
const REMEMBER_KEY = 'remembered_account'

// ── 安全读写辅助（静默降级，防止存储不可用时全应用崩溃）──

function safeGet(key, storage = window.sessionStorage) {
  try { return storage.getItem(key) } catch (e) { return null }
}

function safeSet(key, value, storage = window.sessionStorage) {
  try { storage.setItem(key, value) } catch (e) { /* 存储不可用，静默忽略 */ }
}

function safeRemove(key, storage = window.sessionStorage) {
  try { storage.removeItem(key) } catch (e) { /* 静默忽略 */ }
}

// ── Token ──

export function getToken() {
  return safeGet(TOKEN_KEY)
    || safeGet(TOKEN_KEY, window.localStorage)
}

export function setToken(token, remember = false) {
  safeSet(TOKEN_KEY, token)
  if (remember) {
    safeSet(TOKEN_KEY, token, window.localStorage)
  } else {
    safeRemove(TOKEN_KEY, window.localStorage)
  }
}

export function removeToken() {
  safeRemove(TOKEN_KEY)
  safeRemove(TOKEN_KEY, window.localStorage)
}

// ── User ──

export function getUser() {
  const raw = safeGet(USER_KEY) || safeGet(USER_KEY, window.localStorage)
  if (!raw) return null
  try { return JSON.parse(raw) } catch (e) { return null }
}

export function setUser(user, remember = false) {
  const raw = JSON.stringify(user)
  safeSet(USER_KEY, raw)
  if (remember) {
    safeSet(USER_KEY, raw, window.localStorage)
  } else {
    safeRemove(USER_KEY, window.localStorage)
  }
}

export function removeUser() {
  safeRemove(USER_KEY)
  safeRemove(USER_KEY, window.localStorage)
}

// ── 记住我凭证（仅 localStorage，仅用于登录页自动回填）──

export function getRememberedAccount() {
  const raw = safeGet(REMEMBER_KEY, window.localStorage)
  if (!raw) return null
  try { return JSON.parse(raw) } catch (e) { return null }
}

export function setRememberedAccount(username, password) {
  safeSet(REMEMBER_KEY, JSON.stringify({ username, password }), window.localStorage)
}

export function removeRememberedAccount() {
  safeRemove(REMEMBER_KEY, window.localStorage)
}
