/**
 * 大千 BBS Markdown ↔ HTML 转换
 *
 * mdToHtml 使用 marked 库渲染完整的 Markdown 语法（标题、粗体、斜体、列表、代码块、表格等）。
 * htmlToMd 将 contenteditable 的 innerHTML 转为极简 Markdown 格式（供编辑器使用）。
 */

import { marked } from 'marked'

// 配置 marked：启用换行符转换（\n → <br>），支持 GFM
marked.setOptions({
  breaks: true,
  gfm: true,
})

/**
 * 将 Markdown 渲染为 HTML
 * 支持完整 Markdown 语法：标题、粗体、斜体、列表、代码块、表格、引用、图片、链接等
 */
export function mdToHtml(md) {
  if (!md) return ''
  return marked.parse(md)
}

/**
 * 将 contenteditable 的 innerHTML 转为自定义 Markdown
 *
 * 每个 <p> 元素输出一行内容，<p><br></p> 输出空行。
 */
export function htmlToMd(html) {
  if (!html) return ''

  // 1. 去掉标签间的空白
  let md = html.replace(/>\s+</g, '><')

  // 2. HTML 图片/链接 → markdown 语法
  md = md.replace(/<img[^>]+src="([^"]+)"[^>]*>/gi, (m, s) => `![图片](${s})`)
  md = md.replace(/<a[^>]+href="([^"]+)"[^>]*>([^<]*)<\/a>/gi, (m, h, t) => `[${t}](${h})`)

  // 3. <br> → 换行（段内换行，多行段落用）
  md = md.replace(/<br\s*\/?>/gi, '\n')

  // 4. 统一块级标签：contenteditable 浏览器常用 <div> 包裹，统一转 <p> 处理
  md = md.replace(/<div>/gi, '<p>').replace(/<\/div>/gi, '</p>')

  // 5. 逐个处理 <p> 元素及其间的游离内容（如图片插入空编辑区时是根的直接子元素，不在 <p> 内）
  const lines = []
  const pRegex = /<p[^>]*>([\s\S]*?)<\/p>/gi
  let lastIndex = 0
  let match
  while ((match = pRegex.exec(md)) !== null) {
    // <p> 之间的游离内容：图片已在步骤2转成 ![图片](url)，<br> 已转成 \n
    const between = md.slice(lastIndex, match.index)
    if (between.trim()) {
      const text = between.replace(/<[^>]*>/g, '').trim()
      if (text) {
        // 游离图片是块级内容，独立成行（避免 ![图片](url)hello 粘连）
        text.replace(/!\[[^\]]*\]\([^)]*\)/g, m => '\n' + m + '\n')
            .split('\n').forEach(seg => { if (seg.trim()) lines.push(seg.trim()) })
      }
    }
    const content = match[1].trim()
    if (!content) {
      lines.push('')  // 空行
    } else {
      // 检查是否纯 <br> 内容（空段落变体）
      const stripped = content.replace(/<br\s*\/?>/gi, '').trim()
      if (!stripped) {
        lines.push('')  // <p><br></p> → 空行
      } else {
        lines.push(content)
      }
    }
    lastIndex = pRegex.lastIndex
  }
  // 尾部游离内容（无 <p> 或 <p> 之外的内容，如图片-only 的整段）
  if (lastIndex < md.length) {
    const text = md.slice(lastIndex).replace(/<[^>]*>/g, '').trim()
    if (text) {
      // 游离图片是块级内容，独立成行（避免 ![图片](url)hello 粘连）
      text.replace(/!\[[^\]]*\]\([^)]*\)/g, m => '\n' + m + '\n')
          .split('\n').forEach(seg => { if (seg.trim()) lines.push(seg.trim()) })
    }
  }

  // 6. 组装
  md = lines.join('\n')

  // 7. 清理
  md = md.replace(/&nbsp;/g, ' ')
  md = md.replace(/<[^>]*>/g, '')     // 移除漏网的 HTML 标签
  md = md.replace(/^\n+|\n+$/g, '')   // 去首尾空行

  return md
}
