<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">tune</span>
            排名单位配置
          </h1>
          <p class="text-body-md text-secondary mt-1">在树中勾选参与排名的单位，未勾选的不参与排名展示</p>
        </div>
        <div class="flex gap-3">
          <button class="px-4 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="expandAll">展开全部</button>
          <button class="px-4 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="collapseAll">收起全部</button>
          <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-16">
        <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
      </div>

      <!-- Error -->
      <div v-else-if="loadError" class="bg-error-container border border-error/20 rounded-xl p-8 text-center">
        <span class="material-symbols-outlined" style="font-size:48px;color:rgba(var(--error-rgb),0.6)">error_outline</span>
        <p class="text-body-md text-error mt-2">{{ loadError }}</p>
      </div>

      <!-- Org Tree -->
      <div v-else class="bg-container border border-border rounded-xl p-6">
        <div v-if="!orgTree.length" class="text-center py-12 text-on-surface-variant">
          <span class="material-symbols-outlined opacity-20" style="font-size:48px">account_tree</span>
          <p class="text-body-md mt-2">暂无可配置的单位数据</p>
        </div>
        <OrgTree
          v-else
          ref="orgTree"
          :nodes="orgTree"
          :loading="false"
        >
          <template #node-actions="{ node }">
            <button
              class="w-7 h-7 flex items-center justify-center rounded hover:bg-primary/10 transition-colors"
              :class="node.isRankingSelected ? 'text-primary' : 'text-outline'"
              :title="node.isRankingSelected ? '取消参与排名' : '参与排名'"
              @click.stop="toggleOrg(node)"
            >
              <span class="material-symbols-outlined" style="font-size:18px">
                {{ node.isRankingSelected ? 'toggle_on' : 'toggle_off' }}
              </span>
            </button>
          </template>
        </OrgTree>
      </div>
    </div>
  </div>
</template>

<script>
import OrgTree from '@/components/OrgTree.vue'

function walkTree(nodes, fn) {
  if (!nodes || !Array.isArray(nodes)) return
  for (const n of nodes) {
    fn(n)
    if (n.children && n.children.length) walkTree(n.children, fn)
  }
}

export default {
  name: 'BBSPointsConfig',
  components: { OrgTree },
  data() {
    return {
      loading: false,
      saving: false,
      loadError: '',
      orgTree: [],
      /** orgNo -> boolean, 用于保存 */
      rankingMap: {},
      /** orgNo -> boolean, 用于检测变更 */
      originalState: {},
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      this.loadError = ''
      try {
        const resp = await this.getRequestUrl('/common/saOrgTree')
        if (resp && resp.obj && Array.isArray(resp.obj) && resp.obj.length) {
          this.orgTree = resp.obj
          // 遍历树初始化 rankingMap 和 originalState
          this.rankingMap = {}
          this.originalState = {}
          walkTree(this.orgTree, n => {
            const val = n.isRankingSelected === 1 || n.isRankingSelected === true
            this.$set(this.rankingMap, n.id, val)
            this.$set(this.originalState, n.id, val)
          })
        } else {
          this.orgTree = []
        }
      } catch (e) {
        this.loadError = '加载失败，请检查网络'
      }
      this.loading = false
    },
    toggleOrg(node) {
      const val = !(this.rankingMap[node.id] === true)
      this.$set(this.rankingMap, node.id, val)
      node.isRankingSelected = val ? 1 : 0
    },
    expandAll() {
      if (this.$refs.orgTree) this.$refs.orgTree.expandAll()
    },
    collapseAll() {
      if (this.$refs.orgTree) this.$refs.orgTree.collapseAll()
    },
    hasChanges() {
      return Object.keys(this.rankingMap).some(k => this.rankingMap[k] !== this.originalState[k])
    },
    async handleSave() {
      if (!this.hasChanges()) {
        this.$message.info('没有需要保存的变更')
        return
      }
      this.saving = true
      try {
        const resp = await this.postRequest('/common/saOrg/batchUpdateRanking', this.rankingMap)
        if (resp) {
          this.$message.success('保存成功')
          // 更新原始状态
          Object.keys(this.rankingMap).forEach(k => { this.originalState[k] = this.rankingMap[k] })
        }
      } catch (e) {
        this.$message.error('保存失败')
      }
      this.saving = false
    },
  },
}
</script>
