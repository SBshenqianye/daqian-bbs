<template>
  <div class="bg-surface min-h-screen">
    <div class="max-w-7xl mx-auto px-page-margin-desktop py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface flex items-center gap-2">
            <span class="material-symbols-outlined text-error">warning</span>
            敏感词管理
          </h1>
          <p class="text-body-md text-secondary mt-1">管理论坛敏感词过滤列表</p>
        </div>
      </div>

      <!-- Card -->
      <div class="bg-container border border-border rounded-xl p-card-padding">
        <!-- Toolbar Row -->
        <div class="flex flex-wrap items-center gap-3 mb-6">
          <!-- Add Input -->
          <div class="flex-1 min-w-[200px] grid grid-cols-1 grid-rows-1">
            <input v-model="keyword" class="w-full col-start-1 row-start-1 pl-9 pr-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md" placeholder="输入敏感词，按回车添加" @keyup.enter="handleAdd">
            <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[18px] pointer-events-none">add_circle</span>
          </div>
          <button class="inline-flex items-center gap-1.5 px-5 py-2.5 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md disabled:opacity-60" :disabled="adding" @click="handleAdd">
            <span v-if="adding" class="inline-block w-4 h-4 border-2 border-on-primary/30 border-t-on-primary rounded-full animate-spin"></span>
            <span v-else class="material-symbols-outlined text-[18px]">add</span>
            {{ adding ? '添加中...' : '添加' }}
          </button>

          <div class="h-6 w-px bg-outline-variant hidden sm:block"></div>

          <!-- Search Input -->
          <div class="grid grid-cols-1 grid-rows-1">
            <input v-model="searchKeyword" class="w-full col-start-1 row-start-1 pl-9 pr-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md" placeholder="搜索敏感词..." @keyup.enter="handleSearch">
            <span class="material-symbols-outlined col-start-1 row-start-1 self-center ml-3 text-outline text-[18px] pointer-events-none">search</span>
          </div>

          <div class="h-6 w-px bg-outline-variant hidden sm:block"></div>

          <!-- Export Button -->
          <button class="inline-flex items-center gap-1.5 px-4 py-2.5 bg-success/10 text-success rounded-lg hover:bg-success/20 transition-all font-label-md text-label-md" @click="handleExport">
            <span class="material-symbols-outlined text-[18px]">download</span>
            导出
          </button>

          <!-- Import Button -->
          <button class="inline-flex items-center gap-1.5 px-4 py-2.5 bg-primary/10 text-primary rounded-lg hover:bg-primary/20 transition-all font-label-md text-label-md" @click="triggerImport">
            <span class="material-symbols-outlined text-[18px]">upload</span>
            导入
          </button>
          <input ref="importInput" type="file" accept=".xlsx,.xls" class="hidden" @change="handleImport">

          <!-- Refresh -->
          <button class="inline-flex items-center gap-1.5 px-4 py-2.5 bg-surface-variant text-on-surface rounded-lg hover:bg-outline-variant transition-all font-label-md text-label-md" @click="loadPage">
            <span class="material-symbols-outlined text-[18px]">refresh</span>
          </button>
        </div>

        <!-- List -->
        <div class="border border-outline-variant rounded-lg overflow-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0 && !loading" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-[48px] opacity-20">shield</span>
            <p class="text-body-md">{{ searchKeyword ? '未找到匹配的敏感词' : '暂无敏感词' }}</p>
          </div>
          <div v-else class="divide-y divide-outline-variant/50">
            <div v-for="item in list" :key="item.id" class="flex items-center justify-between px-5 py-4 hover:bg-surface-container-low/50 transition-colors">
              <span class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
                <span class="material-symbols-outlined text-error text-[18px]">block</span>
                {{ item.keyword }}
              </span>
              <button class="inline-flex items-center gap-1 px-3 py-1.5 text-[12px] font-medium text-error bg-error/5 rounded hover:bg-error/10 transition-colors disabled:opacity-50" :disabled="deletingId === item.id" @click="handleDelete(item)">
                <span v-if="deletingId === item.id" class="inline-block w-3 h-3 border-2 border-error/30 border-t-error rounded-full animate-spin"></span>
                <span v-else class="material-symbols-outlined text-[14px]">delete</span>
                删除
              </button>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="total > 0" class="flex items-center justify-between mt-4 pt-4 border-t border-outline-variant">
          <span class="text-body-sm text-secondary">共 {{ total }} 条</span>
          <el-pagination
            background
            layout="prev, pager, next, sizes"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>

      <!-- Import Result Dialog -->
      <div v-if="importResultVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="importResultVisible = false">
        <div class="fixed inset-0 bg-black/30"></div>
        <div class="relative bg-container w-full max-w-md rounded-xl shadow-2xl overflow-hidden">
          <div class="flex items-center justify-between p-5 border-b border-outline-variant">
            <h3 class="font-headline-sm text-headline-sm text-on-surface flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">upload_file</span>
              导入结果
            </h3>
            <button class="text-outline hover:text-error transition-colors" @click="importResultVisible = false">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="p-5">
            <div v-if="importResult" class="space-y-3">
              <div class="flex items-center justify-between py-2 border-b border-outline-variant/50">
                <span class="text-body-md text-secondary">总行数</span>
                <span class="font-headline-sm text-on-surface">{{ importResult.totalCount }}</span>
              </div>
              <div class="flex items-center justify-between py-2 border-b border-outline-variant/50">
                <span class="text-body-md text-success">新增</span>
                <span class="font-headline-sm text-success">+{{ importResult.addedCount }}</span>
              </div>
              <div class="flex items-center justify-between py-2 border-b border-outline-variant/50">
                <span class="text-body-md text-warning">重复跳过</span>
                <span class="font-headline-sm text-warning">{{ importResult.duplicateCount }}</span>
              </div>
              <div class="flex items-center justify-between py-2">
                <span class="text-body-md text-secondary">空白跳过</span>
                <span class="font-headline-sm text-on-surface-variant">{{ importResult.skippedCount }}</span>
              </div>
            </div>
            <p v-else class="text-body-md text-secondary text-center py-4">暂无导入结果</p>
          </div>
          <div class="flex justify-end p-5 border-t border-outline-variant bg-surface-container-lowest">
            <button class="px-7 py-2 bg-primary text-on-primary rounded hover:opacity-90 transition-all font-label-md text-label-md shadow-sm" @click="importResultVisible = false">确定</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BBSSensitiveWord',
  data() {
    return {
      loading: false,
      adding: false,
      deletingId: null,
      keyword: '',
      searchKeyword: '',
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 20,
      importing: false,
      importResultVisible: false,
      importResult: null
    }
  },
  mounted() { this.loadPage() },
  methods: {
    async loadPage() {
      if (typeof this.getRequestUrl !== 'function') { this.list = []; return }
      this.loading = true
      try {
        let url = `/sensitiveWord/getPage?page=${this.currentPage}&size=${this.pageSize}`
        if (this.searchKeyword && this.searchKeyword.trim()) {
          url += `&keyword=${encodeURIComponent(this.searchKeyword.trim())}`
        }
        const res = await this.getRequestUrl(url)
        if (res && res.code == 200 && res.obj) {
          this.list = Array.isArray(res.obj.list) ? res.obj.list : []
          this.total = res.obj.total || 0
        } else {
          this.list = []
          this.total = 0
          if (res && res.message) this.$message.error(res.message)
        }
      } catch (e) {
        this.list = []
        this.total = 0
      }
      finally { this.loading = false }
    },
    handleSearch() {
      this.currentPage = 1
      this.loadPage()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadPage()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.loadPage()
    },
    async handleAdd() {
      const kw = (this.keyword || '').trim()
      if (!kw) { this.$message.warning('请输入敏感词'); return }
      if (this.adding) return
      this.adding = true
      try {
        const res = await this.getRequestUrl(`/sensitiveWord/addSensitiveWord?keyword=${encodeURIComponent(kw)}`)
        if (res && res.code == 200) {
          this.$message.success('添加成功')
          this.keyword = ''
          this.currentPage = 1
          await this.loadPage()
        } else {
          this.$message.error((res && res.message) ? res.message : '添加失败')
        }
      } catch (e) { this.$message.error('添加失败') }
      finally { this.adding = false }
    },
    handleDelete(item) {
      if (!item || item.id == null) return
      this.$confirm('确定删除该敏感词吗？', '提示', { type: 'warning' })
        .then(() => this.doDelete(item))
        .catch(() => {})
    },
    async doDelete(item) {
      if (this.deletingId != null) return
      this.deletingId = item.id
      try {
        const res = await this.getRequestUrl(`/sensitiveWord/delSensitiveWord?id=${item.id}`)
        if (res && res.code == 200) {
          this.$message.success('删除成功')
          await this.loadPage()
        } else {
          this.$message.error((res && res.message) ? res.message : '删除失败')
        }
      } catch (e) { this.$message.error('删除失败') }
      finally { this.deletingId = null }
    },
    handleExport() {
      if (this.total === 0) {
        this.$message.warning('没有数据可导出')
        return
      }
      // 使用 downloadFile 下载 Excel
      const token = window.sessionStorage.getItem('tokenStr') || ''
      const url = `${process.env.VUE_APP_BBS_API}/sensitiveWord/export`
      const a = document.createElement('a')
      a.href = url
      a.download = '敏感词列表.xlsx'
      // 需要带 Authorization header，用 fetch + blob 下载
      fetch(url, {
        headers: { 'Authorization': token }
      }).then(res => {
        if (!res.ok) throw new Error('下载失败')
        return res.blob()
      }).then(blob => {
        const blobUrl = window.URL.createObjectURL(blob)
        a.href = blobUrl
        a.click()
        window.URL.revokeObjectURL(blobUrl)
        this.$message.success('导出成功')
      }).catch(e => {
        this.$message.error('导出失败：' + e.message)
      })
    },
    triggerImport() {
      this.$refs.importInput.value = ''
      this.$refs.importInput.click()
    },
    async handleImport(e) {
      const file = e.target.files && e.target.files[0]
      if (!file) return
      const ext = (file.name || '').split('.').pop().toLowerCase()
      if (ext !== 'xlsx' && ext !== 'xls') {
        this.$message.warning('只支持 .xlsx 或 .xls 格式的 Excel 文件')
        return
      }
      if (this.importing) return
      this.importing = true
      this.loading = true
      try {
        const formData = new FormData()
        formData.append('file', file)
        const res = await this.uploadFile('/sensitiveWord/importExcel', formData)
        if (res && res.code == 200) {
          this.$message.success(res.message || '导入成功')
          this.importResult = res.obj
          this.importResultVisible = true
          this.currentPage = 1
          await this.loadPage()
        } else {
          this.$message.error((res && res.message) ? res.message : '导入失败')
        }
      } catch (e) {
        this.$message.error('导入失败')
      } finally {
        this.importing = false
        this.loading = false
      }
    }
  }
}
</script>
