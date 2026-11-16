/**
 * 等级图标体系（QQ 风格：星星 → 月亮 → 太阳 → 皇冠）
 *
 * 设计口径：8 个大阶段 × 3 个小阶段 = 24 级
 *   1 学徒工    ★ ×1..3
 *   2 初级工    ☾ ×1..3
 *   3 中级工    ☀ ×1..3
 *   4 高级工    ♛ ×1..3
 *   5 技师      ♛ + ★ ×1..3   （紫）
 *   6 高级技师  ♛ + ☾ ×1..3   （靛）
 *   7 特级技师  ♛ + ☀ ×1..3   （玫红）
 *   8 首席技师  ♛💎 + ★ ×1..3 （红金，至尊冠）
 *
 * 两种渲染：
 *   levelRowSvg(levelIndex)    —— 完整「图标串」，用于徽章、个人中心、等级表
 *   levelEmblemSvg(levelIndex) —— 单枚「微型徽记」，用于头像角标等极小尺寸
 */

const TONES = [
  { major: 1, tone: 'green', color: '#16a34a', soft: '#dcfce7', name: '学徒工' },
  { major: 2, tone: 'sky', color: '#0284c7', soft: '#e0f2fe', name: '初级工' },
  { major: 3, tone: 'amber', color: '#d97706', soft: '#fef3c7', name: '中级工' },
  { major: 4, tone: 'gold', color: '#ca8a04', soft: '#fef9c3', name: '高级工' },
  { major: 5, tone: 'violet', color: '#7c3aed', soft: '#ede9fe', name: '技师' },
  { major: 6, tone: 'indigo', color: '#4f46e5', soft: '#e0e7ff', name: '高级技师' },
  { major: 7, tone: 'pink', color: '#db2777', soft: '#fce7f3', name: '特级技师' },
  { major: 8, tone: 'red', color: '#dc2626', soft: '#fee2e2', name: '首席技师' },
]

/* ---------- 基础图形（均按 24×24 画布绘制，纯填充，小尺寸也清晰） ---------- */

const STAR = (c) =>
  `<path fill="${c}" d="M12 1.9l3.05 6.18 6.82.99-4.94 4.81 1.17 6.79L12 17.48l-6.1 3.19 1.17-6.79L2.13 9.07l6.82-.99z"/>`

const MOON = (c) =>
  `<path fill="${c}" d="M13.9 1.9a10.1 10.1 0 108.2 15.1A8.6 8.6 0 0113.9 1.9z"/>`

const SUN = (c) => {
  const rays = [
    [12, 1.7, 12, 4.7], [12, 19.3, 12, 22.3], [1.7, 12, 4.7, 12], [19.3, 12, 22.3, 12],
    [4.6, 4.6, 6.8, 6.8], [17.2, 17.2, 19.4, 19.4], [19.4, 4.6, 17.2, 6.8], [6.8, 17.2, 4.6, 19.4],
  ]
    .map(([x1, y1, x2, y2]) => `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}"/>`)
    .join('')
  return `<circle cx="12" cy="12" r="4.7" fill="${c}"/>`
    + `<g stroke="${c}" stroke-width="2.1" stroke-linecap="round">${rays}</g>`
}

const CROWN_BODY = 'M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z'
const CROWN = (c) =>
  `<path fill="${c}" d="${CROWN_BODY}"/>`
  + `<rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="${c}"/>`

const CROWN_GEM = (c) =>
  CROWN(c)
  + `<circle cx="12" cy="10.4" r="1.85" fill="#fff" opacity="0.92"/>`
  + `<circle cx="12" cy="10.4" r="0.85" fill="${c}" opacity="0.55"/>`

const GLYPHS = {
  star: STAR,
  moon: MOON,
  sun: SUN,
  crown: CROWN,
  crownGem: CROWN_GEM,
}

/* ---------- 等级 → 图形组合 ---------- */

function repeat(key, times) {
  const n = Math.max(1, Math.min(3, times))
  return Array.from({ length: n }, () => key)
}

function rowGlyphKeys(major, minor) {
  switch (major) {
    case 1: return repeat('star', minor)
    case 2: return repeat('moon', minor)
    case 3: return repeat('sun', minor)
    case 4: return repeat('crown', minor)
    case 5: return ['crown', ...repeat('star', minor)]
    case 6: return ['crown', ...repeat('moon', minor)]
    case 7: return ['crown', ...repeat('sun', minor)]
    default: return ['crownGem', ...repeat('star', minor)]
  }
}

/** 归一化等级序号到 1..24 */
export function normalizeLevel(levelIndex) {
  const n = Number(levelIndex)
  if (!n || n < 1) return 1
  return Math.min(24, Math.floor(n))
}

/** 等级规格：大阶段、小阶段、配色、图形串 */
export function levelSpec(levelIndex) {
  const level = normalizeLevel(levelIndex)
  const major = Math.floor((level - 1) / 3) + 1
  const minor = ((level - 1) % 3) + 1
  const tone = TONES[major - 1]
  return {
    level,
    major,
    minor,
    ...tone,
    glyphs: rowGlyphKeys(major, minor),
  }
}

/* ---------- 渲染 ---------- */

/** 完整图标串；size 传入时按像素高度输出，宽度按比例 */
export function levelRowSvg(levelIndex, size) {
  const spec = levelSpec(levelIndex)
  const gap = 1.6
  const vbWidth = spec.glyphs.length * 24 + (spec.glyphs.length - 1) * gap
  const body = spec.glyphs
    .map((key, i) => `<g transform="translate(${(i * (24 + gap)).toFixed(2)} 0)">${GLYPHS[key](spec.color)}</g>`)
    .join('')
  const h = Number(size) > 0 ? Number(size) : 24
  const w = (vbWidth / 24) * h
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${vbWidth} 24" width="${w.toFixed(2)}" height="${h}" aria-hidden="true">${body}</svg>`
}

/** 单枚微型徽记（头像角标 / 12~18px 场景） */
export function levelEmblemSvg(levelIndex, size) {
  const spec = levelSpec(levelIndex)
  let body
  if (spec.major <= 4) {
    const key = ['star', 'moon', 'sun', 'crown'][spec.major - 1]
    body = `<g transform="translate(1.5 1.5) scale(0.875)">${GLYPHS[key](spec.color)}</g>`
  } else if (spec.major <= 7) {
    const sat = ['star', 'moon', 'sun'][spec.major - 5]
    body = `<g transform="translate(0.4 3.2) scale(0.8)">${GLYPHS.crown(spec.color)}</g>`
      + `<g transform="translate(14.2 0) scale(0.4)">${GLYPHS[sat]('#f59e0b')}</g>`
  } else {
    body = `<g transform="translate(1.5 1.5) scale(0.875)">${GLYPHS.crownGem(spec.color)}</g>`
    + `<g transform="translate(15.4 1) scale(0.34)">${GLYPHS.star('#fbbf24')}</g>`
  }
  const s = Number(size) > 0 ? Number(size) : 24
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="${s}" height="${s}" aria-hidden="true">${body}</svg>`
}

/** 兼容旧接口：iconKey = 'level-1'..'level-8'（= 大阶段序号） */
export function levelIcon(key) {
  const m = /^level-(\d+)$/.exec(String(key || ''))
  const major = m ? Math.max(1, Math.min(8, parseInt(m[1], 10))) : 1
  return levelEmblemSvg((major - 1) * 3 + 1)
}

/** 可自定义的图形清单（等级图标选择器 / 绘制器使用） */
export const LEVEL_GLYPHS = [
  { key: 'star', label: '星星' },
  { key: 'moon', label: '月亮' },
  { key: 'sun', label: '太阳' },
  { key: 'crown', label: '皇冠' },
  { key: 'crownGem', label: '宝石皇冠' },
]

/** 单枚指定图形（供自定义图标预览；尺寸由外层 CSS 控制） */
export function glyphSvg(glyph, color) {
  const draw = GLYPHS[glyph] || GLYPHS.star
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" aria-hidden="true">${draw(color || TONES[0].color)}</svg>`
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
