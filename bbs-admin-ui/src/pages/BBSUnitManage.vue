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
          <button
            class="inline-flex items-center gap-1 px-3 py-1.5 font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
            style="font-size: 12px;"
            @click="$refs.orgTree.expandAll()"
          >
            <span class="material-symbols-outlined" style="font-size: 14px;">unfold_more</span>
            全部展开
          </button>
          <button
            class="inline-flex items-center gap-1 px-3 py-1.5 font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
            style="font-size: 12px;"
            @click="$refs.orgTree.collapseAll()"
          >
            <span class="material-symbols-outlined" style="font-size: 14px;">unfold_less</span>
            全部折叠
          </button>
        </div>
      </div>

      <!-- Tree Card -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <OrgTree
          ref="orgTree"
          :nodes="orgTree"
          :loading="loading"
          @node-click="onNodeClick"
        >
          <template #node-actions="{ node }">
            <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-150 flex-shrink-0 ml-2">
              <button
                class="inline-flex items-center gap-0.5 px-2 py-1 font-medium text-primary hover:bg-primary/10 rounded-md transition-colors"
                style="font-size: 11px;"
                @click.stop="openAdd(node)"
              >
                <span class="material-symbols-outlined" style="font-size: 12px;">add</span>
                新增
              </button>
              <button
                v-if="node.id && node.id.length !== 5"
                class="inline-flex items-center gap-0.5 px-2 py-1 font-medium text-error hover:bg-error/10 rounded-md transition-colors"
                style="font-size: 11px;"
                @click.stop="handleRemove(node)"
              >
                <span class="material-symbols-outlined" style="font-size: 12px;">delete</span>
                删除
              </button>
            </div>
          </template>
        </OrgTree>
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

export default {
  name: 'BBSUnitManage',
  components: { OrgTree },
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
  mounted() { this.getAllOrgTree() },
  methods: {
    async getAllOrgTree() {
      this.loading = true
      try {
        const res = await this.getRequestUrl('/common/saOrgTree')
        if (res.code == 200) {
          this.orgTree = res.obj || []
        } else {
          this.orgTree = []
        }
      } catch (e) { this.orgTree = [] }
      this.loading = false
    },

    onNodeClick(node) {
      if (node._hasChildren) {
        this.$refs.orgTree.toggleNode(node)
      }
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
    }
  }
}
</script>
