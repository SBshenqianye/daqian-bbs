/**
 * 身份认证工具 — 统一管理 token / user / 记住我凭证的读写。
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

// ── Token ──

export function getToken() {
  return window.sessionStorage.getItem(TOKEN_KEY)
    || window.localStorage.getItem(TOKEN_KEY)
}

export function setToken(token, remember = false) {
  window.sessionStorage.setItem(TOKEN_KEY, token)
  if (remember) {
    window.localStorage.setItem(TOKEN_KEY, token)
  } else {
    window.localStorage.removeItem(TOKEN_KEY)
  }
}

export function removeToken() {
  window.sessionStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(TOKEN_KEY)
}

// ── User ──

export function getUser() {
  const raw = window.sessionStorage.getItem(USER_KEY)
    || window.localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export function setUser(user, remember = false) {
  const raw = JSON.stringify(user)
  window.sessionStorage.setItem(USER_KEY, raw)
  if (remember) {
    window.localStorage.setItem(USER_KEY, raw)
  } else {
    window.localStorage.removeItem(USER_KEY)
  }
}

export function removeUser() {
  window.sessionStorage.removeItem(USER_KEY)
  window.localStorage.removeItem(USER_KEY)
}

// ── 记住我凭证（仅 localStorage，仅用于登录页自动回填）──

export function getRememberedAccount() {
  const raw = window.localStorage.getItem(REMEMBER_KEY)
  return raw ? JSON.parse(raw) : null
}

export function setRememberedAccount(username, password) {
  window.localStorage.setItem(REMEMBER_KEY, JSON.stringify({ username, password }))
}

export function removeRememberedAccount() {
  window.localStorage.removeItem(REMEMBER_KEY)
}
