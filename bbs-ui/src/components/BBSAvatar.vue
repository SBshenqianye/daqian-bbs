<template>
  <span class="relative inline-flex shrink-0" :style="{ width: px, height: px }">
    <img
      :src="failed ? defaultAvatar : (src || defaultAvatar)"
      class="w-full h-full rounded-full object-cover bg-surface-variant"
      :style="ringStyle"
      alt=""
      @error="onError"
    >
    <!-- 等级角标：全站头像统一展示 -->
    <span
      v-if="showLevel"
      class="absolute -bottom-0.5 -right-0.5 rounded-full bg-white shadow-[0_1px_3px_rgba(0,0,0,0.18)] flex items-center justify-center"
      :style="{ width: badgePx, height: badgePx, padding: badgePad }"
      :title="levelTip"
    >
      <BBSLevelIcon :level-index="resolvedLevel" :size="iconPx" mode="emblem" :icon-svg="iconSvg" :with-title="false" />
    </span>
  </span>
</template>

<script>
import BBSLevelIcon from '@/components/BBSLevelIcon.vue'
import { levelSpec, normalizeLevel } from '../../../shared/level-icons'

const PRESETS = { xs: 24, sm: 28, md: 36, lg: 48, xl: 64, xxl: 96 }

export default {
  name: 'BBSAvatar',
  components: { BBSLevelIcon },
  props: {
    src: { type: String, default: '' },
    /** 数字 px，或 xs/sm/md/lg/xl/xxl */
    size: { type: [Number, String], default: 'sm' },
    levelIndex: { type: [Number, String], default: null },
    iconSvg: { type: String, default: '' },
    showLevel: { type: Boolean, default: true },
    /** 等级环：按等级配色描边，扫一眼能看出层级 */
    showRing: { type: Boolean, default: true },
  },
  data() {
    return { failed: false }
  },
  computed: {
    defaultAvatar() {
      return require('../assets/portrait.png')
    },
    px() {
      const raw = this.size
      const n = typeof raw === 'number' ? raw : PRESETS[raw]
      return (n || 28) + 'px'
    },
    pxNumber() {
      const raw = this.size
      return typeof raw === 'number' ? raw : (PRESETS[raw] || 28)
    },
    resolvedLevel() {
      return normalizeLevel(this.levelIndex)
    },
    spec() {
      return levelSpec(this.resolvedLevel)
    },
    badgePx() {
      return Math.max(13, Math.round(this.pxNumber * 0.44)) + 'px'
    },
    badgePad() {
      return Math.max(1, Math.round(this.pxNumber * 0.02)) + 'px'
    },
    iconPx() {
      return Math.max(10, Math.round(this.pxNumber * 0.34))
    },
    ringStyle() {
      if (!this.showRing || this.levelIndex == null) return {}
      const w = this.pxNumber >= 40 ? 2.5 : 2
      return { boxShadow: `0 0 0 ${w}px ${this.spec.soft}, 0 0 0 ${w + 1}px ${this.spec.color}55` }
    },
    levelTip() {
      return `${this.spec.name} · Lv.${this.resolvedLevel}`
    },
  },
  methods: {
    onError(e) {
      if (this.failed) return
      this.failed = true
      if (e && e.target) e.target.src = this.defaultAvatar
    },
  },
}
</script>
