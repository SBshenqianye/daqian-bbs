<template>
  <main class="max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen">
    <header class="mb-8">
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的举报</h1>
    </header>

    <div v-if="loading" class="flex items-center justify-center py-20">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <template v-if="!loading">
      <div v-if="groupedList.length > 0" class="space-y-gutter">
        <div v-for="group in groupedList" :key="group.key" class="bg-container border border-outline-variant rounded-lg p-card-padding">
          <!-- 主记录（该内容最新一条举报） -->
          <div class="flex items-start justify-between">
            <div class="min-w-0">
              <div class="flex items-center gap-2 mb-2 flex-wrap">
                <span :class="statusBadgeClass(group.main.status)">{{ getStatusLabel(group.main.status) }}</span>
                <span class="font-body-sm text-on-surface-variant">{{ getTargetTypeLabel(group.main.targetType) }} #{{ group.main.targetId }}</span>
                <!-- 同一内容的历史举报折叠入口（驳回后重报会产生多条） -->
                <button
                  v-if="group.history.length > 0"
                  class="px-2 py-0.5 bg-blue-50 text-blue-700 rounded text-[11px] font-medium hover:bg-blue-100"
                  @click="toggleGroup(group.key)"
                >
                  {{ isExpanded(group.key) ? '▲ 收起历史' : '历史举报 ' + group.history.length + ' 条 ▼' }}
                </button>
              </div>
              <p class="font-body-md text-body-md text-on-surface">{{ group.main.reason || '无原因说明' }}</p>
              <p v-if="group.main.reviewRemark" class="font-body-sm text-body-sm text-on-surface-variant mt-2">审核意见: {{ group.main.reviewRemark }}</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">{{ group.main.createTime }}</p>
            </div>
            <span v-if="group.main.status === 'confirmed' && group.main.pointsAwarded" class="font-body-sm text-green-600 flex-shrink-0">+2 积分</span>
          </div>

          <!-- 折叠的历史举报记录（同内容更早的记录） -->
          <div v-if="isExpanded(group.key)" class="mt-3 pt-3 border-t border-outline-variant space-y-2">
            <div v-for="item in group.history" :key="item.id" class="flex items-center gap-2 text-[13px]">
              <span :class="statusBadgeClass(item.status)" class="flex-shrink-0">{{ getStatusLabel(item.status) }}</span>
              <span class="text-on-surface-variant flex-1 truncate" :title="item.reason">
                {{ item.reason || '无原因说明' }}<template v-if="item.reviewRemark">（{{ item.reviewRemark }}）</template>
              </span>
              <span class="text-on-surface-variant whitespace-nowrap">{{ item.createTime }}</span>
            </div>
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
  data() { return { loading: false, list: [], expandedKeys: {} } },
  computed: {
    /**
     * 按举报目标（targetType+targetId）分组折叠，避免驳回后重报产生的同内容记录刷屏。
     * 后端按 createTime 倒序返回：首次遇到的是该内容最新一条（main），后续遇到的归入 history。
     */
    groupedList() {
      const groups = []
      const index = {}
      for (const item of this.list) {
        const key = item.targetType + '-' + item.targetId
        if (index[key] === undefined) {
          index[key] = groups.length
          groups.push({ key, main: item, history: [] })
        } else {
          groups[index[key]].history.push(item)
        }
      }
      return groups
    },
  },
  mounted() { this.loadList() },
  methods: {
    getUserId() { return JSON.parse(localStorage.getItem('user') || '{}').id },
    getStatusLabel(s) { return { pending: '审核中', confirmed: '已核实', rejected: '已驳回' }[s] || s },
    getTargetTypeLabel(t) { return { article: '文章', comment: '评论', reply: '回复' }[t] || t },
    statusBadgeClass(s) {
      return {
        'px-2 py-0.5 rounded text-[12px] font-medium': true,
        'bg-yellow-100 text-yellow-800': s === 'pending',
        'bg-green-100 text-green-800': s === 'confirmed',
        'bg-red-100 text-red-800': s === 'rejected'
      }
    },
    isExpanded(key) { return !!this.expandedKeys[key] },
    toggleGroup(key) { this.$set(this.expandedKeys, key, !this.expandedKeys[key]) },
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
