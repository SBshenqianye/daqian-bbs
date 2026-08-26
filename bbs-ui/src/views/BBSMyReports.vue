<template>
  <main class="max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen">
    <header class="mb-8">
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的举报</h1>
    </header>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <template v-if="!loading">
      <div v-if="list.length > 0" class="space-y-gutter">
        <div v-for="item in list" :key="item.id" class="bg-container border border-outline-variant rounded-lg p-card-padding">
          <div class="flex items-start justify-between">
            <div>
              <div class="flex items-center gap-2 mb-2">
                <span :class="{
                  'px-2 py-0.5 rounded text-[12px] font-medium': true,
                  'bg-yellow-100 text-yellow-800': item.status === 'pending',
                  'bg-green-100 text-green-800': item.status === 'confirmed',
                  'bg-red-100 text-red-800': item.status === 'rejected'
                }">{{ getStatusLabel(item.status) }}</span>
                <span class="font-body-sm text-on-surface-variant">{{ item.targetType }} #{{ item.targetId }}</span>
              </div>
              <p class="font-body-md text-body-md text-on-surface">{{ item.reason || '无原因说明' }}</p>
              <p v-if="item.reviewRemark" class="font-body-sm text-body-sm text-on-surface-variant mt-2">审核意见: {{ item.reviewRemark }}</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">{{ item.createTime }}</p>
            </div>
            <span v-if="item.status === 'confirmed' && item.pointsAwarded" class="font-body-sm text-green-600">+2 积分</span>
          </div>
        </div>
      </div>
      <div v-else class="flex flex-col items-center justify-center py-24 text-on-surface-variant">
        <span class="material-symbols-outlined text-6xl mb-4 opacity-20">flag</span>
        <p class="font-body-lg text-body-lg">暂无举报记录</p>
      </div>
    </template>
  </main>
</template>

<script>
export default {
  name: 'BBSMyReports',
  data() { return { loading: false, list: [] } },
  mounted() { this.loadList() },
  methods: {
    getUserId() { return JSON.parse(localStorage.getItem('user') || '{}').id },
    getStatusLabel(s) { return { pending: '审核中', confirmed: '已核实', rejected: '已驳回' }[s] || s },
    async loadList() {
      const userId = this.getUserId()
      if (!userId) return
      this.loading = true
      try {
        const res = await this.postRequest('/user/report/myList', { reporterId: userId, page: 1, size: 100 })
        if (res && res.code == 200 && res.obj) this.list = res.obj.records || []
        else this.list = []
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    }
  }
}
</script>
