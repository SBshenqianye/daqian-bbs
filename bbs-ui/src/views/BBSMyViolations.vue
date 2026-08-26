<template>
  <main class="max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen">
    <header class="mb-8">
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的违规记录</h1>
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
                <span class="material-symbols-outlined text-error text-[20px]">gavel</span>
                <span class="font-body-md font-semibold text-on-surface">{{ getViolationLabel(item.violationType) }}</span>
              </div>
              <p class="font-body-sm text-body-sm text-on-surface-variant">{{ item.remark || '无备注' }}</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">{{ item.createTime }}</p>
            </div>
            <span class="font-headline-sm text-headline-sm text-red-600">-{{ item.pointsDeducted }}</span>
          </div>
        </div>
      </div>
      <div v-else class="flex flex-col items-center justify-center py-24 text-on-surface-variant">
        <span class="material-symbols-outlined text-6xl mb-4 opacity-20">verified</span>
        <p class="font-body-lg text-body-lg">暂无违规记录</p>
        <p class="font-body-md text-body-md text-outline mt-1">继续保持良好表现</p>
      </div>
    </template>
  </main>
</template>

<script>
export default {
  name: 'BBSMyViolations',
  data() { return { loading: false, list: [] } },
  mounted() { this.loadList() },
  methods: {
    getUserId() { return JSON.parse(localStorage.getItem('user') || '{}').id },
    getViolationLabel(t) {
      return { illegal: '违法违规内容', attack: '人身攻击', spam: '恶意灌水', plagiarism: '抄袭剽窃', false_report: '虚假举报', leak: '泄露秘密' }[t] || t
    },
    async loadList() {
      const userId = this.getUserId()
      if (!userId) return
      this.loading = true
      try {
        const res = await this.postRequest('/user/violation/myList', { userId, page: 1, size: 100 })
        if (res && res.code == 200 && res.obj) this.list = res.obj.records || []
        else this.list = []
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    }
  }
}
</script>
