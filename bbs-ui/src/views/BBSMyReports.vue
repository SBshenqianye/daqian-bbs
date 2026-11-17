<template>
  <main :class="embedded ? '' : 'max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen'">
    <header v-if="!embedded" class="mb-8">
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的举报</h1>
    </header>

    <div v-if="loading" class="flex items-center justify-center py-16">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <template v-if="!loading">
      <div v-if="groupedList.length > 0" class="space-y-2">
        <div v-for="group in groupedList" :key="group.key" class="ui-card p-3.5">
          <!-- 主记录（该内容最新一条举报） -->
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="flex items-center gap-1.5 mb-1.5 flex-wrap">
                <span :class="statusBadgeClass(group.main.status)">{{ getStatusLabel(group.main.status) }}</span>
                <span class="ui-meta">{{ getTargetTypeLabel(group.main.targetType) }} #{{ group.main.targetId }}</span>
                <!-- 同一内容的历史举报折叠入口（驳回后重报会产生多条） -->
                <button
                  v-if="group.history.length > 0"
                  class="px-1.5 py-0.5 bg-primary/10 text-primary rounded text-[10.5px] font-medium hover:bg-primary/20"
                  @click="toggleGroup(group.key)"
                >
                  {{ isExpanded(group.key) ? '收起历史' : '历史 ' + group.history.length + ' 条' }}
                  <span class="material-symbols-outlined text-[12px] align-[-2px]">{{ isExpanded(group.key) ? 'expand_less' : 'expand_more' }}</span>
                </button>
              </div>
              <p class="text-[12.5px] text-on-surface leading-relaxed">{{ group.main.reason || '无原因说明' }}</p>
              <p v-if="group.main.reviewRemark" class="text-[11.5px] text-on-surface-variant mt-1">审核意见：{{ group.main.reviewRemark }}</p>
              <p class="ui-meta mt-1"><span class="material-symbols-outlined">schedule</span>{{ group.main.createTime }}</p>
            </div>
            <span v-if="group.main.status === 'confirmed' && group.main.pointsAwarded" class="ui-chip bg-green-50 text-green-700 flex-shrink-0">+2 积分</span>
          </div>

          <!-- 折叠的历史举报记录（同内容更早的记录） -->
          <div v-if="isExpanded(group.key)" class="mt-2.5 pt-2.5 border-t border-outline-variant/50 space-y-1.5">
            <div v-for="item in group.history" :key="item.id" class="flex items-center gap-2 text-[11.5px]">
              <span :class="statusBadgeClass(item.status)" class="flex-shrink-0">{{ getStatusLabel(item.status) }}</span>
              <span class="text-on-surface-variant flex-1 truncate" :title="item.reason">
                {{ item.reason || '无原因说明' }}<template v-if="item.reviewRemark">（{{ item.reviewRemark }}）</template>
              </span>
              <span class="text-outline whitespace-nowrap">{{ item.createTime }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="ui-empty">
        <span class="material-symbols-outlined">flag</span>
        <p class="ui-empty-title">暂无举报记录</p>
        <p class="ui-empty-desc">发现违规内容时，可在帖子或评论上点击举报</p>
      </div>
    </template>
  </main>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import { getUser } from '@/utils/auth'

export default {
  name: 'BBSMyReports',
  props: { embedded: { type: Boolean, default: false } },
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
    getUserId() { const user = getUser(); return user && user.id },
    getStatusLabel(s) { return { pending: '审核中', confirmed: '已核实', rejected: '已驳回' }[s] || s },
    getTargetTypeLabel(t) { return { article: '文章', comment: '评论', reply: '回复' }[t] || t },
    statusBadgeClass(s) {
      return {
        'px-1.5 py-0.5 rounded-md text-[10.5px] font-medium': true,
        'bg-amber-50 text-amber-700': s === 'pending',
        'bg-green-50 text-green-700': s === 'confirmed',
        'bg-red-50 text-red-700': s === 'rejected'
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
        handleResponse(res, {
          silent: true,
          onSuccess: (resp) => { this.list = (resp.obj && resp.obj.records) || [] },
          onError: () => { this.list = [] },
        })
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    }
  }
}
</script>
