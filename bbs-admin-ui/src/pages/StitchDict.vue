<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">settings</span>
            配置管理
          </h1>
          <p class="text-body-md text-secondary mt-1">管理系统配置项</p>
        </div>
      </div>

      <!-- Actions -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex flex-wrap items-center gap-3">
          <button v-if="isAdminId1" class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="openAdd">
            <span class="material-symbols-outlined text-[18px]">add</span>
            新增字典
          </button>
          <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant transition-all font-label-md text-label-md" @click="loadDictList">
            <span class="material-symbols-outlined text-[18px]">refresh</span>
            刷新
          </button>
        </div>
      </div>

      <!-- Dict Table -->
      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full whitespace-nowrap">
            <thead>
              <tr class="bg-surface-container-low border-b border-border">
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[120px]">中文描述</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[120px]">值</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[150px]">备注说明</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[140px]">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in dictList" :key="item.id || index" class="border-b border-border hover:bg-surface-container-low/50 transition-colors">
                <td class="p-4 font-body-md text-on-surface">{{ item.dictLabel }}</td>
                <td class="p-4 font-body-md text-on-surface-variant">{{ formatDictValue(item) }}</td>
                <td class="p-4 font-body-md text-on-surface-variant max-w-[200px] truncate" :title="item.remark">{{ item.remark || '-' }}</td>
                <td class="p-4">
                  <div class="flex items-center gap-2">
                    <button class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded hover:bg-primary/10 transition-colors" @click="openEdit(item)">
                      <span class="material-symbols-outlined text-[14px]">edit</span>
                      编辑
                    </button>
                    <button v-if="isAdminId1" class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-error bg-error/5 rounded hover:bg-error/10 transition-colors" @click="handleDelete(item)">
                      <span class="material-symbols-outlined text-[14px]">delete</span>
                      删除
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="dictList.length === 0">
                <td colspan="4" class="p-12 text-center">
                  <div class="flex flex-col items-center gap-2 text-on-surface-variant">
                    <span class="material-symbols-outlined text-[48px] opacity-20">settings</span>
                    <p class="text-body-md">暂无配置数据</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Add Dialog -->
      <div v-if="addVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="addVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">add_circle</span>
              新增字典
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="addVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">中文描述 <span class="text-error">*</span></label>
              <input v-model="addForm.dictLabel" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="请输入描述" maxlength="100">
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">字典类型 <span class="text-error">*</span></label>
              <input v-model="addForm.dictType" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="请输入字典类型" maxlength="100">
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">值 <span class="text-error">*</span></label>
              <input v-model="addForm.dictValue" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="请输入值" maxlength="100">
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">排序序号</label>
              <input v-model.number="addForm.dictSort" type="number" min="0" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md">
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">备注说明</label>
              <textarea v-model="addForm.remark" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md resize-none" rows="3" placeholder="请输入备注说明" maxlength="200"></textarea>
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
        <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">edit</span>
              编辑字典
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="editVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5 space-y-4">
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">ID</label>
              <input :value="editForm.id" class="w-full px-4 py-2.5 bg-surface-variant border border-outline-variant rounded font-body-md text-body-md text-on-surface-variant" disabled>
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">中文描述</label>
              <input :value="editForm.dictLabel" class="w-full px-4 py-2.5 bg-surface-variant border border-outline-variant rounded font-body-md text-body-md text-on-surface-variant" disabled>
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">值 <span class="text-error">*</span></label>
              <select v-if="isEditSwitchType" v-model="editForm.dictValue" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md">
                <option value="0">关</option>
                <option value="1">开</option>
              </select>
              <input v-else-if="isEditDateType" v-model="editForm.dictValue" type="date" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md">
              <input v-else v-model="editForm.dictValue" class="w-full px-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="请输入值" maxlength="100">
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
  name: 'StitchDict',
  data() {
    return {
      dictList: [],
      addVisible: false,
      editVisible: false,
      addForm: { dictType: '', dictValue: '', dictLabel: '', dictSort: 0, remark: '' },
      editForm: { id: null, dictType: '', dictValue: '', dictLabel: '', dictSort: 0, remark: '' }
    }
  },
  computed: {
    isEditSwitchType() {
      return (this.editForm.dictType || '').toString().trim().toLowerCase() === 'switch'
    },
    isEditDateType() {
      const type = (this.editForm.dictType || '').toString().trim().toLowerCase()
      return type === 'points-start-time' || type === 'point-end-time'
    },
    isAdminId1() {
      try {
        const admin = window.sessionStorage.getItem('admin')
        if (admin) {
          const obj = JSON.parse(admin)
          return obj && (obj.id === 1 || obj.id === '1')
        }
      } catch (e) {}
      return false
    }
  },
  mounted() { this.loadDictList() },
  methods: {
    formatDictValue(row) {
      if (!row) return ''
      const type = (row.dictType || '').toString().trim().toLowerCase()
      const val = (row.dictValue != null ? row.dictValue : '').toString().trim()
      if (type === 'switch') return val === '0' ? '关' : val === '1' ? '开' : val
      return val
    },
    getCurrentUsername() {
      try {
        const admin = window.sessionStorage.getItem('admin')
        if (admin) return JSON.parse(admin).username || ''
      } catch (e) {}
      return ''
    },
    loadDictList() {
      this.postRequest('/admin/listDict', {}).then(resp => {
        if (resp) {
          const list = resp.obj || resp.data || resp.list || (Array.isArray(resp) ? resp : [])
          this.dictList = Array.isArray(list) ? list : []
        }
      })
    },
    openAdd() {
      this.addForm = { dictType: '', dictValue: '', dictLabel: '', dictSort: 0, remark: '' }
      this.addVisible = true
    },
    submitAdd() {
      const dictType = (this.addForm.dictType || '').trim()
      const dictValue = (this.addForm.dictValue || '').trim()
      const dictLabel = (this.addForm.dictLabel || '').trim()
      if (!dictType) { this.$message.warning('字典类型不能为空'); return }
      if (!dictValue) { this.$message.warning('值不能为空'); return }
      if (!dictLabel) { this.$message.warning('中文描述不能为空'); return }
      this.postRequest('/admin/addDict', {
        dictType, dictValue, dictLabel, dictSort: Number(this.addForm.dictSort) || 0,
        createBy: this.getCurrentUsername(), remark: (this.addForm.remark || '').trim()
      }).then(resp => {
        if (resp) { this.$message.success('添加成功'); this.addVisible = false; this.loadDictList() }
      })
    },
    openEdit(row) {
      if (!row) return
      const dictType = (row.dictType || '').toString().trim()
      const isSwitch = dictType.toLowerCase() === 'switch'
      const rawVal = row.dictValue
      this.editForm = {
        id: row.id, dictType, dictLabel: row.dictLabel || '',
        dictValue: (rawVal != null && rawVal !== '') ? String(rawVal) : (isSwitch ? '0' : ''),
        dictSort: Number(row.dictSort) || 0, remark: row.remark || ''
      }
      this.editVisible = true
    },
    submitEdit() {
      const { id, dictType, dictValue, dictLabel, dictSort, remark } = this.editForm
      if (id == null || id === '') { this.$message.warning('ID不能为空'); return }
      if (!dictType) { this.$message.warning('字典类型不能为空'); return }
      if (!dictValue) { this.$message.warning('值不能为空'); return }
      if (!dictLabel) { this.$message.warning('中文描述不能为空'); return }
      if (dictSort === '' || isNaN(dictSort) || dictSort < 0) { this.$message.warning('排序序号无效'); return }
      this.postRequest('/admin/updateDict', {
        id, dictType, dictValue, dictLabel, dictSort: Number(dictSort),
        updateBy: this.getCurrentUsername(), remark: (remark || '').trim()
      }).then(resp => {
        if (resp) { this.$message.success('修改成功'); this.editVisible = false; this.loadDictList() }
      })
    },
    handleDelete(row) {
      if (!row || (row.id == null)) { this.$message.warning('无法获取记录ID'); return }
      this.$confirm('确定要删除该字典项吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/deleteDict', { id: row.id }).then(resp => {
          if (resp) { this.$message.success('删除成功'); this.loadDictList() }
        })
      }).catch(() => {})
    }
  }
}
</script>
