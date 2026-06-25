<template>
  <div>
    <!-- loading -->
    <div v-if="loading" class="py-12 text-center text-on-surface-variant flex flex-col items-center gap-2">
      <span class="material-symbols-outlined opacity-50 animate-spin" style="font-size: 36px;">sync</span>
      <p class="text-body-md">加载中...</p>
    </div>
    <!-- empty -->
    <div v-else-if="!filteredTree.length" class="py-12 text-center text-on-surface-variant flex flex-col items-center gap-2">
      <span class="material-symbols-outlined opacity-20" style="font-size: 48px;">account_tree</span>
      <p class="text-body-md">{{ filterText ? '无匹配单位' : '暂无组织数据' }}</p>
    </div>
    <!-- tree -->
    <div v-else class="select-none">
      <div v-for="(node, i) in visibleTree" :key="node.id || i">
        <div
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
              :style="{ transform: node._expanded ? 'rotate(90deg)' : 'none', transition: 'transform .2s ease' }"
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
function walkTree(nodes, fn) {
  if (!nodes || !Array.isArray(nodes)) return
  for (const n of nodes) {
    fn(n)
    if (n.children && n.children.length) walkTree(n.children, fn)
  }
}

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
      /** Internally-decorated copy of `nodes` — stored in data so Vue 2
       *  makes _expanded reactive and toggleNode() triggers re-render. */
      treeData: []
    }
  },
  computed: {
    /** Apply search filter on top of treeData, or pass through. */
    filteredTree() {
      if (!this.treeData.length) return []
      const ft = (this.filterText || '').trim().toLowerCase()
      if (!ft) return this.treeData
      return this._filterAndDecorate(this.treeData, ft)
    },
    /** Flatten tree to a single-level list, skipping collapsed subtrees. */
    visibleTree() {
      const list = []
      const walk = nodes => {
        if (!nodes || !nodes.length) return
        for (const n of nodes) {
          list.push(n)
          if (n._expanded && n._hasChildren) walk(n.children)
        }
      }
      walk(this.filteredTree)
      return list
    }
  },
  watch: {
    nodes: {
      immediate: true,
      handler(val) {
        if (val && val.length) {
          this.treeData = val.map(n => this._decorate(n, this.defaultExpanded, 0))
        } else {
          this.treeData = []
        }
      }
    }
  },
  methods: {
    /* ---------- helpers ---------- */

    /** Decorate a raw node with display-oriented properties. */
    _decorate(node, expanded, depth) {
      const hasChildren = !!(node.children && Array.isArray(node.children) && node.children.length)
      const id = node.orgNo != null ? String(node.orgNo) : (node.id != null ? String(node.id) : '')
      const label = node.orgName != null ? node.orgName : (node.label || '')
      const out = { ...node, id, label, _expanded: expanded, _depth: depth, _hasChildren: hasChildren }
      if (hasChildren) {
        out.children = node.children.map(c => this._decorate(c, expanded, depth + 1))
      } else {
        out.children = []
      }
      return out
    },

    /** Build a filtered subtree where matching nodes + ancestors are fully
     *  expanded.  Returns newly-decorated objects so original treeData
     *  toggle state is preserved when filter clears. */
    _filterAndDecorate(nodes, ft) {
      const result = []
      for (const n of nodes) {
        const label = n.orgName != null ? n.orgName : (n.label || '')
        const labelMatch = label.toLowerCase().includes(ft)
        const matchingChildren = n.children && n.children.length
          ? this._filterAndDecorate(n.children, ft)
          : []
        if (labelMatch || matchingChildren.length) {
          const decorated = this._decorate(n, true, n._depth || 0)
          if (matchingChildren.length) decorated.children = matchingChildren
          result.push(decorated)
        }
      }
      return result
    },

    /* ---------- public methods (callable via ref) ---------- */

    /** Toggle a node's expanded state. */
    toggleNode(node) {
      if (!node._hasChildren) return
      node._expanded = !node._expanded
    },

    /** Expand every node in the tree. */
    expandAll() {
      walkTree(this.treeData, n => { n._expanded = true })
    },

    /** Collapse every node in the tree. */
    collapseAll() {
      walkTree(this.treeData, n => { n._expanded = false })
    },

    /* ---------- internal ---------- */

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
