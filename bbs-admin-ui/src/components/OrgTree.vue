<template>
  <div>
    <!-- Tree (always mounted, CSS toggle to avoid v-if DOM destruction) -->
    <div
      :class="showTree ? '' : 'opacity-0 h-0 overflow-hidden pointer-events-none'"
    >
      <el-tree
        ref="tree"
        :data="normalizedNodes"
        node-key="id"
        :props="elTreeProps"
        :default-expand-all="defaultExpanded"
        :filter-node-method="filterNode"
        :expand-on-click-node="false"
        :highlight-current="false"
        :indent="indent"
        class="org-el-tree"
        @node-click="handleNodeClick"
      >
        <template #default="{ node: elNode, data }">
          <div
            class="org-tree-row flex items-center gap-1 w-full"
            :class="rowCls(data, elNode)"
            :data-nid="data.id"
          >
            <!-- Chevron (expand/collapse) -->
            <button
              v-if="data._hasChildren"
              class="w-5 h-5 flex items-center justify-center rounded hover:bg-surface-variant transition-colors flex-shrink-0 -ml-0.5"
              @click.stop="toggleElNode(elNode)"
            >
              <span
                class="material-symbols-outlined tree-chevron"
                :class="{ 'tree-copen': elNode.expanded }"
                style="font-size:14px"
              >chevron_right</span>
            </button>
            <span v-else class="w-5 h-5 flex-shrink-0"></span>

            <!-- Node icon -->
            <span
              class="material-symbols-outlined flex-shrink-0 text-outline"
              style="font-size: 18px;"
            >{{ data._hasChildren ? 'folder' : 'description' }}</span>

            <!-- Label -->
            <span class="flex-1 font-body-md truncate min-w-0 ml-1">{{ data.label }}</span>

            <!-- ═══ mode=unit-manage: toggle buttons + CRUD ═══ -->
            <template v-if="mode === 'unit-manage'">
              <div class="flex items-center gap-0.5 flex-shrink-0">
                <!-- Ranking toggle -->
                <div class="flex items-center gap-1 ml-1">
                  <span class="text-[11px] text-on-surface-variant whitespace-nowrap">排名</span>
                  <button
                    class="relative rounded-full transition-all duration-200 flex-shrink-0"
                    :class="rankingBtnCls(data)"
                    style="height:18px;width:36px"
                    :title="rankingTitle(data)"
                    @click.stop="emitToggleRanking(data)"
                  >
                    <span
                      class="absolute top-0.5 w-3.5 h-3.5 bg-white rounded-full shadow-sm transition-all duration-200"
                      :style="rankingDotStyle(data)"
                    ></span>
                  </button>
                </div>

                <!-- Display toggle -->
                <div class="flex items-center gap-1 ml-1">
                  <span class="text-[11px] text-on-surface-variant whitespace-nowrap">显示</span>
                  <button
                    class="relative rounded-full transition-all duration-200 flex-shrink-0"
                    :class="displayBtnCls(data)"
                    style="height:18px;width:36px"
                    :title="displayTitle(data)"
                    @click.stop="emitToggleDisplay(data)"
                  >
                    <span
                      class="absolute top-0.5 w-3.5 h-3.5 bg-white rounded-full shadow-sm transition-all duration-200"
                      :style="displayDotStyle(data)"
                    ></span>
                  </button>
                </div>

                <!-- CRUD (hover show) -->
                <div class="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity duration-150 ml-1">
                  <button
                    class="inline-flex items-center gap-0.5 px-1.5 py-1 font-medium text-primary hover:bg-primary/10 rounded-md transition-colors"
                    style="font-size: 11px;"
                    @click.stop="emitAdd(data)"
                  >
                    <span class="material-symbols-outlined" style="font-size: 12px;">add</span>
                    新增
                  </button>
                  <button
                    v-if="data.id && data.id.length !== 5"
                    class="inline-flex items-center gap-0.5 px-1.5 py-1 font-medium text-error hover:bg-error/10 rounded-md transition-colors"
                    style="font-size: 11px;"
                    @click.stop="emitDelete(data)"
                  >
                    <span class="material-symbols-outlined" style="font-size: 12px;">delete</span>
                    删除
                  </button>
                </div>
              </div>
            </template>

            <!-- "当前组织" tag -->
            <span
              v-if="data.id === currentValueId && data.id !== selectedId"
              class="text-outline/60 ml-1 flex-shrink-0 whitespace-nowrap"
              style="font-size: 10px; line-height: 14px;"
            >当前组织</span>

            <!-- Default mode: parent slot -->
            <slot v-if="mode !== 'unit-manage'" name="node-actions" :node="data" />
          </div>
        </template>
      </el-tree>
    </div>

    <!-- Overlays (simple v-if chain, no DOM destruction risk) -->
    <div v-if="loading" class="py-12 text-center text-on-surface-variant flex flex-col items-center gap-2">
      <span class="material-symbols-outlined opacity-50 animate-spin" style="font-size: 36px;">sync</span>
      <p class="text-body-md">加载中...</p>
    </div>
    <div v-else-if="!initialized" class="py-12 text-center text-on-surface-variant flex flex-col items-center gap-2">
      <span class="material-symbols-outlined opacity-20" style="font-size: 48px;">account_tree</span>
      <p class="text-body-md">暂无组织数据</p>
    </div>
    <div v-else-if="filterText && !hasMatches" class="py-12 text-center text-on-surface-variant flex flex-col items-center gap-2">
      <span class="material-symbols-outlined opacity-20" style="font-size: 48px;">search_off</span>
      <p class="text-body-md">无匹配单位</p>
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
    /** 数据库中当前保存的组织 ID (用于黯淡高亮) */
    currentValueId: { type: String, default: '' },
    filterText: { type: String, default: '' },
    indent: { type: Number, default: 24 },
    defaultExpanded: { type: Boolean, default: true },
    /** 渲染模式: 'unit-manage' 内联渲染按钮; 其他值使用 slot */
    mode: { type: String, default: '' },
  },
  emits: ['node-click', 'toggle-ranking', 'toggle-display', 'cascade-ranking', 'cascade-display', 'add-node', 'delete-node'],
  data() {
    return {
      initialized: false,
      hasMatches: true,
      elTreeProps: { children: 'children', label: 'label' },
    }
  },
  computed: {
    showTree() {
      return this.initialized && this.normalizedNodes.length > 0
    },
    normalizedNodes() {
      const result = this._normalizeData(this.nodes)
      this.initialized = result.length > 0
      return result
    },
  },
  watch: {
    filterText(val) {
      try {
        const tree = this.$refs.tree
        if (!tree) return
        const ft = (val || '').trim()
        tree.filter(ft || null)
        if (ft) {
          // Expand all to show matching results
          this.$nextTick(() => {
            if (this._isDestroyed) return
            this._expandAllNodes()
            this._checkMatches()
          })
        } else {
          this.hasMatches = true
        }
      } catch (e) {
        console.error('[OrgTree] filterText error:', e)
      }
    },
  },
  methods: {
    /* ═══════════ Data Normalization ═══════════ */

    _normalizeData(nodes) {
      if (!nodes || !Array.isArray(nodes)) return []
      return nodes.map(n => {
        const id = String(n.orgNo != null ? n.orgNo : (n.id != null ? n.id : ''))
        const label = n.orgName != null ? n.orgName : (n.label || '')
        const hasChildren = !!(n.children && Array.isArray(n.children) && n.children.length)
        const out = { ...n, id, label, _hasChildren: hasChildren }
        if (hasChildren) {
          out.children = this._normalizeData(n.children)
        }
        return out
      })
    },

    /* ═══════════ Filter ═══════════ */

    filterNode(value, data) {
      if (!value) return true
      return (data.label || '').toLowerCase().includes(value.toLowerCase())
    },

    _checkMatches() {
      const tree = this.$refs.tree
      if (!tree) { this.hasMatches = false; return }
      // Check if any top-level node is visible after filtering
      const root = tree.store.root
      this.hasMatches = root.childNodes && root.childNodes.some(n => n.visible)
    },

    /* ═══════════ Expand/Collapse ═══════════ */

    toggleElNode(elNode) {
      if (elNode.expanded) {
        elNode.collapse()
      } else {
        elNode.expand()
      }
    },

    expandAll() {
      try {
        this._expandAllNodes()
      } catch (e) {
        console.error('[OrgTree] expandAll error:', e)
      }
    },

    _expandAllNodes() {
      const tree = this.$refs.tree
      if (!tree) return
      const walk = (nodes) => {
        for (const n of nodes) {
          if (!n.expanded) n.expand()
          if (n.childNodes && n.childNodes.length) walk(n.childNodes)
        }
      }
      walk(tree.store.root.childNodes || [])
    },

    collapseAll() {
      try {
        const tree = this.$refs.tree
        if (!tree) return
        const walk = (nodes) => {
          for (const n of nodes) {
            if (n.childNodes && n.childNodes.length) walk(n.childNodes)
            if (n.expanded) n.collapse()
          }
        }
        walk(tree.store.root.childNodes || [])
      } catch (e) {
        console.error('[OrgTree] collapseAll error:', e)
      }
    },

    /** 展开到指定节点 (向上展开所有祖先) */
    expandToNode(id) {
      try {
        const tree = this.$refs.tree
        if (!tree) return false
        const elNode = tree.store.getNode(id)
        if (!elNode) return false
        // Expand all ancestors
        let current = elNode.parent
        while (current && current.data) {
          if (!current.expanded) current.expand()
          current = current.parent
        }
        return true
      } catch (e) {
        console.error('[OrgTree] expandToNode error:', e)
        return false
      }
    },

    /** 按 ID toggle 节点展开/折叠 */
    toggleNodeById(id) {
      try {
        const tree = this.$refs.tree
        if (!tree) return false
        const elNode = tree.store.getNode(id)
        if (!elNode) return false
        this.toggleElNode(elNode)
        return true
      } catch (e) {
        console.error('[OrgTree] toggleNodeById error:', e)
        return false
      }
    },

    /** 获取从根到指定节点的路径 (用于 OrgTreePicker 路径显示) */
    getNodePath(id) {
      const path = []
      const find = (nodes, target) => {
        for (const n of nodes) {
          if (n.id === target) { path.push(n); return true }
          if (n.children && n.children.length) {
            if (find(n.children, target)) { path.unshift(n); return true }
          }
        }
        return false
      }
      find(this.normalizedNodes, id)
      return path
    },

    /* ═══════════ Row Styling ═══════════ */

    rowCls(data, elNode) {
      const c = ['group']
      if (data.id === this.selectedId) {
        c.push('bg-primary/15 border-primary text-primary font-semibold')
      } else if (data.id === this.currentValueId) {
        c.push('bg-surface-variant/30 border-outline-variant/40 text-on-surface-variant/60')
      } else {
        c.push('bg-surface-container-low border-transparent hover:border-outline-variant/30 hover:bg-surface-container-low/80')
      }
      return c
    },

    /* ═══════════ Toggle Visual State ═══════════ */

    rankingBtnCls(node) {
      const on = node.isRankingSelected == 1 || node.isRankingSelected === true
      return on ? 'bg-primary' : 'bg-gray-300'
    },
    rankingDotStyle(node) {
      const on = node.isRankingSelected == 1 || node.isRankingSelected === true
      return on ? 'left:19px' : 'left:2px'
    },
    rankingTitle(node) {
      const on = node.isRankingSelected == 1 || node.isRankingSelected === true
      return on ? '取消参与排名' : '参与排名'
    },
    displayBtnCls(node) {
      const on = node.isDisplaySelected == 1 || node.isDisplaySelected === true
      return on ? 'bg-primary' : 'bg-gray-300'
    },
    displayDotStyle(node) {
      const on = node.isDisplaySelected == 1 || node.isDisplaySelected === true
      return on ? 'left:19px' : 'left:2px'
    },
    displayTitle(node) {
      const on = node.isDisplaySelected == 1 || node.isDisplaySelected === true
      return on ? '不在前台显示' : '在前台显示'
    },

    /* ═══════════ Event Emitters ═══════════ */

    handleNodeClick(data, elNode) {
      this.$emit('node-click', data, elNode)
    },

    emitToggleRanking(node) {
      if (node._hasChildren) {
        const newVal = !(node.isRankingSelected == 1 || node.isRankingSelected === true)
        this.$emit('toggle-ranking', node)
        this.$emit('cascade-ranking', node, newVal)
        return
      }
      this.$emit('toggle-ranking', node)
    },

    emitToggleDisplay(node) {
      if (node._hasChildren) {
        const newVal = !(node.isDisplaySelected == 1 || node.isDisplaySelected === true)
        this.$emit('toggle-display', node)
        this.$emit('cascade-display', node, newVal)
        return
      }
      this.$emit('toggle-display', node)
    },

    emitAdd(node) {
      this.$emit('add-node', node)
    },
    emitDelete(node) {
      this.$emit('delete-node', node)
    },
  },
}
</script>

<style>
/* Override el-tree default styles for consistency */
.org-el-tree .el-tree-node__content {
  height: auto;
  padding: 0;
  border-radius: 8px;
  margin-bottom: 2px;
}
.org-el-tree .el-tree-node {
  position: relative;
}
/* Hide default el-tree expand icon (we render custom chevron in scoped slot) */
.org-el-tree .el-tree-node__expand-icon {
  display: none !important;
}
/* Remove el-tree default hover highlight */
.org-el-tree .el-tree-node__content:hover {
  background-color: transparent;
}
/* Row styles */
.org-tree-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
  width: 100%;
  box-sizing: border-box;
}
.org-tree-row:hover {
  border-color: rgba(var(--outline-variant-rgb, 200,200,200), 0.3);
  background-color: rgba(var(--surface-container-low-rgb, 245,245,245), 0.8);
}
/* Expand arrow rotation */
.tree-chevron { transition: transform .12s ease; }
.tree-copen { transform: rotate(90deg); }
</style>
