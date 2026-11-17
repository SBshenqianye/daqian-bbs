<template>
  <main :class="embedded ? '' : 'max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen'">
    <header v-if="!embedded" class="mb-8 flex items-center gap-3">
      <button
        class="p-2 -ml-2 rounded-full hover:bg-surface-container-low transition-colors text-on-surface-variant"
        @click="$router.back()"
      >
        <span class="material-symbols-outlined text-[24px]">arrow_back</span>
      </button>
      <h1 class="font-headline-lg text-headline-lg text-on-surface">我的违规记录</h1>
    </header>

    <div v-if="loading" class="flex items-center justify-center py-16">
      <span class="inline-block w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
    </div>

    <template v-if="!loading">
      <div v-if="list.length > 0" class="space-y-2">
        <div v-for="item in list" :key="item.id" class="ui-card p-3.5 flex items-start gap-3">
          <span class="w-8 h-8 rounded-lg bg-error/10 grid place-items-center shrink-0 mt-0.5">
            <span class="material-symbols-outlined text-[17px] text-error">gavel</span>
          </span>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-1.5 flex-wrap">
              <span class="text-[13px] font-semibold text-on-surface">{{ getViolationLabel(item.violationType) }}</span>
              <!-- 申诉状态标签 -->
              <span v-if="item.appealStatus === 'pending'" class="ui-chip bg-amber-50 text-amber-700">申诉中</span>
              <span v-else-if="item.appealStatus === 'accepted'" class="ui-chip bg-green-50 text-green-700">申诉通过</span>
              <span v-else-if="item.appealStatus === 'rejected'" class="ui-chip bg-red-50 text-red-700">申诉驳回</span>
            </div>
            <p class="text-[12px] text-on-surface-variant mt-1 line-clamp-2">{{ item.remark || '无备注' }}</p>
            <p class="ui-meta mt-1"><span class="material-symbols-outlined">schedule</span>{{ item.createTime }}</p>
          </div>
          <div class="flex items-center gap-2 flex-shrink-0">
            <span class="text-[14px] font-bold text-red-600 tabnum">-{{ item.pointsDeducted }}分</span>
            <!-- 申诉按钮 -->
            <button
              v-if="!item.appealStatus || item.appealStatus === 'rejected'"
              class="ui-chip ui-chip-primary px-2 py-1 hover:bg-primary/20"
              @click="goAppeal(item.id)"
            >
              申诉
            </button>
          </div>
        </div>
      </div>
      <div v-else class="ui-empty">
        <span class="material-symbols-outlined">verified</span>
        <p class="ui-empty-title">暂无违规记录</p>
        <p class="ui-empty-desc">继续保持良好表现</p>
      </div>
    </template>
  </main>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import { getUser } from '@/utils/auth'

export default {
  name: 'BBSMyViolations',
  props: { embedded: { type: Boolean, default: false } },
  data() { return { loading: false, list: [] } },
  mounted() { this.loadList() },
  methods: {
    getUserId() { const user = getUser(); return user && user.id },
    getViolationLabel(t) {
      return { illegal: '违法违规内容', attack: '人身攻击', spam: '恶意灌水', plagiarism: '抄袭剽窃', false_report: '虚假举报', leak: '泄露秘密' }[t] || t
    },
    goAppeal(violationId) {
      if (this.embedded) {
        this.$root.$emit('account-tab', 'appeals', { violationId: String(violationId) })
      } else {
        this.$router.push({ path: '/userinfo', query: { tab: 'appeals', violationId: String(violationId) } })
      }
    },
    async loadList() {
      const userId = this.getUserId()
      if (!userId) return
      this.loading = true
      try {
        const res = await this.postRequest('/user/violation/myList', { userId, page: 1, size: 100 })
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
