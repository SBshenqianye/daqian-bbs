/**
 * 生成两套 21 级方案的图标文件与预览页。
 * 运行：node docs/level-icons/build-preview.mjs
 * 产物：docs/level-icons/<scheme>/<major>-<glyph>.svg、docs/level-icons/preview.html
 */
import { writeFileSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { SCHEMES, THRESHOLDS, TIER_SUFFIX } from './glyphs.mjs'
import { levelRowSvg, LEVEL_TONES } from '../../shared/level-icons.js'

const here = dirname(fileURLToPath(import.meta.url))

function svgFile(stage) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64" role="img" aria-label="${stage.majorName}">
  <title>${stage.majorName}</title>
${stage.draw(stage.color)}
</svg>
`
}

function inlineSvg(stage, size) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="${size}" height="${size}" role="img" aria-label="${stage.majorName}">${stage.draw(stage.color)}</svg>`
}

/* ---------- 1. 单枚 SVG 文件 ---------- */
SCHEMES.forEach((scheme) => {
  const dir = join(here, scheme.key)
  mkdirSync(dir, { recursive: true })
  scheme.stages.forEach((stage) => {
    writeFileSync(join(dir, `${stage.major}-${stage.glyph}.svg`), svgFile(stage), 'utf8')
  })
})

/* ---------- 2. 预览页 ---------- */
const refRow = LEVEL_TONES.map((tone) => `
      <div class="ref-item">
        <div class="ref-icon">${levelRowSvg((tone.major - 1) * 3 + 1, 46)}</div>
        <div class="ref-name">${tone.name}</div>
        <div class="ref-lv">Lv.${(tone.major - 1) * 3 + 1}–${tone.major * 3}</div>
      </div>`).join('')

function card(scheme, stage) {
  const first = (stage.major - 1) * 3
  const tiers = TIER_SUFFIX.map((suffix, i) => {
    const lv = first + i + 1
    const badge = i === 0 ? `<span class="badge">${stage.badge}</span>` : ''
    return `<li>
            <span class="lv">Lv.${lv}</span>
            <span class="tname">${stage.majorName}·${suffix}</span>
            <span class="pt">${THRESHOLDS[lv - 1]} 分</span>
            ${badge}
          </li>`
  }).join('')

  return `
      <article class="card" style="--c:${stage.color};--soft:${stage.soft};--ink:${stage.ink}">
        <div class="card-top">
          <div class="icon">${inlineSvg(stage, 68)}</div>
          <div class="head">
            <h3>${stage.majorName}</h3>
            <div class="meta">对标 ${stage.ref} · ${stage.label}</div>
            <div class="chip">${stage.color.toUpperCase()}</div>
          </div>
        </div>
        <ul class="tiers">${tiers}</ul>
      </article>`
}

const sections = SCHEMES.map((scheme) => `
  <section class="scheme">
    <div class="scheme-head">
      <h2>${scheme.name}</h2>
      <span class="sub">7 大阶段 × 3 小档 = 21 级</span>
    </div>
    <p class="note">${scheme.note}</p>
    <div class="grid">${scheme.stages.map((s) => card(scheme, s)).join('')}</div>
  </section>`).join('')

const palette = SCHEMES.map((scheme) => `
      <div class="palette">
        <span class="palette-name">${scheme.name}</span>
        ${scheme.stages.map((s) => `<span class="swatch" style="background:${s.color}" title="${s.majorName}"></span>`).join('')}
      </div>`).join('')

const miniBlocks = SCHEMES.map((scheme) => `
    <div class="mini-block">
      <div class="mini-title">${scheme.name}</div>
      <div class="mini-grid">${scheme.stages.map((stage) => `
        <div class="mini-row">
          <span class="mini-name" style="--c:${stage.color}">${stage.majorName}</span>
          <span class="mini-cell">${inlineSvg(stage, 16)}</span>
          <span class="mini-cell">${inlineSvg(stage, 20)}</span>
          <span class="mini-cell">${inlineSvg(stage, 24)}</span>
          <span class="mini-cell">${inlineSvg(stage, 32)}</span>
        </div>`).join('')}</div>
    </div>`).join('')

const html = `<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>两套 21 级方案 · 阶段图标设计稿</title>
<style>
  * { box-sizing: border-box; }
  body { margin: 0; padding: 40px 44px 56px; background: #f1f5f9; color: #0f172a;
         font-family: "Microsoft YaHei", "PingFang SC", system-ui, sans-serif; }
  h1 { margin: 0 0 8px; font-size: 26px; letter-spacing: .5px; }
  .lede { margin: 0 0 6px; color: #475569; font-size: 14px; line-height: 1.7; }
  .rule { margin: 0 0 28px; color: #64748b; font-size: 13px; }
  h2 { margin: 0; font-size: 19px; }
  section { margin-bottom: 34px; }
  .ref { background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 20px 22px; }
  .ref h2 { font-size: 15px; color: #334155; margin-bottom: 14px; }
  .ref-row { display: flex; gap: 14px; flex-wrap: wrap; }
  .ref-item { width: 82px; text-align: center; }
  .ref-icon { height: 52px; display: flex; align-items: center; justify-content: center; }
  .ref-name { font-size: 12px; color: #334155; margin-top: 4px; }
  .ref-lv { font-size: 11px; color: #94a3b8; }
  .scheme-head { display: flex; align-items: baseline; gap: 12px; }
  .sub { font-size: 13px; color: #2563eb; background: #dbeafe; border-radius: 999px; padding: 3px 10px; }
  .note { margin: 8px 0 16px; color: #64748b; font-size: 13px; }
  .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
  .card { background: #fff; border: 1px solid #e2e8f0; border-left: 5px solid var(--c);
          border-radius: 14px; padding: 16px 18px 13px; box-shadow: 0 1px 2px rgba(15,23,42,.04); }
  .card-top { display: flex; gap: 14px; align-items: center; }
  .icon { width: 76px; height: 76px; border-radius: 16px; background: var(--soft);
          display: flex; align-items: center; justify-content: center; flex: 0 0 auto; }
  .head h3 { margin: 0 0 4px; font-size: 17px; }
  .meta { font-size: 12px; color: #64748b; line-height: 1.5; }
  .chip { display: inline-block; margin-top: 6px; font-size: 11px; color: var(--ink);
          background: var(--soft); border: 1px solid color-mix(in srgb, var(--c) 35%, #fff);
          border-radius: 999px; padding: 1px 8px; letter-spacing: .4px; }
  .tiers { list-style: none; margin: 12px 0 0; padding: 0; }
  .tiers li { display: flex; align-items: center; gap: 8px; padding: 6px 0;
              border-top: 1px dashed #e2e8f0; font-size: 13px; }
  .lv { width: 42px; color: #94a3b8; font-variant-numeric: tabular-nums; font-size: 12px; }
  .tname { flex: 1; color: #1e293b; font-weight: 600; }
  .pt { width: 58px; text-align: right; color: #64748b; font-variant-numeric: tabular-nums; font-size: 12px; }
  .badge { width: 74px; text-align: center; font-size: 11px; color: var(--ink);
           border: 1px solid color-mix(in srgb, var(--c) 35%, #fff); border-radius: 999px; padding: 1px 0; }
  .tiers li:not(:has(.badge)) .badge { visibility: hidden; }
  .palette { display: flex; align-items: center; gap: 6px; margin-bottom: 12px; }
  .palette-name { font-size: 12px; color: #475569; width: 150px; }
  .swatch { width: 34px; height: 14px; border-radius: 4px; }
  .mini { background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 20px 22px 22px; }
  .mini-block + .mini-block { margin-top: 16px; }
  .mini-title { font-size: 13px; color: #2563eb; margin: 6px 0 8px; }
  .mini-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 26px; }
  .mini-row { display: flex; align-items: center; gap: 14px; padding: 7px 0; border-top: 1px dashed #e2e8f0; }
  .mini-name { width: 104px; font-size: 12px; color: #334155; border-left: 3px solid var(--c); padding-left: 8px; }
  .mini-cell { width: 34px; display: inline-flex; align-items: center; justify-content: center; }
  footer { border-top: 1px solid #e2e8f0; padding-top: 18px; color: #64748b; font-size: 12px; line-height: 1.8; }
  code { background: #e2e8f0; border-radius: 4px; padding: 1px 5px; font-size: 12px; }
</style>
</head>
<body>
  <h1>两套 21 级方案 · 阶段图标设计稿</h1>
  <p class="lede">沿用默认 24 级方案（<code>shared/level-icons.js</code>）的图标语言：64×64 纯几何填充、一个大阶段一枚图标、同一阶段 3 小档共用、颜色即稀有度。</p>
  <p class="rule">因此每套方案 = 7 枚图标；两套合计 14 枚。小档差异由「Lv.序号 + ·观/·悟/·行」承担，不额外加角标，避免头像小尺寸（16px）糊成一片。</p>

  <section class="ref">
    <h2>参考：默认 24 级方案（8 大阶段，可数元素 = 阶段序号）</h2>
    <div class="ref-row">${refRow}</div>
  </section>

  ${sections}

  <section class="mini">
    <div class="scheme-head">
      <h2>小尺寸验证</h2>
      <span class="sub">头像角标实际以 16–20px 呈现</span>
    </div>
    <p class="note">左侧为阶段名，右侧依次为 16 / 20 / 24 / 32px 渲染；16px 仍能分辨图形轮廓即视为通过。</p>
    ${miniBlocks}
  </section>

  <footer>
    <div>${palette}</div>
    设计规则：同一大阶段的三小档共用图标；跨越大阶段时换图形 + 换色；颜色顺序与默认方案一致（绿 → 青 → 蓝 → 靛/紫 → 金 → 红）。<br>
    形状全部为内联 SVG，无外链、无字体依赖，可直接入库到 <code>bbs_svg_asset(category='level')</code> 或写进 <code>bbs_level_item.icon_svg</code>。
  </footer>
</body>
</html>
`

writeFileSync(join(here, 'preview.html'), html, 'utf8')
console.log('generated:', join(here, 'preview.html'))
