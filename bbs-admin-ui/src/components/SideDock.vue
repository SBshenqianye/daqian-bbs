<template>
  <!-- 外层 fixed + 阴影 -->
  <div
    ref="dock"
    class="fixed z-[200]"
    :style="outerStyle"
    @mouseenter="$emit('update:expanded', true)"
    @mouseleave="$emit('update:expanded', false)"
  >
    <!-- 内层：overflow-hidden + 圆角 确保 tab 和 content 视觉上是一体的 -->
    <div
      class="overflow-hidden bg-gray-50"
      :style="[slideStyle, radiusStyle]"
    >
      <div class="flex items-stretch" :class="dirClass">
        <!-- 把手 -->
        <div
          class="flex-shrink-0 flex items-center justify-center select-none cursor-grab active:cursor-grabbing"
          :style="tabStyle"
          @mousedown.prevent="startDrag"
        >
          <div class="flex items-center justify-center text-gray-400 gap-1">
            <!-- 拖动把手图标 -->
            <span class="material-symbols-outlined text-gray-400" style="font-size: 16px;">drag_indicator</span>
            <slot name="tab">
              <svg width="14" height="24" viewBox="0 0 14 24" fill="currentColor">
                <rect x="1" y="2" width="2.5" height="2.5" rx="1"/>
                <rect x="6" y="2" width="2.5" height="2.5" rx="1"/>
                <rect x="1" y="7.5" width="2.5" height="2.5" rx="1"/>
                <rect x="6" y="7.5" width="2.5" height="2.5" rx="1"/>
                <rect x="1" y="13" width="2.5" height="2.5" rx="1"/>
                <rect x="6" y="13" width="2.5" height="2.5" rx="1"/>
                <rect x="1" y="18.5" width="2.5" height="2.5" rx="1"/>
                <rect x="6" y="18.5" width="2.5" height="2.5" rx="1"/>
              </svg>
            </slot>
          </div>
        </div>
        <!-- 内容 -->
        <div class="overflow-hidden" :style="contentSize">
          <div class="h-full w-full"><slot /></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const TAB = 48
const RADIUS = 10
const SHADOW = '0 8px 32px rgba(0,0,0,0.15), 0 2px 8px rgba(0,0,0,0.08)'
const TRANSITION = 'transform 0.25s cubic-bezier(0.4, 0, 0.2, 1)'

export default {
  name: 'SideDock',
  props: {
    edge: { type: String, default: 'right' },
    expanded: { type: Boolean, default: false },
    panelSize: { type: Number, default: 280 },
  },
  emits: ['update:edge', 'update:expanded'],
  data() {
    return {
      dragging: false,
      freeX: null,
      freeY: null,
      snappedOffset: null,
      snappedEdge: null,
      _drag: null,
    }
  },
  computed: {
    activeEdge() { return this.snappedEdge || this.edge },
    isHorizontal() { return this.activeEdge === 'right' || this.activeEdge === 'left' },

    /* ---- 外层 fixed 定位 ---- */
    outerStyle() {
      const s = { position: 'fixed', zIndex: 200, filter: 'drop-shadow(' + SHADOW + ')' }
      if (this.dragging) {
        s.left = (this.freeX != null ? this.freeX : 0) + 'px'
        s.top = (this.freeY != null ? this.freeY : 0) + 'px'
        s.transform = 'none'
      } else {
        const e = this.activeEdge
        if (this.snappedOffset != null) {
          if (e === 'right') { s.right = '0'; s.top = this.snappedOffset + 'px' }
          else if (e === 'left') { s.left = '0'; s.top = this.snappedOffset + 'px' }
          else if (e === 'top') { s.top = '0'; s.left = this.snappedOffset + 'px' }
          else if (e === 'bottom') { s.bottom = '0'; s.left = this.snappedOffset + 'px' }
        } else {
          if (e === 'right') { s.right = '0'; s.top = '50%'; s.transform = 'translateY(-50%)' }
          else if (e === 'left') { s.left = '0'; s.top = '50%'; s.transform = 'translateY(-50%)' }
          else if (e === 'top') { s.top = '0'; s.left = '50%'; s.transform = 'translateX(-50%)' }
          else if (e === 'bottom') { s.bottom = '0'; s.left = '50%'; s.transform = 'translateX(-50%)' }
        }
      }
      if (this.isHorizontal) s.height = 'auto'; else s.width = 'auto'
      return s
    },

    /* ---- 内层统一圆角（窗口边缘那一侧不加，视觉上紧贴） ---- */
    radiusStyle() {
      const e = this.activeEdge
      const r = RADIUS
      if (e === 'right') return { borderRadius: r + 'px 0 0 ' + r + 'px' }
      if (e === 'left') return { borderRadius: '0 ' + r + 'px ' + r + 'px 0' }
      if (e === 'top') return { borderRadius: '0 0 ' + r + 'px ' + r + 'px' }
      return { borderRadius: r + 'px ' + r + 'px ' + '0 0' }
    },

    /* ---- 内层 slide 容器 ---- */
    dirClass() {
      const e = this.activeEdge
      return (e === 'right' || e === 'left') ? 'flex-row' : 'flex-col'
    },
    slideStyle() {
      const e = this.activeEdge
      const p = this.panelSize + TAB
      const tx = { transition: this.dragging ? 'none' : TRANSITION }
      // 整体滑入/滑出
      // 为保证展开时 content 紧贴窗口边缘，收起时只露出 tab：
      // 对于 horizontal: 收缩量 = panelSize（content 宽度）
      if (e === 'right') tx.transform = this.expanded ? 'translateX(0)' : 'translateX(' + this.panelSize + 'px)'
      else if (e === 'left') tx.transform = this.expanded ? 'translateX(0)' : 'translateX(-' + this.panelSize + 'px)'
      else if (e === 'top') tx.transform = this.expanded ? 'translateY(0)' : 'translateY(-' + this.panelSize + 'px)'
      else tx.transform = this.expanded ? 'translateY(0)' : 'translateY(' + this.panelSize + 'px)'
      return tx
    },

    /* ---- 把手 ---- */
    tabOrder() {
      const e = this.activeEdge
      return (e === 'right' || e === 'bottom') ? '1' : '2'
    },
    tabStyle() {
      const s = { order: this.tabOrder }
      if (this.isHorizontal) { s.width = TAB + 'px' }
      else { s.height = TAB + 'px'; s.width = '100%' }
      return s
    },

    /* ---- 内容 ---- */
    contentOrder() { return this.tabOrder === '2' ? '1' : '2' },
    contentSize() {
      const s = { order: this.contentOrder }
      if (this.isHorizontal) { s.width = this.panelSize + 'px' }
      else { s.height = this.panelSize + 'px'; s.width = 'auto' }
      return s
    },
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onDrag)
    document.removeEventListener('mouseup', this.stopDrag)
  },
  methods: {
    startDrag(e) {
      const el = this.$refs.dock
      if (!el) return
      const rect = el.getBoundingClientRect()
      this.dragging = true
      this.freeX = rect.left
      this.freeY = rect.top
      this._drag = {
        startX: e.clientX, startY: e.clientY,
        baseX: rect.left, baseY: rect.top,
        w: rect.width, h: rect.height,
      }
      this.$emit('update:expanded', true)
      document.body.style.userSelect = 'none'
      document.body.style.cursor = 'grabbing'
      document.addEventListener('mousemove', this.onDrag)
      document.addEventListener('mouseup', this.stopDrag)
    },
    onDrag(e) {
      if (!this._drag) return
      const d = this._drag
      this.freeX = d.baseX + (e.clientX - d.startX)
      this.freeY = d.baseY + (e.clientY - d.startY)
    },
    stopDrag() {
      if (!this._drag) return
      document.body.style.userSelect = ''
      document.body.style.cursor = ''
      const d = this._drag
      const lx = this.freeX, ly = this.freeY
      const vw = window.innerWidth, vh = window.innerHeight
      const cx = lx + d.w / 2, cy = ly + d.h / 2
      const dists = { right: vw - cx, left: cx, top: cy, bottom: vh - cy }
      const best = Object.entries(dists).reduce((a, b) => a[1] < b[1] ? a : b)[0]
      this.snappedEdge = best
      this.dragging = false
      this.freeX = null
      this.freeY = null
      if (best === 'right' || best === 'left') {
        this.snappedOffset = Math.max(0, Math.min(vh - TAB, ly))
      } else {
        this.snappedOffset = Math.max(0, Math.min(vw - TAB, lx))
      }
      this.$emit('update:edge', best)
      this._drag = null
      document.removeEventListener('mousemove', this.onDrag)
      document.removeEventListener('mouseup', this.stopDrag)
    },
  },
}
</script>
