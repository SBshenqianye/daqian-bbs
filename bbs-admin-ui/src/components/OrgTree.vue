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
    <!-- tree (always renders ALL nodes; visibility via v-show for zero-DOM-shuffle toggling) -->
    <div v-else class="select-none">
      <div v-for="(node, i) in flatList" :key="node.id || i">
        <div
          v-show="node._visible"
          class="group flex items-center gap-1 px-3 py-1.5 rounded-lg transition-all duration-150 border mb-0.5 cursor-pointer"
          :class="rowClass(node)"
          :style="{ marginLeft: node._depth * indent + 'px' }"
          @click="handleRowClick(node)"
        >
          <!-- expand/collapse arrow -->
          <button
            v-if="node._hasChildren"
            class="w-5 h-5 flex items-center justify-center rounded hover:bg-surface-variant transition-colors flex-shrink-0 -ml-0.5"
            :class="node._expanded ? 'text-primary' : 'text-outline'"
            @click.stop="toggleNode(node)"
          >
            <span
              class="material-symbols-outlined"
              style="font-size: 14px;"
              :style="{ transform: node._expanded ? 'rotate(90deg)' : 'none', transition: 'transform .12s ease' }"
            >chevron_right</span>
          </button>
          <span v-else class="w-5 h-5 flex-shrink-0"></span>

          <!-- type icon -->
          <span
            class="material-symbols-outlined flex-shrink-0"
            style="font-size: 18px;"
            :class="node._hasChildren ? 'text-primary' : 'text-outline'"
          >{{ node._hasChildren ? 'folder' : 'description' }}</span>

          <!-- label -->
          <span class="flex-1 font-body-md truncate min-w-0 ml-1" :class="{ 'font-semibold text-primary': node.id === selectedId }">{{ node.label }}</span>

          <!-- selected checkmark -->
          <span
            v-if="node.id === selectedId"
            class="material-symbols-outlined text-primary flex-shrink-0"
            style="font-size: 16px;"
          >check_circle</span>

          <!-- consumer-provided per-node actions -->
          <slot name="node-actions" :node="node" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'OrgTree',
  props: {
    /** Raw (un-decorated) tree nodes from API */
    nodes: { type: Array, default: () => [] },
    /** Loading state */
    loading: { type: Boolean, default: false },
    /** Currently selected node id (for highlighting) */
    selectedId: { type: String, default: '' },
    /** Search text to filter the tree */
    filterText: { type: String, default: '' },
    /** Pixels of indentation per depth level */
    indent: { type: Number, default: 24 },
    /** Whether nodes start expanded */
    defaultExpanded: { type: Boolean, default: true }
  },
  emits: ['node-click'],
  data() {
    return {
      /** Decorated tree (rebuilt on nodes prop change). All nodes live here. */
      treeData: [],
      /** DFS flat list of ALL nodes — built once, never replaced.
       *  Expand/collapse only toggles node._visible; no list churn. */
      flatList: [],
      /** Set true once treeData is populated (distinguishes "no data" from "filtered empty") */
      initialized: false,
      /** Number of nodes matching current filter (0 filtered-but-some-match vs no-data) */
      matchCount: 0
    }
  },
  computed: {
    /* no visibleTree/visibleList computeds — see toggleNode/expandAll/collapseAll */
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
        // Build flat list once
        this._buildFlatList()
        // Apply current filter (or set visible from expansion state)
        this._syncVisibility()
      }
    },
    filterText() {
      this._syncVisibility()
    }
  },
  methods: {
    /* ===================== decoration / flattening ===================== */

    /** Decorate a raw node with display-oriented properties.
     *  _parent is stored non-enumerable so Vue 2's observe() skips it,
     *  preventing circular-reactivity overhead on the parent pointer. */
    _decorate(node, parent, expanded, depth) {
      const hasChildren = !!(node.children && Array.isArray(node.children) && node.children.length)
      const id = node.orgNo != null ? String(node.orgNo) : (node.id != null ? String(node.id) : '')
      const label = node.orgName != null ? node.orgName : (node.label || '')
      const out = { ...node, id, label, _expanded: expanded, _depth: depth, _hasChildren: hasChildren, _visible: false }
      // Store parent as non-enumerable to keep Vue 2 reactivity shallow
      Object.defineProperty(out, '_parent', {
        value: parent, enumerable: false, writable: true, configurable: true
      })
      if (hasChildren) {
        out.children = node.children.map(c => this._decorate(c, out, expanded, depth + 1))
      } else {
        out.children = []
      }
      return out
    },

    /** Build flatList — DFS pre-order of ALL nodes. */
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

    /* ===================== visibility sync ===================== */

    /** Recalculate _visible for all nodes based on filter + expansion.
     *  flatList is DFS pre-order so parent is always before children. */
    _syncVisibility() {
      const ft = (this.filterText || '').trim().toLowerCase()
      if (ft) {
        this._syncFiltered(ft)
      } else {
        this._syncExpanded()
        this.matchCount = this.flatList.length
      }
    },

    /** Set _visible purely from expansion state (no filter). */
    _syncExpanded() {
      let count = 0
      for (let i = 0; i < this.flatList.length; i++) {
        const n = this.flatList[i]
        if (n._depth === 0) {
          n._visible = true
          count++
        } else {
          n._visible = n._parent._visible && n._parent._expanded
          if (n._visible) count++
        }
      }
      this.matchCount = count
    },

    /** Set _Visible from filter + expansion.
     *  A node is visible if (it matches filter OR has matching descendants)
     *  AND all its ancestors are expanded. */
    _syncFiltered(ft) {
      // Phase 1: bottom-up match detection via tree walk
      this._markFilterMatch(this.treeData, ft)
      // Phase 2: top-down visibility from expansion + filter state
      let count = 0
      for (let i = 0; i < this.flatList.length; i++) {
        const n = this.flatList[i]
        if (n._depth === 0) {
          n._visible = n._filterMatch
          if (n._visible) count++
        } else {
          n._visible = n._parent._visible && n._parent._expanded && n._filterMatch
          if (n._visible) count++
        }
      }
      this.matchCount = count
    },

    /** Bottom-up: set _filterMatch if node label matches or any descendant matches. */
    _markFilterMatch(nodes, ft) {
      let localMatch = false
      for (const n of nodes) {
        const label = n.orgName != null ? n.orgName : (n.label || '')
        const labelMatch = label.toLowerCase().includes(ft)
        const childMatch = n.children && n.children.length
          ? this._markFilterMatch(n.children, ft)
          : false
        n._filterMatch = labelMatch || childMatch
        if (n._filterMatch) localMatch = true
      }
      return localMatch
    },

    /** After toggle/expand/collapse: recalculate _visible for the subtree
     *  starting from the affected node, using DFS-order position in flatList. */
    _recalcSubtree(startNode) {
      if (!this.filterText) {
        // No filter: just propagate expansion visibility downward
        this._propagateVisibility(startNode)
        // Also recount matchCount for the whole list
        this.matchCount = 0
        for (let i = 0; i < this.flatList.length; i++) {
          if (this.flatList[i]._visible) this.matchCount++
        }
      } else {
        // With filter active: full resync needed (filterMatch may differ per node)
        this._syncVisibility()
      }
    },

    /** Propagate _visible downward from a node based on expansion state only. */
    _propagateVisibility(node) {
      if (!node._hasChildren || !node.children) return
      for (const child of node.children) {
        child._visible = child._parent._visible && child._parent._expanded
        if (child._hasChildren) this._propagateVisibility(child)
      }
    },

    /* ===================== public methods ===================== */

    toggleNode(node) {
      if (!node._hasChildren) return
      node._expanded = !node._expanded
      this._recalcSubtree(node)
    },

    expandAll() {
      this._walkTree(this.treeData, n => { n._expanded = true })
      this._syncVisibility()
    },

    collapseAll() {
      this._walkTree(this.treeData, n => { n._expanded = false })
      this._syncVisibility()
    },

    /* ===================== internal ===================== */

    _walkTree(nodes, fn) {
      if (!nodes || !Array.isArray(nodes)) return
      for (const n of nodes) {
        fn(n)
        if (n.children && n.children.length) this._walkTree(n.children, fn)
      }
    },

    rowClass(node) {
      if (node.id === this.selectedId) {
        return 'bg-primary/10 border-primary/30 text-primary font-semibold'
      }
      return 'bg-surface-container-low border-transparent hover:border-outline-variant/30 hover:bg-surface-container-low/80'
    },

    handleRowClick(node) {
      this.$emit('node-click', node)
    }
  }
}
</script>
