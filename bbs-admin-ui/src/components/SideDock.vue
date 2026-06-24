<template>
  <!-- 外层容器：始终 fixed 定位 -->
  <div
    ref="dock"
    class="fixed z-[200]"
    :style="outerStyle"
    @mouseenter="hovering = true; $emit('update:expanded', true)"
    @mouseleave="hovering = false; $emit('update:expanded', false)"
  >
    <!-- 内层：由 order 控制 tab/content 排列 -->
    <div
      class="flex"
      :class="innerDir"
      :style="innerStyle"
    >
      <!-- tab 把手 -->
      <div
        ref="tabEl"
        class="flex-shrink-0 flex items-center justify-center select-none cursor-grab active:cursor-grabbing"
        :class="tabClasses"
        :style="tabSizeStyle"
        @mousedown.prevent="startDrag"
      >
        <slot name="tab">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="currentColor">
            <circle cx="7" cy="2.5" r="1.3"/>
            <circle cx="7" cy="7" r="1.3"/>
            <circle cx="7" cy="11.5" r="1.3"/>
          </svg>
        </slot>
      </div>
      <!-- 内容面板 -->
      <div
        class="bg-container overflow-hidden"
        :class="contentBorder"
        :style="contentSizeStyle"
      >
        <div class="h-full w-full"><slot /></div>
      </div>
    </div>
  </div>
</template>

<script>
const TAB = 36
const TRANSITION = 'transform 0.25s cubic-bezier(0.4, 0, 0.2, 1)'

export default {
  name: 'SideDock',
  props: {
    edge: { type: String, default: 'right' },
    expanded: { type: Boolean, default: false },
    panelSize: { type: Number, default: 260 },
  },
  emits: ['update:edge', 'update:expanded'],
  data() {
    return {
      hovering: false,
      // 拖拽状态（null = 未拖拽）
      dragging: false,
      // 拖拽中自由定位
      freeX: null,
      freeY: null,
      // 拖拽结束吸附后的偏移（仅单轴: right/left→Y, top/bottom→X）
      snappedOffset: null,
      // 当前吸附到的 edge（覆盖 prop）
      snappedEdge: null,
      // 拖拽起始记录
      _drag: null,
    }
  },
  computed: {
    /** 实际 edge */
    activeEdge() { return this.snappedEdge || this.edge },
    isHorizontal() { return this.activeEdge === 'right' || this.activeEdge === 'left' },

    /* ---------- 外层 fixed 定位 ---------- */
    outerStyle() {
      const s = { position: 'fixed', zIndex: 200, willChange: 'transform' }

      if (this.dragging) {
        // 拖拽中：自由定位，不吸附
        s.left = (this.freeX != null ? this.freeX : 0) + 'px'
        s.top = (this.freeY != null ? this.freeY : 0) + 'px'
        s.transform = 'none'
      } else {
        const e = this.activeEdge
        if (this.snappedOffset !== null) {
          // 已吸附：用像素偏移
          if (e === 'right') { s.right = '0'; s.top = this.snappedOffset + 'px' }
          else if (e === 'left') { s.left = '0'; s.top = this.snappedOffset + 'px' }
          else if (e === 'top') { s.top = '0'; s.left = this.snappedOffset + 'px' }
          else if (e === 'bottom') { s.bottom = '0'; s.left = this.snappedOffset + 'px' }
        } else {
          // 默认居中
          if (e === 'right') { s.right = '0'; s.top = '50%'; s.transform = 'translateY(-50%)' }
          else if (e === 'left') { s.left = '0'; s.top = '50%'; s.transform = 'translateY(-50%)' }
          else if (e === 'top') { s.top = '0'; s.left = '50%'; s.transform = 'translateX(-50%)' }
          else if (e === 'bottom') { s.bottom = '0'; s.left = '50%'; s.transform = 'translateX(-50%)' }
        }
      }

      if (this.isHorizontal) s.height = 'auto'; else s.width = 'auto'
      return s
    },

    /* ---------- 内层排列 ---------- */
    innerDir() {
      const e = this.activeEdge
      return (e === 'right' || e === 'left') ? 'flex-row' : 'flex-col'
    },

    tabOrder() {
      const e = this.activeEdge
      // right/bottom → tab 在末尾（内容在前）
      return (e === 'right' || e === 'bottom') ? '2' : '1'
    },
    contentOrder() { return this.tabOrder === '2' ? '1' : '2' },

    innerStyle() {
      const e = this.activeEdge
      const tx = { transition: this.dragging ? 'none' : TRANSITION, display: 'flex' }
      const p = this.panelSize

      if (e === 'right') tx.transform = this.expanded ? 'translateX(0)' : `translateX(${p}px)`
      else if (e === 'left') tx.transform = this.expanded ? 'translateX(0)' : `translateX(-${p}px)`
      else if (e === 'top') tx.transform = this.expanded ? 'translateY(0)' : `translateY(-${p}px)`
      else tx.transform = this.expanded ? 'translateY(0)' : `translateY(${p}px)`

      return tx
    },

    /* ---------- tab ---------- */
    tabClasses() {
      const e = this.activeEdge
      return {
        'bg-primary text-on-primary': true,
        'rounded-l-lg': e === 'right',
        'rounded-r-lg': e === 'left',
        'rounded-b-lg': e === 'top',
        'rounded-t-lg': e === 'bottom',
      }
    },
    tabSizeStyle() {
      const s = { order: this.tabOrder }
      if (this.isHorizontal) { s.width = TAB + 'px'; s.height = '100%' }
      else { s.height = TAB + 'px'; s.width = '100%' }
      return s
    },

    /* ---------- content ---------- */
    contentBorder() {
      const e = this.activeEdge
      const base = 'border-outline-variant'
      // content 紧贴 tab 的那一侧加边框
      const dir = {
        right: 'border-r',  // [content][tab] → content 右边缘贴 tab
        left: 'border-l',   // [tab][content] → content 左边缘贴 tab
        top: 'border-b',    // [tab] / [content] → content 上边缘贴 tab（垂直排列）
        bottom: 'border-t', // [content] / [tab] → content 下边缘贴 tab
      }
      return { [dir[e]]: true, [base]: true }
    },
    contentSizeStyle() {
      const s = { order: this.contentOrder }
      if (this.isHorizontal) { s.width = this.panelSize + 'px'; s.height = '100%' }
      else { s.height = this.panelSize + 'px'; s.width = '100%' }
      return s
    },
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onDrag)
    document.removeEventListener('mouseup', this.stopDrag)
  },
  methods: {
    /* ======== 拖拽 ======== */
    startDrag(e) {
      const el = this.$refs.dock
      if (!el) return
      const rect = el.getBoundingClientRect()

      // 拖拽中自由定位（覆盖默认 edge 定位）
      this.dragging = true
      this.freeX = rect.left
      this.freeY = rect.top

      this._drag = {
        startX: e.clientX,
        startY: e.clientY,
        baseX: rect.left,
        baseY: rect.top,
        w: rect.width,
        h: rect.height,
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
      const lastX = this.freeX, lastY = this.freeY
      const vw = window.innerWidth, vh = window.innerHeight
      const cx = lastX + d.w / 2
      const cy = lastY + d.h / 2

      // 计算最近边缘
      const dists = { right: vw - cx, left: cx, top: cy, bottom: vh - cy }
      const best = Object.entries(dists).reduce((a, b) => a[1] < b[1] ? a : b)[0]

      // 吸附
      this.snappedEdge = best
      this.dragging = false
      this.freeX = null
      this.freeY = null

      // 计算吸附后的单轴偏移
      if (best === 'right' || best === 'left') {
        this.snappedOffset = Math.max(0, Math.min(vh - TAB, lastY))
      } else {
        this.snappedOffset = Math.max(0, Math.min(vw - TAB, lastX))
      }

      this.$emit('update:edge', best)
      this._drag = null
      document.removeEventListener('mousemove', this.onDrag)
      document.removeEventListener('mouseup', this.stopDrag)
    },
  },
}
</script>
