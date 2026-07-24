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
    <div v-else class="select-none" @click="onTreeClick">
      <div v-for="node in flatList" :key="node.id" :data-nid="node.id">
        <div
          class="group flex items-center gap-1 px-3 py-1.5 rounded-lg border mb-0.5 cursor-pointer"
          :class="rowCls(node)"
        >
          <!--
            v-once：整个行内容（静态结构 + mode=unit-manage 按钮）只创建一次 VNode，
            后续 $forceUpdate 完全跳过。chevron 旋转/高亮和 toggle 开关状态通过
            直接 DOM 操作更新。
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

            <!-- ────── mode=unit-manage：按钮组（全量） ────── -->
            <template v-if="mode === 'unit-manage'">
              <div class="flex items-center gap-0.5 flex-shrink-0">
                <!-- 级联排名操作（仅父节点） -->
                <template v-if="node._hasChildren">
                  <button
                    class="w-6 h-6 flex items-center justify-center rounded text-outline hover:text-primary hover:bg-primary/10 transition-all"
                    title="勾选所有子级单位参与排名"
                    @click.stop="emitCascadeRanking(node, true)"
                  >
                    <span class="material-symbols-outlined" style="font-size:14px">done_all</span>
                  </button>
                  <button
                    class="w-6 h-6 flex items-center justify-center rounded text-outline hover:text-error hover:bg-error/10 transition-all"
                    title="取消所有子级单位排名"
                    @click.stop="emitCascadeRanking(node, false)"
                  >
                    <span class="material-symbols-outlined" style="font-size:14px">indeterminate_check_box</span>
                  </button>
                </template>

                <!-- 排名开关 -->
                <div class="flex items-center gap-1 ml-1">
                  <span class="text-[11px] text-on-surface-variant whitespace-nowrap">排名</span>
                  <button
                    class="relative rounded-full transition-all duration-200 flex-shrink-0"
                    :class="node.isRankingSelected == 1 || node.isRankingSelected === true ? 'bg-primary' : 'bg-gray-300'"
                    style="height:18px;width:36px"
                    :title="(node.isRankingSelected == 1 || node.isRankingSelected === true) ? '取消参与排名' : '参与排名'"
                    data-track="ranking"
                    @click.stop="emitToggleRanking(node)"
                  >
                    <span
                      class="absolute top-0.5 w-3.5 h-3.5 bg-white rounded-full shadow-sm transition-all duration-200 toggle-dot"
                      :style="(node.isRankingSelected == 1 || node.isRankingSelected === true) ? 'left:19px' : 'left:2px'"
                    ></span>
                  </button>
                </div>

                <!-- 显示开关 -->
                <div class="flex items-center gap-1 ml-1">
                  <span class="text-[11px] text-on-surface-variant whitespace-nowrap">显示</span>
                  <button
                    class="relative rounded-full transition-all duration-200 flex-shrink-0"
                    :class="node.isDisplaySelected == 1 || node.isDisplaySelected === true ? 'bg-primary' : 'bg-gray-300'"
                    style="height:18px;width:36px"
                    :title="(node.isDisplaySelected == 1 || node.isDisplaySelected === true) ? '不在前台显示' : '在前台显示'"
                    data-track="display"
                    @click.stop="emitToggleDisplay(node)"
                  >
                    <span
                      class="absolute top-0.5 w-3.5 h-3.5 bg-white rounded-full shadow-sm transition-all duration-200 toggle-dot"
                      :style="(node.isDisplaySelected == 1 || node.isDisplaySelected === true) ? 'left:19px' : 'left:2px'"
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
          </div>

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
    filterText: { type: String, default: '' },
    indent: { type: Number, default: 24 },
    defaultExpanded: { type: Boolean, default: true },
    /** 渲染模式：'unit-manage' 在 v-once 内联渲染全量按钮；其他值使用 slot */
    mode: { type: String, default: '' }
  },
  emits: ['node-click', 'toggle-ranking', 'toggle-display', 'cascade-ranking', 'add-node', 'delete-node'],
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
          this.treeData = val.map(n => this._decorate(n, null, this.defaultExpanded, 0))
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
      }
    },
    filterText() {
      this._syncVisibility()
      this.$forceUpdate()
    }
  },
  methods: {
    /* ===================== 节点装饰 ===================== */

    /** 创建节点对象。
     *  先 spread 原始属性（枚举），再覆写内部属性为非枚举。
     *  这样 Vue 2 的 observe() 只处理枚举属性，跳过后台属性，
     *  避免 200+ 节点的深层响应式追踪。
     *  修改 _expanded/_visible 后须手动 $forceUpdate()。 */
    _decorate(node, parent, expanded, depth) {
      const hasChildren = !!(node.children && Array.isArray(node.children) && node.children.length)
      const id = node.orgNo != null ? String(node.orgNo) : (node.id != null ? String(node.id) : '')
      const label = node.orgName != null ? node.orgName : (node.label || '')
      // 1. 先复制原始枚举属性（包括 id, label, pOrgNo, isRankingSelected …）
      const out = { ...node, id, label }
      // 2. 内部属性全部非枚举 → Vue 跳过
      const priv = {
        _expanded: expanded,
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
        ? node.children.map(c => this._decorate(c, out, expanded, depth + 1))
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
    },

    collapseAll() {
      this._walkTree(this.treeData, n => { n._expanded = false })
      this._syncVisibility()
      this.$forceUpdate()
    },

    /* ===================== mode=unit-manage 按钮事件 ===================== */

    emitToggleRanking(node) {
      this.$emit('toggle-ranking', node)
      // $emit 是同步的 — 父组件 handler 已执行完毕，node.isRankingSelected 已翻转
      this._updateRankingToggleDOM(node)
    },

    emitToggleDisplay(node) {
      this.$emit('toggle-display', node)
      this._updateDisplayToggleDOM(node)
    },

    emitCascadeRanking(node, selected) {
      this.$emit('cascade-ranking', node, selected)
      // 父组件 cascadeRanking 已同步执行完所有子节点的 isRankingSelected 翻转
      this._updateCascadeRankingDOM(node)
    },

    emitAdd(node) {
      this.$emit('add-node', node)
    },

    emitDelete(node) {
      this.$emit('delete-node', node)
    },

    /* ===================== 直接 DOM 更新 toggle 视觉状态 ===================== */

    _updateRankingToggleDOM(node) {
      const on = node.isRankingSelected == 1 || node.isRankingSelected === true
      const track = this.$el.querySelector(`[data-nid="${node.id}"] [data-track="ranking"]`)
      if (!track) return
      track.classList.toggle('bg-primary', on)
      track.classList.toggle('bg-gray-300', !on)
      track.title = on ? '取消参与排名' : '参与排名'
      const dot = track.querySelector('.toggle-dot')
      if (dot) dot.style.left = on ? '19px' : '2px'
    },

    _updateDisplayToggleDOM(node) {
      const on = node.isDisplaySelected == 1 || node.isDisplaySelected === true
      const track = this.$el.querySelector(`[data-nid="${node.id}"] [data-track="display"]`)
      if (!track) return
      track.classList.toggle('bg-primary', on)
      track.classList.toggle('bg-gray-300', !on)
      track.title = on ? '不在前台显示' : '在前台显示'
      const dot = track.querySelector('.toggle-dot')
      if (dot) dot.style.left = on ? '19px' : '2px'
    },

    _updateCascadeRankingDOM(node) {
      if (!node._hasChildren || !node.children) return
      const walk = (nodes) => {
        for (const child of nodes) {
          this._updateRankingToggleDOM(child)
          if (child._hasChildren && child.children) walk(child.children)
        }
      }
      walk(node.children)
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
        c.push('bg-primary/10 border-primary/30 text-primary font-semibold')
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
/* 隐藏（替代 v-show / :style display） */
.th { display: none; }
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
