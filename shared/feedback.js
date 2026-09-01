/**
 * 统一反馈工具模块
 * 解决 postRequest 双重 Toast 问题，提供标准化的操作反馈
 */
import { Message } from 'element-ui'

// ========== 消息合并队列 ==========

const messageQueue = []
let mergeTimer = null
const MERGE_WINDOW = 300 // ms，合并窗口

/**
 * 将消息入队，等待合并窗口结束后统一显示
 */
function enqueueMessage(msg) {
  messageQueue.push(msg)
  if (!mergeTimer) {
    mergeTimer = setTimeout(flushMessages, MERGE_WINDOW)
  }
}

/**
 * 刷新消息队列：单条直接显示，多条合并显示
 */
function flushMessages() {
  mergeTimer = null
  if (messageQueue.length === 0) return

  if (messageQueue.length === 1) {
    // 单条消息：直接显示
    const msg = messageQueue[0]
    Message(msg)
  } else {
    // 多条消息：合并显示
    const texts = messageQueue.map(m => m.message).filter(Boolean)
    const hasError = messageQueue.some(m => m.type === 'error')
    const hasWarning = messageQueue.some(m => m.type === 'warning')
    const type = hasError ? 'error' : hasWarning ? 'warning' : 'success'

    Message({
      type,
      message: `共 ${messageQueue.length} 项操作：${texts.join('；')}`,
      showClose: true,
      duration: 5000,
      offset: 54
    })
  }
  messageQueue.length = 0
}

/**
 * 去重显示消息（相同内容不重复弹出）
 */
function showUniqueMessage(msg) {
  // 检查队列中是否已有相同内容
  const isDuplicate = messageQueue.some(
    existing => existing.type === msg.type && existing.message === msg.message
  )
  if (!isDuplicate) {
    enqueueMessage(msg)
  }
}

// ========== 标准化反馈方法 ==========

/**
 * 标准化操作反馈 — 非导航操作使用
 * 弹出且仅弹出一个 Toast
 *
 * @param {Object} resp - postRequest 返回值（可能为 undefined/null）
 * @param {Object} [options]
 * @param {string} [options.successMsg] - 成功消息（不传则用后端 message）
 * @param {string} [options.errorMsg] - 失败消息（不传则用后端 message）
 * @param {Function} [options.onSuccess] - 成功回调（如刷新列表）
 * @param {Function} [options.onError] - 失败回调
 * @param {boolean} [options.silent] - 是否静默（不弹任何提示，仅执行回调）
 * @returns {boolean} 是否成功
 */
export function handleResponse(resp, options = {}) {
  const {
    successMsg,
    errorMsg,
    onSuccess,
    onError,
    silent = false
  } = options

  // 请求被拦截器拒绝（返回 undefined）
  if (!resp) {
    if (!silent && errorMsg) {
      showUniqueMessage({ type: 'error', message: errorMsg, showClose: true, offset: 54 })
    }
    if (onError) onError()
    return false
  }

  // 业务成功
  if (resp.code == 200) {
    if (!silent && successMsg) {
      // 只有显式传了 successMsg 才弹成功提示，不自动使用后端 message
      // 避免列表加载等操作弹出无意义的成功 Toast
      showUniqueMessage({ type: 'success', message: successMsg, showClose: true, offset: 54 })
    }
    if (onSuccess) onSuccess(resp)
    return true
  }

  // 业务失败（code != 200 但请求成功，如 400/500 业务码）
  if (!silent) {
    const msg = errorMsg || resp.message || '操作失败'
    showUniqueMessage({ type: resp.code == 500 ? 'warning' : 'error', message: msg, showClose: true, offset: 54 })
  }
  if (onError) onError(resp)
  return false
}

/**
 * 批量操作反馈 — 多个请求结果合并为一个 Toast
 *
 * @param {Promise[]} promises - 请求数组
 * @param {Object} [options]
 * @param {string} [options.successMsg] - 全部成功时的消息
 * @param {string} [options.partialMsg] - 部分成功时的消息模板（用 {success}/{fail} 占位）
 * @param {Function} [options.onSuccess] - 全部成功回调
 * @param {Function} [options.onPartial] - 部分成功回调
 * @param {Function} [options.onError] - 全部失败回调
 * @param {boolean} [options.silent] - 是否静默
 */
export async function handleBatchResponse(promises, options = {}) {
  const {
    successMsg,
    partialMsg,
    onSuccess,
    onPartial,
    onError,
    silent = false
  } = options

  const results = await Promise.allSettled(promises)
  const succeeded = results.filter(r => r.status === 'fulfilled' && r.value && r.value.code == 200)
  const failed = results.length - succeeded.length

  if (!silent) {
    if (failed === 0) {
      // 全部成功
      const msg = successMsg || `全部操作成功（共 ${succeeded.length} 项）`
      showUniqueMessage({ type: 'success', message: msg, showClose: true, offset: 54 })
    } else if (succeeded.length === 0) {
      // 全部失败
      showUniqueMessage({ type: 'error', message: `全部操作失败（共 ${failed} 项）`, showClose: true, offset: 54 })
    } else {
      // 部分成功
      const msg = partialMsg
        ? partialMsg.replace('{success}', succeeded.length).replace('{fail}', failed)
        : `操作完成：${succeeded.length} 项成功，${failed} 项失败`
      showUniqueMessage({ type: 'warning', message: msg, showClose: true, offset: 54 })
    }
  }

  if (succeeded.length === results.length && onSuccess) {
    onSuccess(succeeded.map(r => r.value))
  } else if (succeeded.length > 0 && onPartial) {
    onPartial(succeeded.map(r => r.value), failed)
  } else if (succeeded.length === 0 && onError) {
    onError()
  }

  return { succeeded, failed, total: results.length }
}

/**
 * 导航操作反馈 — 成功时静默跳转，失败时弹错误提示
 *
 * @param {Object} resp - postRequest 返回值
 * @param {Function} navigate - 导航函数（如 () => this.$router.push('/forum')）
 * @param {Object} [options]
 * @param {string} [options.errorMsg] - 失败时的消息
 * @param {Function} [options.onError] - 失败回调
 * @returns {boolean} 是否成功并执行了导航
 */
export function handleNavigation(resp, navigate, options = {}) {
  const { errorMsg, onError } = options

  if (!resp) {
    if (errorMsg) {
      showUniqueMessage({ type: 'error', message: errorMsg, showClose: true, offset: 54 })
    }
    if (onError) onError()
    return false
  }

  if (resp.code == 200) {
    // 成功：不弹 Toast，直接导航
    navigate()
    return true
  }

  // 失败：弹错误提示
  const msg = errorMsg || resp.message || '操作失败'
  showUniqueMessage({ type: 'error', message: msg, showClose: true, offset: 54 })
  if (onError) onError(resp)
  return false
}

/**
 * 手动显示成功消息（适用于非 postRequest 场景，如表单校验提示等）
 */
export function showSuccess(msg) {
  showUniqueMessage({ type: 'success', message: msg, showClose: true, offset: 54 })
}

/**
 * 手动显示错误消息
 */
export function showError(msg) {
  showUniqueMessage({ type: 'error', message: msg, showClose: true, offset: 54 })
}

/**
 * 手动显示警告消息
 */
export function showWarning(msg) {
  showUniqueMessage({ type: 'warning', message: msg, showClose: true, offset: 54 })
}
