<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2"><span class="material-symbols-outlined text-primary">upload_file</span>组织导入</h1>
          <p class="text-body-md text-secondary mt-1">上传组织文件 → 校验/差异预览 → 安全自动应用；有移动/停用等不安全变更时整批停止</p>
        </div>
        <div class="flex gap-2">
          <button class="px-4 py-2 bg-surface-variant text-on-surface rounded-lg text-label-md" @click="downloadTemplate">下载模板</button>
          <button class="px-4 py-2 bg-primary text-on-primary rounded-lg text-label-md" @click="$refs.fileInput.click()">上传组织文件</button>
          <input ref="fileInput" type="file" accept=".xlsx" class="hidden" @change="handleFile">
        </div>
      </div>

      <div v-if="result" class="bg-container border rounded-xl p-card-padding mb-5" :class="result.safe ? 'border-green-200' : 'border-amber-200'">
        <div class="flex items-center gap-3 mb-3">
          <span class="material-symbols-outlined" :class="result.safe ? 'text-green-600' : 'text-amber-600'">{{ result.safe ? 'check_circle' : 'warning' }}</span>
          <h2 class="font-headline-sm text-headline-sm">{{ result.safe ? '校验通过，可应用' : '存在不安全变更，已停止自动应用' }}</h2>
          <span class="ml-auto text-[12px] text-on-surface-variant">批次 #{{ result.batch.id }}</span>
        </div>
        <p v-if="result.bulkChange" class="text-[12px] text-amber-700 mb-2">
          移动/停用 {{ result.unsafeCount }} 条，占 {{ result.unsafeRatio }}%，超过 10% 阈值，请逐条核对后再确认。
        </p>
        <div class="grid grid-cols-2 md:grid-cols-5 gap-3 text-center mb-4">
          <div class="bg-surface rounded-lg p-2"><p class="text-xl font-bold">{{ result.batch.totalCount }}</p><p class="text-[11px] text-outline">总行数</p></div>
          <div class="bg-surface rounded-lg p-2"><p class="text-xl font-bold text-green-600">{{ result.batch.addCount }}</p><p class="text-[11px] text-outline">新增</p></div>
          <div class="bg-surface rounded-lg p-2"><p class="text-xl font-bold text-blue-600">{{ result.batch.updateCount }}</p><p class="text-[11px] text-outline">改名</p></div>
          <div class="bg-surface rounded-lg p-2"><p class="text-xl font-bold text-amber-600">{{ result.batch.moveCount }}</p><p class="text-[11px] text-outline">移动</p></div>
          <div class="bg-surface rounded-lg p-2"><p class="text-xl font-bold text-error">{{ result.batch.disableCount }}</p><p class="text-[11px] text-outline">停用</p></div>
        </div>
        <div class="flex justify-end gap-2">
          <button v-if="!result.safe" class="px-4 py-2 border border-amber-400 text-amber-700 rounded-lg text-[12px]" @click="apply(result.batch.id, true)">我已确认，手动应用</button>
          <button v-if="result.safe" class="px-4 py-2 bg-primary text-white rounded-lg text-[12px]" @click="apply(result.batch.id, false)">应用本批次</button>
        </div>
        <div class="mt-4 max-h-64 overflow-y-auto border border-outline-variant rounded-lg">
          <table class="w-full text-[12px]">
            <thead><tr class="bg-surface-container-low"><th class="p-2 text-left">动作</th><th class="p-2 text-left">组织编号</th><th class="p-2 text-left">状态</th></tr></thead>
            <tbody><tr v-for="i in result.items" :key="i.id" class="border-t border-outline-variant/40"><td class="p-2">{{ i.action }}</td><td class="p-2">{{ i.orgNo }}</td><td class="p-2">{{ i.status }}</td></tr></tbody>
          </table>
        </div>
      </div>

      <div class="bg-container border border-border rounded-xl p-card-padding">
        <h2 class="font-headline-sm mb-3">最近批次</h2>
        <table class="w-full text-[12px]">
          <thead><tr class="bg-surface-container-low text-on-surface-variant"><th class="p-2 text-left">ID</th><th class="p-2 text-left">文件</th><th class="p-2 text-left">状态</th><th class="p-2 text-left">时间</th><th class="p-2 text-left">操作</th></tr></thead>
          <tbody>
            <tr v-for="b in batches" :key="b.id" class="border-t border-outline-variant/40">
              <td class="p-2">{{ b.id }}</td><td class="p-2">{{ b.fileName }}</td><td class="p-2">{{ b.status }}</td><td class="p-2">{{ b.startTime }}</td>
              <td class="p-2"><button class="text-primary text-[12px]" @click="loadDetail(b.id)">查看</button><button v-if="b.status==='APPLIED'" class="text-error text-[12px] ml-2" @click="rollback(b.id)">回滚</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
export default {
  name: 'OrgImportPage',
  data() { return { result: null, batches: [] } },
  mounted() { this.loadBatches() },
  methods: {
    async handleFile(event) {
      const file = event.target.files && event.target.files[0]
      if (!file) return
      const formData = new FormData(); formData.append('file', file)
      try {
        const res = await this.uploadFile('/admin/org/import/validate', formData)
        if (res && res.obj) {
          this.result = res.obj
          this.$message[res.obj.safe ? 'success' : 'warning']((res && res.message) || '校验完成')
          this.loadBatches()
        } else this.$message.error((res && res.message) || '校验失败')
      } catch (e) { this.$message.error('校验失败') }
      event.target.value = ''
    },
    async apply(batchId, confirm) {
      const res = await this.postRequest('/admin/org/import/apply', { batchId, confirm })
      handleResponse(res, { successMsg: '应用成功', errorMsg: '应用失败', onSuccess: () => { this.result = null; this.loadBatches() } })
    },
    async loadBatches() {
      const res = await this.getRequestUrl('/admin/org/import/batches')
      this.batches = (res && res.obj) || []
    },
    async loadDetail(id) {
      const res = await this.getRequestUrl(`/admin/org/import/batch/${id}`)
      this.result = { batch: this.batches.find(b => b.id === id) || { id }, items: (res && res.obj) || [], safe: true }
    },
    async rollback(id) {
      this.$confirm('确定回滚该批次吗？', '回滚组织导入', { type: 'warning' }).then(async () => {
        const res = await this.postRequest('/admin/org/import/rollback', { batchId: id })
        handleResponse(res, { successMsg: '已回滚', errorMsg: '回滚失败', onSuccess: () => this.loadBatches() })
      }).catch(() => {})
    },
    async downloadTemplate() {
      const res = await this.getRequestUrl('/admin/org/import/template')
      if (!res || !res.obj) return
      const bytes = Uint8Array.from(atob(res.obj), c => c.charCodeAt(0))
      const blob = new Blob([bytes], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const url = URL.createObjectURL(blob); const a = document.createElement('a')
      a.href = url; a.download = '组织导入模板.xlsx'; a.click(); URL.revokeObjectURL(url)
    },
  },
}
</script>
