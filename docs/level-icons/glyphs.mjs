/**
 * 两套 21 级方案的阶段图标（设计稿源）
 *
 * 设计口径（对齐默认 24 级方案 shared/level-icons.js）：
 *   · 64×64 纯几何填充，无位图、无外链；
 *   · 一个大阶段 = 一枚图标，同一阶段的 3 小档（观 / 悟 / 行）共用；
 *   · 颜色 = 稀有度，由低到高走「绿 → 青 → 蓝 → 靛/紫 → 金 → 红」；
 *   · 造型 = 该大阶段名称的语义符号，保持与默认 8 枚图标同一家族（厚填充 + 深色投影 + 白色高光）。
 *
 * 每个 draw 接收主色 c，返回 <svg> 内部的图形片段，签名与 shared/level-icons.js 的 GLYPHS_64 一致，
 * 后续接入时可直接搬进 GLYPHS_64 并注册到 LEVEL_GLYPHS。
 */

/** 在十六进制主色上做明暗，k < 1 变暗、k > 1 变亮（与 shared/level-icons.js 同实现） */
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

/** 与白色混合，t=0 原色、t=1 纯白；用于远景层（比 shade 更接近粉彩） */
function tint(hex, t) {
  const h = String(hex || '').replace('#', '')
  if (h.length !== 6) return hex
  const n = parseInt(h, 16)
  if (Number.isNaN(n)) return hex
  const mix = (v) => Math.round(v + (255 - v) * t)
  const r = mix((n >> 16) & 255)
  const g = mix((n >> 8) & 255)
  const b = mix(n & 255)
  return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`
}

export const SCHEME_A = {
  key: 'scheme-a',
  name: '方案一 · 智荟成长',
  note: '论坛成长线：新芽 → 拾光 → 先锋 → 造浪 → 引路 → 领航 → 执炬',
  stages: [
    {
      major: 1,
      majorName: '智荟新芽',
      ref: '初级工',
      color: '#4ade80',
      ink: '#16a34a',
      soft: '#f0fdf4',
      badge: '新芽徽章',
      glyph: 'zySprout',
      label: '新芽 + 露珠',
      draw: (c) => `<path d="M32 7C46 20 46 42 32 53C18 42 18 20 32 7Z" fill="${c}"/>
  <path d="M32 7C46 20 46 42 32 53Z" fill="${shade(c, 0.82)}"/>
  <path d="M32 13V50" stroke="${shade(c, 0.55)}" stroke-width="2.2" stroke-linecap="round"/>
  <path d="M33 36C41 31 47 34 48 40C41 45 34 42 33 36Z" fill="${shade(c, 0.7)}"/>
  <path d="M31 30C23 25 17 28 16 34C23 39 30 36 31 30Z" fill="${shade(c, 1.3)}"/>
  <path d="M32 53V58" stroke="${shade(c, 0.7)}" stroke-width="3.6" stroke-linecap="round"/>
  <circle cx="44" cy="16" r="4.6" fill="#ffffff" stroke="${shade(c, 0.7)}" stroke-width="1.6"/>
  <path d="M42.2 16.6A2.8 2.8 0 0 1 45.6 13.6" fill="none" stroke="${shade(c, 0.9)}" stroke-width="1.4" stroke-linecap="round"/>`,
    },
    {
      major: 2,
      majorName: '拾光新锐',
      ref: '中级工',
      color: '#14b8a6',
      ink: '#0f766e',
      soft: '#f0fdfa',
      badge: '耕耘徽章',
      glyph: 'zyGlow',
      label: '三束光汇成晶石',
      draw: (c) => `<g stroke="${c}" stroke-width="4.6" stroke-linecap="round">
    <path d="M11 9 24 22"/>
    <path d="M32 4V22"/>
    <path d="M53 9 40 22"/>
  </g>
  <path d="M32 22 44 36 32 54 20 36Z" fill="${c}"/>
  <path d="M32 22 44 36 32 54Z" fill="${shade(c, 0.78)}"/>
  <path d="M32 22 44 36 32 36Z" fill="${shade(c, 1.35)}"/>
  <circle cx="32" cy="36" r="3.2" fill="#ffffff"/>
  <path d="M20 58H44" stroke="${shade(c, 1.35)}" stroke-width="3" stroke-linecap="round"/>`,
    },
    {
      major: 3,
      majorName: '精锐先锋',
      ref: '高级工',
      color: '#0ea5e9',
      ink: '#0369a1',
      soft: '#f0f9ff',
      badge: '创见徽章',
      glyph: 'zyArrow',
      label: '上冲箭头 + 速度线',
      draw: (c) => `<path d="M32 6 55 31H43V50H21V31H9Z" fill="${c}"/>
  <path d="M32 6 55 31H43V50H32Z" fill="${shade(c, 0.8)}"/>
  <path d="M10 57H23" stroke="${shade(c, 1.5)}" stroke-width="3.4" stroke-linecap="round"/>
  <path d="M41 57H54" stroke="${shade(c, 1.5)}" stroke-width="3.4" stroke-linecap="round"/>`,
    },
    {
      major: 4,
      majorName: '破局造浪人',
      ref: '技师',
      color: '#2563eb',
      ink: '#1d4ed8',
      soft: '#eff6ff',
      badge: '砺新徽章',
      glyph: 'zyWave',
      label: '三道浪：远景 + 主卷浪 + 碎浪',
      draw: (c) => `<path d="M0 34C1 22 6 14 14 12C19 10.5 23 13 23 18C23 22 20 25 16 25C18 20 16 17 12 17C7 18 4 25 3 33Z" fill="${tint(c, 0.55)}"/>
  <path d="M2 58C2 36 9 19 26 15C35 12.5 41 18 41 27C41 31 38 34 33 34C36 28 34 23 29 22C24 22 21 27 21 33C21 39 26 43 33 45L32 58Z" fill="${c}"/>
  <path d="M26 15C35 12.5 41 18 41 27C41 31 38 34 33 34C36 28 34 23 29 22" fill="none" stroke="${shade(c, 1.4)}" stroke-width="3" stroke-linecap="round"/>
  <path d="M46 58L43 52L49 49L44 41C47 33 52 27 58 24C61 22.5 62 25 62 30L62 58Z" fill="${shade(c, 0.82)}"/>
  <path d="M44 41C47 33 52 27 58 24C61 22.5 62 25 62 30" fill="none" stroke="${shade(c, 1.3)}" stroke-width="2.8" stroke-linecap="round"/>
  <path d="M47 9 54 15 47 18Z" fill="${shade(c, 1.5)}"/>
  <path d="M56 2 60 9 53 10Z" fill="${shade(c, 1.5)}"/>
  <circle cx="40" cy="9" r="2.2" fill="${shade(c, 1.7)}"/>`,
    },
    {
      major: 5,
      majorName: '知灯引路人',
      ref: '高级技师',
      color: '#6366f1',
      ink: '#4338ca',
      soft: '#eef2ff',
      badge: '传知徽章',
      glyph: 'zyLantern',
      label: '宫灯 + 灯光四射',
      draw: (c) => `<path d="M32 4V11" stroke="${shade(c, 0.7)}" stroke-width="3.2" stroke-linecap="round"/>
  <path d="M21 12H43L46 19H18Z" fill="${c}"/>
  <path d="M23 19H41V45H23Z" fill="${c}"/>
  <path d="M32 19H41V45H32Z" fill="${shade(c, 0.8)}"/>
  <path d="M18 45H46L42 52H22Z" fill="${shade(c, 0.9)}"/>
  <path d="M32 52V58" stroke="${shade(c, 0.7)}" stroke-width="3" stroke-linecap="round"/>
  <circle cx="32" cy="32" r="6.6" fill="#ffffff"/>
  <circle cx="32" cy="32" r="3.2" fill="#fbbf24"/>
  <g stroke="${shade(c, 1.5)}" stroke-width="2.6" stroke-linecap="round">
    <path d="M12 26 17 28"/><path d="M12 38 17 36"/>
    <path d="M52 26 47 28"/><path d="M52 38 47 36"/>
  </g>`,
    },
    {
      major: 6,
      majorName: '云衢领航者',
      ref: '特级技师',
      color: '#eab308',
      ink: '#a16207',
      soft: '#fefce8',
      badge: '星引徽章',
      glyph: 'zyCompass',
      label: '罗盘星 + 云衢',
      draw: (c) => `<path d="M32 3 38.5 19.5 55 26 38.5 32.5 32 49 25.5 32.5 9 26 25.5 19.5Z" fill="${c}"/>
  <path d="M32 3 38.5 19.5 55 26 32 26Z" fill="${shade(c, 1.18)}"/>
  <circle cx="32" cy="26" r="5.8" fill="#fffbeb" stroke="${shade(c, 0.62)}" stroke-width="1.8"/>
  <circle cx="32" cy="26" r="2.2" fill="${c}"/>
  <g fill="none" stroke="${shade(c, 1.2)}" stroke-width="4" stroke-linecap="round">
    <path d="M8 53C12 47 18 47 22 53"/>
    <path d="M25 59C29 53 35 53 39 59"/>
    <path d="M42 53C46 47 52 47 56 53"/>
  </g>`,
    },
    {
      major: 7,
      majorName: '大千执炬者',
      ref: '首席技师',
      color: '#ef4444',
      ink: '#b91c1c',
      soft: '#fef2f2',
      badge: '大成徽章',
      glyph: 'zyTorch',
      label: '火炬 + 四溅火星',
      draw: (c) => `<path d="M32 3C42 10 44 20 39 26 37 19 34 17 31 15 31 21 29 24 27 27 22 21 24 10 32 3Z" fill="${c}"/>
  <path d="M32 11C36 15 37 20 34 24 33 20 31 19 30 18 30 21 29 23 28 24 26 21 27 16 32 11Z" fill="#fde68a"/>
  <rect x="24" y="27" width="16" height="5" rx="2.5" fill="#fbbf24"/>
  <path d="M28 32H36L34 48H30Z" fill="${shade(c, 0.72)}"/>
  <path d="M28 32H32L31 48H30Z" fill="${c}"/>
  <path d="M22 48H42L37 58H27Z" fill="${shade(c, 0.72)}"/>
  <g fill="${shade(c, 1.45)}">
    <circle cx="13" cy="13" r="2.6"/><circle cx="21" cy="6" r="2"/>
    <circle cx="51" cy="13" r="2.6"/><circle cx="43" cy="6" r="2"/>
  </g>`,
    },
  ],
}

export const SCHEME_B = {
  key: 'scheme-b',
  name: '方案二 · 电网匠心',
  note: '电网业务线：银线 → 杆塔 → 送电 → 电光 → 熔光 → 经纬 → 万伏',
  stages: [
    {
      major: 1,
      majorName: '银线学徒',
      ref: '初级工',
      color: '#94a3b8',
      ink: '#475569',
      soft: '#f8fafc',
      badge: '线芯徽章',
      glyph: 'dwWire',
      label: '线轴 + 裸铜线头',
      draw: (c) => `<rect x="8" y="11" width="5.4" height="42" rx="2.7" fill="${shade(c, 0.62)}"/>
  <rect x="38.6" y="11" width="5.4" height="42" rx="2.7" fill="${shade(c, 0.62)}"/>
  <rect x="13.4" y="17" width="25.2" height="30" rx="3.4" fill="${c}"/>
  <g stroke="${shade(c, 1.35)}" stroke-width="2.2" stroke-linecap="round">
    <path d="M15 24H37"/><path d="M15 32H37"/><path d="M15 40H37"/>
  </g>
  <path d="M44 26C53 22 60 31 55 41" fill="none" stroke="${c}" stroke-width="3.6" stroke-linecap="round"/>
  <path d="M55 41 60 48 51 49Z" fill="#f59e0b"/>`,
    },
    {
      major: 2,
      majorName: '线网知客',
      ref: '中级工',
      color: '#14b8a6',
      ink: '#0f766e',
      soft: '#f0fdfa',
      badge: '杆塔徽章',
      glyph: 'dwPylon',
      label: '输电杆塔 + 三回线路',
      draw: (c) => `<g stroke="${shade(c, 1.45)}" stroke-width="2.6" stroke-linecap="round">
    <path d="M5 24H59"/><path d="M8 36H56"/><path d="M12 47H52"/>
  </g>
  <path d="M32 5 21 58H43Z" fill="none" stroke="${c}" stroke-width="4.4" stroke-linejoin="round"/>
  <path d="M23 20H41" stroke="${c}" stroke-width="4.4" stroke-linecap="round"/>
  <path d="M18 34H46" stroke="${c}" stroke-width="4.4" stroke-linecap="round"/>
  <path d="M22 47H42" stroke="${c}" stroke-width="4.4" stroke-linecap="round"/>
  <path d="M26 50 32 40 38 50" fill="none" stroke="${c}" stroke-width="3" stroke-linejoin="round"/>`,
    },
    {
      major: 3,
      majorName: '送电革新者',
      ref: '高级工',
      color: '#0ea5e9',
      ink: '#0369a1',
      soft: '#f0f9ff',
      badge: '革新徽章',
      glyph: 'dwBoltLoop',
      label: '闪电 + 循环箭头',
      draw: (c) => `<path d="M36 4 16 32H28L25 60 46 28H34Z" fill="${c}"/>
  <path d="M36 4 16 32H28L25 60 34 40Z" fill="${shade(c, 0.8)}"/>
  <path d="M10 22A23 23 0 0 1 21 7" fill="none" stroke="${shade(c, 1.5)}" stroke-width="3.4" stroke-linecap="round"/>
  <path d="M54 42A23 23 0 0 1 43 57" fill="none" stroke="${shade(c, 1.5)}" stroke-width="3.4" stroke-linecap="round"/>
  <path d="M21 7 26 13 18 14Z" fill="${shade(c, 1.5)}"/>
  <path d="M43 57 38 51 46 50Z" fill="${shade(c, 1.5)}"/>`,
    },
    {
      major: 4,
      majorName: '电光造梦师',
      ref: '技师',
      color: '#2563eb',
      ink: '#1d4ed8',
      soft: '#eff6ff',
      badge: '电芒徽章',
      glyph: 'dwBulb',
      label: '灯泡 + 电弧灯丝',
      draw: (c) => `<path d="M32 5C21 5 13 13 13 24C13 31 17 36 21 41C23 43 24 46 24 49H40C40 46 41 43 43 41C47 36 51 31 51 24C51 13 43 5 32 5Z" fill="${c}"/>
  <path d="M32 5C43 5 51 13 51 24C51 31 47 36 43 41C41 43 40 46 40 49H32Z" fill="${shade(c, 0.82)}"/>
  <path d="M24 49H40V54H24Z" fill="${shade(c, 1.6)}"/>
  <path d="M28 58H36" stroke="${shade(c, 1.6)}" stroke-width="3" stroke-linecap="round"/>
  <path d="M32 24 26 35H31L29 44 38 32H33Z" fill="#fbbf24"/>
  <g stroke="${shade(c, 1.5)}" stroke-width="3" stroke-linecap="round">
    <path d="M7 15 13 19"/><path d="M57 15 51 19"/><path d="M3 30H9"/><path d="M61 30H55"/>
  </g>`,
    },
    {
      major: 5,
      majorName: '熔光传习师',
      ref: '高级技师',
      color: '#a855f7',
      ink: '#7e22ce',
      soft: '#faf5ff',
      badge: '传灯徽章',
      glyph: 'dwCrucible',
      label: '熔炉 + 熔光焰',
      draw: (c) => `<path d="M14 25H50L45 51C44 55 41 57 37 57H27C23 57 20 55 19 51Z" fill="${c}"/>
  <path d="M32 25H50L45 51C44 55 41 57 37 57H32Z" fill="${shade(c, 0.8)}"/>
  <ellipse cx="32" cy="25" rx="18" ry="5.6" fill="${shade(c, 1.7)}"/>
  <ellipse cx="32" cy="25" rx="12" ry="3.4" fill="${shade(c, 1.35)}"/>
  <path d="M14 31C8 31 8 43 14 43" fill="none" stroke="${shade(c, 0.68)}" stroke-width="3.4" stroke-linecap="round"/>
  <path d="M50 31C56 31 56 43 50 43" fill="none" stroke="${shade(c, 0.68)}" stroke-width="3.4" stroke-linecap="round"/>
  <path d="M32 4C39 10 40 16 36 21 34 16 31 15 30 14 30 18 28 20 26 22 22 17 25 9 32 4Z" fill="${c}"/>
  <path d="M32 9C35 13 36 17 33 20 32 17 30 16 29 15 29 18 28 19 27 20 25 17 27 12 32 9Z" fill="${shade(c, 1.6)}"/>
  <g fill="${shade(c, 1.5)}">
    <circle cx="12" cy="14" r="2.2"/><circle cx="52" cy="14" r="2.2"/>
    <circle cx="20" cy="6" r="1.8"/><circle cx="44" cy="6" r="1.8"/>
  </g>`,
    },
    {
      major: 6,
      majorName: '经纬领航者',
      ref: '特级技师',
      color: '#eab308',
      ink: '#a16207',
      soft: '#fefce8',
      badge: '母线徽章',
      glyph: 'dwGlobe',
      label: '经纬球 + 罗盘针',
      draw: (c) => `<circle cx="32" cy="32" r="25" fill="${c}" stroke="${shade(c, 0.62)}" stroke-width="2"/>
  <g fill="none" stroke="${shade(c, 0.62)}" stroke-width="2" opacity=".55">
    <ellipse cx="32" cy="32" rx="9" ry="25"/>
    <ellipse cx="32" cy="32" rx="18" ry="25"/>
    <path d="M7 32H57"/>
    <path d="M10.5 20H53.5"/>
    <path d="M10.5 44H53.5"/>
  </g>
  <path d="M32 10 41 32 32 54 23 32Z" fill="#fffbeb" stroke="${shade(c, 0.62)}" stroke-width="1.4" stroke-linejoin="round"/>
  <path d="M32 10 41 32 32 32Z" fill="#dc2626"/>
  <circle cx="32" cy="32" r="3" fill="${shade(c, 0.62)}"/>`,
    },
    {
      major: 7,
      majorName: '万伏执炬人',
      ref: '首席技师',
      color: '#dc2626',
      ink: '#991b1b',
      soft: '#fef2f2',
      badge: '金炬徽章',
      glyph: 'dwTowerTorch',
      label: '高压塔举火炬',
      draw: (c) => `<path d="M32 2C37 7 37 12 34 15 33 11 31 10 30 9 30 12 29 13 28 14 25 10 27 5 32 2Z" fill="#fbbf24"/>
  <path d="M32 7C34 10 34 12 32.5 14 32 12 31 11.5 30.5 11 30.5 13 30 13.5 29.5 14 28 12 29 9 32 7Z" fill="#fde68a"/>
  <path d="M32 15 22 59H42Z" fill="none" stroke="${c}" stroke-width="4.4" stroke-linejoin="round"/>
  <path d="M21 25H43" stroke="${c}" stroke-width="4.4" stroke-linecap="round"/>
  <path d="M24 38H40" stroke="${c}" stroke-width="4.4" stroke-linecap="round"/>
  <path d="M25 52 32 44 39 52" fill="none" stroke="${c}" stroke-width="3.2" stroke-linejoin="round"/>
  <g stroke="${shade(c, 1.55)}" stroke-width="2.4" stroke-linecap="round">
    <path d="M9 8 15 12"/><path d="M55 8 49 12"/><path d="M5 24H11"/><path d="M59 24H53"/>
  </g>`,
    },
  ],
}

/** 两套方案共用的累计积分门槛（v061 起） */
export const THRESHOLDS = [
  0, 20, 40, 60, 90, 120, 150, 200, 250, 300, 400,
  500, 600, 700, 800, 900, 1100, 1300, 1500, 1700, 1900,
]

export const TIER_SUFFIX = ['观', '悟', '行']

export const SCHEMES = [SCHEME_A, SCHEME_B]
