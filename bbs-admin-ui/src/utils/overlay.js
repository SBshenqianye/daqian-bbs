/**
 * 弹层 z-index 统一分配。
 *
 * 复用 Element UI 的 PopupManager 计数器，使自定义弹窗与 el-dialog / MessageBox
 * 处在同一序列中：后打开的永远在上面，避免「新弹窗被旧弹窗盖住」。
 */
let popupManager = null
try {
  // eslint-disable-next-line global-require
  popupManager = require('element-ui/lib/utils/popup').PopupManager
} catch (e) {
  popupManager = null
}

let fallback = 3000

export function nextOverlayZ() {
  if (popupManager && typeof popupManager.nextZIndex === 'function') {
    const z = popupManager.nextZIndex()
    if (z > fallback) fallback = z
    return z
  }
  fallback += 1
  return fallback
}

export default { nextOverlayZ }
