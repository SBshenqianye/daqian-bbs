/**
 * 大千 BBS Markdown 渲染
 *
 * mdToHtml 轻量 Markdown 渲染器（零外部依赖）。
 * 发帖页编辑器直接持有 Markdown 源码，发布/详情/预览共用本函数渲染。
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

    // 有序列表 1. 2. 3. —— 保留原始序号显示，不用 <ol> 自动编号
    if (/^\s*\d+\.\s+/.test(line)) {
      const items = []
      while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
        items.push(lines[i].trim())
        i++
      }
      result.push(items.map(item => `<p style="padding-left:2em;text-indent:-2em;margin:0">${renderInline(item)}</p>`).join(''))
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
