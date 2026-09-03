/**
 * 大千 BBS Markdown ↔ HTML 转换
 *
 * mdToHtml 轻量 Markdown 渲染器（零外部依赖）。
 * htmlToMd 将 contenteditable 的 innerHTML 转为极简 Markdown 格式（供编辑器使用）。
 */

/**
 * 转义 HTML 特殊字符（防 XSS）
 */
function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/**
 * 行内 Markdown 语法：粗体、斜体、行内代码、图片、链接
 */
function renderInline(text) {
  return text
    // 行内代码（优先处理，内部不渲染其他语法）
    .replace(/`([^`]+)`/g, (_, code) => `<code>${escapeHtml(code)}</code>`)
    // 图片
    .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" style="max-width:100%;display:block;margin:4px 0">')
    // 链接
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
    // 粗体 **text** 或 __text__
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    // 斜体 *text* 或 _text_
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/_(.+?)_/g, '<em>$1</em>')
    // 删除线 ~~text~~
    .replace(/~~(.+?)~~/g, '<del>$1</del>')
}

/**
 * 将 Markdown 渲染为 HTML
 * 支持：标题(h1-h6)、粗体、斜体、删除线、行内代码、代码块、引用块、有序/无序列表、图片、链接、表格、水平线
 */
export function mdToHtml(md) {
  if (!md) return ''

  const lines = md.split('\n')
  const result = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i]

    // 空行
    if (line.trim() === '') {
      i++
      continue
    }

    // 代码块 ```
    if (line.trim().startsWith('```')) {
      const codeLines = []
      i++ // 跳过 opening ```
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        codeLines.push(escapeHtml(lines[i]))
        i++
      }
      if (i < lines.length) i++ // 跳过 closing ```
      result.push('<pre><code>' + codeLines.join('\n') + '</code></pre>')
      continue
    }

    // 标题 # ~ ######
    const headingMatch = line.match(/^(#{1,6})\s+(.+)$/)
    if (headingMatch) {
      const level = headingMatch[1].length
      result.push(`<h${level}>${renderInline(headingMatch[2])}</h${level}>`)
      i++
      continue
    }

    // 水平线 --- 或 *** 或 ___
    if (/^[-*_]{3,}\s*$/.test(line.trim())) {
      result.push('<hr>')
      i++
      continue
    }

    // 引用块 >
    if (line.trim().startsWith('>')) {
      const quoteLines = []
      while (i < lines.length && lines[i].trim().startsWith('>')) {
        quoteLines.push(lines[i].trim().replace(/^>\s?/, ''))
        i++
      }
      result.push('<blockquote>' + mdToHtml(quoteLines.join('\n')) + '</blockquote>')
      continue
    }

    // 无序列表 - 或 * 或 +
    if (/^\s*[-*+]\s+/.test(line)) {
      const items = []
      while (i < lines.length && /^\s*[-*+]\s+/.test(lines[i])) {
        items.push(lines[i].replace(/^\s*[-*+]\s+/, ''))
        i++
      }
      result.push('<ul>' + items.map(item => `<li>${renderInline(item)}</li>`).join('') + '</ul>')
      continue
    }

    // 有序列表 1. 2. 3.
    if (/^\s*\d+\.\s+/.test(line)) {
      const items = []
      while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
        items.push(lines[i].replace(/^\s*\d+\.\s+/, ''))
        i++
      }
      result.push('<ol>' + items.map(item => `<li>${renderInline(item)}</li>`).join('') + '</ol>')
      continue
    }

    // 表格 | col1 | col2 |
    if (line.trim().startsWith('|')) {
      const tableLines = []
      while (i < lines.length && lines[i].trim().startsWith('|')) {
        tableLines.push(lines[i].trim())
        i++
      }
      // 解析表头
      const headerCells = tableLines[0].split('|').filter(c => c.trim() !== '').map(c => c.trim())
      // 跳过分隔行 |---|---|
      const startIdx = tableLines.length > 1 && /^[\s|:-]+$/.test(tableLines[1]) ? 2 : 1
      // 解析数据行
      const bodyRows = []
      for (let r = startIdx; r < tableLines.length; r++) {
        const cells = tableLines[r].split('|').filter(c => c.trim() !== '').map(c => c.trim())
        bodyRows.push(cells)
      }
      let table = '<table><thead><tr>' + headerCells.map(c => `<th>${renderInline(c)}</th>`).join('') + '</tr></thead><tbody>'
      for (const row of bodyRows) {
        table += '<tr>' + row.map(c => `<td>${renderInline(c)}</td>`).join('') + '</tr>'
      }
      table += '</tbody></table>'
      result.push(table)
      continue
    }

    // 普通段落（合并连续非空行）
    const paraLines = []
    while (i < lines.length && lines[i].trim() !== '' &&
           !lines[i].trim().startsWith('#') &&
           !lines[i].trim().startsWith('```') &&
           !lines[i].trim().startsWith('>') &&
           !lines[i].trim().startsWith('|') &&
           !/^[-*_]{3,}\s*$/.test(lines[i].trim()) &&
           !/^\s*[-*+]\s+/.test(lines[i]) &&
           !/^\s*\d+\.\s+/.test(lines[i])) {
      paraLines.push(lines[i])
      i++
    }
    if (paraLines.length > 0) {
      result.push('<p>' + renderInline(paraLines.join('\n')) + '</p>')
    }
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

  // 6. 组装（\n\n 分段：markdown 中单 \n 是同段落软换行，\n\n 才是段落分隔）
  md = lines.join('\n\n')

  // 7. 清理
  md = md.replace(/<[^>]*>/g, '')     // 移除漏网的 HTML 标签
  md = md.replace(/&nbsp;/g, ' ')
  md = md.replace(/&gt;/g, '>')       // 解码 > （contenteditable 粘贴时被转义）
  md = md.replace(/&lt;/g, '<')
  md = md.replace(/&quot;/g, '"')
  md = md.replace(/&amp;/g, '&')      // & 必须放最后，避免重复解码
  md = md.replace(/^\n+|\n+$/g, '')   // 去首尾空行

  return md
}