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
        <!-- Add Row -->
        <div class="flex items-center gap-3 mb-6">
          <div class="flex-1 relative">
            <span class="material-symbols-outlined absolute left-3 inset-y-0 flex items-center text-outline text-[18px]">add_circle</span>
            <input v-model="keyword" class="w-full pl-9 pr-4 py-2.5 bg-surface border border-outline-variant rounded focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-body-md text-body-md" placeholder="输入敏感词，按回车添加" @keyup.enter="handleAdd">
          </div>
          <button class="inline-flex items-center gap-1.5 px-5 py-2.5 bg-primary text-on-primary rounded-lg hover:opacity-90 transition-all font-label-md text-label-md disabled:opacity-60" :disabled="adding" @click="handleAdd">
            <span v-if="adding" class="inline-block w-4 h-4 border-2 border-on-primary/30 border-t-on-primary rounded-full animate-spin"></span>
            <span v-else class="material-symbols-outlined text-[18px]">add</span>
            {{ adding ? '添加中...' : '添加' }}
          </button>
        </div>

        <!-- List -->
        <div class="border border-outline-variant rounded-lg overflow-hidden" v-loading="loading">
          <div v-if="!list || list.length === 0" class="py-12 text-center flex flex-col items-center gap-2 text-on-surface-variant">
            <span class="material-symbols-outlined text-[48px] opacity-20">shield</span>
            <p class="text-body-md">暂无敏感词</p>
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
      list: []
    }
  },
  mounted() { this.loadList() },
  methods: {
    async loadList() {
      if (typeof this.getRequestUrl !== 'function') { this.list = []; return }
      this.loading = true
      try {
        const res = await this.getRequestUrl('/sensitiveWord/getList')
        if (res && res.code == 200) {
          this.list = Array.isArray(res.obj) ? res.obj : []
        } else {
          this.list = []
          if (res && res.message) this.$message.error(res.message)
        }
      } catch (e) { this.list = [] }
      finally { this.loading = false }
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
          await this.loadList()
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
          await this.loadList()
        } else {
          this.$message.error((res && res.message) ? res.message : '删除失败')
        }
      } catch (e) { this.$message.error('删除失败') }
      finally { this.deletingId = null }
    }
  }
}
</script>
