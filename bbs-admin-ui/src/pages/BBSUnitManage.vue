<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">account_tree</span>
            单位管理
          </h1>
          <p class="text-body-md text-secondary mt-1">管理组织单位结构</p>
        </div>
        <div v-if="orgTree.length" class="flex items-center gap-2">
          <button class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors" @click="expandAll">
            <span class="material-symbols-outlined text-[14px]">unfold_more</span>
            全部展开
          </button>
          <button class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors" @click="collapseAll">
            <span class="material-symbols-outlined text-[14px]">unfold_less</span>
            全部折叠
          </button>
        </div>
      </div>

      <!-- Tree Card -->
      <div class="bg-container border border-border rounded-xl p-card-padding" v-loading="loading">
        <!-- Empty -->
        <div v-if="!orgTree.length && !loading" class="py-16 text-center flex flex-col items-center gap-3 text-on-surface-variant">
          <span class="material-symbols-outlined text-[56px] opacity-15">account_tree</span>
          <p class="text-body-md">暂无组织数据</p>
        </div>
        <!-- Tree -->
        <div v-else-if="!loading" class="select-none">
          <div v-for="(node, i) in flatTree" :key="node.id || i">
            <div
              class="group flex items-center gap-1 px-3 py-1.5 rounded-lg transition-all duration-150 border mb-0.5 cursor-pointer"
              :class="[
                node._expanded
                  ? 'bg-primary/5 border-primary/15'
                  : 'bg-surface-container-low border-transparent hover:border-outline-variant/30'
              ]"
              :style="{ marginLeft: node._depth * 28 + 'px' }"
              @click="toggleNode(node)"
            >
              <!-- expand/collapse arrow -->
              <button
                v-if="node._hasChildren"
                class="w-5 h-5 flex items-center justify-center rounded hover:bg-surface-variant transition-colors flex-shrink-0 -ml-0.5"
                :class="node._expanded ? 'text-primary' : 'text-outline'"
                @click.stop="toggleNode(node)"
              >
                <span class="material-symbols-outlined text-[14px]"
                  :style="{ transform: node._expanded ? 'rotate(90deg)' : 'none', transition: 'transform .2s ease' }">chevron_right</span>
              </button>
              <span v-else class="w-5 h-5 flex-shrink-0"></span>

              <!-- type icon -->
              <span class="material-symbols-outlined text-[18px] flex-shrink-0"
                :class="node._hasChildren ? 'text-primary' : 'text-outline'"
              >{{ node._hasChildren ? 'folder' : 'description' }}</span>

              <!-- label -->
              <span class="flex-1 font-body-md text-on-surface truncate min-w-0 ml-1">{{ node.label }}</span>

              <!-- actions -->
              <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-150 flex-shrink-0 ml-2">
                <button
                  class="inline-flex items-center gap-0.5 px-2 py-1 text-[11px] font-medium text-primary hover:bg-primary/10 rounded-md transition-colors"
                  @click.stop="openAdd(node)"
                >
                  <span class="material-symbols-outlined text-[12px]">add</span>
                  新增
                </button>
                <button
                  v-if="node.id && node.id.length !== 5"
                  class="inline-flex items-center gap-0.5 px-2 py-1 text-[11px] font-medium text-error hover:bg-error/10 rounded-md transition-colors"
                  @click.stop="handleRemove(node)"
                >
                  <span class="material-symbols-outlined text-[12px]">delete</span>
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Add Dialog -->
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="dialogVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-md rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">add_circle</span>
              新增下级单位
            </h3>
            <button class="w-8 h-8 flex items-center justify-center rounded-full text-outline hover:bg-surface-variant transition-colors" @click="dialogVisible = false">
              <span class="material-symbols-outlined text-[18px]">close</span>
            </button>
          </div>
          <div class="p-5">
            <div v-if="addParentLabel" class="mb-4 px-3 py-2 bg-surface-variant rounded-lg text-body-md text-on-surface-variant flex items-center gap-2">
              <span class="material-symbols-outlined text-[16px]">arrow_upward</span>
              上级单位：{{ addParentLabel }}
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">单位名称</label>
              <input
                ref="orgNameInput"
                v-model="addOrgName"
                class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md"
                placeholder="请输入单位名称"
                maxlength="50"
                @keyup.enter="onSubmit"
              >
            </div>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-5 py-2 border border-outline rounded-lg text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="dialogVisible = false">取消</button>
            <button class="px-7 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="onSubmit">确认</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
/**
 * Walk the tree and set/clear _expanded on every node.
 */
function walkTree(nodes, fn) {
  if (!nodes || !Array.isArray(nodes)) return
  for (const n of nodes) {
    fn(n)
    if (n.children && n.children.length) walkTree(n.children, fn)
  }
}

export default {
  name: 'BBSUnitManage',
  data() {
    return {
      loading: false,
      orgTree: [],
      dialogVisible: false,
      addPOrgNo: '',
      addParentLabel: '',
      addOrgName: ''
    }
  },
  computed: {
    /**
     * Flatten the tree into a single-level list that only includes
     * nodes whose every ancestor is expanded.
     *
     * Vue 2's computed tracks every `_expanded` access, so when a node
     * is toggled the list automatically updates — no key tricks needed.
     */
    flatTree() {
      const list = []
      const walk = nodes => {
        if (!nodes || !nodes.length) return
        for (const n of nodes) {
          list.push(n)
          if (n._expanded && n._hasChildren) walk(n.children)
        }
      }
      walk(this.orgTree)
      return list
    }
  },
  mounted() { this.getAllOrgTree() },
  methods: {
    async getAllOrgTree() {
      this.loading = true
      try {
        const res = await this.getRequestUrl('/common/saOrgTree')
        if (res.code == 200) {
          const raw = res.obj || []
          this.orgTree = raw.map(n => this._decorate(n, true, 0))
        } else {
          this.orgTree = []
        }
      } catch (e) { this.orgTree = [] }
      this.loading = false
    },
    _decorate(node, expanded, depth) {
      const hasChildren = !!(node.children && Array.isArray(node.children) && node.children.length)
      const out = { ...node, _expanded: expanded, _depth: depth, _hasChildren: hasChildren }
      if (hasChildren) {
        out.children = node.children.map(c => this._decorate(c, expanded, depth + 1))
      } else {
        out.children = []
      }
      return out
    },
    toggleNode(node) {
      if (!node._hasChildren) return
      node._expanded = !node._expanded
      // flatTree computed will re-evaluate automatically
    },
    openAdd(data) {
      this.addPOrgNo = data.id
      this.addParentLabel = data.label
      this.addOrgName = ''
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.orgNameInput) this.$refs.orgNameInput.focus()
      })
    },
    async onSubmit() {
      if (!this.addOrgName.trim()) { this.$message.warning('请输入单位名称'); return }
      try {
        const res = await this.getRequestUrl(`/saOrg/addSaOrg?pOrgNo=${this.addPOrgNo}&orgName=${encodeURIComponent(this.addOrgName.trim())}`)
        if (res.code == 200) {
          this.$message.success('新增成功')
          this.dialogVisible = false
          this.addOrgName = ''
          this.getAllOrgTree()
        } else {
          this.$message.error(res.message || '新增失败')
        }
      } catch (e) { this.$message.error('新增失败') }
    },
    handleRemove(data) {
      this.$confirm('确定删除该单位吗？', '提示', { type: 'warning' }).then(async () => {
        try {
          const res = await this.getRequestUrl(`/saOrg/deleteSaOrgByOrgNo?orgNo=${data.id}`)
          if (res.code == 200) { this.$message.success('删除成功'); this.getAllOrgTree() }
          else { this.$message.error(res.message || '删除失败') }
        } catch (e) { this.$message.error('删除失败') }
      }).catch(() => {})
    },
    expandAll() {
      walkTree(this.orgTree, n => { n._expanded = true })
    },
    collapseAll() {
      walkTree(this.orgTree, n => { n._expanded = false })
    }
  }
}
</script>
