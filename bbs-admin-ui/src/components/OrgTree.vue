<template>
  <div>
    <!-- loading -->
    <div v-if="loading" class="py-12 text-center text-on-surface-variant flex flex-col items-center gap-2">
      <span class="material-symbols-outlined opacity-50 animate-spin" style="font-size: 36px;">sync</span>
      <p class="text-body-md">加载中...</p>
    </div>
    <!-- empty (when no data at all, not just filtered) -->
    <div v-else-if="!initialized" class="py-12 text-center text-on-surface-variant flex flex-col items-center gap-2">
      <span class="material-symbols-outlined opacity-20" style="font-size: 48px;">account_tree</span>
      <p class="text-body-md">暂无组织数据</p>
    </div>
    <!-- no match -->
    <div v-else-if="filterText && !matchCount" class="py-12 text-center text-on-surface-variant flex flex-col items-center gap-2">
      <span class="material-symbols-outlined opacity-20" style="font-size: 48px;">search_off</span>
      <p class="text-body-md">无匹配单位</p>
    </div>
    <!-- tree -->
    <div v-else :key="'tree-' + filterText" class="select-none" @click="onTreeClick">
      <div v-for="node in flatList" :key="node.id" :data-nid="node.id">
        <div
          class="group flex items-center gap-1 px-3 py-1.5 rounded-lg border mb-0.5 cursor-pointer"
          :class="rowCls(node)"
        >
          <!--
            v-once：仅包含树结构静态部分（箭头、图标、标签），
            toggle 按钮在其外，通过 Vue 响应式直接更新。
          -->
          <div v-once class="flex items-center gap-1 min-w-0 flex-1 w-full">
            <!-- Chevron -->
            <button
              v-if="node._hasChildren"
              class="w-5 h-5 flex items-center justify-center rounded hover:bg-surface-variant transition-colors flex-shrink-0 -ml-0.5"
              @click.stop="onToggle(node)"
            >
              <span class="material-symbols-outlined tree-chevron" style="font-size:14px">chevron_right</span>
            </button>
            <span v-else class="w-5 h-5 flex-shrink-0"></span>

            <!-- Icon -->
            <span class="material-symbols-outlined flex-shrink-0 text-outline" style="font-size: 18px;">{{ node._hasChildren ? 'folder' : 'description' }}</span>

            <!-- Label -->
            <span class="flex-1 font-body-md truncate min-w-0 ml-1">{{ node.label }}</span>
            <span class="material-symbols-outlined text-primary flex-shrink-0" style="font-size: 16px; display:none;">check_circle</span>
          </div>

          <!-- ────── mode=unit-manage：按钮组（v-once 外，Vue 响应式驱动） ────── -->
          <template v-if="mode === 'unit-manage'">
            <div class="flex items-center gap-0.5 flex-shrink-0">
              <!-- 排名开关 -->
              <div class="flex items-center gap-1 ml-1">
                <span class="text-[11px] text-on-surface-variant whitespace-nowrap">排名</span>
                <button
                  class="relative rounded-full transition-all duration-200 flex-shrink-0"
                  :class="rankingBtnCls(node)"
                  style="height:18px;width:36px"
                  :title="rankingTitle(node)"
                  @click.stop="emitToggleRanking(node)"
                >
                  <span
                    class="absolute top-0.5 w-3.5 h-3.5 bg-white rounded-full shadow-sm transition-all duration-200"
                    :style="rankingDotStyle(node)"
                  ></span>
                </button>
              </div>

              <!-- 显示开关 -->
              <div class="flex items-center gap-1 ml-1">
                <span class="text-[11px] text-on-surface-variant whitespace-nowrap">显示</span>
                <button
                  class="relative rounded-full transition-all duration-200 flex-shrink-0"
                  :class="displayBtnCls(node)"
                  style="height:18px;width:36px"
                  :title="displayTitle(node)"
                  @click.stop="emitToggleDisplay(node)"
                >
                  <span
                    class="absolute top-0.5 w-3.5 h-3.5 bg-white rounded-full shadow-sm transition-all duration-200"
                    :style="displayDotStyle(node)"
                  ></span>
                </button>
              </div>

              <!-- CRUD（hover 显示） -->
              <div class="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity duration-150 ml-1">
                <button
                  class="inline-flex items-center gap-0.5 px-1.5 py-1 font-medium text-primary hover:bg-primary/10 rounded-md transition-colors"
                  style="font-size: 11px;"
                  @click.stop="emitAdd(node)"
                >
                  <span class="material-symbols-outlined" style="font-size: 12px;">add</span>
                  新增
                </button>
                <button
                  v-if="node.id && node.id.length !== 5"
                  class="inline-flex items-center gap-0.5 px-1.5 py-1 font-medium text-error hover:bg-error/10 rounded-md transition-colors"
                  style="font-size: 11px;"
                  @click.stop="emitDelete(node)"
                >
                  <span class="material-symbols-outlined" style="font-size: 12px;">delete</span>
                  删除
                </button>
              </div>
            </div>
          </template>

          <!-- 当前组织标签（数据库原值，与新选区分） -->
          <span
            v-if="node.id === currentValueId && node.id !== selectedId"
            class="text-outline/60 ml-1 flex-shrink-0 whitespace-nowrap"
            style="font-size: 10px; line-height: 14px;"
          >当前组织</span>

          <!-- slot：非 unit-manage 模式向后兼容（BBSPointsConfig 等） -->
          <slot v-if="mode !== 'unit-manage' && node._visible" name="node-actions" :node="node" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'OrgTree',
  props: {
    nodes: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    selectedId: { type: String, default: '' },
    /** 数据库中当前保存的组织 ID（与新选中的 selectedId 区分，做黯淡高亮） */
    currentValueId: { type: String, default: '' },
    filterText: { type: String, default: '' },
    indent: { type: Number, default: 24 },
    defaultExpanded: { type: Boolean, default: true },
    /** 渲染模式：'unit-manage' 在 v-once 内联渲染全量按钮；其他值使用 slot */
    mode: { type: String, default: '' }
  },
  emits: ['node-click', 'toggle-ranking', 'toggle-display', 'cascade-ranking', 'cascade-display', 'add-node', 'delete-node'],
  data() {
    return {
      /** 装饰后的树（仅首次构建，后续不重写） */
      treeData: [],
      /** DFS 平铺列表（仅首次构建，后续不动引用）—— 自身是响应式数组，
       *  但内部节点的 _ 属性全部非响应式，修改后须 $forceUpdate() */
      flatList: [],
      initialized: false,
      /** 当前 filter 命中的行数（0 表示无匹配 vs 无数据） */
      matchCount: 0
    }
  },
  watch: {
    nodes: {
      immediate: true,
      handler(val) {
        if (val && val.length) {
          this.treeData = val.map(n => this._decorate(n, null, this.defaultExpanded ? 2 : false, 0))
          this.initialized = true
        } else {
          this.treeData = []
          this.flatList = []
          this.initialized = false
          this.matchCount = 0
          return
        }
        this._buildFlatList()
        this._syncVisibility()
        this.$forceUpdate()
        this.$nextTick(() => this._syncChevrons())
      }
    },
    filterText() {
      this._syncVisibility()
      this.$forceUpdate()
    },
  },
  methods: {
    /* ===================== 节点装饰 ===================== */

    /** 创建节点对象。
     *  @param {number|boolean} expandDepth - false=全折叠, true=全展开, 数字=展开到该深度
     */
    _decorate(node, parent, expandDepth, depth) {
      const hasChildren = !!(node.children && Array.isArray(node.children) && node.children.length)
      const id = node.orgNo != null ? String(node.orgNo) : (node.id != null ? String(node.id) : '')
      const label = node.orgName != null ? node.orgName : (node.label || '')
      // 1. 先复制原始枚举属性（包括 id, label, pOrgNo, isRankingSelected …）
      const out = { ...node, id, label }
      // 2. 内部属性全部非枚举 → Vue 跳过
      const initiallyExpanded = expandDepth === true ? true : expandDepth === false ? false : depth < expandDepth
      const priv = {
        _expanded: initiallyExpanded,
        _depth: depth,
        _hasChildren: hasChildren,
        _visible: false,
        _filterMatch: false,
        _parent: parent
      }
      for (const [k, v] of Object.entries(priv)) {
        Object.defineProperty(out, k, {
          value: v, enumerable: false, writable: true, configurable: true
        })
      }
      // 3. children 也非枚举（不触发 reactive 递归）
      const childArr = hasChildren
        ? node.children.map(c => this._decorate(c, out, expandDepth, depth + 1))
        : []
      Object.defineProperty(out, 'children', {
        value: childArr, enumerable: false, writable: true, configurable: true
      })
      return out
    },

    /** DFS 平铺全部节点（只调用一次，之后只读不写引用） */
    _buildFlatList() {
      const list = []
      const stack = this.treeData.length ? [...this.treeData].reverse() : []
      while (stack.length) {
        const n = stack.pop()
        list.push(n)
        if (n._hasChildren && n.children) {
          for (let i = n.children.length - 1; i >= 0; i--) {
            stack.push(n.children[i])
          }
        }
      }
      this.flatList = list
    },

    /* ===================== 可见性同步 ===================== */

    _syncVisibility() {
      const ft = (this.filterText || '').trim().toLowerCase()
      if (ft) this._syncFiltered(ft)
      else this._syncExpanded()
    },

    _syncExpanded() {
      let count = 0
      for (let i = 0; i < this.flatList.length; i++) {
        const n = this.flatList[i]
        if (n._depth === 0) {
          n._visible = true; count++
        } else {
          n._visible = n._parent._visible && n._parent._expanded
          if (n._visible) count++
        }
      }
      this.matchCount = count
    },

    _syncFiltered(ft) {
      this._markFilterMatch(this.treeData, ft)
      let count = 0
      for (let i = 0; i < this.flatList.length; i++) {
        const n = this.flatList[i]
        if (n._depth === 0) {
          n._visible = n._filterMatch
        } else {
          n._visible = n._parent._visible && n._parent._expanded && n._filterMatch
        }
        if (n._visible) count++
      }
      this.matchCount = count
    },

    _markFilterMatch(nodes, ft) {
      let hit = false
      for (const n of nodes) {
        const label = n.orgName != null ? n.orgName : (n.label || '')
        const labelMatch = label.toLowerCase().includes(ft)
        const childMatch = n.children && n.children.length
          ? this._markFilterMatch(n.children, ft)
          : false
        n._filterMatch = labelMatch || childMatch
        if (n._filterMatch) hit = true
      }
      return hit
    },

    /** 从 startNode 向下传播 _visible 状态（基于 _expanded） */
    _propagateVisibility(node) {
      if (!node._hasChildren || !node.children) return
      for (const child of node.children) {
        child._visible = child._parent._visible && child._parent._expanded
        if (child._hasChildren) this._propagateVisibility(child)
      }
    },

    /** toggle/expand/collapse 后重算可见性 + 强制重绘 */
    _updateAndRender() {
      if (!(this.filterText || '').trim()) {
        let count = 0
        for (let i = 0; i < this.flatList.length; i++) {
          if (this.flatList[i]._visible) count++
        }
        this.matchCount = count
      } else {
        this._syncFiltered((this.filterText || '').trim().toLowerCase())
      }
      this.$forceUpdate()
    },

    /* ===================== 公开方法 ===================== */

    toggleNode(node) {
      if (!node._hasChildren) return
      node._expanded = !node._expanded
      // 直接操作 DOM 旋转 chevron（v-once 冻结了模板，但 DOM 操作不受影响）
      const row = this.$el.querySelector(`[data-nid="${node.id}"]`)
      if (row) {
        const chevron = row.querySelector('.tree-chevron')
        if (chevron) chevron.classList.toggle('tree-copen', node._expanded)
      }
      if (!(this.filterText || '').trim()) this._propagateVisibility(node)
      this._updateAndRender()
    },

    expandAll() {
      this._walkTree(this.treeData, n => { n._expanded = true })
      this._syncVisibility()
      this.$forceUpdate()
      this.$nextTick(() => this._syncChevrons())
    },

    collapseAll() {
      this._walkTree(this.treeData, n => { n._expanded = false })
      this._syncVisibility()
      this.$forceUpdate()
      this.$nextTick(() => this._syncChevrons())
    },

    /** 展开到指定节点（向上展开所有祖先），确保节点可见。
     *  返回 true 如果找到并展开，false 如果未找到。 */
    expandToNode(id) {
      const findNode = (nodes, targetId) => {
        for (const n of nodes) {
          if (n.id === targetId) return n
          if (n.children && n.children.length) {
            const found = findNode(n.children, targetId)
            if (found) return found
          }
        }
        return null
      }
      const node = findNode(this.treeData, id)
      if (!node) return false
      // 向上展开所有祖先
      let current = node
      while (current._parent) {
        current = current._parent
        if (!current._expanded) current._expanded = true
      }
      this._syncVisibility()
      this.$forceUpdate()
      this.$nextTick(() => this._syncChevrons())
      return true
    },

    /** 按 ID toggle 节点的展开/折叠状态 */
    toggleNodeById(id) {
      const findNode = (nodes, targetId) => {
        for (const n of nodes) {
          if (n.id === targetId) return n
          if (n.children && n.children.length) {
            const found = findNode(n.children, targetId)
            if (found) return found
          }
        }
        return null
      }
      const node = findNode(this.treeData, id)
      if (!node || !node._hasChildren) return false
      this.toggleNode(node)
      return true
    },

    /** 同步所有节点的 chevron 旋转状态（v-once 冻结了模板，须 DOM 操作） */
    _syncChevrons() {
      if (!this.$el || !this.flatList) return
      for (const node of this.flatList) {
        if (!node._hasChildren) continue
        const row = this.$el.querySelector(`[data-nid="${node.id}"]`)
        if (!row) continue
        const chevron = row.querySelector('.tree-chevron')
        if (chevron) chevron.classList.toggle('tree-copen', node._expanded)
      }
    },

    /* ============= 响应式：toggle 视觉状态 ============= */

    /** 排名按钮背景 class */
    rankingBtnCls(node) {
      const on = node.isRankingSelected == 1 || node.isRankingSelected === true
      return on ? 'bg-primary' : 'bg-gray-300'
    },
    /** 排名按钮圆点位置 */
    rankingDotStyle(node) {
      const on = node.isRankingSelected == 1 || node.isRankingSelected === true
      return on ? 'left:19px' : 'left:2px'
    },
    /** 排名按钮 title */
    rankingTitle(node) {
      const on = node.isRankingSelected == 1 || node.isRankingSelected === true
      return on ? '取消参与排名' : '参与排名'
    },

    /** 显示按钮背景 class */
    displayBtnCls(node) {
      const on = node.isDisplaySelected == 1 || node.isDisplaySelected === true
      return on ? 'bg-primary' : 'bg-gray-300'
    },
    /** 显示按钮圆点位置 */
    displayDotStyle(node) {
      const on = node.isDisplaySelected == 1 || node.isDisplaySelected === true
      return on ? 'left:19px' : 'left:2px'
    },
    /** 显示按钮 title */
    displayTitle(node) {
      const on = node.isDisplaySelected == 1 || node.isDisplaySelected === true
      return on ? '不在前台显示' : '在前台显示'
    },

    /* ============= 事件发射 ============= */

    emitToggleRanking(node) {
      if (node._hasChildren) {
        // 父级：翻转自己并向下级联到所有子级
        const newVal = !(node.isRankingSelected == 1 || node.isRankingSelected === true)
        this.$emit('toggle-ranking', node)
        this.$emit('cascade-ranking', node, newVal)
        this.$forceUpdate()
        return
      }
      this.$emit('toggle-ranking', node)
      this.$forceUpdate()
    },

    emitToggleDisplay(node) {
      if (node._hasChildren) {
        const newVal = !(node.isDisplaySelected == 1 || node.isDisplaySelected === true)
        this.$emit('toggle-display', node)
        this.$emit('cascade-display', node, newVal)
        this.$forceUpdate()
        return
      }
      this.$emit('toggle-display', node)
      this.$forceUpdate()
    },

    /* ============= 公共方法 ============= */

    emitAdd(node) {
      this.$emit('add-node', node)
    },
    emitDelete(node) {
      this.$emit('delete-node', node)
    },

    /* ===================== 内部 ===================== */

    _walkTree(nodes, fn) {
      if (!nodes || !Array.isArray(nodes)) return
      for (const n of nodes) {
        fn(n)
        if (n.children && n.children.length) this._walkTree(n.children, fn)
      }
    },

    /** 单次 class diff 替代 :style 对象对比 —— 快 10×+ */
    rowCls(node) {
      const c = []
      if (!node._visible) c.push('th')
      c.push('d' + Math.min(node._depth, 6))
      if (node.id === this.selectedId) {
        // ⭐ 新选中的组织（明亮）
        c.push('bg-primary/15 border-primary text-primary font-semibold')
      } else if (node.id === this.currentValueId) {
        // ⭐ 数据库中当前保存的组织（黯淡 + 灰色 "当前组织" 标签）
        c.push('bg-surface-variant/30 border-outline-variant/40 text-on-surface-variant/60')
      } else {
        c.push('bg-surface-container-low border-transparent hover:border-outline-variant/30 hover:bg-surface-container-low/80')
      }
      return c
    },

    /** 事件委托：点击行触发 node-click */
    onTreeClick(e) {
      const row = e.target.closest('[data-nid]')
      if (!row) return
      for (let i = 0; i < this.flatList.length; i++) {
        if (this.flatList[i].id === row.dataset.nid) {
          this.$emit('node-click', this.flatList[i])
          return
        }
      }
    },

    onToggle(node) {
      this.toggleNode(node)
    }
  }
}
</script>

<style>
/* 每行作为独立布局/样式/绘制容器，避免展开时触发全量 layout 回溯 */
.group { contain: layout style paint; }
/* 隐藏（替代 v-show / :style display）
 * 使用 .group.th 提升特异性，确保始终覆盖 Tailwind 的 .flex { display:flex }，
 * 避免 CSS 加载顺序不同导致 .flex 胜出产生"幽灵"可见行。 */
.group.th { display: none !important; }
/* 缩进层级（CSS class 比 :style 的 js 对象 diff 快得多） */
.d0 { margin-left: 0; }
.d1 { margin-left: 24px; }
.d2 { margin-left: 48px; }
.d3 { margin-left: 72px; }
.d4 { margin-left: 96px; }
.d5 { margin-left: 120px; }
.d6 { margin-left: 144px; }
/* 展开箭头旋转（替代 :style transform） */
.tree-chevron { transition: transform .12s ease; }
.tree-copen { transform: rotate(90deg); }
</style>
