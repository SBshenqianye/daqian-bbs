<template>
  <main :class="embedded ? '' : 'max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen'">
    <header v-if="!embedded" class="mb-8 flex items-center gap-3">
      <button
        class="p-2 -ml-2 rounded-full hover:bg-surface-container-low transition-colors text-on-surface-variant"
        @click="$router.back()"
      >
        <span class="material-symbols-outlined text-[24px]">arrow_back</span>
      </button>
      <h1 class="font-headline-lg text-headline-lg text-on-surface">申诉</h1>
    </header>

    <!-- 提交成功提示 -->
    <div v-if="submitSuccess" class="mb-6 flex items-center gap-3 p-4 bg-green-50 border border-green-200 rounded-lg">
      <span class="material-symbols-outlined text-green-600 text-[24px]">check_circle</span>
      <span class="font-body-md text-green-800">申诉已提交，等待审核</span>
    </div>

    <!-- 提交失败提示 -->
    <div v-if="submitError" class="mb-6 flex items-center gap-3 p-4 bg-red-50 border border-red-200 rounded-lg">
      <span class="material-symbols-outlined text-red-600 text-[24px]">error</span>
      <span class="font-body-md text-red-800">{{ submitError }}</span>
    </div>

    <!-- 无违规记录 -->
    <div v-if="!violationsLoading && violations.length === 0" class="ui-card p-6 text-center">
      <span class="material-symbols-outlined text-[36px] text-outline opacity-30 mb-2 block">verified</span>
      <p class="text-[13px] text-on-surface-variant">暂无违规记录，无需申诉</p>
      <router-link to="/userinfo?tab=violations" class="inline-flex items-center gap-1 mt-2 text-primary text-[12px] hover:underline">
        <span class="material-symbols-outlined text-[14px]">arrow_forward</span>查看我的违规记录
      </router-link>
    </div>

    <!-- 申诉表单 -->
    <template v-if="violations.length > 0">
      <!-- 步骤1：选择要申诉的违规记录 -->
      <div class="ui-card p-4 mb-3">
        <h3 class="flex items-center gap-2 text-[14px] font-semibold text-on-surface mb-1">
          <span class="w-5 h-5 rounded-full bg-primary/10 text-primary text-[11px] font-bold grid place-items-center">1</span>
          选择要申诉的违规记录
        </h3>
        <p class="text-[11.5px] text-outline mb-3 ml-7">点击选择一条违规记录，已有待审核申诉的记录不可选</p>

        <div v-if="violationsLoading" class="flex items-center justify-center py-8">
          <span class="inline-block w-5 h-5 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
        </div>

        <div v-else class="space-y-2 max-h-[360px] overflow-y-auto custom-scrollbar">
          <div
            v-for="item in violations"
            :key="item.id"
            class="flex items-start gap-3 p-3 rounded-lg border transition-all cursor-pointer"
            :class="selectedViolation && selectedViolation.id === item.id
              ? 'border-primary bg-primary/5 ring-1 ring-primary/20'
              : isAppealed(item.id) ? 'border-outline-variant/50 bg-surface-container-lowest/50 opacity-60 cursor-not-allowed' : 'border-outline-variant hover:border-primary/50 hover:bg-surface-container-low'"
            @click="selectViolation(item)"
          >
            <!-- 选中指示 -->
            <div class="mt-0.5 flex-shrink-0">
              <span v-if="selectedViolation && selectedViolation.id === item.id" class="material-symbols-outlined text-primary text-[18px]">radio_button_checked</span>
              <span v-else class="material-symbols-outlined text-outline text-[18px]">radio_button_unchecked</span>
            </div>
            <!-- 违规信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-[12.5px] font-semibold text-on-surface">{{ getViolationLabel(item.violationType) }}</span>
                <span class="text-[11.5px] text-red-600 tabnum">-{{ item.pointsDeducted }}分</span>
                <span v-if="isAppealed(item.id)" class="ui-chip bg-amber-50 text-amber-700">已申诉</span>
              </div>
              <p v-if="item.remark" class="text-[11.5px] text-on-surface-variant truncate">{{ item.remark }}</p>
              <p class="ui-meta mt-0.5">{{ item.createTime }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤2：填写申诉理由 -->
      <div class="ui-card p-4 mb-3">
        <h3 class="flex items-center gap-2 text-[14px] font-semibold text-on-surface mb-1">
          <span class="w-5 h-5 rounded-full bg-primary/10 text-primary text-[11px] font-bold grid place-items-center">2</span>
          填写申诉理由
        </h3>
        <p class="text-[11.5px] text-outline mb-3 ml-7">请详细说明申诉原因，有助于管理员快速处理</p>
        <textarea
          v-model="appealContent"
          class="ui-textarea"
          rows="4"
          placeholder="请描述您的申诉理由..."
          :disabled="!selectedViolation"
        ></textarea>
        <div class="flex justify-between items-center mt-3">
          <span class="text-[11px] text-outline tabnum">{{ appealContent.length }}/500</span>
          <button
            class="ui-btn ui-btn-sm ui-btn-primary"
            :disabled="!selectedViolation || !appealContent.trim() || submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '提交中...' : '提交申诉' }}
          </button>
        </div>
      </div>
    </template>

    <!-- 我的申诉记录 -->
    <div v-if="!loading && list.length > 0" class="mt-5">
      <h3 class="ui-section-title mb-3">我的申诉记录</h3>
      <div class="space-y-2">
        <div v-for="item in list" :key="item.id" class="ui-card p-3.5">
          <div class="flex items-center gap-2 mb-1.5">
            <span class="ui-chip"
              :class="{
                'bg-amber-50 text-amber-700': item.status === 'pending',
                'bg-green-50 text-green-700': item.status === 'accepted',
                'bg-red-50 text-red-700': item.status === 'rejected'
              }">{{ getStatusLabel(item.status) }}</span>
            <span class="text-[11px] text-on-surface-variant">{{ getAppealLabel(item.appealType) }}</span>
            <span class="ui-meta ml-auto">{{ item.createTime }}</span>
          </div>
          <p class="text-[12.5px] leading-relaxed text-on-surface">{{ item.content }}</p>
          <p v-if="item.reviewRemark" class="mt-1.5 text-[11.5px] text-on-surface-variant">审核意见：{{ item.reviewRemark }}</p>
        </div>
      </div>
    </div>
  </main>
</template>

<script>
import { handleResponse } from '../../../shared/feedback'
import { getUser } from '@/utils/auth'

export default {
  name: 'BBSMyAppeals',
  props: { embedded: { type: Boolean, default: false } },
  data() {
    return {
      loading: false,
      violationsLoading: false,
      submitting: false,
      list: [],
      violations: [],
      selectedViolation: null,
      appealContent: '',
      submitSuccess: false,
      submitError: '',
      appealedViolationIds: [], // 已有待审核申诉的违规ID
    }
  },
  mounted() {
    this.loadViolations()
    this.loadList()
  },
  methods: {
    getUserId() { const user = getUser(); return user && user.id },
    getStatusLabel(s) { return { pending: '待审核', accepted: '已通过', rejected: '已驳回' }[s] || s },
    getAppealLabel(t) { return { violation: '违规申诉', points: '积分申诉', other: '其他' }[t] || t },
    getViolationLabel(t) {
      return { illegal: '违法违规内容', attack: '人身攻击', spam: '恶意灌水', plagiarism: '抄袭剽窃', false_report: '虚假举报', leak: '泄露秘密' }[t] || t
    },
    isAppealed(violationId) {
      return this.appealedViolationIds.includes(violationId)
    },
    selectViolation(item) {
      if (this.isAppealed(item.id)) return
      this.selectedViolation = this.selectedViolation && this.selectedViolation.id === item.id ? null : item
      this.submitSuccess = false
      this.submitError = ''
    },
    async loadViolations() {
      const userId = this.getUserId()
      if (!userId) return
      this.violationsLoading = true
      try {
        const res = await this.postRequest('/user/violation/myList', { userId, page: 1, size: 100 })
        handleResponse(res, {
          silent: true,
          onSuccess: (resp) => {
            this.violations = (resp.obj && resp.obj.records) || []
            // 从路由 query 预选违规记录（从违规记录页"申诉"按钮跳转过来）
            const preselectId = this.$route.query.violationId
            if (preselectId) {
              const target = this.violations.find(v => String(v.id) === String(preselectId))
              if (target && !this.isAppealed(target.id)) {
                this.selectedViolation = target
              }
              // 清除 query 参数，避免刷新重复选中（内嵌时不改路由）
              if (this.$route.query.violationId && !this.embedded) {
                this.$router.replace({ path: '/userinfo', query: { tab: 'appeals' } })
              }
            }
          },
          onError: () => { this.violations = [] },
        })
      } catch (e) { this.violations = [] }
      finally { this.violationsLoading = false }
    },
    async loadList() {
      const userId = this.getUserId()
      if (!userId) return
      this.loading = true
      try {
        const res = await this.postRequest('/user/appeal/myList', { userId, page: 1, size: 100 })
        handleResponse(res, {
          silent: true,
          onSuccess: (resp) => {
            this.list = (resp.obj && resp.obj.records) || []
            // 记录已有待审核申诉的违规ID，防止重复申诉
            this.appealedViolationIds = this.list
              .filter(a => a.status === 'pending' && a.appealType === 'violation' && a.relatedId != null)
              .map(a => a.relatedId)
          },
          onError: () => { this.list = [] },
        })
      } catch (e) { this.list = [] }
      finally { this.loading = false }
    },
    async handleSubmit() {
      this.submitSuccess = false
      this.submitError = ''
      if (!this.selectedViolation) {
        this.submitError = '请先选择一条违规记录'
        return
      }
      if (!this.appealContent.trim()) {
        this.submitError = '请填写申诉理由'
        return
      }
      const userId = this.getUserId()
      if (!userId) return
      this.submitting = true
      try {
        const res = await this.postRequest('/user/appeal/submit', {
          userId,
          appealType: 'violation',
          relatedId: this.selectedViolation.id,
          content: this.appealContent.trim(),
        })
        handleResponse(res, {
          successMsg: '申诉已提交',
          onSuccess: async () => {
            this.appealContent = ''
            this.selectedViolation = null
            this.submitSuccess = true
            await this.loadList()
            await this.loadViolations() // 刷新违规列表（可能需要更新已申诉状态）
          },
          onError: (resp) => {
            this.submitError = (resp && resp.message) || '提交失败，请稍后重试'
          },
        })
      } catch (e) {
        this.submitError = '网络异常，请稍后重试'
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>
