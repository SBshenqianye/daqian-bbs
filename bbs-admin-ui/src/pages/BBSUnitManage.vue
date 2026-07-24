<template>
  <div class="bg-surface min-h-screen">
    <!-- Sticky Header -->
    <div class="sticky top-0 z-20 bg-surface border-b border-outline-variant/30 shadow-sm">
      <div class="max-w-7xl mx-auto px-page-margin-desktop py-4">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">account_tree</span>
              组织管理
            </h1>
            <p class="text-body-md text-secondary mt-1">管理组织单位结构、配置排名参与和用户前台显示</p>
          </div>
          <div class="flex items-center gap-2">
            <!-- Search -->
            <div class="relative">
              <input
                v-model="filterText"
                class="w-52 h-9 pl-8 pr-3 bg-surface border border-outline-variant rounded-lg text-body-md text-on-surface placeholder:text-outline focus:border-primary focus:ring-1 focus:ring-primary/30 outline-none transition-all"
                placeholder="搜索单位名称..."
              />
              <!-- 居中搜索图标：用 flex items-center 替代 top-1/2 translate 避免字体垂直偏移 -->
              <div class="absolute left-2.5 inset-y-0 flex items-center pointer-events-none">
                <span class="material-symbols-outlined text-outline leading-none" style="font-size:16px">search</span>
              </div>
              <button
                v-if="filterText"
                class="absolute right-1.5 top-1/2 -translate-y-1/2 w-5 h-5 flex items-center justify-center rounded text-outline hover:text-on-surface hover:bg-surface-variant transition-all"
                @click="filterText = ''"
              >
                <span class="material-symbols-outlined" style="font-size:12px">close</span>
              </button>
            </div>
          <div v-if="orgTree.length" class="flex items-center gap-1">
            <button
              class="inline-flex items-center gap-1 px-2.5 py-1.5 font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
              style="font-size: 12px;"
              @click="$refs.orgTree.expandAll()"
            >
              <span class="material-symbols-outlined" style="font-size: 14px;">unfold_more</span>
              展开全部
            </button>
            <button
              class="inline-flex items-center gap-1 px-2.5 py-1.5 font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
              style="font-size: 12px;"
              @click="$refs.orgTree.collapseAll()"
            >
              <span class="material-symbols-outlined" style="font-size: 14px;">unfold_less</span>
              收起全部
            </button>
          </div>
          <button
            class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!hasChanges || saving"
            @click="handleSave"
          >
            <span class="material-symbols-outlined text-[18px]">save</span>
            {{ saving ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Content -->
  <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">

      <!-- Selected summary -->
      <div v-if="orgTree.length && (rankingSelectedCount > 0 || displaySelectedCount > 0)" class="mb-4 flex items-center gap-4 text-body-md">
        <div v-if="rankingSelectedCount > 0" class="px-3 py-1.5 bg-primary/5 border border-primary/15 rounded-lg flex items-center gap-1.5 text-primary">
          <span class="material-symbols-outlined" style="font-size:16px">emoji_events</span>
          排名：<strong>{{ rankingSelectedCount }}</strong> 个单位
        </div>
        <div v-if="displaySelectedCount > 0" class="px-3 py-1.5 bg-primary/5 border border-primary/15 rounded-lg flex items-center gap-1.5 text-primary">
          <span class="material-symbols-outlined" style="font-size:16px">visibility</span>
          显示：<strong>{{ displaySelectedCount }}</strong> 个节点
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-16">
        <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
      </div>

      <!-- Tree Card -->
      <div v-else class="bg-container border border-border rounded-xl p-card-padding">
        <template v-if="!orgTree.length">
          <div class="text-center py-12 text-on-surface-variant flex flex-col items-center gap-2">
            <span class="material-symbols-outlined opacity-20" style="font-size:48px">account_tree</span>
            <p class="text-body-md">暂无组织数据</p>
          </div>
        </template>
        <template v-else>
          <OrgTree
            ref="orgTree"
            mode="unit-manage"
            :nodes="orgTree"
            :filter-text="filterText"
            :loading="false"
            @node-click="onNodeClick"
            @toggle-ranking="toggleRanking"
            @toggle-display="toggleDisplay"
            @cascade-ranking="cascadeRanking"
            @add-node="openAdd"
            @delete-node="handleRemove"
          />
        </template>
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
              <span class="material-symbols-outlined" style="font-size: 18px;">close</span>
            </button>
          </div>
          <div class="p-5">
            <div v-if="addParentLabel" class="mb-4 px-3 py-2 bg-surface-variant rounded-lg text-body-md text-on-surface-variant flex items-center gap-2">
              <span class="material-symbols-outlined" style="font-size: 16px;">arrow_upward</span>
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
import OrgTree from '../components/OrgTree.vue'

function walkTree(nodes, fn) {
  if (!nodes || !Array.isArray(nodes)) return
  for (const n of nodes) {
    fn(n)
    if (n.children && n.children.length) walkTree(n.children, fn)
  }
}

export default {
  name: 'BBSUnitManage',
  components: { OrgTree },
  data() {
    return {
      loading: false,
      saving: false,
      orgTree: [],
      filterText: '',
      // 排名开关
      rankingMap: {},
      originalRanking: {},
      // 显示开关
      displayMap: {},
      originalDisplay: {},
      // 新增弹窗
      dialogVisible: false,
      addPOrgNo: '',
      addParentLabel: '',
      addOrgName: '',
    }
  },
  computed: {
    rankingSelectedCount() {
      return Object.values(this.rankingMap).filter(Boolean).length
    },
    displaySelectedCount() {
      return Object.values(this.displayMap).filter(Boolean).length
    },
    hasChanges() {
      return Object.keys(this.rankingMap).some(k => this.rankingMap[k] !== this.originalRanking[k])
          || Object.keys(this.displayMap).some(k => this.displayMap[k] !== this.originalDisplay[k])
    },
  },
  mounted() { this.loadData() },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await this.getRequestUrl('/common/saOrgTree')
        if (res.code == 200) {
          this.orgTree = res.obj || []
          // 初始化 maps
          this.rankingMap = {}
          this.originalRanking = {}
          this.displayMap = {}
          this.originalDisplay = {}
          walkTree(this.orgTree, n => {
            const rankVal = n.isRankingSelected === 1 || n.isRankingSelected === true
            this.$set(this.rankingMap, n.id, rankVal)
            this.$set(this.originalRanking, n.id, rankVal)
            const dispVal = n.isDisplaySelected === 1 || n.isDisplaySelected === true
            this.$set(this.displayMap, n.id, dispVal)
            this.$set(this.originalDisplay, n.id, dispVal)
          })
        } else {
          this.orgTree = []
        }
      } catch (e) { this.orgTree = [] }
      this.loading = false
    },

    // ---- 树交互 ----
    onNodeClick(node) {
      if (node._hasChildren) {
        this.$refs.orgTree.toggleNode(node)
      }
    },

    // ---- 排名开关 ----
    getRankingVal(id) {
      return this.rankingMap[id] === true
    },
    toggleRanking(node) {
      const val = !(this.rankingMap[node.id] === true)
      this.$set(this.rankingMap, node.id, val)
      node.isRankingSelected = val ? 1 : 0
    },
    cascadeRanking(node, selected) {
      walkTree(node.children, child => {
        const val = selected ? 1 : 0
        this.$set(this.rankingMap, child.id, !!val)
        child.isRankingSelected = val
      })
    },

    // ---- 显示开关 ----
    getDisplayVal(id) {
      return this.displayMap[id] === true
    },
    toggleDisplay(node) {
      const val = !(this.displayMap[node.id] === true)
      this.$set(this.displayMap, node.id, val)
      node.isDisplaySelected = val ? 1 : 0
    },

    // ---- 保存 ----
    async handleSave() {
      if (!this.hasChanges) {
        this.$message.info('没有需要保存的变更')
        return
      }
      this.saving = true
      try {
        const rankingChanged = Object.keys(this.rankingMap).some(k => this.rankingMap[k] !== this.originalRanking[k])
        const displayChanged = Object.keys(this.displayMap).some(k => this.displayMap[k] !== this.originalDisplay[k])

        const promises = []
        if (rankingChanged) {
          promises.push(this.postRequest('/common/saOrg/batchUpdateRanking', this.rankingMap))
        }
        if (displayChanged) {
          promises.push(this.postRequest('/common/saOrg/batchUpdateDisplay', this.displayMap))
        }

        const results = await Promise.all(promises)
        const allOk = results.every(r => r && r.code === 200)

        if (allOk) {
          this.$message.success('保存成功')
          // 更新原始状态
          Object.keys(this.rankingMap).forEach(k => { this.originalRanking[k] = this.rankingMap[k] })
          Object.keys(this.displayMap).forEach(k => { this.originalDisplay[k] = this.displayMap[k] })
        } else {
          // 部分失败时重新加载数据以恢复真实状态
          this.$message.error('部分配置保存失败，已重置')
          this.loadData()
        }
      } catch (e) {
        this.$message.error('保存失败')
        this.loadData()
      }
      this.saving = false
    },

    // ---- 新增单位 ----
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
          this.loadData()
        } else {
          this.$message.error(res.message || '新增失败')
        }
      } catch (e) { this.$message.error('新增失败') }
    },

    // ---- 删除单位 ----
    handleRemove(data) {
      this.$confirm('确定删除该单位吗？', '提示', { type: 'warning' }).then(async () => {
        try {
          const res = await this.getRequestUrl(`/saOrg/deleteSaOrgByOrgNo?orgNo=${data.id}`)
          if (res.code == 200) { this.$message.success('删除成功'); this.loadData() }
          else { this.$message.error(res.message || '删除失败') }
        } catch (e) { this.$message.error('删除失败') }
      }).catch(() => {})
    },
  },
}
</script>
