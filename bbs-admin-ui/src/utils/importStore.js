import Vue from 'vue'

/**
 * 全局导入状态 — Vue.observable 使其跨组件、跨路由响应。
 *
 * 所有 UserPage 和 ImportOverlay 共享同一份状态：
 *   UserPage 触发预览/导入 → 写入状态
 *   ImportOverlay 读取状态 → 渲染 UI
 *   App.vue 挂载 ImportOverlay，路由切换不影响其存活
 */
export const importStore = Vue.observable({
  /** idle | previewing | previewDone | importing | done | error */
  status: 'idle',

  // ——— 预览阶段 ———
  /** 预览是否正在加载 */
  previewLoading: false,
  /** 预览数据（由 UserPage 写入，ImportOverlay 可不关心具体内容） */
  previewData: null,

  // ——— 导入阶段 ———
  /** 后端返回的 taskId */
  taskId: null,
  /** 已处理行数 */
  progress: 0,
  /** 总行数 */
  total: 0,
  /** 导入结果 */
  result: null,
  /** 错误消息 */
  error: null,

  // ——— 预览中标记缺失键的行 ——
  rowsWithMissingKeys: [],
})

/** 重置到初始状态 */
export function resetImport() {
  importStore.status = 'idle'
  importStore.previewLoading = false
  importStore.previewData = null
  importStore.taskId = null
  importStore.progress = 0
  importStore.total = 0
  importStore.result = null
  importStore.error = null
  importStore.rowsWithMissingKeys = []
}
