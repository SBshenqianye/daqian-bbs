<template>
  <span class="bbs-level-svg inline-flex items-center shrink-0" :style="{ height: size + 'px' }" :title="tip" v-html="svg"></span>
</template>

<script>
import { levelRowSvg, levelEmblemSvg, levelSpec, glyphSvg } from '../../../shared/level-icons'
import { safeIconSvg, sizeSvg } from '../../../shared/icon-fallback'

export default {
  name: 'BBSLevelIcon',
  props: {
    /** 等级序号 1..24 */
    levelIndex: { type: [Number, String], default: 1 },
    /** 高度（px），宽度按比例自适应 */
    size: { type: [Number, String], default: 16 },
    /** row：完整图标串；emblem：单枚徽记（头像角标用） */
    mode: { type: String, default: 'row' },
    /** 自定义图标（优先于自动生成） */
    iconSvg: { type: String, default: '' },
    /** 自定义图形（star/moon/sun/crown/crownGem） */
    glyph: { type: String, default: '' },
    /** 自定义主色 */
    color: { type: String, default: '' },
    withTitle: { type: Boolean, default: true },
  },
  computed: {
    svg() {
      const px = Number(this.size) || 16
      if (this.iconSvg && this.iconSvg.trim()) return sizeSvg(safeIconSvg(this.iconSvg), px)
      if (this.glyph) return glyphSvg(this.glyph, this.color || undefined)
      return this.mode === 'emblem'
        ? levelEmblemSvg(this.levelIndex, px)
        : levelRowSvg(this.levelIndex, px)
    },
    tip() {
      if (!this.withTitle) return null
      const spec = levelSpec(this.levelIndex)
      return `${spec.name} · Lv.${spec.level}`
    },
  },
}
</script>
