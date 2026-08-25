<template>
  <div v-show="visible" class="fixed inset-0 flex items-center justify-center p-4" style="z-index: 100;">
    <!-- backdrop -->
    <div class="fixed inset-0 bg-black/30" @click="$emit('close')"></div>
    <!-- dialog card -->
    <div ref="dialogCard" class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl flex flex-col" style="max-height: 80vh; transform: translate3d(0,0,0); transition: none;">
      <!-- drag handle header -->
      <div class="flex items-center justify-between p-5 border-b border-outline-variant select-none" @mousedown.prevent="startDrag">
        <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">corporate_fare</span>
          选择单位
        </h3>
        <button class="text-outline hover:text-error transition-colors" @click="$emit('close')">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <!-- search + toolbar -->
      <div class="px-5 pt-4 pb-2">
        <div class="grid grid-cols-1 grid-rows-1">
          <input
            v-model="filterText"
            class="w-full col-start-1 row-start-1 pl-9 pr-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md"
            placeholder="搜索单位名称"
          >
          <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline pointer-events-none" style="font-size: 18px;">search</span>
        </div>
        <div v-if="treeData.length" class="flex gap-2 mt-2">
          <button
            class="inline-flex items-center gap-1 px-2.5 py-1 font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
            style="font-size: 11px;"
            @click="$refs.orgTree.expandAll()"
          >
            <span class="material-symbols-outlined" style="font-size: 13px;">unfold_more</span>
            全部展开
          </button>
          <button
            class="inline-flex items-center gap-1 px-2.5 py-1 font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
            style="font-size: 11px;"
            @click="$refs.orgTree.collapseAll()"
          >
            <span class="material-symbols-outlined" style="font-size: 13px;">unfold_less</span>
            全部折叠
          </button>
        </div>
      </div>

      <!-- tree body -->
      <div ref="scrollContainer" class="px-4 pb-4 overflow-y-auto flex-1 relative" @scroll="onTreeScroll">
        <!-- ────── 浮动指示器：新选中（蓝色） ────── -->
        <div v-if="showScrollUp" class="sticky top-0 flex justify-center pointer-events-none" style="margin-bottom: -30px; z-index: 2;">
          <button class="pointer-events-auto inline-flex items-center gap-1 px-3 py-1.5 bg-primary text-on-primary rounded-full shadow-lg hover:opacity-90 transition-opacity cursor-pointer" style="font-size: 11px; font-weight: 500;" @click.stop="scrollToSelected">
            <span class="material-symbols-outlined" style="font-size: 13px;">arrow_upward</span>已选
          </button>
        </div>
        <!-- 浮动指示器：当前值（灰色，在已选下方） -->
        <div v-if="showCurrentUp" class="sticky flex justify-center pointer-events-none" :style="'z-index: 1; margin-bottom: -30px; ' + (showScrollUp ? 'top: 38px;' : 'top: 0;')">
          <button class="pointer-events-auto inline-flex items-center gap-1 px-3 py-1.5 bg-surface-variant text-on-surface-variant rounded-full shadow-lg hover:opacity-90 transition-opacity cursor-pointer" style="font-size: 11px; font-weight: 500;" @click.stop="scrollToCurrent">
            <span class="material-symbols-outlined" style="font-size: 13px;">arrow_upward</span>当前
          </button>
        </div>

        <OrgTree
          ref="orgTree"
          :nodes="treeData"
          :loading="loading"
          :selected-id="localSelectedId"
          :current-value-id="selectedId"
          :filter-text="filterTextLocal"
          @node-click="onNodeClick"
        />

        <!-- ────── 浮动指示器：新选中（蓝色） ────── -->
        <div v-if="showScrollDown" class="sticky bottom-0 flex justify-center pointer-events-none" style="margin-top: -30px; z-index: 2;">
          <button class="pointer-events-auto inline-flex items-center gap-1 px-3 py-1.5 bg-primary text-on-primary rounded-full shadow-lg hover:opacity-90 transition-opacity cursor-pointer" style="font-size: 11px; font-weight: 500;" @click.stop="scrollToSelected">
            已选<span class="material-symbols-outlined" style="font-size: 13px;">arrow_downward</span>
          </button>
        </div>
        <!-- 浮动指示器：当前值（灰色，在已选上方） -->
        <div v-if="showCurrentDown" class="sticky flex justify-center pointer-events-none" :style="'z-index: 1; margin-top: -30px; ' + (showScrollDown ? 'bottom: 38px;' : 'bottom: 0;')">
          <button class="pointer-events-auto inline-flex items-center gap-1 px-3 py-1.5 bg-surface-variant text-on-surface-variant rounded-full shadow-lg hover:opacity-90 transition-opacity cursor-pointer" style="font-size: 11px; font-weight: 500;" @click.stop="scrollToCurrent">
            当前<span class="material-symbols-outlined" style="font-size: 13px;">arrow_downward</span>
          </button>
        </div>
      </div>

      <!-- footer -->
      <div class="flex justify-between items-center px-5 py-4 border-t border-outline-variant bg-surface-container-lowest">
        <span class="text-body-md text-on-surface-variant truncate min-w-0" :title="localSelectedPath || localSelectedLabel">
          <template v-if="localSelectedPath && localSelectedPath !== localSelectedLabel">
            <span class="text-body-sm text-outline block truncate">{{ localSelectedPath }}</span>
            <span class="text-on-surface">{{ localSelectedLabel }}</span>
          </template>
          <template v-else>{{ localSelectedLabel || '未选择' }}</template>
        </span>
        <div class="flex gap-3 flex-shrink-0 ml-3">
          <button
            class="px-5 py-2 border border-outline rounded-lg text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md"
            @click="$emit('close')"
          >取消</button>
          <button
            class="px-7 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md shadow-sm"
            :disabled="!localSelectedId"
            @click="confirm"
          >确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import OrgTree from './OrgTree.vue'

export default {
  name: 'OrgTreePicker',
  components: { OrgTree },
  props: {
    visible: { type: Boolean, default: false },
    selectedId: { type: String, default: '' },
    selectedLabel: { type: String, default: '' }
  },
  data() {
    return {
      loading: false,
      treeData: [],
      filterText: '',
      localSelectedId: '',
      localSelectedLabel: '',
      localSelectedPath: '',
      showScrollUp: false,
      showScrollDown: false,
      showCurrentUp: false,
      showCurrentDown: false,
      _scrollCheckPending: false,
      dragState: null
    }
  },
  computed: {
    /** Pass to OrgTree only when non-empty, so the tree stays
     *  interactive (expandable) when search is cleared. */
    filterTextLocal() {
      const ft = (this.filterText || '').trim()
      return ft || undefined
    }
  },
  watch: {
    filterText() {
      // 搜索条件变化时，清除浮动指示器，防止"幽灵"残留
      this.showScrollUp = false
      this.showScrollDown = false
      this.showCurrentUp = false
      this.showCurrentDown = false
    },
    visible(val) {
      if (val) {
        this.localSelectedId = this.selectedId || ''
        this.localSelectedLabel = this.selectedLabel || ''
        this.localSelectedPath = ''
        this.showScrollUp = false
        this.showScrollDown = false
        this.showCurrentUp = false
        this.showCurrentDown = false
        this.filterText = ''

        const afterDataReady = () => {
          this.$nextTick(() => {
            // 计算初始选中节点的完整路径
            if (this.localSelectedId && this.treeData.length) {
              this.localSelectedPath = this._findPathInTree(this.treeData, this.localSelectedId)
            }
            // 自动展开并滚动到选中节点
            if (this.localSelectedId) this.scrollToSelected()
          })
        }

        if (!this.treeData.length) {
          this.loadTree().then(afterDataReady).catch(() => {})
        } else {
          afterDataReady()
        }
      }
    }
  },
  methods: {
    async loadTree() {
      this.loading = true
      try {
        const res = await this.getRequestUrl('/common/saOrgTree')
        if (res && res.obj) {
          this.treeData = res.obj
        }
      } catch (e) { this.treeData = [] }
      this.loading = false
    },

    onNodeClick(node) {
      this.localSelectedId = node.id
      this.localSelectedLabel = node.label
      this.localSelectedPath = this._getNodePath(node)
      // 用户刚点击了节点，它必然在视口内，无需调用 checkSelectedVisibility
    },

    confirm() {
      if (this.localSelectedId) {
        this.$emit('select', {
          id: this.localSelectedId,
          label: this.localSelectedLabel,
          path: this.localSelectedPath || this.localSelectedLabel
        })
      }
    },

    /* ===================== 完整路径计算 ===================== */

    /** 从已装饰节点的 _parent 链向上拼接完整路径（节点点击时用），叶子在前 */
    _getNodePath(node) {
      const segments = [node.label]
      let current = node
      while (current._parent) {
        current = current._parent
        segments.push(current.label)
      }
      return segments.join(' / ')
    },

    /** 递归搜索原始 treeData 拼接路径（打开弹窗已有选中时用） */
    _findPathInTree(nodes, targetId, pathSoFar) {
      pathSoFar = pathSoFar || []
      for (const node of nodes) {
        const nodeId = node.orgNo != null ? String(node.orgNo) : (node.id != null ? String(node.id) : '')
        const nodeLabel = node.orgName || node.label || ''
        const currentPath = [...pathSoFar, nodeLabel]
        if (nodeId === targetId) {
          return currentPath.reverse().join(' / ')
        }
        if (node.children && node.children.length) {
          const found = this._findPathInTree(node.children, targetId, currentPath)
          if (found) return found
        }
      }
      return ''
    },

    /* ===================== 滚动指示器 ===================== */

    /** 展开树并滚动到新选中的节点 */
    scrollToSelected() {
      this._scrollToNode(this.localSelectedId, 'showScroll')
    },

    /** 展开树并滚动到当前值（数据库原值）节点 */
    scrollToCurrent() {
      this._scrollToNode(this.selectedId, 'showCurrent')
    },

    /** 展开树并滚动到指定节点，更新对应的 show* 状态 */
    _scrollToNode(nodeId, prefix) {
      const container = this.$refs.scrollContainer
      if (!container || !nodeId) return
      if (this.$refs.orgTree) {
        this.$refs.orgTree.expandToNode(nodeId)
      }
      this.$nextTick(() => {
        const row = container.querySelector(`[data-nid="${nodeId}"]`)
        if (row) {
          row.scrollIntoView({ block: 'center', behavior: 'smooth' })
          setTimeout(() => { if (this._isDestroyed) return; this.checkSelectedVisibility() }, 400)
        } else {
          this[prefix + 'Up'] = false
          this[prefix + 'Down'] = false
        }
      })
    },

    /** 检查选中节点和当前值节点是否在当前可视区域内 */
    checkSelectedVisibility() {
      if (this._isDestroyed) return
      if (!this._checkNodeVisibility(this.localSelectedId, 'showScroll')) {
        this.showScrollUp = false
        this.showScrollDown = false
      }
      // 当前值节点（仅当不同于新选时检测）
      if (this.selectedId && this.selectedId !== this.localSelectedId) {
        this._checkNodeVisibility(this.selectedId, 'showCurrent')
      } else {
        this.showCurrentUp = false
        this.showCurrentDown = false
      }
    },

    /** 检查指定节点在滚动容器内的可见性，更新对应的 show* 状态。
     *  prefix — 'showScroll'（新选）/ 'showCurrent'（当前值）
     *  返回 true 表示节点存在且已检查，false 表示未找到或无意义。 */
    _checkNodeVisibility(nodeId, prefix) {
      const container = this.$refs.scrollContainer
      if (!container || !nodeId) return false
      const row = container.querySelector(`[data-nid="${nodeId}"]`)
      if (!row) return false
      const cr = container.getBoundingClientRect()
      const rr = row.getBoundingClientRect()
      const tolerance = 8
      this[prefix + 'Up'] = rr.top < cr.top + tolerance
      this[prefix + 'Down'] = rr.bottom > cr.bottom - tolerance
      return true
    },

    /** 滚动事件处理（rAF 节流） */
    onTreeScroll() {
      if (this._scrollCheckPending) return
      this._scrollCheckPending = true
      requestAnimationFrame(() => {
        this.checkSelectedVisibility()
        this._scrollCheckPending = false
      })
    },

    /* ===================== 结束 滚动指示器 ===================== */

    // ====== drag ======
    startDrag(e) {
      const el = this.$refs.dialogCard
      if (!el) return
      const cur = el.style.transform
      let ox = 0, oy = 0
      const m = cur.match(/translate3?d?\(([-\d.]+)px,?\s*([-\d.]+)px/)
      if (m) { ox = parseFloat(m[1]); oy = parseFloat(m[2]) }
      this.dragState = { el, startX: e.clientX, startY: e.clientY, origX: ox, origY: oy }
      document.body.style.userSelect = 'none'
      document.body.style.cursor = 'grabbing'
      // 保存绑定后的 handler 引用，确保 beforeDestroy 能正常移除
      this._onDragHandler = (ev) => this.onDrag(ev)
      this._stopDragHandler = (ev) => this.stopDrag(ev)
      document.addEventListener('mousemove', this._onDragHandler)
      document.addEventListener('mouseup', this._stopDragHandler)
      this._dragListenersAttached = true
    },
    onDrag(e) {
      if (!this.dragState || !this.dragState.el) return
      const d = this.dragState
      d.el.style.transform = 'translate3d(' + (d.origX + e.clientX - d.startX) + 'px,' + (d.origY + e.clientY - d.startY) + 'px,0)'
    },
    stopDrag() {
      if (!this.dragState) return
      document.body.style.userSelect = ''
      document.body.style.cursor = ''
      this.dragState = null
      document.removeEventListener('mousemove', this._onDragHandler)
      document.removeEventListener('mouseup', this._stopDragHandler)
      this._dragListenersAttached = false
    }
  },
  beforeDestroy() {
    if (this._dragListenersAttached) {
      document.removeEventListener('mousemove', this._onDragHandler)
      document.removeEventListener('mouseup', this._stopDragHandler)
      this._dragListenersAttached = false
    }
  }
}
</script>
