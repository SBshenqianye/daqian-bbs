<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">settings</span>
            系统配置
          </h1>
          <p class="text-body-md text-secondary mt-1">管理系统的扩展配置项</p>
        </div>
      </div>

      <!-- Contact Settings Card -->
      <div class="bg-container border border-border rounded-xl p-card-padding mb-6">
        <div class="flex items-center gap-2 mb-4 pb-3 border-b border-outline-variant">
          <span class="material-symbols-outlined text-primary">contact_mail</span>
          <h2 class="font-headline-sm text-headline-sm text-on-surface">联系方式设置</h2>
          <span class="text-[11px] text-on-surface-variant ml-auto">用于用户端"使用反馈"弹窗</span>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">联系人姓名</label>
            <input v-model="contactForm.name" class="w-full px-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="如 张三" maxlength="50">
          </div>
          <div>
            <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1.5 block">联系邮箱</label>
            <input v-model="contactForm.email" class="w-full px-4 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="如 zhangsan@company.com" maxlength="200">
          </div>
        </div>
        <div class="flex justify-end mt-4">
          <button class="inline-flex items-center gap-1.5 px-5 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="saveContactInfo" :disabled="contactSaving">
            {{ contactSaving ? '保存中...' : '保存联系方式' }}
          </button>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex items-center gap-3 mb-6">
        <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md" @click="openAdd">
          <span class="material-symbols-outlined text-[18px]">add</span>
          新增配置
        </button>
        <button class="inline-flex items-center gap-1.5 px-4 py-2 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant transition-all font-label-md text-label-md" @click="loadList">
          <span class="material-symbols-outlined text-[18px]">refresh</span>
          刷新
        </button>
      </div>

      <!-- Config Table -->
      <div class="bg-container border border-border rounded-xl overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full whitespace-nowrap">
            <thead>
              <tr class="bg-surface-container-low border-b border-border">
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[100px]">分组</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[120px]">配置名称</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[100px]">配置键</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[200px]">配置值</th>
                <th class="p-4 text-left font-label-md text-label-md text-on-surface-variant min-w-[140px]">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in configList" :key="item.id || index" class="border-b border-border hover:bg-surface-container-low/50 transition-colors">
                <td class="p-4">
                  <span class="inline-flex px-2 py-0.5 text-[11px] font-medium bg-primary/10 text-primary rounded-full">{{ item.configGroup || '-' }}</span>
                </td>
                <td class="p-4 font-body-md text-on-surface">{{ item.configLabel || '-' }}</td>
                <td class="p-4 font-body-md text-on-surface-variant">
                  <code class="text-[12px] bg-surface-variant px-1.5 py-0.5 rounded">{{ item.configKey }}</code>
                </td>
                <td class="p-4 font-body-md text-on-surface-variant max-w-[300px]">
                  <div class="truncate" :title="item.configValue">{{ item.configValue || '-' }}</div>
                </td>
                <td class="p-4">
                  <div class="flex items-center gap-2">
                    <button class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-primary bg-primary/5 rounded hover:bg-primary/10 transition-colors" @click="openEdit(item)">
                      <span class="material-symbols-outlined text-[14px]">edit</span>
                      编辑
                    </button>
                    <button class="inline-flex items-center gap-1 px-2.5 py-1.5 text-[12px] font-medium text-error bg-error/5 rounded hover:bg-error/10 transition-colors" @click="handleDelete(item)">
                      <span class="material-symbols-outlined text-[14px]">delete</span>
                      删除
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="configList.length === 0">
                <td colspan="5" class="p-12 text-center">
                  <div class="flex flex-col items-center gap-2 text-on-surface-variant">
                    <span class="material-symbols-outlined text-[48px] opacity-20">settings</span>
                    <p class="text-body-md">暂无配置数据</p>
                    <p class="text-body-sm text-outline mt-1">点击上方"新增配置"按钮添加</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Add Dialog -->
      <div v-if="addVisible" class="fixed inset-0 z-50 flex items-start justify-center p-4 pt-[10vh]" @click.self="addVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl overflow-hidden max-h-[80vh] flex flex-col">
          <div class="flex items-center justify-between p-4 border-b border-outline-variant shrink-0">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">add_circle</span>
              新增配置
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="addVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-4 space-y-3 overflow-y-auto">
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1 block">配置键 <span class="text-error">*</span></label>
              <input v-model="addForm.configKey" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="如 feedback_contact" maxlength="100">
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1 block">配置名称 <span class="text-error">*</span></label>
              <input v-model="addForm.configLabel" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="如 使用反馈联系方式" maxlength="100">
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1 block">配置分组</label>
              <input v-model="addForm.configGroup" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md" placeholder="如 contact / system" maxlength="100">
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1 block">配置值 <span class="text-error">*</span></label>
              <textarea v-model="addForm.configValue" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md resize-none" rows="3" placeholder="支持文本或 JSON 格式" maxlength="5000"></textarea>
            </div>
          </div>
          <div class="flex justify-end gap-3 p-4 border-t border-outline-variant bg-surface-container-lowest shrink-0">
            <button class="px-5 py-2 border border-outline rounded text-on-surface hover:bg-surface-variant transition-all font-label-md text-label-md" @click="addVisible = false">取消</button>
            <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="submitAdd">确定</button>
          </div>
        </div>
      </div>

      <!-- Edit Dialog -->
      <div v-if="editVisible" class="fixed inset-0 z-50 flex items-start justify-center p-4 pt-[10vh]" @click.self="editVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-lg rounded-xl shadow-2xl overflow-hidden max-h-[80vh] flex flex-col">
          <div class="flex items-center justify-between p-4 border-b border-outline-variant shrink-0">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">edit</span>
              编辑配置
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="editVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-4 space-y-3 overflow-y-auto">
            <div class="flex items-center gap-3 text-body-md text-on-surface-variant bg-surface-variant rounded px-3 py-2">
              <span class="font-label-md shrink-0">配置键：</span>
              <span>{{ editForm.configKey }}</span>
              <span class="mx-2 text-outline-variant">|</span>
              <span class="font-label-md shrink-0">名称：</span>
              <span>{{ editForm.configLabel }}</span>
              <span class="mx-2 text-outline-variant">|</span>
              <span class="font-label-md shrink-0">分组：</span>
              <span>{{ editForm.configGroup }}</span>
            </div>
            <div>
              <label class="font-label-md text-label-md text-secondary ml-0.5 mb-1 block">配置值 <span class="text-error">*</span></label>
              <textarea v-model="editForm.configValue" class="w-full px-3 py-2 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none font-body-md text-body-md resize-none" rows="4" maxlength="5000"></textarea>
            </div>
          </div>
          <div class="flex justify-end gap-3 p-4 border-t border-outline-variant bg-surface-container-lowest shrink-0">
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
  name: 'SystemConfigPage',
  data() {
    return {
      configList: [],
      addVisible: false,
      editVisible: false,
      addForm: { configKey: '', configValue: '', configLabel: '', configGroup: 'default', configType: 'text', sortOrder: 0, remark: '' },
      editForm: { id: null, configKey: '', configValue: '', configLabel: '', configGroup: '', configType: 'text', sortOrder: 0, remark: '' },
      contactForm: { name: '', email: '' },
      contactSaving: false,
    }
  },
  mounted() { this.loadList(); this.loadContactInfo() },
  methods: {
    loadList() {
      this.postRequest('/admin/systemConfig/list', {}).then(resp => {
        if (resp) {
          const list = resp.obj || resp.data || resp.list || (Array.isArray(resp) ? resp : [])
          this.configList = Array.isArray(list) ? list : []
        }
      })
    },
    openAdd() {
      this.addForm = { configKey: '', configValue: '', configLabel: '', configGroup: 'default', configType: 'text', sortOrder: 0, remark: '' }
      this.addVisible = true
    },
    submitAdd() {
      const { configKey, configValue, configLabel } = this.addForm
      if (!configKey.trim()) { this.$message.warning('配置键不能为空'); return }
      if (!configLabel.trim()) { this.$message.warning('配置名称不能为空'); return }
      if (!configValue.trim()) { this.$message.warning('配置值不能为空'); return }
      this.postRequest('/admin/systemConfig/add', {
        ...this.addForm,
        configKey: configKey.trim(),
        configLabel: configLabel.trim(),
        configGroup: (this.addForm.configGroup || 'default').trim(),
        sortOrder: Number(this.addForm.sortOrder) || 0,
      }).then(resp => {
        if (resp) { this.$message.success('添加成功'); this.addVisible = false; this.loadList() }
      })
    },
    openEdit(row) {
      if (!row) return
      this.editForm = {
        id: row.id,
        configKey: row.configKey || '',
        configValue: row.configValue || '',
        configLabel: row.configLabel || '',
        configGroup: row.configGroup || '',
        configType: row.configType || 'text',
        sortOrder: Number(row.sortOrder) || 0,
        remark: row.remark || ''
      }
      this.editVisible = true
    },
    submitEdit() {
      const { id, configValue } = this.editForm
      if (id == null) { this.$message.warning('ID不能为空'); return }
      if (!configValue.trim()) { this.$message.warning('配置值不能为空'); return }
      this.postRequest('/admin/systemConfig/update', {
        id, configValue: configValue.trim()
      }).then(resp => {
        if (resp) { this.$message.success('修改成功'); this.editVisible = false; this.loadList() }
      })
    },
    handleDelete(row) {
      if (!row || row.id == null) { this.$message.warning('无法获取记录ID'); return }
      this.$confirm('确定要删除该配置项吗？', '提示', { type: 'warning' }).then(() => {
        this.postRequest('/admin/systemConfig/delete', { id: row.id }).then(resp => {
          if (resp) { this.$message.success('删除成功'); this.loadList() }
        })
      }).catch(() => {})
    },
    loadContactInfo() {
      this.postRequest('/common/systemConfig/listByGroup', { configGroup: 'contact' }).then(resp => {
        if (resp && resp.obj && Array.isArray(resp.obj)) {
          const item = resp.obj.find(c => c.configKey === 'feedback_contact')
          if (item && item.configValue) {
            try {
              const parsed = JSON.parse(item.configValue)
              this.contactForm.name = parsed.name || ''
              this.contactForm.email = parsed.email || ''
            } catch (e) {
              // ignore parse error
            }
          }
        }
      })
    },
    saveContactInfo() {
      if (!this.contactForm.name.trim() && !this.contactForm.email.trim()) {
        this.$message.warning('请填写至少一项联系信息')
        return
      }
      this.contactSaving = true
      const value = JSON.stringify({ name: this.contactForm.name.trim(), email: this.contactForm.email.trim() })
      // 先查是否存在
      this.postRequest('/common/systemConfig/listByGroup', { configGroup: 'contact' }).then(resp => {
        if (resp && resp.obj && Array.isArray(resp.obj)) {
          const item = resp.obj.find(c => c.configKey === 'feedback_contact')
          if (item && item.id) {
            // 更新
            return this.postRequest('/admin/systemConfig/update', { id: item.id, configValue: value })
          }
        }
        // 新增
        return this.postRequest('/admin/systemConfig/add', {
          configKey: 'feedback_contact',
          configValue: value,
          configLabel: '使用反馈联系方式',
          configGroup: 'contact',
          configType: 'json',
          sortOrder: 0,
          remark: '配置使用反馈弹窗中的联系人信息'
        })
      }).then(resp => {
        if (resp) { this.$message.success('联系方式已保存'); this.loadList(); this.loadContactInfo() }
      }).catch(() => {}).finally(() => { this.contactSaving = false })
    }
  }
}
</script>
