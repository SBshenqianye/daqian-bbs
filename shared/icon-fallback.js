/**
 * 图标兜底：素材/配置里的 SVG 被清空、损坏或格式不对时，统一渲染一枚「图裂」占位。
 *
 * 背景：等级与勋章保存的是 SVG 快照，正常情况下不会缺失；
 * 但历史数据、手工改库、导入失败都可能留下空串或非 SVG 文本，
 * 直接 v-html 会得到一块空白，看起来像「图标丢了」却毫无提示。
 */

/** 是否是可用作 v-html 的 SVG 片段 */
export function isSvgMarkup(svg) {
  return typeof svg === 'string' && /<svg[\s>]/i.test(svg)
}

/** 图裂占位（24×24，灰阶虚线框 + 破损图像） */
export function fallbackIconSvg() {
  return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" aria-label="图标缺失">'
    + '<rect x="2.2" y="4.2" width="19.6" height="15.6" rx="3" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="1.2" stroke-dasharray="3 2.2"/>'
    + '<path d="M4.6 16.4l4.2-4.2 3 3 2.2-2.2 5.4 5.4" fill="none" stroke="#cbd5e1" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>'
    + '<circle cx="9" cy="8.6" r="1.5" fill="#cbd5e1"/>'
    + '<path d="M4 4l16 16" fill="none" stroke="#94a3b8" stroke-width="1.6" stroke-linecap="round"/>'
    + '</svg>'
}

/** 有合法 SVG 就返回原样，否则返回图裂占位 */
export function safeIconSvg(svg) {
  return isSvgMarkup(svg) ? svg : fallbackIconSvg()
}
