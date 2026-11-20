<template>
  <span
    class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-[11px] font-medium leading-none whitespace-nowrap"
    :style="{ borderColor: spec.soft, backgroundColor: spec.soft, color: spec.ink || spec.color }"
    :title="tip"
  >
    <BBSLevelIcon
      :level-index="resolvedLevel"
      :size="12"
      mode="emblem"
      :icon-svg="(level && level.iconSvg) || iconSvg || ''"
      :with-title="false"
    />
    <span>{{ label }}</span>
  </span>
</template>

<script>
import BBSLevelIcon from '@/components/BBSLevelIcon.vue'
import { levelSpec, normalizeLevel } from '../../../shared/level-icons'

export default {
  name: 'BBSLevelBadge',
  components: { BBSLevelIcon },
  props: {
    /** 完整 level 对象（levelIndex / levelName / iconKey） */
    level: { type: Object, default: () => ({}) },
    /** 也可直接传等级序号 */
    levelIndex: { type: [Number, String], default: null },
    /** 等级自定义图标 SVG（素材库快照），level 里没有时用它 */
    iconSvg: { type: String, default: '' },
  },
  computed: {
    resolvedLevel() {
      if (this.levelIndex != null && this.levelIndex !== '') return normalizeLevel(this.levelIndex)
      const lv = this.level || {}
      return normalizeLevel(lv.levelIndex || lv.level || 1)
    },
    spec() {
      return levelSpec(this.resolvedLevel)
    },
    label() {
      const lv = this.level || {}
      return lv.levelName || `${this.spec.name}-${this.spec.minor}`
    },
    tip() {
      const lv = this.level || {}
      const pts = lv.totalPoints != null ? ` · 累计 ${lv.totalPoints} 分` : ''
      const badge = lv.badgeName ? ` · 徽章 ${lv.badgeName}` : ''
      return `${this.label}（Lv.${this.resolvedLevel}）${pts}${badge}`
    },
  },
}
</script>
