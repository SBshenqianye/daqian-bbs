/**
 * 精华跑马灯封面生成插件（Canvas）
 *
 * 当精华帖没有上传封面图时，用「标题 + 正文摘要 + 作者」现场生成一张
 * 小红书风格的内容卡，避免跑马灯出现大片空白。
 *
 * 不依赖第三方包：只使用 Canvas 2D API，生成结果作为 dataURL 直接喂给 <img>。
 */

const WIDTH = 700
const HEIGHT = 500
const PAD = 44

const FONT_STACK = '"Manrope", "Microsoft YaHei", "PingFang SC", "Noto Sans SC", sans-serif'

// 三套同一蓝色系内的渐变，避免所有卡片完全一样，同时不破坏论坛主色
const PALETTES = [
  ['#061a3d', '#0052d9', '#ffd76a'],
  ['#0b1f5e', '#2f5cff', '#9ae6ff'],
  ['#04263d', '#0b6bcb', '#ffd76a'],
]

const cache = new Map()

function hashString(value) {
  let hash = 0
  const text = String(value || '')
  for (let i = 0; i < text.length; i++) {
    hash = ((hash << 5) - hash + text.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}

function roundRect(ctx, x, y, w, h, r) {
  const radius = Math.min(r, w / 2, h / 2)
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.arcTo(x + w, y, x + w, y + h, radius)
  ctx.arcTo(x + w, y + h, x, y + h, radius)
  ctx.arcTo(x, y + h, x, y, radius)
  ctx.arcTo(x, y, x + w, y, radius)
  ctx.closePath()
}

/** 按字符折行，中英文都不会整段溢出 */
function wrapLines(ctx, text, maxWidth, maxLines) {
  const chars = Array.from(String(text || '').replace(/\s+/g, ' ').trim())
  const lines = []
  let line = ''
  for (const ch of chars) {
    if (ch === '\n') {
      if (line) lines.push(line)
      line = ''
      if (lines.length >= maxLines) break
      continue
    }
    const next = line + ch
    if (line && ctx.measureText(next).width > maxWidth) {
      lines.push(line.trim())
      line = ch === ' ' ? '' : ch
      if (lines.length >= maxLines) break
    } else {
      line = next
    }
  }
  if (lines.length < maxLines && line.trim()) lines.push(line.trim())
  return lines
}

function titleFontSize(title) {
  const length = Array.from(String(title || '')).length
  if (length <= 12) return 49
  if (length <= 24) return 43
  return 37
}

/**
 * 生成封面。返回空字符串表示当前环境不支持 Canvas。
 * @param {{articleId?: string|number, title?: string, summary?: string, content?: string, author?: string, views?: number}} article
 */
export function generateFeaturedCover(article) {
  if (typeof document === 'undefined') return ''
  const data = article || {}
  const cacheKey = [data.articleId, data.title, data.content, data.summary, data.author].join('|')
  if (cache.has(cacheKey)) return cache.get(cacheKey)

  const canvas = document.createElement('canvas')
  canvas.width = WIDTH
  canvas.height = HEIGHT
  const ctx = canvas.getContext('2d')
  if (!ctx) return ''

  const palette = PALETTES[hashString(data.articleId || data.title) % PALETTES.length]
  const [from, to, accent] = palette

  // 主渐变
  const bg = ctx.createLinearGradient(0, 0, WIDTH, HEIGHT)
  bg.addColorStop(0, from)
  bg.addColorStop(1, to)
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, WIDTH, HEIGHT)

  // 两个半透明圆形，给纯文字封面一点层次
  ctx.save()
  ctx.fillStyle = 'rgba(255,255,255,0.10)'
  ctx.beginPath()
  ctx.arc(WIDTH - 40, -40, 190, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = 'rgba(255,255,255,0.06)'
  ctx.beginPath()
  ctx.arc(WIDTH - 90, HEIGHT + 30, 250, 0, Math.PI * 2)
  ctx.fill()
  ctx.restore()

  // 右上角大引号：填充空白但不抢标题
  ctx.save()
  ctx.globalAlpha = 0.08
  ctx.fillStyle = '#ffffff'
  ctx.font = `700 230px ${FONT_STACK}`
  ctx.fillText('“', WIDTH - 218, 258)
  ctx.restore()

  // 顶部标签
  ctx.fillStyle = 'rgba(255,255,255,0.16)'
  roundRect(ctx, PAD, 33, 150, 32, 16)
  ctx.fill()
  ctx.fillStyle = accent
  ctx.font = `700 17px ${FONT_STACK}`
  ctx.textBaseline = 'middle'
  ctx.fillText('★ 精华推荐', PAD + 15, 50)
  ctx.textBaseline = 'alphabetic'

  // 右上角站点名
  ctx.fillStyle = 'rgba(255,255,255,0.55)'
  ctx.font = `500 17px ${FONT_STACK}`
  ctx.textAlign = 'right'
  ctx.fillText('大千智荟', WIDTH - PAD, 54)
  ctx.textAlign = 'left'

  const title = data.title || '未命名内容'
  const titleSize = titleFontSize(title)
  const titleLineHeight = Math.round(titleSize * 1.28)
  ctx.font = `700 ${titleSize}px ${FONT_STACK}`
  ctx.fillStyle = '#ffffff'
  const titleLines = wrapLines(ctx, title, WIDTH - PAD * 2, 3)
  let cursorY = 116
  titleLines.forEach((line) => {
    ctx.fillText(line, PAD, cursorY)
    cursorY += titleLineHeight
  })

  // 正文摘要（优先正文，其次文章摘要）；过短的正文用说明文案填充版面
  const excerpt = String(data.content || data.summary || '').trim()
  const fallback = '在论坛里读到一篇值得分享的内容，点击查看全文。'
  const excerptText = Array.from(excerpt).length >= 8 ? excerpt : fallback
  ctx.font = `400 25px ${FONT_STACK}`
  ctx.fillStyle = 'rgba(255,255,255,0.78)'
  const excerptMaxLines = titleLines.length >= 3 ? 2 : 3
  const excerptLines = wrapLines(ctx, excerptText, WIDTH - PAD * 2, excerptMaxLines)
  let excerptY = cursorY + 18
  excerptLines.forEach((line) => {
    ctx.fillText(line, PAD, excerptY)
    excerptY += 34
  })

  // 底部信息
  ctx.strokeStyle = 'rgba(255,255,255,0.20)'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(PAD, HEIGHT - 78)
  ctx.lineTo(WIDTH - PAD, HEIGHT - 78)
  ctx.stroke()

  ctx.fillStyle = 'rgba(255,255,255,0.92)'
  ctx.font = `600 21px ${FONT_STACK}`
  ctx.fillText(`@${data.author || '论坛用户'}`, PAD, HEIGHT - 42)

  ctx.fillStyle = 'rgba(255,255,255,0.58)'
  ctx.font = `400 17px ${FONT_STACK}`
  ctx.textAlign = 'right'
  ctx.fillText(`${data.views || 0} 阅读 · 大千智荟原创`, WIDTH - PAD, HEIGHT - 42)
  ctx.textAlign = 'left'

  const url = canvas.toDataURL('image/jpeg', 0.9)
  cache.set(cacheKey, url)
  return url
}
