<template>
  <main class="max-w-7xl mx-auto px-page-margin-desktop py-12 bg-background min-h-screen">
    <header class="mb-8 flex items-center gap-3">
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
    <div v-if="!violationsLoading && violations.length === 0" class="mb-8 p-6 bg-container border border-outline-variant rounded-lg text-center">
      <span class="material-symbols-outlined text-5xl text-outline mb-3 block">verified</span>
      <p class="font-body-md text-on-surface-variant">暂无违规记录，无需申诉</p>
      <router-link to="/my-violations" class="inline-block mt-3 text-primary text-body-sm hover:underline">查看我的违规记录</router-link>
    </div>

    <!-- 申诉表单 -->
    <template v-if="violations.length > 0">
      <!-- 步骤1：选择要申诉的违规记录 -->
      <div class="bg-container border border-outline-variant rounded-lg p-card-padding mb-6">
        <h3 class="font-headline-sm text-headline-sm mb-1">① 选择要申诉的违规记录</h3>
        <p class="font-body-sm text-on-surface-variant mb-4">点击选择一条违规记录进行申诉，已有待审核申诉的记录不可选</p>

        <div v-if="violationsLoading" class="flex items-center justify-center py-8">
          <span class="inline-block w-5 h-5 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span>
        </div>

        <div v-else class="space-y-2 max-h-[360px] overflow-y-auto">
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
              <span v-if="selectedViolation && selectedViolation.id === item.id" class="material-symbols-outlined text-primary text-[20px]">radio_button_checked</span>
              <span v-else class="material-symbols-outlined text-outline text-[20px]">radio_button_unchecked</span>
            </div>
            <!-- 违规信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="font-body-md font-semibold text-on-surface">{{ getViolationLabel(item.violationType) }}</span>
                <span class="font-body-sm text-red-600">-{{ item.pointsDeducted }}分</span>
                <span v-if="isAppealed(item.id)" class="px-1.5 py-0.5 rounded text-[11px] bg-yellow-100 text-yellow-700">已申诉</span>
              </div>
              <p v-if="item.remark" class="font-body-sm text-on-surface-variant truncate">{{ item.remark }}</p>
              <p class="font-body-sm text-outline mt-0.5">{{ item.createTime }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤2：填写申诉理由 -->
      <div class="bg-container border border-outline-variant rounded-lg p-card-padding mb-6">
        <h3 class="font-headline-sm text-headline-sm mb-1">② 填写申诉理由</h3>
        <p class="font-body-sm text-on-surface-variant mb-4">请详细说明申诉原因，有助于管理员快速处理</p>
        <textarea
          v-model="appealContent"
          class="w-full px-3 py-2 bg-surface border border-outline-variant rounded-lg focus:border-primary outline-none resize-none"
          rows="4"
          placeholder="请描述您的申诉理由..."
          :disabled="!selectedViolation"
        ></textarea>
        <div class="flex justify-between items-center mt-4">
          <span class="font-body-sm text-outline">{{ appealContent.length }}/500</span>
          <button
            class="px-6 py-2 bg-primary text-on-primary rounded-lg hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed font-label-md"
            :disabled="!selectedViolation || !appealContent.trim() || submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '提交中...' : '提交申诉' }}
          </button>
        </div>
      </div>
    </template>

    <!-- 我的申诉记录 -->
    <div v-if="!loading && list.length > 0" class="mt-10">
      <h3 class="font-headline-sm text-headline-sm mb-4">我的申诉记录</h3>
      <div class="space-y-3">
        <div v-for="item in list" :key="item.id" class="bg-container border border-outline-variant rounded-lg p-card-padding">
          <div class="flex items-start justify-between">
            <div>
              <div class="flex items-center gap-2 mb-2">
                <span :class="{
                  'px-2 py-0.5 rounded text-[12px] font-medium': true,
                  'bg-yellow-100 text-yellow-800': item.status === 'pending',
                  'bg-green-100 text-green-800': item.status === 'accepted',
                  'bg-red-100 text-red-800': item.status === 'rejected'
                }">{{ getStatusLabel(item.status) }}</span>
                <span class="font-body-sm text-on-surface-variant">{{ getAppealLabel(item.appealType) }}</span>
              </div>
              <p class="font-body-md text-body-md text-on-surface">{{ item.content }}</p>
              <p v-if="item.reviewRemark" class="font-body-sm text-body-sm text-on-surface-variant mt-2">审核意见: {{ item.reviewRemark }}</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">{{ item.createTime }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script>
import { Message } from 'element-ui'

export default {
  name: 'BBSMyAppeals',
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
    getUserId() { return JSON.parse(localStorage.getItem('user') || '{}').id },
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
        if (res && res.code == 200 && res.obj) this.violations = res.obj.records || []
        else this.violations = []
        // 从路由 query 预选违规记录（从违规记录页"申诉"按钮跳转过来）
        const preselectId = this.$route.query.violationId
        if (preselectId) {
          const target = this.violations.find(v => String(v.id) === String(preselectId))
          if (target && !this.isAppealed(target.id)) {
            this.selectedViolation = target
          }
          // 清除 query 参数，避免刷新重复选中
          if (this.$route.query.violationId) {
            this.$router.replace({ path: '/my-appeals' })
          }
        }
      } catch (e) { this.violations = [] }
      finally { this.violationsLoading = false }
    },
    async loadList() {
      const userId = this.getUserId()
      if (!userId) return
      this.loading = true
      try {
        const res = await this.postRequest('/user/appeal/myList', { userId, page: 1, size: 100 })
        if (res && res.code == 200 && res.obj) {
          this.list = res.obj.records || []
          // 记录已有待审核申诉的违规ID，防止重复申诉
          this.appealedViolationIds = this.list
            .filter(a => a.status === 'pending' && a.appealType === 'violation' && a.relatedId != null)
            .map(a => a.relatedId)
        } else {
          this.list = []
        }
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
        if (res && res.code == 200) {
          this.appealContent = ''
          this.selectedViolation = null
          this.submitSuccess = true
          await this.loadList()
          await this.loadViolations() // 刷新违规列表（可能需要更新已申诉状态）
        } else {
          this.submitError = (res && res.message) || '提交失败，请稍后重试'
        }
      } catch (e) {
        this.submitError = '网络异常，请稍后重试'
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>
