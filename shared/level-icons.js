/**
 * 等级图标体系（默认 8 大阶段 · 可数元素 + 稀有度配色）
 *
 * 设计口径：8 个大阶段 × 3 个小阶段 = 24 级
 *   同一大阶段的 3 个小档共用同一枚图标，不额外加标记。
 *   图标里的可数元素数量 = 大阶段序号，颜色 = 稀有度。
 *
 *   1 学徒工    1 片新芽               嫩绿
 *   2 初级工    2 把交叉开口扳手        青
 *   3 中级工    6 齿齿轮，高亮 3 个齿    天蓝
 *   4 高级工    闪电自带 4 个转角        蓝 + 黄
 *   5 技师      五角星 5 个角            靛
 *   6 高级技师  六边形螺母 6 个边        紫
 *   7 特级技师  七面宝石 7 个面          金
 *   8 首席技师  3 冠尖 + 2 暗尖 + 2 帽檐 + 1 宝石 = 8  红
 *
 * 两种渲染（现已统一为单枚图标）：
 *   levelRowSvg(levelIndex)    —— 徽章、个人中心、等级表
 *   levelEmblemSvg(levelIndex) —— 头像角标等极小尺寸
 */

const TONES = [
  { major: 1, tone: 'green', color: '#4ade80', soft: '#f0fdf4', ink: '#16a34a', name: '学徒工' },
  { major: 2, tone: 'teal', color: '#14b8a6', soft: '#f0fdfa', ink: '#0f766e', name: '初级工' },
  { major: 3, tone: 'sky', color: '#0ea5e9', soft: '#f0f9ff', ink: '#0369a1', name: '中级工' },
  { major: 4, tone: 'blue', color: '#2563eb', soft: '#eff6ff', ink: '#1d4ed8', name: '高级工' },
  { major: 5, tone: 'indigo', color: '#6366f1', soft: '#eef2ff', ink: '#4f46e5', name: '技师' },
  { major: 6, tone: 'violet', color: '#a855f7', soft: '#faf5ff', ink: '#7e22ce', name: '高级技师' },
  { major: 7, tone: 'gold', color: '#eab308', soft: '#fefce8', ink: '#a16207', name: '特级技师' },
  { major: 8, tone: 'red', color: '#ef4444', soft: '#fef2f2', ink: '#b91c1c', name: '首席技师' },
]

/* ---------- 颜色工具 ---------- */

/** 在十六进制主色上做明暗，k < 1 变暗、k > 1 变亮 */
function shade(hex, k) {
  const h = String(hex || '').replace('#', '')
  if (h.length !== 6) return hex
  const n = parseInt(h, 16)
  if (Number.isNaN(n)) return hex
  const clamp = (v) => Math.max(0, Math.min(255, Math.round(v)))
  const r = clamp(((n >> 16) & 255) * k)
  const g = clamp(((n >> 8) & 255) * k)
  const b = clamp((n & 255) * k)
  return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`
}

/* ---------- 几何小工具（64×64 画布） ---------- */

const STAR5_PATH = 'M32 5 41 24 62 27 47 42 51 62 32 52 13 62 17 42 2 27 23 24Z'
/** 星体右半（沿 x=32 切开），用于「左亮右暗」的硬切面 */
const STAR5_RIGHT = 'M32 5 41 24 62 27 47 42 51 62 32 52Z'

function polar(r, deg) {
  const a = (-90 + deg) * Math.PI / 180
  return [32 + r * Math.cos(a), 32 + r * Math.sin(a)]
}

function pointsStr(points) {
  return points.map((p) => p.map((v) => Math.round(v * 10) / 10).join(',')).join(' ')
}

/** 6 齿齿轮：只高亮指定的齿（0 = 正上方，顺时针每 60° 一个） */
function gearPolygon(indexes, rRoot, rTip, halfRoot, halfTip) {
  const points = []
  indexes.forEach((i) => {
    const a = i * 60
    points.push(
      polar(rRoot, a - halfRoot), polar(rTip, a - halfTip),
      polar(rTip, a + halfTip), polar(rRoot, a + halfRoot),
    )
  })
  return pointsStr(points)
}

/* ---------- 8 枚图标（64×64，纯几何填充） ---------- */

const LEAF = (c) =>
  `<path fill="${c}" d="M32 5C45.5 18.5 45.5 40 32 51.5C18.5 40 18.5 18.5 32 5Z"/>`
  + `<path d="M32 11V49" stroke="${shade(c, 0.72)}" stroke-width="2.2" stroke-linecap="round"/>`
  + `<path d="M32 51.5V58" stroke="${c}" stroke-width="3.4" stroke-linecap="round"/>`

const WRENCH_PATH = 'M22.7 19l-9.1-9.1c.9-2.3.4-5-1.5-6.9-2-2-5-2.4-7.4-1.3L9 6 6 9 1.6 4.7C.4 7.1.9 10.1 2.9 12.1c1.9 1.9 4.6 2.4 6.9 1.5l9.1 9.1c.4.4 1 .4 1.4 0l2.3-2.3c.5-.4.5-1.1.1-1.4z'

function wrenchHalf(fill, mirror) {
  const sx = mirror ? -1.7 : 1.7
  return `<g transform="translate(32 32) scale(${sx} 1.7) translate(-12 -12)">`
    + `<path d="${WRENCH_PATH}" fill="#fff" stroke="#fff" stroke-width="2" stroke-linejoin="round"/>`
    + `<path d="${WRENCH_PATH}" fill="${fill}"/>`
    + '</g>'
}

const WRENCH = (c) => wrenchHalf(shade(c, 0.6), true) + wrenchHalf(c, false)

const GEAR = (c) => {
  const all = gearPolygon([0, 1, 2, 3, 4, 5], 13.8, 21.6, 17, 10.5)
  const hi = gearPolygon([0, 2, 4], 13.8, 21.6, 17, 10.5)
  return `<polygon points="${all}" fill="#0f172a" opacity=".16" transform="translate(0 1.4)"/>`
    + `<polygon points="${all}" fill="${c}" stroke="${c}" stroke-width="2.4" stroke-linejoin="round"/>`
    + `<polygon points="${hi}" fill="#facc15" stroke="#facc15" stroke-width="2.4" stroke-linejoin="round"/>`
    + `<circle cx="32" cy="32" r="12" fill="none" stroke="#fff" stroke-opacity=".3" stroke-width="1.6"/>`
    + `<circle cx="32" cy="32" r="8.4" fill="#fff"/>`
    + `<circle cx="32" cy="32" r="4.2" fill="${c}"/>`
}

const BOLT_PATH = 'M37 4L15 35H28L25 60L49 27H35Z'

/** 双色偏移叠放的闪电：蓝在上、黄在右下 */
const BOLT = (c) =>
  `<g transform="translate(32 32) scale(0.84) translate(-32 -32)">`
  + `<path d="${BOLT_PATH}" fill="#facc15" transform="translate(3 3)"/>`
  + `<path d="${BOLT_PATH}" fill="${c}"/>`
  + '</g>'

const miniGear = (cx, cy, r, c) => {
  let teeth = ''
  for (let i = 0; i < 6; i += 1) {
    teeth += `<rect x="${(cx - 1.8).toFixed(1)}" y="${(cy - r - 2.6).toFixed(1)}" width="3.6" height="5" rx="1.2" fill="${c}" transform="rotate(${i * 60} ${cx} ${cy})"/>`
  }
  return `${teeth}<circle cx="${cx}" cy="${cy}" r="${r}" fill="${c}"/><circle cx="${cx}" cy="${cy}" r="${r * 0.4}" fill="#fff"/>`
}

/** 五角星：右下硬边阴影 + 左亮右暗硬切面 */
const STAR = (c) =>
  `<path d="${STAR5_PATH}" fill="${shade(c, 0.68)}" transform="translate(2.4 2.8)"/>`
  + `<path d="${STAR5_PATH}" fill="${shade(c, 1.22)}"/>`
  + `<path d="${STAR5_RIGHT}" fill="${shade(c, 0.82)}"/>`
  + `<circle cx="32" cy="37" r="9.6" fill="#fff"/>`
  + miniGear(32, 37, 5.4, c)

/** 正六边形螺母：外接圆 R=24，带侧面厚度和内孔螺纹 */
const NUT = (c) => {
  const R = 24
  const cy = 33
  const hex = []
  for (let i = 0; i < 6; i += 1) {
    const a = -Math.PI / 2 + (i * Math.PI) / 3
    hex.push([Math.round((32 + R * Math.cos(a)) * 10) / 10, Math.round((cy + R * Math.sin(a)) * 10) / 10])
  }
  const pts = pointsStr(hex)
  return `<polygon points="${pts}" fill="${shade(c, 0.6)}" transform="translate(2.4 3)"/>`
    + `<polygon points="${pts}" fill="${c}"/>`
    + `<polygon points="${hex[5].join(',')} ${hex[0].join(',')} ${hex[1].join(',')} 32,${cy}" fill="#fff" opacity=".13"/>`
    + `<circle cx="32" cy="${cy}" r="10.6" fill="#fff"/>`
    + `<circle cx="32" cy="${cy}" r="7.4" fill="none" stroke="${c}" stroke-width="1.8" stroke-dasharray="2.2 2"/>`
    + `<circle cx="32" cy="${cy}" r="3" fill="${c}"/>`
}

/** 七面宝石 */
const GEM = (c) => {
  const outer = []
  const inner = []
  for (let i = 0; i < 7; i += 1) {
    const a = -Math.PI / 2 + (Math.PI * 2 * i) / 7
    outer.push([32 + 26 * Math.cos(a), 34 + 26 * Math.sin(a)])
    inner.push([32 + 13 * Math.cos(a), 34 + 13 * Math.sin(a)])
  }
  const lines = outer
    .map((p) => `<line x1="32" y1="34" x2="${p[0].toFixed(1)}" y2="${p[1].toFixed(1)}" stroke="#f59e0b" stroke-opacity=".75" stroke-width="1.7"/>`)
    .join('')
  return `<polygon points="${pointsStr(outer)}" fill="${c}"/>`
    + `<polygon points="${pointsStr(inner)}" fill="none" stroke="#fff" stroke-opacity=".6" stroke-width="1.6"/>`
    + lines
    + `<polygon points="${pointsStr(inner)}" fill="${shade(c, 0.82)}" opacity=".95"/>`
    + `<path d="M18 22 26 15 32 20 24 27Z" fill="#fff" opacity=".28"/>`
}

/** 皇冠：3 亮冠尖 + 2 暗尖（远景）+ 2 帽檐 + 冠带 + 居中宝石 */
const CROWN = (c) => {
  const body = 'M10 48 L12 15 L25 27 L32 8 L39 27 L52 15 L54 48 Z'
  return `<path d="M20 34 L25 16 L30 34 Z" fill="${shade(c, 0.6)}"/>`
    + `<path d="M34 34 L39 16 L44 34 Z" fill="${shade(c, 0.6)}"/>`
    + `<path d="${body}" fill="${c}"/>`
    + '<rect x="5" y="44" width="54" height="11" rx="2" fill="#eab308"/>'
    + '<rect x="5" y="51" width="54" height="4" rx="2" fill="#ca8a04"/>'
    + `<path d="M32 30 L36.4 36 L32 42.6 L27.6 36 Z" fill="#fff3c4" stroke="#ca8a04" stroke-width="1.4" stroke-linejoin="round"/>`
    + '<path d="M32 30.6 L32 41.8" stroke="#eab308" stroke-width="1.1"/>'
}

const GLYPHS_64 = {
  leaf: LEAF,
  wrench2: WRENCH,
  gear3: GEAR,
  bolt4: BOLT,
  star5: STAR,
  nut6: NUT,
  gem7: GEM,
  crown8: CROWN,
}

/* ---------- 旧图形（仅用于兼容历史 iconKey，不再出现在选择器里） ---------- */

const LEGACY = {
  star: (c) => `<path fill="${c}" d="M12 1.9l3.05 6.18 6.82.99-4.94 4.81 1.17 6.79L12 17.48l-6.1 3.19 1.17-6.79L2.13 9.07l6.82-.99z"/>`,
  moon: (c) => `<path fill="${c}" d="M13.9 1.9a10.1 10.1 0 108.2 15.1A8.6 8.6 0 0113.9 1.9z"/>`,
  sun: (c) => {
    const rays = [
      [12, 1.7, 12, 4.7], [12, 19.3, 12, 22.3], [1.7, 12, 4.7, 12], [19.3, 12, 22.3, 12],
      [4.6, 4.6, 6.8, 6.8], [17.2, 17.2, 19.4, 19.4], [19.4, 4.6, 17.2, 6.8], [6.8, 17.2, 4.6, 19.4],
    ].map(([x1, y1, x2, y2]) => `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}"/>`).join('')
    return `<circle cx="12" cy="12" r="4.7" fill="${c}"/><g stroke="${c}" stroke-width="2.1" stroke-linecap="round">${rays}</g>`
  },
  crown: (c) => `<path fill="${c}" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/>`
    + `<rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="${c}"/>`,
  crownGem: (c) => `<path fill="${c}" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/>`
    + `<rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="${c}"/>`
    + '<circle cx="12" cy="10.4" r="1.85" fill="#fff" opacity="0.92"/>'
    + `<circle cx="12" cy="10.4" r="0.85" fill="${c}" opacity="0.55"/>`,
}

/* ---------- 等级 → 图形 ---------- */

const MAJOR_GLYPHS = ['leaf', 'wrench2', 'gear3', 'bolt4', 'star5', 'nut6', 'gem7', 'crown8']

/** 归一化等级序号到 1..24 */
export function normalizeLevel(levelIndex) {
  const n = Number(levelIndex)
  if (!n || n < 1) return 1
  return Math.min(24, Math.floor(n))
}

/** 等级规格：大阶段、小阶段、配色、图形 */
export function levelSpec(levelIndex) {
  const level = normalizeLevel(levelIndex)
  const major = Math.floor((level - 1) / 3) + 1
  const minor = ((level - 1) % 3) + 1
  const tone = TONES[major - 1]
  const glyph = MAJOR_GLYPHS[major - 1]
  return {
    level,
    major,
    minor,
    ...tone,
    glyph,
    glyphs: [glyph],
  }
}

/* ---------- 渲染 ---------- */

/** 完整图标；size 为像素边长 */
export function levelRowSvg(levelIndex, size) {
  const spec = levelSpec(levelIndex)
  const px = Number(size) > 0 ? Number(size) : 24
  const body = GLYPHS_64[spec.glyph](spec.color)
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="${px}" height="${px}" aria-hidden="true">${body}</svg>`
}

/** 单枚微型徽记（头像角标 / 12~18px 场景），与完整图标同一造型 */
export function levelEmblemSvg(levelIndex, size) {
  return levelRowSvg(levelIndex, size)
}

/** 兼容旧接口：iconKey = 'level-1'..'level-8'（= 大阶段序号） */
export function levelIcon(key) {
  const m = /^level-(\d+)$/.exec(String(key || ''))
  const major = m ? Math.max(1, Math.min(8, parseInt(m[1], 10))) : 1
  return levelEmblemSvg((major - 1) * 3 + 1)
}

/** 可自定义的图形清单（等级图标选择器使用） */
export const LEVEL_GLYPHS = [
  { key: 'leaf', label: '新芽' },
  { key: 'wrench2', label: '交叉扳手' },
  { key: 'gear3', label: '三亮齿齿轮' },
  { key: 'bolt4', label: '闪电' },
  { key: 'star5', label: '五角星' },
  { key: 'nut6', label: '六边螺母' },
  { key: 'gem7', label: '七面宝石' },
  { key: 'crown8', label: '皇冠' },
]

/** 单枚指定图形（供自定义图标预览；尺寸由外层 CSS 控制） */
export function glyphSvg(glyph, color) {
  const key = String(glyph || '')
  const c = color || TONES[4].color
  let body
  if (GLYPHS_64[key]) {
    body = GLYPHS_64[key](c)
  } else if (LEGACY[key]) {
    body = `<g transform="scale(2.6667)">${LEGACY[key](c)}</g>`
  } else {
    body = GLYPHS_64.star5(c)
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" aria-hidden="true">${body}</svg>`
}

/** 等级图标默认配色（供绘制器默认值） */
export function toneColor(levelIndex) {
  return levelSpec(levelIndex).color
}

/** 兼容旧常量：LEVEL_ICONS['level-1'] */
export const LEVEL_ICONS = TONES.reduce((acc, t) => {
  acc[`level-${t.major}`] = levelEmblemSvg((t.major - 1) * 3 + 1)
  return acc
}, {})

export const LEVEL_TONES = TONES
