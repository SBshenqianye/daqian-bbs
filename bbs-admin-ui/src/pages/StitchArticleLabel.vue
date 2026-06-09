<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">label</span>
            标签管理
          </h1>
          <p class="text-body-md text-secondary mt-1">管理文章标签分类</p>
        </div>
      </div>

      <!-- Search & Actions -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex flex-wrap items-center gap-3">
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="openAdd">
            <span class="material-symbols-outlined text-[18px]">add</span>
            新增标签
          </button>
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-error text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md disabled:opacity-50 disabled:cursor-not-allowed" :disabled="multipleSelection.length === 0" @click="handleBatchDelete">
            <span class="material-symbols-outlined text-[18px]">delete</span>
            批量删除
          </button>
          <div class="flex-1 min-w-[200px]">
            <div class="relative">
              <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[18px]">search</span>
              <input v-model="searchLabelName" class="w-full pl-9 pr-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md" placeholder="搜索标签名称" @keyup.enter="handleSearch">
            </div>
          </div>
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="handleSearch">
            <span class="material-symbols-outlined text-[18px]">search</span>
            搜索
          </button>
        </div>
      </div>

      <!-- Labels Table -->
      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full whitespace-nowrap">
            <thead>
              <tr class="bg-surface-container-low border-b border-border">
                <th class="p-4 text-left w-12">
                  <input type="checkbox" class="w-4 h-4 text-primary border-outline-variant rounded" :checked="isAllSelected" @change="selectAll">
                </th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">标签ID</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[120px]">标签名称</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant">是否禁用</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[140px]">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(label, index) in labelsRaw" :key="label.labelId || index" class="border-b border-border hover:bg-surface-container-low/50 transition-colors">
                <td class="p-4">
                  <input type="checkbox" class="w-4 h-4 text-primary border-outline-variant rounded" :checked="isSelected(label)" @change="toggleSelect(label)">
                </td>
                <td class="p-4 font-body-md text-on-surface">{{ label.labelId }}</td>
                <td class="p-4 font-body-md text-on-surface font-medium max-w-[200px] truncate" :title="label.labelName">{{ label.labelName }}</td>
                <td class="p-4">
                  <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[12px] font-medium" :class="label.isDisable === 1 ? 'bg-error-container text-error' : 'bg-green-50 text-green-700'">
                    <span class="w-1.5 h-1.5 rounded-full" :class="label.isDisable === 1 ? 'bg-error' : 'bg-green-500'"></span>
                    {{ label.isDisable === 1 ? '是' : '否' }}
                  </span>
                </td>
                <td class="p-4">
                  <div class="flex items-center gap-2">
                    <button class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded hover:bg-primary/10 transition-colors" @click="openEdit(label)">
                      <span class="material-symbols-outlined text-[14px]">edit</span>
                      编辑
                    </button>
                    <button class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-error bg-error/5 rounded hover:bg-error/10 transition-colors" @click="handleDelete(label)">
                      <span class="material-symbols-outlined text-[14px]">delete</span>
                      删除
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="labelsRaw.length === 0">
                <td colspan="5" class="p-12 text-center">
                  <div class="flex flex-col items-center gap-2 text-on-surface-variant">
                    <span class="material-symbols-outlined text-[48px] opacity-20">label_off</span>
                    <p class="text-body-md">暂无标签数据</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Pagination -->
      <div class="mt-4 bg-container border border-border rounded-xl px-6 py-4 flex items-center justify-between">
        <span class="text-body-md text-on-surface-variant">共 {{ total }} 条记录</span>
        <div class="flex items-center gap-3">
          <span class="text-body-md text-on-surface-variant">每页</span>
          <select v-model.number="pageParams.pageSize" class="bg-surface border border-outline-variant rounded px-3 py-1.5 text-body-md text-on-surface outline-none focus:border-primary" @change="handleSizeChange">
            <option :value="10">10</option>
            <option :value="15">15</option>
            <option :value="20">20</option>
            <option :value="40">40</option>
            <option :value="60">60</option>
            <option :value="100">100</option>
          </select>
          <span class="text-body-md text-on-surface-variant">条</span>
          <div class="flex items-center gap-1">
            <button class="w-8 h-8 flex items-center justify-center rounded border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30" :disabled="pageParams.pageIndex <= 1" @click="pageParams.pageIndex--; getArticleLabelPage()">
              <span class="material-symbols-outlined text-[18px]">chevron_left</span>
            </button>
            <span class="px-3 py-1.5 font-body-md text-on-surface">{{ pageParams.pageIndex }} / {{ totalPages }}</span>
            <button class="w-8 h-8 flex items-center justify-center rounded border border-outline-variant text-on-surface-variant hover:border-primary hover:text-primary transition-all disabled:opacity-30" :disabled="pageParams.pageIndex >= totalPages" @click="pageParams.pageIndex++; getArticleLabelPage()">
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Add Dialog -->
      <div v-if="addVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="addVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-md rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">add_circle</span>
              新增标签
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="addVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">标签名称</label>
              <input v-model="addForm.labelName" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="请输入标签名称" maxlength="50">
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">是否禁用</label>
              <button class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors" :class="addForm.isDisable ? 'bg-error' : 'bg-surface-variant'" @click="addForm.isDisable = addForm.isDisable ? 0 : 1">
                <span class="inline-block h-5 w-5 transform rounded-full bg-white transition-transform shadow-sm" :class="addForm.isDisable ? 'translate-x-6' : 'translate-x-0.5'"></span>
              </button>
            </div>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="addVisible = false">取消</button>
            <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="submitAdd">确定</button>
          </div>
        </div>
      </div>

      <!-- Edit Dialog -->
      <div v-if="editVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="editVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-md rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">edit</span>
              编辑标签
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="editVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">标签ID</label>
              <input :value="editForm.labelId" class="w-full px-4 py-2.5 bg-surface-variant border border-outline-variant rounded font-body-md text-body-md text-on-surface-variant" disabled>
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">标签名称</label>
              <input v-model="editForm.labelName" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="请输入标签名称" maxlength="50">
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">是否禁用</label>
              <button class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors" :class="editForm.enabled ? 'bg-error' : 'bg-surface-variant'" @click="editForm.enabled = editForm.enabled ? 0 : 1">
                <span class="inline-block h-5 w-5 transform rounded-full bg-white transition-transform shadow-sm" :class="editForm.enabled ? 'translate-x-6' : 'translate-x-0.5'"></span>
              </button>
            </div>
          </div>
          <div class="flex justify-end gap-3 p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="editVisible = false">取消</button>
            <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="submitEdit">保存</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StitchArticleLabel',
  data() {
    return {
      searchLabelName: '',
      labelsRaw: [],
      multipleSelection: [],
      addVisible: false,
      editVisible: false,
      addForm: { labelName: '', isDisable: 0 },
      editForm: { labelId: null, labelName: '', enabled: 0 },
      pageParams: { pageIndex: 1, pageSize: 10 },
      total: 0
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageParams.pageSize))
    },
    isAllSelected() {
      return this.labelsRaw.length > 0 && this.labelsRaw.every(l => this.isSelected(l))
    }
  },
  mounted() {
    this.getArticleLabelPage()
  },
  methods: {
    isSelected(label) {
      return this.multipleSelection.some(s => (s.labelId || s.id) === (label.labelId || label.id))
    },
    toggleSelect(label) {
      const idx = this.multipleSelection.findIndex(s => (s.labelId || s.id) === (label.labelId || label.id))
      if (idx >= 0) this.multipleSelection.splice(idx, 1)
      else this.multipleSelection.push(label)
    },
    selectAll() {
      if (this.isAllSelected) this.multipleSelection = []
      else this.multipleSelection = [...this.labelsRaw]
    },
    getArticleLabelPage() {
      const params = {
        pageIndex: this.pageParams.pageIndex,
        pageSize: this.pageParams.pageSize,
        searchInfo: (this.searchLabelName || '').trim()
      }
      this.getRequest('/admin/pageArticleLabel', JSON.stringify(params)).then(resp => {
        if (resp) {
          let list = [], total = 0
          const obj = resp.obj || resp
          if (obj && Array.isArray(obj.list)) { list = obj.list; total = obj.total || 0 }
          else if (Array.isArray(obj)) { list = obj; total = list.length }
          this.total = total
          this.labelsRaw = list.map(item => {
            if (!item) return item
            return Object.assign({}, item, {
              labelId: item.labelId || item.id,
              labelName: item.labelName || item.name || '',
              isDisable: Number(typeof item.isDisable !== 'undefined' ? item.isDisable : (item.disable || 0))
            })
          })
        }
      })
    },
    handleSearch() { this.pageParams.pageIndex = 1; this.getArticleLabelPage() },
    handleSizeChange() { this.pageParams.pageIndex = 1; this.getArticleLabelPage() },
    openAdd() {
      this.addForm = { labelName: '', isDisable: 0 }
      this.addVisible = true
    },
    submitAdd() {
      const labelName = (this.addForm.labelName || '').trim()
      if (!labelName) { this.$message.warning('标签名称不能为空'); return }
      this.postRequest('/admin/addArticleLabel', { labelName, enabled: this.addForm.isDisable }).then(resp => {
        if (resp) {
          this.$message.success('添加成功')
          this.addVisible = false
          this.getArticleLabelPage()
        }
      })
    },
    openEdit(row) {
      if (!row) return
      this.editForm = {
        labelId: row.labelId || row.id,
        labelName: row.labelName || '',
        enabled: Number(typeof row.enabled !== 'undefined' ? row.enabled : 0)
      }
      this.editVisible = true
    },
    submitEdit() {
      const labelId = this.editForm.labelId
      const labelName = (this.editForm.labelName || '').trim()
      if (!labelId) { this.$message.warning('标签ID不能为空'); return }
      if (!labelName) { this.$message.warning('标签名称不能为空'); return }
      this.postRequest('/admin/updArticleLabel', { labelId, labelName, enabled: this.editForm.enabled }).then(resp => {
        if (resp) {
          this.$message.success('修改成功')
          this.editVisible = false
          this.getArticleLabelPage()
        }
      })
    },
    handleDelete(row) {
      const labelId = row.labelId || row.id
      if (!labelId) return
      this.$confirm('确定要删除该标签吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/delArticleLabel', { labelId }).then(resp => {
          if (resp) { this.$message.success('删除成功'); this.getArticleLabelPage() }
        })
      }).catch(() => {})
    },
    handleBatchDelete() {
      const rows = this.multipleSelection || []
      if (rows.length === 0) return
      const labelIds = rows.map(r => r.labelId || r.id).filter(Boolean)
      if (labelIds.length === 0) return
      this.$confirm(`确定要删除选中的 ${labelIds.length} 个标签吗？`, '提示', { type: 'warning' }).then(() => {
        Promise.all(labelIds.map(id => this.postRequest('/admin/delArticleLabel', { labelId: id })))
          .then(() => { this.$message.success('批量删除完成'); this.getArticleLabelPage() })
      }).catch(() => {})
    }
  }
}
</script>
