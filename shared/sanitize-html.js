/**
 * 正文 HTML 白名单过滤（零依赖，仅浏览器端）
 *
 * 背景：帖子正文按 Markdown 渲染后通过 `v-html` 上屏，历史数据里也可能存过裸 HTML。
 * 渲染器（bbs-ui 的 mdToHtml / 管理端的 markdown-it）都放行原文里的标签，
 * 于是 `<script>`、`<img onerror=...>`、`<a href="javascript:...">` 会原样进 DOM。
 *
 * 策略：
 *  - 白名单保留排版标签（段落/标题/列表/表格/图片/链接/代码/引用/分割线等）；
 *  - `<script>` / `<iframe>` / `<object>` / `<svg>` 等可执行或可嵌套的标签整块删除；
 *  - 其余未知标签“拆壳”保留文字（不让内容凭空消失，也不让标签生效）；
 *  - 删除所有 `on*` 事件属性；URL 只放行 http(s)/mailto/tel、站内相对路径与锚点，
 *    图片额外放行 data:image/*;base64；
 *  - `style` 保留（老帖有行内排版），但含 expression/javascript:/behavior 等一律删除；
 *  - `target="_blank"` 的链接强制补 `rel="noopener noreferrer"`。
 *
 * 非浏览器环境（没有 DOMParser）退化为纯文本转义，宁可显示原始标签也不放行。
 */
const ALLOWED_TAGS = new Set([
  'a', 'p', 'br', 'span', 'div', 'strong', 'b', 'em', 'i', 'u', 's', 'del', 'ins', 'mark',
  'small', 'sub', 'sup', 'code', 'pre', 'blockquote', 'q', 'cite',
  'ul', 'ol', 'li', 'dl', 'dt', 'dd', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'hr', 'img', 'figure', 'figcaption',
  'table', 'thead', 'tbody', 'tfoot', 'tr', 'th', 'td', 'caption', 'colgroup', 'col',
])

/** 整块删除（含内容）：可执行、可加载外部资源、或解析行为与常规 HTML 不同的标签 */
const DROP_WITH_CONTENT = new Set([
  'script', 'style', 'iframe', 'frame', 'frameset', 'object', 'embed', 'applet',
  'form', 'input', 'button', 'select', 'option', 'textarea', 'label', 'link', 'meta', 'base',
  'template', 'noscript', 'svg', 'math', 'video', 'audio', 'source', 'track', 'canvas',
])

/** 所有标签都允许的属性（style 另做内容检查） */
const GLOBAL_ATTRS = ['class', 'style', 'title', 'align']

/** 按标签放行的属性 */
const TAG_ATTRS = {
  a: ['href', 'target', 'rel', 'name'],
  img: ['src', 'alt', 'width', 'height'],
  ol: ['start', 'type', 'reversed'],
  li: ['value'],
  th: ['colspan', 'rowspan', 'scope'],
  td: ['colspan', 'rowspan'],
  col: ['span', 'width'],
  colgroup: ['span'],
}

/** 只接受数字（可带 % 或 px）的属性 */
const NUMERIC_ATTRS = new Set(['width', 'height', 'colspan', 'rowspan', 'span', 'start', 'value'])

const UNSAFE_STYLE = /expression\s*\(|javascript\s*:|vbscript\s*:|behavior\s*:|-moz-binding|@import/i
const DATA_IMAGE = /^data:image\/(png|jpe?g|gif|webp|bmp);base64,[a-z0-9+/=]+$/i

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

/**
 * 过滤单个 URL：去掉控制字符后再判断协议，避免 `java\nscript:` 这类绕过。
 * 不合法返回空串，由调用方删除该属性。
 */
function sanitizeUrl(raw, allowDataImage) {
  const url = String(raw == null ? '' : raw).trim()
  if (!url) return ''
  const probe = url.replace(/[\u0000-\u0020]+/g, '')
  if (allowDataImage && DATA_IMAGE.test(probe)) return url
  if (/^(https?:|mailto:|tel:)/i.test(probe)) return url
  if (probe.startsWith('//')) return url          // 协议相对地址
  if (/^(#|\/(?!\/)|\.{1,2}\/)/.test(probe)) return url // 锚点、站内绝对/相对路径
  return ''
}

function unwrap(el) {
  const parent = el.parentNode
  if (!parent) return
  while (el.firstChild) parent.insertBefore(el.firstChild, el)
  parent.removeChild(el)
}

function cleanAttributes(el, tag) {
  const allowed = TAG_ATTRS[tag] || []
  for (const attr of Array.from(el.attributes)) {
    const name = attr.name.toLowerCase()
    const value = attr.value == null ? '' : String(attr.value)

    if (name.startsWith('on')) { el.removeAttribute(attr.name); continue }
    if (name === 'style' && UNSAFE_STYLE.test(value)) { el.removeAttribute(attr.name); continue }
    if (name === 'href' || name === 'src') {
      const safe = sanitizeUrl(value, tag === 'img')
      if (safe) el.setAttribute(name, safe)
      else el.removeAttribute(attr.name)
      continue
    }
    if (NUMERIC_ATTRS.has(name) && !/^\d+(\.\d+)?(%|px)?$/.test(value.trim())) {
      el.removeAttribute(attr.name)
      continue
    }
    if (GLOBAL_ATTRS.indexOf(name) !== -1 || allowed.indexOf(name) !== -1) continue
    el.removeAttribute(attr.name)
  }
  if (tag === 'a' && el.getAttribute('target') === '_blank') {
    el.setAttribute('rel', 'noopener noreferrer')
  }
}

function cleanTree(node) {
  if (!node || node.nodeType !== 1) return
  const tag = node.tagName.toLowerCase()
  if (DROP_WITH_CONTENT.has(tag)) { node.remove(); return }
  // 先清理子节点，再决定当前节点拆壳还是保留
  for (const child of Array.from(node.children)) cleanTree(child)
  if (!ALLOWED_TAGS.has(tag)) { unwrap(node); return }
  cleanAttributes(node, tag)
}

export function sanitizeHtml(html) {
  if (!html) return ''
  const str = String(html)
  if (typeof window === 'undefined' || typeof window.DOMParser === 'undefined') {
    return escapeHtml(str)
  }
  const doc = new window.DOMParser().parseFromString('<!doctype html><body>' + str, 'text/html')
  const root = doc.body
  for (const child of Array.from(root.children)) cleanTree(child)
  return root.innerHTML
}
