/**
 * 大千 BBS 自定义 Markdown ↔ HTML 转换
 *
 * 存储格式（极简类 Markdown）：
 *   - 每行一段文字，空行表示段落间的空行
 *   - 图片：![图片](url)
 *   - 链接：[text](url)
 *
 * htmlToMd 将 contenteditable 的 innerHTML 转为此格式。
 * mdToHtml 将格式转回 HTML。
 */

/**
 * 将自定义 Markdown 渲染为 HTML
 *
 * 每段文字用 <p> 包裹。
 * 空行（连续的 \n）渲染为 <p><br></p>（可见空行）。
 * 规则：几个连续空行就输出几个 <p><br></p>
 */
export function mdToHtml(md) {
  if (!md) return ''
  const lines = md.split('\n')
  const result = []
  let blanks = 0

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) {
      blanks++
      continue
    }

    // 先输出空行
    while (blanks > 0) {
      result.push('<p><br></p>')
      blanks--
    }

    // 处理段内内容
    const processed = line
      .replace(/!\[(.*?)\]\((.*?)\)/g, (m, alt, src) =>
        `<img src="${src}" alt="${alt}" style="max-width:100%;display:block;margin:4px 0">`)
      .replace(/\[([^\]]*)\]\((.*?)\)/g, '<a href="$2">$1</a>')

    result.push(`<p>${processed}</p>`)
  }

  return result.join('\n')
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

  // 4. 逐个处理 <p> 元素
  const lines = []
  const pRegex = /<p[^>]*>([\s\S]*?)<\/p>/gi
  let match
  while ((match = pRegex.exec(md)) !== null) {
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
  }

  // 5. 组装
  md = lines.join('\n')

  // 6. 清理
  md = md.replace(/&nbsp;/g, ' ')
  md = md.replace(/<[^>]*>/g, '')     // 移除漏网的 HTML 标签
  md = md.replace(/^\n+|\n+$/g, '')   // 去首尾空行

  return md
}
